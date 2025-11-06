package ru.maga.urlshortener.service;

import ru.maga.urlshortener.domain.ShortUrl;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.UUID;

/**
 * Service for generating unique short codes for URLs.
 * Uses SHA-256 hash of (originalUrl + userId + timestamp) to ensure uniqueness per user.
 */
public class UrlShortenerService {
    private static final String BASE62_CHARS = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";
    private final int codeLength;

    public UrlShortenerService(int codeLength) {
        this.codeLength = codeLength;
    }

    /**
     * Generates a unique short code for the given URL and user.
     * Each user gets a different short code for the same URL.
     */
    public String generateShortCode(String originalUrl, UUID userId) {
        String input = originalUrl + userId.toString() + System.nanoTime();
        return hashToBase62(input, codeLength);
    }

    private String hashToBase62(String input, int length) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));

            StringBuilder result = new StringBuilder();
            long value = 0;

            // Use first 8 bytes of hash to create a long value
            for (int i = 0; i < Math.min(8, hash.length); i++) {
                value = (value << 8) | (hash[i] & 0xFF);
            }

            // Convert to base62
            value = Math.abs(value);
            while (result.length() < length) {
                result.append(BASE62_CHARS.charAt((int) (value % 62)));
                value /= 62;
                if (value == 0) {
                    value = Math.abs(hash[result.length() % hash.length]);
                }
            }

            return result.substring(0, length);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 algorithm not available", e);
        }
    }
}

