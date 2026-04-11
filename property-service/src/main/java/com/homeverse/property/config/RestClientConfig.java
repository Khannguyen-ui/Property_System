package com.homeverse.property.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
public class RestClientConfig {
    @Value("${homeverse.services.identity}")
    private String identityServiceUrl;

    @Bean
    public RestClient identityRestClient() {
        return RestClient.builder()
                .baseUrl(identityServiceUrl)
                .build();
    }
}