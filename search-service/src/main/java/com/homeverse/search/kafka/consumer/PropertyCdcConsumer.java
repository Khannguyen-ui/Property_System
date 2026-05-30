package com.homeverse.search.kafka.consumer;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.homeverse.search.document.PropertyDocument;
import com.homeverse.search.repository.PropertySearchRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.elasticsearch.core.geo.GeoPoint;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class PropertyCdcConsumer {

    private final ObjectMapper objectMapper;
    private final PropertySearchRepository esRepository;

    @KafkaListener(topics = "homeverse_db.public.properties", groupId = "search-service-group")
    public void consumePropertyChanges(String message) {
        try {
            JsonNode rootNode = objectMapper.readTree(message);
            JsonNode payloadNode = rootNode.has("payload") ? rootNode.get("payload") : rootNode;

            if (payloadNode == null || payloadNode.isNull()) return;

            String op = payloadNode.path("op").asText("");
            JsonNode after = payloadNode.path("after");

            if ("d".equals(op)) {
                Long id = payloadNode.path("before").path("id").asLong();
                esRepository.deleteById(id);
                log.info("🗑CDC: Đã xóa cứng Property ID {} khỏi Elasticsearch", id);
                return;
            }

            if (after != null && !after.isMissingNode() && !after.isNull()) {
                Long propertyId = after.path("id").asLong();
                String status = after.path("status").asText(null);

                if ("DELETED".equals(status)) {
                    esRepository.deleteById(propertyId);
                    log.info("🗑️ CDC: Đã xóa mềm Property ID {} khỏi Elasticsearch", propertyId);
                    return;
                }

                PropertyDocument doc = new PropertyDocument();
                doc.setId(propertyId);
                doc.setStatus(status);

                doc.setTitle(after.path("title").asText(null));
                doc.setDescription(after.path("description").asText(null));
                doc.setAddress(after.path("address").asText(null));
                doc.setDistrict(after.path("district").asText(null));
                doc.setStreet(after.path("street").asText(null));
                doc.setWard(after.path("ward").asText(null));
                doc.setProvince(after.path("province").asText(null));

                if (!after.path("price").isMissingNode()) {
                    String priceStr = after.path("price").asText("0");
                    try {
                        doc.setPrice(new BigDecimal(priceStr));
                    } catch (NumberFormatException e) {
                        log.warn("Lỗi ép kiểu giá tiền từ Kafka: {}. Đặt giá trị mặc định là 0", priceStr);
                        doc.setPrice(BigDecimal.ZERO);
                    }
                }
                if (after.hasNonNull("price_per_sqm")) {
                    doc.setPricePerSqm(new BigDecimal(after.path("price_per_sqm").asText("0")));
                }

                if (after.hasNonNull("legal_document_type")) {
                    doc.setLegalDocumentType(after.path("legal_document_type").asText(null));
                }
                doc.setArea(after.path("area").asDouble(0.0));

                doc.setPropertyType(after.path("property_type").asText(null));
                doc.setTransactionType(after.path("transaction_type").asText(null));
                doc.setBedrooms(after.path("bedrooms").asInt(0));
                doc.setBathrooms(after.path("bathrooms").asInt(0));

                if (after.hasNonNull("has_balcony")) {
                    doc.setHasBalcony(after.path("has_balcony").asBoolean());
                }

                doc.setOwnerId(after.path("owner_id").asLong());

                // --- ĐỒNG BỘ CÁC TRƯỜNG LỌC NÂNG CAO MỚI ---
                if (after.hasNonNull("project_id")) doc.setProjectId(after.path("project_id").asLong());
                if (after.hasNonNull("capacity")) doc.setCapacity(after.path("capacity").asInt());
                doc.setFurnishingStatus(after.path("furnishing_status").asText(null));
                doc.setAvailabilityStatus(after.path("availability_status").asText(null));
                doc.setElectricityPrice(after.path("electricity_price").asText(null));
                doc.setWaterPrice(after.path("water_price").asText(null));
                doc.setInternetPrice(after.path("internet_price").asText(null));

                // --- TỌA ĐỘ GEOJSON ---
                if (after.hasNonNull("location")) {
                    JsonNode locationNode = after.path("location");
                    try {
                        // Trường hợp 1: Debezium gửi luôn Object
                        if (locationNode.isObject() && locationNode.hasNonNull("coordinates")) {
                            JsonNode coords = locationNode.get("coordinates");
                            if (coords.isArray() && coords.size() >= 2) {
                                double lon = coords.get(0).asDouble();
                                double lat = coords.get(1).asDouble();
                                doc.setLocation(new GeoPoint(lat, lon));
                            }
                        }
                        // Trường hợp 2: Debezium gửi chuỗi String
                        else if (locationNode.isTextual() && !locationNode.asText().trim().isEmpty()) {
                            JsonNode geoJsonNode = objectMapper.readTree(locationNode.asText());
                            if (geoJsonNode.hasNonNull("coordinates")) {
                                JsonNode coords = geoJsonNode.get("coordinates");
                                if (coords.isArray() && coords.size() >= 2) {
                                    double lon = coords.get(0).asDouble();
                                    double lat = coords.get(1).asDouble();
                                    doc.setLocation(new GeoPoint(lat, lon));
                                }
                            }
                        }
                    } catch (Exception e) {
                        log.warn("Lỗi parse tọa độ GeoJSON (bỏ qua tọa độ cho Property ID {}): {}", propertyId, e.getMessage());
                    }
                }

                if (after.hasNonNull("amenities")) {
                    doc.setAmenities(objectMapper.readValue(after.path("amenities").asText("[]"), new TypeReference<List<String>>() {}));
                }
                if (after.hasNonNull("images")) {
                    doc.setImages(objectMapper.readValue(after.path("images").asText("[]"), new TypeReference<List<String>>() {}));
                }

                if (after.hasNonNull("created_at")) {
                    long timestampMicro = after.path("created_at").asLong();

                    Instant instant = Instant.ofEpochSecond(timestampMicro / 1000000, (timestampMicro % 1000000) * 1000);
                    doc.setCreatedAt(LocalDateTime.ofInstant(instant, ZoneId.of("UTC")));
                }
                esRepository.save(doc);
                log.info(" CDC: Đã UPSERT Property ID {} lên Elasticsearch thành công!", doc.getId());
            }

        } catch (Exception e) {
            log.error(" Lỗi đồng bộ CDC lên Elasticsearch: {}", e.getMessage(), e);
        }
    }
}