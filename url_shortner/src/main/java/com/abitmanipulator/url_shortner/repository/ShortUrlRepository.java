package com.abitmanipulator.url_shortner.repository;


import com.abitmanipulator.url_shortner.domain.entity.ShortUrl;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ShortUrlRepository extends JpaRepository<ShortUrl, Long> {
    @Query("SELECT su " +
            "FROM ShortUrl su " +
            "WHERE su.isPrivate = false " +
            "ORDER BY su.createdAt DESC")
    List<ShortUrl> findPublicShortUrls();
//    List<ShortUrl> findByIsPrivateIsFalseOrderByCreatedAtDesc();
}
