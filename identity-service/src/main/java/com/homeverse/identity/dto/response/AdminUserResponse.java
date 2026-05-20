package com.homeverse.identity.dto.response;

import com.homeverse.identity.entity.UserCredential;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class AdminUserResponse {

    private Long id;

    private String email;


    private UserCredential.Role role;

    private String kycStatus;

    private boolean active;

    private LocalDateTime createdAt;

    private Integer freePostsRemaining;
}