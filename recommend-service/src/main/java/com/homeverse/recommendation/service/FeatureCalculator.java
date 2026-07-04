package com.homeverse.recommendation.service;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Objects;

@Component
public class FeatureCalculator {

    public Integer matchText(String a, String b) {
        String left = normalizeText(a);
        String right = normalizeText(b);

        if (left.isBlank() || right.isBlank()) {
            return 0;
        }

        return left.equals(right) ? 1 : 0;
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

    public Double diffRatio(Double value, Double target) {
        double safeValue = value != null ? value : 0.0;
        double safeTarget = target != null ? target : 0.0;

        double base = safeTarget > 0 ? safeTarget : safeValue;

        if (base <= 0) {
            return 1.0;
        }

        return Math.min(Math.abs(safeValue - safeTarget) / base, 1.0);
    }

    public Double amenityMatchRatio(List<String> propertyAmenities, List<String> userAmenities) {
        if (propertyAmenities == null || propertyAmenities.isEmpty()
                || userAmenities == null || userAmenities.isEmpty()) {
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

        return (double) matched / userAmenities.size();
    }

    public Integer safeMatch(Integer value) {
        return value != null ? value : 0;
    }

    public Double safeDouble(Double value) {
        return value != null ? value : 0.0;
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