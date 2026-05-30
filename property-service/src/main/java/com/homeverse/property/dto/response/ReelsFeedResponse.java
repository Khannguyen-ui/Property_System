package com.homeverse.property.dto.response;

import com.homeverse.property.dto.response.PropertyReelResponseDTO;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
public class ReelsFeedResponse {
    private List<PropertyReelResponseDTO> items;

    private String nextCursor;

    public boolean hasNext() {
        return nextCursor != null && !nextCursor.isEmpty();
    }
}