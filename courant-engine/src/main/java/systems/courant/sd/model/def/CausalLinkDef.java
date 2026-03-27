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
 * @param bias control point offset along the chord direction ({@code 0.0} = centered)
 * @param color optional custom color as a hex string (e.g. "#FF0000"), or null for default
 */
public record CausalLinkDef(
        String from,
        String to,
        Polarity polarity,
        String comment,
        double strength,
        double bias,
        String color
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
     * Returns true if this link has a non-zero bias (apex shifted along chord).
     */
    public boolean hasBias() {
        return bias != 0.0;
    }

    /**
     * Returns true if this link has a custom color.
     */
    public boolean hasColor() {
        return color != null && !color.isBlank();
    }

    /**
     * Backward-compatible constructor without color.
     */
    public CausalLinkDef(String from, String to, Polarity polarity, String comment,
                          double strength, double bias) {
        this(from, to, polarity, comment, strength, bias, null);
    }

    /**
     * Creates a causal link with default (auto) curve strength and no bias.
     */
    public CausalLinkDef(String from, String to, Polarity polarity, String comment,
                          double strength) {
        this(from, to, polarity, comment, strength, 0.0, null);
    }

    /**
     * Creates a causal link with default (auto) curve strength and no bias.
     */
    public CausalLinkDef(String from, String to, Polarity polarity, String comment) {
        this(from, to, polarity, comment, Double.NaN, 0.0, null);
    }

    /**
     * Creates a causal link without a comment.
     */
    public CausalLinkDef(String from, String to, Polarity polarity) {
        this(from, to, polarity, null, Double.NaN, 0.0, null);
    }

    /**
     * Creates a causal link with {@link Polarity#UNKNOWN} polarity and no comment.
     */
    public CausalLinkDef(String from, String to) {
        this(from, to, Polarity.UNKNOWN, null, Double.NaN, 0.0, null);
    }

    /**
     * Returns a copy of this link with a different strength value, preserving bias and color.
     */
    public CausalLinkDef withStrength(double newStrength) {
        return new CausalLinkDef(from, to, polarity, comment, newStrength, bias, color);
    }

    /**
     * Returns a copy of this link with a different bias value, preserving strength and color.
     */
    public CausalLinkDef withBias(double newBias) {
        return new CausalLinkDef(from, to, polarity, comment, strength, newBias, color);
    }

    /**
     * Returns a copy of this link with both strength and bias replaced, preserving color.
     */
    public CausalLinkDef withStrengthAndBias(double newStrength, double newBias) {
        return new CausalLinkDef(from, to, polarity, comment, newStrength, newBias, color);
    }

    /**
     * Returns a copy of this link with a different color, preserving all other fields.
     */
    public CausalLinkDef withColor(String newColor) {
        return new CausalLinkDef(from, to, polarity, comment, strength, bias, newColor);
    }
}
