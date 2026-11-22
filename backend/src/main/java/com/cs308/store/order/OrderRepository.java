package com.cs308.store.order;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderRepository extends JpaRepository<Order, Long> {

    /** A customer's order history, newest first (requirement 3). */
    List<Order> findByUserIdOrderByCreatedAtDesc(Long userId);
}
