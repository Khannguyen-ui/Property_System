package com.homeverse.property.controller;

import com.homeverse.property.dto.request.ProjectCreateDTO;
import com.homeverse.property.dto.response.ProjectResponseDTO;
import com.homeverse.property.service.ProjectService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/admin/projects")
@RequiredArgsConstructor
public class AdminProjectController {

    private final ProjectService projectService;

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ProjectResponseDTO> createProject(
            @AuthenticationPrincipal String adminId,
            @RequestBody @Valid ProjectCreateDTO dto) {
        return ResponseEntity.ok(projectService.createProject(Long.valueOf(adminId), dto));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ProjectResponseDTO> updateProject(
            @AuthenticationPrincipal String adminId,
            @PathVariable Long id,
            @RequestBody @Valid ProjectCreateDTO dto) {
        return ResponseEntity.ok(projectService.updateProject(Long.valueOf(adminId), id, dto));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<String> deleteProject(
            @AuthenticationPrincipal String adminId,
            @PathVariable Long id) {
        projectService.deleteProject(Long.valueOf(adminId), id);


        return ResponseEntity.ok("Đã chuyển dự án vào thùng rác (Xóa mềm).");
    }
    @GetMapping("/trash")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Page<ProjectResponseDTO>> getDeletedProjects(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        // Không cần truyền adminId vì Admin nào cũng được xem thùng rác dự án chung
        return ResponseEntity.ok(projectService.getDeletedProjects(page, size));
    }

    // 2. Khôi phục dự án
    @PutMapping("/{id}/restore")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<String> restoreProject(
            @AuthenticationPrincipal String adminId,
            @PathVariable Long id) {

        projectService.restoreProject(Long.valueOf(adminId), id);
        return ResponseEntity.ok("Đã khôi phục dự án thành công. Trạng thái hiện tại: ACTIVE.");
    }

    // 3. Xóa vĩnh viễn (Hard Delete)
    @DeleteMapping("/{id}/force")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<String> hardDeleteProject(
            @AuthenticationPrincipal String adminId,
            @PathVariable Long id) {

        projectService.hardDeleteProject(Long.valueOf(adminId), id);
        return ResponseEntity.ok("Đã xóa vĩnh viễn dự án khỏi hệ thống. Không thể khôi phục!");
    }
    // 1. Xem danh sách dự án (Cho Bảng quản trị của Admin)
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Page<ProjectResponseDTO>> getAllProjects(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        return ResponseEntity.ok(projectService.getAllProjectsForAdmin(page, size));
    }

    // 2. Xem chi tiết dự án (Để Admin bấm vào xem trước khi Sửa/Xóa)
    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ProjectResponseDTO> getProjectDetail(@PathVariable Long id) {

        return ResponseEntity.ok(projectService.getProjectDetailForAdmin(id));
    }
}