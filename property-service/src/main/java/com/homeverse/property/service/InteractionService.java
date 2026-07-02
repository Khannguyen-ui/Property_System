package com.homeverse.property.service;

import com.homeverse.property.dto.response.InteractionPropertyDTO;
import org.springframework.data.domain.Page;

public interface InteractionService {

    boolean toggleLike(Long userId, String guestId, Long propertyId);

    boolean toggleSave(Long userId, String guestId, Long propertyId);

    Page<InteractionPropertyDTO> getLikedProperties(Long userId, String guestId, int page, int size);

    Page<InteractionPropertyDTO> getSavedProperties(Long userId, String guestId, int page, int size);
    void trackView(Long userId, String guestId, Long propertyId);
    void shareProperty(Long userId, Long propertyId);
    void trackClick(Long userId, String guestId, Long propertyId);
    void contactProperty(
            Long userId,
            Long propertyId
    );
}