package com.deathrayresearch.forrester.model.expr;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("ExprParser")
class ExprParserTest {

    @Nested
    @DisplayName("Literals")
    class Literals {

        @Test
        void shouldParseInteger() {
            Expr result = ExprParser.parse("42");
            assertInstanceOf(Expr.Literal.class, result);
            assertEquals(42.0, ((Expr.Literal) result).value());
        }

        @Test
        void shouldParseDecimal() {
            Expr result = ExprParser.parse("3.14");
            assertInstanceOf(Expr.Literal.class, result);
            assertEquals(3.14, ((Expr.Literal) result).value(), 1e-10);
        }

        @Test
        void shouldParseScientificNotation() {
            Expr result = ExprParser.parse("1.5e3");
            assertInstanceOf(Expr.Literal.class, result);
            assertEquals(1500.0, ((Expr.Literal) result).value());
        }

        @Test
        void shouldParseNegativeExponent() {
            Expr result = ExprParser.parse("2.5E-4");
            assertInstanceOf(Expr.Literal.class, result);
            assertEquals(2.5e-4, ((Expr.Literal) result).value(), 1e-15);
        }

        @Test
        void shouldParseLeadingDecimalPoint() {
            Expr result = ExprParser.parse(".5");
            assertInstanceOf(Expr.Literal.class, result);
            assertEquals(0.5, ((Expr.Literal) result).value());
        }

        @Test
        void shouldParseZero() {
            Expr result = ExprParser.parse("0");
            assertEquals(new Expr.Literal(0.0), result);
        }
    }

    @Nested
    @DisplayName("References")
    class References {

        @Test
        void shouldParseSimpleIdentifier() {
            Expr result = ExprParser.parse("Population");
            assertEquals(new Expr.Ref("Population"), result);
        }

        @Test
        void shouldParseIdentifierWithUnderscore() {
            Expr result = ExprParser.parse("Birth_Rate");
            assertEquals(new Expr.Ref("Birth_Rate"), result);
        }

        @Test
        void shouldParseQuotedIdentifier() {
            Expr result = ExprParser.parse("`Tasks Remaining`");
            assertEquals(new Expr.Ref("Tasks Remaining"), result);
        }

        @Test
        void shouldParseQuotedIdentifierWithSpecialChars() {
            Expr result = ExprParser.parse("`Coffee Temperature`");
            assertEquals(new Expr.Ref("Coffee Temperature"), result);
        }
    }

    @Nested
    @DisplayName("Binary operations")
    class BinaryOperations {

        @Test
        void shouldParseAddition() {
            Expr result = ExprParser.parse("a + b");
            assertEquals(
                    new Expr.BinaryOp(new Expr.Ref("a"), BinaryOperator.ADD, new Expr.Ref("b")),
                    result);
        }

        @Test
        void shouldParseSubtraction() {
            Expr result = ExprParser.parse("a - b");
            assertEquals(
                    new Expr.BinaryOp(new Expr.Ref("a"), BinaryOperator.SUB, new Expr.Ref("b")),
                    result);
        }

        @Test
        void shouldParseMultiplication() {
            Expr result = ExprParser.parse("a * b");
            assertEquals(
                    new Expr.BinaryOp(new Expr.Ref("a"), BinaryOperator.MUL, new Expr.Ref("b")),
                    result);
        }

        @Test
        void shouldParseDivision() {
            Expr result = ExprParser.parse("a / b");
            assertEquals(
                    new Expr.BinaryOp(new Expr.Ref("a"), BinaryOperator.DIV, new Expr.Ref("b")),
                    result);
        }

        @Test
        void shouldParseModulo() {
            Expr result = ExprParser.parse("a % b");
            assertEquals(
                    new Expr.BinaryOp(new Expr.Ref("a"), BinaryOperator.MOD, new Expr.Ref("b")),
                    result);
        }

        @Test
        void shouldParsePower() {
            Expr result = ExprParser.parse("a ^ b");
            assertEquals(
                    new Expr.BinaryOp(new Expr.Ref("a"), BinaryOperator.POW, new Expr.Ref("b")),
                    result);
        }

        @Test
        void shouldParseComparisons() {
            assertEquals(BinaryOperator.EQ,
                    ((Expr.BinaryOp) ExprParser.parse("a == b")).operator());
            assertEquals(BinaryOperator.NE,
                    ((Expr.BinaryOp) ExprParser.parse("a != b")).operator());
            assertEquals(BinaryOperator.LT,
                    ((Expr.BinaryOp) ExprParser.parse("a < b")).operator());
            assertEquals(BinaryOperator.LE,
                    ((Expr.BinaryOp) ExprParser.parse("a <= b")).operator());
            assertEquals(BinaryOperator.GT,
                    ((Expr.BinaryOp) ExprParser.parse("a > b")).operator());
            assertEquals(BinaryOperator.GE,
                    ((Expr.BinaryOp) ExprParser.parse("a >= b")).operator());
        }

        @Test
        void shouldParseLogicalOr() {
            Expr result = ExprParser.parse("a || b");
            assertEquals(
                    new Expr.BinaryOp(new Expr.Ref("a"), BinaryOperator.OR, new Expr.Ref("b")),
                    result);
        }

        @Test
        void shouldParseLogicalAnd() {
            Expr result = ExprParser.parse("a && b");
            assertEquals(
                    new Expr.BinaryOp(new Expr.Ref("a"), BinaryOperator.AND, new Expr.Ref("b")),
                    result);
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
            assertEquals(BinaryOperator.ADD, top.operator());
            assertEquals(new Expr.Ref("a"), top.left());
            Expr.BinaryOp right = (Expr.BinaryOp) top.right();
            assertEquals(BinaryOperator.MUL, right.operator());
        }

        @Test
        void shouldRespectParentheses() {
            // (a + b) * c
            Expr result = ExprParser.parse("(a + b) * c");
            Expr.BinaryOp top = (Expr.BinaryOp) result;
            assertEquals(BinaryOperator.MUL, top.operator());
            assertInstanceOf(Expr.BinaryOp.class, top.left());
        }

        @Test
        void shouldRespectPowerRightAssociativity() {
            // a ^ b ^ c  should parse as  a ^ (b ^ c)
            Expr result = ExprParser.parse("a ^ b ^ c");
            Expr.BinaryOp top = (Expr.BinaryOp) result;
            assertEquals(BinaryOperator.POW, top.operator());
            assertEquals(new Expr.Ref("a"), top.left());
            Expr.BinaryOp right = (Expr.BinaryOp) top.right();
            assertEquals(BinaryOperator.POW, right.operator());
            assertEquals(new Expr.Ref("b"), right.left());
            assertEquals(new Expr.Ref("c"), right.right());
        }

        @Test
        void shouldRespectAdditionLeftAssociativity() {
            // a - b + c  should parse as  (a - b) + c
            Expr result = ExprParser.parse("a - b + c");
            Expr.BinaryOp top = (Expr.BinaryOp) result;
            assertEquals(BinaryOperator.ADD, top.operator());
            assertEquals(new Expr.Ref("c"), top.right());
            Expr.BinaryOp left = (Expr.BinaryOp) top.left();
            assertEquals(BinaryOperator.SUB, left.operator());
        }

        @Test
        void shouldHandleComplexPrecedence() {
            // a || b && c == d + e * f
            // Should parse as: a || (b && ((c == (d + (e * f)))))
            Expr result = ExprParser.parse("a || b && c == d + e * f");
            Expr.BinaryOp top = (Expr.BinaryOp) result;
            assertEquals(BinaryOperator.OR, top.operator());
        }
    }

    @Nested
    @DisplayName("Unary operations")
    class UnaryOps {

        @Test
        void shouldParseNegation() {
            Expr result = ExprParser.parse("-x");
            assertEquals(new Expr.UnaryOp(UnaryOperator.NEGATE, new Expr.Ref("x")), result);
        }

        @Test
        void shouldParseNot() {
            Expr result = ExprParser.parse("!flag");
            assertEquals(new Expr.UnaryOp(UnaryOperator.NOT, new Expr.Ref("flag")), result);
        }

        @Test
        void shouldParseDoubleNegation() {
            Expr result = ExprParser.parse("--x");
            Expr.UnaryOp outer = (Expr.UnaryOp) result;
            assertEquals(UnaryOperator.NEGATE, outer.operator());
            Expr.UnaryOp inner = (Expr.UnaryOp) outer.operand();
            assertEquals(UnaryOperator.NEGATE, inner.operator());
            assertEquals(new Expr.Ref("x"), inner.operand());
        }

        @Test
        void shouldParseNegationOfExpression() {
            Expr result = ExprParser.parse("-(a + b)");
            assertInstanceOf(Expr.UnaryOp.class, result);
        }
    }

    @Nested
    @DisplayName("Function calls")
    class FunctionCalls {

        @Test
        void shouldParseSingleArgFunction() {
            Expr result = ExprParser.parse("ABS(x)");
            Expr.FunctionCall call = (Expr.FunctionCall) result;
            assertEquals("ABS", call.name());
            assertEquals(1, call.arguments().size());
            assertEquals(new Expr.Ref("x"), call.arguments().get(0));
        }

        @Test
        void shouldParseMultiArgFunction() {
            Expr result = ExprParser.parse("SMOOTH(input, 5)");
            Expr.FunctionCall call = (Expr.FunctionCall) result;
            assertEquals("SMOOTH", call.name());
            assertEquals(2, call.arguments().size());
            assertEquals(new Expr.Ref("input"), call.arguments().get(0));
            assertEquals(new Expr.Literal(5.0), call.arguments().get(1));
        }

        @Test
        void shouldParseThreeArgFunction() {
            Expr result = ExprParser.parse("SMOOTH(input, 5, 10)");
            Expr.FunctionCall call = (Expr.FunctionCall) result;
            assertEquals(3, call.arguments().size());
        }

        @Test
        void shouldParseNestedFunctionCalls() {
            Expr result = ExprParser.parse("MAX(ABS(x), ABS(y))");
            Expr.FunctionCall call = (Expr.FunctionCall) result;
            assertEquals("MAX", call.name());
            assertEquals(2, call.arguments().size());
            assertInstanceOf(Expr.FunctionCall.class, call.arguments().get(0));
            assertInstanceOf(Expr.FunctionCall.class, call.arguments().get(1));
        }

        @Test
        void shouldParseZeroArgFunctions() {
            Expr result = ExprParser.parse("TIME");
            Expr.FunctionCall call = (Expr.FunctionCall) result;
            assertEquals("TIME", call.name());
            assertTrue(call.arguments().isEmpty());
        }

        @Test
        void shouldParseDT() {
            Expr result = ExprParser.parse("DT");
            Expr.FunctionCall call = (Expr.FunctionCall) result;
            assertEquals("DT", call.name());
            assertTrue(call.arguments().isEmpty());
        }
    }

    @Nested
    @DisplayName("Conditional (IF)")
    class Conditionals {

        @Test
        void shouldParseSimpleIf() {
            Expr result = ExprParser.parse("IF(x > 0, x, 0)");
            assertInstanceOf(Expr.Conditional.class, result);
            Expr.Conditional cond = (Expr.Conditional) result;
            assertInstanceOf(Expr.BinaryOp.class, cond.condition());
            assertEquals(new Expr.Ref("x"), cond.thenExpr());
            assertEquals(new Expr.Literal(0.0), cond.elseExpr());
        }

        @Test
        void shouldParseNestedIf() {
            Expr result = ExprParser.parse("IF(a > b, IF(c > d, 1, 2), 3)");
            Expr.Conditional outer = (Expr.Conditional) result;
            assertInstanceOf(Expr.Conditional.class, outer.thenExpr());
        }
    }

    @Nested
    @DisplayName("Complex expressions")
    class ComplexExpressions {

        @Test
        void shouldParseSIRInfectionFormula() {
            String formula = "Contact_Rate * Infectious / (Susceptible + Infectious + Recovered) * Infectivity * Susceptible";
            Expr result = ExprParser.parse(formula);
            assertNotNull(result);
        }

        @Test
        void shouldParseFormulaWithQuotedIds() {
            String formula = "`Contact Rate` * `Infectious` / (`Susceptible` + `Infectious` + `Recovered`)";
            Expr result = ExprParser.parse(formula);
            assertNotNull(result);
        }

        @Test
        void shouldParseExpressionWithMixedOps() {
            Expr result = ExprParser.parse("a + b * c - d / e ^ f");
            assertNotNull(result);
        }
    }

    @Nested
    @DisplayName("Error cases")
    class ErrorCases {

        @Test
        void shouldThrowOnEmptyInput() {
            assertThrows(ParseException.class, () -> ExprParser.parse(""));
        }

        @Test
        void shouldThrowOnNullInput() {
            assertThrows(ParseException.class, () -> ExprParser.parse(null));
        }

        @Test
        void shouldThrowOnBlankInput() {
            assertThrows(ParseException.class, () -> ExprParser.parse("   "));
        }

        @Test
        void shouldThrowOnUnterminatedQuote() {
            assertThrows(ParseException.class, () -> ExprParser.parse("`unterminated"));
        }

        @Test
        void shouldThrowOnMissingOperand() {
            assertThrows(ParseException.class, () -> ExprParser.parse("a +"));
        }

        @Test
        void shouldThrowOnUnmatchedParen() {
            assertThrows(ParseException.class, () -> ExprParser.parse("(a + b"));
        }

        @Test
        void shouldThrowOnExtraCloseParen() {
            assertThrows(ParseException.class, () -> ExprParser.parse("a + b)"));
        }

        @Test
        void shouldThrowOnEmptyQuotedIdentifier() {
            assertThrows(ParseException.class, () -> ExprParser.parse("``"));
        }
    }

    @Nested
    @DisplayName("Round-trip: parse → stringify → parse")
    class RoundTrip {

        private void assertRoundTrips(String input) {
            Expr first = ExprParser.parse(input);
            String stringified = ExprStringifier.stringify(first);
            Expr second = ExprParser.parse(stringified);
            assertEquals(first, second,
                    "Round-trip failed.\n  Input:       " + input
                            + "\n  Stringified: " + stringified);
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
            assertRoundTrips("a ^ b ^ c");
        }
    }
}
