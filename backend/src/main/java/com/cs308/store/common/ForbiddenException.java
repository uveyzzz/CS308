package com.cs308.store.common;

/**
 * Throw when the user is logged in with the right role but a business rule does not let them
 * do this (for example, reviewing a product they have not received). It becomes a 403 with
 * the message, unlike a role check, which answers only "Access denied".
 */
public class ForbiddenException extends RuntimeException {

    public ForbiddenException(String message) {
        super(message);
    }
}
