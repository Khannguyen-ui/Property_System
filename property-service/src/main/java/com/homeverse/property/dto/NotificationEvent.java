package com.homeverse.property.dto;

import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationEvent {

    private Long receiverId;

    private String title;

    private String content;

    private String type;

    private Long referenceId;
}