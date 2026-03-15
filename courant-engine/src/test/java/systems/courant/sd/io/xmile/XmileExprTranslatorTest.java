package systems.courant.sd.io.xmile;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("XmileExprTranslator")
class XmileExprTranslatorTest {

    @Nested
    @DisplayName("toCourant")
    class ToCourant {

        @Test
        void shouldTranslateIfThenElse() {
            var result = XmileExprTranslator.toCourant("IF_THEN_ELSE(x > 0, 1, 0)");
            assertThat(result.expression()).isEqualTo("IF(x > 0, 1, 0)");
        }

        @Test
        void shouldTranslateIfThenElseWithSpaces() {
            var result = XmileExprTranslator.toCourant("IF THEN ELSE(x > 0, 1, 0)");
            assertThat(result.expression()).isEqualTo("IF(x > 0, 1, 0)");
        }

        @Test
        void shouldTranslateAndKeyword() {
            var result = XmileExprTranslator.toCourant("x > 0 AND y > 0");
            assertThat(result.expression()).isEqualTo("x > 0 and y > 0");
        }

        @Test
        void shouldTranslateOrKeyword() {
            var result = XmileExprTranslator.toCourant("x > 0 OR y > 0");
            assertThat(result.expression()).isEqualTo("x > 0 or y > 0");
        }

        @Test
        void shouldTranslateNotKeyword() {
            var result = XmileExprTranslator.toCourant("NOT x > 0");
            assertThat(result.expression()).startsWith("not");
            assertThat(result.expression()).contains("x > 0");
        }

        @Test
        void shouldTranslateEqualityOperator() {
            var result = XmileExprTranslator.toCourant("x = 5");
            assertThat(result.expression()).isEqualTo("x == 5");
        }

        @Test
        void shouldTranslateInequalityOperator() {
            var result = XmileExprTranslator.toCourant("x <> 5");
            assertThat(result.expression()).isEqualTo("x != 5");
        }

        @Test
        void shouldTranslateTimeVariable() {
            var result = XmileExprTranslator.toCourant("Time * 2");
            assertThat(result.expression()).isEqualTo("TIME * 2");
        }

        @Test
        void shouldTranslateTimeInParens() {
            var result = XmileExprTranslator.toCourant("ABS(Time)");
            assertThat(result.expression()).isEqualTo("ABS(TIME)");
        }

        @Test
        void shouldNotTranslateTimeInCompoundNameWithSpaceBefore() {
            var result = XmileExprTranslator.toCourant("Processing Time * 2");
            assertThat(result.expression()).isEqualTo("Processing Time * 2");
        }

        @Test
        void shouldNotTranslateTimeInCompoundNameWithSpaceAfter() {
            var result = XmileExprTranslator.toCourant("Time Constant + 1");
            assertThat(result.expression()).isEqualTo("Time Constant + 1");
        }

        @Test
        void shouldTranslateStandaloneTimeCaseInsensitive() {
            var result = XmileExprTranslator.toCourant("time + 1");
            assertThat(result.expression()).isEqualTo("TIME + 1");
        }

        @Test
        void shouldTranslateCaretToDoubleStar() {
            var result = XmileExprTranslator.toCourant("x^2 + y^3");
            assertThat(result.expression()).isEqualTo("x**2 + y**3");
        }

        @Test
        void shouldTranslateSmth3ToSmooth3() {
            var result = XmileExprTranslator.toCourant("SMTH3(input, 5)");
            assertThat(result.expression()).isEqualTo("SMOOTH3(input, 5)");
            assertThat(result.warnings()).noneMatch(w -> w.contains("SMTH3"));
        }

        @Test
        void shouldTranslateSmth1ToSmooth() {
            var result = XmileExprTranslator.toCourant("SMTH1(input, 5)");
            assertThat(result.expression()).isEqualTo("SMOOTH(input, 5)");
            assertThat(result.warnings()).noneMatch(w -> w.contains("SMTH1"));
        }

        @Test
        void shouldPassThroughStandardFunctions() {
            var result = XmileExprTranslator.toCourant("EXP(x) + LN(y) + ABS(z)");
            assertThat(result.expression()).isEqualTo("EXP(x) + LN(y) + ABS(z)");
            assertThat(result.warnings()).isEmpty();
        }

        @Test
        void shouldPassThroughMinMax() {
            var result = XmileExprTranslator.toCourant("MIN(a, b) + MAX(c, d)");
            assertThat(result.expression()).isEqualTo("MIN(a, b) + MAX(c, d)");
        }

        @Test
        void shouldPassThroughDelay3() {
            var result = XmileExprTranslator.toCourant("DELAY3(input, 5)");
            assertThat(result.expression()).isEqualTo("DELAY3(input, 5)");
        }

        @Test
        void shouldHandleNull() {
            var result = XmileExprTranslator.toCourant(null);
            assertThat(result.expression()).isNull();
        }

        @Test
        void shouldHandleBlank() {
            var result = XmileExprTranslator.toCourant("  ");
            assertThat(result.expression()).isEqualTo("  ");
        }

        @Test
        void shouldNotDoubleTranslateEqualityInGte() {
            // >= should remain >=, not become >==
            var result = XmileExprTranslator.toCourant("x >= 5");
            assertThat(result.expression()).isEqualTo("x >= 5");
        }

        @Test
        void shouldNotDoubleTranslateEqualityInLte() {
            // <= should remain <=, not become <==
            var result = XmileExprTranslator.toCourant("x <= 5");
            assertThat(result.expression()).isEqualTo("x <= 5");
        }

        @Test
        void shouldWarnAboutSafediv() {
            var result = XmileExprTranslator.toCourant("SAFEDIV(a, b, 0)");
            assertThat(result.warnings()).anyMatch(w -> w.contains("SAFEDIV"));
            assertThat(result.expression()).isEqualTo("SAFEDIV(a, b, 0)");
        }

        @Test
        void shouldWarnAboutInit() {
            var result = XmileExprTranslator.toCourant("INIT(Population)");
            assertThat(result.warnings()).anyMatch(w -> w.contains("INIT"));
        }

        @Test
        void shouldWarnAboutPrevious() {
            var result = XmileExprTranslator.toCourant("PREVIOUS(Stock, 0)");
            assertThat(result.warnings()).anyMatch(w -> w.contains("PREVIOUS"));
        }

        @Test
        void shouldWarnAboutHistory() {
            var result = XmileExprTranslator.toCourant("HISTORY(Stock, TIME - 5)");
            assertThat(result.warnings()).anyMatch(w -> w.contains("HISTORY"));
        }

        @Test
        void shouldHandleCombinedExpression() {
            var result = XmileExprTranslator.toCourant(
                    "IF_THEN_ELSE(x = 5 AND y <> 3, Time, 0)");
            assertThat(result.expression()).isEqualTo(
                    "IF(x == 5 and y != 3, TIME, 0)");
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
            String result = XmileExprTranslator.toXmile("x > 0 and y > 0");
            assertThat(result).isEqualTo("x > 0 AND y > 0");
        }

        @Test
        void shouldTranslateOrOperator() {
            String result = XmileExprTranslator.toXmile("x > 0 or y > 0");
            assertThat(result).isEqualTo("x > 0 OR y > 0");
        }

        @Test
        void shouldTranslateNotOperator() {
            String result = XmileExprTranslator.toXmile("not(x > 0)");
            assertThat(result).isEqualTo("NOT(x > 0)");
        }

        @Test
        void shouldTranslateDoubleStarToCaret() {
            String result = XmileExprTranslator.toXmile("x**2 + y**3");
            assertThat(result).isEqualTo("x^2 + y^3");
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
        void shouldTranslateUppercaseAndOperator() {
            String result = XmileExprTranslator.toXmile("x > 0 AND y > 0");
            assertThat(result).isEqualTo("x > 0 AND y > 0");
        }

        @Test
        void shouldTranslateMixedCaseAndOperator() {
            String result = XmileExprTranslator.toXmile("x > 0 And y > 0");
            assertThat(result).isEqualTo("x > 0 AND y > 0");
        }

        @Test
        void shouldTranslateUppercaseOrOperator() {
            String result = XmileExprTranslator.toXmile("x > 0 OR y > 0");
            assertThat(result).isEqualTo("x > 0 OR y > 0");
        }

        @Test
        void shouldTranslateMixedCaseOrOperator() {
            String result = XmileExprTranslator.toXmile("x > 0 Or y > 0");
            assertThat(result).isEqualTo("x > 0 OR y > 0");
        }

        @Test
        void shouldTranslateUppercaseNotOperator() {
            String result = XmileExprTranslator.toXmile("NOT(x > 0)");
            assertThat(result).isEqualTo("NOT(x > 0)");
        }

        @Test
        void shouldTranslateMixedCaseNotOperator() {
            String result = XmileExprTranslator.toXmile("Not(x > 0)");
            assertThat(result).isEqualTo("NOT(x > 0)");
        }

        @Test
        void shouldNotTranslateNotEqualsToNotXmile() {
            // != should become <>, the "not" inside "!=" should not be affected
            String result = XmileExprTranslator.toXmile("x != 5");
            assertThat(result).contains("<>");
        }
    }

    @Nested
    @DisplayName("Round-trip")
    class RoundTrip {

        @Test
        void shouldRoundTripSimpleArithmetic() {
            String original = "a + b * c";
            var imported = XmileExprTranslator.toCourant(original);
            String exported = XmileExprTranslator.toXmile(imported.expression());
            assertThat(exported).isEqualTo(original);
        }

        @Test
        void shouldRoundTripIfExpression() {
            String xmile = "IF_THEN_ELSE(x > 0, 1, 0)";
            var imported = XmileExprTranslator.toCourant(xmile);
            assertThat(imported.expression()).isEqualTo("IF(x > 0, 1, 0)");
            String exported = XmileExprTranslator.toXmile(imported.expression());
            assertThat(exported).isEqualTo(xmile);
        }
    }
}
