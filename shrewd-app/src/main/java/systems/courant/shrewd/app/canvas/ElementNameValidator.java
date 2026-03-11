package systems.courant.shrewd.app.canvas;

import java.util.Locale;
import java.util.Set;

/**
 * Static utility methods for validating and generating element names within a model.
 * Extracted from {@link ModelEditor} to keep naming logic in a single, testable class.
 */
final class ElementNameValidator {

    static final int MAX_NAME_LENGTH = 128;

    private static final Set<String> RESERVED_NAMES = Set.of(
            "TIME", "DT", "Pi", "PI", "E",
            "AND", "OR", "NOT",
            "IF", "THEN", "ELSE");

    private ElementNameValidator() {
    }

    /**
     * Returns true if the given name is valid for an element identifier.
     * A valid name is non-blank, at most {@value MAX_NAME_LENGTH} characters,
     * contains only letters, digits, spaces, and underscores, and is not a
     * reserved word.
     */
    static boolean isValidName(String name) {
        if (name == null || name.isBlank()) {
            return false;
        }
        if (name.length() > MAX_NAME_LENGTH) {
            return false;
        }
        if (RESERVED_NAMES.contains(name.toUpperCase(Locale.ROOT))) {
            return false;
        }
        for (int i = 0; i < name.length(); i++) {
            char c = name.charAt(i);
            if (!Character.isLetterOrDigit(c) && c != ' ' && c != '_') {
                return false;
            }
        }
        return true;
    }

    /**
     * Returns the original name if it is not already taken; otherwise falls back
     * to auto-generated names using the given prefix and starting id.
     */
    static String resolveUniqueName(String originalName, String prefix, int startId,
                                     Set<String> existingNames) {
        if (originalName != null && !originalName.isBlank() && !existingNames.contains(originalName)) {
            return originalName;
        }
        int id = startId;
        String candidate = prefix + id;
        while (existingNames.contains(candidate)) {
            id++;
            candidate = prefix + id;
        }
        return candidate;
    }

    /**
     * Parses the numeric suffix from an auto-generated name (e.g. "Stock 3" → 3).
     * Returns 0 if the suffix is not a valid integer.
     */
    static int parseIdSuffix(String name, String prefix) {
        try {
            return Integer.parseInt(name.substring(prefix.length()));
        } catch (NumberFormatException | IndexOutOfBoundsException e) {
            return 0;
        }
    }
}
