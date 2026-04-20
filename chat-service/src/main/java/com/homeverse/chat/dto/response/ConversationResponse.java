package com.homeverse.chat.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConversationResponse {
    
    // ID của người đối diện (Partner ID) - Dùng để FE gọi API lấy lịch sử chat
    private Long id; 

    // Thông tin hiển thị của người đối diện (Lấy từ Customer Service qua FeignClient)
    private String fullName;
    private String avatar;

    // Thông tin tin nhắn cuối cùng để hiển thị bản xem trước (Preview)
    private String lastMessage;
    private LocalDateTime lastTime;

    // Trạng thái bổ sung
    private boolean isOnline;    // Có thể dùng Redis để check trạng thái này
    private int unreadCount;     // Số tin nhắn chưa đọc của người dùng hiện tại trong hội thoại này
}