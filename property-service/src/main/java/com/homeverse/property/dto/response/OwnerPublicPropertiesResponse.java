package com.homeverse.property.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.domain.Page;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OwnerPublicPropertiesResponse {
    private Long ownerId;
    private Long totalActivePosts;
    private List<PropertyTypeCountDTO> typeCounts;
    private Page<PropertyResponseDTO> properties;
}