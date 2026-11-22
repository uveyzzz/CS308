package com.cs308.store.catalog;

import org.springframework.data.domain.Sort;

/** The sort options a customer can choose on the listing, category and search pages (SCRUM-20). */
public enum ProductSort {

    NEWEST(Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id"))),
    PRICE_ASC(Sort.by(Sort.Order.asc("finalPrice"), Sort.Order.desc("id"))),
    PRICE_DESC(Sort.by(Sort.Order.desc("finalPrice"), Sort.Order.desc("id"))),
    POPULAR(Sort.by(Sort.Order.desc("salesCount"), Sort.Order.desc("avgRating"), Sort.Order.desc("id")));

    private final Sort sort;

    ProductSort(Sort sort) {
        this.sort = sort;
    }

    public Sort toSort() {
        return sort;
    }

    /**
     * Reads the ?sort= value from the URL, e.g. "price_asc". A missing or unknown value falls back
     * to NEWEST instead of an error, so a hand-typed URL still works.
     */
    public static ProductSort fromParam(String value) {
        if (value == null || value.isBlank()) {
            return NEWEST;
        }
        for (ProductSort option : values()) {
            if (option.name().equalsIgnoreCase(value.trim())) {
                return option;
            }
        }
        return NEWEST;
    }
}