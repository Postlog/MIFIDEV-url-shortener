package ru.maga.urlshortener.repository;

import ru.maga.urlshortener.domain.ShortUrl;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Thread-safe repository for managing short URLs.
 */
public class ShortUrlRepository {
    private final Map<String, ShortUrl> urlsByShortCode = new ConcurrentHashMap<>();
    private final Map<UUID, Set<String>> shortCodesByOwner = new ConcurrentHashMap<>();

    public void save(ShortUrl shortUrl) {
        urlsByShortCode.put(shortUrl.getShortCode(), shortUrl);
        shortCodesByOwner
                .computeIfAbsent(shortUrl.getOwnerId(), k -> ConcurrentHashMap.newKeySet())
                .add(shortUrl.getShortCode());
    }

    public Optional<ShortUrl> findByShortCode(String shortCode) {
        return Optional.ofNullable(urlsByShortCode.get(shortCode));
    }

    public List<ShortUrl> findByOwnerId(UUID ownerId) {
        Set<String> codes = shortCodesByOwner.get(ownerId);
        if (codes == null) {
            return Collections.emptyList();
        }
        return codes.stream()
                .map(urlsByShortCode::get)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    public void delete(String shortCode) {
        ShortUrl removed = urlsByShortCode.remove(shortCode);
        if (removed != null) {
            Set<String> codes = shortCodesByOwner.get(removed.getOwnerId());
            if (codes != null) {
                codes.remove(shortCode);
            }
        }
    }

    public List<ShortUrl> findAll() {
        return new ArrayList<>(urlsByShortCode.values());
    }

    public boolean exists(String shortCode) {
        return urlsByShortCode.containsKey(shortCode);
    }

    public int count() {
        return urlsByShortCode.size();
    }
}

