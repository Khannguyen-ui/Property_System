package com.homeverse.property.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.util.List;

@Data
public class ProjectCreateDTO {
    @NotBlank(message = "Tên dự án/khu trọ không được trống")
    private String name;

    private String description;

    @NotBlank(message = "Địa chỉ không được trống")
    private String address;

    @NotNull(message = "Vĩ độ (Lat) không được trống")
    private Double latitude;

    @NotNull(message = "Kinh độ (Long) không được trống")
    private Double longitude;

    @NotNull(message = "Loại hình dự án không được trống")
    private String projectType;

    private List<String> amenities;
}