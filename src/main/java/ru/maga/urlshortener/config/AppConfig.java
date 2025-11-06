package ru.maga.urlshortener.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Application configuration loaded from application.properties.
 */
public class AppConfig {
    private static final Logger logger = LoggerFactory.getLogger(AppConfig.class);
    private static final String CONFIG_FILE = "application.properties";

    private final int linkTtlSeconds;
    private final int defaultClickLimit;
    private final int cleanupIntervalSeconds;
    private final String shortenerDomain;
    private final int shortenerCodeLength;
    private final boolean notificationEnabled;

    public AppConfig() {
        Properties props = loadProperties();
        this.linkTtlSeconds = getIntProperty(props, "link.ttl.seconds", 86400);
        this.defaultClickLimit = getIntProperty(props, "link.default.click.limit", 100);
        this.cleanupIntervalSeconds = getIntProperty(props, "cleanup.scheduler.interval.seconds", 3600);
        this.shortenerDomain = props.getProperty("shortener.domain", "short.ly");
        this.shortenerCodeLength = getIntProperty(props, "shortener.code.length", 6);
        this.notificationEnabled = getBooleanProperty(props, "notification.enabled", true);

        logger.info("Configuration loaded: TTL={}s, ClickLimit={}, CleanupInterval={}s",
                linkTtlSeconds, defaultClickLimit, cleanupIntervalSeconds);
    }

    private Properties loadProperties() {
        Properties props = new Properties();
        try (InputStream input = getClass().getClassLoader().getResourceAsStream(CONFIG_FILE)) {
            if (input != null) {
                props.load(input);
                logger.info("Loaded configuration from {}", CONFIG_FILE);
            } else {
                logger.warn("Configuration file {} not found, using defaults", CONFIG_FILE);
            }
        } catch (IOException e) {
            logger.error("Error loading configuration file", e);
        }
        return props;
    }

    private int getIntProperty(Properties props, String key, int defaultValue) {
        String value = props.getProperty(key);
        if (value != null) {
            try {
                return Integer.parseInt(value.trim());
            } catch (NumberFormatException e) {
                logger.warn("Invalid integer value for {}: {}, using default: {}", key, value, defaultValue);
            }
        }
        return defaultValue;
    }

    private boolean getBooleanProperty(Properties props, String key, boolean defaultValue) {
        String value = props.getProperty(key);
        if (value != null) {
            return Boolean.parseBoolean(value.trim());
        }
        return defaultValue;
    }

    public int getLinkTtlSeconds() {
        return linkTtlSeconds;
    }

    public int getDefaultClickLimit() {
        return defaultClickLimit;
    }

    public int getCleanupIntervalSeconds() {
        return cleanupIntervalSeconds;
    }

    public String getShortenerDomain() {
        return shortenerDomain;
    }

    public int getShortenerCodeLength() {
        return shortenerCodeLength;
    }

    public boolean isNotificationEnabled() {
        return notificationEnabled;
    }
}

