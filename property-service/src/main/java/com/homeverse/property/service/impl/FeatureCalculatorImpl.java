package com.homeverse.property.service.impl;

import com.homeverse.property.dto.request.TrackEventRequest;
import com.homeverse.property.dto.response.UserInterestProfileDTO;
import com.homeverse.property.entity.Property;
import com.homeverse.property.service.FeatureCalculator;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Objects;

@Component
public class FeatureCalculatorImpl implements FeatureCalculator {

    @Override
    public TrackEventRequest buildTrackRequest(
            Long userId,
            Property property,
            UserInterestProfileDTO profile,
            String action) {
        if (profile == null) {
            profile = new UserInterestProfileDTO();
        }

        double propertyPrice = toDouble(property.getPrice());
        double propertyArea = defaultDouble(property.getArea(), 0.0);

        return TrackEventRequest.builder()
                .userId(userId)
                .itemId(property.getId())
                .itemType(property.getVideoUrl() != null ? "REEL" : "PROPERTY")
                .action(action)

                .watchTime(0.0)
                .duration(1.0)

                .price(propertyPrice)
                .userBudget(defaultDouble(profile.getBudget(), propertyPrice))

                .area(propertyArea)
                .userArea(defaultDouble(profile.getPreferredArea(), propertyArea))
                .province(safeText(property.getProvince()))
                .district(safeText(property.getDistrict()))
                .ward(safeText(property.getWard()))
                .street(safeText(property.getStreet()))
                .propertyType(enumName(property.getPropertyType()))
                .transactionType(enumName(property.getTransactionType()))
                .provinceMatch(matchText(property.getProvince(), profile.getProvince()))
                .districtMatch(matchText(property.getDistrict(), profile.getDistrict()))
                .wardMatch(matchText(property.getWard(), profile.getWard()))
                .streetMatch(matchText(property.getStreet(), profile.getStreet()))
                .district(safeText(property.getDistrict()))
                .locationMatch(resolveLocationMatch(property, profile))

                .categoryMatch(matchText(enumName(property.getPropertyType()), profile.getPropertyType()))
                .transactionMatch(matchText(enumName(property.getTransactionType()), profile.getTransactionType()))

                .bedroomMatch(matchNumber(property.getBedrooms(), profile.getBedrooms()))
                .bathroomMatch(matchNumber(property.getBathrooms(), profile.getBathrooms()))
                .balconyMatch(matchBoolean(property.getHasBalcony(), profile.getHasBalcony()))

                .furnishingMatch(matchText(enumName(property.getFurnishingStatus()), profile.getFurnishingStatus()))
                .availabilityMatch(
                        matchText(enumName(property.getAvailabilityStatus()), profile.getAvailabilityStatus()))

                .amenityMatchRatio(amenityMatchRatio(property.getAmenities(), profile.getAmenities()))

                .build();
    }

    public Integer matchText(String a, String b) {
        String left = normalizeText(a);
        String right = normalizeText(b);

        if (left.isBlank() || right.isBlank()) {
            return 0;
        }

        return left.equals(right) ? 1 : 0;
    }

    private String safeText(String value) {
        String text = normalizeText(value);
        return text.isBlank() ? "" : value.trim();
    }

    public Integer matchNumber(Integer a, Integer b) {
        if (a == null || b == null || a <= 0 || b <= 0) {
            return 0;
        }

        return Objects.equals(a, b) ? 1 : 0;
    }

    public Integer matchBoolean(Boolean a, Boolean b) {
        if (a == null || b == null) {
            return 0;
        }

        return Objects.equals(a, b) ? 1 : 0;
    }

    public Double amenityMatchRatio(List<String> propertyAmenities, List<String> userAmenities) {
        if (propertyAmenities == null || propertyAmenities.isEmpty()) {
            return 0.0;
        }

        if (userAmenities == null || userAmenities.isEmpty()) {
            return 0.0;
        }

        long matched = userAmenities.stream()
                .map(this::normalizeText)
                .filter(userAmenity -> !userAmenity.isBlank())
                .filter(userAmenity -> propertyAmenities.stream()
                        .map(this::normalizeText)
                        .anyMatch(propertyAmenity -> !propertyAmenity.isBlank()
                                && propertyAmenity.equals(userAmenity)))
                .count();

        return Math.min((double) matched / userAmenities.size(), 1.0);
    }

    private Integer resolveLocationMatch(Property property, UserInterestProfileDTO profile) {
        int province = matchText(property.getProvince(), profile.getProvince());
        int district = matchText(property.getDistrict(), profile.getDistrict());
        int ward = matchText(property.getWard(), profile.getWard());
        int street = matchText(property.getStreet(), profile.getStreet());

        return province == 1 || district == 1 || ward == 1 || street == 1 ? 1 : 0;
    }

    private String enumName(Enum<?> value) {
        return value == null ? "" : value.name();
    }

    private Double toDouble(java.math.BigDecimal value) {
        return value == null ? 0.0 : value.doubleValue();
    }

    private Double defaultDouble(Double value, Double fallback) {
        return value != null ? value : fallback;
    }

    private String normalizeText(String value) {
        if (value == null) {
            return "";
        }

        String text = value.trim().toLowerCase();

        if (text.isBlank()
                || text.equals("null")
                || text.equals("unknown")
                || text.equals("không xác định")) {
            return "";
        }

        return text.replaceAll("\\s+", " ");
    }
}