package com.cs308.store.common;

/** Throw when the caller could not prove who they are; it becomes a 401. */
public class UnauthorizedException extends RuntimeException {

    public UnauthorizedException(String message) {
        super(message);
    }
}
