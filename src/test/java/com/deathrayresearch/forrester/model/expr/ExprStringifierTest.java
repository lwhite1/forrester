package com.deathrayresearch.forrester.model.expr;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("ExprStringifier")
class ExprStringifierTest {

    @Test
    void shouldStringifyLiteral() {
        assertEquals("42", ExprStringifier.stringify(new Expr.Literal(42)));
    }

    @Test
    void shouldStringifyDecimalLiteral() {
        assertEquals("3.14", ExprStringifier.stringify(new Expr.Literal(3.14)));
    }

    @Test
    void shouldStringifyRef() {
        assertEquals("Population", ExprStringifier.stringify(new Expr.Ref("Population")));
    }

    @Test
    void shouldQuoteRefWithSpaces() {
        assertEquals("`Tasks Remaining`",
                ExprStringifier.stringify(new Expr.Ref("Tasks Remaining")));
    }

    @Test
    void shouldOmitParensWhenNotNeeded() {
        // a + b * c → "a + b * c" (no parens needed, * binds tighter)
        Expr expr = new Expr.BinaryOp(
                new Expr.Ref("a"),
                BinaryOperator.ADD,
                new Expr.BinaryOp(new Expr.Ref("b"), BinaryOperator.MUL, new Expr.Ref("c")));
        assertEquals("a + b * c", ExprStringifier.stringify(expr));
    }

    @Test
    void shouldAddParensWhenNeeded() {
        // (a + b) * c → needs parens around a+b
        Expr expr = new Expr.BinaryOp(
                new Expr.BinaryOp(new Expr.Ref("a"), BinaryOperator.ADD, new Expr.Ref("b")),
                BinaryOperator.MUL,
                new Expr.Ref("c"));
        assertEquals("(a + b) * c", ExprStringifier.stringify(expr));
    }

    @Test
    void shouldHandleNestedSamePrecedenceLeftAssociative() {
        // (a - b) + c → "a - b + c" (left-associative, no parens needed for left)
        Expr expr = new Expr.BinaryOp(
                new Expr.BinaryOp(new Expr.Ref("a"), BinaryOperator.SUB, new Expr.Ref("b")),
                BinaryOperator.ADD,
                new Expr.Ref("c"));
        assertEquals("a - b + c", ExprStringifier.stringify(expr));
    }

    @Test
    void shouldAddParensForRightSubtractionAssociativity() {
        // a - (b + c) → "a - (b + c)" (needs parens on right for same precedence)
        Expr expr = new Expr.BinaryOp(
                new Expr.Ref("a"),
                BinaryOperator.SUB,
                new Expr.BinaryOp(new Expr.Ref("b"), BinaryOperator.ADD, new Expr.Ref("c")));
        assertEquals("a - (b + c)", ExprStringifier.stringify(expr));
    }

    @Test
    void shouldStringifyUnary() {
        assertEquals("-x", ExprStringifier.stringify(
                new Expr.UnaryOp(UnaryOperator.NEGATE, new Expr.Ref("x"))));
    }

    @Test
    void shouldStringifyNot() {
        assertEquals("!flag", ExprStringifier.stringify(
                new Expr.UnaryOp(UnaryOperator.NOT, new Expr.Ref("flag"))));
    }

    @Test
    void shouldStringifyFunctionCall() {
        assertEquals("SMOOTH(x, 5)", ExprStringifier.stringify(
                new Expr.FunctionCall("SMOOTH",
                        List.of(new Expr.Ref("x"), new Expr.Literal(5)))));
    }

    @Test
    void shouldStringifyConditional() {
        assertEquals("IF(x > 0, x, 0)", ExprStringifier.stringify(
                new Expr.Conditional(
                        new Expr.BinaryOp(new Expr.Ref("x"), BinaryOperator.GT, new Expr.Literal(0)),
                        new Expr.Ref("x"),
                        new Expr.Literal(0))));
    }

    @Test
    void shouldStringifyZeroArgFunction() {
        assertEquals("TIME", ExprStringifier.stringify(
                new Expr.FunctionCall("TIME", List.of())));
    }

    @Test
    void shouldStringifyPowerRightAssociative() {
        // a ^ (b ^ c) → "a ^ b ^ c" (right-associative, no parens)
        Expr expr = new Expr.BinaryOp(
                new Expr.Ref("a"),
                BinaryOperator.POW,
                new Expr.BinaryOp(new Expr.Ref("b"), BinaryOperator.POW, new Expr.Ref("c")));
        assertEquals("a ^ b ^ c", ExprStringifier.stringify(expr));
    }
}
