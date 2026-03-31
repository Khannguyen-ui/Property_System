package com.homeverse.customer.dto.response;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class CustomerPublicResponseDTO {
    private String id;
    private String fullName;
    private String avatarUrl;
    private String kycStatus;

    private String phone;
    private LocalDateTime createdAt;
}