package com.homeverse.recommendation.service;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Objects;

@Component
public class FeatureCalculator {

    public Integer matchText(String a, String b) {
        if (a == null || b == null) return 0;
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

    public Double diffRatio(Double value, Double target) {
        if (value == null) value = 0.0;
        if (target == null) target = 0.0;

        double base = target > 0 ? target : value;
        if (base <= 0) return 1.0;

        return Math.min(Math.abs(value - target) / base, 1.0);
    }

    public Double amenityMatchRatio(List<String> propertyAmenities, List<String> userAmenities) {
        if (propertyAmenities == null || userAmenities == null || userAmenities.isEmpty()) {
            return 0.0;
        }

        long matched = userAmenities.stream()
                .filter(u -> propertyAmenities.stream()
                        .anyMatch(p -> p != null && u != null && p.equalsIgnoreCase(u)))
                .count();

        return (double) matched / userAmenities.size();
    }
}