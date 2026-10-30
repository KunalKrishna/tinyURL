package com.abitmanipulator.url_shortner.domain.models;

import java.io.Serializable;

/**
 * DTO for {@link com.abitmanipulator.url_shortner.domain.entities.User}
 */
public record UserDto(Long id, String name) implements Serializable {
}