package com.homeverse.payment.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class ServiceOrderDTO {
    // Request gửi lên
    private Integer partnerId;
    private LocalDateTime bookingTime;
    private String note;

    // Response trả về
    private Long id;
    private String partnerName;
    private String status;
}