package com.homeverse.customer.dto.response;

import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class KycOcrResponseDTO {
    private String kycToken;
    private String citizenId;
    private String fullName;
    private String address;
}