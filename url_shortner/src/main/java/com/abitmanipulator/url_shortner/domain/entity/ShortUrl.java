package com.abitmanipulator.url_shortner.domain.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name ="short_urls")
public class ShortUrl {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true ,nullable = false)
    private String shortKey;

    @Column(nullable = false)
    private String originalUrl;

    // Many short_urls belong to one User
    // @JoinColumn specifies the foreign key column in the 'short_urls' table
    @ManyToOne
    @JoinColumn(name = "created_by")// This will be the FK column in the 'short_urls' table
    private User createdBy;

    private LocalDateTime createdAt = LocalDateTime.now();

    private LocalDateTime expiresAt;

    private Boolean isPrivate;

    private int click_count;

}
