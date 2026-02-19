package com.abitmanipulator.url_shortner.web.controller.dtos;

import jakarta.validation.constraints.NotBlank;

public record CreateShortUrlForm(
        @NotBlank(message = "Original URL is required.")
        String originalUrl) {
}
