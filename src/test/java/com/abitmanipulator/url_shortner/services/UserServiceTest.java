package com.abitmanipulator.url_shortner.services;

import com.abitmanipulator.url_shortner.domain.entities.User;
import com.abitmanipulator.url_shortner.domain.models.CreateUserCmd;
import com.abitmanipulator.url_shortner.domain.models.Role;
import com.abitmanipulator.url_shortner.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    UserRepository userRepository;

    // real encoder: cheap at strength 4 and proves the stored value is a hash of the raw password
    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder(4);

    @Test
    void createUserStoresHashedPassword() {
        when(userRepository.existsByEmail("new@example.com")).thenReturn(false);

        new UserService(userRepository, passwordEncoder)
                .createUser(new CreateUserCmd("new@example.com", "s3cret", "New", Role.ROLE_USER));

        var captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        User saved = captor.getValue();
        assertThat(saved.getEmail()).isEqualTo("new@example.com");
        assertThat(saved.getRole()).isEqualTo(Role.ROLE_USER);
        assertThat(saved.getCreatedAt()).isNotNull();
        assertThat(saved.getPassword()).isNotEqualTo("s3cret");
        assertThat(passwordEncoder.matches("s3cret", saved.getPassword())).isTrue();
    }

    @Test
    void createUserRejectsDuplicateEmail() {
        when(userRepository.existsByEmail("taken@example.com")).thenReturn(true);
        var service = new UserService(userRepository, passwordEncoder);

        assertThatThrownBy(() -> service.createUser(
                new CreateUserCmd("taken@example.com", "x", "X", Role.ROLE_USER)))
                .hasMessageContaining("Email already exists");
        verify(userRepository, never()).save(any());
    }

    @Test
    void userDetailsCarryEmailHashAndRole() {
        var user = new User();
        user.setEmail("siva@gmail.com");
        user.setPassword("$2a$hash");
        user.setRole(Role.ROLE_ADMIN);
        when(userRepository.findByEmail("siva@gmail.com")).thenReturn(Optional.of(user));

        var details = new SecurityUserDetailsService(userRepository).loadUserByUsername("siva@gmail.com");

        assertThat(details.getUsername()).isEqualTo("siva@gmail.com");
        assertThat(details.getPassword()).isEqualTo("$2a$hash");
        assertThat(details.getAuthorities()).extracting(Object::toString).containsExactly("ROLE_ADMIN");
    }

    @Test
    void unknownEmailFailsLogin() {
        when(userRepository.findByEmail("ghost@example.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> new SecurityUserDetailsService(userRepository).loadUserByUsername("ghost@example.com"))
                .isInstanceOf(UsernameNotFoundException.class);
    }
}
