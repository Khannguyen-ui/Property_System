package com.homeverse.recommendation.controller;

import com.homeverse.recommendation.model.UserInterestProfile;
import com.homeverse.recommendation.service.UserInterestProfileService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/recommend/users")
@RequiredArgsConstructor
public class UserInterestProfileController {

    private final UserInterestProfileService userInterestProfileService;

    @GetMapping("/{userId}/interest-profile")
    public UserInterestProfile getProfile(@PathVariable Long userId) {
        return userInterestProfileService.getProfile(userId);
    }
}