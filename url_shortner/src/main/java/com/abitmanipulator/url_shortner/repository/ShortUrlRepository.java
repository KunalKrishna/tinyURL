package com.abitmanipulator.url_shortner.repository;


import com.abitmanipulator.url_shortner.domain.entities.ShortUrl;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ShortUrlRepository extends JpaRepository<ShortUrl, Long> {
    /**
     * A method to return public short urls in descending order of creation (latest first).
     * Cons : lengthy function name.
     * @return List<ShortUrl> of `com.abitmanipulator.url_shortner.domain.entities.ShortUrl`
     */
    List<ShortUrl> findByIsPrivateIsFalseOrderByCreatedAtDesc();

//    @Query("SELECT su FROM ShortUrl su LEFT JOIN FETCH su.createdBy WHERE su.isPrivate = false ORDER BY su.createdAt DESC")
    @Query("SELECT su FROM ShortUrl su WHERE su.isPrivate = false ORDER BY su.createdAt DESC")
    @EntityGraph(attributePaths = {"createdBy"})
    List<ShortUrl> findPublicShortUrls();

    /**
     *
     * @param pageable
     * @return Page of result without sorting in any order.
     */
    @Query("SELECT su FROM ShortUrl su WHERE su.isPrivate = false")
    Page<ShortUrl> findPublicShortUrls(Pageable  pageable);

    boolean existsByShortKey(String shortKey);

    Optional<ShortUrl> findByShortKey(String shortKey);
}
