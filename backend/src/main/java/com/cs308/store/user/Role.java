package com.cs308.store.user;

/**
 * The four roles from requirement 10. Each user has exactly one, so privileges never mix
 * (requirement 17). The names match the values allowed in the users.role column.
 */
public enum Role {
    CUSTOMER,
    SALES_MANAGER,
    PRODUCT_MANAGER,
    SUPPORT_AGENT;

    private static final String AUTHORITY_PREFIX = "ROLE_";

    /** The authority Spring Security expects: hasRole("SALES_MANAGER") looks for ROLE_SALES_MANAGER. */
    public String authority() {
        return AUTHORITY_PREFIX + name();
    }
}
