package com.homeverse.chat.dto.ai;

import lombok.*;

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