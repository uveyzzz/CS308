package com.cs308.store.user;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * A field left out of the request keeps its current value. There is deliberately no role field:
 * nobody can change a role through the API, and the JSON parser ignores unknown fields.
 */
public record UpdateProfileRequest(
        @Size(max = 100) @Pattern(regexp = ".*\\S.*", message = "must not be blank") String fullName,
        @Size(max = 500) String homeAddress) {
}
