package com.cs308.store.common;

/** Throw from a service when a requested entity does not exist; it becomes a 404. */
public class NotFoundException extends RuntimeException {

    public NotFoundException(String message) {
        super(message);
    }
}
