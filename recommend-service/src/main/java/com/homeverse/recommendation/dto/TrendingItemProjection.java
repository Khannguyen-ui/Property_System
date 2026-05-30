package com.homeverse.recommendation.dto;

public interface TrendingItemProjection {

    Long getItemId();

    String getItemType();

    Double getTrendingScore();
}