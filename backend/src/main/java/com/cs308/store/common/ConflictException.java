package com.cs308.store.common;

/** Throw when a request clashes with existing data; it becomes a 409. */
public class ConflictException extends RuntimeException {

    public ConflictException(String message) {
        super(message);
    }
}
