package com.warrantypro.agent.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;


@Configuration
public class AgentWorkflowConfig {

    @Bean(name = "agentWorkflowExecutor")
    public ThreadPoolTaskExecutor agentWorkflowExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(8);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("agent-workflow-");
        executor.initialize();
        return executor;
    }
}
