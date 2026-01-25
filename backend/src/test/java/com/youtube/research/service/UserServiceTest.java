package com.youtube.research.service;

import com.youtube.research.entity.User;
import com.youtube.research.repository.UserRepository;
import com.youtube.research.security.UserInputValidator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserInputValidator inputValidator;

    @InjectMocks
    private UserService userService;

    @Test
    void shouldRegisterNewUser() {
        // Arrange
        String username = "testuser";
        String password = "MyPassword123";

        User expectedUser = new User();
        expectedUser.setId(1L);
        expectedUser.setUsername(username);
        expectedUser.setPasswordHash("hashed_password");

        // Mock the validator to pass (don't throw)
        doNothing().when(inputValidator).validateUsername(username);
        doNothing().when(inputValidator).validatePassword(password);

        when(userRepository.findByUsername(username))
                .thenReturn(Optional.empty());

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
        String plainPassword = "MySecurePassword123";

        // Mock the validator to pass
        doNothing().when(inputValidator).validateUsername(username);
        doNothing().when(inputValidator).validatePassword(plainPassword);

        when(userRepository.findByUsername(username))
                .thenReturn(Optional.empty());

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
    void shouldRejectWeakPassword() {
        // Arrange
        String username = "testuser";
        String weakPassword = "weak";

        // Mock validator to throw exception for weak password
        doNothing().when(inputValidator).validateUsername(username);
        doThrow(new IllegalArgumentException("Password must contain at least one uppercase letter, one lowercase letter, and one number"))
                .when(inputValidator).validatePassword(weakPassword);

        // Act & Assert
        assertThatThrownBy(() -> userService.registerUser(username, weakPassword))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("uppercase");
    }

    @Test
    void shouldThrowExceptionWhenUsernameAlreadyExists() {
        // Arrange
        String username = "existinguser";
        String password = "MyPassword123";

        // Mock validator to pass
        doNothing().when(inputValidator).validateUsername(username);
        doNothing().when(inputValidator).validatePassword(password);

        User existingUser = new User();
        existingUser.setId(1L);
        existingUser.setUsername(username);

        when(userRepository.findByUsername(username))
                .thenReturn(Optional.of(existingUser));

        // Act & Assert
        assertThatThrownBy(() -> userService.registerUser(username, password))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Username already exists");
    }

    @Test
    void shouldThrowExceptionWhenUsernameIsInvalid() {
        // Arrange
        String invalidUsername = "";
        String password = "MyPassword123";

        // Mock validator to throw exception for invalid username
        doThrow(new IllegalArgumentException("Username cannot be empty"))
                .when(inputValidator).validateUsername(invalidUsername);

        // Act & Assert
        assertThatThrownBy(() -> userService.registerUser(invalidUsername, password))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Username cannot be empty");
    }

    @Test
    void shouldAuthenticateUserWithCorrectPassword() {
        // Arrange
        String username = "testuser";
        String password = "MyPassword123";

        User user = new User();
        user.setId(1L);
        user.setUsername(username);
        // Use a real BCrypt hash of "MyPassword123"
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        user.setPasswordHash(encoder.encode(password));

        when(userRepository.findByUsername(username))
                .thenReturn(Optional.of(user));

        // Act
        boolean isAuthenticated = userService.authenticateUser(username, password);

        // Assert
        assertThat(isAuthenticated).isTrue();
    }

    @Test
    void shouldNotAuthenticateUserWithWrongPassword() {
        // Arrange
        String username = "testuser";
        String correctPassword = "MyPassword123";
        String wrongPassword = "WrongPassword456";

        User user = new User();
        user.setId(1L);
        user.setUsername(username);
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        user.setPasswordHash(encoder.encode(correctPassword));

        when(userRepository.findByUsername(username))
                .thenReturn(Optional.of(user));

        // Act
        boolean isAuthenticated = userService.authenticateUser(username, wrongPassword);

        // Assert
        assertThat(isAuthenticated).isFalse();
    }

    @Test
    void shouldNotAuthenticateNonExistentUser() {
        // Arrange
        String username = "nonexistent";
        String password = "MyPassword123";

        when(userRepository.findByUsername(username))
                .thenReturn(Optional.empty());

        // Act
        boolean isAuthenticated = userService.authenticateUser(username, password);

        // Assert
        assertThat(isAuthenticated).isFalse();
    }
}