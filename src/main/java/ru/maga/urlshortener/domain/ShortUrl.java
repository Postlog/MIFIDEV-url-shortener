package ru.maga.urlshortener.domain;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Represents a shortened URL with its metadata.
 * Each short URL is owned by a specific user and has TTL and click limits.
 */
public class ShortUrl {
    private final String shortCode;
    private final String originalUrl;
    private final UUID ownerId;
    private final Instant createdAt;
    private final Instant expiresAt;
    private int clickLimit;
    private int clickCount;

    public ShortUrl(String shortCode, String originalUrl, UUID ownerId,
                    Instant createdAt, Instant expiresAt, int clickLimit) {
        this.shortCode = Objects.requireNonNull(shortCode, "Short code cannot be null");
        this.originalUrl = Objects.requireNonNull(originalUrl, "Original URL cannot be null");
        this.ownerId = Objects.requireNonNull(ownerId, "Owner ID cannot be null");
        this.createdAt = Objects.requireNonNull(createdAt, "Created timestamp cannot be null");
        this.expiresAt = Objects.requireNonNull(expiresAt, "Expiration timestamp cannot be null");
        this.clickLimit = clickLimit;
        this.clickCount = 0;
    }

    public boolean isExpired() {
        return Instant.now().isAfter(expiresAt);
    }

    public boolean hasReachedClickLimit() {
        return clickLimit > 0 && clickCount >= clickLimit;
    }

    public boolean isAccessible() {
        return !isExpired() && !hasReachedClickLimit();
    }

    public void incrementClickCount() {
        this.clickCount++;
    }

    public boolean isOwnedBy(UUID userId) {
        return this.ownerId.equals(userId);
    }

    public String getShortCode() {
        return shortCode;
    }

    public String getOriginalUrl() {
        return originalUrl;
    }

    public UUID getOwnerId() {
        return ownerId;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public int getClickLimit() {
        return clickLimit;
    }

    public void setClickLimit(int clickLimit) {
        this.clickLimit = clickLimit;
    }

    public int getClickCount() {
        return clickCount;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ShortUrl shortUrl = (ShortUrl) o;
        return Objects.equals(shortCode, shortUrl.shortCode);
    }

    @Override
    public int hashCode() {
        return Objects.hash(shortCode);
    }

    @Override
    public String toString() {
        return "ShortUrl{" +
                "shortCode='" + shortCode + '\'' +
                ", originalUrl='" + originalUrl + '\'' +
                ", ownerId=" + ownerId +
                ", clickCount=" + clickCount +
                ", clickLimit=" + clickLimit +
                ", expiresAt=" + expiresAt +
                '}';
    }
}

