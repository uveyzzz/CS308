package com.cs308.store.order;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.cs308.store.catalog.OrderableVariant;
import com.cs308.store.catalog.ProductVariantRepository;
import com.cs308.store.common.ConflictException;
import com.cs308.store.common.NotFoundException;
import com.cs308.store.payment.PaymentGateway;

/** Placing an order (requirements 3, 4, 18; SCRUM-26). */
@Service
public class OrderService {

    private final OrderRepository orders;
    private final OrderItemRepository orderItems;
    private final ProductVariantRepository variants;
    private final PaymentGateway paymentGateway;

    public OrderService(OrderRepository orders, OrderItemRepository orderItems, ProductVariantRepository variants,
            PaymentGateway paymentGateway) {
        this.orders = orders;
        this.orderItems = orderItems;
        this.variants = variants;
        this.paymentGateway = paymentGateway;
    }

    /**
     * Turns the cart into an order, all in one transaction. If any step throws, everything done
     * before it is rolled back: no stock change, no order, no lines.
     *
     * 1. Merge lines with the same variant (the database allows a variant once per order).
     * 2. Load price, cost and names from the database. Unknown or deactivated variants stop here,
     *    before any stock is touched.
     * 3. Take the items out of stock with one atomic UPDATE per variant, in ascending id order.
     *    The fixed order means two orders that share variants lock rows in the same order and
     *    cannot deadlock. 0 rows updated means not enough stock: the whole order fails.
     * 4. Charge the card only after the stock is reserved, so a stock failure never charges.
     * 5. Save the order (PROCESSING) and its lines with the prices copied in.
     */
    @Transactional
    public OrderResponse place(long userId, PlaceOrderRequest request) {
        SortedMap<Long, Integer> quantities = mergeQuantities(request.items());

        Map<Long, OrderableVariant> byId = variants.findOrderable(quantities.keySet()).stream()
                .collect(Collectors.toMap(OrderableVariant::getVariantId, Function.identity()));
        for (Long variantId : quantities.keySet()) {
            OrderableVariant variant = byId.get(variantId);
            if (variant == null || !Boolean.TRUE.equals(variant.getActive())) {
                throw new NotFoundException("Product variant " + variantId + " is not available");
            }
        }

        BigDecimal total = BigDecimal.ZERO;
        for (Map.Entry<Long, Integer> line : quantities.entrySet()) {
            OrderableVariant variant = byId.get(line.getKey());
            if (variants.decreaseStock(line.getKey(), line.getValue()) == 0) {
                throw new ConflictException("Not enough stock for " + variant.getProductName() + " ("
                        + label(variant) + ")");
            }
            total = total.add(variant.getUnitPrice().multiply(BigDecimal.valueOf(line.getValue())));
        }

        String paymentRef = paymentGateway.charge(total, request.cardNumber());

        Order order = orders.save(new Order(userId, total, request.shippingAddress().trim(),
                request.billingAddress().trim(), request.cardLast4(), paymentRef));

        List<OrderItem> items = new ArrayList<>();
        for (Map.Entry<Long, Integer> line : quantities.entrySet()) {
            OrderableVariant variant = byId.get(line.getKey());
            items.add(new OrderItem(order.getId(), line.getKey(), line.getValue(), variant.getUnitPrice(),
                    variant.getDiscountRate(), variant.getUnitCost(), variant.getProductName(), label(variant)));
        }
        orderItems.saveAll(items);

        return OrderResponse.from(order, items);
    }

    /** Adds up lines that name the same variant. A TreeMap keeps the variant ids in ascending order. */
    static SortedMap<Long, Integer> mergeQuantities(List<OrderLineRequest> lines) {
        SortedMap<Long, Integer> quantities = new TreeMap<>();
        for (OrderLineRequest line : lines) {
            quantities.merge(line.variantId(), line.quantity(), Integer::sum);
        }
        return quantities;
    }

    /** For example "M / Black". */
    private static String label(OrderableVariant variant) {
        return variant.getSize() + " / " + variant.getColor();
    }
}
