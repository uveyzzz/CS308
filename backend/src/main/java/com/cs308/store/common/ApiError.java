package com.cs308.store.common;

import java.time.Instant;
import java.util.Map;

/**
 * The one error shape every endpoint returns, so the frontend handles errors in one place.
 *
 * @param fieldErrors field name -> message, filled only for validation errors
 */
public record ApiError(int status, String message, Map<String, String> fieldErrors, Instant timestamp) {

    public static ApiError of(int status, String message) {
        return new ApiError(status, message, Map.of(), Instant.now());
    }
}
