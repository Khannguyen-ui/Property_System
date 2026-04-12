package com.homeverse.property.service;

import com.homeverse.property.dto.response.PropertyResponseDTO;
import org.springframework.data.domain.Page;

public interface AdminPropertyService {
    // Hàm sếp đã có
    void updatePropertyStatus(Long adminId, Long id, String statusStr);

    // Các hàm mới bổ sung
    Page<PropertyResponseDTO> getAllProperties(int page, int size, String status);
    PropertyResponseDTO getPropertyDetail(Long id);
    void deleteProperty(Long adminId, Long id);
    Page<PropertyResponseDTO> getDeletedProperties(int page, int size);
    void restoreProperty(Long adminId, Long id);
    void hardDeleteProperty(Long adminId, Long id);
}