package com.abitmanipulator.url_shortner.services;

import com.abitmanipulator.url_shortner.AppConfigProperties;
import com.abitmanipulator.url_shortner.domain.entities.ShortUrl;
import com.abitmanipulator.url_shortner.domain.models.CreateShortUrlCmd;
import com.abitmanipulator.url_shortner.domain.models.PagedResult;
import com.abitmanipulator.url_shortner.domain.models.ShortUrlDto;
import com.abitmanipulator.url_shortner.repository.ShortUrlRepository;
import com.abitmanipulator.url_shortner.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class ShortUrlService {

    private final ShortUrlRepository shortUrlRepository;
    private final EntityMapper entityMapper;
    private final AppConfigProperties properties;
    private final UserRepository userRepository;

    public ShortUrlService(ShortUrlRepository shortUrlRepository, EntityMapper entityMapper, AppConfigProperties properties, UserRepository userRepository) {
        this.shortUrlRepository = shortUrlRepository;
        this.entityMapper = entityMapper;
        this.properties = properties;
        this.userRepository = userRepository;
    }

    public PagedResult<ShortUrlDto> findAllPublicShortUrls(int pageNo, int pageSize) {
        Pageable pageable = getPageable(pageNo, pageSize);
        Page<ShortUrlDto> shortUrlDtoPage = shortUrlRepository.findPublicShortUrls(pageable)
                .map(entityMapper::toShortUrlDto);
        return PagedResult.from(shortUrlDtoPage);
    }

    private Pageable getPageable(int pageNo, int pageSize) {
        pageNo = (pageNo > 1)? pageNo - 1 : 0;
        return PageRequest.of(pageNo, pageSize, Sort.by("createdAt").descending()); // Sort.by(Sort.Direction.DESC, "createdAt");
    }

    @Transactional
    public Optional<ShortUrlDto> accessOriginalUrl(String shortKey, Long userId) {
        Optional<ShortUrl> shortUrlOpt = shortUrlRepository.findByShortKey(shortKey);
        if(shortUrlOpt.isEmpty()) {
            return Optional.empty();
        }
        ShortUrl shortUrl = shortUrlOpt.get();
        if(shortUrl.getExpiresAt() != null && shortUrl.getExpiresAt().isBefore(Instant.now())) {
            return Optional.empty();
        }
        if(shortUrl.getIsPrivate() != null && shortUrl.getCreatedBy() != null && !Objects.equals(shortUrl.getCreatedBy().getId(), userId)) {
            return Optional.empty();
        }
        shortUrl.setClickCount(shortUrl.getClickCount() + 1);
        shortUrlRepository.save(shortUrl);
        return shortUrlOpt.map(entityMapper::toShortUrlDto);
    }

    public PagedResult<ShortUrlDto> getUserShortUrls(Long userId, int pageNo, int pageSize) {
        Pageable pageable = getPageable(pageNo, pageSize);
        var shortUrlsPage = shortUrlRepository.findByCreatedById(userId, pageable).map(entityMapper::toShortUrlDto);
        return PagedResult.from(shortUrlsPage);
    }

    @Transactional
    public void deleteUserShortUrls(List<Long> ids, Long userId) {
        if (ids != null && !ids.isEmpty() && userId != null) {
            shortUrlRepository.deleteByIdInAndCreatedById(ids, userId);
        }
    }

    public PagedResult<ShortUrlDto> findAllShortUrls(int pageNo, int pageSize) {
        Pageable pageable = getPageable(pageNo, pageSize);
        Page<ShortUrlDto> shortUrlDtoPage = shortUrlRepository.findAllShortUrls(pageable)
                .map(entityMapper::toShortUrlDto);
        return PagedResult.from(shortUrlDtoPage);
    }

    @Transactional
    public ShortUrlDto createShortUrl(CreateShortUrlCmd cmd) {
        if(properties.validateOriginalUrl()) {
            boolean urlExists = UrlExistenceValidator.isUrlExists(cmd.originalUrl());
            if(!urlExists) {
                throw new RuntimeException("Invalid URL : "+ cmd.originalUrl() );
            }
        }

        var shortKey = generateRandomShortKey();
        var shortUrl = new ShortUrl();
        shortUrl.setOriginalUrl(cmd.originalUrl());
        shortUrl.setShortKey(shortKey);
        if(cmd.userId() == null) {
            shortUrl.setCreatedBy(null);
            shortUrl.setIsPrivate(false);
            shortUrl.setExpiresAt(Instant.now().plus(properties.defaultExpiryDays(), java.time.temporal.ChronoUnit.DAYS));
        } else {
            shortUrl.setCreatedBy(userRepository.findById(cmd.userId()).orElseThrow());
            shortUrl.setIsPrivate(cmd.isPrivate());
            shortUrl.setExpiresAt(cmd.expirationInDays() != null ?
                                    Instant.now().plus(properties.defaultExpiryDays() , java.time.temporal.ChronoUnit.DAYS):
                                    null
                                );
        }
        shortUrl.setClickCount(0l);
        shortUrl.setCreatedAt(Instant.now());
        shortUrlRepository.save(shortUrl);
        return entityMapper.toShortUrlDto(shortUrl);
    }

    private String generateUniqueShortKey() {
        String shortKey ;
        do{
            shortKey = generateRandomShortKey();
        } while(shortUrlRepository.existsByShortKey(shortKey));
        return shortKey;
    }

    private static final String CHARACTERS = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
    private static final int SHORT_KEY_LENGTH = 6;
    private static final SecureRandom RANDOM = new SecureRandom();

    public static String generateRandomShortKey() {
        StringBuilder sb = new StringBuilder(SHORT_KEY_LENGTH);
        for (int i = 0; i < SHORT_KEY_LENGTH; i++) {
            sb.append(CHARACTERS.charAt(RANDOM.nextInt(CHARACTERS.length())));
        }
        return sb.toString();
    }


}
