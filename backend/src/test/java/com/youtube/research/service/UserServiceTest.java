package com.youtube.research.service;

import com.youtube.research.entity.User;
import com.youtube.research.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserService userService;

    @Test
    void shouldRegisterNewUser() {
        // Arrange
        String username = "testuser";
        String password = "plainpassword123";

        User expectedUser = new User();
        expectedUser.setId(1L);
        expectedUser.setUsername(username);
        expectedUser.setPasswordHash("hashed_password");

        when(userRepository.save(any(User.class)))
                .thenReturn(expectedUser);

        // Act
        User registeredUser = userService.registerUser(username, password);

        // Assert
        assertThat(registeredUser.getId()).isEqualTo(1L);
        assertThat(registeredUser.getUsername()).isEqualTo(username);
    }

    @Test
    void shouldHashPasswordWhenRegisteringUser() {
        // Arrange
        String username = "testuser";
        String plainPassword = "mySecurePassword123!";

        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User userBeingSaved = invocation.getArgument(0);
            userBeingSaved.setId(1L);
            return userBeingSaved;
        });

        // Act
        User registeredUser = userService.registerUser(username, plainPassword);

        // Assert - verify BCrypt hashed the password correctly
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        boolean passwordMatches = encoder.matches(plainPassword, registeredUser.getPasswordHash());
        assertThat(passwordMatches)
                .as("Plain password should match the hashed password")
                .isTrue();
    }

    @Test
    void shouldThrowExceptionWhenUsernameAlreadyExists() {
        // Arrange
        String username = "existinguser";
        String password = "password123";

        when(userRepository.findByUsername(username))
                .thenReturn(java.util.Optional.of(new User()));

        // Act & Assert
        assertThatThrownBy(() -> userService.registerUser(username, password))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Username already exists");
    }
}
