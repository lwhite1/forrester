package systems.courant.sd.tools.importer;

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
     * Formats a double value for use in Java source code.
     * Handles NaN and Infinity which cannot be written as bare numeric literals.
     */
    public static String formatDoubleForSource(double value) {
        if (Double.isNaN(value)) {
            return "Double.NaN";
        }
        if (Double.isInfinite(value)) {
            return value > 0 ? "Double.POSITIVE_INFINITY" : "Double.NEGATIVE_INFINITY";
        }
        return String.valueOf(value);
    }

    /**
     * Formats a double array as a Java source literal, e.g. {@code new double[]{1.0, 2.5, 3.7}}.
     * Handles special values (NaN, Infinity) correctly.
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
            sb.append(formatDoubleForSource(values[i]));
        }
        sb.append('}');
        return sb.toString();
    }

    /**
     * Converts a string to PascalCase. Splits on non-alphanumeric characters
     * (spaces, hyphens, underscores, etc.). All-uppercase tokens (acronyms like
     * SIR, HIV, GDP) are preserved as-is.
     *
     * <p>Examples: "my model name" → "MyModelName", "SIR-model" → "SIRModel"
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
            if (token.length() > 1 && token.equals(token.toUpperCase())) {
                // Preserve all-uppercase tokens (acronyms)
                sb.append(token);
            } else {
                sb.append(Character.toUpperCase(token.charAt(0)));
                if (token.length() > 1) {
                    sb.append(token.substring(1).toLowerCase());
                }
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
     * Converts a name to a valid Java identifier by lowercasing, stripping
     * non-alphanumeric characters, and prefixing with an underscore if it
     * starts with a digit.
     *
     * <p>Examples: "3rd Stage" → "_3rdstage", "my module" → "mymodule"
     */
    public static String toValidIdentifier(String input) {
        String base = toPackageSegment(input);
        if (!base.isEmpty() && Character.isDigit(base.charAt(0))) {
            return "_" + base;
        }
        return base;
    }

}
