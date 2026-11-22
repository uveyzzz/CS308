package com.cs308.store.catalog;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class SearchTextTest {

    @Test
    void blankInputMeansNoSearch() {
        assertThat(SearchText.toPattern(null)).isNull();
        assertThat(SearchText.toPattern("")).isNull();
        assertThat(SearchText.toPattern("   ")).isNull();
    }

    @Test
    void patternMatchesTheWordAnywhereIgnoringCase() {
        assertThat(SearchText.toPattern("  Gömlek ")).isEqualTo("%gömlek%");
        assertThat(SearchText.toPattern("GÖMLEK")).isEqualTo("%gömlek%");
    }

    @Test
    void turkishCapitalsBecomeTheirSmallLetters() {
        assertThat(SearchText.fold("ÇĞÖŞÜ")).isEqualTo("çğöşü");
    }

    @Test
    void allFormsOfIAreTreatedAlike() {
        assertThat(SearchText.fold("IŞIK")).isEqualTo(SearchText.fold("ışık"));
        assertThat(SearchText.fold("İPEK")).isEqualTo("ipek");
    }

    @Test
    void likeWildcardsTypedByTheUserAreEscaped() {
        assertThat(SearchText.toPattern("50%_off!")).isEqualTo("%50!%!_off!!%");
    }

    @Test
    void veryLongInputIsCut() {
        String pattern = SearchText.toPattern("a".repeat(500));
        assertThat(pattern).hasSize(SearchText.MAX_LENGTH + 2);
    }
}
