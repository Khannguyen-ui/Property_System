package com.homeverse.chat.controller;

import com.homeverse.chat.dto.request.ChatMessageDTO;
import com.homeverse.chat.dto.response.ChatMessageResponse;
import com.homeverse.chat.dto.response.ConversationResponse;
import com.homeverse.chat.service.ChatService; // Import Interface
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
public class ChatController {

    private final SimpMessagingTemplate messagingTemplate;
    private final ChatService chatService; // Dùng Interface

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
                response
        );
        
        // Gửi ngược lại cho chính người gửi để họ xác nhận tin nhắn đã lên server
        messagingTemplate.convertAndSend(
                "/topic/user/" + chatMessage.getSenderId(),
                response
        );
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
                response
        );

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
}