package com.homeverse.property.controller;

import com.homeverse.common.dto.ApiResponse;
import com.homeverse.property.dto.request.PropertyRequestDTO;
import com.homeverse.property.dto.response.PropertyResponseDTO;
import com.homeverse.property.service.PropertyService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/properties")
@RequiredArgsConstructor
public class PropertyController {

    private final PropertyService propertyService;

    @PostMapping
    public ApiResponse<PropertyResponseDTO> createProperty(
            @RequestBody PropertyRequestDTO request,
            @RequestHeader("Authorization") String token) {

        return ApiResponse.<PropertyResponseDTO>builder()
                .code(200)
                .message("Tạo bất động sản thành công")
                .result(propertyService.createProperty(request, token))
                .build();
    }

    @PutMapping("/{id}")
    public ApiResponse<PropertyResponseDTO> updateProperty(
            @PathVariable Long id,
            @RequestBody PropertyRequestDTO request,
            @RequestHeader("Authorization") String token) {

        return ApiResponse.<PropertyResponseDTO>builder()
                .code(200)
                .message("Cập nhật bất động sản thành công")
                .result(propertyService.updateProperty(id, request, token))
                .build();
    }

    @DeleteMapping("/{id}")
    public ApiResponse<String> deleteProperty(
            @PathVariable Long id,
            @RequestHeader("Authorization") String token) {

        propertyService.deleteProperty(id, token);
        return ApiResponse.<String>builder()
                .code(200)
                .message("Xóa bất động sản thành công")
                .build();
    }
    @PatchMapping("/{id}/status")
    public ApiResponse<String> updatePropertyStatus(
            @PathVariable Long id,
            @RequestParam String status,
            @RequestHeader("Authorization") String token) {

        propertyService.updatePropertyStatus(id, status, token);
        return ApiResponse.<String>builder()
                .code(200)
                .message("Cập nhật trạng thái thành công")
                .build();
    }

    // 🟢 API Dành riêng cho ADMIN: Lấy danh sách tin chờ duyệt
    @GetMapping("/admin/pending")
    public ApiResponse<Page<PropertyResponseDTO>> getPendingProperties(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestHeader("Authorization") String token) {

        return ApiResponse.<Page<PropertyResponseDTO>>builder()
                .code(200)
                .message("Lấy danh sách chờ duyệt thành công")
                .result(propertyService.getPendingProperties(page, size, token))
                .build();
    }

    @GetMapping("/{id}")
    public ApiResponse<PropertyResponseDTO> getPropertyById(@PathVariable Long id) {
        return ApiResponse.<PropertyResponseDTO>builder()
                .code(200)
                .result(propertyService.getPropertyById(id))
                .build();
    }

    @GetMapping
    public ApiResponse<Page<PropertyResponseDTO>> getAllProperties(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        return ApiResponse.<Page<PropertyResponseDTO>>builder()
                .code(200)
                .result(propertyService.getAllProperties(page, size))
                .build();
    }

    @GetMapping("/landlord/{landlordId}")
    public ApiResponse<Page<PropertyResponseDTO>> getPropertiesByLandlord(
            @PathVariable Long landlordId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        return ApiResponse.<Page<PropertyResponseDTO>>builder()
                .code(200)
                .result(propertyService.getPropertiesByLandlord(landlordId, page, size))
                .build();
    }

    // 🟢 API TÌM KIẾM NÂNG CAO (Hứng tất cả các param filter từ Frontend)
    // Ví dụ gọi API: /properties/search?keyword=Vinhome&minPrice=2000000&maxPrice=5000000&type=WHOLE
    @GetMapping("/search")
    public ApiResponse<Page<PropertyResponseDTO>> searchProperties(
            @RequestParam(required = false) Double lat,
            @RequestParam(required = false) Double lng,
            @RequestParam(required = false) Double radius,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice,
            @RequestParam(required = false) Double minArea,
            @RequestParam(required = false) Double maxArea,
            @RequestParam(required = false) List<Integer> bedroomList,
            @RequestParam(required = false) List<Integer> bathroomList,
            @RequestParam(required = false) List<String> directionList,
            @RequestParam(required = false) String furniture,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        Pageable pageable = PageRequest.of(page, size);

        Page<PropertyResponseDTO> result = propertyService.searchNearbyAdvanced(
                lat, lng, radius, keyword, type, minPrice, maxPrice,
                minArea, maxArea, bedroomList, bathroomList, directionList, furniture, pageable);

        return ApiResponse.<Page<PropertyResponseDTO>>builder()
                .code(200)
                .message("Tìm kiếm thành công")
                .result(result)
                .build();
    }
}