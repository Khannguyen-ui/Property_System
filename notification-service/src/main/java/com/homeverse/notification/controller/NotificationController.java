package com.homeverse.notification.controller;

import com.homeverse.common.dto.ApiResponse;
import com.homeverse.notification.entity.Notification;
import com.homeverse.notification.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationRepository notificationRepository;


    @GetMapping
    public ApiResponse<Page<Notification>> getAllNotifications(
            Principal principal,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        String userId = principal.getName();
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());

        return ApiResponse.<Page<Notification>>builder()
                .result(notificationRepository.findByUserId(userId, pageable))
                .build();
    }


    @GetMapping("/unread")
    public ApiResponse<Page<Notification>> getUnreadNotifications(
            Principal principal,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        String userId = principal.getName();
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());

        return ApiResponse.<Page<Notification>>builder()
                .result(notificationRepository.findByUserIdAndIsReadFalse(userId, pageable))
                .build();
    }


    @GetMapping("/unread-count")
    public ApiResponse<Long> getUnreadCount(Principal principal) {
        String userId = principal.getName();
        return ApiResponse.<Long>builder()
                .result(notificationRepository.countByUserIdAndIsReadFalse(userId))
                .build();
    }


    @PutMapping("/{id}/read")
    public ApiResponse<String> markAsRead(@PathVariable Long id, Principal principal) {
        String userId = principal.getName();

        notificationRepository.findById(id).ifPresent(notif -> {

            if (notif.getUserId().equals(userId)) {
                notif.setRead(true);
                notificationRepository.save(notif);
            }
        });
        return ApiResponse.<String>builder().result("Đã đánh dấu đọc").build();
    }


    @PutMapping("/read-all")
    public ApiResponse<String> markAllAsRead(Principal principal) {
        String userId = principal.getName();
        notificationRepository.markAllAsReadByUserId(userId);
        return ApiResponse.<String>builder().result("Đã đánh dấu đọc tất cả").build();
    }
}