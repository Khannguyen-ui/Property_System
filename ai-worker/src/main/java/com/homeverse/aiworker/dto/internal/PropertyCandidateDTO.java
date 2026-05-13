package com.homeverse.aiworker.dto.internal;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PropertyCandidateDTO {

    private Long propertyId;

    private String title;

    private Double price;

    private String province;

    private String district;

    private String propertyType;

    private String transactionType;

    private String status;

    private String imageUrl;
}