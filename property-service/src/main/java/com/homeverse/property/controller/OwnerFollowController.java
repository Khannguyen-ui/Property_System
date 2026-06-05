package com.homeverse.property.controller;

import com.homeverse.property.dto.response.OwnerFollowResponse;
import com.homeverse.property.service.OwnerFollowService;
import lombok.RequiredArgsConstructor;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/owners")
@RequiredArgsConstructor
public class OwnerFollowController {

    private final OwnerFollowService ownerFollowService;

    @PostMapping("/{ownerId}/follow")
    public ResponseEntity<OwnerFollowResponse> toggleFollow(
            Authentication authentication,
            @RequestHeader(value = "X-User-Id", required = false) Long headerUserId,
            @PathVariable Long ownerId) {
        Long followerId = extractUserId(authentication);

        if (followerId == null) {
            followerId = headerUserId;
        }

        return ResponseEntity.ok(
                ownerFollowService.toggleFollow(followerId, ownerId));
    }

    @GetMapping("/{ownerId}/followers/count")
    public ResponseEntity<Long> countFollowers(@PathVariable Long ownerId) {
        return ResponseEntity.ok(
                ownerFollowService.countFollowers(ownerId));
    }

    @GetMapping("/{ownerId}/is-following")
    public ResponseEntity<Boolean> isFollowing(
            Authentication authentication,
            @RequestHeader(value = "X-User-Id", required = false) Long headerUserId,
            @PathVariable Long ownerId) {
        Long followerId = extractUserId(authentication);

        if (followerId == null) {
            followerId = headerUserId;
        }

        return ResponseEntity.ok(
                ownerFollowService.isFollowing(followerId, ownerId));
    }

    private Long extractUserId(Authentication authentication) {
        if (authentication == null || authentication.getPrincipal() == null) {
            return null;
        }

        try {
            return Long.valueOf(authentication.getPrincipal().toString());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    @GetMapping("/following/{followerId}")
    public ResponseEntity<List<Long>> getFollowedOwnerIds(
            @PathVariable Long followerId) {
        return ResponseEntity.ok(
                ownerFollowService.getFollowedOwnerIds(followerId));
    }
}