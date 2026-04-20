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
    public class ChatMessageResponse {
        private Long id;
        private Long senderId;
        private Long receiverId;
        private String content;
        private String type;
        private LocalDateTime createdAt;
    }