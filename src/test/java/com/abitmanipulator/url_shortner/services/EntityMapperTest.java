package com.abitmanipulator.url_shortner.services;

import com.abitmanipulator.url_shortner.domain.entities.ShortUrl;
import com.abitmanipulator.url_shortner.domain.entities.User;
import com.abitmanipulator.url_shortner.domain.models.PagedResult;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class EntityMapperTest {

    private final EntityMapper mapper = new EntityMapper();

    @Test
    void mapsShortUrlAndExposesOnlyIdAndNameOfOwner() {
        var owner = new User();
        owner.setId(3L);
        owner.setName("Siva");
        owner.setEmail("siva@gmail.com");
        owner.setPassword("$2a$hash");
        var url = new ShortUrl();
        url.setId(10L);
        url.setShortKey("abc123");
        url.setOriginalUrl("https://example.com");
        url.setIsPrivate(true);
        url.setClickCount(4L);
        url.setCreatedAt(Instant.EPOCH);
        url.setCreatedBy(owner);

        var dto = mapper.toShortUrlDto(url);

        assertThat(dto.shortKey()).isEqualTo("abc123");
        assertThat(dto.isPrivate()).isTrue();
        assertThat(dto.clickCount()).isEqualTo(4L);
        assertThat(dto.createdBy().id()).isEqualTo(3L);
        assertThat(dto.createdBy().name()).isEqualTo("Siva"); // UserDto has no email/password fields at all
    }

    @Test
    void anonymousUrlHasNoOwner() {
        var url = new ShortUrl();
        url.setShortKey("abc123");

        assertThat(mapper.toShortUrlDto(url).createdBy()).isNull();
    }

    @Test
    void pagedResultIsOneBased() {
        // Spring Data pages are 0-based; the UI shows 1-based page numbers
        var page = new PageImpl<>(List.of("a", "b"), PageRequest.of(1, 2), 5);

        var result = PagedResult.from(page);

        assertThat(result.pageNumber()).isEqualTo(2);
        assertThat(result.totalPages()).isEqualTo(3);
        assertThat(result.totalElements()).isEqualTo(5);
        assertThat(result.hasPrevious()).isTrue();
        assertThat(result.hasNext()).isTrue();
    }
}
