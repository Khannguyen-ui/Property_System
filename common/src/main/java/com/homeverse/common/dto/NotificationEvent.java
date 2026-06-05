package com.homeverse.common.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationEvent {

    private Long receiverId;
    private Long senderId;
    private String title;
    private String content;
    private String type;
    private Long referenceId;
}
