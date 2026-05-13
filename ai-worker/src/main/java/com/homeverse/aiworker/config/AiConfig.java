package com.homeverse.aiworker.config;

import org.elasticsearch.client.RestClient;
import org.springframework.ai.vectorstore.ElasticsearchVectorStore;
import org.springframework.ai.vectorstore.ElasticsearchVectorStoreOptions;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

// 🟢 Import thẳng class service của bạn vào
import com.homeverse.aiworker.service.GeminiEmbeddingService;

@Configuration
public class AiConfig {


    @Bean
    public VectorStore vectorStore(RestClient restClient, GeminiEmbeddingService geminiEmbeddingService) {

        ElasticsearchVectorStoreOptions options = new ElasticsearchVectorStoreOptions();
        options.setIndexName("homeverse-properties-vector");
        options.setDimensions(768);

        return new ElasticsearchVectorStore(
                options,
                restClient,
                geminiEmbeddingService,
                true
        );
    }
}