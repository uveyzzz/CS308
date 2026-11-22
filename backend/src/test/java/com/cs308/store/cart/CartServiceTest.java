package com.cs308.store.cart;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;

import com.cs308.store.catalog.ProductVariant;
import com.cs308.store.catalog.ProductVariantRepository;
import com.cs308.store.common.NotFoundException;

class CartServiceTest {

    private static final long USER_ID = 7L;

    private final CartItemRepository cartItems = mock(CartItemRepository.class);
    private final ProductVariantRepository variants = mock(ProductVariantRepository.class);
    private final CartService cartService = new CartService(cartItems, variants);

    @BeforeEach
    void noVariantsUnlessGiven() {
        given(variants.findById(any())).willReturn(Optional.empty());
        given(cartItems.findById(any())).willReturn(Optional.empty());
    }

    private void variantWithStock(long id, int stock) {
        ProductVariant variant = new ProductVariant(1L, "M", "Black", stock, "SKU-" + id);
        ReflectionTestUtils.setField(variant, "id", id);
        given(variants.findById(id)).willReturn(Optional.of(variant));
    }

    private void lineInCart(long variantId, int quantity) {
        given(cartItems.findById(new CartItemId(USER_ID, variantId)))
                .willReturn(Optional.of(new CartItem(USER_ID, variantId, quantity)));
    }

    private CartItem savedLine() {
        ArgumentCaptor<CartItem> saved = ArgumentCaptor.forClass(CartItem.class);
        verify(cartItems).save(saved.capture());
        return saved.getValue();
    }

    @Test
    void addsANewLine() {
        variantWithStock(10, 5);

        cartService.add(USER_ID, new CartLineRequest(10L, 2));

        CartItem line = savedLine();
        assertThat(line.getUserId()).isEqualTo(USER_ID);
        assertThat(line.getVariantId()).isEqualTo(10L);
        assertThat(line.getQuantity()).isEqualTo(2);
    }

    @Test
    void addingTheSameVariantRaisesTheQuantityUpToTheStock() {
        variantWithStock(10, 5);
        lineInCart(10, 4);

        cartService.add(USER_ID, new CartLineRequest(10L, 3));

        assertThat(savedLine().getQuantity()).isEqualTo(5);
    }

    @Test
    void addingAnUnknownVariantIsA404() {
        assertThatThrownBy(() -> cartService.add(USER_ID, new CartLineRequest(99L, 1)))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void anOutOfStockVariantIsNotAdded() {
        variantWithStock(10, 0);

        cartService.add(USER_ID, new CartLineRequest(10L, 1));

        verify(cartItems, never()).save(any());
    }

    // The acceptance criterion of SCRUM-24: items added before login are still there after it.
    @Test
    void mergeAddsTheGuestLinesToTheSavedCart() {
        variantWithStock(10, 5);
        lineInCart(10, 1);

        cartService.merge(USER_ID, List.of(new CartLineRequest(10L, 2)));

        assertThat(savedLine().getQuantity()).isEqualTo(3);
    }

    @Test
    void mergeAddsUpRepeatedLinesAndCapsThemAtTheStock() {
        variantWithStock(10, 4);

        cartService.merge(USER_ID, List.of(new CartLineRequest(10L, 2), new CartLineRequest(10L, 3)));

        assertThat(savedLine().getQuantity()).isEqualTo(4);
    }

    @Test
    void mergeSkipsUnknownAndOutOfStockVariantsButKeepsTheRest() {
        variantWithStock(10, 5);
        variantWithStock(11, 0);

        cartService.merge(USER_ID, List.of(new CartLineRequest(99L, 1), new CartLineRequest(11L, 1),
                new CartLineRequest(10L, 2)));

        CartItem line = savedLine();
        assertThat(line.getVariantId()).isEqualTo(10L);
        assertThat(line.getQuantity()).isEqualTo(2);
    }

    @Test
    void setQuantityIsCappedAtTheStock() {
        variantWithStock(10, 3);
        lineInCart(10, 1);

        cartService.setQuantity(USER_ID, 10L, 8);

        assertThat(savedLine().getQuantity()).isEqualTo(3);
    }

    @Test
    void setQuantityRemovesTheLineWhenNothingIsLeftInStock() {
        variantWithStock(10, 0);
        lineInCart(10, 2);

        cartService.setQuantity(USER_ID, 10L, 1);

        verify(cartItems).deleteById(new CartItemId(USER_ID, 10L));
        verify(cartItems, never()).save(any());
    }

    @Test
    void removeOnlyTouchesTheUsersOwnLine() {
        cartService.remove(USER_ID, 10L);

        verify(cartItems).deleteById(new CartItemId(USER_ID, 10L));
    }

    @Test
    void sumByVariantAddsUpLinesForTheSameVariant() {
        assertThat(CartService.sumByVariant(List.of(new CartLineRequest(2L, 1), new CartLineRequest(1L, 2),
                new CartLineRequest(2L, 3)))).containsExactly(java.util.Map.entry(1L, 2), java.util.Map.entry(2L, 4));
    }
}
