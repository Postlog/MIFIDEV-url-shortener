package ru.maga.urlshortener.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ru.maga.urlshortener.config.AppConfig;
import ru.maga.urlshortener.domain.ShortUrl;
import ru.maga.urlshortener.repository.ShortUrlRepository;
import ru.maga.urlshortener.repository.UserRepository;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Tests for LinkManagementService using real instances (no mocks).
 * This approach is more reliable and works with all Java versions.
 */
class LinkManagementServiceTest {

    private UserRepository userRepository;
    private ShortUrlRepository shortUrlRepository;
    private UrlShortenerService urlShortenerService;
    private NotificationService notificationService;
    private AppConfig config;
    private LinkManagementService service;

    @BeforeEach
    void setUp() {
        // Use real instances instead of mocks
        userRepository = new UserRepository();
        shortUrlRepository = new ShortUrlRepository();
        urlShortenerService = new UrlShortenerService(6);
        notificationService = new NotificationService(false); // Disable for tests
        config = new AppConfig();

        service = new LinkManagementService(
                userRepository,
                shortUrlRepository,
                urlShortenerService,
                notificationService,
                config
        );
    }

    @Test
    void shouldCreateUser() {
        UUID userId = service.createUser();

        assertThat(userId).isNotNull();
        assertThat(userRepository.exists(userId)).isTrue();
    }

    @Test
    void shouldCheckUserExists() {
        UUID userId = service.createUser();

        boolean exists = service.userExists(userId);

        assertThat(exists).isTrue();
        assertThat(service.userExists(UUID.randomUUID())).isFalse();
    }

    @Test
    void shouldCreateShortUrl() {
        UUID userId = service.createUser();
        String url = "https://example.com";

        ShortUrl result = service.createShortUrl(url, userId, 50);

        assertThat(result).isNotNull();
        assertThat(result.getShortCode()).isNotNull().hasSize(6);
        assertThat(result.getOriginalUrl()).isEqualTo(url);
        assertThat(result.getOwnerId()).isEqualTo(userId);
        assertThat(result.getClickLimit()).isEqualTo(50);
        assertThat(shortUrlRepository.exists(result.getShortCode())).isTrue();
    }

    @Test
    void shouldThrowExceptionWhenCreatingUrlForNonexistentUser() {
        UUID userId = UUID.randomUUID();

        assertThatThrownBy(() -> service.createShortUrl("https://example.com", userId, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Пользователь не существует");
    }

    @Test
    void shouldThrowExceptionForInvalidUrl() {
        UUID userId = service.createUser();

        assertThatThrownBy(() -> service.createShortUrl("invalid-url", userId, null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void shouldProcessClickSuccessfully() {
        UUID userId = service.createUser();
        ShortUrl shortUrl = service.createShortUrl("https://example.com", userId, 10);
        String shortCode = shortUrl.getShortCode();

        Optional<String> result = service.processClick(shortCode);

        assertThat(result).isPresent();
        assertThat(result.get()).isEqualTo("https://example.com");
        assertThat(shortUrl.getClickCount()).isEqualTo(1);
    }

    @Test
    void shouldNotProcessClickForExpiredUrl() {
        // Create expired URL manually
        UUID userId = service.createUser();
        ShortUrl expiredUrl = new ShortUrl(
                "expired",
                "https://example.com",
                userId,
                Instant.now().minusSeconds(1000),
                Instant.now().minusSeconds(1), // Expired
                10
        );
        shortUrlRepository.save(expiredUrl);

        Optional<String> result = service.processClick("expired");

        assertThat(result).isEmpty();
        assertThat(expiredUrl.getClickCount()).isZero(); // No click processed
    }

    @Test
    void shouldNotProcessClickWhenLimitReached() {
        UUID userId = service.createUser();
        ShortUrl shortUrl = service.createShortUrl("https://example.com", userId, 2);
        String shortCode = shortUrl.getShortCode();

        // Consume all clicks
        service.processClick(shortCode);
        service.processClick(shortCode);

        // Next click should fail
        Optional<String> result = service.processClick(shortCode);

        assertThat(result).isEmpty();
        assertThat(shortUrl.getClickCount()).isEqualTo(2);
    }

    @Test
    void shouldUpdateClickLimit() {
        UUID userId = service.createUser();
        ShortUrl shortUrl = service.createShortUrl("https://example.com", userId, 10);
        String shortCode = shortUrl.getShortCode();

        service.updateClickLimit(shortCode, userId, 50);

        assertThat(shortUrl.getClickLimit()).isEqualTo(50);
    }

    @Test
    void shouldThrowExceptionWhenNonOwnerTriesToUpdate() {
        UUID owner = service.createUser();
        UUID otherUser = service.createUser();
        ShortUrl shortUrl = service.createShortUrl("https://example.com", owner, 10);

        assertThatThrownBy(() -> service.updateClickLimit(shortUrl.getShortCode(), otherUser, 50))
                .isInstanceOf(SecurityException.class)
                .hasMessageContaining("нет прав");
    }

    @Test
    void shouldDeleteShortUrl() {
        UUID userId = service.createUser();
        ShortUrl shortUrl = service.createShortUrl("https://example.com", userId, 10);
        String shortCode = shortUrl.getShortCode();

        service.deleteShortUrl(shortCode, userId);

        assertThat(shortUrlRepository.exists(shortCode)).isFalse();
    }

    @Test
    void shouldThrowExceptionWhenNonOwnerTriesToDelete() {
        UUID owner = service.createUser();
        UUID otherUser = service.createUser();
        ShortUrl shortUrl = service.createShortUrl("https://example.com", owner, 10);

        assertThatThrownBy(() -> service.deleteShortUrl(shortUrl.getShortCode(), otherUser))
                .isInstanceOf(SecurityException.class)
                .hasMessageContaining("нет прав");
    }

    @Test
    void shouldGetUserLinks() {
        UUID userId = service.createUser();
        service.createShortUrl("https://example1.com", userId, null);
        service.createShortUrl("https://example2.com", userId, null);

        assertThat(service.getUserLinks(userId)).hasSize(2);
    }

    @Test
    void shouldCleanupExpiredLinks() {
        UUID userId = service.createUser();
        
        // Create expired link
        ShortUrl expiredUrl = new ShortUrl(
                "expired",
                "https://expired.com",
                userId,
                Instant.now().minusSeconds(1000),
                Instant.now().minusSeconds(1),
                100
        );
        shortUrlRepository.save(expiredUrl);
        
        // Create active link
        service.createShortUrl("https://active.com", userId, null);

        int deleted = service.cleanupExpiredLinks();

        assertThat(deleted).isEqualTo(1);
        assertThat(shortUrlRepository.exists("expired")).isFalse();
    }
}

