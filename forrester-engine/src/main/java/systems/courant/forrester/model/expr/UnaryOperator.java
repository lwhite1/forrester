package systems.courant.forrester.model.expr;

/**
 * Unary operators supported in expression ASTs.
 */
public enum UnaryOperator {

    NEGATE("-"),
    NOT("not ");

    private final String symbol;

    UnaryOperator(String symbol) {
        this.symbol = symbol;
    }

    /**
     * Returns the prefix symbol for this operator (e.g., {@code "-"}, {@code "not "}).
     */
    public String symbol() {
        return symbol;
    }
}
