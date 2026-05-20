package com.homeverse.property.dto.response;

import com.homeverse.property.entity.Property;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PropertyTypeCountDTO {
    private Property.PropertyType propertyType;
    private Long total;
}