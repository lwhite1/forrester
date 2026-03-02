package com.deathrayresearch.forrester.model.expr;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("ExprDependencies")
class ExprDependenciesTest {

    @Test
    void shouldReturnEmptyForLiteral() {
        Set<String> deps = ExprDependencies.extract(new Expr.Literal(42));
        assertTrue(deps.isEmpty());
    }

    @Test
    void shouldReturnSingletonForRef() {
        Set<String> deps = ExprDependencies.extract(new Expr.Ref("Population"));
        assertEquals(Set.of("Population"), deps);
    }

    @Test
    void shouldReturnUnionForBinaryOp() {
        Expr expr = new Expr.BinaryOp(
                new Expr.Ref("a"), BinaryOperator.ADD, new Expr.Ref("b"));
        assertEquals(Set.of("a", "b"), ExprDependencies.extract(expr));
    }

    @Test
    void shouldDeduplicateRefs() {
        // x * x → should return {"x"}, not {"x", "x"}
        Expr expr = new Expr.BinaryOp(
                new Expr.Ref("x"), BinaryOperator.MUL, new Expr.Ref("x"));
        Set<String> deps = ExprDependencies.extract(expr);
        assertEquals(1, deps.size());
        assertTrue(deps.contains("x"));
    }

    @Test
    void shouldExtractFromUnaryOp() {
        Expr expr = new Expr.UnaryOp(UnaryOperator.NEGATE, new Expr.Ref("x"));
        assertEquals(Set.of("x"), ExprDependencies.extract(expr));
    }

    @Test
    void shouldExtractFromFunctionArgs() {
        Expr expr = new Expr.FunctionCall("SMOOTH",
                List.of(new Expr.Ref("input"), new Expr.Literal(5)));
        assertEquals(Set.of("input"), ExprDependencies.extract(expr));
    }

    @Test
    void shouldExtractFromConditional() {
        Expr expr = new Expr.Conditional(
                new Expr.Ref("condition"),
                new Expr.Ref("thenVal"),
                new Expr.Ref("elseVal"));
        assertEquals(Set.of("condition", "thenVal", "elseVal"),
                ExprDependencies.extract(expr));
    }

    @Test
    void shouldExtractFromComplexNested() {
        // Parse a complex formula and verify all refs found
        Expr expr = ExprParser.parse(
                "Contact_Rate * Infectious / (Susceptible + Infectious + Recovered) * Infectivity * Susceptible");
        Set<String> deps = ExprDependencies.extract(expr);
        assertEquals(Set.of("Contact_Rate", "Infectious", "Susceptible", "Recovered", "Infectivity"),
                deps);
    }

    @Test
    void shouldExtractFromNestedFunctions() {
        Expr expr = ExprParser.parse("MAX(ABS(x), MIN(y, z))");
        assertEquals(Set.of("x", "y", "z"), ExprDependencies.extract(expr));
    }
}
