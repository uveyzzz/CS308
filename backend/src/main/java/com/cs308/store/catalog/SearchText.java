package com.cs308.store.catalog;

/**
 * Turns what the user typed in the search bar into a LIKE pattern (SCRUM-19).
 * The database query in ProductRepository.SEARCH_MATCH folds the product text the same way,
 * so "GÖMLEK", "gömlek" and "Gömlek" all match, whatever the database's language settings are.
 */
final class SearchText {

    /** Longer input is cut, so a pasted essay does not become a huge query. */
    static final int MAX_LENGTH = 100;

    /** Marks a %, _ or ! that the user typed as a plain character in LIKE. */
    static final char ESCAPE = '!';

    private SearchText() {
    }

    /** @return "%folded text%", or null when there is nothing to search for. */
    static String toPattern(String query) {
        if (query == null || query.isBlank()) {
            return null;
        }
        String trimmed = query.trim();
        if (trimmed.length() > MAX_LENGTH) {
            trimmed = trimmed.substring(0, MAX_LENGTH);
        }
        return "%" + escapeLike(fold(trimmed)) + "%";
    }

    /**
     * Small letters, with the Turkish capitals mapped by hand and I, İ and ı all treated as i.
     * String.toLowerCase is not used: its result for I and İ depends on the computer's language.
     */
    static String fold(String text) {
        StringBuilder folded = new StringBuilder(text.length());
        for (char c : text.toCharArray()) {
            folded.append(switch (c) {
                case 'Ç' -> 'ç';
                case 'Ğ' -> 'ğ';
                case 'İ', 'I', 'ı' -> 'i';
                case 'Ö' -> 'ö';
                case 'Ş' -> 'ş';
                case 'Ü' -> 'ü';
                default -> Character.toLowerCase(c);
            });
        }
        return folded.toString();
    }

    /** In LIKE, % and _ are wildcards. A user searching for "50%" means the characters 5, 0 and %. */
    static String escapeLike(String text) {
        return text.replace("!", "!!").replace("%", "!%").replace("_", "!_");
    }
}
