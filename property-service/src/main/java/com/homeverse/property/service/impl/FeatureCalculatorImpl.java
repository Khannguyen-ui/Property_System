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
            String action
    ) {
        if (profile == null) {
            profile = new UserInterestProfileDTO();
        }

        return TrackEventRequest.builder()
                .userId(userId)
                .itemId(property.getId())
                .itemType(property.getVideoUrl() != null ? "reel" : "property")
                .action(action)

                .watchTime(0.0)
                .duration(1.0)

                .price(toDouble(property.getPrice()))
                .userBudget(defaultDouble(profile.getBudget(), toDouble(property.getPrice())))

                .area(defaultDouble(property.getArea(), 0.0))
                .userArea(defaultDouble(profile.getPreferredArea(), property.getArea()))

                .provinceMatch(matchText(property.getProvince(), profile.getProvince()))
                .districtMatch(matchText(property.getDistrict(), profile.getDistrict()))
                .wardMatch(matchText(property.getWard(), profile.getWard()))
                .streetMatch(matchText(property.getStreet(), profile.getStreet()))

                .categoryMatch(matchText(enumName(property.getPropertyType()), profile.getPropertyType()))
                .transactionMatch(matchText(enumName(property.getTransactionType()), profile.getTransactionType()))

                .bedroomMatch(matchNumber(property.getBedrooms(), profile.getBedrooms()))
                .bathroomMatch(matchNumber(property.getBathrooms(), profile.getBathrooms()))
                .balconyMatch(matchBoolean(property.getHasBalcony(), profile.getHasBalcony()))

                .furnishingMatch(matchText(enumName(property.getFurnishingStatus()), profile.getFurnishingStatus()))
                .availabilityMatch(matchText(enumName(property.getAvailabilityStatus()), profile.getAvailabilityStatus()))

                .amenityMatchRatio(amenityMatchRatio(property.getAmenities(), profile.getAmenities()))

                .build();
    }

    public Integer matchText(String a, String b) {
        if (a == null || b == null) return 0;
        if (a.isBlank() || b.isBlank()) return 0;
        return a.trim().equalsIgnoreCase(b.trim()) ? 1 : 0;
    }

    public Integer matchNumber(Integer a, Integer b) {
        if (a == null || b == null || a <= 0 || b <= 0) return 0;
        return Objects.equals(a, b) ? 1 : 0;
    }

    public Integer matchBoolean(Boolean a, Boolean b) {
        if (a == null || b == null) return 0;
        return Objects.equals(a, b) ? 1 : 0;
    }

    public Double amenityMatchRatio(List<String> propertyAmenities, List<String> userAmenities) {
        if (propertyAmenities == null || propertyAmenities.isEmpty()) return 0.0;
        if (userAmenities == null || userAmenities.isEmpty()) return 0.0;

        long matched = userAmenities.stream()
                .filter(u -> u != null && !u.isBlank())
                .filter(u -> propertyAmenities.stream()
                        .anyMatch(p -> p != null && !p.isBlank() && p.trim().equalsIgnoreCase(u.trim())))
                .count();

        return Math.min((double) matched / userAmenities.size(), 1.0);
    }

    private String enumName(Enum<?> value) {
        return value == null ? null : value.name();
    }

    private Double toDouble(java.math.BigDecimal value) {
        return value == null ? 0.0 : value.doubleValue();
    }

    private Double defaultDouble(Double value, Double fallback) {
        return value != null ? value : fallback;
    }
}