package com.homeverse.aiworker.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PropertyCardDTO {

    private Long propertyId;

    private String title;

    private Double price;

    private String district;

    private String imageUrl;
}