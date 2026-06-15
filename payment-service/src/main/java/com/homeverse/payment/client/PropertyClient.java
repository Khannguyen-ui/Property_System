package com.homeverse.payment.client;

import com.homeverse.payment.dto.PropertyResponseDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component
@RequiredArgsConstructor
public class PropertyClient {

    private final RestTemplate restTemplate;

    @Value("${services.property-service-url:http://property-service:8086}")
    private String propertyServiceUrl;

    public PropertyResponseDTO getProperty(Long propertyId) {
        return restTemplate.getForObject(
                propertyServiceUrl + "/public/properties/internal/" + propertyId,
                PropertyResponseDTO.class
        );
    }
}