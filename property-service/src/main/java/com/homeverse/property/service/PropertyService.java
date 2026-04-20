package com.homeverse.property.service;

import com.homeverse.property.dto.request.PropertyCreateDTO;
import com.homeverse.property.dto.response.PropertyResponseDTO;
import com.homeverse.property.dto.response.ReelsFeedResponse;
import com.homeverse.property.entity.Property;
import org.springframework.data.domain.Page;

import java.util.Optional;

public interface PropertyService {
    PropertyResponseDTO createProperty(Long ownerId, PropertyCreateDTO dto);
    PropertyResponseDTO updateProperty(Long ownerId, Long id, PropertyCreateDTO dto);
    void deleteProperty(Long ownerId, Long id);
    void hardDeleteProperty(Long ownerId, Long id);
    void restoreProperty(Long ownerId, Long id);
    Page<PropertyResponseDTO> getMyDeletedProperties(Long ownerId, int page, int size);
    Page<PropertyResponseDTO> getPublicProperties(int page, int size);
    PropertyResponseDTO getPublicPropertyDetail(Long id);

    ReelsFeedResponse getReelsFeed(Long currentUserId,String guestId,String cursor, int size);
      // API Xem Trang cá nhân
    org.springframework.data.domain.Page<com.homeverse.property.dto.response.PropertyResponseDTO> getPropertiesByOwnerId(Long ownerId, int page, int size);
}