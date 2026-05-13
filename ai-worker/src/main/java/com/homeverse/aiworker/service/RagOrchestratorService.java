package com.homeverse.aiworker.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.homeverse.aiworker.dto.internal.PropertyCandidateDTO;
import com.homeverse.aiworker.dto.request.AiChatRequest;
import com.homeverse.aiworker.dto.response.AiChatResponse;
import com.homeverse.aiworker.dto.response.PropertyCardDTO;
import com.homeverse.aiworker.messaging.AiResponseProducer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class RagOrchestratorService {

    private final DocumentService documentService;
    private final ChatMemoryService chatMemoryService;
    private final AiResponseProducer aiResponseProducer;
    private final GeminiChatService geminiChatService;
    private final ObjectMapper objectMapper;

    public void processAndReply(AiChatRequest request) {
        log.info("Đang xử lý RAG cho User: {} (Conversation: {})",
                request.getUserId(), request.getConversationId());

        if (request.getUserMessage() == null || request.getUserMessage().trim().isEmpty()) {
            AiChatResponse response = AiChatResponse.builder()
                    .userId(request.getUserId())
                    .conversationId(request.getConversationId())
                    .aiReply("Vui lòng nhập nội dung tìm kiếm.")
                    .status("SUCCESS")
                    .items(List.of())
                    .build();

            chatMemoryService.saveMessage(request.getConversationId(), "User", "");
            chatMemoryService.saveMessage(request.getConversationId(), "AI", response.getAiReply());

            aiResponseProducer.sendReply(response);
            return;
        }

        try {
            List<Document> results = documentService.findRelevantPropertyDocuments(request.getUserMessage());

            List<PropertyCandidateDTO> candidates = results.stream()
                    .map(this::mapToPropertyCandidate)
                    .filter(item -> item.getPropertyId() != null)
                    .collect(Collectors.toMap(
                            PropertyCandidateDTO::getPropertyId,
                            item -> item,
                            (a, b) -> a,
                            LinkedHashMap::new
                    ))
                    .values()
                    .stream()
                    // Giữ thứ tự relevance từ Elastic, không sort theo giá trước
                    .limit(10)
                    .collect(Collectors.toList());

            if (candidates.isEmpty()) {
                String aiReply = "Không tìm thấy bất động sản phù hợp.";

                chatMemoryService.saveMessage(request.getConversationId(), "User", request.getUserMessage());
                chatMemoryService.saveMessage(request.getConversationId(), "AI", aiReply);

                AiChatResponse response = AiChatResponse.builder()
                        .userId(request.getUserId())
                        .conversationId(request.getConversationId())
                        .aiReply(aiReply)
                        .status("SUCCESS")
                        .items(List.of())
                        .build();

                aiResponseProducer.sendReply(response);
                return;
            }

            String contextDocs = buildCompactContext(candidates);
            String chatHistory = chatMemoryService.getChatHistory(request.getConversationId());

            String systemPrompt = String.format("""
                    Bạn là bộ lọc dữ liệu của HomeVerse.

                    Nhiệm vụ:
                    - Chỉ chọn các bất động sản phù hợp với yêu cầu khách hàng.
                    - Phải kiểm tra đúng province, district, propertyType, transactionType, status.
                    - Nếu khách yêu cầu FOR_SALE thì không chọn FOR_RENT.
                    - Nếu khách yêu cầu FOR_RENT thì không chọn FOR_SALE.
                    - Nếu khách yêu cầu Hà Nội thì không chọn TP Hồ Chí Minh.
                    - Nếu khách yêu cầu TP Hồ Chí Minh thì không chọn Hà Nội.
                    - Không viết văn dài.
                    - Không giải thích lan man.
                    - Không thêm thông tin ngoài dữ liệu cung cấp.
                    - Chỉ trả về JSON hợp lệ, không markdown, không ký tự thừa.

                    Hãy trả về đúng mẫu sau:
                    {
                      "summary": "câu ngắn gọn, tối đa 1 câu",
                      "selectedPropertyIds": [1, 2, 3]
                    }

                    Quy tắc:
                    - selectedPropertyIds chỉ chứa các propertyId thực sự phù hợp.
                    - Nếu không có bất động sản phù hợp, trả về:
                    {
                      "summary": "Không tìm thấy bất động sản phù hợp.",
                      "selectedPropertyIds": []
                    }

                    Dữ liệu bất động sản ứng viên:
                    %s

                    Lịch sử trò chuyện gần đây:
                    %s
                    """, contextDocs, chatHistory == null ? "" : chatHistory);

            String aiRaw = geminiChatService.callGemini25Flash(systemPrompt, request.getUserMessage());
            ParsedGeminiResult parsed = parseGeminiResult(aiRaw);

            List<PropertyCardDTO> selectedItems;
            String aiReply;

            if (parsed.validJson()) {
                selectedItems = resolveSelectedItems(candidates, parsed.selectedPropertyIds());

                if (selectedItems.isEmpty()) {
                    aiReply = "Không tìm thấy bất động sản phù hợp.";
                } else {
                    aiReply = (parsed.summary() == null || parsed.summary().isBlank())
                            ? "Tìm thấy " + selectedItems.size() + " bất động sản phù hợp."
                            : parsed.summary().trim();
                }
            } else {
                selectedItems = candidates.stream()
                        .limit(3)
                        .map(this::toPropertyCard)
                        .collect(Collectors.toList());

                aiReply = selectedItems.isEmpty()
                        ? "Không tìm thấy bất động sản phù hợp."
                        : "Tìm thấy " + selectedItems.size() + " bất động sản phù hợp.";
            }

            chatMemoryService.saveMessage(request.getConversationId(), "User", request.getUserMessage());
            chatMemoryService.saveMessage(request.getConversationId(), "AI", aiReply);

            AiChatResponse response = AiChatResponse.builder()
                    .userId(request.getUserId())
                    .conversationId(request.getConversationId())
                    .aiReply(aiReply)
                    .status("SUCCESS")
                    .items(selectedItems)
                    .build();

            aiResponseProducer.sendReply(response);

            log.info("Đã trả về {} items cho conversation {}",
                    selectedItems.size(),
                    request.getConversationId());

        } catch (Exception e) {
            log.error("Lỗi xử lý RAG: ", e);

            String aiReply = "Hệ thống đang bận, vui lòng thử lại.";

            chatMemoryService.saveMessage(request.getConversationId(), "User", request.getUserMessage());
            chatMemoryService.saveMessage(request.getConversationId(), "AI", aiReply);

            aiResponseProducer.sendReply(AiChatResponse.builder()
                    .userId(request.getUserId())
                    .conversationId(request.getConversationId())
                    .aiReply(aiReply)
                    .status("ERROR")
                    .items(List.of())
                    .build());
        }
    }

    private List<PropertyCardDTO> resolveSelectedItems(
            List<PropertyCandidateDTO> candidates,
            List<Long> selectedIds
    ) {
        if (selectedIds == null || selectedIds.isEmpty()) {
            return List.of();
        }

        Set<Long> idSet = new HashSet<>(selectedIds);

        return candidates.stream()
                .filter(item -> item.getPropertyId() != null && idSet.contains(item.getPropertyId()))
                .map(this::toPropertyCard)
                .limit(3)
                .collect(Collectors.toList());
    }

    private ParsedGeminiResult parseGeminiResult(String aiRaw) {
        try {
            if (aiRaw == null || aiRaw.isBlank()) {
                return new ParsedGeminiResult(false, "Không tìm thấy bất động sản phù hợp.", List.of());
            }

            JsonNode root = objectMapper.readTree(aiRaw);

            String summary = root.path("summary").asText("Không tìm thấy bất động sản phù hợp.");

            List<Long> ids = new ArrayList<>();
            JsonNode arr = root.path("selectedPropertyIds");

            if (arr.isArray()) {
                for (JsonNode node : arr) {
                    if (node.isNumber()) {
                        ids.add(node.asLong());
                    } else {
                        try {
                            ids.add(Long.parseLong(node.asText()));
                        } catch (Exception ignored) {
                        }
                    }
                }
            }

            return new ParsedGeminiResult(true, summary, ids);

        } catch (Exception e) {
            log.warn("Gemini trả JSON không hợp lệ: {}", e.getMessage());
            return new ParsedGeminiResult(
                    false,
                    aiRaw == null || aiRaw.isBlank()
                            ? "Không tìm thấy bất động sản phù hợp."
                            : aiRaw.trim(),
                    List.of()
            );
        }
    }

    private PropertyCandidateDTO mapToPropertyCandidate(Document doc) {
        Map<String, Object> meta = doc.getMetadata();

        return PropertyCandidateDTO.builder()
                .propertyId(asLong(meta.get("propertyId")))
                .title(asString(meta.get("title")))
                .price(asDouble(meta.get("price")))
                .province(asString(meta.get("province")))
                .district(asString(meta.get("district")))
                .propertyType(asString(meta.get("propertyType")))
                .transactionType(asString(meta.get("transactionType")))
                .status(asString(meta.get("status")))
                .imageUrl(asString(meta.get("imageUrl")))
                .build();
    }

    private PropertyCardDTO toPropertyCard(PropertyCandidateDTO item) {
        return PropertyCardDTO.builder()
                .propertyId(item.getPropertyId())
                .title(item.getTitle())
                .price(item.getPrice())
                .district(item.getDistrict())
                .imageUrl(item.getImageUrl())
                .build();
    }

    private String buildCompactContext(List<PropertyCandidateDTO> items) {
        if (items == null || items.isEmpty()) return "";

        return items.stream()
                .limit(10)
                .map(item -> "propertyId=" + item.getPropertyId()
                        + " | title=" + item.getTitle()
                        + " | price=" + item.getPrice()
                        + " | province=" + item.getProvince()
                        + " | district=" + item.getDistrict()
                        + " | propertyType=" + item.getPropertyType()
                        + " | transactionType=" + item.getTransactionType()
                        + " | status=" + item.getStatus())
                .collect(Collectors.joining("\n"));
    }

    private String asString(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private Long asLong(Object value) {
        try {
            if (value == null) return null;
            if (value instanceof Number number) return number.longValue();
            return Long.parseLong(String.valueOf(value));
        } catch (Exception e) {
            return null;
        }
    }

    private Double asDouble(Object value) {
        try {
            if (value == null) return null;
            if (value instanceof Number number) return number.doubleValue();
            return Double.parseDouble(String.valueOf(value));
        } catch (Exception e) {
            return null;
        }
    }

    private record ParsedGeminiResult(boolean validJson, String summary, List<Long> selectedPropertyIds) {
    }
}