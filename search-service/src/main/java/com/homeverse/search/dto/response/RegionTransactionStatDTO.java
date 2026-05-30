package com.homeverse.search.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class RegionTransactionStatDTO {
    private String regionName;
    private Long totalPosts;
    private Long forSaleCount;
    private Long forRentCount;
}