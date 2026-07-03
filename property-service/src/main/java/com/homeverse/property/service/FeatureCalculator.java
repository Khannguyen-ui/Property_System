package com.homeverse.property.service;

import com.homeverse.property.dto.request.TrackEventRequest;
import com.homeverse.property.dto.response.UserInterestProfileDTO;
import com.homeverse.property.entity.Property;

public interface FeatureCalculator {

    TrackEventRequest buildTrackRequest(
            Long userId,
            Property property,
            UserInterestProfileDTO profile,
            String action
    );
}