package com.homeverse.property.controller;

import com.homeverse.common.dto.ApiResponse;
import com.homeverse.property.dto.request.AppointmentCreateRequest;
import com.homeverse.property.dto.response.AppointmentResponse;
import com.homeverse.property.entity.Appointment;
import com.homeverse.property.service.AppointmentService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/appointments")
@RequiredArgsConstructor
public class AppointmentController {

    private final AppointmentService appointmentService;

    @GetMapping("/my-calendar")
    public ResponseEntity<ApiResponse<List<AppointmentResponse>>> getMyCalendar(Authentication authentication) {
        Long currentUserId = getCurrentUserId(authentication);

        return ResponseEntity.ok(
                ApiResponse.<List<AppointmentResponse>>builder()
                        .message("Lấy lịch hẹn thành công")
                        .result(appointmentService.getMyCalendar(currentUserId))
                        .build()
        );
    }

    @PostMapping
    public ResponseEntity<ApiResponse<AppointmentResponse>> create(
            Authentication authentication,
            @RequestBody AppointmentCreateRequest request
    ) {
        Long currentUserId = getCurrentUserId(authentication);

        return ResponseEntity.ok(
                ApiResponse.<AppointmentResponse>builder()
                        .message("Tạo lịch hẹn thành công")
                        .result(appointmentService.create(currentUserId, request))
                        .build()
        );
    }

    @PutMapping("/{id}/status")
    public ResponseEntity<ApiResponse<AppointmentResponse>> updateStatus(
            Authentication authentication,
            @PathVariable Long id,
            @RequestParam String status
    ) {
        Long currentUserId = getCurrentUserId(authentication);
        Appointment.Status parsedStatus = parseStatus(status);

        return ResponseEntity.ok(
                ApiResponse.<AppointmentResponse>builder()
                        .message("Cập nhật trạng thái lịch hẹn thành công")
                        .result(appointmentService.updateStatus(currentUserId, id, parsedStatus))
                        .build()
        );
    }

    @PutMapping("/{id}/suggest")
    public ResponseEntity<ApiResponse<AppointmentResponse>> suggestNewTime(
            Authentication authentication,
            @PathVariable Long id,
            @RequestParam
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            LocalDateTime newTime,
            @RequestParam(required = false) String note
    ) {
        Long currentUserId = getCurrentUserId(authentication);

        return ResponseEntity.ok(
                ApiResponse.<AppointmentResponse>builder()
                        .message("Đề xuất giờ mới thành công")
                        .result(appointmentService.suggestNewTime(currentUserId, id, newTime, note))
                        .build()
        );
    }

    @PutMapping("/{id}/accept-suggestion")
    public ResponseEntity<ApiResponse<AppointmentResponse>> acceptSuggestion(
            Authentication authentication,
            @PathVariable Long id
    ) {
        Long currentUserId = getCurrentUserId(authentication);

        return ResponseEntity.ok(
                ApiResponse.<AppointmentResponse>builder()
                        .message("Đồng ý giờ đề xuất thành công")
                        .result(appointmentService.acceptSuggestion(currentUserId, id))
                        .build()
        );
    }

    private Appointment.Status parseStatus(String status) {
        if (status == null || status.isBlank()) {
            throw new IllegalArgumentException("Thiếu trạng thái lịch hẹn");
        }

        String normalized = status.trim().toUpperCase();

        if ("APPROVED".equals(normalized)) {
            normalized = "ACCEPTED";
        }

        return Appointment.Status.valueOf(normalized);
    }

    private Long getCurrentUserId(Authentication authentication) {
        if (authentication == null || authentication.getName() == null) {
            throw new IllegalArgumentException("Bạn chưa đăng nhập");
        }

        return Long.valueOf(authentication.getName());
    }
}