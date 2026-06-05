package com.homeverse.property.dto.request;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class AppointmentCreateRequest {

    private Long propertyId;

    private LocalDateTime appointmentTime;

    private LocalDateTime scheduledAt;

    private String note;
}