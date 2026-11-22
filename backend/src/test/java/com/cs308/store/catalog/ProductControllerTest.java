package com.cs308.store.catalog;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.cs308.store.auth.JwtService;
import com.cs308.store.common.NotFoundException;
import com.cs308.store.common.PageResponse;
import com.cs308.store.config.SecurityConfig;

/** Checks the HTTP side: who may call the endpoints and the JSON the frontend reads. */
@WebMvcTest(ProductController.class)
@Import(SecurityConfig.class)
@MockitoBean(types = JwtService.class)
class ProductControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ProductService productService;

    @Test
    void guestCanListProductsInTheShapeTheFrontendReads() throws Exception {
        ProductSummary card = new ProductSummary(1L, "basic-tee", "Basic Tee", new BigDecimal("100.00"),
                new BigDecimal("80.00"), new BigDecimal("4.50"), 2, 7, "/img/tee.jpg");
        given(productService.list(0, 12)).willReturn(new PageResponse<>(List.of(card), 0, 12, 1, 1));

        mockMvc.perform(get("/api/products"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].slug").value("basic-tee"))
                .andExpect(jsonPath("$.content[0].discountedPrice").value(80.00))
                .andExpect(jsonPath("$.content[0].stock").value(7))
                .andExpect(jsonPath("$.totalPages").value(1));
    }

    @Test
    void passesPageAndSizeFromTheUrl() throws Exception {
        given(productService.list(2, 24)).willReturn(new PageResponse<>(List.of(), 2, 24, 0, 0));

        mockMvc.perform(get("/api/products").param("page", "2").param("size", "24"))
                .andExpect(status().isOk());

        verify(productService).list(2, 24);
    }

    @Test
    void unknownSlugReturns404InTheCommonErrorShape() throws Exception {
        given(productService.getBySlug("nope")).willThrow(new NotFoundException("Product not found"));

        mockMvc.perform(get("/api/products/nope"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("Product not found"));
    }
}
