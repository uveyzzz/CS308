package com.cs308.store.catalog;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

/** SCRUM-19: the search text reaches the right query, together with the category and the sort. */
class ProductServiceSearchTest {

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
    void searchUsesTheSearchQueryWithTheFoldedPattern() {
        given(products.search(any(), any(Pageable.class))).willReturn(new PageImpl<>(List.of()));

        service.list(0, 12, null, ProductSort.NEWEST, "Gömlek");

        verify(products).search(eq("%gömlek%"), any(Pageable.class));
        verify(products, never()).findByActiveTrue(any(Pageable.class));
    }

    @Test
    void searchInsideACategoryLooksInItsSubcategoriesToo() {
        given(categoryService.subtreeIds("women")).willReturn(Set.of(1L, 2L));
        given(products.searchInCategories(any(), any(), any(Pageable.class))).willReturn(new PageImpl<>(List.of()));

        service.list(0, 12, "women", ProductSort.NEWEST, "gömlek");

        verify(products).searchInCategories(eq("%gömlek%"), eq(Set.of(1L, 2L)), any(Pageable.class));
    }

    @Test
    void blankSearchIsTheNormalList() {
        given(products.findByActiveTrue(any(Pageable.class))).willReturn(new PageImpl<>(List.of()));

        service.list(0, 12, null, ProductSort.NEWEST, "   ");

        verify(products).findByActiveTrue(any(Pageable.class));
        verify(products, never()).search(any(), any(Pageable.class));
    }

    @Test
    void searchResultsKeepTheChosenSort() {
        given(products.search(any(), any(Pageable.class))).willReturn(new PageImpl<>(List.of()));

        service.list(0, 12, null, ProductSort.PRICE_ASC, "gömlek");

        ArgumentCaptor<Pageable> pageable = ArgumentCaptor.forClass(Pageable.class);
        verify(products).search(eq("%gömlek%"), pageable.capture());
        assertThat(pageable.getValue().getSort()).isEqualTo(ProductSort.PRICE_ASC.toSort());
    }
}
