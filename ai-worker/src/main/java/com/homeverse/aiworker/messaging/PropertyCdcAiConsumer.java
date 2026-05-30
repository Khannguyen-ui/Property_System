package com.homeverse.aiworker.messaging;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.homeverse.aiworker.dto.request.PropertyEventDTO;
import com.homeverse.aiworker.service.DocumentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class PropertyCdcAiConsumer {

    private final ObjectMapper objectMapper;
    private final DocumentService documentService;

    @KafkaListener(topics = "homeverse_db.public.properties", groupId = "ai-worker-group")
    public void consumeCdcForAi(String message) {
        try {
            JsonNode rootNode = objectMapper.readTree(message);
            JsonNode payloadNode = rootNode.has("payload") ? rootNode.get("payload") : rootNode;

            if (payloadNode == null || payloadNode.isNull()) {
                return;
            }

            String op = payloadNode.path("op").asText("");
            JsonNode after = payloadNode.path("after");

            // 1. XỬ LÝ DELETE THẬT TỪ CDC
            if ("d".equals(op)) {
                Long propertyId = payloadNode.path("before").path("id").asLong();

                documentService.ingestNewProperty(
                        PropertyEventDTO.builder()
                                .propertyId(propertyId)
                                .eventType("DELETE")
                                .build()
                );

                return;
            }

            // 2. XỬ LÝ CREATE / UPDATE
            if (after == null || after.isMissingNode() || after.isNull()) {
                return;
            }

            Long propertyId = after.path("id").asLong();
            String status = after.path("status").asText(null);

            // 3. STATUS DELETED THÌ XÓA KHỎI VECTORSTORE
            if ("DELETED".equals(status)) {
                documentService.ingestNewProperty(
                        PropertyEventDTO.builder()
                                .propertyId(propertyId)
                                .eventType("DELETE")
                                .build()
                );

                return;
            }

            // 4. CHỈ CHO BÀI ĐÃ DUYỆT / ĐANG ACTIVE VÀO AI
            if (!"APPROVED".equals(status) && !"ACTIVE".equals(status)) {
                log.info("Bỏ qua và xóa khỏi AI VectorStore propertyId={} vì status={}", propertyId, status);

                documentService.ingestNewProperty(
                        PropertyEventDTO.builder()
                                .propertyId(propertyId)
                                .eventType("DELETE")
                                .build()
                );

                return;
            }

            // 5. PARSE IMAGES
            List<String> imagesList = parseStringList(after.path("images"));

            // 6. PARSE AMENITIES
            List<String> amenitiesList = parseStringList(after.path("amenities"));

            // 7. BUILD EVENT ĐỂ NHÚNG VÀO VECTORSTORE
            PropertyEventDTO event = PropertyEventDTO.builder()
                    .propertyId(propertyId)
                    .status(status)
                    .title(after.path("title").asText("Không có tiêu đề"))
                    .description(after.path("description").asText("Không có mô tả"))

                    // Địa chỉ
                    .address(after.path("address").asText(""))
                    .street(after.path("street").asText(""))
                    .ward(after.path("ward").asText(""))
                    .district(after.path("district").asText(""))
                    .province(after.path("province").asText(""))

                    // Giá & pháp lý
                    .price(after.path("price").asDouble(0.0))
                    .pricePerSqm(after.path("price_per_sqm").asDouble(0.0))
                    .legalDocumentType(after.path("legal_document_type").asText("Chưa rõ"))

                    // Cấu trúc & không gian
                    .area(after.path("area").asDouble(0.0))
                    .propertyType(after.path("property_type").asText("Phòng trọ"))
                    .transactionType(after.path("transaction_type").asText("Cho thuê"))
                    .bedrooms(after.path("bedrooms").asInt(0))
                    .bathrooms(after.path("bathrooms").asInt(0))
                    .hasBalcony(after.path("has_balcony").asBoolean(false))
                    .capacity(after.hasNonNull("capacity") ? after.path("capacity").asInt() : 0)

                    // Tình trạng & tiện ích
                    .furnishingStatus(after.path("furnishing_status").asText("Không rõ"))
                    .availabilityStatus(after.path("availability_status").asText("Đang trống"))
                    .amenities(amenitiesList)
                    .images(imagesList)

                    // Chi phí sinh hoạt
                    .electricityPrice(after.path("electricity_price").asText("Đang cập nhật"))
                    .waterPrice(after.path("water_price").asText("Đang cập nhật"))
                    .internetPrice(after.path("internet_price").asText("Đang cập nhật"))

                    .eventType("UPSERT")
                    .build();

            documentService.ingestNewProperty(event);

        } catch (Exception e) {
            log.error("Lỗi đồng bộ CDC lên AI VectorStore: {}", e.getMessage(), e);
        }
    }

    private List<String> parseStringList(JsonNode node) {
        try {
            List<String> result = new ArrayList<>();

            if (node == null || node.isMissingNode() || node.isNull()) {
                return result;
            }

            if (node.isArray()) {
                for (JsonNode item : node) {
                    if (!item.isNull()) {
                        result.add(item.asText());
                    }
                }

                return result;
            }

            if (node.isTextual()) {
                String raw = node.asText();

                if (raw == null || raw.isBlank()) {
                    return result;
                }

                return objectMapper.readValue(raw, new TypeReference<List<String>>() {});
            }

            return result;
        } catch (Exception e) {
            log.warn("Không parse được jsonb list: {}", node, e);
            return new ArrayList<>();
        }
    }
}