package com.homeverse.property.service;

import com.homeverse.property.dto.request.PropertyCreateDTO;
import com.homeverse.property.dto.response.OwnerPublicPropertiesResponse;
import com.homeverse.property.dto.response.PropertyResponseDTO;
import com.homeverse.property.dto.response.ReelsFeedResponse;
import org.springframework.data.domain.Page;

public interface PropertyService {
    PropertyResponseDTO createProperty(Long ownerId, PropertyCreateDTO dto);

    PropertyResponseDTO updateProperty(Long ownerId, Long id, PropertyCreateDTO dto);

    void deleteProperty(Long ownerId, Long id);

    void hardDeleteProperty(Long ownerId, Long id);

    void restoreProperty(Long ownerId, Long id);

    Page<PropertyResponseDTO> getMyDeletedProperties(Long ownerId, int page, int size);

    Page<PropertyResponseDTO> getPublicProperties(int page, int size);

    PropertyResponseDTO getPublicPropertyDetail(Long id);

    ReelsFeedResponse getReelsFeed(Long currentUserId, String guestId, String cursor, int size);

    // API cũ: chỉ lấy danh sách bài ACTIVE của owner
    Page<PropertyResponseDTO> getPropertiesByOwnerId(
            Long ownerId,
            int page,
            int size,
            String transactionType
    );

    // API mới: lấy bài + tổng bài + thống kê theo loại
    OwnerPublicPropertiesResponse getOwnerPublicProperties(
            Long ownerId,
            int page,
            int size,
            String propertyType
    );
}