package com.cs308.store.order;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Body of POST /api/orders. The cart lives in the browser (SCRUM-23), so its lines come in the
 * request. The customer comes from the token, and prices come from the database.
 */
public record PlaceOrderRequest(
        @NotEmpty(message = "The cart is empty")
        @Size(max = 50, message = "An order can have at most 50 lines")
        List<@Valid @NotNull OrderLineRequest> items,

        @NotBlank(message = "Shipping address is required")
        @Size(max = 500, message = "Shipping address must be at most 500 characters")
        String shippingAddress,

        @NotBlank(message = "Billing address is required")
        @Size(max = 500, message = "Billing address must be at most 500 characters")
        String billingAddress,

        @NotBlank(message = "Card number is required")
        @Pattern(regexp = "[0-9]{13,19}", message = "Card number must be 13 to 19 digits")
        String cardNumber) {

    /** Only the last 4 digits are ever stored (requirements 15, 17). */
    public String cardLast4() {
        return cardNumber.substring(cardNumber.length() - 4);
    }

    /**
     * A record's default toString prints every field. This one hides the card number, so it can
     * never end up in a log by accident (requirement 17).
     */
    @Override
    public String toString() {
        return "PlaceOrderRequest[items=" + items + ", shippingAddress=" + shippingAddress
                + ", billingAddress=" + billingAddress + ", cardNumber=****]";
    }
}
