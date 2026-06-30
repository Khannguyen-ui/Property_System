package com.homeverse.identity.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class OAuth2ExchangeCodeRequest {
    @NotBlank
    private String code;
}