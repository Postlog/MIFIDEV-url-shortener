package ru.maga.urlshortener.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Service for sending notifications to users.
 * Currently uses console output, can be extended to email/SMS.
 */
public class NotificationService {
    private static final Logger logger = LoggerFactory.getLogger(NotificationService.class);
    private final boolean enabled;

    public NotificationService(boolean enabled) {
        this.enabled = enabled;
    }

    public void notifyLinkExpired(String shortCode, String originalUrl) {
        if (!enabled) return;
        String message = String.format(
                "\n⚠️  УВЕДОМЛЕНИЕ: Срок действия ссылки истёк!\n" +
                        "   Короткая ссылка: %s\n" +
                        "   Оригинальный URL: %s\n",
                shortCode, originalUrl
        );
        System.out.println(message);
        logger.info("Link expired notification: {}", shortCode);
    }

    public void notifyClickLimitReached(String shortCode, String originalUrl, int limit) {
        if (!enabled) return;
        String message = String.format(
                "\n⚠️  УВЕДОМЛЕНИЕ: Достигнут лимит переходов!\n" +
                        "   Короткая ссылка: %s\n" +
                        "   Оригинальный URL: %s\n" +
                        "   Лимит переходов: %d\n",
                shortCode, originalUrl, limit
        );
        System.out.println(message);
        logger.info("Click limit reached notification: {}", shortCode);
    }

    public void notifyLinkUnavailable(String shortCode) {
        if (!enabled) return;
        String message = String.format(
                "\n⚠️  УВЕДОМЛЕНИЕ: Ссылка недоступна!\n" +
                        "   Короткая ссылка: %s\n" +
                        "   Возможные причины: истёк срок действия или исчерпан лимит переходов.\n",
                shortCode
        );
        System.out.println(message);
        logger.info("Link unavailable notification: {}", shortCode);
    }
}

