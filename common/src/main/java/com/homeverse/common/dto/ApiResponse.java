package com.homeverse.common.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL) // Chỉ trả về các trường có giá trị (không null) để tiết kiệm băng thông
public class ApiResponse<T> {

    @Builder.Default
    private int code = 1000;

    private String message;

    private T result;
}