package com.homeverse.property.dto.request;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TrackEventRequest {

    private Long userId;
    private Long itemId;
    private String itemType;
    private String action;

    private Double watchTime;
    private Double duration;
    private Double price;
    private Double userBudget;

    private Integer locationMatch;
    private Integer categoryMatch;

    private String district;
}