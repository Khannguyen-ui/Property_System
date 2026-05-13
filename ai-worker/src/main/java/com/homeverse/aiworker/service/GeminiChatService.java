package com.homeverse.aiworker.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Service
public class GeminiChatService {

    @Value("${gemini.api-key}")
    private String apiKey;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final RestTemplate restTemplate;

    public GeminiChatService(RestTemplateBuilder builder) {
        this.restTemplate = builder
                .setConnectTimeout(Duration.ofSeconds(10))
                .setReadTimeout(Duration.ofSeconds(60))
                .build();
    }

    // Gắn annotation của Resilience4j
    @RateLimiter(name = "geminiApi", fallbackMethod = "geminiFallback")
    @Retry(name = "geminiApi")
    public String callGemini25Flash(String systemPrompt, String userMessage) {

        log.info("🚀 Calling Gemini 2.5 Flash...");
        String url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key=" + apiKey;

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        Map<String, Object> body = Map.of(
                "system_instruction", Map.of("parts", List.of(Map.of("text", systemPrompt))),
                "contents", List.of(Map.of("role", "user", "parts", List.of(Map.of("text", userMessage)))),
                "generationConfig", Map.of(
                        "response_mime_type", "application/json",
                        "response_schema", Map.of(
                                "type", "OBJECT",
                                "properties", Map.of(
                                        "summary", Map.of("type", "STRING"),
                                        "selectedPropertyIds", Map.of(
                                                "type", "ARRAY",
                                                "items", Map.of("type", "NUMBER")
                                        )
                                ),
                                "required", List.of("summary", "selectedPropertyIds")
                        ),
                        "temperature", 0.2,
                        "topP", 0.8,
                        "maxOutputTokens", 2048
                )
        );

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);

        try {
            String response = restTemplate.postForObject(url, request, String.class);
            if (response == null || response.isBlank()) throw new RuntimeException("Gemini trả response rỗng.");

            JsonNode root = objectMapper.readTree(response);

            // Validate block reason & candidates (Giữ nguyên logic của bạn)
            if (root.path("promptFeedback").hasNonNull("blockReason")) {
                throw new RuntimeException("Gemini block request: " + root.path("promptFeedback").path("blockReason").asText());
            }

            JsonNode candidates = root.path("candidates");
            if (!candidates.isArray() || candidates.isEmpty())
                throw new RuntimeException("Gemini không trả candidates.");

            // BỔ SUNG: Kiểm tra xem Gemini có bị ngắt ngang do hết Token không
            String finishReason = candidates.get(0).path("finishReason").asText(null);
            if ("MAX_TOKENS".equals(finishReason)) {
                log.warn("⚠️ Cảnh báo: Phản hồi từ Gemini bị cắt cụt do vượt quá maxOutputTokens.");
                throw new RuntimeException("Phản hồi quá dài, hệ thống không thể xử lý hết.");
            }

            String text = candidates.get(0).path("content").path("parts").get(0).path("text").asText(null);
            if (text == null || text.isBlank()) throw new RuntimeException("Gemini trả text rỗng.");

            String cleanJson = extractJson(text);
            if (cleanJson == null || cleanJson.isBlank()) throw new RuntimeException("Không tìm thấy JSON hợp lệ.");

            // Kích hoạt check hợp lệ JSON
            objectMapper.readTree(cleanJson);
            log.info("✅ Gemini JSON response success");

            return cleanJson;

        } catch (HttpStatusCodeException e) {
            log.error("❌ Gemini API Error {}: {}", e.getStatusCode(), e.getResponseBodyAsString());
            throw e; // Ném ra để cơ chế @Retry bắt lấy
        } catch (Exception e) {
            log.error("❌ Lỗi xử lý Gemini: {}", e.getMessage());
            throw new RuntimeException("Lỗi xử lý Gemini: " + e.getMessage(), e);
        }
    }

    // Fallback method được gọi khi RateLimiter chặn request (hoặc sau khi Retry hết số lần)
    public String geminiFallback(String systemPrompt, String userMessage, Exception ex) {
        log.warn(" Kích hoạt Fallback do hệ thống quá tải: {}", ex.getMessage());
        // Trả về JSON mặc định giả lập để RagOrchestratorService vẫn parse được mà không bị crash
        return """
                {
                  "summary": "Hệ thống AI hiện đang xử lý quá nhiều yêu cầu, vui lòng thử lại sau ít phút.",
                  "selectedPropertyIds": []
                }
                """;
    }

    // =========================================================
    // JSON EXTRACTOR
    // =========================================================
    private String extractJson(String text) {

        if (text == null || text.isBlank()) {
            return null;
        }

        String cleaned = text.trim();

        Pattern fencedPattern = Pattern.compile(
                "(?s)```(?:json)?\\s*(\\{.*?\\})\\s*```"
        );

        Matcher fencedMatcher = fencedPattern.matcher(cleaned);

        if (fencedMatcher.find()) {

            return fencedMatcher.group(1).trim();
        }


        int start = cleaned.indexOf('{');
        int end = cleaned.lastIndexOf('}');

        if (start != -1 && end != -1 && start < end) {

            return cleaned.substring(start, end + 1).trim();
        }

        return null;
    }


}