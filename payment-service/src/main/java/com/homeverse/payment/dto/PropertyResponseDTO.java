package com.homeverse.payment.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class PropertyResponseDTO {
    private Long id;
    private Long ownerId;
    private String title;
    private String status;
    private LocalDateTime expiresAt;
}