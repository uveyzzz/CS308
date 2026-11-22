package com.cs308.store.order;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/** What POST /api/orders returns: the new order and its lines. Never the card number or the cost. */
public record OrderResponse(Long id, OrderStatus status, BigDecimal totalAmount, String cardLast4,
        Instant createdAt, List<Line> items) {

    public record Line(Long variantId, String productName, String variantLabel, int quantity,
            BigDecimal unitPrice, BigDecimal lineTotal) {
    }

    public static OrderResponse from(Order order, List<OrderItem> items) {
        List<Line> lines = items.stream()
                .map(item -> new Line(item.getVariantId(), item.getProductName(), item.getVariantLabel(),
                        item.getQuantity(), item.getUnitPrice(), item.lineTotal()))
                .toList();
        return new OrderResponse(order.getId(), order.getStatus(), order.getTotalAmount(), order.getCardLast4(),
                order.getCreatedAt(), lines);
    }
}
