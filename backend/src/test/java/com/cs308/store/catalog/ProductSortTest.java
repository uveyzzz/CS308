package com.cs308.store.catalog;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Locale;

import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Sort;

class ProductSortTest {

    @Test
    void readsEveryOptionFromTheUrlIgnoringCase() {
        assertThat(ProductSort.fromParam("newest")).isEqualTo(ProductSort.NEWEST);
        assertThat(ProductSort.fromParam("price_asc")).isEqualTo(ProductSort.PRICE_ASC);
        assertThat(ProductSort.fromParam("PRICE_DESC")).isEqualTo(ProductSort.PRICE_DESC);
        assertThat(ProductSort.fromParam(" popular ")).isEqualTo(ProductSort.POPULAR);
    }

    @Test
    void missingOrUnknownValueFallsBackToNewest() {
        assertThat(ProductSort.fromParam(null)).isEqualTo(ProductSort.NEWEST);
        assertThat(ProductSort.fromParam("")).isEqualTo(ProductSort.NEWEST);
        assertThat(ProductSort.fromParam("cheapest")).isEqualTo(ProductSort.NEWEST);
    }

    @Test
    void parsingDoesNotDependOnTheTurkishLocale() {
        Locale original = Locale.getDefault();
        try {
            Locale.setDefault(Locale.forLanguageTag("tr-TR"));
            assertThat(ProductSort.fromParam("price_asc")).isEqualTo(ProductSort.PRICE_ASC);
        } finally {
            Locale.setDefault(original);
        }
    }

    @Test
    void priceSortsUseTheDiscountedPrice() {
        assertThat(ProductSort.PRICE_ASC.toSort().getOrderFor("finalPrice").getDirection())
                .isEqualTo(Sort.Direction.ASC);
        assertThat(ProductSort.PRICE_DESC.toSort().getOrderFor("finalPrice").getDirection())
                .isEqualTo(Sort.Direction.DESC);
    }

    @Test
    void popularMeansMostSoldFirstThenBestRated() {
        assertThat(ProductSort.POPULAR.toSort()).containsExactly(
                Sort.Order.desc("salesCount"), Sort.Order.desc("avgRating"), Sort.Order.desc("id"));
    }
}
