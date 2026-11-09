package com.ecommerce.security;

public record AuthenticatedUserPrincipal(
        Long userId,
        String name,
        String email,
        String role,
        String planTier,
        String tokenJti,
        String authSource
) {}

