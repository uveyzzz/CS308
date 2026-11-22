package com.cs308.store.order;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;

import com.cs308.store.catalog.OrderableVariant;
import com.cs308.store.catalog.ProductVariantRepository;
import com.cs308.store.common.ConflictException;
import com.cs308.store.common.NotFoundException;
import com.cs308.store.payment.PaymentGateway;

class OrderServiceTest {

    private static final long USER = 7L;
    private static final long TEE = 10L;
    private static final long HOODIE = 11L;
    private static final String CARD = "4111111111111111";

    private OrderRepository orders;
    private OrderItemRepository orderItems;
    private ProductVariantRepository variants;
    private PaymentGateway paymentGateway;
    private OrderService service;

    @BeforeEach
    void setUp() {
        orders = mock(OrderRepository.class);
        orderItems = mock(OrderItemRepository.class);
        variants = mock(ProductVariantRepository.class);
        paymentGateway = mock(PaymentGateway.class);
        service = new OrderService(orders, orderItems, variants, paymentGateway);
        given(orders.save(any(Order.class))).willAnswer(invocation -> invocation.getArgument(0));
        given(paymentGateway.charge(any(), anyString())).willReturn("MOCK-1");
    }

    /** A variant as the database query returns it. */
    private static OrderableVariant variant(long id, String name, String size, String color, String price,
            String cost, boolean active) {
        return new OrderableVariant() {
            public Long getVariantId() { return id; }
            public String getProductName() { return name; }
            public String getSize() { return size; }
            public String getColor() { return color; }
            public BigDecimal getUnitPrice() { return new BigDecimal(price); }
            public BigDecimal getDiscountRate() { return new BigDecimal("0.00"); }
            public BigDecimal getUnitCost() { return new BigDecimal(cost); }
            public Boolean getActive() { return active; }
        };
    }

    private static final OrderableVariant TEE_M_BLACK = variant(TEE, "Basic Tee", "M", "Black", "79.90", "40.00", true);
    private static final OrderableVariant HOODIE_L_GREY = variant(HOODIE, "Hoodie", "L", "Grey", "120.00", "60.00", true);

    private static PlaceOrderRequest request(OrderLineRequest... lines) {
        return new PlaceOrderRequest(List.of(lines), "Tuzla, Istanbul", "Tuzla, Istanbul", CARD);
    }

    private void inStock(OrderableVariant... found) {
        given(variants.findOrderable(any())).willReturn(List.of(found));
        given(variants.decreaseStock(anyLong(), anyInt())).willReturn(1);
    }

    @SuppressWarnings("unchecked")
    private List<OrderItem> savedItems() {
        ArgumentCaptor<List<OrderItem>> captor = ArgumentCaptor.forClass(List.class);
        verify(orderItems).saveAll(captor.capture());
        return captor.getValue();
    }

    @Test
    void orderIsCreatedInProcessingAndStockGoesDown() {
        inStock(TEE_M_BLACK, HOODIE_L_GREY);

        OrderResponse response = service.place(USER, request(
                new OrderLineRequest(TEE, 2), new OrderLineRequest(HOODIE, 1)));

        verify(variants).decreaseStock(TEE, 2);
        verify(variants).decreaseStock(HOODIE, 1);
        assertThat(response.status()).isEqualTo(OrderStatus.PROCESSING);
        // 2 x 79.90 + 1 x 120.00
        assertThat(response.totalAmount()).isEqualByComparingTo("279.80");
    }

    @Test
    void orderBelongsToTheUserAndKeepsOnlyTheLastFourCardDigits() {
        inStock(TEE_M_BLACK);

        service.place(USER, request(new OrderLineRequest(TEE, 1)));

        ArgumentCaptor<Order> saved = ArgumentCaptor.forClass(Order.class);
        verify(orders).save(saved.capture());
        assertThat(saved.getValue().getUserId()).isEqualTo(USER);
        assertThat(saved.getValue().getCardLast4()).isEqualTo("1111");
        assertThat(saved.getValue().getPaymentRef()).isEqualTo("MOCK-1");
    }

    @Test
    void priceCostAndNamesAreCopiedIntoTheOrderLines() {
        inStock(TEE_M_BLACK);

        service.place(USER, request(new OrderLineRequest(TEE, 2)));

        OrderItem item = savedItems().get(0);
        assertThat(item.getUnitPrice()).isEqualByComparingTo("79.90");
        assertThat(item.getUnitCost()).isEqualByComparingTo("40.00");
        assertThat(item.getProductName()).isEqualTo("Basic Tee");
        assertThat(item.getVariantLabel()).isEqualTo("M / Black");
        assertThat(item.getQuantity()).isEqualTo(2);
    }

    @Test
    void stockIsTakenInAscendingVariantOrderAndOnlyThenTheCardIsCharged() {
        inStock(TEE_M_BLACK, HOODIE_L_GREY);

        // The hoodie (11) comes first in the cart, but the tee (10) must be locked first.
        service.place(USER, request(new OrderLineRequest(HOODIE, 1), new OrderLineRequest(TEE, 1)));

        InOrder order = inOrder(variants, paymentGateway);
        order.verify(variants).decreaseStock(TEE, 1);
        order.verify(variants).decreaseStock(HOODIE, 1);
        order.verify(paymentGateway).charge(any(), anyString());
    }

    @Test
    void linesWithTheSameVariantAreMerged() {
        inStock(TEE_M_BLACK);

        service.place(USER, request(new OrderLineRequest(TEE, 1), new OrderLineRequest(TEE, 2)));

        verify(variants).decreaseStock(TEE, 3);
        assertThat(savedItems()).singleElement().extracting(OrderItem::getQuantity).isEqualTo(3);
    }

    @Test
    void notEnoughStockFailsAndNothingIsSavedOrCharged() {
        given(variants.findOrderable(any())).willReturn(List.of(TEE_M_BLACK, HOODIE_L_GREY));
        given(variants.decreaseStock(TEE, 1)).willReturn(1);
        given(variants.decreaseStock(HOODIE, 5)).willReturn(0);

        assertThatThrownBy(() -> service.place(USER, request(
                new OrderLineRequest(TEE, 1), new OrderLineRequest(HOODIE, 5))))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("Hoodie");

        // The tee's stock change is undone by the transaction rollback (@Transactional).
        verify(paymentGateway, never()).charge(any(), anyString());
        verify(orders, never()).save(any());
        verify(orderItems, never()).saveAll(any());
    }

    @Test
    void unknownVariantFailsBeforeAnyStockIsTouched() {
        given(variants.findOrderable(any())).willReturn(List.of(TEE_M_BLACK));

        assertThatThrownBy(() -> service.place(USER, request(
                new OrderLineRequest(TEE, 1), new OrderLineRequest(999L, 1))))
                .isInstanceOf(NotFoundException.class);

        verify(variants, never()).decreaseStock(anyLong(), anyInt());
        verify(orders, never()).save(any());
    }

    @Test
    void deactivatedProductCannotBeOrdered() {
        given(variants.findOrderable(any()))
                .willReturn(List.of(variant(TEE, "Basic Tee", "M", "Black", "79.90", "40.00", false)));

        assertThatThrownBy(() -> service.place(USER, request(new OrderLineRequest(TEE, 1))))
                .isInstanceOf(NotFoundException.class);

        verify(variants, never()).decreaseStock(anyLong(), anyInt());
    }
}
