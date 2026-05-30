package com.homeverse.search.service;

import com.homeverse.search.dto.request.PropertySearchRequestDTO;
import com.homeverse.search.dto.response.PropertySearchItemDTO;
import com.homeverse.search.dto.response.WardPriceDTO;
import org.springframework.data.domain.Page;

import java.util.List;

public interface PropertySearchService {
    Page<PropertySearchItemDTO> advancedSearch(PropertySearchRequestDTO request);
    List<PropertySearchItemDTO> findByIds(List<Long> ids);

}