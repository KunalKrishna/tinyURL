package com.abitmanipulator.url_shortner.domain.models;

public record CreateUserCmd(
        String email,
        String password,
        String name,
        Role role
) {
}
