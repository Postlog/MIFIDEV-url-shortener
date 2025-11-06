package ru.maga.urlshortener.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class UrlShortenerServiceTest {

    private UrlShortenerService service;

    @BeforeEach
    void setUp() {
        service = new UrlShortenerService(6);
    }

    @Test
    void shouldGenerateShortCode() {
        String url = "https://example.com";
        UUID userId = UUID.randomUUID();

        String shortCode = service.generateShortCode(url, userId);

        assertThat(shortCode).isNotNull();
        assertThat(shortCode).hasSize(6);
    }

    @Test
    void shouldGenerateCodeWithOnlyAllowedCharacters() {
        String url = "https://example.com";
        UUID userId = UUID.randomUUID();

        String shortCode = service.generateShortCode(url, userId);

        assertThat(shortCode).matches("[0-9A-Za-z]+");
    }

    @Test
    void shouldGenerateDifferentCodesForDifferentUsers() {
        String url = "https://example.com";
        UUID user1 = UUID.randomUUID();
        UUID user2 = UUID.randomUUID();

        String code1 = service.generateShortCode(url, user1);
        String code2 = service.generateShortCode(url, user2);

        assertThat(code1).isNotEqualTo(code2);
    }

    @Test
    void shouldGenerateDifferentCodesForDifferentUrls() {
        UUID userId = UUID.randomUUID();
        String url1 = "https://example.com";
        String url2 = "https://another.com";

        String code1 = service.generateShortCode(url1, userId);
        String code2 = service.generateShortCode(url2, userId);

        assertThat(code1).isNotEqualTo(code2);
    }

    @Test
    void shouldGenerateDifferentCodesForSameInputCalledMultipleTimes() {
        String url = "https://example.com";
        UUID userId = UUID.randomUUID();

        String code1 = service.generateShortCode(url, userId);
        String code2 = service.generateShortCode(url, userId);

        // Due to nanoTime, should be different
        assertThat(code1).isNotEqualTo(code2);
    }

    @Test
    void shouldGenerateUniqueCodesInBulk() {
        String url = "https://example.com";
        UUID userId = UUID.randomUUID();
        Set<String> codes = new HashSet<>();

        for (int i = 0; i < 1000; i++) {
            String code = service.generateShortCode(url, userId);
            codes.add(code);
        }

        // All codes should be unique due to nanoTime
        assertThat(codes).hasSize(1000);
    }

    @Test
    void shouldRespectConfiguredCodeLength() {
        UrlShortenerService customService = new UrlShortenerService(8);
        String url = "https://example.com";
        UUID userId = UUID.randomUUID();

        String shortCode = customService.generateShortCode(url, userId);

        assertThat(shortCode).hasSize(8);
    }
}

