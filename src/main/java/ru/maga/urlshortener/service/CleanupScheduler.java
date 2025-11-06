package ru.maga.urlshortener.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Scheduler for periodic cleanup of expired links.
 */
public class CleanupScheduler {
    private static final Logger logger = LoggerFactory.getLogger(CleanupScheduler.class);

    private final LinkManagementService linkManagementService;
    private final int intervalSeconds;
    private final ScheduledExecutorService scheduler;

    public CleanupScheduler(LinkManagementService linkManagementService, int intervalSeconds) {
        this.linkManagementService = linkManagementService;
        this.intervalSeconds = intervalSeconds;
        this.scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread thread = new Thread(r, "cleanup-scheduler");
            thread.setDaemon(true);
            return thread;
        });
    }

    /**
     * Starts the cleanup scheduler.
     */
    public void start() {
        scheduler.scheduleAtFixedRate(
                this::runCleanup,
                intervalSeconds,
                intervalSeconds,
                TimeUnit.SECONDS
        );
        logger.info("Cleanup scheduler started with interval: {}s", intervalSeconds);
    }

    /**
     * Stops the cleanup scheduler.
     */
    public void stop() {
        scheduler.shutdown();
        try {
            if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
            logger.info("Cleanup scheduler stopped");
        } catch (InterruptedException e) {
            scheduler.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    private void runCleanup() {
        try {
            int deleted = linkManagementService.cleanupExpiredLinks();
            if (deleted > 0) {
                logger.info("Cleanup completed: {} expired links removed", deleted);
            }
        } catch (Exception e) {
            logger.error("Error during cleanup", e);
        }
    }
}

