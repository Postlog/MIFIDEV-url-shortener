package ru.maga.urlshortener.repository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ru.maga.urlshortener.domain.ShortUrl;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class ShortUrlRepositoryTest {

    private ShortUrlRepository repository;

    @BeforeEach
    void setUp() {
        repository = new ShortUrlRepository();
    }

    @Test
    void shouldSaveAndFindShortUrl() {
        ShortUrl shortUrl = createShortUrl("abc123", UUID.randomUUID());
        repository.save(shortUrl);

        Optional<ShortUrl> found = repository.findByShortCode("abc123");

        assertThat(found).isPresent();
        assertThat(found.get()).isEqualTo(shortUrl);
    }

    @Test
    void shouldReturnEmptyWhenShortUrlNotFound() {
        Optional<ShortUrl> found = repository.findByShortCode("nonexistent");

        assertThat(found).isEmpty();
    }

    @Test
    void shouldFindUrlsByOwner() {
        UUID owner1 = UUID.randomUUID();
        UUID owner2 = UUID.randomUUID();

        ShortUrl url1 = createShortUrl("abc123", owner1);
        ShortUrl url2 = createShortUrl("def456", owner1);
        ShortUrl url3 = createShortUrl("ghi789", owner2);

        repository.save(url1);
        repository.save(url2);
        repository.save(url3);

        List<ShortUrl> owner1Urls = repository.findByOwnerId(owner1);
        List<ShortUrl> owner2Urls = repository.findByOwnerId(owner2);

        assertThat(owner1Urls).hasSize(2).contains(url1, url2);
        assertThat(owner2Urls).hasSize(1).contains(url3);
    }

    @Test
    void shouldReturnEmptyListWhenOwnerHasNoUrls() {
        List<ShortUrl> urls = repository.findByOwnerId(UUID.randomUUID());

        assertThat(urls).isEmpty();
    }

    @Test
    void shouldDeleteShortUrl() {
        ShortUrl shortUrl = createShortUrl("abc123", UUID.randomUUID());
        repository.save(shortUrl);

        repository.delete("abc123");

        assertThat(repository.findByShortCode("abc123")).isEmpty();
    }

    @Test
    void shouldRemoveUrlFromOwnerIndexOnDelete() {
        UUID ownerId = UUID.randomUUID();
        ShortUrl shortUrl = createShortUrl("abc123", ownerId);
        repository.save(shortUrl);

        repository.delete("abc123");

        assertThat(repository.findByOwnerId(ownerId)).isEmpty();
    }

    @Test
    void shouldFindAllUrls() {
        ShortUrl url1 = createShortUrl("abc123", UUID.randomUUID());
        ShortUrl url2 = createShortUrl("def456", UUID.randomUUID());

        repository.save(url1);
        repository.save(url2);

        List<ShortUrl> all = repository.findAll();

        assertThat(all).hasSize(2).contains(url1, url2);
    }

    @Test
    void shouldCheckIfShortCodeExists() {
        ShortUrl shortUrl = createShortUrl("abc123", UUID.randomUUID());
        repository.save(shortUrl);

        assertThat(repository.exists("abc123")).isTrue();
        assertThat(repository.exists("nonexistent")).isFalse();
    }

    @Test
    void shouldCountUrls() {
        assertThat(repository.count()).isZero();

        repository.save(createShortUrl("abc123", UUID.randomUUID()));
        assertThat(repository.count()).isEqualTo(1);

        repository.save(createShortUrl("def456", UUID.randomUUID()));
        assertThat(repository.count()).isEqualTo(2);
    }

    private ShortUrl createShortUrl(String shortCode, UUID ownerId) {
        return new ShortUrl(
                shortCode,
                "https://example.com",
                ownerId,
                Instant.now(),
                Instant.now().plusSeconds(3600),
                100
        );
    }
}

