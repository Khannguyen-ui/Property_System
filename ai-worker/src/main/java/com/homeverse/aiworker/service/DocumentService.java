package com.homeverse.aiworker.service;

import com.homeverse.aiworker.dto.request.PropertyEventDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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
                            - Giá điện: %s
                            - Giá nước: %s
                            - Phí Internet: %s
                            - Giấy tờ pháp lý: %s
                            
                            [TIỆN ÍCH]
                            - %s
                            
                            [MÔ TẢ TỪ CHỦ NHÀ]
                            %s
                            """,
                    event.getTitle(), event.getPropertyType(), event.getTransactionType(),
                    event.getStatus(), event.getAvailabilityStatus(),
                    event.getAddress(), event.getStreet(), event.getWard(), event.getDistrict(), event.getProvince(),
                    event.getArea(), event.getCapacity(),
                    event.getBedrooms(), event.getBathrooms(), balconyStr,
                    event.getFurnishingStatus(),
                    event.getPrice(), event.getPricePerSqm(),
                    event.getElectricityPrice(), event.getWaterPrice(), event.getInternetPrice(),
                    event.getLegalDocumentType(),
                    amenitiesStr,
                    event.getDescription()
            );

            Map<String, Object> metadata = new HashMap<>();
            metadata.put("propertyId", event.getPropertyId());
            metadata.put("title", event.getTitle());
            metadata.put("price", event.getPrice());
            metadata.put("province", event.getProvince());
            metadata.put("district", event.getDistrict());
            metadata.put("propertyType", event.getPropertyType());
            metadata.put("transactionType", event.getTransactionType());
            metadata.put("status", event.getStatus());
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
}