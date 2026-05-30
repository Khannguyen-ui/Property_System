package com.homeverse.aiworker.config;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.json.jackson.JacksonJsonpMapper;
import co.elastic.clients.transport.ElasticsearchTransport;
import co.elastic.clients.transport.rest_client.RestClientTransport;
import org.apache.http.Header;
import org.apache.http.HttpHost;
import org.apache.http.message.BasicHeader;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestClientBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary; // 🟢 IMPORT THÊM CÁI NÀY

@Configuration
public class ElasticConfig {

    @Value("${spring.elasticsearch.uris}")
    private String uris;

    @Value("${spring.elasticsearch.api-key:}")
    private String apiKey;

    @Bean
    public RestClient restClient() {
        HttpHost httpHost = HttpHost.create(uris);
        RestClientBuilder builder = RestClient.builder(httpHost);

        if (org.springframework.util.StringUtils.hasText(apiKey)) {
            builder.setDefaultHeaders(new Header[]{
                    new BasicHeader("Authorization", "ApiKey " + apiKey)
            });
        }

        return builder.build();
    }

    @Bean
    @Primary
    public ElasticsearchClient elasticsearchClient(RestClient restClient) {
        ElasticsearchTransport transport =
                new RestClientTransport(restClient, new JacksonJsonpMapper());
        return new ElasticsearchClient(transport);
    }
}