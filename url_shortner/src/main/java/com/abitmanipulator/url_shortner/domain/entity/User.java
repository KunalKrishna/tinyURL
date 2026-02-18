package com.abitmanipulator.url_shortner.domain.entity;

import com.abitmanipulator.url_shortner.domain.model.Role;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/*
* LIMITATIONS : Not suitable for production-grade application.
* Because : all things required in DB cannot be achieved by JPA Entities e.g. stored procedure, views
* Also : if you rename a column then JPA adds a new col w/ new name. w/o removing old column from table.
* */
@Entity
@Table(name = "users")
@Data
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(nullable = false)
    private Long id;

    @Column(unique = true ,nullable = false)
    private String email;

    @Column(nullable = false)
    private String password;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private Role role;

    @Column(nullable = false)
    private LocalDateTime created_at = LocalDateTime.now();
}
