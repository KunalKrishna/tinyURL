package com.abitmanipulator.url_shortner.repository;

import com.abitmanipulator.url_shortner.TestcontainersConfiguration;
import com.abitmanipulator.url_shortner.domain.entities.ShortUrl;
import com.abitmanipulator.url_shortner.domain.entities.User;
import jakarta.persistence.EntityManager;
import org.hibernate.Hibernate;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * JPA slice against a real Postgres 17 (Testcontainers) with the Flyway migrations applied,
 * so queries run on the same SQL dialect and schema as production. Each test rolls back.
 * Assertions are relative to what is in the table, so they don't break when seed data changes.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE) // keep the container, no embedded DB
@Import(TestcontainersConfiguration.class)
class ShortUrlRepositoryTest {

    private static final long ADMIN_ID = 1L; // seeded by V2__insert_sample_data.sql
    private static final long SIVA_ID = 2L;

    @Autowired
    ShortUrlRepository repository;
    @Autowired
    EntityManager entityManager;

    private ShortUrl persist(long ownerId, boolean isPrivate) {
        var url = new ShortUrl();
        url.setShortKey(UUID.randomUUID().toString().substring(0, 8));
        url.setOriginalUrl("https://example.com/" + url.getShortKey());
        url.setIsPrivate(isPrivate);
        url.setClickCount(0L);
        url.setCreatedAt(Instant.now());
        url.setCreatedBy(entityManager.getReference(User.class, ownerId));
        return repository.saveAndFlush(url);
    }

    @Test
    void flywaySeedDataIsPresent() {
        assertThat(repository.count()).isPositive();
    }

    @Test
    void publicPageContainsNoPrivateUrls() {
        persist(SIVA_ID, true);
        long publicCount = repository.findAll().stream().filter(u -> !u.getIsPrivate()).count();

        var page = repository.findPublicShortUrls(PageRequest.of(0, 1000, Sort.by("createdAt").descending()));

        assertThat(page.getContent()).noneMatch(ShortUrl::getIsPrivate);
        assertThat(page.getTotalElements()).isEqualTo(publicCount);
    }

    @Test
    void findByShortKey() {
        var saved = persist(SIVA_ID, false);

        assertThat(repository.findByShortKey(saved.getShortKey())).get()
                .extracting(ShortUrl::getOriginalUrl).isEqualTo(saved.getOriginalUrl());
        assertThat(repository.existsByShortKey(saved.getShortKey())).isTrue();
        assertThat(repository.findByShortKey("missing")).isEmpty();
    }

    @Test
    void userPageContainsOnlyThatUsersUrls() {
        persist(SIVA_ID, true);
        persist(SIVA_ID, false);

        var page = repository.findByCreatedById(SIVA_ID, PageRequest.of(0, 100));

        assertThat(page.getContent()).hasSizeGreaterThanOrEqualTo(2)
                .allMatch(u -> u.getCreatedBy().getId() == SIVA_ID);
    }

    @Test
    void deleteOnlyRemovesUrlsOwnedByTheUser() {
        var sivas = persist(SIVA_ID, false);
        var admins = persist(ADMIN_ID, false);

        repository.deleteByIdInAndCreatedById(List.of(sivas.getId(), admins.getId()), SIVA_ID);
        entityManager.flush();
        entityManager.clear();

        assertThat(repository.findById(sivas.getId())).isEmpty();
        assertThat(repository.findById(admins.getId())).isPresent();
    }

    @Test
    void adminPagePaginatesAllUrlsWithOwnerLoaded() {
        long total = repository.count();

        var page = repository.findAllShortUrls(PageRequest.of(0, 5));
        entityManager.clear(); // owner must already be fetched (join fetch), not lazily loaded later

        assertThat(page.getContent()).hasSize((int) Math.min(5, total));
        assertThat(page.getTotalElements()).isEqualTo(total);
        assertThat(page.getContent()).allMatch(u -> u.getCreatedBy() == null || Hibernate.isInitialized(u.getCreatedBy()));
    }
}
