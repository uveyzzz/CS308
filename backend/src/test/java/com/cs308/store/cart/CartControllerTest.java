package com.cs308.store.cart;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import com.cs308.store.auth.JwtService;
import com.cs308.store.common.NotFoundException;
import com.cs308.store.config.SecurityConfig;
import com.cs308.store.user.Role;

@WebMvcTest(CartController.class)
@Import(SecurityConfig.class)
@MockitoBean(types = JwtService.class)
class CartControllerTest {

    private static final long USER_ID = 7L;
    private static final CartLine SHIRT = new CartLine(10L, 1L, "oxford-shirt", "Oxford Shirt", "M", "Blue",
            new BigDecimal("40.00"), "/shirt.jpg", 2, 5);

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CartService cartService;

    /** Logs in the same way JwtAuthenticationFilter does: the principal is the user id. */
    private static RequestPostProcessor loggedIn(Role role) {
        return authentication(new UsernamePasswordAuthenticationToken(USER_ID, null,
                List.of(new SimpleGrantedAuthority(role.authority()))));
    }

    private static RequestPostProcessor customer() {
        return loggedIn(Role.CUSTOMER);
    }

    @Test
    void guestsKeepTheirCartInTheBrowser() throws Exception {
        mockMvc.perform(get("/api/cart")).andExpect(status().isUnauthorized());
    }

    @ParameterizedTest
    @EnumSource(value = Role.class, names = "CUSTOMER", mode = EnumSource.Mode.EXCLUDE)
    void staffHaveNoCart(Role role) throws Exception {
        mockMvc.perform(get("/api/cart").with(loggedIn(role))).andExpect(status().isForbidden());
        verify(cartService, never()).get(anyLong());
    }

    @Test
    void returnsTheCartOfTheUserInTheToken() throws Exception {
        given(cartService.get(USER_ID)).willReturn(List.of(SHIRT));

        mockMvc.perform(get("/api/cart").with(customer()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].variantId").value(10))
                .andExpect(jsonPath("$[0].name").value("Oxford Shirt"))
                .andExpect(jsonPath("$[0].price").value(40.00))
                .andExpect(jsonPath("$[0].quantity").value(2))
                .andExpect(jsonPath("$[0].maxStock").value(5));
    }

    @Test
    void mergesTheGuestCartForTheUserInTheToken() throws Exception {
        given(cartService.merge(eq(USER_ID), any())).willReturn(List.of(SHIRT));

        mockMvc.perform(post("/api/cart/merge").with(customer()).contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"items": [{"variantId": 10, "quantity": 2}]}
                        """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].variantId").value(10));

        verify(cartService).merge(USER_ID, List.of(new CartLineRequest(10L, 2)));
    }

    @Test
    void rejectsAZeroQuantity() throws Exception {
        mockMvc.perform(post("/api/cart/items").with(customer()).contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"variantId": 10, "quantity": 0}
                        """))
                .andExpect(status().isBadRequest());
        verify(cartService, never()).add(anyLong(), any());
    }

    @Test
    void rejectsABadLineInsideTheMergedCart() throws Exception {
        mockMvc.perform(post("/api/cart/merge").with(customer()).contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"items": [{"variantId": null, "quantity": 1}]}
                        """))
                .andExpect(status().isBadRequest());
        verify(cartService, never()).merge(anyLong(), any());
    }

    @Test
    void unknownVariantIsA404() throws Exception {
        given(cartService.add(eq(USER_ID), any())).willThrow(new NotFoundException("Product variant 99 does not exist"));

        mockMvc.perform(post("/api/cart/items").with(customer()).contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"variantId": 99, "quantity": 1}
                        """))
                .andExpect(status().isNotFound());
    }

    @Test
    void setsAQuantity() throws Exception {
        mockMvc.perform(put("/api/cart/items/10").with(customer()).contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"quantity": 3}
                        """))
                .andExpect(status().isOk());

        verify(cartService).setQuantity(USER_ID, 10L, 3);
    }

    @Test
    void removesALineAndClearsTheCart() throws Exception {
        mockMvc.perform(delete("/api/cart/items/10").with(customer())).andExpect(status().isOk());
        mockMvc.perform(delete("/api/cart").with(customer())).andExpect(status().isNoContent());

        verify(cartService).remove(USER_ID, 10L);
        verify(cartService).clear(USER_ID);
    }
}
