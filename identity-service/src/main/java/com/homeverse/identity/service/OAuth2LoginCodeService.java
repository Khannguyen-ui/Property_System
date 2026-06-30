package com.homeverse.identity.service;

public interface OAuth2LoginCodeService {
    String issueCode(String email);
    String consumeEmail(String code);
}