package systems.courant.forrester.tools.importer;

/**
 * Utility methods for generating syntactically correct Java source code.
 */
public final class JavaSourceEscaper {

    private JavaSourceEscaper() {
    }

    /**
     * Escapes a string for use inside a Java string literal.
     * Returns the literal {@code "null"} token (unquoted) for null input.
     */
    public static String escapeString(String value) {
        if (value == null) {
            return "null";
        }
        StringBuilder sb = new StringBuilder(value.length() + 8);
        sb.append('"');
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            switch (c) {
                case '"' -> sb.append("\\\"");
                case '\\' -> sb.append("\\\\");
                case '\n' -> sb.append("\\n");
                case '\r' -> sb.append("\\r");
                case '\t' -> sb.append("\\t");
                default -> sb.append(c);
            }
        }
        sb.append('"');
        return sb.toString();
    }

    /**
     * Formats a double array as a Java source literal, e.g. {@code new double[]{1.0, 2.5, 3.7}}.
     */
    public static String doubleArrayLiteral(double[] values) {
        if (values == null) {
            return "null";
        }
        StringBuilder sb = new StringBuilder();
        sb.append("new double[]{");
        for (int i = 0; i < values.length; i++) {
            if (i > 0) {
                sb.append(", ");
            }
            sb.append(values[i]);
        }
        sb.append('}');
        return sb.toString();
    }

    /**
     * Converts a string to PascalCase. Splits on spaces, hyphens, underscores,
     * and camelCase boundaries.
     *
     * <p>Examples: "my model name" → "MyModelName", "SIR-model" → "SirModel"
     */
    public static String toPascalCase(String input) {
        if (input == null || input.isBlank()) {
            return "";
        }
        // Split on non-alphanumeric characters
        String[] tokens = input.split("[^a-zA-Z0-9]+");
        StringBuilder sb = new StringBuilder();
        for (String token : tokens) {
            if (token.isEmpty()) {
                continue;
            }
            sb.append(Character.toUpperCase(token.charAt(0)));
            if (token.length() > 1) {
                sb.append(token.substring(1).toLowerCase());
            }
        }
        return sb.toString();
    }

    /**
     * Converts a category name to a valid Java package segment.
     * Lowercases and strips non-alphanumeric characters.
     *
     * <p>Examples: "Epidemiology" → "epidemiology", "Supply Chain" → "supplychain"
     */
    public static String toPackageSegment(String input) {
        if (input == null || input.isBlank()) {
            return "";
        }
        return input.replaceAll("[^a-zA-Z0-9]", "").toLowerCase();
    }

    /**
     * Returns "null" if the value is null, otherwise the escaped string literal.
     * Convenience for generating nullable constructor arguments.
     */
    public static String nullableString(String value) {
        return escapeString(value);
    }
}
