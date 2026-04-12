package com.homeverse.property.dto.response;

import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class ProjectResponseDTO {
    private Long id;
    private String name;
    private String description;
    private String address;

    private Double latitude;
    private Double longitude;

    private String projectType;
    private List<String> amenities;

    private Long createdBy;
    private String status;
    private LocalDateTime createdAt;
}