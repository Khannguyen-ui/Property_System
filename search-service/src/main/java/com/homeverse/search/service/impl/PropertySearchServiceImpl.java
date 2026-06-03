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

import co.elastic.clients.elasticsearch._types.FieldValue;
import co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.Operator;
import co.elastic.clients.json.JsonData;

import java.math.BigDecimal;
import java.text.Normalizer;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PropertySearchServiceImpl implements PropertySearchService {

    private final PropertySearchRepository esRepository;
    private final ElasticsearchOperations elasticsearchOperations;

    private static final List<String> ALLOWED_SORT_FIELDS =
            List.of("createdAt", "price", "area", "bedrooms");

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
            Sort.Direction direction =
                    "asc".equalsIgnoreCase(req.getSortDir()) ? Sort.Direction.ASC : Sort.Direction.DESC;
            sort = Sort.by(direction, safeSortBy);
        }

        NativeQuery query = NativeQuery.builder()
                .withQuery(q -> q.bool(b -> buildFilters(b, req)))
                .withPageable(PageRequest.of(safePage, safeSize))
                .withSort(sort)
                .build();

        SearchHits<PropertyDocument> searchHits =
                elasticsearchOperations.search(query, PropertyDocument.class);

        List<PropertySearchItemDTO> results = searchHits.getSearchHits().stream()
                .map(hit -> mapDocumentToItemDTO(hit.getContent()))
                .collect(Collectors.toList());

        return new PageImpl<>(results, PageRequest.of(safePage, safeSize), searchHits.getTotalHits());
    }

    private BoolQuery.Builder buildFilters(BoolQuery.Builder b, PropertySearchRequestDTO req) {

        // --- TÌM KIẾM TỰ NHIÊN ---
        if (req.getKeyword() != null && !req.getKeyword().trim().isEmpty()) {
            String keyword = req.getKeyword().trim();

            String remainingKeyword = applyKeywordIntentFilters(b, keyword, req);

            if (!remainingKeyword.isBlank()) {
                b.must(m -> m.multiMatch(mm -> mm
                        .fields(
                                "title^4",
                                "description^2",
                                "address^2",
                                "province",
                                "district^2",
                                "ward",
                                "street"
                        )
                        .query(remainingKeyword)
                        .operator(Operator.Or)
                        .type(co.elastic.clients.elasticsearch._types.query_dsl.TextQueryType.CrossFields)
                ));
            }
        }

        // --- FILTER GIÁ ---
        if (req.getMinPrice() != null || req.getMaxPrice() != null) {
            b.filter(f -> f.range(r -> {
                r.field("price");
                if (req.getMinPrice() != null) r.gte(JsonData.of(req.getMinPrice()));
                if (req.getMaxPrice() != null) r.lte(JsonData.of(req.getMaxPrice()));
                return r;
            }));
        }

        // --- FILTER DIỆN TÍCH ---
        if (req.getMinArea() != null || req.getMaxArea() != null) {
            b.filter(f -> f.range(r -> {
                r.field("area");
                if (req.getMinArea() != null) r.gte(JsonData.of(req.getMinArea()));
                if (req.getMaxArea() != null) r.lte(JsonData.of(req.getMaxArea()));
                return r;
            }));
        }

        if (req.getPropertyTypes() != null && !req.getPropertyTypes().isEmpty()) {
            b.filter(f -> f.terms(t -> t
                    .field("propertyType")
                    .terms(t2 -> t2.value(req.getPropertyTypes().stream()
                            .map(FieldValue::of)
                            .collect(Collectors.toList())))
            ));
        }

        if (req.getTransactionTypes() != null && !req.getTransactionTypes().isEmpty()) {
            b.filter(f -> f.terms(t -> t
                    .field("transactionType")
                    .terms(t2 -> t2.value(req.getTransactionTypes().stream()
                            .map(FieldValue::of)
                            .collect(Collectors.toList())))
            ));
        }

        if (req.getAmenities() != null && !req.getAmenities().isEmpty()) {
            b.filter(f -> f.terms(t -> t
                    .field("amenities")
                    .terms(t2 -> t2.value(req.getAmenities().stream()
                            .map(FieldValue::of)
                            .collect(Collectors.toList())))
            ));
        }

        if (req.getProvince() != null && !req.getProvince().isBlank()) {
            b.filter(f -> f.match(m -> m
                    .field("province")
                    .query(req.getProvince())
                    .operator(Operator.And)
            ));
        }

        if (req.getDistrict() != null && !req.getDistrict().isBlank()) {
            b.filter(f -> f.match(m -> m
                    .field("district")
                    .query(req.getDistrict())
                    .operator(Operator.And)
            ));
        }

        if (req.getWard() != null && !req.getWard().isBlank()) {
            b.filter(f -> f.match(m -> m
                    .field("ward")
                    .query(req.getWard())
                    .operator(Operator.And)
            ));
        }

        if (req.getStreet() != null && !req.getStreet().isBlank()) {
            b.filter(f -> f.match(m -> m
                    .field("street")
                    .query(req.getStreet())
                    .operator(Operator.And)
            ));
        }

        // --- LỌC NÂNG CAO ---
        if (req.getMinBedrooms() != null) {
            b.filter(f -> f.range(r -> r.field("bedrooms").gte(JsonData.of(req.getMinBedrooms()))));
        }

        if (req.getMinBathrooms() != null) {
            b.filter(f -> f.range(r -> r.field("bathrooms").gte(JsonData.of(req.getMinBathrooms()))));
        }

        if (req.getMinCapacity() != null) {
            b.filter(f -> f.range(r -> r.field("capacity").gte(JsonData.of(req.getMinCapacity()))));
        }

        if (req.getProjectId() != null) {
            b.filter(f -> f.term(t -> t.field("projectId").value(req.getProjectId())));
        }

        if (req.getHasBalcony() != null) {
            b.filter(f -> f.term(t -> t.field("hasBalcony").value(req.getHasBalcony())));
        }

        if (req.getFurnishingStatuses() != null && !req.getFurnishingStatuses().isEmpty()) {
            b.filter(f -> f.terms(t -> t
                    .field("furnishingStatus")
                    .terms(t2 -> t2.value(req.getFurnishingStatuses().stream()
                            .map(FieldValue::of)
                            .collect(Collectors.toList())))
            ));
        }

        if (req.getAvailabilityStatuses() != null && !req.getAvailabilityStatuses().isEmpty()) {
            b.filter(f -> f.terms(t -> t
                    .field("availabilityStatus")
                    .terms(t2 -> t2.value(req.getAvailabilityStatuses().stream()
                            .map(FieldValue::of)
                            .collect(Collectors.toList())))
            ));
        }

        if (req.getElectricityPrices() != null && !req.getElectricityPrices().isEmpty()) {
            b.filter(f -> f.terms(t -> t
                    .field("electricityPrice")
                    .terms(t2 -> t2.value(req.getElectricityPrices().stream()
                            .map(FieldValue::of)
                            .collect(Collectors.toList())))
            ));
        }

        if (req.getWaterPrices() != null && !req.getWaterPrices().isEmpty()) {
            b.filter(f -> f.terms(t -> t
                    .field("waterPrice")
                    .terms(t2 -> t2.value(req.getWaterPrices().stream()
                            .map(FieldValue::of)
                            .collect(Collectors.toList())))
            ));
        }

        if (req.getInternetPrices() != null && !req.getInternetPrices().isEmpty()) {
            b.filter(f -> f.terms(t -> t
                    .field("internetPrice")
                    .terms(t2 -> t2.value(req.getInternetPrices().stream()
                            .map(FieldValue::of)
                            .collect(Collectors.toList())))
            ));
        }

        // --- LỌC VỊ TRÍ GEO ---
        if (req.getLatitude() != null && req.getLongitude() != null) {
            int radius = (req.getRadiusKm() != null && req.getRadiusKm() > 0)
                    ? req.getRadiusKm()
                    : 5;

            b.filter(f -> f.geoDistance(g -> g
                    .field("location")
                    .distance(radius + "km")
                    .location(l -> l.latlon(ll -> ll
                            .lat(req.getLatitude())
                            .lon(req.getLongitude())))
            ));
        }

        if (req.getFilterMonth() != null && req.getFilterMonth().matches("\\d{2}/\\d{4}")) {
            String[] parts = req.getFilterMonth().split("/");
            int month = Integer.parseInt(parts[0]);
            int year = Integer.parseInt(parts[1]);

            java.time.LocalDateTime startOfMonth =
                    java.time.LocalDateTime.of(year, month, 1, 0, 0, 0);
            java.time.LocalDateTime endOfMonth =
                    startOfMonth.plusMonths(1).minusSeconds(1);

            java.time.format.DateTimeFormatter formatter =
                    java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

            b.filter(f -> f.range(r -> r
                    .field("createdAt")
                    .gte(JsonData.of(startOfMonth.format(formatter)))
                    .lte(JsonData.of(endOfMonth.format(formatter)))
            ));
        }

        // --- CHỈ LẤY BÀI ACTIVE ---
        b.filter(f -> f.term(t -> t.field("status").value("ACTIVE")));

        return b;
    }

    @Override
    @Transactional(readOnly = true)
    public List<PropertySearchItemDTO> findByIds(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }

        NativeQuery query = NativeQuery.builder()
                .withQuery(q -> q.bool(b -> b
                        .filter(f -> f.terms(t -> t
                                .field("id")
                                .terms(v -> v.value(ids.stream()
                                        .map(FieldValue::of)
                                        .collect(Collectors.toList())))
                        ))
                        .filter(f -> f.term(t -> t.field("status").value("ACTIVE")))
                ))
                .withPageable(PageRequest.of(0, ids.size()))
                .build();

        SearchHits<PropertyDocument> searchHits =
                elasticsearchOperations.search(query, PropertyDocument.class);

        Map<Long, PropertyDocument> docMap = searchHits.getSearchHits().stream()
                .map(hit -> hit.getContent())
                .filter(doc -> doc.getId() != null)
                .collect(Collectors.toMap(
                        PropertyDocument::getId,
                        doc -> doc,
                        (a, b) -> a
                ));

        return ids.stream()
                .map(docMap::get)
                .filter(Objects::nonNull)
                .map(this::mapDocumentToItemDTO)
                .collect(Collectors.toList());
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
        dto.setHasBalcony(doc.getHasBalcony());
        dto.setFurnishingStatus(doc.getFurnishingStatus());
        dto.setCapacity(doc.getCapacity());
        dto.setAvailabilityStatus(doc.getAvailabilityStatus());
        dto.setElectricityPrice(doc.getElectricityPrice());
        dto.setWaterPrice(doc.getWaterPrice());
        dto.setInternetPrice(doc.getInternetPrice());
        dto.setAmenities(doc.getAmenities());
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

    // =========================================================
    // NATURAL LANGUAGE SEARCH HELPERS
    // =========================================================

    private String applyKeywordIntentFilters(
            BoolQuery.Builder b,
            String keyword,
            PropertySearchRequestDTO req
    ) {
        String kw = normalizeKeyword(keyword);
        if (kw.isBlank()) return "";

        String remaining = kw;

        boolean hasExplicitPropertyType =
                req.getPropertyTypes() != null && !req.getPropertyTypes().isEmpty();

        boolean hasExplicitTransactionType =
                req.getTransactionTypes() != null && !req.getTransactionTypes().isEmpty();

        boolean hasExplicitFurnishing =
                req.getFurnishingStatuses() != null && !req.getFurnishingStatuses().isEmpty();

        boolean hasExplicitAvailability =
                req.getAvailabilityStatuses() != null && !req.getAvailabilityStatuses().isEmpty();

        // 1. PROPERTY TYPE
        if (!hasExplicitPropertyType) {
            if (
                    containsPhrase(kw, "phong tro")
                            || containsPhrase(kw, "nha tro")
                            || containsAny(kw, "phong", "tro", "room")
            ) {
                b.filter(f -> f.term(t -> t.field("propertyType").value("ROOM")));
                remaining = removeTerms(remaining, "phong tro", "nha tro", "phong", "tro", "room");
            } else if (
                    containsPhrase(kw, "can ho chung cu")
                            || containsPhrase(kw, "can ho")
                            || containsPhrase(kw, "chung cu")
                            || containsAny(kw, "apartment")
            ) {
                b.filter(f -> f.term(t -> t.field("propertyType").value("APARTMENT")));
                remaining = removeTerms(remaining, "can ho chung cu", "can ho", "chung cu", "apartment");
            } else if (
                    containsPhrase(kw, "nha nguyen can")
                            || containsPhrase(kw, "nha rieng")
                            || containsPhrase(kw, "nha pho")
                            || containsAny(kw, "house")
            ) {
                b.filter(f -> f.term(t -> t.field("propertyType").value("HOUSE")));
                remaining = removeTerms(remaining, "nha nguyen can", "nha rieng", "nha pho", "house");
            } else if (
                    containsPhrase(kw, "biet thu")
                            || containsAny(kw, "villa")
            ) {
                b.filter(f -> f.term(t -> t.field("propertyType").value("VILLA")));
                remaining = removeTerms(remaining, "biet thu", "villa");
            } else if (
                    containsPhrase(kw, "mat bang kinh doanh")
                            || containsPhrase(kw, "mat bang")
                            || containsPhrase(kw, "kinh doanh")
                            || containsPhrase(kw, "thuong mai")
                            || containsAny(kw, "commercial")
            ) {
                b.filter(f -> f.term(t -> t.field("propertyType").value("COMMERCIAL")));
                remaining = removeTerms(
                        remaining,
                        "mat bang kinh doanh",
                        "mat bang",
                        "kinh doanh",
                        "thuong mai",
                        "commercial"
                );
            }
        }

        // 2. TRANSACTION TYPE
        if (!hasExplicitTransactionType) {
            if (
                    containsPhrase(kw, "cho thue")
                            || containsPhrase(kw, "can thue")
                            || containsPhrase(kw, "muon thue")
                            || containsAny(kw, "thue", "rent")
            ) {
                b.filter(f -> f.term(t -> t.field("transactionType").value("FOR_RENT")));
                remaining = removeTerms(remaining, "cho thue", "can thue", "muon thue", "thue", "rent");
            } else if (
                    containsPhrase(kw, "can ban")
                            || containsPhrase(kw, "rao ban")
                            || containsPhrase(kw, "mua nha")
                            || containsPhrase(kw, "mua can ho")
                            || containsAny(kw, "sale")
            ) {
                b.filter(f -> f.term(t -> t.field("transactionType").value("FOR_SALE")));
                remaining = removeTerms(remaining, "can ban", "rao ban", "mua nha", "mua can ho", "sale");
            }
        }

        // 3. FURNISHING STATUS
        if (!hasExplicitFurnishing) {
            if (
                    containsPhrase(kw, "day du noi that")
                            || containsPhrase(kw, "noi that day du")
                            || containsPhrase(kw, "full noi that")
                            || containsAny(kw, "full")
            ) {
                b.filter(f -> f.term(t -> t.field("furnishingStatus").value("FULLY_FURNISHED")));
                remaining = removeTerms(remaining, "day du noi that", "noi that day du", "full noi that", "full");
            } else if (
                    containsPhrase(kw, "noi that co ban")
                            || containsPhrase(kw, "co ban")
                            || containsPhrase(kw, "ban noi that")
            ) {
                b.filter(f -> f.term(t -> t.field("furnishingStatus").value("PARTIALLY_FURNISHED")));
                remaining = removeTerms(remaining, "noi that co ban", "co ban", "ban noi that");
            } else if (
                    containsPhrase(kw, "nha trong")
                            || containsPhrase(kw, "khong noi that")
                            || containsPhrase(kw, "trong")
            ) {
                b.filter(f -> f.term(t -> t.field("furnishingStatus").value("UNFURNISHED")));
                remaining = removeTerms(remaining, "nha trong", "khong noi that", "trong");
            }
        }

        // 4. AVAILABILITY STATUS
        if (!hasExplicitAvailability) {
            if (
                    containsPhrase(kw, "vao o ngay")
                            || containsPhrase(kw, "o ngay")
                            || containsPhrase(kw, "co san")
                            || containsPhrase(kw, "san phong")
            ) {
                b.filter(f -> f.term(t -> t.field("availabilityStatus").value("IMMEDIATELY")));
                remaining = removeTerms(remaining, "vao o ngay", "o ngay", "co san", "san phong");
            } else if (
                    containsPhrase(kw, "trong thang")
                            || containsPhrase(kw, "thang nay")
            ) {
                b.filter(f -> f.term(t -> t.field("availabilityStatus").value("THIS_MONTH")));
                remaining = removeTerms(remaining, "trong thang", "thang nay");
            } else if (
                    containsPhrase(kw, "dau thang sau")
                            || containsPhrase(kw, "thang sau")
            ) {
                b.filter(f -> f.term(t -> t.field("availabilityStatus").value("NEXT_MONTH")));
                remaining = removeTerms(remaining, "dau thang sau", "thang sau");
            } else if (
                    containsPhrase(kw, "thoa thuan")
                            || containsPhrase(kw, "linh hoat")
            ) {
                b.filter(f -> f.term(t -> t.field("availabilityStatus").value("NEGOTIABLE")));
                remaining = removeTerms(remaining, "thoa thuan", "linh hoat");
            }
        }

        // 5. BALCONY
        if (req.getHasBalcony() == null) {
            if (
                    containsPhrase(kw, "khong co ban cong")
                            || containsPhrase(kw, "khong ban cong")
            ) {
                b.filter(f -> f.term(t -> t.field("hasBalcony").value(false)));
                remaining = removeTerms(remaining, "khong co ban cong", "khong ban cong");
            } else if (
                    containsPhrase(kw, "co ban cong")
                            || containsPhrase(kw, "ban cong")
                            || containsAny(kw, "balcony")
            ) {
                b.filter(f -> f.term(t -> t.field("hasBalcony").value(true)));
                remaining = removeTerms(remaining, "co ban cong", "ban cong", "balcony");
            }
        }

        // 6. UTILITY PRICE
        if (containsPhrase(kw, "mien phi dien") || containsPhrase(kw, "dien mien phi")) {
            b.filter(f -> f.term(t -> t.field("electricityPrice").value("FREE")));
            remaining = removeTerms(remaining, "mien phi dien", "dien mien phi");
        }

        if (containsPhrase(kw, "mien phi nuoc") || containsPhrase(kw, "nuoc mien phi")) {
            b.filter(f -> f.term(t -> t.field("waterPrice").value("FREE")));
            remaining = removeTerms(remaining, "mien phi nuoc", "nuoc mien phi");
        }

        if (
                containsPhrase(kw, "mien phi internet")
                        || containsPhrase(kw, "internet mien phi")
                        || containsPhrase(kw, "wifi mien phi")
        ) {
            b.filter(f -> f.term(t -> t.field("internetPrice").value("FREE")));
            remaining = removeTerms(remaining, "mien phi internet", "internet mien phi", "wifi mien phi");
        }

        if (containsPhrase(kw, "dien nha nuoc")) {
            b.filter(f -> f.term(t -> t.field("electricityPrice").value("STATE_PRICE")));
            remaining = removeTerms(remaining, "dien nha nuoc");
        }

        if (containsPhrase(kw, "nuoc nha nuoc")) {
            b.filter(f -> f.term(t -> t.field("waterPrice").value("STATE_PRICE")));
            remaining = removeTerms(remaining, "nuoc nha nuoc");
        }

        if (containsPhrase(kw, "gia nha nuoc")) {
            b.filter(f -> f.term(t -> t.field("electricityPrice").value("STATE_PRICE")));
            b.filter(f -> f.term(t -> t.field("waterPrice").value("STATE_PRICE")));
            remaining = removeTerms(remaining, "gia nha nuoc");
        }

        // 7. BEDROOMS / BATHROOMS / CAPACITY
        Integer bedrooms = extractNumberBeforeTerms(kw, "phong ngu", "pn", "bedroom");
        if (bedrooms != null && req.getMinBedrooms() == null) {
            b.filter(f -> f.range(r -> r.field("bedrooms").gte(JsonData.of(bedrooms))));
            remaining = removeNumberTerm(remaining, bedrooms, "phong ngu", "pn", "bedroom");
        }

        Integer bathrooms = extractNumberBeforeTerms(kw, "phong tam", "phong ve sinh", "wc", "toilet", "bathroom");
        if (bathrooms != null && req.getMinBathrooms() == null) {
            b.filter(f -> f.range(r -> r.field("bathrooms").gte(JsonData.of(bathrooms))));
            remaining = removeNumberTerm(remaining, bathrooms, "phong tam", "phong ve sinh", "wc", "toilet", "bathroom");
        }

        Integer capacity = extractNumberBeforeTerms(kw, "nguoi", "person", "people");
        if (capacity != null && req.getMinCapacity() == null) {
            b.filter(f -> f.range(r -> r.field("capacity").gte(JsonData.of(capacity))));
            remaining = removeNumberTerm(remaining, capacity, "nguoi", "person", "people");
        }

        // 8. PRICE INTENT
        if (req.getMinPrice() == null && req.getMaxPrice() == null) {
            PriceRange priceRange = extractPriceRange(kw);

            if (priceRange.min != null || priceRange.max != null) {
                b.filter(f -> f.range(r -> {
                    r.field("price");
                    if (priceRange.min != null) r.gte(JsonData.of(priceRange.min));
                    if (priceRange.max != null) r.lte(JsonData.of(priceRange.max));
                    return r;
                }));

                remaining = removePriceTerms(remaining);
            }
        }

        // 9. AREA INTENT
        if (req.getMinArea() == null && req.getMaxArea() == null) {
            NumberRange areaRange = extractAreaRange(kw);

            if (areaRange.min != null || areaRange.max != null) {
                b.filter(f -> f.range(r -> {
                    r.field("area");
                    if (areaRange.min != null) r.gte(JsonData.of(areaRange.min));
                    if (areaRange.max != null) r.lte(JsonData.of(areaRange.max));
                    return r;
                }));

                remaining = removeAreaTerms(remaining);
            }
        }

        return remaining.replaceAll("\\s+", " ").trim();
    }

    private String normalizeKeyword(String input) {
        if (input == null) return "";

        String normalized = Normalizer.normalize(input.toLowerCase().trim(), Normalizer.Form.NFD);
        normalized = normalized.replaceAll("\\p{M}", "");
        normalized = normalized.replace("đ", "d");
        normalized = normalized.replaceAll("[^a-z0-9\\s]", " ");
        normalized = normalized.replaceAll("\\s+", " ").trim();

        return " " + normalized + " ";
    }

    private boolean containsAny(String text, String... words) {
        for (String word : words) {
            if (text.contains(" " + word + " ")) {
                return true;
            }
        }
        return false;
    }

    private boolean containsPhrase(String text, String phrase) {
        return text.contains(" " + phrase + " ");
    }

    private String removeTerms(String text, String... terms) {
        String result = text;

        for (String term : terms) {
            result = result.replace(" " + term + " ", " ");
        }

        return result.replaceAll("\\s+", " ").trim();
    }

    private Integer extractNumberBeforeTerms(String normalizedKeyword, String... terms) {
        String text = normalizedKeyword.trim();

        for (String term : terms) {
            Pattern pattern = Pattern.compile("(\\d+)\\s+" + Pattern.quote(term));
            Matcher matcher = pattern.matcher(text);

            if (matcher.find()) {
                try {
                    return Integer.parseInt(matcher.group(1));
                } catch (NumberFormatException ignored) {
                    return null;
                }
            }
        }

        return null;
    }

    private String removeNumberTerm(String text, Integer number, String... terms) {
        String result = text;

        for (String term : terms) {
            result = result.replaceAll("\\b" + number + "\\s+" + Pattern.quote(term) + "\\b", " ");
        }

        return result.replaceAll("\\s+", " ").trim();
    }

    private PriceRange extractPriceRange(String normalizedKeyword) {
        String text = normalizedKeyword.trim();

        PriceRange range = new PriceRange();

        Matcher betweenMatcher = Pattern.compile(
                "(?:tu\\s+)?(\\d+(?:\\.\\d+)?)\\s*(trieu|tr|ty|nghin|k)\\s+(?:den|toi|-)\\s+(\\d+(?:\\.\\d+)?)\\s*(trieu|tr|ty|nghin|k)"
        ).matcher(text);

        if (betweenMatcher.find()) {
            range.min = toMoney(betweenMatcher.group(1), betweenMatcher.group(2));
            range.max = toMoney(betweenMatcher.group(3), betweenMatcher.group(4));
            return range;
        }

        Matcher maxMatcher = Pattern.compile(
                "(?:duoi|toi da|khong qua|max)\\s+(\\d+(?:\\.\\d+)?)\\s*(trieu|tr|ty|nghin|k)"
        ).matcher(text);

        if (maxMatcher.find()) {
            range.max = toMoney(maxMatcher.group(1), maxMatcher.group(2));
        }

        Matcher minMatcher = Pattern.compile(
                "(?:tren|tu|toi thieu|min)\\s+(\\d+(?:\\.\\d+)?)\\s*(trieu|tr|ty|nghin|k)"
        ).matcher(text);

        if (minMatcher.find()) {
            range.min = toMoney(minMatcher.group(1), minMatcher.group(2));
        }

        return range;
    }

    private BigDecimal toMoney(String amountText, String unit) {
        BigDecimal amount = new BigDecimal(amountText);

        return switch (unit) {
            case "ty" -> amount.multiply(BigDecimal.valueOf(1_000_000_000L));
            case "trieu", "tr" -> amount.multiply(BigDecimal.valueOf(1_000_000L));
            case "nghin", "k" -> amount.multiply(BigDecimal.valueOf(1_000L));
            default -> amount;
        };
    }

    private String removePriceTerms(String text) {
        return text
                .replaceAll("\\b(?:tu\\s+)?\\d+(?:\\.\\d+)?\\s*(trieu|tr|ty|nghin|k)\\s+(?:den|toi|-)\\s+\\d+(?:\\.\\d+)?\\s*(trieu|tr|ty|nghin|k)\\b", " ")
                .replaceAll("\\b(?:duoi|toi da|khong qua|max|tren|tu|toi thieu|min)\\s+\\d+(?:\\.\\d+)?\\s*(trieu|tr|ty|nghin|k)\\b", " ")
                .replaceAll("\\s+", " ")
                .trim();
    }

    private NumberRange extractAreaRange(String normalizedKeyword) {
        String text = normalizedKeyword.trim();

        NumberRange range = new NumberRange();

        Matcher betweenMatcher = Pattern.compile(
                "(?:tu\\s+)?(\\d+(?:\\.\\d+)?)\\s*(?:m2|met vuong)\\s+(?:den|toi|-)\\s+(\\d+(?:\\.\\d+)?)\\s*(?:m2|met vuong)"
        ).matcher(text);

        if (betweenMatcher.find()) {
            range.min = Double.parseDouble(betweenMatcher.group(1));
            range.max = Double.parseDouble(betweenMatcher.group(2));
            return range;
        }

        Matcher maxMatcher = Pattern.compile(
                "(?:duoi|toi da|khong qua|max)\\s+(\\d+(?:\\.\\d+)?)\\s*(?:m2|met vuong)"
        ).matcher(text);

        if (maxMatcher.find()) {
            range.max = Double.parseDouble(maxMatcher.group(1));
        }

        Matcher minMatcher = Pattern.compile(
                "(?:tren|tu|toi thieu|min)\\s+(\\d+(?:\\.\\d+)?)\\s*(?:m2|met vuong)"
        ).matcher(text);

        if (minMatcher.find()) {
            range.min = Double.parseDouble(minMatcher.group(1));
        }

        return range;
    }

    private String removeAreaTerms(String text) {
        return text
                .replaceAll("\\b(?:tu\\s+)?\\d+(?:\\.\\d+)?\\s*(m2|met vuong)\\s+(?:den|toi|-)\\s+\\d+(?:\\.\\d+)?\\s*(m2|met vuong)\\b", " ")
                .replaceAll("\\b(?:duoi|toi da|khong qua|max|tren|tu|toi thieu|min)\\s+\\d+(?:\\.\\d+)?\\s*(m2|met vuong)\\b", " ")
                .replaceAll("\\s+", " ")
                .trim();
    }

    private static class PriceRange {
        private BigDecimal min;
        private BigDecimal max;
    }

    private static class NumberRange {
        private Double min;
        private Double max;
    }
}