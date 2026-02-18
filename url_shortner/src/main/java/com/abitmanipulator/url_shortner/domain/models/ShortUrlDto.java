package com.abitmanipulator.url_shortner.domain.models;

import java.io.Serializable;
import java.time.Instant;

/**
 * DTO for {@link com.abitmanipulator.url_shortner.domain.entities.ShortUrl}
 */
public record ShortUrlDto(Long id, String shortKey, String originalUrl, Boolean isPrivate, Instant expiresAt,
                          UserDto createdBy, Long clickCount, Instant createdAt) implements Serializable {
}