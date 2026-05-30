package com.homeverse.aiworker.dto.common;

import lombok.Data;

@Data
public class ApiResponseDTO<T> {
    private int code;
    private String message;
    private T result;
}