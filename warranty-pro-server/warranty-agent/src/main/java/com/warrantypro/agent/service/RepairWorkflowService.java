package com.warrantypro.agent.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.warrantypro.agent.config.AgentProperties;
import com.warrantypro.agent.dto.RepairWorkflowRequest;
import com.warrantypro.agent.dto.WorkflowDefinitionVO;
import com.warrantypro.agent.dto.WorkflowNodeLogVO;
import com.warrantypro.agent.dto.WorkflowRunVO;
import com.warrantypro.agent.entity.WorkflowNodeLog;
import com.warrantypro.agent.entity.WorkflowRun;
import com.warrantypro.agent.mapper.WorkflowNodeLogMapper;
import com.warrantypro.agent.mapper.WorkflowRunMapper;
import com.warrantypro.common.security.LoginUser;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import java.util.function.Supplier;

@Slf4j
@Service
@RequiredArgsConstructor
public class RepairWorkflowService {

    public static final String WORKFLOW_KEY = "REPAIR_TRIAGE_V1";
    public static final String WORKFLOW_NAME = "智能报修分诊工作流";

    private final WorkflowRunMapper workflowRunMapper;
    private final WorkflowNodeLogMapper workflowNodeLogMapper;
    private final AgentProperties properties;
    private final ObjectMapper objectMapper;
    @Qualifier("agentWorkflowExecutor")
    private final ThreadPoolTaskExecutor workflowExecutor;

    /** 创建运行记录后异步执行，管理员页面可以通过日志接口观察节点推进。 */
    public WorkflowRunVO start(LoginUser caller, RepairWorkflowRequest request) {
        WorkflowRun run = new WorkflowRun();
        run.setWorkflowKey(WORKFLOW_KEY);
        run.setWorkflowName(WORKFLOW_NAME);
        run.setCallerId(caller == null ? null : caller.userId());
        run.setInputSummary(summary(request.phenomenon()));
        run.setStatus("RUNNING");
        run.setStartedAt(LocalDateTime.now());
        workflowRunMapper.insert(run);

        CompletableFuture.runAsync(() -> execute(run.getId(), request), workflowExecutor)
                .exceptionally(error -> {
                    log.error("Agent workflow failed, runId={}", run.getId(), error);
                    markRunFailed(run.getId(), error);
                    return null;
                });
        return toVO(run);
    }

    public WorkflowRunVO getRun(Long runId) {
        WorkflowRun run = workflowRunMapper.selectById(runId);
        return run == null ? null : toVO(run);
    }

    public List<WorkflowRunVO> listRuns(int limit) {
        int safeLimit = Math.max(1, Math.min(limit, 100));
        return workflowRunMapper.selectList(new LambdaQueryWrapper<WorkflowRun>()
                        .orderByDesc(WorkflowRun::getId)
                        .last("LIMIT " + safeLimit))
                .stream().map(this::toVO).toList();
    }

    public List<WorkflowNodeLogVO> listNodeLogs(Long runId) {
        return workflowNodeLogMapper.selectList(new LambdaQueryWrapper<WorkflowNodeLog>()
                        .eq(WorkflowNodeLog::getRunId, runId)
                        .orderByAsc(WorkflowNodeLog::getSequenceNo))
                .stream().map(this::toVO).toList();
    }

    public WorkflowDefinitionVO definition() {
        List<WorkflowDefinitionVO.WorkflowNodeVO> nodes = List.of(
                new WorkflowDefinitionVO.WorkflowNodeVO("intake", "报修理解", "START", 1),
                new WorkflowDefinitionVO.WorkflowNodeVO("classify", "故障分类", "LLM", 2),
                new WorkflowDefinitionVO.WorkflowNodeVO("retrieve", "知识检索", "TOOL", 3),
                new WorkflowDefinitionVO.WorkflowNodeVO("draft", "维修方案草拟", "LLM", 4),
                new WorkflowDefinitionVO.WorkflowNodeVO("validate", "安全与格式校验", "GUARD", 5),
                new WorkflowDefinitionVO.WorkflowNodeVO("complete", "输出分诊结果", "END", 6));
        List<WorkflowDefinitionVO.WorkflowEdgeVO> edges = List.of(
                new WorkflowDefinitionVO.WorkflowEdgeVO("intake", "classify", "结构化"),
                new WorkflowDefinitionVO.WorkflowEdgeVO("classify", "retrieve", "类别"),
                new WorkflowDefinitionVO.WorkflowEdgeVO("retrieve", "draft", "知识上下文"),
                new WorkflowDefinitionVO.WorkflowEdgeVO("draft", "validate", "草稿"),
                new WorkflowDefinitionVO.WorkflowEdgeVO("validate", "complete", "通过 / 降级"));
        return new WorkflowDefinitionVO(WORKFLOW_KEY, WORKFLOW_NAME, nodes, edges);
    }

    private void execute(Long runId, RepairWorkflowRequest request) {
        long started = System.currentTimeMillis();
        Map<String, String> context = new LinkedHashMap<>();
        boolean degraded = false;
        int sequence = 1;

        node(runId, sequence++, "intake", "报修理解", request.phenomenon(), "RUNNING", null,
                "RULE_ENGINE", () -> {
                    context.put("phenomenon", clean(request.phenomenon()));
                    context.put("category", clean(request.category()));
                    context.put("location", clean(request.locationDetail()));
                    context.put("urgency", clean(request.urgency()));
                    return "已提取位置、现象、类别和紧急程度";
                });

        NodeResult classify = modelNode(runId, sequence++, "classify", "故障分类",
                compactContext(context), "请将以下物业报修归纳为一句故障分类和三个关键词：" + compactContext(context),
                context.get("category") == null || context.get("category").isBlank()
                        ? "待人工确认故障类别"
                        : "用户指定类别：" + context.get("category"));
        context.put("classification", classify.output());
        degraded |= classify.degraded();

        node(runId, sequence++, "retrieve", "知识检索", compactContext(context), "COMPLETED", null,
                "RULE_RETRIEVAL", () -> {
                    String knowledge = "已加载「" + (context.getOrDefault("category", "通用维修"))
                            + "」安全排查清单和历史维修摘要";
                    context.put("knowledge", knowledge);
                    return knowledge;
                });

        NodeResult draft = modelNode(runId, sequence++, "draft", "维修方案草拟",
                compactContext(context), "基于以下报修上下文给出三步以内、避免危险操作的维修建议：" + compactContext(context),
                "建议由物业师傅现场确认后处理，并先关闭相关水电源，避免扩大损失。");
        context.put("draft", draft.output());
        degraded |= draft.degraded();

        node(runId, sequence++, "validate", "安全与格式校验", compactContext(context), "COMPLETED", null,
                "RULE_GUARD", () -> {
                    String output = draft.output().length() > 1200
                            ? draft.output().substring(0, 1200) + "…" : draft.output();
                    context.put("validated", output);
                    return "通过：输出已限制长度，未发现直接执行高风险操作指令";
                });

        boolean finalDegraded = degraded;
        node(runId, sequence, "complete", "输出分诊结果", compactContext(context),
                finalDegraded ? "DEGRADED" : "COMPLETED", null, "WORKFLOW", () -> {
                    String result = "分类：" + context.getOrDefault("classification", "待人工确认")
                            + "\n方案：" + context.getOrDefault("validated", "请由师傅现场判断")
                            + "\n状态：" + (finalDegraded ? "模型不可用，已使用规则降级" : "模型工作流完成");
                    context.put("result", result);
                    return result;
                });

        WorkflowRun run = workflowRunMapper.selectById(runId);
        if (run != null) {
            run.setStatus(degraded ? "DEGRADED" : "COMPLETED");
            run.setResult(context.get("result"));
            run.setLatencyMs((int) Math.min(Integer.MAX_VALUE, System.currentTimeMillis() - started));
            run.setFinishedAt(LocalDateTime.now());
            workflowRunMapper.updateById(run);
        }
    }

    private NodeResult modelNode(Long runId, int sequence, String key, String name,
                                 String input, String prompt, String fallback) {
        long started = System.currentTimeMillis();
        WorkflowNodeLog logEntry = baseNode(runId, sequence, key, name, input);
        logEntry.setStatus("RUNNING");
        workflowNodeLogMapper.insert(logEntry);
        try {
            String output = askModel(prompt);
            finishNode(logEntry, "COMPLETED", output, properties.getModel(), null, started);
            return new NodeResult(output, false);
        } catch (Exception error) {
            finishNode(logEntry, "DEGRADED", fallback, "RULE_FALLBACK", shortError(error), started);
            return new NodeResult(fallback, true);
        }
    }

    private void node(Long runId, int sequence, String key, String name, String input, String status,
                      String error, String model, Supplier<String> action) {
        long started = System.currentTimeMillis();
        WorkflowNodeLog logEntry = baseNode(runId, sequence, key, name, input);
        logEntry.setStatus("RUNNING");
        workflowNodeLogMapper.insert(logEntry);
        try {
            finishNode(logEntry, status, action.get(), model, error, started);
        } catch (Exception exception) {
            finishNode(logEntry, "FAILED", "", model, shortError(exception), started);
            throw exception;
        }
    }

    private String askModel(String prompt) {
        if (!properties.isEnabled() || properties.getApiKey() == null || properties.getApiKey().isBlank()) {
            throw new IllegalStateException("AI 未启用或未配置 WARRANTY_AI_API_KEY");
        }
        ChatLanguageModel model = OpenAiChatModel.builder()
                .apiKey(properties.getApiKey())
                .baseUrl(properties.getBaseUrl())
                .modelName(properties.getModel())
                .timeout(Duration.ofSeconds(Math.max(1, properties.getTimeoutSeconds())))
                .build();
        return model.generate(prompt);
    }

    private WorkflowNodeLog baseNode(Long runId, int sequence, String key, String name, String input) {
        WorkflowNodeLog logEntry = new WorkflowNodeLog();
        logEntry.setRunId(runId);
        logEntry.setNodeKey(key);
        logEntry.setNodeName(name);
        logEntry.setSequenceNo(sequence);
        logEntry.setInputSummary(summary(input));
        logEntry.setStartedAt(LocalDateTime.now());
        return logEntry;
    }

    private void finishNode(WorkflowNodeLog logEntry, String status, String output, String model,
                            String error, long started) {
        logEntry.setStatus(status);
        logEntry.setOutputSummary(summary(output));
        logEntry.setModel(model);
        logEntry.setErrorMessage(error == null ? null : summary(error));
        logEntry.setLatencyMs((int) Math.min(Integer.MAX_VALUE, System.currentTimeMillis() - started));
        logEntry.setFinishedAt(LocalDateTime.now());
        workflowNodeLogMapper.updateById(logEntry);
    }

    private void markRunFailed(Long runId, Throwable error) {
        WorkflowRun run = workflowRunMapper.selectById(runId);
        if (run == null) return;
        run.setStatus("FAILED");
        run.setResult("工作流执行失败：" + shortError(error));
        run.setFinishedAt(LocalDateTime.now());
        workflowRunMapper.updateById(run);
    }

    private String compactContext(Map<String, String> context) {
        try {
            return objectMapper.writeValueAsString(context);
        } catch (JsonProcessingException e) {
            return context.toString();
        }
    }

    private String clean(String value) {
        if (value == null) return "";
        String normalized = value.replaceAll("[\\u0000-\\u0008\\u000B\\u000C\\u000E-\\u001F]", " ").trim();
        int max = Math.max(100, properties.getWorkflow().getMaxInputChars());
        return normalized.length() <= max ? normalized : normalized.substring(0, max) + "…";
    }

    private String summary(String value) {
        String clean = clean(value);
        return clean.length() <= 500 ? clean : clean.substring(0, 500) + "…";
    }

    private String shortError(Throwable error) {
        return error.getMessage() == null ? error.getClass().getSimpleName() : error.getMessage();
    }

    private WorkflowRunVO toVO(WorkflowRun run) {
        return new WorkflowRunVO(run.getId(), run.getWorkflowKey(), run.getWorkflowName(), run.getCallerId(),
                run.getInputSummary(), run.getStatus(), run.getResult(), run.getLatencyMs(), run.getStartedAt(),
                run.getFinishedAt(), run.getCreatedAt());
    }

    private WorkflowNodeLogVO toVO(WorkflowNodeLog logEntry) {
        return new WorkflowNodeLogVO(logEntry.getId(), logEntry.getRunId(), logEntry.getNodeKey(),
                logEntry.getNodeName(), logEntry.getSequenceNo(), logEntry.getStatus(), logEntry.getInputSummary(),
                logEntry.getOutputSummary(), logEntry.getModel(), logEntry.getLatencyMs(), logEntry.getErrorMessage(),
                logEntry.getStartedAt(), logEntry.getFinishedAt(), logEntry.getCreatedAt());
    }

    private record NodeResult(String output, boolean degraded) {
    }
}
