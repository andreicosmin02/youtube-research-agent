package com.youtube.research.entity;

import com.youtube.research.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
public class UserEntityTest {

    @Autowired
    private UserRepository userRepository;

    @Test
    void shouldCreateAndSaveUser() {
        User user = new User();
        user.setUsername("testuser");
        user.setPasswordHash("hashedpassword123");

        User savedUser = userRepository.save(user);

        assertThat(savedUser.getId()).isNotNull();
        assertThat(savedUser.getUsername()).isEqualTo("testuser");
        assertThat(savedUser.getPasswordHash()).isEqualTo("hashedpassword123");
        assertThat(savedUser.getCreatedAt()).isNotNull();
        assertThat(savedUser.getUpdatedAt()).isNotNull();
    }

    @Test
    void shouldFindUserById() {
        User user = new User();
        user.setUsername("testuser");
        user.setPasswordHash("hash");
        User saved = userRepository.save(user);

        Optional<User> found = userRepository.findById(saved.getId());

        assertThat(found).isPresent();
        assertThat(found.get().getUsername()).isEqualTo("testuser");
    }

    @Test
    void shouldFindUserByUsername() {
        User user = new User();
        user.setUsername("alice");
        user.setPasswordHash("hashedpassword");
        userRepository.save(user);

        Optional<User> found = userRepository.findByUsername("alice");

        assertThat(found).isPresent();
        assertThat(found.get().getUsername()).isEqualTo("alice");
    }

    @Test
    void shouldUpdateUser() {
        User user = new User();
        user.setUsername("alice");
        user.setPasswordHash("oldpassword");
        User saved = userRepository.save(user);

        saved.setPasswordHash("newpassword");
        User updated = userRepository.save(saved);

        assertThat(updated.getPasswordHash()).isEqualTo("newpassword");
        assertThat(updated.getUpdatedAt()).isAfter(updated.getCreatedAt());
    }

    @Test
    void shouldDeleteUser() {
        User user = new User();
        user.setUsername("alice");
        user.setPasswordHash("password");
        User saved = userRepository.save(user);
        Long userId = saved.getId();

        userRepository.deleteById(userId);

        Optional<User> found = userRepository.findById(userId);
        assertThat(found).isEmpty();
    }
}
