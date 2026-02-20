package com.abitmanipulator.url_shortner.domain.models;

public record CreateShortUrlCmd(String originalUrl, Integer expirationInDays, boolean isPrivate, Long userId) {
}
