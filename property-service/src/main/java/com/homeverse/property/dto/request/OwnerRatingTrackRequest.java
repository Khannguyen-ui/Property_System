package com.homeverse.property.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OwnerRatingTrackRequest {
    private Long userId;
    private Long ownerId;
    private Integer rating;
}
