package com.homeverse.property.dto.response;

import lombok.*;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AppointmentResponse {

    private Long id;

    private Long propertyId;

    private Long userId;

    private Long ownerId;

    private Long partnerId;

    private LocalDateTime appointmentTime;


    private LocalDateTime scheduledAt;

    private String note;

    private String status;

    private LocalDateTime suggestedTime;

    private String suggestedNote;

    private Boolean myRequest;
}