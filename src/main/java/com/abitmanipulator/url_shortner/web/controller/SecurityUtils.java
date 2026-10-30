package com.abitmanipulator.url_shortner.web.controller;

import com.abitmanipulator.url_shortner.domain.entities.User;
import com.abitmanipulator.url_shortner.repository.UserRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service
public class SecurityUtils {

    private final UserRepository userRepository;

    public SecurityUtils(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated()) {
            String email = authentication.getName();
            return userRepository.findByEmail(email).orElse(null);
        }
        return null;
    }

    public Long getCurrentUserId(){
        User user = getCurrentUser();
        return user != null ? user.getId() : null;

    }
}