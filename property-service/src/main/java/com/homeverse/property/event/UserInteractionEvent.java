package com.homeverse.property.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor // Bắt buộc phải có để Kafka/Jackson có thể convert từ JSON sang Object
@AllArgsConstructor // Bắt buộc phải có khi dùng chung với @Builder
public class UserInteractionEvent {

    private Long userId;
    private String guestId;
    private Long propertyId;
    private String actionType; // Sẽ chứa các chữ: "LIKE", "UNLIKE", "SAVE", "UNSAVE", "VIEW"
    private Long timestamp;    // Thời gian xảy ra sự kiện (dùng để AI phân tích)

}