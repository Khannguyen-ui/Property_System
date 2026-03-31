package com.homeverse.identity.dto.request;

import lombok.Data;

@Data
public class ChangeEmailRequest {

    private String password;
    private String newEmail;
}