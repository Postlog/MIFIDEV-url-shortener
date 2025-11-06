package ru.maga.urlshortener.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.maga.urlshortener.config.AppConfig;
import ru.maga.urlshortener.domain.ShortUrl;
import ru.maga.urlshortener.domain.User;
import ru.maga.urlshortener.repository.ShortUrlRepository;
import ru.maga.urlshortener.repository.UserRepository;
import ru.maga.urlshortener.util.UrlValidator;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Main service for link management operations.
 * Handles creation, retrieval, updates, and access control for short URLs.
 */
public class LinkManagementService {
    private static final Logger logger = LoggerFactory.getLogger(LinkManagementService.class);

    private final UserRepository userRepository;
    private final ShortUrlRepository shortUrlRepository;
    private final UrlShortenerService urlShortenerService;
    private final NotificationService notificationService;
    private final AppConfig config;

    public LinkManagementService(UserRepository userRepository,
                                 ShortUrlRepository shortUrlRepository,
                                 UrlShortenerService urlShortenerService,
                                 NotificationService notificationService,
                                 AppConfig config) {
        this.userRepository = userRepository;
        this.shortUrlRepository = shortUrlRepository;
        this.urlShortenerService = urlShortenerService;
        this.notificationService = notificationService;
        this.config = config;
    }

    /**
     * Creates a new user and returns their UUID.
     */
    public UUID createUser() {
        User user = User.create();
        userRepository.save(user);
        logger.info("Created new user: {}", user.getId());
        return user.getId();
    }

    /**
     * Checks if a user exists.
     */
    public boolean userExists(UUID userId) {
        return userRepository.exists(userId);
    }

    /**
     * Creates a short URL for the given original URL and user.
     */
    public ShortUrl createShortUrl(String originalUrl, UUID userId, Integer customClickLimit) {
        UrlValidator.validate(originalUrl);

        if (!userExists(userId)) {
            throw new IllegalArgumentException("Пользователь не существует: " + userId);
        }

        String shortCode = generateUniqueShortCode(originalUrl, userId);
        Instant now = Instant.now();
        Instant expiresAt = now.plusSeconds(config.getLinkTtlSeconds());
        int clickLimit = customClickLimit != null ? customClickLimit : config.getDefaultClickLimit();

        ShortUrl shortUrl = new ShortUrl(shortCode, originalUrl, userId, now, expiresAt, clickLimit);
        shortUrlRepository.save(shortUrl);

        logger.info("Created short URL: {} -> {} for user {}", shortCode, originalUrl, userId);
        return shortUrl;
    }

    /**
     * Retrieves the original URL and processes the click (if accessible).
     */
    public Optional<String> processClick(String shortCode) {
        Optional<ShortUrl> shortUrlOpt = shortUrlRepository.findByShortCode(shortCode);

        if (shortUrlOpt.isEmpty()) {
            return Optional.empty();
        }

        ShortUrl shortUrl = shortUrlOpt.get();

        // Check if expired
        if (shortUrl.isExpired()) {
            notificationService.notifyLinkExpired(shortCode, shortUrl.getOriginalUrl());
            return Optional.empty();
        }

        // Check if click limit reached
        if (shortUrl.hasReachedClickLimit()) {
            notificationService.notifyClickLimitReached(
                    shortCode, shortUrl.getOriginalUrl(), shortUrl.getClickLimit());
            return Optional.empty();
        }

        // Increment counter and return URL
        shortUrl.incrementClickCount();
        logger.info("Processed click for {}: count={}/{}", shortCode,
                shortUrl.getClickCount(), shortUrl.getClickLimit());

        return Optional.of(shortUrl.getOriginalUrl());
    }

    /**
     * Updates the click limit for a short URL.
     * Only the owner can update.
     */
    public void updateClickLimit(String shortCode, UUID userId, int newLimit) {
        ShortUrl shortUrl = shortUrlRepository.findByShortCode(shortCode)
                .orElseThrow(() -> new IllegalArgumentException("Ссылка не найдена: " + shortCode));

        if (!shortUrl.isOwnedBy(userId)) {
            throw new SecurityException("У вас нет прав на изменение этой ссылки");
        }

        if (newLimit < 0 && newLimit != -1) {
            throw new IllegalArgumentException("Лимит должен быть положительным числом или -1 для безлимита");
        }

        shortUrl.setClickLimit(newLimit);
        logger.info("Updated click limit for {}: {}", shortCode, newLimit);
    }

    /**
     * Deletes a short URL.
     * Only the owner can delete.
     */
    public void deleteShortUrl(String shortCode, UUID userId) {
        ShortUrl shortUrl = shortUrlRepository.findByShortCode(shortCode)
                .orElseThrow(() -> new IllegalArgumentException("Ссылка не найдена: " + shortCode));

        if (!shortUrl.isOwnedBy(userId)) {
            throw new SecurityException("У вас нет прав на удаление этой ссылки");
        }

        shortUrlRepository.delete(shortCode);
        logger.info("Deleted short URL: {} by user {}", shortCode, userId);
    }

    /**
     * Gets all short URLs for a user.
     */
    public List<ShortUrl> getUserLinks(UUID userId) {
        return shortUrlRepository.findByOwnerId(userId);
    }

    /**
     * Gets information about a short URL without processing a click.
     */
    public Optional<ShortUrl> getShortUrlInfo(String shortCode) {
        return shortUrlRepository.findByShortCode(shortCode);
    }

    /**
     * Cleans up expired links.
     */
    public int cleanupExpiredLinks() {
        List<ShortUrl> allLinks = shortUrlRepository.findAll();
        int deletedCount = 0;

        for (ShortUrl link : allLinks) {
            if (link.isExpired()) {
                shortUrlRepository.delete(link.getShortCode());
                notificationService.notifyLinkExpired(link.getShortCode(), link.getOriginalUrl());
                deletedCount++;
            }
        }

        if (deletedCount > 0) {
            logger.info("Cleanup: removed {} expired links", deletedCount);
        }

        return deletedCount;
    }

    /**
     * Gets statistics about the system.
     */
    public String getStatistics() {
        return String.format(
                "Пользователей: %d, Ссылок: %d",
                userRepository.count(),
                shortUrlRepository.count()
        );
    }

    private String generateUniqueShortCode(String originalUrl, UUID userId) {
        String shortCode;
        int attempts = 0;
        do {
            shortCode = urlShortenerService.generateShortCode(originalUrl, userId);
            attempts++;
            if (attempts > 10) {
                throw new RuntimeException("Не удалось сгенерировать уникальный код после 10 попыток");
            }
        } while (shortUrlRepository.exists(shortCode));

        return shortCode;
    }
}

