package systems.courant.sd.io;

/**
 * Shared formatting and parsing utilities for model importers and exporters.
 */
public final class FormatUtils {

    private FormatUtils() {
    }

    /**
     * Formats a double value as a compact string: integers are rendered without
     * a decimal point (e.g. {@code 42} instead of {@code 42.0}), while fractional
     * values use standard {@link Double#toString(double)} formatting.
     *
     * @param value the value to format
     * @return the formatted string
     * @throws IllegalArgumentException if value is NaN or infinite
     */
    public static String formatDouble(double value) {
        if (Double.isNaN(value)) {
            throw new IllegalArgumentException("Cannot format NaN as a numeric string");
        }
        if (Double.isInfinite(value)) {
            throw new IllegalArgumentException("Cannot format Infinity as a numeric string");
        }
        if (value == Math.floor(value) && Math.abs(value) < 1e15) {
            return String.valueOf((long) value);
        }
        return String.valueOf(value);
    }

    /**
     * Finds the position of the first comma at parenthesis depth zero,
     * searching from the beginning of the string.
     *
     * @param content the string to search
     * @return the index of the first top-level comma, or {@code -1} if none found
     */
    public static int findTopLevelComma(String content) {
        return findTopLevelComma(content, 0);
    }

    /**
     * Finds the position of the first comma at parenthesis depth zero,
     * searching from {@code startPos}.
     *
     * @param content  the string to search
     * @param startPos the index to start searching from
     * @return the index of the first top-level comma, or {@code -1} if none found
     */
    public static int findTopLevelComma(String content, int startPos) {
        int depth = 0;
        for (int i = startPos; i < content.length(); i++) {
            char c = content.charAt(i);
            if (c == '(') {
                depth++;
            } else if (c == ')') {
                if (depth == 0) {
                    return -1;
                }
                depth--;
            } else if (c == ',' && depth == 0) {
                return i;
            }
        }
        return -1;
    }

    /**
     * Finds the index of the closing parenthesis that matches the opening
     * parenthesis at {@code openParenPos}, respecting nested parentheses.
     *
     * @param content      the string to search
     * @param openParenPos the index of the opening parenthesis
     * @return the index of the matching closing parenthesis, or {@code -1} if not found
     */
    public static int findMatchingCloseParen(String content, int openParenPos) {
        int depth = 1;
        for (int i = openParenPos + 1; i < content.length(); i++) {
            char c = content.charAt(i);
            if (c == '(') {
                depth++;
            } else if (c == ')') {
                depth--;
                if (depth == 0) {
                    return i;
                }
            }
        }
        return -1;
    }
}
