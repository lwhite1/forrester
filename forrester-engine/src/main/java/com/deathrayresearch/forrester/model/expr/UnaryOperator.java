package com.deathrayresearch.forrester.model.expr;

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

    public String symbol() {
        return symbol;
    }
}
