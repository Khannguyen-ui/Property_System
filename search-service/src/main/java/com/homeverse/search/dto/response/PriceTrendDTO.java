package com.homeverse.search.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PriceTrendDTO {
    private String month;
    private BigDecimal averagePrice;
    private Long totalPosts;
}