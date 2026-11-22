package com.cs308.store.catalog;

import org.springframework.data.jpa.repository.JpaRepository;

/** The key is the product id, so findById(productId) returns a product's details. */
public interface ProductDetailsRepository extends JpaRepository<ProductDetails, Long> {
}
