package systems.courant.forrester.model.def;

/**
 * A causal link between two variables in a causal loop diagram.
 * The link indicates that the source variable causally influences the target variable,
 * with the specified polarity.
 *
 * @param from source variable name
 * @param to target variable name
 * @param polarity the direction of influence (positive, negative, or unknown)
 * @param comment optional annotation (e.g., "after a delay")
 */
public record CausalLinkDef(
        String from,
        String to,
        Polarity polarity,
        String comment
) {

    public enum Polarity {
        POSITIVE("+"),
        NEGATIVE("-"),
        UNKNOWN("?");

        private final String symbol;

        Polarity(String symbol) {
            this.symbol = symbol;
        }

        /**
         * Returns the display symbol for this polarity ({@code "+"}, {@code "-"}, or {@code "?"}).
         *
         * @return the polarity symbol
         */
        public String symbol() {
            return symbol;
        }

        /**
         * Parses a symbol string into a {@code Polarity} value.
         * Returns {@link #UNKNOWN} for {@code null} or unrecognized symbols.
         *
         * @param symbol the symbol to parse ({@code "+"}, {@code "-"}, or anything else)
         * @return the corresponding polarity
         */
        public static Polarity fromSymbol(String symbol) {
            if (symbol == null) {
                return UNKNOWN;
            }
            return switch (symbol) {
                case "+" -> POSITIVE;
                case "-" -> NEGATIVE;
                default -> UNKNOWN;
            };
        }
    }

    public CausalLinkDef {
        if (from == null || from.isBlank()) {
            throw new IllegalArgumentException("Causal link source must not be blank");
        }
        if (to == null || to.isBlank()) {
            throw new IllegalArgumentException("Causal link target must not be blank");
        }
        if (polarity == null) {
            polarity = Polarity.UNKNOWN;
        }
    }

    /**
     * Creates a causal link without a comment.
     *
     * @param from     the source variable name
     * @param to       the target variable name
     * @param polarity the direction of influence
     */
    public CausalLinkDef(String from, String to, Polarity polarity) {
        this(from, to, polarity, null);
    }

    /**
     * Creates a causal link with {@link Polarity#UNKNOWN} polarity and no comment.
     *
     * @param from the source variable name
     * @param to   the target variable name
     */
    public CausalLinkDef(String from, String to) {
        this(from, to, Polarity.UNKNOWN, null);
    }
}
