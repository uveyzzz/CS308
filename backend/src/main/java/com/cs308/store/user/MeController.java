package com.cs308.store.user;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;

/**
 * The logged-in user's own account. The id always comes from the token, never from the request,
 * so nobody can reach another user's profile. The frontend uses the role here only to decide
 * which pages to show: SecurityConfig still checks it on every request.
 */
@RestController
@RequestMapping("/api/me")
public class MeController {

    private final UserService userService;

    public MeController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping
    public UserResponse currentUser(@AuthenticationPrincipal Long userId) {
        return UserResponse.from(userService.getById(userId));
    }

    @PatchMapping
    public UserResponse updateProfile(@AuthenticationPrincipal Long userId,
            @Valid @RequestBody UpdateProfileRequest request) {
        return UserResponse.from(userService.updateProfile(userId, request));
    }
}
