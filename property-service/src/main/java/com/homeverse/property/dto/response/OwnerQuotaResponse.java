package com.homeverse.property.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OwnerQuotaResponse {
    private Long ownerId;
    private Integer freePostsRemaining;
    private String role;
}