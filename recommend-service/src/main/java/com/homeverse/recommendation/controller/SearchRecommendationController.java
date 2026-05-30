package com.homeverse.recommendation.controller;

import com.homeverse.recommendation.dto.SearchSuggestionResponse;
import com.homeverse.recommendation.dto.SearchTrackRequest;
import com.homeverse.recommendation.service.SearchRecommendationService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/recommend/search")
@RequiredArgsConstructor
public class SearchRecommendationController {

    private final SearchRecommendationService searchRecommendationService;

    @PostMapping("/track")
    public void track(@RequestBody SearchTrackRequest request) {
        searchRecommendationService.track(request);
    }

    @GetMapping("/suggest")
    public SearchSuggestionResponse suggest(
            @RequestParam(defaultValue = "") String keyword
    ) {
        return searchRecommendationService.suggest(keyword);
    }

    @GetMapping("/top")
    public List<String> getTopKeywords() {
        return searchRecommendationService.getTopKeywords();
    }
}