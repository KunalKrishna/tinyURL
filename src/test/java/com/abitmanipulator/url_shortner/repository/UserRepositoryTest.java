package com.abitmanipulator.url_shortner.repository;

import com.abitmanipulator.url_shortner.TestcontainersConfiguration;
import com.abitmanipulator.url_shortner.domain.entities.User;
import com.abitmanipulator.url_shortner.domain.models.Role;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jdbc.test.autoconfigure.JdbcTest;
import org.springframework.context.annotation.Import;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * UserRepository is hand-written SQL on JdbcClient (not Spring Data), so it gets a JDBC slice:
 * DataSource + JdbcClient + Flyway, and only this one repository bean imported.
 */
@JdbcTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({TestcontainersConfiguration.class, UserRepository.class})
class UserRepositoryTest {

    @Autowired
    UserRepository repository;

    @Test
    void findsSeededAdminWithRole() {
        var admin = repository.findByEmail("admin@gmail.com");

        assertThat(admin).get().satisfies(u -> {
            assertThat(u.getRole()).isEqualTo(Role.ROLE_ADMIN);
            assertThat(u.getPassword()).startsWith("$2a$"); // V3 replaced the plain-text seed passwords
            assertThat(u.getCreatedAt()).isNotNull();
        });
    }

    @Test
    void saveThenFind() {
        var user = new User();
        user.setEmail("repo-test@example.com");
        user.setPassword("$2a$hash");
        user.setName("Repo Test");
        user.setRole(Role.ROLE_USER);
        user.setCreatedAt(Instant.now());

        repository.save(user);

        assertThat(repository.existsByEmail("repo-test@example.com")).isTrue();
        var found = repository.findByEmail("repo-test@example.com").orElseThrow();
        assertThat(found.getId()).isNotNull();
        assertThat(repository.findById(found.getId())).get()
                .extracting(User::getName).isEqualTo("Repo Test");
    }

    @Test
    void unknownUserIsEmpty() {
        assertThat(repository.findByEmail("ghost@example.com")).isEmpty();
        assertThat(repository.existsByEmail("ghost@example.com")).isFalse();
        assertThat(repository.findById(-1L)).isEmpty();
    }
}
