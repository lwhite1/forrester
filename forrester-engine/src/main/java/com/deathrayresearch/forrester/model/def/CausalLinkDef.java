package com.deathrayresearch.forrester.model.def;

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

        public String symbol() {
            return symbol;
        }

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

    public CausalLinkDef(String from, String to, Polarity polarity) {
        this(from, to, polarity, null);
    }

    public CausalLinkDef(String from, String to) {
        this(from, to, Polarity.UNKNOWN, null);
    }
}
