package com.cs308.store.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.util.Locale;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import com.cs308.store.common.ConflictException;
import com.cs308.store.common.UnauthorizedException;
import com.cs308.store.user.Role;
import com.cs308.store.user.User;
import com.cs308.store.user.UserRepository;

class AuthServiceTest {

    private static final String PASSWORD = "correct-password";

    private final UserRepository users = mock(UserRepository.class);
    // Strength 4 is the lowest BCrypt allows; it only keeps these tests fast.
    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder(4);
    private final JwtService jwtService = new JwtService("test-secret-that-is-long-enough-for-hs256", 60);
    private final AuthService authService = new AuthService(users, passwordEncoder, jwtService);

    @BeforeEach
    void saveAssignsAnId() {
        given(users.saveAndFlush(any(User.class))).willAnswer(call -> {
            User user = call.getArgument(0);
            ReflectionTestUtils.setField(user, "id", 5L);
            return user;
        });
    }

    private static RegisterRequest registerRequest(String email) {
        return new RegisterRequest("Ada Lovelace", email, PASSWORD, "Istanbul");
    }

    private User storedUser() {
        User user = new User("Ada Lovelace", "ada@example.com", passwordEncoder.encode(PASSWORD), Role.CUSTOMER, null);
        ReflectionTestUtils.setField(user, "id", 5L);
        return user;
    }

    private User savedUser() {
        ArgumentCaptor<User> saved = ArgumentCaptor.forClass(User.class);
        verify(users).saveAndFlush(saved.capture());
        return saved.getValue();
    }

    @Test
    void newAccountIsAlwaysACustomer() {
        AuthResponse response = authService.register(registerRequest("ada@example.com"));

        assertThat(savedUser().getRole()).isEqualTo(Role.CUSTOMER);
        assertThat(response.user().role()).isEqualTo(Role.CUSTOMER);
    }

    @Test
    void passwordIsStoredOnlyAsAHash() {
        authService.register(registerRequest("ada@example.com"));

        String hash = savedUser().getPasswordHash();
        assertThat(hash).isNotEqualTo(PASSWORD);
        assertThat(passwordEncoder.matches(PASSWORD, hash)).isTrue();
    }

    @Test
    void registeringLogsTheUserIn() {
        AuthResponse response = authService.register(registerRequest("ada@example.com"));

        assertThat(jwtService.readClaims(response.token())).contains(new TokenClaims(5L, Role.CUSTOMER));
    }

    @Test
    void emailIsLowerCasedEvenOnATurkishMachine() {
        Locale original = Locale.getDefault();
        Locale.setDefault(Locale.forLanguageTag("tr-TR"));
        try {
            authService.register(registerRequest("  INFO@Example.COM "));
        } finally {
            Locale.setDefault(original);
        }

        assertThat(savedUser().getEmail()).isEqualTo("info@example.com");
    }

    @Test
    void takenEmailBecomesAConflict() {
        given(users.saveAndFlush(any(User.class))).willThrow(new DataIntegrityViolationException("duplicate"));

        assertThatThrownBy(() -> authService.register(registerRequest("ada@example.com")))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void correctPasswordReturnsATokenForThatUser() {
        given(users.findByEmail("ada@example.com")).willReturn(Optional.of(storedUser()));

        AuthResponse response = authService.login(new LoginRequest("ADA@example.com", PASSWORD));

        assertThat(jwtService.readClaims(response.token())).contains(new TokenClaims(5L, Role.CUSTOMER));
        assertThat(response.user().email()).isEqualTo("ada@example.com");
    }

    @Test
    void wrongPasswordAndUnknownEmailGiveTheSameError() {
        given(users.findByEmail("ada@example.com")).willReturn(Optional.of(storedUser()));
        given(users.findByEmail("nobody@example.com")).willReturn(Optional.empty());

        assertThatThrownBy(() -> authService.login(new LoginRequest("ada@example.com", "wrong-password")))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessage("Invalid email or password");
        assertThatThrownBy(() -> authService.login(new LoginRequest("nobody@example.com", PASSWORD)))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessage("Invalid email or password");
    }
}
