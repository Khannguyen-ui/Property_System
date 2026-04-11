package com.homeverse.common.kafka.cdc.message;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class UserCdcMessage {
    private Long id;
    private String email;
    @JsonProperty("full_name")
    private String fullName;
    private String phone;
    private String role;
    @JsonProperty("kyc_status")
    private String kycStatus;
}