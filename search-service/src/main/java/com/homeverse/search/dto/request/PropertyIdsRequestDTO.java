package com.homeverse.search.dto.request;

import lombok.Data;

import java.util.List;

@Data
public class PropertyIdsRequestDTO {
    private List<Long> ids;
}