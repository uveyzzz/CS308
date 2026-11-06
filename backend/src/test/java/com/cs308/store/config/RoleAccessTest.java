package com.cs308.store.config;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import com.cs308.store.auth.JwtService;
import com.cs308.store.user.Role;

/**
 * Checks who can reach which area. The real admin endpoints do not exist yet, so
 * {@link AreaEndpoints} stands in for them; only the paths and annotations matter here.
 */
@WebMvcTest(RoleAccessTest.AreaEndpoints.class)
@Import({ SecurityConfig.class, RoleAccessTest.AreaEndpoints.class })
@MockitoBean(types = JwtService.class)
class RoleAccessTest {

    private static final String SALES = "/api/admin/sales/ping";
    private static final String PRODUCTS = "/api/admin/products/ping";
    private static final String SUPPORT = "/api/admin/support/ping";
    private static final String UNKNOWN_ADMIN_AREA = "/api/admin/other/ping";
    private static final String ORDERS = "/api/orders/ping";
    private static final String METHOD_CHECKED = "/api/reports/ping";

    @RestController
    public static class AreaEndpoints {

        @GetMapping({ SALES, PRODUCTS, SUPPORT, UNKNOWN_ADMIN_AREA, ORDERS, "/api/products/1" })
        public String read() {
            return "ok";
        }

        @PostMapping({ "/api/products", "/api/categories" })
        public String create() {
            return "ok";
        }

        @DeleteMapping({ "/api/products/1", "/api/categories/1" })
        public String remove() {
            return "ok";
        }

        /** Outside /api/admin, so only @PreAuthorize can stop a wrong role here. */
        @PreAuthorize("hasRole('SALES_MANAGER')")
        @GetMapping(METHOD_CHECKED)
        public String methodChecked() {
            return "ok";
        }
    }

    @Autowired
    private MockMvc mockMvc;

    private static RequestPostProcessor loggedInAs(String role) {
        return user("someone").roles(role);
    }

    @ParameterizedTest
    @ValueSource(strings = { SALES, PRODUCTS, SUPPORT, UNKNOWN_ADMIN_AREA, ORDERS, METHOD_CHECKED })
    void withoutTokenReturns401(String path) throws Exception {
        mockMvc.perform(get(path))
                .andExpect(status().isUnauthorized());
    }

    // Every role against every admin area: privileges must not mix (requirement 17).
    @ParameterizedTest
    @CsvSource({
            "CUSTOMER,        " + SALES + ",    403",
            "CUSTOMER,        " + PRODUCTS + ", 403",
            "CUSTOMER,        " + SUPPORT + ",  403",
            "SALES_MANAGER,   " + SALES + ",    200",
            "SALES_MANAGER,   " + PRODUCTS + ", 403",
            "SALES_MANAGER,   " + SUPPORT + ",  403",
            "PRODUCT_MANAGER, " + SALES + ",    403",
            "PRODUCT_MANAGER, " + PRODUCTS + ", 200",
            "PRODUCT_MANAGER, " + SUPPORT + ",  403",
            "SUPPORT_AGENT,   " + SALES + ",    403",
            "SUPPORT_AGENT,   " + PRODUCTS + ", 403",
            "SUPPORT_AGENT,   " + SUPPORT + ",  200",
    })
    void eachRoleReachesOnlyItsOwnArea(String role, String path, int expectedStatus) throws Exception {
        mockMvc.perform(get(path).with(loggedInAs(role)))
                .andExpect(status().is(expectedStatus));
    }

    @ParameterizedTest
    @EnumSource(Role.class)
    void anAdminPathWithoutAnOwnerIsClosedToEveryone(Role role) throws Exception {
        mockMvc.perform(get(UNKNOWN_ADMIN_AREA).with(loggedInAs(role.name())))
                .andExpect(status().isForbidden());
    }

    @ParameterizedTest
    @EnumSource(Role.class)
    void everyLoggedInRoleReachesOrdinaryEndpoints(Role role) throws Exception {
        mockMvc.perform(get(ORDERS).with(loggedInAs(role.name())))
                .andExpect(status().isOk());
    }

    @Test
    void preAuthorizeLetsTheRightRoleThrough() throws Exception {
        mockMvc.perform(get(METHOD_CHECKED).with(loggedInAs("SALES_MANAGER")))
                .andExpect(status().isOk());
    }

    @Test
    void preAuthorizeStopsAWrongRoleWithTheCommonErrorFormat() throws Exception {
        mockMvc.perform(get(METHOD_CHECKED).with(loggedInAs("CUSTOMER")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("Access denied"));
    }

    @Test
    void guestCanReadTheCatalog() throws Exception {
        mockMvc.perform(get("/api/products/1"))
                .andExpect(status().isOk());
    }

    @Test
    void guestCannotChangeTheCatalog() throws Exception {
        mockMvc.perform(post("/api/products"))
                .andExpect(status().isUnauthorized());
    }

    @ParameterizedTest
    @ValueSource(strings = { "CUSTOMER", "SALES_MANAGER", "SUPPORT_AGENT" })
    void onlyProductManagersChangeTheCatalog(String role) throws Exception {
        mockMvc.perform(post("/api/products").with(loggedInAs(role)))
                .andExpect(status().isForbidden());
        mockMvc.perform(delete("/api/categories/1").with(loggedInAs(role)))
                .andExpect(status().isForbidden());
    }

    @Test
    void productManagerChangesTheCatalog() throws Exception {
        mockMvc.perform(post("/api/categories").with(loggedInAs("PRODUCT_MANAGER")))
                .andExpect(status().isOk());
        mockMvc.perform(delete("/api/products/1").with(loggedInAs("PRODUCT_MANAGER")))
                .andExpect(status().isOk());
    }

    @Test
    void forbiddenResponseUsesTheCommonErrorFormat() throws Exception {
        mockMvc.perform(get(SALES).with(loggedInAs("CUSTOMER")))
                .andExpect(status().isForbidden())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.message").value("Access denied"))
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    void unauthorizedResponseUsesTheCommonErrorFormat() throws Exception {
        mockMvc.perform(get(SALES))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.message").value("Authentication required"))
                .andExpect(jsonPath("$.timestamp").exists());
    }
}
