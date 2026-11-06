package com.cs308.store.auth;

import java.nio.charset.StandardCharsets;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** There is deliberately no role field: every new account is a customer. */
public record RegisterRequest(
        @NotBlank @Size(max = 100) String fullName,
        @NotBlank @Email @Size(max = 255) String email,
        @NotBlank @Size(min = 8) String password,
        @Size(max = 500) String homeAddress) {

    private static final int BCRYPT_MAX_BYTES = 72;

    /** BCrypt reads at most 72 bytes, and Turkish letters take two bytes each. */
    @AssertTrue(message = "must be at most 72 bytes")
    public boolean isPasswordShortEnough() {
        return password == null || password.getBytes(StandardCharsets.UTF_8).length <= BCRYPT_MAX_BYTES;
    }
}
