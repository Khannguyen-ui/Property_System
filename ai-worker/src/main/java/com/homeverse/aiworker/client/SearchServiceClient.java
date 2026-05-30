package com.homeverse.aiworker.client;

import com.homeverse.aiworker.dto.common.ApiResponseDTO;
import com.homeverse.aiworker.dto.search.PropertyIdsRequestDTO;
import com.homeverse.aiworker.dto.search.SearchPropertyItemDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class SearchServiceClient {

    private final RestTemplate restTemplate;

    @Value("${services.search-service-url}")
    private String searchServiceUrl;

    public List<SearchPropertyItemDTO> getPropertiesByIds(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<PropertyIdsRequestDTO> entity =
                    new HttpEntity<>(new PropertyIdsRequestDTO(ids), headers);

            ResponseEntity<ApiResponseDTO<List<SearchPropertyItemDTO>>> response =
                    restTemplate.exchange(
                            searchServiceUrl + "/search/properties/by-ids",
                            HttpMethod.POST,
                            entity,
                            new ParameterizedTypeReference<>() {}
                    );

            if (response.getBody() == null || response.getBody().getResult() == null) {
                return List.of();
            }

            return response.getBody().getResult();

        } catch (Exception e) {
            log.error("Lỗi gọi search-service getPropertiesByIds ids={}", ids, e);
            return List.of();
        }
    }
}