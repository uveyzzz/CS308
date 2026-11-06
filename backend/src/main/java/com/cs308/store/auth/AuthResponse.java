package com.cs308.store.auth;

import com.cs308.store.user.UserResponse;

public record AuthResponse(String token, UserResponse user) {
}
