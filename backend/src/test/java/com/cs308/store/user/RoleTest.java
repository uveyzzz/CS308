package com.cs308.store.user;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class RoleTest {

    @Test
    void thereAreExactlyTheFourRolesOfRequirement10() {
        assertThat(Role.values()).containsExactlyInAnyOrder(
                Role.CUSTOMER, Role.SALES_MANAGER, Role.PRODUCT_MANAGER, Role.SUPPORT_AGENT);
    }

    @Test
    void authorityHasThePrefixSpringSecurityExpects() {
        assertThat(Role.SALES_MANAGER.authority()).isEqualTo("ROLE_SALES_MANAGER");
    }
}
