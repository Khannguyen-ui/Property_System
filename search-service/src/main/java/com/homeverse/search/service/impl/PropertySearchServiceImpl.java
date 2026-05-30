package com.homeverse.search.service.impl;

import com.homeverse.search.dto.request.PropertySearchRequestDTO;
import com.homeverse.search.dto.response.PropertySearchItemDTO;
import com.homeverse.search.document.PropertyDocument;
import com.homeverse.search.repository.PropertySearchRepository;
import com.homeverse.search.service.PropertySearchService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.elasticsearch.client.elc.NativeQuery;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import co.elastic.clients.elasticsearch._types.query_dsl.Operator;

import co.elastic.clients.elasticsearch._types.FieldValue;
import co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery;
import co.elastic.clients.json.JsonData;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PropertySearchServiceImpl implements PropertySearchService {

    private final PropertySearchRepository esRepository;
    private final ElasticsearchOperations elasticsearchOperations;

    private static final List<String> ALLOWED_SORT_FIELDS = List.of("createdAt", "price", "area", "bedrooms");

    @Override
    @Transactional(readOnly = true)
    public Page<PropertySearchItemDTO> advancedSearch(PropertySearchRequestDTO req) {

        int safeSize = Math.min(req.getSize() > 0 ? req.getSize() : 12, 100);
        int safePage = Math.max(req.getPage(), 0);

        Sort sort;
        if (req.getKeyword() != null && !req.getKeyword().trim().isEmpty()) {
            sort = Sort.by(Sort.Direction.DESC, "_score");
        } else {
            String reqSortBy = req.getSortBy() != null ? req.getSortBy() : "createdAt";
            String safeSortBy = ALLOWED_SORT_FIELDS.contains(reqSortBy) ? reqSortBy : "createdAt";
            Sort.Direction direction = "asc".equalsIgnoreCase(req.getSortDir()) ? Sort.Direction.ASC : Sort.Direction.DESC;
            sort = Sort.by(direction, safeSortBy);
        }

        NativeQuery query = NativeQuery.builder()
                .withQuery(q -> q.bool(b -> buildFilters(b, req)))
                .withPageable(PageRequest.of(safePage, safeSize))
                .withSort(sort)
                .build();

        SearchHits<PropertyDocument> searchHits = elasticsearchOperations.search(query, PropertyDocument.class);

        List<PropertySearchItemDTO> results = searchHits.getSearchHits().stream()
                .map(hit -> mapDocumentToItemDTO(hit.getContent()))
                .collect(Collectors.toList());

        return new PageImpl<>(results, PageRequest.of(safePage, safeSize), searchHits.getTotalHits());
    }

    private BoolQuery.Builder buildFilters(BoolQuery.Builder b, PropertySearchRequestDTO req) {

        // --- TÌM KIẾM CƠ BẢN ---
        if (req.getKeyword() != null && !req.getKeyword().trim().isEmpty()) {
            b.must(m -> m.multiMatch(mm -> mm
                    .fields("title^3", "description^2", "address", "province", "street", "ward", "district")
                    .query(req.getKeyword())
                    .operator(Operator.And)
                    .type(co.elastic.clients.elasticsearch._types.query_dsl.TextQueryType.CrossFields)
            ));
        }

        if (req.getMinPrice() != null || req.getMaxPrice() != null) {
            b.filter(f -> f.range(r -> {
                r.field("price");
                if (req.getMinPrice() != null) r.gte(JsonData.of(req.getMinPrice()));
                if (req.getMaxPrice() != null) r.lte(JsonData.of(req.getMaxPrice()));
                return r;
            }));
        }

        if (req.getMinArea() != null || req.getMaxArea() != null) {
            b.filter(f -> f.range(r -> {
                r.field("area");
                if (req.getMinArea() != null) r.gte(JsonData.of(req.getMinArea()));
                if (req.getMaxArea() != null) r.lte(JsonData.of(req.getMaxArea()));
                return r;
            }));
        }

        if (req.getPropertyTypes() != null && !req.getPropertyTypes().isEmpty()) {
            b.filter(f -> f.terms(t -> t.field("propertyType").terms(t2 -> t2.value(req.getPropertyTypes().stream().map(FieldValue::of).collect(Collectors.toList())))));
        }

        if (req.getTransactionTypes() != null && !req.getTransactionTypes().isEmpty()) {
            b.filter(f -> f.terms(t -> t.field("transactionType").terms(t2 -> t2.value(req.getTransactionTypes().stream().map(FieldValue::of).collect(Collectors.toList())))));
        }

        if (req.getAmenities() != null && !req.getAmenities().isEmpty()) {
            b.filter(f -> f.terms(t -> t.field("amenities").terms(t2 -> t2.value(req.getAmenities().stream().map(FieldValue::of).collect(Collectors.toList())))));
        }
        if (req.getProvince() != null && !req.getProvince().isEmpty()) {

            b.filter(f -> f.term(t -> t.field("province.keyword").value(req.getProvince())));
        }
        if (req.getDistrict() != null && !req.getDistrict().isEmpty()) {
            // 🟢 THÊM .keyword
            b.filter(f -> f.term(t -> t.field("district.keyword").value(req.getDistrict())));
        }
        if (req.getWard() != null && !req.getWard().isEmpty()) {
            // 🟢 THÊM .keyword
            b.filter(f -> f.term(t -> t.field("ward.keyword").value(req.getWard())));
        }
        if (req.getStreet() != null && !req.getStreet().isEmpty()) {
            // 🟢 THÊM .keyword
            b.filter(f -> f.term(t -> t.field("street.keyword").value(req.getStreet())));
        }
        // --- LỌC NÂNG CAO (ADVANCED FILTERS) ---
        if (req.getMinBedrooms() != null)
            b.filter(f -> f.range(r -> r.field("bedrooms").gte(JsonData.of(req.getMinBedrooms()))));
        if (req.getMinBathrooms() != null)
            b.filter(f -> f.range(r -> r.field("bathrooms").gte(JsonData.of(req.getMinBathrooms()))));
        if (req.getMinCapacity() != null)
            b.filter(f -> f.range(r -> r.field("capacity").gte(JsonData.of(req.getMinCapacity()))));
        if (req.getProjectId() != null) b.filter(f -> f.term(t -> t.field("projectId").value(req.getProjectId())));
        if (req.getHasBalcony() != null) b.filter(f -> f.term(t -> t.field("hasBalcony").value(req.getHasBalcony())));

        if (req.getFurnishingStatuses() != null && !req.getFurnishingStatuses().isEmpty()) {
            b.filter(f -> f.terms(t -> t.field("furnishingStatus").terms(t2 -> t2.value(req.getFurnishingStatuses().stream().map(FieldValue::of).collect(Collectors.toList())))));
        }
        if (req.getAvailabilityStatuses() != null && !req.getAvailabilityStatuses().isEmpty()) {
            b.filter(f -> f.terms(t -> t.field("availabilityStatus").terms(t2 -> t2.value(req.getAvailabilityStatuses().stream().map(FieldValue::of).collect(Collectors.toList())))));
        }
        if (req.getElectricityPrices() != null && !req.getElectricityPrices().isEmpty()) {
            b.filter(f -> f.terms(t -> t.field("electricityPrice").terms(t2 -> t2.value(req.getElectricityPrices().stream().map(FieldValue::of).collect(Collectors.toList())))));
        }
        if (req.getWaterPrices() != null && !req.getWaterPrices().isEmpty()) {
            b.filter(f -> f.terms(t -> t.field("waterPrice").terms(t2 -> t2.value(req.getWaterPrices().stream().map(FieldValue::of).collect(Collectors.toList())))));
        }
        if (req.getInternetPrices() != null && !req.getInternetPrices().isEmpty()) {
            b.filter(f -> f.terms(t -> t.field("internetPrice").terms(t2 -> t2.value(req.getInternetPrices().stream().map(FieldValue::of).collect(Collectors.toList())))));
        }

        // --- LỌC VỊ TRÍ GEO ---
        if (req.getLatitude() != null && req.getLongitude() != null) {
            // Nếu Frontend không gửi bán kính, mặc định là 5km
            int radius = (req.getRadiusKm() != null && req.getRadiusKm() > 0) ? req.getRadiusKm() : 5;

            b.filter(f -> f.geoDistance(g -> g.field("location")
                    .distance(radius + "km")
                    .location(l -> l.latlon(ll -> ll.lat(req.getLatitude()).lon(req.getLongitude())))));
        }
        if (req.getFilterMonth() != null && req.getFilterMonth().matches("\\d{2}/\\d{4}")) {
            String[] parts = req.getFilterMonth().split("/");
            int month = Integer.parseInt(parts[0]);
            int year = Integer.parseInt(parts[1]);

            // Lấy giây đầu tiên của tháng và giây cuối cùng của tháng
            java.time.LocalDateTime startOfMonth = java.time.LocalDateTime.of(year, month, 1, 0, 0, 0);
            java.time.LocalDateTime endOfMonth = startOfMonth.plusMonths(1).minusSeconds(1);

            // Format chuẩn theo cấu hình "uuuu-MM-dd'T'HH:mm:ss" của sếp trong Entity
            java.time.format.DateTimeFormatter formatter = java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

            b.filter(f -> f.range(r -> r
                    .field("createdAt")
                    .gte(JsonData.of(startOfMonth.format(formatter)))
                    .lte(JsonData.of(endOfMonth.format(formatter)))
            ));
        }
        // --- CHỈ LẤY TRẠNG THÁI ACTIVE ---
        b.filter(f -> f.term(t -> t.field("status").value("ACTIVE")));

        return b;
    }

    private PropertySearchItemDTO mapDocumentToItemDTO(PropertyDocument doc) {
        PropertySearchItemDTO dto = new PropertySearchItemDTO();
        dto.setId(doc.getId());
        dto.setPropertyType(doc.getPropertyType());
        dto.setTransactionType(doc.getTransactionType());
        dto.setTitle(doc.getTitle());
        dto.setPrice(doc.getPrice());
        dto.setArea(doc.getArea());
        dto.setAddress(doc.getAddress());
        dto.setProvince(doc.getProvince());
        dto.setStreet(doc.getStreet());
        dto.setWard(doc.getWard());
        dto.setDistrict(doc.getDistrict());
        dto.setBedrooms(doc.getBedrooms());
        dto.setBathrooms(doc.getBathrooms());

        // Map 2 trường mới hiển thị UI
        dto.setHasBalcony(doc.getHasBalcony());
        dto.setFurnishingStatus(doc.getFurnishingStatus());

        dto.setCreatedAt(doc.getCreatedAt());

        if (doc.getLocation() != null) {
            dto.setLatitude(doc.getLocation().getLat());
            dto.setLongitude(doc.getLocation().getLon());
        }

        if (doc.getImages() != null && !doc.getImages().isEmpty()) {
            dto.setThumbnail(doc.getImages().get(0));
        }

        return dto;
    }
}