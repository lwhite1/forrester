package com.deathrayresearch.forrester.model.expr;

import java.util.List;
import java.util.Objects;

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

        public Ref {
            Objects.requireNonNull(name, "Ref name must not be null");
            if (name.isBlank()) {
                throw new IllegalArgumentException("Ref name must not be blank");
            }
        }
    }

    /**
     * A binary operation (e.g. {@code a + b}, {@code x * y}).
     */
    record BinaryOp(Expr left, BinaryOperator operator, Expr right) implements Expr {

        public BinaryOp {
            Objects.requireNonNull(left, "BinaryOp left must not be null");
            Objects.requireNonNull(operator, "BinaryOp operator must not be null");
            Objects.requireNonNull(right, "BinaryOp right must not be null");
        }
    }

    /**
     * A unary operation (e.g. {@code -x}, {@code !flag}).
     */
    record UnaryOp(UnaryOperator operator, Expr operand) implements Expr {

        public UnaryOp {
            Objects.requireNonNull(operator, "UnaryOp operator must not be null");
            Objects.requireNonNull(operand, "UnaryOp operand must not be null");
        }
    }

    /**
     * A function call with a name and argument list (e.g. {@code SMOOTH(input, 5)}).
     */
    record FunctionCall(String name, List<Expr> arguments) implements Expr {

        public FunctionCall {
            Objects.requireNonNull(name, "FunctionCall name must not be null");
            arguments = List.copyOf(arguments);
        }
    }

    /**
     * A conditional expression, corresponding to {@code IF(condition, thenExpr, elseExpr)}.
     */
    record Conditional(Expr condition, Expr thenExpr, Expr elseExpr) implements Expr {

        public Conditional {
            Objects.requireNonNull(condition, "Conditional condition must not be null");
            Objects.requireNonNull(thenExpr, "Conditional thenExpr must not be null");
            Objects.requireNonNull(elseExpr, "Conditional elseExpr must not be null");
        }
    }
}
