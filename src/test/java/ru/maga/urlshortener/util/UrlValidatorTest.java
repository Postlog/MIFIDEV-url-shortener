package ru.maga.urlshortener.util;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class UrlValidatorTest {

    @Test
    void shouldValidateCorrectHttpUrl() {
        assertThat(UrlValidator.isValid("http://example.com")).isTrue();
    }

    @Test
    void shouldValidateCorrectHttpsUrl() {
        assertThat(UrlValidator.isValid("https://example.com")).isTrue();
    }

    @Test
    void shouldValidateUrlWithPath() {
        assertThat(UrlValidator.isValid("https://example.com/path/to/resource")).isTrue();
    }

    @Test
    void shouldValidateUrlWithQueryParams() {
        assertThat(UrlValidator.isValid("https://example.com?param=value&foo=bar")).isTrue();
    }

    @Test
    void shouldRejectNullUrl() {
        assertThat(UrlValidator.isValid(null)).isFalse();
    }

    @Test
    void shouldRejectEmptyUrl() {
        assertThat(UrlValidator.isValid("")).isFalse();
        assertThat(UrlValidator.isValid("   ")).isFalse();
    }

    @Test
    void shouldRejectUrlWithoutProtocol() {
        assertThat(UrlValidator.isValid("example.com")).isFalse();
        assertThat(UrlValidator.isValid("www.example.com")).isFalse();
    }

    @Test
    void shouldRejectInvalidProtocol() {
        assertThat(UrlValidator.isValid("ftp://example.com")).isFalse();
        assertThat(UrlValidator.isValid("file:///path/to/file")).isFalse();
    }

    @Test
    void shouldRejectMalformedUrl() {
        assertThat(UrlValidator.isValid("https://")).isFalse();
        assertThat(UrlValidator.isValid("http://invalid url with spaces")).isFalse();
    }

    @Test
    void shouldThrowExceptionForInvalidUrl() {
        assertThatThrownBy(() -> UrlValidator.validate("invalid-url"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Некорректный URL");
    }

    @Test
    void shouldNotThrowExceptionForValidUrl() {
        UrlValidator.validate("https://example.com");
    }
}

