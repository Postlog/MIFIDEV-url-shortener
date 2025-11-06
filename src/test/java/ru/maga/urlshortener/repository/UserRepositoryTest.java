package ru.maga.urlshortener.repository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ru.maga.urlshortener.domain.User;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class UserRepositoryTest {

    private UserRepository repository;

    @BeforeEach
    void setUp() {
        repository = new UserRepository();
    }

    @Test
    void shouldSaveAndFindUser() {
        User user = User.create();
        repository.save(user);

        Optional<User> found = repository.findById(user.getId());

        assertThat(found).isPresent();
        assertThat(found.get()).isEqualTo(user);
    }

    @Test
    void shouldReturnEmptyWhenUserNotFound() {
        UUID randomId = UUID.randomUUID();

        Optional<User> found = repository.findById(randomId);

        assertThat(found).isEmpty();
    }

    @Test
    void shouldCheckUserExists() {
        User user = User.create();
        repository.save(user);

        assertThat(repository.exists(user.getId())).isTrue();
        assertThat(repository.exists(UUID.randomUUID())).isFalse();
    }

    @Test
    void shouldCountUsers() {
        assertThat(repository.count()).isZero();

        repository.save(User.create());
        assertThat(repository.count()).isEqualTo(1);

        repository.save(User.create());
        assertThat(repository.count()).isEqualTo(2);
    }
}

