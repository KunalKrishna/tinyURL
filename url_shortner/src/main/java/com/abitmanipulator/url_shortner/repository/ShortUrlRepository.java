package com.abitmanipulator.url_shortner.repository;


import com.abitmanipulator.url_shortner.domain.entities.ShortUrl;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ShortUrlRepository extends JpaRepository<ShortUrl, Long> {
//    @Query("SELECT su FROM ShortUrl su LEFT JOIN FETCH su.createdBy WHERE su.isPrivate = false ORDER BY su.createdAt DESC")
    @Query("SELECT su FROM ShortUrl su WHERE su.isPrivate = false ORDER BY su.createdAt DESC")
    @EntityGraph(attributePaths = {"createdBy"})
    List<ShortUrl> findPublicShortUrls();
//    List<ShortUrl> findByIsPrivateIsFalseOrderByCreatedAtDesc();

    boolean existsByShortKey(String shortKey);

    Optional<ShortUrl> findByShortKey(String shortKey);
}
