package systems.courant.sd.model.expr;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;

@DisplayName("ExprParser")
class ExprParserTest {

    @Nested
    @DisplayName("Literals")
    class Literals {

        @Test
        void shouldParseInteger() {
            Expr result = ExprParser.parse("42");
            assertThat(result).isInstanceOf(Expr.Literal.class);
            assertThat(((Expr.Literal) result).value()).isEqualTo(42.0);
        }

        @Test
        void shouldParseDecimal() {
            Expr result = ExprParser.parse("3.14");
            assertThat(result).isInstanceOf(Expr.Literal.class);
            assertThat(((Expr.Literal) result).value()).isCloseTo(3.14, within(1e-10));
        }

        @Test
        void shouldParseScientificNotation() {
            Expr result = ExprParser.parse("1.5e3");
            assertThat(result).isInstanceOf(Expr.Literal.class);
            assertThat(((Expr.Literal) result).value()).isEqualTo(1500.0);
        }

        @Test
        void shouldParseNegativeExponent() {
            Expr result = ExprParser.parse("2.5E-4");
            assertThat(result).isInstanceOf(Expr.Literal.class);
            assertThat(((Expr.Literal) result).value()).isCloseTo(2.5e-4, within(1e-15));
        }

        @Test
        void shouldParseLeadingDecimalPoint() {
            Expr result = ExprParser.parse(".5");
            assertThat(result).isInstanceOf(Expr.Literal.class);
            assertThat(((Expr.Literal) result).value()).isEqualTo(0.5);
        }

        @Test
        void shouldParseZero() {
            Expr result = ExprParser.parse("0");
            assertThat(result).isEqualTo(new Expr.Literal(0.0));
        }
    }

    @Nested
    @DisplayName("References")
    class References {

        @Test
        void shouldParseSimpleIdentifier() {
            Expr result = ExprParser.parse("Population");
            assertThat(result).isEqualTo(new Expr.Ref("Population"));
        }

        @Test
        void shouldParseIdentifierWithUnderscore() {
            Expr result = ExprParser.parse("Birth_Rate");
            assertThat(result).isEqualTo(new Expr.Ref("Birth_Rate"));
        }

        @Test
        void shouldParseQuotedIdentifier() {
            Expr result = ExprParser.parse("`Tasks Remaining`");
            assertThat(result).isEqualTo(new Expr.Ref("Tasks Remaining"));
        }

        @Test
        void shouldParseQuotedIdentifierWithSpecialChars() {
            Expr result = ExprParser.parse("`Coffee Temperature`");
            assertThat(result).isEqualTo(new Expr.Ref("Coffee Temperature"));
        }

        @Test
        void shouldParseQuotedIdentifierAsFunctionCall() {
            Expr result = ExprParser.parse("`Effect of Density`(density)");
            assertThat(result).isInstanceOf(Expr.FunctionCall.class);
            Expr.FunctionCall call = (Expr.FunctionCall) result;
            assertThat(call.name()).isEqualTo("Effect of Density");
            assertThat(call.arguments()).hasSize(1);
            assertThat(call.arguments().get(0)).isEqualTo(new Expr.Ref("density"));
        }
    }

    @Nested
    @DisplayName("Binary operations")
    class BinaryOperations {

        @Test
        void shouldParseAddition() {
            Expr result = ExprParser.parse("a + b");
            assertThat(result).isEqualTo(
                    new Expr.BinaryOp(new Expr.Ref("a"), BinaryOperator.ADD, new Expr.Ref("b")));
        }

        @Test
        void shouldParseSubtraction() {
            Expr result = ExprParser.parse("a - b");
            assertThat(result).isEqualTo(
                    new Expr.BinaryOp(new Expr.Ref("a"), BinaryOperator.SUB, new Expr.Ref("b")));
        }

        @Test
        void shouldParseMultiplication() {
            Expr result = ExprParser.parse("a * b");
            assertThat(result).isEqualTo(
                    new Expr.BinaryOp(new Expr.Ref("a"), BinaryOperator.MUL, new Expr.Ref("b")));
        }

        @Test
        void shouldParseDivision() {
            Expr result = ExprParser.parse("a / b");
            assertThat(result).isEqualTo(
                    new Expr.BinaryOp(new Expr.Ref("a"), BinaryOperator.DIV, new Expr.Ref("b")));
        }

        @Test
        void shouldParseModulo() {
            Expr result = ExprParser.parse("a % b");
            assertThat(result).isEqualTo(
                    new Expr.BinaryOp(new Expr.Ref("a"), BinaryOperator.MOD, new Expr.Ref("b")));
        }

        @Test
        void shouldParsePower() {
            Expr result = ExprParser.parse("a ** b");
            assertThat(result).isEqualTo(
                    new Expr.BinaryOp(new Expr.Ref("a"), BinaryOperator.POW, new Expr.Ref("b")));
        }

        @Test
        void shouldParseComparisons() {
            assertThat(((Expr.BinaryOp) ExprParser.parse("a == b")).operator())
                    .isEqualTo(BinaryOperator.EQ);
            assertThat(((Expr.BinaryOp) ExprParser.parse("a != b")).operator())
                    .isEqualTo(BinaryOperator.NE);
            assertThat(((Expr.BinaryOp) ExprParser.parse("a < b")).operator())
                    .isEqualTo(BinaryOperator.LT);
            assertThat(((Expr.BinaryOp) ExprParser.parse("a <= b")).operator())
                    .isEqualTo(BinaryOperator.LE);
            assertThat(((Expr.BinaryOp) ExprParser.parse("a > b")).operator())
                    .isEqualTo(BinaryOperator.GT);
            assertThat(((Expr.BinaryOp) ExprParser.parse("a >= b")).operator())
                    .isEqualTo(BinaryOperator.GE);
        }

        @Test
        void shouldParseSingleEqualsAsEquality() {
            Expr result = ExprParser.parse("x = 5");
            assertThat(result).isInstanceOf(Expr.BinaryOp.class);
            Expr.BinaryOp bin = (Expr.BinaryOp) result;
            assertThat(bin.operator()).isEqualTo(BinaryOperator.EQ);
            assertThat(bin.left()).isEqualTo(new Expr.Ref("x"));
            assertThat(bin.right()).isEqualTo(new Expr.Literal(5.0));
        }

        @Test
        void shouldParseDoubleEqualsAsEquality() {
            Expr result = ExprParser.parse("x == 5");
            assertThat(result).isInstanceOf(Expr.BinaryOp.class);
            assertThat(((Expr.BinaryOp) result).operator()).isEqualTo(BinaryOperator.EQ);
        }

        @Test
        void shouldParseLogicalOr() {
            Expr result = ExprParser.parse("a or b");
            assertThat(result).isEqualTo(
                    new Expr.BinaryOp(new Expr.Ref("a"), BinaryOperator.OR, new Expr.Ref("b")));
        }

        @Test
        void shouldParseLogicalAnd() {
            Expr result = ExprParser.parse("a and b");
            assertThat(result).isEqualTo(
                    new Expr.BinaryOp(new Expr.Ref("a"), BinaryOperator.AND, new Expr.Ref("b")));
        }
    }

    @Nested
    @DisplayName("Precedence and associativity")
    class Precedence {

        @Test
        void shouldRespectMultOverAdd() {
            // a + b * c  should parse as  a + (b * c)
            Expr result = ExprParser.parse("a + b * c");
            Expr.BinaryOp top = (Expr.BinaryOp) result;
            assertThat(top.operator()).isEqualTo(BinaryOperator.ADD);
            assertThat(top.left()).isEqualTo(new Expr.Ref("a"));
            Expr.BinaryOp right = (Expr.BinaryOp) top.right();
            assertThat(right.operator()).isEqualTo(BinaryOperator.MUL);
        }

        @Test
        void shouldRespectParentheses() {
            // (a + b) * c
            Expr result = ExprParser.parse("(a + b) * c");
            Expr.BinaryOp top = (Expr.BinaryOp) result;
            assertThat(top.operator()).isEqualTo(BinaryOperator.MUL);
            assertThat(top.left()).isInstanceOf(Expr.BinaryOp.class);
        }

        @Test
        void shouldRespectPowerRightAssociativity() {
            // a ** b ** c  should parse as  a ** (b ** c)
            Expr result = ExprParser.parse("a ** b ** c");
            Expr.BinaryOp top = (Expr.BinaryOp) result;
            assertThat(top.operator()).isEqualTo(BinaryOperator.POW);
            assertThat(top.left()).isEqualTo(new Expr.Ref("a"));
            Expr.BinaryOp right = (Expr.BinaryOp) top.right();
            assertThat(right.operator()).isEqualTo(BinaryOperator.POW);
            assertThat(right.left()).isEqualTo(new Expr.Ref("b"));
            assertThat(right.right()).isEqualTo(new Expr.Ref("c"));
        }

        @Test
        void shouldRespectAdditionLeftAssociativity() {
            // a - b + c  should parse as  (a - b) + c
            Expr result = ExprParser.parse("a - b + c");
            Expr.BinaryOp top = (Expr.BinaryOp) result;
            assertThat(top.operator()).isEqualTo(BinaryOperator.ADD);
            assertThat(top.right()).isEqualTo(new Expr.Ref("c"));
            Expr.BinaryOp left = (Expr.BinaryOp) top.left();
            assertThat(left.operator()).isEqualTo(BinaryOperator.SUB);
        }

        @Test
        void shouldHandleComplexPrecedence() {
            // a or b and c == d + e * f
            // Should parse as: a or (b and ((c == (d + (e * f)))))
            Expr result = ExprParser.parse("a or b and c == d + e * f");
            Expr.BinaryOp top = (Expr.BinaryOp) result;
            assertThat(top.operator()).isEqualTo(BinaryOperator.OR);
        }
    }

    @Nested
    @DisplayName("Unary operations")
    class UnaryOps {

        @Test
        void shouldParseNegation() {
            Expr result = ExprParser.parse("-x");
            assertThat(result).isEqualTo(new Expr.UnaryOp(UnaryOperator.NEGATE, new Expr.Ref("x")));
        }

        @Test
        void shouldParseNot() {
            Expr result = ExprParser.parse("not flag");
            assertThat(result).isEqualTo(new Expr.UnaryOp(UnaryOperator.NOT, new Expr.Ref("flag")));
        }

        @Test
        void shouldParseDoubleNegation() {
            Expr result = ExprParser.parse("--x");
            Expr.UnaryOp outer = (Expr.UnaryOp) result;
            assertThat(outer.operator()).isEqualTo(UnaryOperator.NEGATE);
            Expr.UnaryOp inner = (Expr.UnaryOp) outer.operand();
            assertThat(inner.operator()).isEqualTo(UnaryOperator.NEGATE);
            assertThat(inner.operand()).isEqualTo(new Expr.Ref("x"));
        }

        @Test
        void shouldParseNegationOfExpression() {
            Expr result = ExprParser.parse("-(a + b)");
            assertThat(result).isInstanceOf(Expr.UnaryOp.class);
        }

        @Test
        void shouldParseUnaryPlus() {
            Expr result = ExprParser.parse("+x");
            assertThat(result).isEqualTo(new Expr.Ref("x"));
        }

        @Test
        void shouldParseUnaryPlusBeforeSubtraction() {
            // Common in Vensim net flow equations: +inflow-outflow
            Expr result = ExprParser.parse("+a-b");
            assertThat(result).isInstanceOf(Expr.BinaryOp.class);
            Expr.BinaryOp bin = (Expr.BinaryOp) result;
            assertThat(bin.operator()).isEqualTo(BinaryOperator.SUB);
            assertThat(bin.left()).isEqualTo(new Expr.Ref("a"));
            assertThat(bin.right()).isEqualTo(new Expr.Ref("b"));
        }
    }

    @Nested
    @DisplayName("Function calls")
    class FunctionCalls {

        @Test
        void shouldParseSingleArgFunction() {
            Expr result = ExprParser.parse("ABS(x)");
            Expr.FunctionCall call = (Expr.FunctionCall) result;
            assertThat(call.name()).isEqualTo("ABS");
            assertThat(call.arguments().size()).isEqualTo(1);
            assertThat(call.arguments().get(0)).isEqualTo(new Expr.Ref("x"));
        }

        @Test
        void shouldParseMultiArgFunction() {
            Expr result = ExprParser.parse("SMOOTH(input, 5)");
            Expr.FunctionCall call = (Expr.FunctionCall) result;
            assertThat(call.name()).isEqualTo("SMOOTH");
            assertThat(call.arguments().size()).isEqualTo(2);
            assertThat(call.arguments().get(0)).isEqualTo(new Expr.Ref("input"));
            assertThat(call.arguments().get(1)).isEqualTo(new Expr.Literal(5.0));
        }

        @Test
        void shouldParseThreeArgFunction() {
            Expr result = ExprParser.parse("SMOOTH(input, 5, 10)");
            Expr.FunctionCall call = (Expr.FunctionCall) result;
            assertThat(call.arguments().size()).isEqualTo(3);
        }

        @Test
        void shouldParseNestedFunctionCalls() {
            Expr result = ExprParser.parse("MAX(ABS(x), ABS(y))");
            Expr.FunctionCall call = (Expr.FunctionCall) result;
            assertThat(call.name()).isEqualTo("MAX");
            assertThat(call.arguments().size()).isEqualTo(2);
            assertThat(call.arguments().get(0)).isInstanceOf(Expr.FunctionCall.class);
            assertThat(call.arguments().get(1)).isInstanceOf(Expr.FunctionCall.class);
        }

        @Test
        void shouldParseZeroArgFunctions() {
            Expr result = ExprParser.parse("TIME");
            Expr.FunctionCall call = (Expr.FunctionCall) result;
            assertThat(call.name()).isEqualTo("TIME");
            assertThat(call.arguments().isEmpty()).isTrue();
        }

        @Test
        void shouldParseDT() {
            Expr result = ExprParser.parse("DT");
            Expr.FunctionCall call = (Expr.FunctionCall) result;
            assertThat(call.name()).isEqualTo("DT");
            assertThat(call.arguments().isEmpty()).isTrue();
        }
    }

    @Nested
    @DisplayName("Conditional (IF)")
    class Conditionals {

        @Test
        void shouldParseSimpleIf() {
            Expr result = ExprParser.parse("IF(x > 0, x, 0)");
            assertThat(result).isInstanceOf(Expr.Conditional.class);
            Expr.Conditional cond = (Expr.Conditional) result;
            assertThat(cond.condition()).isInstanceOf(Expr.BinaryOp.class);
            assertThat(cond.thenExpr()).isEqualTo(new Expr.Ref("x"));
            assertThat(cond.elseExpr()).isEqualTo(new Expr.Literal(0.0));
        }

        @Test
        void shouldParseNestedIf() {
            Expr result = ExprParser.parse("IF(a > b, IF(c > d, 1, 2), 3)");
            Expr.Conditional outer = (Expr.Conditional) result;
            assertThat(outer.thenExpr()).isInstanceOf(Expr.Conditional.class);
        }
    }

    @Nested
    @DisplayName("Complex expressions")
    class ComplexExpressions {

        @Test
        void shouldParseSIRInfectionFormula() {
            String formula = "Contact_Rate * Infectious / (Susceptible + Infectious + Recovered) * Infectivity * Susceptible";
            Expr result = ExprParser.parse(formula);
            assertThat(result).isNotNull();
        }

        @Test
        void shouldParseFormulaWithQuotedIds() {
            String formula = "`Contact Rate` * `Infectious` / (`Susceptible` + `Infectious` + `Recovered`)";
            Expr result = ExprParser.parse(formula);
            assertThat(result).isNotNull();
        }

        @Test
        void shouldParseExpressionWithMixedOps() {
            Expr result = ExprParser.parse("a + b * c - d / e ** f");
            assertThat(result).isNotNull();
        }
    }

    @Nested
    @DisplayName("Error cases")
    class ErrorCases {

        @Test
        void shouldThrowOnEmptyInput() {
            assertThatThrownBy(() -> ExprParser.parse("")).isInstanceOf(ParseException.class);
        }

        @Test
        void shouldThrowOnNullInput() {
            assertThatThrownBy(() -> ExprParser.parse(null)).isInstanceOf(ParseException.class);
        }

        @Test
        void shouldThrowOnBlankInput() {
            assertThatThrownBy(() -> ExprParser.parse("   ")).isInstanceOf(ParseException.class);
        }

        @Test
        void shouldThrowOnUnterminatedQuote() {
            assertThatThrownBy(() -> ExprParser.parse("`unterminated")).isInstanceOf(ParseException.class);
        }

        @Test
        void shouldThrowOnMissingOperand() {
            assertThatThrownBy(() -> ExprParser.parse("a +")).isInstanceOf(ParseException.class);
        }

        @Test
        void shouldThrowOnUnmatchedParen() {
            assertThatThrownBy(() -> ExprParser.parse("(a + b")).isInstanceOf(ParseException.class);
        }

        @Test
        void shouldThrowOnExtraCloseParen() {
            assertThatThrownBy(() -> ExprParser.parse("a + b)")).isInstanceOf(ParseException.class);
        }

        @Test
        void shouldThrowOnEmptyQuotedIdentifier() {
            assertThatThrownBy(() -> ExprParser.parse("``")).isInstanceOf(ParseException.class);
        }
    }

    @Nested
    @DisplayName("Round-trip: parse -> stringify -> parse")
    class RoundTrip {

        private void assertRoundTrips(String input) {
            Expr first = ExprParser.parse(input);
            String stringified = ExprStringifier.stringify(first);
            Expr second = ExprParser.parse(stringified);
            assertThat(second).as(
                    "Round-trip failed.\n  Input:       " + input
                            + "\n  Stringified: " + stringified)
                    .isEqualTo(first);
        }

        @Test
        void shouldRoundTripLiteral() {
            assertRoundTrips("42");
        }

        @Test
        void shouldRoundTripRef() {
            assertRoundTrips("Population");
        }

        @Test
        void shouldRoundTripQuotedRef() {
            assertRoundTrips("`Tasks Remaining`");
        }

        @Test
        void shouldRoundTripBinaryOps() {
            assertRoundTrips("a + b * c");
        }

        @Test
        void shouldRoundTripNestedParens() {
            assertRoundTrips("(a + b) * (c - d)");
        }

        @Test
        void shouldRoundTripUnary() {
            assertRoundTrips("-x");
        }

        @Test
        void shouldRoundTripFunction() {
            assertRoundTrips("SMOOTH(input, 5, 10)");
        }

        @Test
        void shouldRoundTripConditional() {
            assertRoundTrips("IF(x > 0, x, -x)");
        }

        @Test
        void shouldRoundTripComplexFormula() {
            assertRoundTrips(
                    "Contact_Rate * Infectious / (Susceptible + Infectious + Recovered) * Infectivity * Susceptible");
        }

        @Test
        void shouldRoundTripPower() {
            assertRoundTrips("a ** b ** c");
        }

        @Test
        void shouldRoundTripLeftAssociativePower() {
            // (a ** b) ** c must preserve parens through round-trip
            assertRoundTrips("(a ** b) ** c");
        }
    }

    @Nested
    @DisplayName("Subscript bracket notation")
    class SubscriptBrackets {

        @Test
        void shouldParseSimpleBracketRef() {
            Expr result = ExprParser.parse("Population[North]");
            assertThat(result).isInstanceOf(Expr.Ref.class);
            assertThat(((Expr.Ref) result).name()).isEqualTo("Population[North]");
        }

        @Test
        void shouldParseBracketRefInExpression() {
            Expr result = ExprParser.parse("Population[North] * rate");
            assertThat(result).isInstanceOf(Expr.BinaryOp.class);
            Expr.BinaryOp bin = (Expr.BinaryOp) result;
            assertThat(((Expr.Ref) bin.left()).name()).isEqualTo("Population[North]");
            assertThat(((Expr.Ref) bin.right()).name()).isEqualTo("rate");
        }

        @Test
        void shouldParseMultipleBracketRefs() {
            Expr result = ExprParser.parse("Pop[A] + Pop[B]");
            assertThat(result).isInstanceOf(Expr.BinaryOp.class);
            Expr.BinaryOp bin = (Expr.BinaryOp) result;
            assertThat(((Expr.Ref) bin.left()).name()).isEqualTo("Pop[A]");
            assertThat(((Expr.Ref) bin.right()).name()).isEqualTo("Pop[B]");
        }

        @Test
        void shouldThrowOnUnterminatedBracket() {
            assertThatThrownBy(() -> ExprParser.parse("Pop[North"))
                    .isInstanceOf(ParseException.class)
                    .hasMessageContaining("Unterminated");
        }

        @Test
        void shouldThrowOnEmptyBracketLabel() {
            assertThatThrownBy(() -> ExprParser.parse("Pop[]"))
                    .isInstanceOf(ParseException.class)
                    .hasMessageContaining("Empty subscript");
        }
    }

    @Nested
    @DisplayName("Depth limits")
    class DepthLimits {

        @Test
        void shouldRejectDeeplyNestedUnaryNegationChain() {
            // 250 negations should exceed the MAX_DEPTH of 200
            String expr = "-".repeat(250) + "x";
            assertThatThrownBy(() -> ExprParser.parse(expr))
                    .isInstanceOf(ParseException.class)
                    .hasMessageContaining("too deep");
        }

        @Test
        void shouldRejectDeeplyNestedUnaryPlusChain() {
            // 250 unary plus signs should also exceed MAX_DEPTH of 200
            String expr = "+".repeat(250) + "x";
            assertThatThrownBy(() -> ExprParser.parse(expr))
                    .isInstanceOf(ParseException.class)
                    .hasMessageContaining("too deep");
        }

        @Test
        void shouldAllowModerateUnaryPlusChain() {
            // A chain within the depth limit should parse successfully
            String expr = "+".repeat(10) + "42";
            Expr result = ExprParser.parse(expr);
            assertThat(result).isInstanceOf(Expr.Literal.class);
            assertThat(((Expr.Literal) result).value()).isEqualTo(42);
        }
    }
}
