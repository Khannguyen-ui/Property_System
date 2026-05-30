package com.homeverse.recommendation.controller;

import com.homeverse.recommendation.model.RankingConfig;
import com.homeverse.recommendation.service.RankingConfigService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/recommend/admin/ranking-config")
@RequiredArgsConstructor
public class RankingConfigController {

    private final RankingConfigService rankingConfigService;

    @GetMapping
    public RankingConfig getConfig() {
        return rankingConfigService.getConfig();
    }

    @PutMapping
    public RankingConfig updateConfig(@RequestBody RankingConfig request) {
        return rankingConfigService.updateConfig(request);
    }
}