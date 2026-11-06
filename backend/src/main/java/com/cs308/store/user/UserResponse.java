package com.cs308.store.user;

/** What the API shows about a user. The password hash is never part of it. */
public record UserResponse(Long id, String fullName, String email, Role role, String homeAddress) {

    public static UserResponse from(User user) {
        return new UserResponse(user.getId(), user.getFullName(), user.getEmail(), user.getRole(),
                user.getHomeAddress());
    }
}
