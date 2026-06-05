package com.homeverse.wallet.dto;

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

    private String referenceId;
}