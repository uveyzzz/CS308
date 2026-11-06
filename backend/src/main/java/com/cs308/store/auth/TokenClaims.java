package com.cs308.store.auth;

import com.cs308.store.user.Role;

/** What a valid login token says about its owner. */
public record TokenClaims(long userId, Role role) {
}
