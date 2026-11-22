package com.cs308.store.catalog;

import static com.cs308.store.catalog.CatalogTestData.product;
import static com.cs308.store.catalog.CatalogTestData.variant;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;

import com.cs308.store.common.NotFoundException;
import com.cs308.store.common.PageResponse;

class ProductServiceTest {

    private ProductRepository products;
    private ProductDetailsRepository details;
    private ProductVariantRepository variants;
    private ProductService service;

    @BeforeEach
    void setUp() {
        products = mock(ProductRepository.class);
        details = mock(ProductDetailsRepository.class);
        variants = mock(ProductVariantRepository.class);
        service = new ProductService(products, details, variants);
    }

    @Test
    void listTurnsProductsIntoCards() {
        Product discounted = product(1, "basic-tee", "100.00", "80.00", 7);
        given(products.findByActiveTrue(any(Pageable.class)))
                .willReturn(new PageImpl<>(List.of(discounted), PageRequest.of(0, 12), 1));

        PageResponse<ProductSummary> page = service.list(0, 12);

        assertThat(page.totalPages()).isEqualTo(1);
        ProductSummary card = page.content().get(0);
        assertThat(card.slug()).isEqualTo("basic-tee");
        assertThat(card.price()).isEqualByComparingTo("100.00");
        assertThat(card.discountedPrice()).isEqualByComparingTo("80.00");
        assertThat(card.stock()).isEqualTo(7);
        assertThat(card.imageUrl()).isEqualTo("/img/basic-tee.jpg");
    }

    @Test
    void pageAndSizeOutsideTheLimitsAreClamped() {
        Pageable tooBig = ProductService.pageRequest(-3, 500);
        assertThat(tooBig.getPageNumber()).isZero();
        assertThat(tooBig.getPageSize()).isEqualTo(ProductService.MAX_PAGE_SIZE);

        assertThat(ProductService.pageRequest(0, 0).getPageSize()).isEqualTo(1);
    }

    @Test
    void listShowsNewestProductsFirst() {
        assertThat(ProductService.pageRequest(0, 12).getSort()).isEqualTo(ProductService.DEFAULT_SORT);
    }

    @Test
    void detailIncludesDetailsAndEveryVariantWithItsStock() {
        Product tee = product(1, "basic-tee", "100.00", "100.00", 3);
        given(products.findBySlugAndActiveTrue("basic-tee")).willReturn(Optional.of(tee));
        given(details.findById(1L)).willReturn(Optional.of(
                new ProductDetails(1L, "Soft cotton tee", "BT-01", "SN-001", "2 years", "Acme Textiles")));
        given(variants.findByProductIdOrderByIdAsc(1L)).willReturn(List.of(
                variant(10, 1, "M", "Black", 3), variant(11, 1, "L", "Black", 0)));

        ProductDetailResponse response = service.getBySlug("basic-tee");

        assertThat(response.description()).isEqualTo("Soft cotton tee");
        assertThat(response.serialNumber()).isEqualTo("SN-001");
        assertThat(response.warrantyStatus()).isEqualTo("2 years");
        assertThat(response.distributor()).isEqualTo("Acme Textiles");
        assertThat(response.variants()).extracting(ProductDetailResponse.Variant::stock).containsExactly(3, 0);
    }

    @Test
    void unknownOrInactiveSlugIsNotFound() {
        given(products.findBySlugAndActiveTrue("nope")).willReturn(Optional.empty());

        assertThatThrownBy(() -> service.getBySlug("nope"))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("Product not found");
    }

    @Test
    void productWithoutDetailsRowStillHasADetailPage() {
        Product tee = product(1, "basic-tee", "100.00", "100.00", 0);
        given(products.findBySlugAndActiveTrue("basic-tee")).willReturn(Optional.of(tee));
        given(details.findById(1L)).willReturn(Optional.empty());
        given(variants.findByProductIdOrderByIdAsc(1L)).willReturn(List.of());

        ProductDetailResponse response = service.getBySlug("basic-tee");

        assertThat(response.name()).isEqualTo("Product 1");
        assertThat(response.description()).isNull();
        assertThat(response.variants()).isEmpty();
    }

    @Test
    void productWithoutImageHasAnEmptyImageList() {
        Product tee = product(1, "basic-tee", "100.00", "100.00", 0);
        ReflectionTestUtils.setField(tee, "mainImageUrl", null);
        given(products.findBySlugAndActiveTrue("basic-tee")).willReturn(Optional.of(tee));
        given(details.findById(1L)).willReturn(Optional.empty());
        given(variants.findByProductIdOrderByIdAsc(1L)).willReturn(List.of());

        assertThat(service.getBySlug("basic-tee").images()).isEmpty();
    }
}
