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
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import com.cs308.store.auth.JwtService;
import com.cs308.store.user.Role;
import com.cs308.store.user.UserRepository;

/**
 * Checks who can reach which area. The real management endpoints do not exist yet, so
 * {@link AreaEndpoints} stands in for them; only the paths matter to the security rules.
 */
@WebMvcTest(RoleAccessTest.AreaEndpoints.class)
@Import({ SecurityConfig.class, RoleAccessTest.AreaEndpoints.class })
@MockitoBean(types = { JwtService.class, UserRepository.class })
class RoleAccessTest {

    private static final String SALES = "/api/sales/ping";
    private static final String PRODUCT_MANAGER = "/api/product-manager/ping";
    private static final String SUPPORT = "/api/support/ping";
    private static final String ORDERS = "/api/orders/ping";

    @RestController
    static class AreaEndpoints {

        @GetMapping({ SALES, PRODUCT_MANAGER, SUPPORT, ORDERS, "/api/products/1" })
        String read() {
            return "ok";
        }

        @PostMapping({ "/api/products", "/api/categories" })
        String create() {
            return "ok";
        }

        @DeleteMapping({ "/api/products/1", "/api/categories/1" })
        String remove() {
            return "ok";
        }
    }

    @Autowired
    private MockMvc mockMvc;

    private static RequestPostProcessor loggedInAs(String role) {
        return user("someone").roles(role);
    }

    @ParameterizedTest
    @ValueSource(strings = { SALES, PRODUCT_MANAGER, SUPPORT, ORDERS })
    void withoutTokenReturns401(String path) throws Exception {
        mockMvc.perform(get(path))
                .andExpect(status().isUnauthorized());
    }

    // Every role against every management area: privileges must not mix (requirement 17).
    @ParameterizedTest
    @CsvSource({
            "CUSTOMER,        " + SALES + ",           403",
            "CUSTOMER,        " + PRODUCT_MANAGER + ", 403",
            "CUSTOMER,        " + SUPPORT + ",         403",
            "SALES_MANAGER,   " + SALES + ",           200",
            "SALES_MANAGER,   " + PRODUCT_MANAGER + ", 403",
            "SALES_MANAGER,   " + SUPPORT + ",         403",
            "PRODUCT_MANAGER, " + SALES + ",           403",
            "PRODUCT_MANAGER, " + PRODUCT_MANAGER + ", 200",
            "PRODUCT_MANAGER, " + SUPPORT + ",         403",
            "SUPPORT_AGENT,   " + SALES + ",           403",
            "SUPPORT_AGENT,   " + PRODUCT_MANAGER + ", 403",
            "SUPPORT_AGENT,   " + SUPPORT + ",         200",
    })
    void eachRoleReachesOnlyItsOwnArea(String role, String path, int expectedStatus) throws Exception {
        mockMvc.perform(get(path).with(loggedInAs(role)))
                .andExpect(status().is(expectedStatus));
    }

    @ParameterizedTest
    @EnumSource(Role.class)
    void everyLoggedInRoleReachesOrdinaryEndpoints(Role role) throws Exception {
        mockMvc.perform(get(ORDERS).with(loggedInAs(role.name())))
                .andExpect(status().isOk());
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
}
