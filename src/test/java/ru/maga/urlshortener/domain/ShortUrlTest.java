package ru.maga.urlshortener.domain;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class ShortUrlTest {

    @Test
    void shouldCreateShortUrl() {
        UUID ownerId = UUID.randomUUID();
        Instant now = Instant.now();
        Instant expiresAt = now.plusSeconds(3600);

        ShortUrl shortUrl = new ShortUrl(
                "abc123",
                "https://example.com",
                ownerId,
                now,
                expiresAt,
                100
        );

        assertThat(shortUrl.getShortCode()).isEqualTo("abc123");
        assertThat(shortUrl.getOriginalUrl()).isEqualTo("https://example.com");
        assertThat(shortUrl.getOwnerId()).isEqualTo(ownerId);
        assertThat(shortUrl.getClickCount()).isZero();
        assertThat(shortUrl.getClickLimit()).isEqualTo(100);
    }

    @Test
    void shouldNotBeExpiredWhenWithinTtl() {
        Instant now = Instant.now();
        Instant expiresAt = now.plusSeconds(3600);

        ShortUrl shortUrl = new ShortUrl(
                "abc123",
                "https://example.com",
                UUID.randomUUID(),
                now,
                expiresAt,
                100
        );

        assertThat(shortUrl.isExpired()).isFalse();
    }

    @Test
    void shouldBeExpiredWhenPastTtl() {
        Instant now = Instant.now();
        Instant expiresAt = now.minusSeconds(1);

        ShortUrl shortUrl = new ShortUrl(
                "abc123",
                "https://example.com",
                UUID.randomUUID(),
                now,
                expiresAt,
                100
        );

        assertThat(shortUrl.isExpired()).isTrue();
    }

    @Test
    void shouldNotHaveReachedLimitInitially() {
        ShortUrl shortUrl = new ShortUrl(
                "abc123",
                "https://example.com",
                UUID.randomUUID(),
                Instant.now(),
                Instant.now().plusSeconds(3600),
                10
        );

        assertThat(shortUrl.hasReachedClickLimit()).isFalse();
    }

    @Test
    void shouldReachClickLimitAfterEnoughClicks() {
        ShortUrl shortUrl = new ShortUrl(
                "abc123",
                "https://example.com",
                UUID.randomUUID(),
                Instant.now(),
                Instant.now().plusSeconds(3600),
                3
        );

        shortUrl.incrementClickCount();
        shortUrl.incrementClickCount();
        assertThat(shortUrl.hasReachedClickLimit()).isFalse();

        shortUrl.incrementClickCount();
        assertThat(shortUrl.hasReachedClickLimit()).isTrue();
    }

    @Test
    void shouldNotHaveLimitWhenSetToNegative() {
        ShortUrl shortUrl = new ShortUrl(
                "abc123",
                "https://example.com",
                UUID.randomUUID(),
                Instant.now(),
                Instant.now().plusSeconds(3600),
                -1
        );

        for (int i = 0; i < 1000; i++) {
            shortUrl.incrementClickCount();
        }

        assertThat(shortUrl.hasReachedClickLimit()).isFalse();
    }

    @Test
    void shouldBeAccessibleWhenNotExpiredAndUnderLimit() {
        ShortUrl shortUrl = new ShortUrl(
                "abc123",
                "https://example.com",
                UUID.randomUUID(),
                Instant.now(),
                Instant.now().plusSeconds(3600),
                10
        );

        assertThat(shortUrl.isAccessible()).isTrue();
    }

    @Test
    void shouldNotBeAccessibleWhenExpired() {
        ShortUrl shortUrl = new ShortUrl(
                "abc123",
                "https://example.com",
                UUID.randomUUID(),
                Instant.now(),
                Instant.now().minusSeconds(1),
                10
        );

        assertThat(shortUrl.isAccessible()).isFalse();
    }

    @Test
    void shouldNotBeAccessibleWhenLimitReached() {
        ShortUrl shortUrl = new ShortUrl(
                "abc123",
                "https://example.com",
                UUID.randomUUID(),
                Instant.now(),
                Instant.now().plusSeconds(3600),
                1
        );

        shortUrl.incrementClickCount();
        assertThat(shortUrl.isAccessible()).isFalse();
    }

    @Test
    void shouldRecognizeOwner() {
        UUID ownerId = UUID.randomUUID();
        ShortUrl shortUrl = new ShortUrl(
                "abc123",
                "https://example.com",
                ownerId,
                Instant.now(),
                Instant.now().plusSeconds(3600),
                10
        );

        assertThat(shortUrl.isOwnedBy(ownerId)).isTrue();
        assertThat(shortUrl.isOwnedBy(UUID.randomUUID())).isFalse();
    }

    @Test
    void shouldUpdateClickLimit() {
        ShortUrl shortUrl = new ShortUrl(
                "abc123",
                "https://example.com",
                UUID.randomUUID(),
                Instant.now(),
                Instant.now().plusSeconds(3600),
                10
        );

        shortUrl.setClickLimit(50);
        assertThat(shortUrl.getClickLimit()).isEqualTo(50);
    }
}

