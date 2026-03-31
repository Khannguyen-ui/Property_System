package com.homeverse.customer.dto.response;

import com.homeverse.customer.model.json.LifestyleProfile;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CustomerResponseDTO {
    private Long id;
    private String email;
    private String fullName;
    private String phone;
    private String avatarUrl;
    private String bannerUrl;

    private String kycStatus;

    private LifestyleProfile lifestyleProfile;
}