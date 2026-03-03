package com.krishvas.kitchen.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
@Entity
@Table(name = "admin")
public class Admin {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(optional = false)
    @JoinColumn(name = "user_id", unique = true)
    private User user;

    @Column(nullable = false)
    private Instant createdAt = Instant.now();

    @Column(nullable = false)
    private boolean kitchenActive = true;

    @Column(nullable = false)
    private boolean darkMode = false;

    @Column(nullable = false)
    private Instant updatedAt = Instant.now();

    @PreUpdate
    public void touchUpdatedAt() {
        this.updatedAt = Instant.now();
    }
}
