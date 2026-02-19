package com.abitmanipulator.url_shortner.services;

import com.abitmanipulator.url_shortner.domain.entities.ShortUrl;
import com.abitmanipulator.url_shortner.domain.entities.User;
import com.abitmanipulator.url_shortner.domain.models.ShortUrlDto;
import com.abitmanipulator.url_shortner.domain.models.UserDto;
import org.springframework.stereotype.Component;

// Use tools like MapStruct or other Java Beans mapping libraries like Dozer, ModelMapper for mapping.
// MapStruct shines in Spring Boot for DTO-entity mapping due to compile-time code generation (no runtime reflection), type safety, and performance.
// It reduces boilerplate vs. manual mapping and avoids runtime issues in tools like ModelMapper.
@Component
public class EntityMapper {

    public ShortUrlDto toShortUrlDto(ShortUrl shortUrl) {
        UserDto userDto = null;
        if(shortUrl.getCreatedBy()!=null){
            userDto = toUserDto(shortUrl.getCreatedBy());
        }

        return  new ShortUrlDto(
                shortUrl.getId(),
                shortUrl.getShortKey(),
                shortUrl.getOriginalUrl(),
                shortUrl.getIsPrivate(),
                shortUrl.getExpiresAt(),
                userDto,
                shortUrl.getClickCount(),
                shortUrl.getCreatedAt()
        );
    }

    public UserDto toUserDto(User user) {
        return  new UserDto(user.getId(), user.getName());
    }
}
