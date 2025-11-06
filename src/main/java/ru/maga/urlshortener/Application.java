package ru.maga.urlshortener;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.maga.urlshortener.cli.ConsoleInterface;
import ru.maga.urlshortener.config.AppConfig;
import ru.maga.urlshortener.repository.ShortUrlRepository;
import ru.maga.urlshortener.repository.UserRepository;
import ru.maga.urlshortener.service.*;

/**
 * Main application entry point.
 * Initializes all components and starts the CLI interface.
 */
public class Application {
    private static final Logger logger = LoggerFactory.getLogger(Application.class);

    public static void main(String[] args) {
        logger.info("Starting URL Shortener application");

        try {
            // Load configuration
            AppConfig config = new AppConfig();

            // Initialize repositories
            UserRepository userRepository = new UserRepository();
            ShortUrlRepository shortUrlRepository = new ShortUrlRepository();

            // Initialize services
            UrlShortenerService urlShortenerService = new UrlShortenerService(config.getShortenerCodeLength());
            NotificationService notificationService = new NotificationService(config.isNotificationEnabled());

            LinkManagementService linkManagementService = new LinkManagementService(
                    userRepository,
                    shortUrlRepository,
                    urlShortenerService,
                    notificationService,
                    config
            );

            // Start cleanup scheduler
            CleanupScheduler cleanupScheduler = new CleanupScheduler(
                    linkManagementService,
                    config.getCleanupIntervalSeconds()
            );
            cleanupScheduler.start();

            // Add shutdown hook
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                logger.info("Shutting down application");
                cleanupScheduler.stop();
            }));

            // Start CLI
            ConsoleInterface cli = new ConsoleInterface(linkManagementService, config);
            cli.start();

            logger.info("Application terminated normally");
        } catch (Exception e) {
            logger.error("Fatal error in application", e);
            System.err.println("Критическая ошибка приложения: " + e.getMessage());
            System.exit(1);
        }
    }
}

