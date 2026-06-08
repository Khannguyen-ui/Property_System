package com.homeverse.identity.dto.response;

import com.homeverse.identity.entity.UserCredential;
import lombok.*;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminUserResponse {
    private Long id;
    private String publicId;
    private String email;
    private String fullName;
    private UserCredential.Role role;
    private String kycStatus;
    private boolean active;
    private LocalDateTime createdAt;
    private Integer freePostsRemaining;
}