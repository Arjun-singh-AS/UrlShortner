package com.example.URLShortner;

import java.time.Instant;

public record UrlData(
        String shortCode,
        String originalUrl,
        Instant createdAt,
        Instant expiresAt,
        long clickCount) {

    public boolean isExpired() {
        return expiresAt != null && !expiresAt.isAfter(Instant.now());
    }
}
