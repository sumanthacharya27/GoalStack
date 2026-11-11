package com.goalstack.service;

import com.goalstack.dto.AuthResponse;
import com.goalstack.dto.LoginRequest;
import com.goalstack.dto.RegisterRequest;
import com.goalstack.entity.User;
import com.goalstack.exception.EmailAlreadyUsedException;
import com.goalstack.exception.InvalidCredentialsException;
import com.goalstack.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    @InjectMocks
    private AuthService authService;

    @Test
    void registerStoresLowercaseEmailAndPasswordHash() {
        when(userRepository.existsByEmail("a@b.com")).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("HASH");
        when(userRepository.saveAndFlush(any(User.class))).thenAnswer(inv -> inv.getArgument(0));
        when(jwtService.createToken(any(User.class))).thenReturn("token-value");
        when(jwtService.getExpirySeconds()).thenReturn(7200L);

        RegisterRequest request = new RegisterRequest();
        request.setName("  Asha  ");
        request.setEmail("  A@B.com ");
        request.setPassword("password123");

        AuthResponse response = authService.register(request);

        ArgumentCaptor<User> saved = ArgumentCaptor.forClass(User.class);
        verify(userRepository).saveAndFlush(saved.capture());
        assertThat(saved.getValue().getEmail()).isEqualTo("a@b.com");
        assertThat(saved.getValue().getName()).isEqualTo("Asha");
        assertThat(saved.getValue().getPasswordHash()).isEqualTo("HASH");
        assertThat(response.token()).isEqualTo("token-value");
        assertThat(response.user().email()).isEqualTo("a@b.com");
    }

    @Test
    void registerRejectsAnEmailThatIsAlreadyUsed() {
        when(userRepository.existsByEmail("a@b.com")).thenReturn(true);
        RegisterRequest request = new RegisterRequest();
        request.setName("Asha");
        request.setEmail("a@b.com");
        request.setPassword("password123");

        assertThatThrownBy(() -> authService.register(request)).isInstanceOf(EmailAlreadyUsedException.class);
        verify(userRepository, never()).saveAndFlush(any());
    }

    @Test
    void loginSucceedsWithTheRightPassword() {
        User user = userWithHash("HASH");
        when(userRepository.findByEmail("a@b.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("password123", "HASH")).thenReturn(true);
        when(jwtService.createToken(user)).thenReturn("token-value");
        when(jwtService.getExpirySeconds()).thenReturn(7200L);

        AuthResponse response = authService.login(login("A@b.com", "password123"));

        assertThat(response.token()).isEqualTo("token-value");
        assertThat(response.tokenType()).isEqualTo("Bearer");
    }

    @Test
    void loginFailsWithAWrongPassword() {
        when(userRepository.findByEmail("a@b.com")).thenReturn(Optional.of(userWithHash("HASH")));
        when(passwordEncoder.matches("wrong-password", "HASH")).thenReturn(false);

        assertThatThrownBy(() -> authService.login(login("a@b.com", "wrong-password")))
                .isInstanceOf(InvalidCredentialsException.class);
        verify(jwtService, never()).createToken(any());
    }

    @Test
    void loginFailsWithTheSameErrorForAnUnknownEmail() {
        when(userRepository.findByEmail("nobody@b.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.login(login("nobody@b.com", "password123")))
                .isInstanceOf(InvalidCredentialsException.class);
    }

    private User userWithHash(String hash) {
        User user = new User();
        user.setName("Asha");
        user.setEmail("a@b.com");
        user.setPasswordHash(hash);
        return user;
    }

    private LoginRequest login(String email, String password) {
        LoginRequest request = new LoginRequest();
        request.setEmail(email);
        request.setPassword(password);
        return request;
    }
}
