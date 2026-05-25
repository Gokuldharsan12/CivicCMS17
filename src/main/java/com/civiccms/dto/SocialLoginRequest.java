package com.civiccms.dto;

public record SocialLoginRequest(
        String name,
        String email,
        String provider,
        String providerId
) {}
