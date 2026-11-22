package com.cs308.store.order;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import com.cs308.store.auth.JwtService;
import com.cs308.store.common.ConflictException;
import com.cs308.store.config.SecurityConfig;
import com.cs308.store.user.Role;

@WebMvcTest(OrderController.class)
@Import(SecurityConfig.class)
@MockitoBean(types = JwtService.class)
class OrderControllerTest {

    private static final long USER_ID = 7L;
    private static final String VALID_BODY = """
            {"items": [{"variantId": 10, "quantity": 2}],
             "shippingAddress": "Tuzla, Istanbul", "billingAddress": "Tuzla, Istanbul",
             "cardNumber": "4111111111111111"}
            """;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private OrderService orderService;

    /** Logs in the same way JwtAuthenticationFilter does: the principal is the user id. */
    private static RequestPostProcessor loggedIn(Role role) {
        return authentication(new UsernamePasswordAuthenticationToken(USER_ID, null,
                List.of(new SimpleGrantedAuthority(role.authority()))));
    }

    private ResultActions placeOrder(RequestPostProcessor login, String body) throws Exception {
        var request = post("/api/orders").contentType(MediaType.APPLICATION_JSON).content(body);
        return mockMvc.perform(login == null ? request : request.with(login));
    }

    @Test
    void guestMustLogInBeforeOrdering() throws Exception {
        placeOrder(null, VALID_BODY).andExpect(status().isUnauthorized());
        verify(orderService, never()).place(anyLong(), any());
    }

    @Test
    void staffCannotPlaceOrders() throws Exception {
        placeOrder(loggedIn(Role.SALES_MANAGER), VALID_BODY).andExpect(status().isForbidden());
        verify(orderService, never()).place(anyLong(), any());
    }

    @Test
    void customerPlacesAnOrderForThemselves() throws Exception {
        given(orderService.place(eq(USER_ID), any())).willReturn(new OrderResponse(1L, OrderStatus.PROCESSING,
                new BigDecimal("159.80"), "1111", Instant.now(), List.of()));

        placeOrder(loggedIn(Role.CUSTOMER), VALID_BODY)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("PROCESSING"))
                .andExpect(jsonPath("$.cardLast4").value("1111"));
    }

    @Test
    void emptyCartIsRejected() throws Exception {
        placeOrder(loggedIn(Role.CUSTOMER), """
                {"items": [], "shippingAddress": "Tuzla", "billingAddress": "Tuzla",
                 "cardNumber": "4111111111111111"}
                """)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.items").exists());
        verify(orderService, never()).place(anyLong(), any());
    }

    @Test
    void invalidCardNumberIsRejectedWithoutEchoingIt() throws Exception {
        placeOrder(loggedIn(Role.CUSTOMER), """
                {"items": [{"variantId": 10, "quantity": 1}], "shippingAddress": "Tuzla",
                 "billingAddress": "Tuzla", "cardNumber": "4111-abc"}
                """)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.cardNumber").value("Card number must be 13 to 19 digits"));
    }

    @Test
    void zeroQuantityIsRejected() throws Exception {
        placeOrder(loggedIn(Role.CUSTOMER), """
                {"items": [{"variantId": 10, "quantity": 0}], "shippingAddress": "Tuzla",
                 "billingAddress": "Tuzla", "cardNumber": "4111111111111111"}
                """)
                .andExpect(status().isBadRequest());
    }

    @Test
    void outOfStockBecomesA409() throws Exception {
        given(orderService.place(eq(USER_ID), any())).willThrow(new ConflictException("Not enough stock"));

        placeOrder(loggedIn(Role.CUSTOMER), VALID_BODY).andExpect(status().isConflict());
    }
}
