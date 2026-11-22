package com.cs308.store.catalog;

import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.cs308.store.auth.JwtService;
import com.cs308.store.common.NotFoundException;
import com.cs308.store.config.SecurityConfig;

@WebMvcTest(CategoryController.class)
@Import(SecurityConfig.class)
@MockitoBean(types = JwtService.class)
class CategoryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CategoryService categoryService;

    @Test
    void guestCanLoadTheMenuTree() throws Exception {
        given(categoryService.tree()).willReturn(List.of(new CategoryNode(1L, "Women", "women",
                List.of(new CategoryNode(3L, "Tops", "women-tops", List.of())))));

        mockMvc.perform(get("/api/categories"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].slug").value("women"))
                .andExpect(jsonPath("$[0].children[0].slug").value("women-tops"));
    }

    @Test
    void categoryPageReturnsTheBreadcrumb() throws Exception {
        given(categoryService.getBySlug("women-tops")).willReturn(new CategoryPageResponse(3L, "Tops",
                "women-tops", null,
                List.of(new CategoryPageResponse.Crumb("Women", "women"),
                        new CategoryPageResponse.Crumb("Tops", "women-tops")),
                List.of()));

        mockMvc.perform(get("/api/categories/women-tops"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.path[0].name").value("Women"))
                .andExpect(jsonPath("$.path[1].name").value("Tops"));
    }

    @Test
    void unknownCategoryReturns404() throws Exception {
        given(categoryService.getBySlug("kids")).willThrow(new NotFoundException("Category not found"));

        mockMvc.perform(get("/api/categories/kids"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Category not found"));
    }
}
