package com.qaautomation.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "azure.openai")
@Data
public class AzureOpenAIProperties {
    private String endpoint;
    private String apiKey;
    private String deploymentGpt4;
    private String deploymentEmbedding;
    private Integer timeoutSeconds = 30;
    private Integer maxRetries = 3;
    private Integer embeddingDimension = 1536;
}
