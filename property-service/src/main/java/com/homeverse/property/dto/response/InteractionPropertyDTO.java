package com.homeverse.property.dto.response;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
public class InteractionPropertyDTO {

    private Long id;

    private String title;

    private BigDecimal price;

    private String province;

    private String district;

    private String address;

    private String propertyType;

    private String transactionType;

    private String imageUrl;

    private LocalDateTime createdAt;

    private boolean liked;

    private boolean saved;
}