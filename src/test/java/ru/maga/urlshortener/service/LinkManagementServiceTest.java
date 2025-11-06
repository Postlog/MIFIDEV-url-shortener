package ru.maga.urlshortener.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.maga.urlshortener.config.AppConfig;
import ru.maga.urlshortener.domain.ShortUrl;
import ru.maga.urlshortener.repository.ShortUrlRepository;
import ru.maga.urlshortener.repository.UserRepository;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LinkManagementServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private ShortUrlRepository shortUrlRepository;

    @Mock
    private UrlShortenerService urlShortenerService;

    @Mock
    private NotificationService notificationService;

    @Mock
    private AppConfig config;

    private LinkManagementService service;

    @BeforeEach
    void setUp() {
        when(config.getLinkTtlSeconds()).thenReturn(86400);
        when(config.getDefaultClickLimit()).thenReturn(100);

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
        verify(userRepository).save(any());
    }

    @Test
    void shouldCheckUserExists() {
        UUID userId = UUID.randomUUID();
        when(userRepository.exists(userId)).thenReturn(true);

        boolean exists = service.userExists(userId);

        assertThat(exists).isTrue();
    }

    @Test
    void shouldCreateShortUrl() {
        UUID userId = UUID.randomUUID();
        String url = "https://example.com";
        String shortCode = "abc123";

        when(userRepository.exists(userId)).thenReturn(true);
        when(urlShortenerService.generateShortCode(url, userId)).thenReturn(shortCode);
        when(shortUrlRepository.exists(shortCode)).thenReturn(false);

        ShortUrl result = service.createShortUrl(url, userId, null);

        assertThat(result).isNotNull();
        assertThat(result.getShortCode()).isEqualTo(shortCode);
        assertThat(result.getOriginalUrl()).isEqualTo(url);
        assertThat(result.getOwnerId()).isEqualTo(userId);
        verify(shortUrlRepository).save(any(ShortUrl.class));
    }

    @Test
    void shouldThrowExceptionWhenCreatingUrlForNonexistentUser() {
        UUID userId = UUID.randomUUID();
        when(userRepository.exists(userId)).thenReturn(false);

        assertThatThrownBy(() -> service.createShortUrl("https://example.com", userId, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Пользователь не существует");
    }

    @Test
    void shouldThrowExceptionForInvalidUrl() {
        UUID userId = UUID.randomUUID();
        when(userRepository.exists(userId)).thenReturn(true);

        assertThatThrownBy(() -> service.createShortUrl("invalid-url", userId, null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void shouldProcessClickSuccessfully() {
        String shortCode = "abc123";
        ShortUrl shortUrl = new ShortUrl(
                shortCode,
                "https://example.com",
                UUID.randomUUID(),
                Instant.now(),
                Instant.now().plusSeconds(3600),
                10
        );

        when(shortUrlRepository.findByShortCode(shortCode)).thenReturn(Optional.of(shortUrl));

        Optional<String> result = service.processClick(shortCode);

        assertThat(result).isPresent();
        assertThat(result.get()).isEqualTo("https://example.com");
        assertThat(shortUrl.getClickCount()).isEqualTo(1);
    }

    @Test
    void shouldNotProcessClickForExpiredUrl() {
        String shortCode = "abc123";
        ShortUrl shortUrl = new ShortUrl(
                shortCode,
                "https://example.com",
                UUID.randomUUID(),
                Instant.now(),
                Instant.now().minusSeconds(1), // Expired
                10
        );

        when(shortUrlRepository.findByShortCode(shortCode)).thenReturn(Optional.of(shortUrl));

        Optional<String> result = service.processClick(shortCode);

        assertThat(result).isEmpty();
        verify(notificationService).notifyLinkExpired(shortCode, "https://example.com");
    }

    @Test
    void shouldNotProcessClickWhenLimitReached() {
        String shortCode = "abc123";
        ShortUrl shortUrl = new ShortUrl(
                shortCode,
                "https://example.com",
                UUID.randomUUID(),
                Instant.now(),
                Instant.now().plusSeconds(3600),
                1
        );
        shortUrl.incrementClickCount(); // Reach limit

        when(shortUrlRepository.findByShortCode(shortCode)).thenReturn(Optional.of(shortUrl));

        Optional<String> result = service.processClick(shortCode);

        assertThat(result).isEmpty();
        verify(notificationService).notifyClickLimitReached(shortCode, "https://example.com", 1);
    }

    @Test
    void shouldUpdateClickLimit() {
        String shortCode = "abc123";
        UUID ownerId = UUID.randomUUID();
        ShortUrl shortUrl = new ShortUrl(
                shortCode,
                "https://example.com",
                ownerId,
                Instant.now(),
                Instant.now().plusSeconds(3600),
                10
        );

        when(shortUrlRepository.findByShortCode(shortCode)).thenReturn(Optional.of(shortUrl));

        service.updateClickLimit(shortCode, ownerId, 50);

        assertThat(shortUrl.getClickLimit()).isEqualTo(50);
    }

    @Test
    void shouldThrowExceptionWhenNonOwnerTriesToUpdate() {
        String shortCode = "abc123";
        UUID ownerId = UUID.randomUUID();
        UUID otherUserId = UUID.randomUUID();
        ShortUrl shortUrl = new ShortUrl(
                shortCode,
                "https://example.com",
                ownerId,
                Instant.now(),
                Instant.now().plusSeconds(3600),
                10
        );

        when(shortUrlRepository.findByShortCode(shortCode)).thenReturn(Optional.of(shortUrl));

        assertThatThrownBy(() -> service.updateClickLimit(shortCode, otherUserId, 50))
                .isInstanceOf(SecurityException.class)
                .hasMessageContaining("нет прав");
    }

    @Test
    void shouldDeleteShortUrl() {
        String shortCode = "abc123";
        UUID ownerId = UUID.randomUUID();
        ShortUrl shortUrl = new ShortUrl(
                shortCode,
                "https://example.com",
                ownerId,
                Instant.now(),
                Instant.now().plusSeconds(3600),
                10
        );

        when(shortUrlRepository.findByShortCode(shortCode)).thenReturn(Optional.of(shortUrl));

        service.deleteShortUrl(shortCode, ownerId);

        verify(shortUrlRepository).delete(shortCode);
    }

    @Test
    void shouldThrowExceptionWhenNonOwnerTriesToDelete() {
        String shortCode = "abc123";
        UUID ownerId = UUID.randomUUID();
        UUID otherUserId = UUID.randomUUID();
        ShortUrl shortUrl = new ShortUrl(
                shortCode,
                "https://example.com",
                ownerId,
                Instant.now(),
                Instant.now().plusSeconds(3600),
                10
        );

        when(shortUrlRepository.findByShortCode(shortCode)).thenReturn(Optional.of(shortUrl));

        assertThatThrownBy(() -> service.deleteShortUrl(shortCode, otherUserId))
                .isInstanceOf(SecurityException.class)
                .hasMessageContaining("нет прав");
    }
}

