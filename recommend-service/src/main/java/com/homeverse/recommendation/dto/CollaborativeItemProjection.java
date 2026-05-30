package com.homeverse.recommendation.dto;

public interface CollaborativeItemProjection {

    Long getItemId();

    String getItemType();

    Double getCfScore();
}