package com.homeverse.recommendation.controller;

import com.homeverse.recommendation.dto.SourceAnalyticsTrackRequest;
import com.homeverse.recommendation.dto.SourceCTRResponse;
import com.homeverse.recommendation.service.SourceAnalyticsService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/recommend/analytics/source")
@RequiredArgsConstructor
public class SourceAnalyticsController {

    private final SourceAnalyticsService sourceAnalyticsService;

    @PostMapping("/track")
    public void track(@RequestBody SourceAnalyticsTrackRequest request) {
        sourceAnalyticsService.track(request);
    }

    @GetMapping("/ctr")
    public List<SourceCTRResponse> getCtr() {
        return sourceAnalyticsService.getAllSourcesCtr();
    }

    @GetMapping("/best")
    public SourceCTRResponse bestSource() {
        return sourceAnalyticsService.getBestSource();
    }

    @GetMapping("/worst")
    public SourceCTRResponse worstSource() {
        return sourceAnalyticsService.getWorstSource();
    }
}