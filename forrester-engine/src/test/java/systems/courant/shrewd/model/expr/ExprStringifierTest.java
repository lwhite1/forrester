package systems.courant.forrester.model.expr;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("ExprStringifier")
class ExprStringifierTest {

    @Test
    void shouldStringifyLiteral() {
        assertThat(ExprStringifier.stringify(new Expr.Literal(42))).isEqualTo("42");
    }

    @Test
    void shouldStringifyDecimalLiteral() {
        assertThat(ExprStringifier.stringify(new Expr.Literal(3.14))).isEqualTo("3.14");
    }

    @Test
    void shouldStringifyRef() {
        assertThat(ExprStringifier.stringify(new Expr.Ref("Population"))).isEqualTo("Population");
    }

    @Test
    void shouldQuoteRefWithSpaces() {
        assertThat(ExprStringifier.stringify(new Expr.Ref("Tasks Remaining")))
                .isEqualTo("`Tasks Remaining`");
    }

    @Test
    void shouldOmitParensWhenNotNeeded() {
        // a + b * c -> "a + b * c" (no parens needed, * binds tighter)
        Expr expr = new Expr.BinaryOp(
                new Expr.Ref("a"),
                BinaryOperator.ADD,
                new Expr.BinaryOp(new Expr.Ref("b"), BinaryOperator.MUL, new Expr.Ref("c")));
        assertThat(ExprStringifier.stringify(expr)).isEqualTo("a + b * c");
    }

    @Test
    void shouldAddParensWhenNeeded() {
        // (a + b) * c -> needs parens around a+b
        Expr expr = new Expr.BinaryOp(
                new Expr.BinaryOp(new Expr.Ref("a"), BinaryOperator.ADD, new Expr.Ref("b")),
                BinaryOperator.MUL,
                new Expr.Ref("c"));
        assertThat(ExprStringifier.stringify(expr)).isEqualTo("(a + b) * c");
    }

    @Test
    void shouldHandleNestedSamePrecedenceLeftAssociative() {
        // (a - b) + c -> "a - b + c" (left-associative, no parens needed for left)
        Expr expr = new Expr.BinaryOp(
                new Expr.BinaryOp(new Expr.Ref("a"), BinaryOperator.SUB, new Expr.Ref("b")),
                BinaryOperator.ADD,
                new Expr.Ref("c"));
        assertThat(ExprStringifier.stringify(expr)).isEqualTo("a - b + c");
    }

    @Test
    void shouldAddParensForRightSubtractionAssociativity() {
        // a - (b + c) -> "a - (b + c)" (needs parens on right for same precedence)
        Expr expr = new Expr.BinaryOp(
                new Expr.Ref("a"),
                BinaryOperator.SUB,
                new Expr.BinaryOp(new Expr.Ref("b"), BinaryOperator.ADD, new Expr.Ref("c")));
        assertThat(ExprStringifier.stringify(expr)).isEqualTo("a - (b + c)");
    }

    @Test
    void shouldStringifyUnary() {
        assertThat(ExprStringifier.stringify(
                new Expr.UnaryOp(UnaryOperator.NEGATE, new Expr.Ref("x")))).isEqualTo("-x");
    }

    @Test
    void shouldStringifyNot() {
        assertThat(ExprStringifier.stringify(
                new Expr.UnaryOp(UnaryOperator.NOT, new Expr.Ref("flag")))).isEqualTo("not flag");
    }

    @Test
    void shouldStringifyFunctionCall() {
        assertThat(ExprStringifier.stringify(
                new Expr.FunctionCall("SMOOTH",
                        List.of(new Expr.Ref("x"), new Expr.Literal(5))))).isEqualTo("SMOOTH(x, 5)");
    }

    @Test
    void shouldStringifyConditional() {
        assertThat(ExprStringifier.stringify(
                new Expr.Conditional(
                        new Expr.BinaryOp(new Expr.Ref("x"), BinaryOperator.GT, new Expr.Literal(0)),
                        new Expr.Ref("x"),
                        new Expr.Literal(0)))).isEqualTo("IF(x > 0, x, 0)");
    }

    @Test
    void shouldStringifyZeroArgFunction() {
        assertThat(ExprStringifier.stringify(
                new Expr.FunctionCall("TIME", List.of()))).isEqualTo("TIME");
    }

    @Test
    void shouldStringifyPowerRightAssociative() {
        // a ** (b ** c) -> "a ** b ** c" (right-associative, no parens)
        Expr expr = new Expr.BinaryOp(
                new Expr.Ref("a"),
                BinaryOperator.POW,
                new Expr.BinaryOp(new Expr.Ref("b"), BinaryOperator.POW, new Expr.Ref("c")));
        assertThat(ExprStringifier.stringify(expr)).isEqualTo("a ** b ** c");
    }

    @Test
    void shouldAddParensForLeftAssociativePower() {
        // (a ** b) ** c -> must emit "(a ** b) ** c" (not "a ** b ** c" which re-parses as a ** (b ** c))
        Expr expr = new Expr.BinaryOp(
                new Expr.BinaryOp(new Expr.Ref("a"), BinaryOperator.POW, new Expr.Ref("b")),
                BinaryOperator.POW,
                new Expr.Ref("c"));
        assertThat(ExprStringifier.stringify(expr)).isEqualTo("(a ** b) ** c");
    }

    @Test
    void shouldThrowForNaNLiteral() {
        assertThatThrownBy(() -> ExprStringifier.stringify(new Expr.Literal(Double.NaN)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("non-finite");
    }

    @Test
    void shouldThrowForInfinityLiteral() {
        assertThatThrownBy(() -> ExprStringifier.stringify(new Expr.Literal(Double.POSITIVE_INFINITY)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("non-finite");
    }

    @Test
    void shouldStringifyNegativeLiteral() {
        assertThat(ExprStringifier.stringify(new Expr.Literal(-5))).isEqualTo("-5");
    }

    @Test
    void shouldStringifyUnaryWithBinaryOperand() {
        // -(a + b) needs parens around binary operand
        Expr expr = new Expr.UnaryOp(
                UnaryOperator.NEGATE,
                new Expr.BinaryOp(new Expr.Ref("a"), BinaryOperator.ADD, new Expr.Ref("b")));
        assertThat(ExprStringifier.stringify(expr)).isEqualTo("-(a + b)");
    }

    @Test
    void shouldQuoteReservedWords() {
        assertThat(ExprStringifier.stringify(new Expr.Ref("IF"))).isEqualTo("`IF`");
        assertThat(ExprStringifier.stringify(new Expr.Ref("TIME"))).isEqualTo("`TIME`");
        assertThat(ExprStringifier.stringify(new Expr.Ref("DT"))).isEqualTo("`DT`");
    }
}
