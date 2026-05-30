package com.homeverse.customer.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class KycRequestDTO {

    @NotBlank(message = "Số CCCD/CMND không được để trống")
    private String citizenId;

    @NotEmpty(message = "Vui lòng cung cấp hình ảnh CCCD")
    private List<String> citizenImages;
}