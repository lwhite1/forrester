package com.deathrayresearch.forrester.model.expr;

/**
 * Binary operators supported in expression ASTs, with symbol and precedence.
 *
 * <p>Higher precedence values bind tighter. Precedence groups:
 * <ul>
 *     <li>-2: OR</li>
 *     <li>-1: AND</li>
 *     <li> 0: comparison (==, !=, &lt;, &lt;=, &gt;, &gt;=)</li>
 *     <li> 1: additive (+, -)</li>
 *     <li> 2: multiplicative (*, /, %)</li>
 *     <li> 3: power (^)</li>
 * </ul>
 */
public enum BinaryOperator {

    ADD("+", 1),
    SUB("-", 1),
    MUL("*", 2),
    DIV("/", 2),
    MOD("%", 2),
    POW("^", 3),
    EQ("==", 0),
    NE("!=", 0),
    LT("<", 0),
    LE("<=", 0),
    GT(">", 0),
    GE(">=", 0),
    AND("&&", -1),
    OR("||", -2);

    private final String symbol;
    private final int precedence;

    BinaryOperator(String symbol, int precedence) {
        this.symbol = symbol;
        this.precedence = precedence;
    }

    public String symbol() {
        return symbol;
    }

    public int precedence() {
        return precedence;
    }
}
