package com.homeverse.chat.controller;

import com.homeverse.chat.dto.ai.AiChatRequest;
import com.homeverse.chat.dto.request.ChatMessageDTO;
import com.homeverse.chat.dto.request.MessageReactionRequest;
import com.homeverse.chat.dto.request.TypingEvent;
import com.homeverse.chat.dto.response.ChatMessageResponse;
import com.homeverse.chat.dto.response.ConversationResponse;
import com.homeverse.chat.service.impl.ChatPresenceServiceImpl;
import com.homeverse.chat.service.ChatService; // Import Interface
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import com.homeverse.chat.kafka.AiRequestProducer;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import java.security.Principal;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
public class ChatController {

    private final SimpMessagingTemplate messagingTemplate;
    private final ChatService chatService;
    private final AiRequestProducer aiRequestProducer;
    private final ChatPresenceServiceImpl chatPresenceService;

    // ========================================================================
    // 1. WEBSOCKET HANDLER
    // ========================================================================
    @MessageMapping("/chat")
    public void processMessage(@Payload ChatMessageDTO chatMessage) {
        // Service trả về Response DTO luôn, không trả về Entity
        ChatMessageResponse response = chatService.saveMessage(chatMessage);

        // Gửi real-time cho người nhận
        messagingTemplate.convertAndSend(
                "/topic/user/" + chatMessage.getReceiverId(),
                response);

        // Gửi ngược lại cho chính người gửi để họ xác nhận tin nhắn đã lên server
        messagingTemplate.convertAndSend(
                "/topic/user/" + chatMessage.getSenderId(),
                response);
    }

    // ========================================================================
    // 2. HTTP REST API
    // ========================================================================

    @GetMapping("/history/{partnerId}")
    public ResponseEntity<List<ChatMessageResponse>> getChatHistory(@PathVariable Long partnerId) {
        return ResponseEntity.ok(chatService.getChatHistory(partnerId));
    }

    @PostMapping("/send")
    public ResponseEntity<ChatMessageResponse> sendMessage(@RequestBody ChatMessageDTO dto) {
        ChatMessageResponse response = chatService.saveMessage(dto);

        // Vẫn bắn Socket để người kia nhận được ngay (Hybrid)
        messagingTemplate.convertAndSend(
                "/topic/user/" + dto.getReceiverId(),
                response);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/conversations")
    
    public ResponseEntity<List<ConversationResponse>> getConversations() {
        return ResponseEntity.ok(chatService.getUserConversations());
    }

    @PostMapping("/start")
    public ResponseEntity<String> startChat(@RequestBody Map<String, Long> payload) {
        Long partnerId = payload.get("partnerId");
        chatService.createConversationIfNotExists(partnerId);
        return ResponseEntity.ok("Conversation started");
    }

    // THÊM API: Đánh dấu đã đọc
    @PutMapping("/read/{partnerId}")
    public ResponseEntity<Void> markAsRead(@PathVariable Long partnerId) {
        chatService.markAsRead(partnerId);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/test-ai-flow")
    public ResponseEntity<String> testAiFlowFromPostman(@RequestBody AiChatRequest request, Principal principal) {
        // 1. Chặn cửa nếu Postman không gửi JWT Token
        if (principal == null) {
            return ResponseEntity.status(401).body("Lỗi: Sếp chưa gắn Bearer Token vào Postman!");
        }

        // 2. Lấy User ID thật từ Token
        String currentUserId = principal.getName();
        request.setUserId(currentUserId);

        if (request.getConversationId() == null || request.getConversationId().isEmpty()) {
            request.setConversationId("conv-postman-test");
        }

        aiRequestProducer.sendAiRequest(currentUserId, request);

        return ResponseEntity.ok("🚀 Đã ném câu hỏi của User [" + currentUserId
                + "] vào Kafka! Sếp mở log Docker ra xem AI Worker chạy nhé.");
    }

    @MessageMapping("/ai-chat")
    public void processAiMessage(@Payload AiChatRequest aiRequest, Principal principal) {

        // 1. LỚP GIÁP BẢO VỆ (Chuẩn Spring Docs)
        // Nếu không có thông tin định danh -> Đá văng ngay lập tức
        if (principal == null) {
            throw new IllegalArgumentException("Truy cập trái phép! Lỗi xác thực WebSocket.");
            // (Nếu sếp có cấu hình @MessageExceptionHandler, nó sẽ bắt lỗi này báo về FE)
        }

        // 2. Lấy User ID an toàn
        String currentUserId = principal.getName();
        aiRequest.setUserId(currentUserId);

        // 3. Khôi phục logic Conversation ID (Tránh Redis bị ngáo)
        if (aiRequest.getConversationId() == null || aiRequest.getConversationId().isEmpty()) {
            // Tự động sinh ra 1 ID hội thoại mặc định cho user này nếu FE quên gửi
            aiRequest.setConversationId("conv-" + currentUserId);
        }

        // 4. Bắn vào Kafka
        aiRequestProducer.sendAiRequest(currentUserId, aiRequest);
    }

    @MessageMapping("/chat.typing")
    public void typing(@Payload TypingEvent event) {
        System.out.println("TYPING EVENT = " + event);
        messagingTemplate.convertAndSendToUser(
                event.getReceiverId().toString(),
                "/queue/typing",
                event);
    }

    @PostMapping("/presence/online")
    public ResponseEntity<Void> markOnline(
            @RequestHeader("X-User-Id") Long userId) {
        System.out.println("CALL MARK ONLINE USER = " + userId);

        chatPresenceService.markOnline(userId);

        return ResponseEntity.ok().build();
    }

    @PostMapping("/presence/offline")
    public ResponseEntity<Void> markOffline(@RequestHeader("X-User-Id") Long userId) {
        chatPresenceService.markOffline(userId);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/presence/{userId}")
    public ResponseEntity<Map<String, Object>> getPresence(@PathVariable Long userId) {

        String lastSeen = chatPresenceService.getLastSeen(userId);

        Map<String, Object> result = new java.util.HashMap<>();
        result.put("userId", userId);
        result.put("online", chatPresenceService.isOnline(userId));
        result.put("lastSeen", lastSeen != null ? lastSeen : "");

        return ResponseEntity.ok(result);
    }

    @PutMapping("/recall/{messageId}")
    public ResponseEntity<Void> recallMessage(@PathVariable String messageId) {
        chatService.recallMessage(messageId);
        return ResponseEntity.ok().build();
    }
    @PostMapping("/reaction")
public ResponseEntity<ChatMessageResponse> reactMessage(
        @RequestBody MessageReactionRequest request
) {
    return ResponseEntity.ok(
            chatService.reactMessage(
                    request.getMessageId(),
                    request.getEmoji()
            )
    );
}
}