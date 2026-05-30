package com.homeverse.common.kafka.cdc.message;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class CustomerCdcMessage {
    private Long id;

    @JsonProperty("full_name")
    private String fullName;

    @JsonProperty("avatar_url")
    private String avatarUrl;

    @JsonProperty("public_id")
    private String publicId;

    @JsonProperty("phone")
    private String phone;
}