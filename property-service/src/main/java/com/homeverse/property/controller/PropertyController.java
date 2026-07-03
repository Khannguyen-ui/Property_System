package com.homeverse.property.controller;


import com.homeverse.common.dto.ApiResponse;
import com.homeverse.property.dto.request.PropertyCreateDTO;
import com.homeverse.property.dto.response.PropertyResponseDTO;
import com.homeverse.property.service.PropertyService;
import com.homeverse.property.dto.response.OwnerQuotaResponse;
import com.homeverse.property.entity.OwnerQuota;
import com.homeverse.property.repository.OwnerQuotaRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/properties")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('OWNER')")
public class PropertyController {

    private final PropertyService propertyService;
    private final OwnerQuotaRepository ownerQuotaRepository;

    @PostMapping
    public ResponseEntity<PropertyResponseDTO> createProperty(
            Authentication authentication,
            @RequestBody @Valid PropertyCreateDTO dto) {

        Long ownerId = extractUserId(authentication);
        return ResponseEntity.ok(propertyService.createProperty(ownerId, dto));
    }

    @PutMapping("/{id}")
    public ResponseEntity<PropertyResponseDTO> updateProperty(
            Authentication authentication,
            @PathVariable Long id,
            @RequestBody @Valid PropertyCreateDTO dto) {

        Long ownerId = extractUserId(authentication);
        return ResponseEntity.ok(propertyService.updateProperty(ownerId, id, dto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<String> deleteProperty(
            Authentication authentication,
            @PathVariable Long id) {

        Long ownerId = extractUserId(authentication);
        propertyService.deleteProperty(ownerId, id);
        return ResponseEntity.ok("Đã chuyển bài đăng vào thùng rác");
    }

    @GetMapping("/trash")
    public ResponseEntity<Page<PropertyResponseDTO>> getMyTrash(
            Authentication authentication,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        Long ownerId = extractUserId(authentication);
        return ResponseEntity.ok(propertyService.getMyDeletedProperties(ownerId, page, size));
    }

    @PutMapping("/{id}/restore")
    public ResponseEntity<String> restoreProperty(
            Authentication authentication,
            @PathVariable Long id) {

        Long ownerId = extractUserId(authentication);
        propertyService.restoreProperty(ownerId, id);
        return ResponseEntity.ok("Đã khôi phục bài đăng thành công!");
    }

    @DeleteMapping("/{id}/force")
    public ResponseEntity<String> hardDeleteProperty(
            Authentication authentication,
            @PathVariable Long id) {

        Long ownerId = extractUserId(authentication);
        propertyService.hardDeleteProperty(ownerId, id);
        return ResponseEntity.ok("Đã xóa vĩnh viễn bài đăng khỏi hệ thống!");
    }

    @GetMapping
    public ResponseEntity<Page<PropertyResponseDTO>> getMyProperties(
            Authentication authentication,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String transactionType
    ) {
        Long ownerId = extractUserId(authentication);
        return ResponseEntity.ok(
                propertyService.getMyProperties(ownerId, page, size, status, transactionType)
        );
    }
    @GetMapping("/quota/me")
    public ApiResponse<OwnerQuotaResponse> getMyQuota(Authentication authentication) {
        Long ownerId = extractUserId(authentication);

        OwnerQuota quota = ownerQuotaRepository.findById(ownerId)
                .orElse(OwnerQuota.builder()
                        .ownerId(ownerId)
                        .freePostsRemaining(0)
                        .role("USER")
                        .build());

        return ApiResponse.<OwnerQuotaResponse>builder()
                .result(OwnerQuotaResponse.builder()
                        .ownerId(quota.getOwnerId())
                        .freePostsRemaining(quota.getFreePostsRemaining() == null ? 0 : quota.getFreePostsRemaining())
                        .role(quota.getRole())
                        .build())
                .build();
    }


    // ================== Helper method ==================
    private Long extractUserId(Authentication authentication) {
        String userIdStr = (String) authentication.getPrincipal();
        return Long.valueOf(userIdStr);
    }
}