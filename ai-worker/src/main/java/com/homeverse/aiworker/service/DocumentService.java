package com.homeverse.aiworker.service;

import com.homeverse.aiworker.dto.request.PropertyEventDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Set;
import java.util.LinkedHashSet;
import java.util.stream.Collectors;
import java.util.List;
import java.util.Map;


@Slf4j
@Service
@RequiredArgsConstructor
public class DocumentService {

    private final VectorStore vectorStore;
    private final GeminiEmbeddingService geminiEmbeddingService;

    public String findRelevantProperties(String userQuery) {
        try {
            List<Document> results = vectorStore.similaritySearch(
                    SearchRequest.query(userQuery)
                            .withTopK(15)
            );

            if (results.isEmpty()) {
                return "Hiện tại không tìm thấy phòng trọ nào phù hợp với yêu cầu này trong cơ sở dữ liệu.";
            }

            return results.stream()
                    .map(Document::getContent)
                    .collect(Collectors.joining("\n---\n"));

        } catch (Exception e) {
            log.error("Lỗi khi tìm kiếm Vector: ", e);
            return "Hệ thống tìm kiếm phòng trọ đang gián đoạn.";
        }
    }

    public void ingestNewProperty(PropertyEventDTO event) {
        if (event == null || event.getPropertyId() == null) {
            log.warn("Bỏ qua event vì event hoặc propertyId null");
            return;
        }

        String documentId = buildDocumentId(event.getPropertyId());

        if ("DELETE".equals(event.getEventType())) {
            try {
                vectorStore.delete(List.of(documentId));

                log.info("Đã XÓA phòng trọ ID {} khỏi AI Vector Store với documentId={}",
                        event.getPropertyId(), documentId);

            } catch (Exception e) {
                log.error("Lỗi khi xóa phòng trọ ID {} khỏi AI Vector Store: {}",
                        event.getPropertyId(), e.getMessage(), e);
            }

            return;
        }

        try {
            String amenitiesStr = (event.getAmenities() != null && !event.getAmenities().isEmpty())
                    ? String.join(", ", event.getAmenities())
                    : "Không có thông tin tiện ích nổi bật";

            String balconyStr = Boolean.TRUE.equals(event.getHasBalcony())
                    ? "Có ban công"
                    : "Không có ban công";

            String imageUrl = extractFirstImageUrl(event.getImages());

            String content = String.format("""
                [THÔNG TIN CƠ BẢN]
                - Tiêu đề: %s
                - Loại hình: %s (%s)
                - Trạng thái: %s | Tình trạng phòng: %s
                
                [VỊ TRÍ]
                - Địa chỉ cụ thể: %s
                - Khu vực: Đường %s, Phường %s, Quận/Huyện %s, Tỉnh/TP %s
                
                [KHÔNG GIAN & KIẾN TRÚC]
                - Diện tích: %s m2 (Sức chứa tối đa: %d người)
                - Thiết kế: %d phòng ngủ, %d phòng tắm. %s.
                - Tình trạng nội thất: %s
                
                [CHI PHÍ & PHÁP LÝ]
                - Giá: %,.0f VNĐ
                - Giá mỗi m2: %,.0f VNĐ/m2
                - Giá điện: %s
                - Giá nước: %s
                - Phí Internet: %s
                - Giấy tờ pháp lý: %s
                
                [TIỆN ÍCH]
                - %s
                
                [MÔ TẢ TỪ CHỦ NHÀ]
                %s
                """,
                    safeText(event.getTitle()),
                    normalizeEnumText(event.getPropertyType()),
                    normalizeEnumText(event.getTransactionType()),
                    normalizeEnumText(event.getStatus()),
                    normalizeEnumText(event.getAvailabilityStatus()),

                    safeText(event.getAddress()),
                    safeText(event.getStreet()),
                    safeText(event.getWard()),
                    normalizeLocation(event.getDistrict()),
                    normalizeLocation(event.getProvince()),

                    event.getArea() == null ? 0.0 : event.getArea(),
                    event.getCapacity() == null ? 0 : event.getCapacity(),
                    event.getBedrooms() == null ? 0 : event.getBedrooms(),
                    event.getBathrooms() == null ? 0 : event.getBathrooms(),
                    balconyStr,
                    normalizeEnumText(event.getFurnishingStatus()),

                    event.getPrice() == null ? 0.0 : event.getPrice(),
                    event.getPricePerSqm() == null ? 0.0 : event.getPricePerSqm(),
                    normalizeEnumText(event.getElectricityPrice()),
                    normalizeEnumText(event.getWaterPrice()),
                    normalizeEnumText(event.getInternetPrice()),
                    normalizeEnumText(event.getLegalDocumentType()),

                    amenitiesStr,
                    safeText(event.getDescription())
            );

            Map<String, Object> metadata = new HashMap<>();

            metadata.put("propertyId", event.getPropertyId());
            metadata.put("title", safeText(event.getTitle()));
            metadata.put("price", event.getPrice() == null ? 0.0 : event.getPrice());
            metadata.put("pricePerSqm", event.getPricePerSqm() == null ? 0.0 : event.getPricePerSqm());

            metadata.put("province", normalizeLocation(event.getProvince()));
            metadata.put("district", normalizeLocation(event.getDistrict()));
            metadata.put("ward", normalizeLocation(event.getWard()));
            metadata.put("street", normalizeLocation(event.getStreet()));
            metadata.put("address", safeText(event.getAddress()));

            metadata.put("propertyType", normalizeEnumText(event.getPropertyType()));
            metadata.put("transactionType", normalizeEnumText(event.getTransactionType()));
            metadata.put("status", normalizeEnumText(event.getStatus()));

            metadata.put("area", event.getArea() == null ? 0.0 : event.getArea());
            metadata.put("bedrooms", event.getBedrooms() == null ? 0 : event.getBedrooms());
            metadata.put("bathrooms", event.getBathrooms() == null ? 0 : event.getBathrooms());
            metadata.put("capacity", event.getCapacity() == null ? 0 : event.getCapacity());
            metadata.put("hasBalcony", Boolean.TRUE.equals(event.getHasBalcony()));

            metadata.put("furnishingStatus", normalizeEnumText(event.getFurnishingStatus()));
            metadata.put("availabilityStatus", normalizeEnumText(event.getAvailabilityStatus()));
            metadata.put("legalDocumentType", normalizeEnumText(event.getLegalDocumentType()));

            metadata.put("electricityPrice", normalizeEnumText(event.getElectricityPrice()));
            metadata.put("waterPrice", normalizeEnumText(event.getWaterPrice()));
            metadata.put("internetPrice", normalizeEnumText(event.getInternetPrice()));

            metadata.put("amenities", event.getAmenities() == null ? List.of() : event.getAmenities());
            metadata.put("imageUrl", imageUrl);

            List<Double> customVector = geminiEmbeddingService.embedText(content);

            Document doc = new Document(documentId, content, metadata);
            doc.setEmbedding(customVector);

            vectorStore.add(List.of(doc));

            log.info("Đã UPSERT bất động sản ID{} vào AI Vector Store với documentId={}, imageUrl={}",
                    event.getPropertyId(), documentId, imageUrl);

        } catch (Exception e) {
            log.error("Lỗi khi nhúng Vector Full Data cho propertyId={}: {}",
                    event.getPropertyId(), e.getMessage(), e);
        }
    }

    public List<Document> findRelevantPropertyDocuments(String userQuery) {
        try {
            return vectorStore.similaritySearch(
                    SearchRequest.query(userQuery)
                            .withTopK(15)
            );
        } catch (Exception e) {
            log.error("Lỗi khi tìm kiếm Vector: ", e);
            return List.of();
        }
    }

    private String buildDocumentId(Long propertyId) {
        return "property-" + propertyId;
    }

    private String extractFirstImageUrl(List<String> images) {
        if (images == null || images.isEmpty()) {
            return null;
        }

        return images.stream()
                .filter(url -> url != null && !url.isBlank())
                .findFirst()
                .orElse(null);
    }
    public List<Document> findPropertyDocumentsByIds(List<Long> propertyIds) {
        if (propertyIds == null || propertyIds.isEmpty()) {
            return List.of();
        }

        try {
            String query = propertyIds.stream()
                    .map(String::valueOf)
                    .collect(Collectors.joining(" "));

            List<Document> results = vectorStore.similaritySearch(
                    SearchRequest.query(query)
                            .withTopK(Math.max(propertyIds.size() * 3, 10))
            );

            Set<Long> idSet = new LinkedHashSet<>(propertyIds);

            return results.stream()
                    .filter(doc -> {
                        Object id = doc.getMetadata().get("propertyId");
                        if (id == null) return false;

                        try {
                            Long propertyId = id instanceof Number number
                                    ? number.longValue()
                                    : Long.parseLong(String.valueOf(id));

                            return idSet.contains(propertyId);
                        } catch (Exception e) {
                            return false;
                        }
                    })
                    .collect(Collectors.toMap(
                            doc -> {
                                Object id = doc.getMetadata().get("propertyId");
                                return id instanceof Number number
                                        ? number.longValue()
                                        : Long.parseLong(String.valueOf(id));
                            },
                            doc -> doc,
                            (a, b) -> a,
                            LinkedHashMap::new
                    ))
                    .values()
                    .stream()
                    .toList();

        } catch (Exception e) {
            log.error("Lỗi khi tìm property documents theo ids={}", propertyIds, e);
            return List.of();
        }
    }
    private String safeText(String value) {
        return value == null ? "" : value.trim();
    }

    private String normalizeLocation(String value) {
        if (value == null) {
            return "";
        }

        return value.trim()
                .replaceAll("\\s+", " ");
    }

    private String normalizeEnumText(String value) {
        if (value == null) {
            return "";
        }

        return value.trim().toUpperCase();
    }
}