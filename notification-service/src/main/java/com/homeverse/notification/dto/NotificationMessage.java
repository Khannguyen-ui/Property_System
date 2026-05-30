package com.homeverse.notification.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationMessage {
    private String type;         
    private String recipientId; 
    private String title;
    private String content;

    private Object metadata;     
    
    private LocalDateTime timestamp;
}