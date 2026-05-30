package com.homeverse.search.controller;

import com.homeverse.common.dto.ApiResponse;
import com.homeverse.search.dto.request.PropertySearchRequestDTO;
import com.homeverse.search.dto.response.PropertySearchItemDTO;
import com.homeverse.search.service.PropertySearchService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/search")
@RequiredArgsConstructor
public class SearchController {

    private final PropertySearchService searchService;

    @GetMapping("/properties")
    public ApiResponse<Page<PropertySearchItemDTO>> searchProperties(
            @ModelAttribute PropertySearchRequestDTO request,

            @AuthenticationPrincipal String currentUserId) {

        if (currentUserId != null) {
            log.info(" User ID [{}] đang thực hiện tìm kiếm", currentUserId);
            // Có thể truyền currentUserId xuống Service nếu cần lọc gì đó cá nhân hóa
        } else {
            log.info("Khách vãng lai đang thực hiện tìm kiếm");
        }

        Page<PropertySearchItemDTO> result = searchService.advancedSearch(request);
        return ApiResponse.<Page<PropertySearchItemDTO>>builder()
                .result(result)
                .build();
    }
}