package ru.maga.urlshortener.util;

import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;

/**
 * Utility class for URL validation.
 */
public class UrlValidator {

    public static boolean isValid(String url) {
        if (url == null || url.trim().isEmpty()) {
            return false;
        }

        try {
            new URL(url).toURI();
            return url.startsWith("http://") || url.startsWith("https://");
        } catch (MalformedURLException | URISyntaxException e) {
            return false;
        }
    }

    public static void validate(String url) {
        if (!isValid(url)) {
            throw new IllegalArgumentException("Некорректный URL: " + url);
        }
    }
}

