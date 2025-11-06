package ru.maga.urlshortener.repository;

import ru.maga.urlshortener.domain.User;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Thread-safe repository for managing users.
 */
public class UserRepository {
    private final Map<UUID, User> users = new ConcurrentHashMap<>();

    public void save(User user) {
        users.put(user.getId(), user);
    }

    public Optional<User> findById(UUID userId) {
        return Optional.ofNullable(users.get(userId));
    }

    public boolean exists(UUID userId) {
        return users.containsKey(userId);
    }

    public int count() {
        return users.size();
    }
}

