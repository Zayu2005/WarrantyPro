package com.warrantypro.dispatch.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.Map;

/** 派单参数（warranty.dispatch.*；sys_config 运行时覆盖在参数中心落地后接入）。 */
@Data
@Component
@ConfigurationProperties(prefix = "warranty.dispatch")
public class DispatchProperties {

    /** 四因子权重 */
    private Map<String, Double> weights = Map.of(
            "skill", 0.40, "load", 0.25, "location", 0.20, "rating", 0.15);

    /** 到场超时（小时），超时触发自动改派 */
    private int arriveTimeoutHours = 4;

    /** 自动改派最大轮次 */
    private int maxReassignRounds = 3;

    /** 师傅并发单量上限（画像未配置时的缺省） */
    private int maxConcurrent = 3;

    public double weight(String key) {
        return weights.getOrDefault(key, 0.0);
    }
}
