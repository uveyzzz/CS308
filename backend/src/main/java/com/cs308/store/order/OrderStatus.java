package com.cs308.store.order;

/** Where an order is in delivery (requirement 3). Stored as text in orders.status (V6). */
public enum OrderStatus {
    PROCESSING,
    IN_TRANSIT,
    DELIVERED,
    CANCELLED
}
