package com.deathrayresearch.forrester.model.expr;

import java.util.List;

/**
 * Sealed interface representing an expression AST node. Expressions are pure data —
 * they can be inspected, serialized, and compiled into executable formulas.
 *
 * <p>Six concrete forms are permitted:
 * <ul>
 *     <li>{@link Literal} — numeric constant</li>
 *     <li>{@link Ref} — named reference to a model element</li>
 *     <li>{@link BinaryOp} — infix binary operation</li>
 *     <li>{@link UnaryOp} — prefix unary operation</li>
 *     <li>{@link FunctionCall} — named function with arguments</li>
 *     <li>{@link Conditional} — IF(condition, thenExpr, elseExpr)</li>
 * </ul>
 */
public sealed interface Expr
        permits Expr.Literal, Expr.Ref, Expr.BinaryOp, Expr.UnaryOp,
                Expr.FunctionCall, Expr.Conditional {

    /**
     * A numeric literal value (e.g. {@code 3.14}, {@code 1e-6}).
     */
    record Literal(double value) implements Expr {
    }

    /**
     * A named reference to a model element (stock, flow, variable, constant, etc.).
     * The name preserves the original form from the expression string, including spaces
     * for quoted identifiers.
     */
    record Ref(String name) implements Expr {
    }

    /**
     * A binary operation (e.g. {@code a + b}, {@code x * y}).
     */
    record BinaryOp(Expr left, BinaryOperator operator, Expr right) implements Expr {
    }

    /**
     * A unary operation (e.g. {@code -x}, {@code !flag}).
     */
    record UnaryOp(UnaryOperator operator, Expr operand) implements Expr {
    }

    /**
     * A function call with a name and argument list (e.g. {@code SMOOTH(input, 5)}).
     */
    record FunctionCall(String name, List<Expr> arguments) implements Expr {

        public FunctionCall {
            arguments = List.copyOf(arguments);
        }
    }

    /**
     * A conditional expression, corresponding to {@code IF(condition, thenExpr, elseExpr)}.
     */
    record Conditional(Expr condition, Expr thenExpr, Expr elseExpr) implements Expr {
    }
}
