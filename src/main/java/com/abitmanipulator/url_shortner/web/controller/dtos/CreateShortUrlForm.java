package com.abitmanipulator.url_shortner.web.controller.dtos;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

public record CreateShortUrlForm(
        @NotBlank(message = "Original URL is required.")
        String originalUrl,

        @Min(1)
            @Max(365)
        Integer expirationInDays,

        Boolean isPrivate

        ) {
}
