package systems.courant.sd.model.def;

/**
 * A causal link between two variables in a causal loop diagram.
 * The link indicates that the source variable causally influences the target variable,
 * with the specified polarity.
 *
 * @param from source variable name
 * @param to target variable name
 * @param polarity the direction of influence (positive, negative, or unknown)
 * @param comment optional annotation (e.g., "after a delay")
 * @param strength curve strength override ({@code NaN} = auto-computed curvature)
 */
public record CausalLinkDef(
        String from,
        String to,
        Polarity polarity,
        String comment,
        double strength
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
     * Returns true if this link has a user-defined curve strength override.
     */
    public boolean hasStrength() {
        return !Double.isNaN(strength);
    }

    /**
     * Creates a causal link with default (auto) curve strength.
     *
     * @param from     the source variable name
     * @param to       the target variable name
     * @param polarity the direction of influence
     * @param comment  optional annotation
     */
    public CausalLinkDef(String from, String to, Polarity polarity, String comment) {
        this(from, to, polarity, comment, Double.NaN);
    }

    /**
     * Creates a causal link without a comment.
     *
     * @param from     the source variable name
     * @param to       the target variable name
     * @param polarity the direction of influence
     */
    public CausalLinkDef(String from, String to, Polarity polarity) {
        this(from, to, polarity, null, Double.NaN);
    }

    /**
     * Creates a causal link with {@link Polarity#UNKNOWN} polarity and no comment.
     *
     * @param from the source variable name
     * @param to   the target variable name
     */
    public CausalLinkDef(String from, String to) {
        this(from, to, Polarity.UNKNOWN, null, Double.NaN);
    }

    /**
     * Returns a copy of this link with a different strength value.
     */
    public CausalLinkDef withStrength(double newStrength) {
        return new CausalLinkDef(from, to, polarity, comment, newStrength);
    }
}
