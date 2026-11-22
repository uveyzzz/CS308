package com.cs308.store.cart;

import java.util.List;
import java.util.Optional;
import java.util.SortedMap;
import java.util.TreeMap;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.cs308.store.catalog.ProductVariant;
import com.cs308.store.catalog.ProductVariantRepository;
import com.cs308.store.common.NotFoundException;

/**
 * The logged-in customer's cart (SCRUM-24, requirement 4). A quantity never goes above the
 * variant's current stock. The stock is only checked here, not reserved: placing the order
 * takes it out of stock (SCRUM-26).
 */
@Service
public class CartService {

    private final CartItemRepository cartItems;
    private final ProductVariantRepository variants;

    public CartService(CartItemRepository cartItems, ProductVariantRepository variants) {
        this.cartItems = cartItems;
        this.variants = variants;
    }

    @Transactional(readOnly = true)
    public List<CartLine> get(long userId) {
        return cartItems.findLines(userId).stream().map(CartLineView::toLine).toList();
    }

    /** Adds one variant, or raises the quantity of the line that already has it. Unknown variant: 404. */
    @Transactional
    public List<CartLine> add(long userId, CartLineRequest line) {
        ProductVariant variant = variants.findById(line.variantId())
                .orElseThrow(() -> new NotFoundException("Product variant " + line.variantId() + " does not exist"));
        addCapped(userId, variant, line.quantity());
        return get(userId);
    }

    /**
     * Merges the guest cart into the saved cart at login. Quantities of the same variant are
     * added up and capped at the stock. A variant that no longer exists or is out of stock is
     * skipped, so one stale line cannot block the rest of the cart.
     */
    @Transactional
    public List<CartLine> merge(long userId, List<CartLineRequest> guestLines) {
        for (var line : sumByVariant(guestLines).entrySet()) {
            variants.findById(line.getKey()).ifPresent(variant -> addCapped(userId, variant, line.getValue()));
        }
        return get(userId);
    }

    /** Sets a line's quantity, capped at the stock. With no stock left, the line is removed. */
    @Transactional
    public List<CartLine> setQuantity(long userId, long variantId, int quantity) {
        ProductVariant variant = variants.findById(variantId)
                .orElseThrow(() -> new NotFoundException("Product variant " + variantId + " does not exist"));
        CartItemId id = new CartItemId(userId, variantId);
        int capped = Math.min(quantity, variant.getStockQuantity());
        if (capped <= 0) {
            cartItems.deleteById(id);
        } else {
            CartItem item = cartItems.findById(id).orElseGet(() -> new CartItem(userId, variantId, capped));
            item.setQuantity(capped);
            cartItems.save(item);
        }
        return get(userId);
    }

    @Transactional
    public List<CartLine> remove(long userId, long variantId) {
        cartItems.deleteById(new CartItemId(userId, variantId));
        return get(userId);
    }

    @Transactional
    public void clear(long userId) {
        cartItems.deleteAllForUser(userId);
    }

    private void addCapped(long userId, ProductVariant variant, int quantity) {
        int stock = variant.getStockQuantity();
        if (stock <= 0) {
            return;
        }
        CartItemId id = new CartItemId(userId, variant.getId());
        Optional<CartItem> existing = cartItems.findById(id);
        int current = existing.map(CartItem::getQuantity).orElse(0);
        int capped = Math.min(current + quantity, stock);
        CartItem item = existing.orElseGet(() -> new CartItem(userId, variant.getId(), capped));
        item.setQuantity(capped);
        cartItems.save(item);
    }

    /** The browser could send the same variant twice; add those lines up first. */
    static SortedMap<Long, Integer> sumByVariant(List<CartLineRequest> lines) {
        SortedMap<Long, Integer> totals = new TreeMap<>();
        for (CartLineRequest line : lines) {
            totals.merge(line.variantId(), line.quantity(), Integer::sum);
        }
        return totals;
    }
}
