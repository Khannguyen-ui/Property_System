package com.homeverse.recommendation.repository;

import com.homeverse.recommendation.model.UserInterestProfile;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserInterestProfileRepository extends JpaRepository<UserInterestProfile, Long> {
}