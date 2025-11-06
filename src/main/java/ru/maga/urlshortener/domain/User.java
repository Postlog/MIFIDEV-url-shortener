package ru.maga.urlshortener.domain;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Represents a user in the system.
 * Users are identified by UUID without authentication.
 */
public class User {
    private final UUID id;
    private final Instant createdAt;

    public User(UUID id, Instant createdAt) {
        this.id = Objects.requireNonNull(id, "User ID cannot be null");
        this.createdAt = Objects.requireNonNull(createdAt, "Created timestamp cannot be null");
    }

    public static User create() {
        return new User(UUID.randomUUID(), Instant.now());
    }

    public UUID getId() {
        return id;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        User user = (User) o;
        return Objects.equals(id, user.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "User{id=" + id + ", createdAt=" + createdAt + "}";
    }
}

