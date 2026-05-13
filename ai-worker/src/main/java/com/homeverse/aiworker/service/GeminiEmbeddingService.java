package com.homeverse.aiworker.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.embedding.Embedding;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.embedding.EmbeddingRequest;
import org.springframework.ai.embedding.EmbeddingResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.ai.document.Document;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor // 🟢 Dùng Lombok để tự động Inject
public class GeminiEmbeddingService implements EmbeddingModel { // 🟢 BẮT BUỘC PHẢI CÓ ĐỂ FIX LỖI BEAN

    @Value("${gemini.api-key}")
    private String apiKey;

    // 🟢 Inject ObjectMapper của hệ thống thay vì tạo mới (Tùy chọn: có thể inject luôn RestTemplate)
    private final ObjectMapper objectMapper;
    private final RestTemplate restTemplate = new RestTemplate();

    public List<Double> embedText(String text) {
        String url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-embedding-001:embedContent";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("x-goog-api-key", apiKey);

        Map<String, Object> body = Map.of(
                "content", Map.of(
                        "parts", List.of(Map.of("text", text))
                ),
                "output_dimensionality", 768
        );

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);

        try {
            String response = restTemplate.postForObject(url, request, String.class);
            JsonNode root = objectMapper.readTree(response);
            JsonNode valuesNode = root.path("embedding").path("values");

            List<Double> vector = new ArrayList<>();
            if (valuesNode.isArray()) {
                for (JsonNode node : valuesNode) {
                    vector.add(node.asDouble());
                }
            }
            return vector;
        } catch (Exception e) {
            // 🟢 Tối ưu: Log lỗi ra màn hình để biết chính xác tại sao gọi Google thất bại
            log.error("❌ Gọi API Gemini Embedding thất bại! Message: {}", e.getMessage());
            throw new RuntimeException("Lỗi tự gọi Gemini API: " + e.getMessage(), e);
        }
    }

    // 🟢 BẮT BUỘC CÓ: Hàm này để Spring AI (VectorStore) mượn class này đi nhúng Vector
    @Override
    public EmbeddingResponse call(EmbeddingRequest request) {
        List<Embedding> embeddings = new ArrayList<>();
        int index = 0;
        for (String text : request.getInstructions()) {
            List<Double> vector = embedText(text);
            embeddings.add(new Embedding(vector, index++));
        }
        return new EmbeddingResponse(embeddings);
    }
    // 🟢 THÊM HÀM NÀY ĐỂ FIX LỖI "implement abstract method 'embed(Document)'"
    @Override
    public List<Double> embed(Document document) {
        // Lấy nội dung text từ Document ra và nhúng bằng hàm tự chế của sếp
        return embedText(document.getContent());
    }

    // 🟢 THÊM LUÔN HÀM NÀY CHO CHẮC CÚ (Phòng hờ Spring AI đòi thêm)
    @Override
    public List<Double> embed(String text) {
        return embedText(text);
    }
}