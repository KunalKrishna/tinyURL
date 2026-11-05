package com.abitmanipulator.url_shortner.services;

import com.abitmanipulator.url_shortner.AppConfigProperties;
import com.abitmanipulator.url_shortner.domain.entities.ShortUrl;
import com.abitmanipulator.url_shortner.domain.entities.User;
import com.abitmanipulator.url_shortner.domain.models.CreateShortUrlCmd;
import com.abitmanipulator.url_shortner.domain.models.Role;
import com.abitmanipulator.url_shortner.domain.models.ShortUrlDto;
import com.abitmanipulator.url_shortner.repository.ShortUrlRepository;
import com.abitmanipulator.url_shortner.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Pure unit test: no Spring context, collaborators are Mockito mocks.
 * EntityMapper and AppConfigProperties are simple enough to use the real thing.
 */
@ExtendWith(MockitoExtension.class)
class ShortUrlServiceTest {

    private static final int DEFAULT_EXPIRY_DAYS = 30;

    @Mock
    ShortUrlRepository shortUrlRepository;
    @Mock
    UserRepository userRepository;

    ShortUrlService service;

    @BeforeEach
    void setUp() {
        service = serviceWith(false);
    }

    private ShortUrlService serviceWith(boolean validateOriginalUrl) {
        var props = new AppConfigProperties("http://localhost:8080", DEFAULT_EXPIRY_DAYS, validateOriginalUrl, 10);
        return new ShortUrlService(shortUrlRepository, new EntityMapper(), props, userRepository);
    }

    private static User user(long id) {
        var user = new User();
        user.setId(id);
        user.setName("user-" + id);
        user.setEmail("user" + id + "@example.com");
        user.setRole(Role.ROLE_USER);
        return user;
    }

    private ShortUrl savedShortUrl() {
        var captor = ArgumentCaptor.forClass(ShortUrl.class);
        verify(shortUrlRepository).save(captor.capture());
        return captor.getValue();
    }

    @Nested
    class CreateShortUrl {

        @Test
        void anonymousUrlIsAlwaysPublicWithDefaultExpiry() {
            when(shortUrlRepository.existsByShortKey(anyString())).thenReturn(false);
            Instant before = Instant.now();

            // anonymous users cannot choose privacy or expiry: both requested values are ignored
            ShortUrlDto dto = service.createShortUrl(new CreateShortUrlCmd("https://example.com", 7, true, null));

            assertThat(dto.isPrivate()).isFalse();
            assertThat(dto.createdBy()).isNull();
            assertThat(dto.clickCount()).isZero();
            assertThat(dto.expiresAt())
                    .isBetween(before.plus(DEFAULT_EXPIRY_DAYS, ChronoUnit.DAYS),
                               Instant.now().plus(DEFAULT_EXPIRY_DAYS, ChronoUnit.DAYS));
            assertThat(savedShortUrl().getOriginalUrl()).isEqualTo("https://example.com");
            verifyNoInteractions(userRepository);
        }

        @Test
        void loggedInUserChoosesPrivacyAndExpiry() {
            when(shortUrlRepository.existsByShortKey(anyString())).thenReturn(false);
            when(userRepository.findById(2L)).thenReturn(Optional.of(user(2L)));
            Instant before = Instant.now();

            ShortUrlDto dto = service.createShortUrl(new CreateShortUrlCmd("https://example.com", 7, true, 2L));

            assertThat(dto.isPrivate()).isTrue();
            assertThat(dto.createdBy().id()).isEqualTo(2L);
            assertThat(dto.expiresAt()).isBetween(before.plus(7, ChronoUnit.DAYS), Instant.now().plus(7, ChronoUnit.DAYS));
        }

        @Test
        void loggedInUserWithoutExpiryGetsDefault() {
            when(shortUrlRepository.existsByShortKey(anyString())).thenReturn(false);
            when(userRepository.findById(2L)).thenReturn(Optional.of(user(2L)));
            Instant before = Instant.now();

            ShortUrlDto dto = service.createShortUrl(new CreateShortUrlCmd("https://example.com", null, false, 2L));

            assertThat(dto.expiresAt()).isAfterOrEqualTo(before.plus(DEFAULT_EXPIRY_DAYS, ChronoUnit.DAYS));
        }

        @Test
        void regeneratesKeyWhenItAlreadyExists() {
            // first generated key collides with an existing row, the second one is free
            when(shortUrlRepository.existsByShortKey(anyString())).thenReturn(true, false);

            service.createShortUrl(new CreateShortUrlCmd("https://example.com", null, false, null));

            verify(shortUrlRepository, times(2)).existsByShortKey(anyString());
            assertThat(savedShortUrl().getShortKey()).hasSize(6);
        }

        @Test
        void rejectsUnreachableUrlWhenValidationIsOn() {
            // "not-a-url" is not an absolute URI, so the validator fails before any network call
            var validatingService = serviceWith(true);

            assertThatThrownBy(() -> validatingService.createShortUrl(
                    new CreateShortUrlCmd("not-a-url", null, false, null)))
                    .hasMessageContaining("Invalid URL");
            verify(shortUrlRepository, never()).save(any());
        }
    }

    @Nested
    class AccessOriginalUrl {

        private ShortUrl shortUrl(Instant expiresAt, boolean isPrivate, User owner) {
            var s = new ShortUrl();
            s.setId(1L);
            s.setShortKey("abc123");
            s.setOriginalUrl("https://example.com");
            s.setExpiresAt(expiresAt);
            s.setIsPrivate(isPrivate);
            s.setCreatedBy(owner);
            s.setClickCount(5L);
            s.setCreatedAt(Instant.now());
            return s;
        }

        @Test
        void unknownKeyIsEmpty() {
            when(shortUrlRepository.findByShortKey("nope")).thenReturn(Optional.empty());

            assertThat(service.accessOriginalUrl("nope", null)).isEmpty();
        }

        @Test
        void expiredUrlIsEmptyAndNotCounted() {
            var url = shortUrl(Instant.now().minus(1, ChronoUnit.MINUTES), false, null);
            when(shortUrlRepository.findByShortKey("abc123")).thenReturn(Optional.of(url));

            assertThat(service.accessOriginalUrl("abc123", null)).isEmpty();
            assertThat(url.getClickCount()).isEqualTo(5L);
            verify(shortUrlRepository, never()).save(any());
        }

        @Test
        void urlWithoutExpiryNeverExpires() {
            when(shortUrlRepository.findByShortKey("abc123")).thenReturn(Optional.of(shortUrl(null, false, null)));

            assertThat(service.accessOriginalUrl("abc123", null)).isPresent();
        }

        @Test
        void privateUrlIsHiddenFromOtherUsersAndAnonymous() {
            var url = shortUrl(null, true, user(1L));
            when(shortUrlRepository.findByShortKey("abc123")).thenReturn(Optional.of(url));

            assertThat(service.accessOriginalUrl("abc123", 2L)).isEmpty();
            assertThat(service.accessOriginalUrl("abc123", null)).isEmpty();
        }

        @Test
        void ownerCanOpenPrivateUrlAndClickIsCounted() {
            var url = shortUrl(Instant.now().plus(1, ChronoUnit.DAYS), true, user(1L));
            when(shortUrlRepository.findByShortKey("abc123")).thenReturn(Optional.of(url));

            var result = service.accessOriginalUrl("abc123", 1L);

            assertThat(result).get().extracting(ShortUrlDto::originalUrl).isEqualTo("https://example.com");
            assertThat(url.getClickCount()).isEqualTo(6L);
            verify(shortUrlRepository).save(url);
        }
    }

    @Nested
    class DeleteUserShortUrls {

        @Test
        void deletesOnlyForTheGivenUser() {
            service.deleteUserShortUrls(List.of(1L, 2L), 7L);

            verify(shortUrlRepository).deleteByIdInAndCreatedById(List.of(1L, 2L), 7L);
        }

        @Test
        void ignoresEmptyIdsOrMissingUser() {
            service.deleteUserShortUrls(List.of(), 7L);
            service.deleteUserShortUrls(null, 7L);
            service.deleteUserShortUrls(List.of(1L), null);

            verifyNoInteractions(shortUrlRepository);
        }
    }

    @Test
    void generatedKeyIsSixAlphanumericChars() {
        assertThat(ShortUrlService.generateRandomShortKey()).matches("[a-zA-Z0-9]{6}");
    }
}
