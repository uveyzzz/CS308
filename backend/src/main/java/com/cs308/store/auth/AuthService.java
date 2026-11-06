package com.cs308.store.auth;

import java.util.Locale;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.cs308.store.common.ConflictException;
import com.cs308.store.common.UnauthorizedException;
import com.cs308.store.user.Role;
import com.cs308.store.user.User;
import com.cs308.store.user.UserRepository;
import com.cs308.store.user.UserResponse;

@Service
public class AuthService {

    private final UserRepository users;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthService(UserRepository users, PasswordEncoder passwordEncoder, JwtService jwtService) {
        this.users = users;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    public AuthResponse register(RegisterRequest request) {
        User user = new User(request.fullName().trim(), normalizeEmail(request.email()),
                passwordEncoder.encode(request.password()), Role.CUSTOMER, request.homeAddress());
        try {
            // The UNIQUE column decides, so two simultaneous sign-ups cannot both win (requirement 18).
            return responseFor(users.saveAndFlush(user));
        } catch (DataIntegrityViolationException e) {
            throw new ConflictException("This email is already registered");
        }
    }

    public AuthResponse login(LoginRequest request) {
        return users.findByEmail(normalizeEmail(request.email()))
                .filter(user -> passwordEncoder.matches(request.password(), user.getPasswordHash()))
                .map(this::responseFor)
                // One message for both cases, so nobody can test which emails have an account.
                .orElseThrow(() -> new UnauthorizedException("Invalid email or password"));
    }

    private AuthResponse responseFor(User user) {
        return new AuthResponse(jwtService.createToken(user.getId()), UserResponse.from(user));
    }

    /** Locale.ROOT matters: under a Turkish locale "INFO" would become "ınfo". */
    private String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
