package com.homeverse.common.kafka.cdc.message;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class CustomerCdcMessage {
    private Long id;

    @JsonProperty("full_name") // Tên cột DB của bạn
    private String fullName;

    @JsonProperty("avatar_url") // Tên cột DB
    private String avatarUrl;

    @JsonProperty("public_id") // Tên cột lưu Slug trong DB
    private String publicId;
}