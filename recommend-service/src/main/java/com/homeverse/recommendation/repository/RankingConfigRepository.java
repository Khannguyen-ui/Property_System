package com.homeverse.recommendation.repository;

import com.homeverse.recommendation.model.RankingConfig;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RankingConfigRepository extends JpaRepository<RankingConfig, String> {
}