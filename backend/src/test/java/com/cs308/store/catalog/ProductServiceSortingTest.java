package com.cs308.store.catalog;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

/** SCRUM-20: the sort the user chooses reaches the database query. */
class ProductServiceSortingTest {

    private ProductRepository products;
    private CategoryService categoryService;
    private ProductService service;

    @BeforeEach
    void setUp() {
        products = mock(ProductRepository.class);
        categoryService = mock(CategoryService.class);
        service = new ProductService(products, mock(ProductDetailsRepository.class),
                mock(ProductVariantRepository.class), categoryService);
    }

    @Test
    void chosenSortReachesTheProductQuery() {
        given(products.findByActiveTrue(any(Pageable.class))).willReturn(new PageImpl<>(List.of()));

        service.list(0, 12, null, ProductSort.PRICE_ASC);

        ArgumentCaptor<Pageable> pageable = ArgumentCaptor.forClass(Pageable.class);
        verify(products).findByActiveTrue(pageable.capture());
        assertThat(pageable.getValue().getSort()).isEqualTo(ProductSort.PRICE_ASC.toSort());
    }

    @Test
    void sortAlsoWorksOnCategoryPages() {
        given(categoryService.subtreeIds("women")).willReturn(Set.of(1L));
        given(products.findByActiveTrueAndCategoryIdIn(eq(Set.of(1L)), any(Pageable.class)))
                .willReturn(new PageImpl<>(List.of()));

        service.list(0, 12, "women", ProductSort.POPULAR);

        ArgumentCaptor<Pageable> pageable = ArgumentCaptor.forClass(Pageable.class);
        verify(products).findByActiveTrueAndCategoryIdIn(eq(Set.of(1L)), pageable.capture());
        assertThat(pageable.getValue().getSort()).isEqualTo(ProductSort.POPULAR.toSort());
    }

    @Test
    void noSortMeansNewestFirst() {
        assertThat(ProductService.pageRequest(0, 12, null).getSort()).isEqualTo(ProductService.DEFAULT_SORT);
    }
}
