package com.homeverse.customer.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CustomerPublicResponseDTO {
    private Long id;
    private String publicId;

    private String fullName;
    private String phone;
    private String avatarUrl;

    private String kycStatus;
    private String membershipLevel;
    private LocalDateTime createdAt;
}