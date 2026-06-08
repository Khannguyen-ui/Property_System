package com.homeverse.property.controller;

import com.homeverse.common.dto.ApiResponse;
import com.homeverse.property.dto.response.PropertyResponseDTO;
import com.homeverse.property.service.AdminPropertyService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/admin/properties")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')") // Chặn cửa 100% Admin mới được vào đây
public class AdminPropertyController {

    private final AdminPropertyService adminPropertyService;


    @GetMapping
    public ApiResponse<Page<PropertyResponseDTO>> getAllProperties(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Long ownerId
    ) {
        return ApiResponse.<Page<PropertyResponseDTO>>builder()
                .result(adminPropertyService.getAllProperties(page, size, status, ownerId))
                .build();
    }

    // 2. Xem chi tiết bài đăng (Admin có quyền xem cả những bài đang bị ẩn hoặc chờ duyệt)
    @GetMapping("/{id}")
    public ResponseEntity<PropertyResponseDTO> getPropertyDetail(@PathVariable Long id) {
        return ResponseEntity.ok(adminPropertyService.getPropertyDetail(id));
    }

    // 3. Duyệt bài / Đổi trạng thái (Luồng Outbox sếp đã làm thành công)
    @PatchMapping("/{id}/status")
    public ResponseEntity<String> updatePropertyStatus(
            @AuthenticationPrincipal String adminId,
            @PathVariable Long id,
            @RequestParam String status) {
        adminPropertyService.updatePropertyStatus(Long.valueOf(adminId), id, status);
        return ResponseEntity.ok("Cập nhật trạng thái bài đăng thành công!");
    }

    // 4. Gỡ bài vi phạm (Soft Delete - Đẩy vào thùng rác)
    @DeleteMapping("/{id}")
    public ResponseEntity<String> deleteProperty(
            @AuthenticationPrincipal String adminId,
            @PathVariable Long id) {
        adminPropertyService.deleteProperty(Long.valueOf(adminId), id);
        return ResponseEntity.ok("Đã gỡ bài đăng vi phạm và đưa vào thùng rác.");
    }

    // 5. Xem thùng rác (Các bài đã bị Admin hoặc Chủ nhà xóa mềm)
    @GetMapping("/trash")
    public ResponseEntity<Page<PropertyResponseDTO>> getDeletedProperties(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(adminPropertyService.getDeletedProperties(page, size));
    }

    // 6. Khôi phục bài đăng từ thùng rác
    @PutMapping("/{id}/restore")
    public ResponseEntity<String> restoreProperty(
            @AuthenticationPrincipal String adminId,
            @PathVariable Long id) {
        adminPropertyService.restoreProperty(Long.valueOf(adminId), id);
        return ResponseEntity.ok("Đã khôi phục bài đăng thành công.");
    }

    // 7. Xóa vĩnh viễn (Hard delete - Dọn sạch Database)
    @DeleteMapping("/{id}/force")
    public ResponseEntity<String> hardDeleteProperty(
            @AuthenticationPrincipal String adminId,
            @PathVariable Long id) {
        adminPropertyService.hardDeleteProperty(Long.valueOf(adminId), id);
        return ResponseEntity.ok("Đã xóa vĩnh viễn bài đăng khỏi hệ thống!");
    }
}