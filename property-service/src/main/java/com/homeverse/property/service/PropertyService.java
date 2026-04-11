package com.homeverse.property.service;

import com.homeverse.property.dto.request.PropertyRequestDTO;
import com.homeverse.property.dto.response.PropertyResponseDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.util.List;

public interface PropertyService {
    PropertyResponseDTO createProperty(PropertyRequestDTO request, String token);

    PropertyResponseDTO updateProperty(Long id, PropertyRequestDTO request, String token);

    void deleteProperty(Long id, String token);

    PropertyResponseDTO getPropertyById(Long id);
    void updatePropertyStatus(Long id, String status, String token);

    Page<PropertyResponseDTO> getPendingProperties(int page, int size, String token);
    Page<PropertyResponseDTO> getAllProperties(int page, int size);

    Page<PropertyResponseDTO> getPropertiesByLandlord(Long landlordId, int page, int size);

    // BỔ SUNG: Khai báo hàm tìm kiếm nâng cao để Impl có thể @Override
    Page<PropertyResponseDTO> searchNearbyAdvanced(
            Double lat, Double lng, Double radius,
            String keyword, String type,
            BigDecimal minPrice, BigDecimal maxPrice,
            Double minArea, Double maxArea,
            List<Integer> bedroomList, List<Integer> bathroomList,
            List<String> directionList, String furniture,
            Pageable pageable);
}