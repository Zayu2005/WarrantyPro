package com.warrantypro.agent.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "warranty.ai")
public class AgentProperties {
    private boolean enabled;
    private String provider = "openai-compatible";
    private String baseUrl = "https://api.openai.com/v1";
    private String model = "gpt-4o-mini";
    private String apiKey = "";
    private int timeoutSeconds = 30;
    private Workflow workflow = new Workflow();

    @Data
    public static class Workflow {
        private int pollMs = 2000;
        private int maxInputChars = 2000;
    }
}
