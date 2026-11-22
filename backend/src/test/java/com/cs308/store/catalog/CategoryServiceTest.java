package com.cs308.store.catalog;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.cs308.store.common.NotFoundException;

/**
 * The repository returns only active categories, so an inactive one is simply missing
 * from the rows given here.
 */
class CategoryServiceTest {

    private static final CategoryRow WOMEN = new CategoryRow(1L, "Women", "women", null, null);
    private static final CategoryRow MEN = new CategoryRow(2L, "Men", "men", null, null);
    private static final CategoryRow WOMEN_TOPS = new CategoryRow(3L, "Tops", "women-tops", null, 1L);
    private static final CategoryRow WOMEN_SHIRTS = new CategoryRow(4L, "Shirts", "women-shirts", null, 3L);
    private static final CategoryRow MEN_TOPS = new CategoryRow(5L, "Tops", "men-tops", null, 2L);

    private CategoryRepository repository;
    private CategoryService service;

    @BeforeEach
    void setUp() {
        repository = mock(CategoryRepository.class);
        service = new CategoryService(repository);
    }

    private void activeRows(CategoryRow... rows) {
        given(repository.findActiveRows()).willReturn(List.of(rows));
    }

    @Test
    void treeNestsSubcategoriesUnderTheirParent() {
        activeRows(MEN, MEN_TOPS, WOMEN, WOMEN_TOPS);

        List<CategoryNode> tree = service.tree();

        assertThat(tree).extracting(CategoryNode::slug).containsExactly("men", "women");
        assertThat(tree.get(1).children()).extracting(CategoryNode::slug).containsExactly("women-tops");
        assertThat(tree.get(1).children().get(0).children()).isEmpty();
    }

    @Test
    void treeHidesTheChildrenOfAnInactiveCategory() {
        // Women is inactive, so it is not among the rows, but its subcategory still is.
        activeRows(MEN, WOMEN_TOPS);

        List<CategoryNode> tree = service.tree();

        assertThat(tree).extracting(CategoryNode::slug).containsExactly("men");
    }

    @Test
    void categoryPageHasABreadcrumbFromTheTopDown() {
        activeRows(WOMEN, WOMEN_TOPS, WOMEN_SHIRTS);

        CategoryPageResponse page = service.getBySlug("women-shirts");

        assertThat(page.path()).extracting(CategoryPageResponse.Crumb::slug)
                .containsExactly("women", "women-tops", "women-shirts");
    }

    @Test
    void categoryPageListsTheDirectSubcategories() {
        activeRows(WOMEN, WOMEN_TOPS, WOMEN_SHIRTS);

        CategoryPageResponse page = service.getBySlug("women");

        assertThat(page.children()).extracting(CategoryNode::slug).containsExactly("women-tops");
    }

    @Test
    void unknownSlugIsNotFound() {
        activeRows(WOMEN);

        assertThatThrownBy(() -> service.getBySlug("kids"))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("Category not found");
    }

    @Test
    void categoryUnderAnInactiveParentIsNotFound() {
        activeRows(WOMEN_TOPS);

        assertThatThrownBy(() -> service.getBySlug("women-tops")).isInstanceOf(NotFoundException.class);
    }

    @Test
    void subtreeContainsTheCategoryAndEveryLevelBelowIt() {
        activeRows(WOMEN, WOMEN_TOPS, WOMEN_SHIRTS, MEN, MEN_TOPS);

        assertThat(service.subtreeIds("women")).containsExactlyInAnyOrder(1L, 3L, 4L);
        assertThat(service.subtreeIds("women-shirts")).containsExactly(4L);
    }

    @Test
    void loopInTheDataDoesNotHangTheServer() {
        CategoryRow a = new CategoryRow(10L, "A", "a", null, 11L);
        CategoryRow b = new CategoryRow(11L, "B", "b", null, 10L);
        activeRows(a, b);

        assertThat(service.tree()).isEmpty();
        assertThatThrownBy(() -> service.getBySlug("a")).isInstanceOf(NotFoundException.class);
    }
}
