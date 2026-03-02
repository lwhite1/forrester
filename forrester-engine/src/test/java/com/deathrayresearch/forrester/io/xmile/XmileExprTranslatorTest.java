package com.deathrayresearch.forrester.io.xmile;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("XmileExprTranslator")
class XmileExprTranslatorTest {

    @Nested
    @DisplayName("toForrester")
    class ToForrester {

        @Test
        void shouldTranslateIfThenElse() {
            var result = XmileExprTranslator.toForrester("IF_THEN_ELSE(x > 0, 1, 0)");
            assertThat(result.expression()).isEqualTo("IF(x > 0, 1, 0)");
        }

        @Test
        void shouldTranslateIfThenElseWithSpaces() {
            var result = XmileExprTranslator.toForrester("IF THEN ELSE(x > 0, 1, 0)");
            assertThat(result.expression()).isEqualTo("IF(x > 0, 1, 0)");
        }

        @Test
        void shouldTranslateAndKeyword() {
            var result = XmileExprTranslator.toForrester("x > 0 AND y > 0");
            assertThat(result.expression()).isEqualTo("x > 0 && y > 0");
        }

        @Test
        void shouldTranslateOrKeyword() {
            var result = XmileExprTranslator.toForrester("x > 0 OR y > 0");
            assertThat(result.expression()).isEqualTo("x > 0 || y > 0");
        }

        @Test
        void shouldTranslateNotKeyword() {
            var result = XmileExprTranslator.toForrester("NOT x > 0");
            assertThat(result.expression()).startsWith("!");
            assertThat(result.expression()).contains("x > 0");
        }

        @Test
        void shouldTranslateEqualityOperator() {
            var result = XmileExprTranslator.toForrester("x = 5");
            assertThat(result.expression()).isEqualTo("x == 5");
        }

        @Test
        void shouldTranslateInequalityOperator() {
            var result = XmileExprTranslator.toForrester("x <> 5");
            assertThat(result.expression()).isEqualTo("x != 5");
        }

        @Test
        void shouldTranslateTimeVariable() {
            var result = XmileExprTranslator.toForrester("Time * 2");
            assertThat(result.expression()).isEqualTo("TIME * 2");
        }

        @Test
        void shouldTranslateSmth3ToSmooth() {
            var result = XmileExprTranslator.toForrester("SMTH3(input, 5)");
            assertThat(result.expression()).isEqualTo("SMOOTH(input, 5)");
            assertThat(result.warnings()).anyMatch(w -> w.contains("SMTH3"));
        }

        @Test
        void shouldTranslateSmth1ToSmooth() {
            var result = XmileExprTranslator.toForrester("SMTH1(input, 5)");
            assertThat(result.expression()).isEqualTo("SMOOTH(input, 5)");
            assertThat(result.warnings()).anyMatch(w -> w.contains("SMTH1"));
        }

        @Test
        void shouldPassThroughStandardFunctions() {
            var result = XmileExprTranslator.toForrester("EXP(x) + LN(y) + ABS(z)");
            assertThat(result.expression()).isEqualTo("EXP(x) + LN(y) + ABS(z)");
            assertThat(result.warnings()).isEmpty();
        }

        @Test
        void shouldPassThroughMinMax() {
            var result = XmileExprTranslator.toForrester("MIN(a, b) + MAX(c, d)");
            assertThat(result.expression()).isEqualTo("MIN(a, b) + MAX(c, d)");
        }

        @Test
        void shouldPassThroughDelay3() {
            var result = XmileExprTranslator.toForrester("DELAY3(input, 5)");
            assertThat(result.expression()).isEqualTo("DELAY3(input, 5)");
        }

        @Test
        void shouldHandleNull() {
            var result = XmileExprTranslator.toForrester(null);
            assertThat(result.expression()).isNull();
        }

        @Test
        void shouldHandleBlank() {
            var result = XmileExprTranslator.toForrester("  ");
            assertThat(result.expression()).isEqualTo("  ");
        }

        @Test
        void shouldNotDoubleTranslateEqualityInGte() {
            // >= should remain >=, not become >==
            var result = XmileExprTranslator.toForrester("x >= 5");
            assertThat(result.expression()).isEqualTo("x >= 5");
        }

        @Test
        void shouldNotDoubleTranslateEqualityInLte() {
            // <= should remain <=, not become <==
            var result = XmileExprTranslator.toForrester("x <= 5");
            assertThat(result.expression()).isEqualTo("x <= 5");
        }

        @Test
        void shouldHandleCombinedExpression() {
            var result = XmileExprTranslator.toForrester(
                    "IF_THEN_ELSE(x = 5 AND y <> 3, Time, 0)");
            assertThat(result.expression()).isEqualTo(
                    "IF(x == 5 && y != 3, TIME, 0)");
        }
    }

    @Nested
    @DisplayName("toXmile")
    class ToXmile {

        @Test
        void shouldTranslateIfFunction() {
            String result = XmileExprTranslator.toXmile("IF(x > 0, 1, 0)");
            assertThat(result).isEqualTo("IF_THEN_ELSE(x > 0, 1, 0)");
        }

        @Test
        void shouldTranslateAndOperator() {
            String result = XmileExprTranslator.toXmile("x > 0 && y > 0");
            assertThat(result).isEqualTo("x > 0  AND  y > 0");
        }

        @Test
        void shouldTranslateOrOperator() {
            String result = XmileExprTranslator.toXmile("x > 0 || y > 0");
            assertThat(result).isEqualTo("x > 0  OR  y > 0");
        }

        @Test
        void shouldTranslateNotOperator() {
            String result = XmileExprTranslator.toXmile("!(x > 0)");
            assertThat(result).isEqualTo("NOT (x > 0)");
        }

        @Test
        void shouldTranslateDoubleEquals() {
            String result = XmileExprTranslator.toXmile("x == 5");
            assertThat(result).isEqualTo("x = 5");
        }

        @Test
        void shouldTranslateNotEquals() {
            String result = XmileExprTranslator.toXmile("x != 5");
            assertThat(result).isEqualTo("x <> 5");
        }

        @Test
        void shouldTranslateTimeVariable() {
            String result = XmileExprTranslator.toXmile("TIME * 2");
            assertThat(result).isEqualTo("Time * 2");
        }

        @Test
        void shouldHandleNull() {
            assertThat(XmileExprTranslator.toXmile(null)).isNull();
        }

        @Test
        void shouldHandleBlank() {
            assertThat(XmileExprTranslator.toXmile("  ")).isEqualTo("  ");
        }

        @Test
        void shouldNotTranslateNotEqualsToNotXmile() {
            // != should become <>, not NOT=
            String result = XmileExprTranslator.toXmile("x != 5");
            assertThat(result).doesNotContain("NOT");
            assertThat(result).contains("<>");
        }
    }

    @Nested
    @DisplayName("Round-trip")
    class RoundTrip {

        @Test
        void shouldRoundTripSimpleArithmetic() {
            String original = "a + b * c";
            var imported = XmileExprTranslator.toForrester(original);
            String exported = XmileExprTranslator.toXmile(imported.expression());
            assertThat(exported).isEqualTo(original);
        }

        @Test
        void shouldRoundTripIfExpression() {
            String xmile = "IF_THEN_ELSE(x > 0, 1, 0)";
            var imported = XmileExprTranslator.toForrester(xmile);
            assertThat(imported.expression()).isEqualTo("IF(x > 0, 1, 0)");
            String exported = XmileExprTranslator.toXmile(imported.expression());
            assertThat(exported).isEqualTo(xmile);
        }
    }
}
