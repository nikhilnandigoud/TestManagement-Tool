package com.qaautomation.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestTemplate;

/**
 * Configuration class for application beans.
 */
@Slf4j
@Configuration
@EnableJpaAuditing
public class AppConfiguration {

    /**
     * Configures RestTemplate for making HTTP requests.
     */
    @Bean
    public RestTemplate restTemplate(RestTemplateBuilder builder) {
        log.debug("Initializing RestTemplate bean");
        return builder
            .setConnectTimeout(java.time.Duration.ofSeconds(10))
            .setReadTimeout(java.time.Duration.ofSeconds(60))
            .build();
    }

    /**
     * Configures RestClient for making HTTP requests.
     */
    @Bean
    public RestClient restClient(RestTemplate restTemplate) {
        log.debug("Initializing RestClient bean");
        return RestClient.create(restTemplate);
    }

    /**
     * Configures ObjectMapper for JSON serialization/deserialization.
     */
    @Bean
    @Primary
    public ObjectMapper objectMapper() {
        log.debug("Initializing ObjectMapper bean");
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        log.debug("Registered JavaTimeModule for LocalDateTime serialization");
        return mapper;
    }
}
