package com.homeverse.aiworker.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.homeverse.aiworker.dto.internal.LastSearchContextDTO;
import com.homeverse.aiworker.dto.internal.PropertyCandidateDTO;
import com.homeverse.aiworker.dto.request.AiChatRequest;
import com.homeverse.aiworker.dto.response.AiChatResponse;
import com.homeverse.aiworker.dto.response.PropertyCardDTO;
import com.homeverse.aiworker.messaging.AiResponseProducer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.stereotype.Service;

import com.homeverse.aiworker.client.SearchServiceClient;
import com.homeverse.aiworker.dto.search.SearchPropertyItemDTO;


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
    private final SearchServiceClient searchServiceClient;
    private final ObjectMapper objectMapper;
    private static final int MAX_ITEMS_TO_RETURN = 5;

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
                    .totalMatched(0)
                    .hasMore(false)
                    .build();

            chatMemoryService.saveMessage(request.getUserId(), request.getConversationId(), "User", "");
            chatMemoryService.saveMessage(request.getUserId(), request.getConversationId(), "AI", response.getAiReply());

            aiResponseProducer.sendReply(response);
            return;
        }
        LastSearchContextDTO lastContext = chatMemoryService.getLastSearchContext(
                request.getUserId(),
                request.getConversationId()
        );

        if (lastContext != null
                && lastContext.getLastPropertyIds() != null
                && !lastContext.getLastPropertyIds().isEmpty()
                && isFollowUpQuestion(request.getUserMessage())) {

            processFollowUpQuestion(request, lastContext);
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

                chatMemoryService.saveMessage(request.getUserId(), request.getConversationId(), "User", request.getUserMessage());
                chatMemoryService.saveMessage(request.getUserId(), request.getConversationId(), "AI", aiReply);


                AiChatResponse response = AiChatResponse.builder()
                        .userId(request.getUserId())
                        .conversationId(request.getConversationId())
                        .aiReply(aiReply)
                        .status("SUCCESS")
                        .items(List.of())
                        .totalMatched(0)
                        .hasMore(false)
                        .build();

                aiResponseProducer.sendReply(response);
                return;
            }

            String contextDocs = buildCompactContext(candidates);

            String chatHistory = chatMemoryService.getChatHistory(
                    request.getUserId(),
                    request.getConversationId()
            );

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
                    
                    Quy đổi ngôn ngữ tự nhiên của khách:
                    - Nếu khách nói "mua", "cần mua", "bán", "nhà bán", "căn hộ bán", "mua nhà", "mua căn hộ" thì hiểu là transactionType=FOR_SALE.
                    - Nếu khách nói "thuê", "cần thuê", "cho thuê", "phòng trọ", "ở trọ", "thuê căn hộ", "thuê nhà" thì hiểu là transactionType=FOR_RENT.
                    - Nếu khách nói "phòng trọ" thì ưu tiên propertyType=ROOM.
                    - Nếu khách nói "căn hộ", "chung cư" thì ưu tiên propertyType=APARTMENT.
                    - Nếu khách nói "nhà nguyên căn", "nhà riêng" thì ưu tiên propertyType=HOUSE.
                    - Nếu khách nói "biệt thự" thì ưu tiên propertyType=VILLA.
                    - Nếu khách nói "mặt bằng", "kinh doanh" thì ưu tiên propertyType=COMMERCIAL.
                    
                    Quy tắc lọc vị trí:
                    - Nếu khách nói đang ở một nơi nhưng muốn tìm ở nơi khác, thì địa điểm cần tìm là nơi khách muốn tìm, không phải nơi khách đang ở.
                    - Ví dụ: "Tôi đang ở TP Hồ Chí Minh nhưng muốn mua nhà ở Hà Nội" thì phải chọn province=Hà Nội, không chọn TP Hồ Chí Minh.
                    - Nếu khách yêu cầu district cụ thể thì phải kiểm tra đúng district.
                    - Nếu khách yêu cầu province cụ thể thì phải kiểm tra đúng province.
                    
                    Quy tắc lọc chi tiết:
                    - Nếu khách yêu cầu có ban công thì chỉ chọn hasBalcony=true.
                    - Nếu khách yêu cầu số phòng ngủ, ví dụ "2 phòng ngủ", thì kiểm tra bedrooms.
                    - Nếu khách nói "ít nhất 2 phòng ngủ" thì chọn bedrooms >= 2.
                    - Nếu khách nói "đúng 2 phòng ngủ" thì chọn bedrooms = 2.
                    - Nếu khách yêu cầu số phòng tắm thì kiểm tra bathrooms.
                    - Nếu khách yêu cầu diện tích thì kiểm tra area.
                    - Nếu khách yêu cầu đầy đủ nội thất thì ưu tiên furnishingStatus=FULLY_FURNISHED.
                    - Nếu khách yêu cầu vào ở ngay thì ưu tiên availabilityStatus=IMMEDIATELY.
                    - Nếu khách yêu cầu tiện ích cụ thể thì kiểm tra amenities.
                    
                    Quy tắc trạng thái:
                    - Chỉ chọn bất động sản có status=ACTIVE hoặc status=APPROVED.
                    - Không chọn bất động sản PENDING, REJECTED, HIDDEN, EXPIRED, DELETED.
                    
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
            int totalMatched = 0;
            boolean hasMore = false;

            if (parsed.validJson()) {
                List<PropertyCardDTO> allMatchedItems = resolveAllSelectedItems(
                        candidates,
                        parsed.selectedPropertyIds()
                );

                selectedItems = allMatchedItems.stream()
                        .limit(MAX_ITEMS_TO_RETURN)
                        .collect(Collectors.toList());

                totalMatched = allMatchedItems.size();
                hasMore = totalMatched > selectedItems.size();

                if (selectedItems.isEmpty()) {
                    aiReply = (parsed.summary() == null || parsed.summary().isBlank())
                            ? "Không tìm thấy bất động sản phù hợp."
                            : parsed.summary().trim();
                } else if (hasMore) {
                    aiReply = "Tìm thấy " + totalMatched + " bất động sản phù hợp, mình hiển thị "
                            + selectedItems.size() + " bài phù hợp nhất trước.";
                } else {
                    aiReply = (parsed.summary() == null || parsed.summary().isBlank())
                            ? "Tìm thấy " + selectedItems.size() + " bất động sản phù hợp."
                            : parsed.summary().trim();
                }
            } else {
                List<PropertyCardDTO> fallbackItems = candidates.stream()
                        .map(this::toPropertyCard)
                        .collect(Collectors.toList());

                selectedItems = fallbackItems.stream()
                        .limit(MAX_ITEMS_TO_RETURN)
                        .collect(Collectors.toList());

                totalMatched = fallbackItems.size();
                hasMore = totalMatched > selectedItems.size();

                aiReply = selectedItems.isEmpty()
                        ? "Không tìm thấy bất động sản phù hợp."
                        : hasMore
                        ? "Tìm thấy " + totalMatched + " bất động sản phù hợp, mình hiển thị "
                        + selectedItems.size() + " bài phù hợp nhất trước."
                        : "Tìm thấy " + selectedItems.size() + " bất động sản phù hợp.";
            }

            chatMemoryService.saveMessage(
                    request.getUserId(),
                    request.getConversationId(),
                    "User",
                    request.getUserMessage()
            );

            chatMemoryService.saveMessage(
                    request.getUserId(),
                    request.getConversationId(),
                    "AI",
                    aiReply
            );

            if (!selectedItems.isEmpty()) {
                chatMemoryService.saveLastSearchContext(
                        request.getUserId(),
                        request.getConversationId(),
                        request.getUserMessage(),
                        selectedItems.stream()
                                .map(PropertyCardDTO::getPropertyId)
                                .filter(Objects::nonNull)
                                .toList(),
                        totalMatched,
                        hasMore
                );
            }

            AiChatResponse response = AiChatResponse.builder()
                    .userId(request.getUserId())
                    .conversationId(request.getConversationId())
                    .aiReply(aiReply)
                    .status("SUCCESS")
                    .items(selectedItems)
                    .totalMatched(totalMatched)
                    .hasMore(hasMore)
                    .build();

            aiResponseProducer.sendReply(response);

            log.info("Đã trả về {} items cho conversation {}",
                    selectedItems.size(),
                    request.getConversationId());

        } catch (Exception e) {
            log.error("Lỗi xử lý RAG: ", e);

            String aiReply = "Hệ thống đang bận, vui lòng thử lại.";

            chatMemoryService.saveMessage(
                    request.getUserId(),
                    request.getConversationId(),
                    "User",
                    request.getUserMessage()
            );
            chatMemoryService.saveMessage(
                    request.getUserId(),
                    request.getConversationId(),
                    "AI",
                    aiReply
            );

            aiResponseProducer.sendReply(AiChatResponse.builder()
                    .userId(request.getUserId())
                    .conversationId(request.getConversationId())
                    .aiReply(aiReply)
                    .status("ERROR")
                    .items(List.of())
                    .totalMatched(0)
                    .hasMore(false)
                    .build());
        }
    }

    private List<PropertyCardDTO> resolveAllSelectedItems(
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

    private boolean isFollowUpQuestion(String message) {
        if (message == null) return false;

        String text = message.toLowerCase().trim();

        boolean hasReferenceWord =
                text.contains("bài này")
                        || text.contains("căn này")
                        || text.contains("nhà này")
                        || text.contains("phòng này")
                        || text.contains("bài đầu")
                        || text.contains("bài đầu tiên")
                        || text.contains("bài thứ")
                        || text.contains("căn đầu")
                        || text.contains("căn thứ")
                        || text.contains("nó có")
                        || text.equals("nó")
                        || text.contains("xem thêm");

        boolean shortDetailQuestion =
                text.length() <= 80 && (
                        text.contains("có ban công")
                                || text.contains("mấy phòng ngủ")
                                || text.contains("bao nhiêu phòng ngủ")
                                || text.contains("mấy phòng tắm")
                                || text.contains("bao nhiêu phòng tắm")
                                || text.contains("diện tích")
                                || text.contains("nội thất")
                                || text.contains("giá bao nhiêu")
                                || text.contains("rẻ hơn")
                                || text.contains("đắt hơn")
                );

        boolean looksLikeNewSearch =
                text.contains("tìm ")
                        || text.contains("muốn thuê")
                        || text.contains("muốn mua")
                        || text.contains("cần thuê")
                        || text.contains("cần mua")
                        || text.contains("cho tôi")
                        || text.contains("ở hà nội")
                        || text.contains("ở tp hồ chí minh")
                        || text.contains("ở hồ chí minh")
                        || text.contains("quận ")
                        || text.contains("phường ");

        return hasReferenceWord || (shortDetailQuestion && !looksLikeNewSearch);
    }

    private void processFollowUpQuestion(AiChatRequest request, LastSearchContextDTO lastContext) {
        try {
            List<PropertyCandidateDTO> candidates = searchServiceClient
                    .getPropertiesByIds(lastContext.getLastPropertyIds())
                    .stream()
                    .map(this::mapSearchItemToCandidate)
                    .filter(item -> item.getPropertyId() != null)
                    .collect(Collectors.toMap(
                            PropertyCandidateDTO::getPropertyId,
                            item -> item,
                            (a, b) -> a,
                            LinkedHashMap::new
                    ))
                    .values()
                    .stream()
                    .toList();

            if (candidates.isEmpty()) {
                sendSimpleReply(
                        request,
                        "Mình chưa tìm thấy lại dữ liệu của các bài trước đó, bạn thử tìm kiếm lại giúp mình.",
                        "SUCCESS"
                );
                return;
            }

            String contextDocs = buildCompactContext(candidates);

            String chatHistory = chatMemoryService.getChatHistory(
                    request.getUserId(),
                    request.getConversationId()
            );

            String systemPrompt = String.format("""
                    Bạn là trợ lý bất động sản của HomeVerse.
                    
                    Người dùng đang hỏi tiếp dựa trên các bất động sản đã được gợi ý trước đó.
                    Chỉ trả lời dựa trên dữ liệu trong "Danh sách bài viết trước đó".
                    Không tự bịa thêm thông tin.
                    Nếu dữ liệu không có thông tin để trả lời, hãy nói rõ là chưa có thông tin.
                    
                    Cách hiểu tham chiếu:
                    - "bài này", "căn này", "nhà này", "phòng này", "nó" thường là bài đầu tiên trong danh sách gần nhất.
                    - "bài đầu tiên" là item đầu tiên.
                    - "bài thứ 2" là item thứ hai.
                    - "bài thứ 3" là item thứ ba.
                    - Nếu người dùng hỏi "có ban công không" thì kiểm tra hasBalcony.
                    - Nếu người dùng hỏi số phòng ngủ thì kiểm tra bedrooms.
                    - Nếu người dùng hỏi phòng tắm thì kiểm tra bathrooms.
                    - Nếu người dùng hỏi diện tích thì kiểm tra area.
                    - Nếu người dùng hỏi nội thất thì kiểm tra furnishingStatus.
                    - Nếu người dùng hỏi giá thì kiểm tra price.
                    
                    Chỉ trả về JSON hợp lệ:
                    {
                      "summary": "câu trả lời ngắn",
                      "selectedPropertyIds": [propertyId liên quan]
                    }
                    
                    Danh sách bài viết trước đó:
                    %s
                    
                    Lịch sử trò chuyện:
                    %s
                    """, contextDocs, chatHistory == null ? "" : chatHistory);

            String aiRaw = geminiChatService.callGemini25Flash(systemPrompt, request.getUserMessage());
            ParsedGeminiResult parsed = parseGeminiResult(aiRaw);

            List<PropertyCardDTO> relatedItems;

            if (parsed.validJson()) {
                relatedItems = resolveAllSelectedItems(
                        candidates,
                        parsed.selectedPropertyIds()
                ).stream()
                        .limit(MAX_ITEMS_TO_RETURN)
                        .toList();
            } else {
                relatedItems = candidates.stream()
                        .limit(1)
                        .map(this::toPropertyCard)
                        .toList();
            }

            String finalReply = parsed.validJson()
                    ? parsed.summary()
                    : "Mình chưa hiểu rõ câu hỏi tiếp theo, bạn hỏi lại cụ thể hơn nhé.";

            chatMemoryService.saveMessage(
                    request.getUserId(),
                    request.getConversationId(),
                    "User",
                    request.getUserMessage()
            );

            chatMemoryService.saveMessage(
                    request.getUserId(),
                    request.getConversationId(),
                    "AI",
                    finalReply
            );

            aiResponseProducer.sendReply(AiChatResponse.builder()
                    .userId(request.getUserId())
                    .conversationId(request.getConversationId())
                    .aiReply(finalReply)
                    .status("SUCCESS")
                    .items(relatedItems)
                    .totalMatched(relatedItems.size())
                    .hasMore(false)
                    .build());

        } catch (Exception e) {
            log.error("Lỗi xử lý follow-up question", e);

            sendSimpleReply(
                    request,
                    "Mình chưa xử lý được câu hỏi tiếp theo này, bạn thử hỏi lại rõ hơn nhé.",
                    "ERROR"
            );
        }
    }
    private PropertyCandidateDTO mapSearchItemToCandidate(SearchPropertyItemDTO item) {
        return PropertyCandidateDTO.builder()
                .propertyId(item.getId())
                .title(item.getTitle())
                .price(item.getPrice() == null ? null : item.getPrice().doubleValue())

                .province(item.getProvince())
                .district(item.getDistrict())
                .ward(item.getWard())
                .street(item.getStreet())
                .address(item.getAddress())

                .propertyType(item.getPropertyType())
                .transactionType(item.getTransactionType())
                .status("ACTIVE")

                .area(item.getArea())
                .bedrooms(item.getBedrooms())
                .bathrooms(item.getBathrooms())
                .capacity(item.getCapacity())
                .hasBalcony(item.getHasBalcony())

                .furnishingStatus(item.getFurnishingStatus())
                .availabilityStatus(item.getAvailabilityStatus())

                .electricityPrice(item.getElectricityPrice())
                .waterPrice(item.getWaterPrice())
                .internetPrice(item.getInternetPrice())

                .amenities(item.getAmenities() == null ? List.of() : item.getAmenities())

                .imageUrl(item.getThumbnail())
                .build();
    }
    private PropertyCandidateDTO mapToPropertyCandidate(Document doc) {
        Map<String, Object> meta = doc.getMetadata();

        return PropertyCandidateDTO.builder()
                .propertyId(asLong(meta.get("propertyId")))
                .title(asString(meta.get("title")))
                .price(asDouble(meta.get("price")))
                .pricePerSqm(asDouble(meta.get("pricePerSqm")))

                .province(asString(meta.get("province")))
                .district(asString(meta.get("district")))
                .ward(asString(meta.get("ward")))
                .street(asString(meta.get("street")))
                .address(asString(meta.get("address")))

                .propertyType(asString(meta.get("propertyType")))
                .transactionType(asString(meta.get("transactionType")))
                .status(asString(meta.get("status")))

                .area(asDouble(meta.get("area")))
                .bedrooms(asInteger(meta.get("bedrooms")))
                .bathrooms(asInteger(meta.get("bathrooms")))
                .capacity(asInteger(meta.get("capacity")))
                .hasBalcony(asBoolean(meta.get("hasBalcony")))

                .furnishingStatus(asString(meta.get("furnishingStatus")))
                .availabilityStatus(asString(meta.get("availabilityStatus")))
                .legalDocumentType(asString(meta.get("legalDocumentType")))

                .electricityPrice(asString(meta.get("electricityPrice")))
                .waterPrice(asString(meta.get("waterPrice")))
                .internetPrice(asString(meta.get("internetPrice")))

                .amenities(asStringList(meta.get("amenities")))

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
                        + " | pricePerSqm=" + item.getPricePerSqm()
                        + " | province=" + item.getProvince()
                        + " | district=" + item.getDistrict()
                        + " | ward=" + item.getWard()
                        + " | street=" + item.getStreet()
                        + " | propertyType=" + item.getPropertyType()
                        + " | transactionType=" + item.getTransactionType()
                        + " | status=" + item.getStatus()
                        + " | area=" + item.getArea()
                        + " | bedrooms=" + item.getBedrooms()
                        + " | bathrooms=" + item.getBathrooms()
                        + " | capacity=" + item.getCapacity()
                        + " | hasBalcony=" + item.getHasBalcony()
                        + " | furnishingStatus=" + item.getFurnishingStatus()
                        + " | availabilityStatus=" + item.getAvailabilityStatus()
                        + " | legalDocumentType=" + item.getLegalDocumentType()
                        + " | electricityPrice=" + item.getElectricityPrice()
                        + " | waterPrice=" + item.getWaterPrice()
                        + " | internetPrice=" + item.getInternetPrice()
                        + " | amenities=" + item.getAmenities())
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

    private Integer asInteger(Object value) {
        try {
            if (value == null) return null;
            if (value instanceof Number number) return number.intValue();
            return Integer.parseInt(String.valueOf(value));
        } catch (Exception e) {
            return null;
        }
    }

    private Boolean asBoolean(Object value) {
        if (value == null) return null;
        if (value instanceof Boolean bool) return bool;
        return Boolean.parseBoolean(String.valueOf(value));
    }

    @SuppressWarnings("unchecked")
    private List<String> asStringList(Object value) {
        if (value == null) return List.of();

        if (value instanceof List<?> list) {
            return list.stream()
                    .filter(item -> item != null)
                    .map(String::valueOf)
                    .toList();
        }

        return List.of(String.valueOf(value));
    }

    private void sendSimpleReply(AiChatRequest request, String aiReply, String status) {
        chatMemoryService.saveMessage(
                request.getUserId(),
                request.getConversationId(),
                "User",
                request.getUserMessage()
        );

        chatMemoryService.saveMessage(
                request.getUserId(),
                request.getConversationId(),
                "AI",
                aiReply
        );

        aiResponseProducer.sendReply(AiChatResponse.builder()
                .userId(request.getUserId())
                .conversationId(request.getConversationId())
                .aiReply(aiReply)
                .status(status)
                .items(List.of())
                .totalMatched(0)
                .hasMore(false)
                .build());
    }
}