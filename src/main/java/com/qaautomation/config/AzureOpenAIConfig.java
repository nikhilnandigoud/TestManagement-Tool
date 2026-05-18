package com.qaautomation.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;
import java.time.Duration;

@Slf4j
@Configuration
public class AzureOpenAIConfig {
    
    @Bean
    @ConditionalOnMissingBean(RestTemplate.class)
    public RestTemplate restTemplate(RestTemplateBuilder builder) {
        return builder
            .setConnectTimeout(Duration.ofSeconds(30))
            .setReadTimeout(Duration.ofSeconds(30))
            .build();
    }
    
    @Bean
    @ConditionalOnExpression("'${azure.openai.endpoint:}' != ''")
    public String azureOpenAIHealthCheck(AzureOpenAIProperties properties) {
        log.info("Azure OpenAI configured with endpoint: {}", properties.getEndpoint());
        return "configured";
    }
}
