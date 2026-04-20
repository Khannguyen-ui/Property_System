package com.homeverse.search.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class WardPriceDTO {
    private String wardName;
    private String averagePrice;
    private String unit;
    private Long totalPosts;
}