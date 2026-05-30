package com.homeverse.customer.dto.request;

import com.homeverse.customer.model.json.LifestyleProfile;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CustomerProfileDTO {

    private String fullName;
    private String phone;
    private String avatarUrl;
    private String bannerUrl;

    private LifestyleProfile lifestyleProfile;
}