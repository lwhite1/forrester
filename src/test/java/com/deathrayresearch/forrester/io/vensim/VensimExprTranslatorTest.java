package com.deathrayresearch.forrester.io.vensim;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("VensimExprTranslator")
class VensimExprTranslatorTest {

    private static final Set<String> EMPTY_NAMES = Set.of();

    @Nested
    @DisplayName("Name normalization")
    class NameNormalization {

        @Test
        void shouldReplaceSpacesWithUnderscores() {
            assertThat(VensimExprTranslator.normalizeName("Contact Rate")).isEqualTo("Contact_Rate");
        }

        @Test
        void shouldTrimWhitespace() {
            assertThat(VensimExprTranslator.normalizeName("  x  ")).isEqualTo("x");
        }

        @Test
        void shouldHandleMultipleSpaces() {
            assertThat(VensimExprTranslator.normalizeName("My Long Variable Name"))
                    .isEqualTo("My_Long_Variable_Name");
        }

        @Test
        void shouldRemoveSpecialCharacters() {
            assertThat(VensimExprTranslator.normalizeName("Rate$#")).isEqualTo("Rate");
        }

        @Test
        void shouldHandleEmptyString() {
            assertThat(VensimExprTranslator.normalizeName("")).isEmpty();
        }

        @Test
        void shouldHandleNull() {
            assertThat(VensimExprTranslator.normalizeName(null)).isEmpty();
        }

        @Test
        void shouldPrefixDigitStartingName() {
            assertThat(VensimExprTranslator.normalizeName("123abc")).isEqualTo("_123abc");
        }
    }

    @Nested
    @DisplayName("IF THEN ELSE translation")
    class IfThenElse {

        @Test
        void shouldTranslateIfThenElse() {
            var result = VensimExprTranslator.translate(
                    "IF THEN ELSE(x > 0, x, 0)", "var", EMPTY_NAMES);
            assertThat(result.expression()).isEqualTo("IF(x > 0, x, 0)");
        }

        @Test
        void shouldBeCase_insensitive() {
            var result = VensimExprTranslator.translate(
                    "if then else(x > 0, x, 0)", "var", EMPTY_NAMES);
            assertThat(result.expression()).isEqualTo("IF(x > 0, x, 0)");
        }

        @Test
        void shouldHandleNestedIfThenElse() {
            var result = VensimExprTranslator.translate(
                    "IF THEN ELSE(a > 0, IF THEN ELSE(b > 0, 1, 2), 3)",
                    "var", EMPTY_NAMES);
            assertThat(result.expression()).isEqualTo("IF(a > 0, IF(b > 0, 1, 2), 3)");
        }
    }

    @Nested
    @DisplayName("Logical operator translation")
    class LogicalOperators {

        @Test
        void shouldTranslateAnd() {
            var result = VensimExprTranslator.translate("x > 0 :AND: y > 0", "var", EMPTY_NAMES);
            assertThat(result.expression()).isEqualTo("x > 0 && y > 0");
        }

        @Test
        void shouldTranslateOr() {
            var result = VensimExprTranslator.translate("x > 0 :OR: y > 0", "var", EMPTY_NAMES);
            assertThat(result.expression()).isEqualTo("x > 0 || y > 0");
        }

        @Test
        void shouldTranslateNot() {
            var result = VensimExprTranslator.translate(":NOT: x > 0", "var", EMPTY_NAMES);
            assertThat(result.expression()).isEqualTo("! x > 0");
        }
    }

    @Nested
    @DisplayName("XIDZ and ZIDZ translation")
    class DivisionFunctions {

        @Test
        void shouldTranslateXidz() {
            var result = VensimExprTranslator.translate("XIDZ(a, b, c)", "var", EMPTY_NAMES);
            assertThat(result.expression()).isEqualTo("IF(b == 0, c, a / b)");
        }

        @Test
        void shouldTranslateZidz() {
            var result = VensimExprTranslator.translate("ZIDZ(a, b)", "var", EMPTY_NAMES);
            assertThat(result.expression()).isEqualTo("IF(b == 0, 0, a / b)");
        }

        @Test
        void shouldHandleNestedXidz() {
            var result = VensimExprTranslator.translate(
                    "XIDZ(XIDZ(a, b, 0), c, 1)", "var", EMPTY_NAMES);
            assertThat(result.expression()).isEqualTo("IF(c == 0, 1, IF(b == 0, 0, a / b) / c)");
        }
    }

    @Nested
    @DisplayName("Multi-word name replacement")
    class NameReplacement {

        @Test
        void shouldReplaceMultiWordNames() {
            Set<String> names = Set.of("Contact Rate", "Total Population");
            var result = VensimExprTranslator.translate(
                    "Contact Rate * Total Population", "var", names);
            assertThat(result.expression()).isEqualTo("Contact_Rate * Total_Population");
        }

        @Test
        void shouldReplaceLongestNamesFirst() {
            Set<String> names = Set.of("Rate", "Contact Rate");
            var result = VensimExprTranslator.translate(
                    "Contact Rate * 2", "var", names);
            assertThat(result.expression()).isEqualTo("Contact_Rate * 2");
        }

        @Test
        void shouldNotReplacePartialMatches() {
            Set<String> names = Set.of("Contact Rate");
            var result = VensimExprTranslator.translate(
                    "MyContact Rate2", "var", names);
            // Should not match because of surrounding alphanumeric chars
            assertThat(result.expression()).isEqualTo("MyContact Rate2");
        }
    }

    @Nested
    @DisplayName("Time variable translation")
    class TimeVariable {

        @Test
        void shouldTranslateTimeToTIME() {
            var result = VensimExprTranslator.translate("Time + 1", "var", EMPTY_NAMES);
            assertThat(result.expression()).isEqualTo("TIME + 1");
        }

        @Test
        void shouldNotTranslateTimeInsideOtherWords() {
            var result = VensimExprTranslator.translate("TimeStep + Overtime", "var", EMPTY_NAMES);
            // "Time" at start of "TimeStep" should be replaced by word boundary,
            // but "Overtime" should not be affected
            assertThat(result.expression()).doesNotContain("OverTIME");
        }
    }

    @Nested
    @DisplayName("WITH LOOKUP translation")
    class WithLookup {

        @Test
        void shouldExtractLookupFromWithLookup() {
            String expr = "WITH LOOKUP(Time, ([(0,0)-(100,10)],(0,0),(50,5),(100,10)))";
            var result = VensimExprTranslator.translate(expr, "my_var", EMPTY_NAMES);

            assertThat(result.expression()).contains("LOOKUP(my_var_lookup,");
            assertThat(result.lookups()).hasSize(1);
            assertThat(result.lookups().get(0).name()).isEqualTo("my_var_lookup");
            assertThat(result.lookups().get(0).xValues()).hasSizeGreaterThanOrEqualTo(2);
        }
    }

    @Nested
    @DisplayName("Function approximations")
    class FunctionApproximations {

        @Test
        void shouldTranslateSmooth3ToSmooth() {
            var result = VensimExprTranslator.translate("SMOOTH3(input, 5)", "var", EMPTY_NAMES);
            assertThat(result.expression()).isEqualTo("SMOOTH(input, 5)");
            assertThat(result.warnings()).anyMatch(w -> w.contains("SMOOTH3"));
        }

        @Test
        void shouldTranslateDelay1ToDelay3() {
            var result = VensimExprTranslator.translate("DELAY1(input, 3)", "var", EMPTY_NAMES);
            assertThat(result.expression()).isEqualTo("DELAY3(input, 3)");
            assertThat(result.warnings()).anyMatch(w -> w.contains("DELAY1"));
        }
    }

    @Nested
    @DisplayName("Unsupported functions")
    class UnsupportedFunctions {

        @Test
        void shouldWarnOnPulse() {
            var result = VensimExprTranslator.translate("PULSE(10, 5)", "var", EMPTY_NAMES);
            assertThat(result.warnings()).anyMatch(w -> w.contains("PULSE"));
        }

        @Test
        void shouldWarnOnGame() {
            var result = VensimExprTranslator.translate("GAME(x)", "var", EMPTY_NAMES);
            assertThat(result.warnings()).anyMatch(w -> w.contains("GAME"));
        }
    }

    @Nested
    @DisplayName("Pass-through functions")
    class PassThroughFunctions {

        @Test
        void shouldNotChangeExp() {
            var result = VensimExprTranslator.translate("EXP(x)", "var", EMPTY_NAMES);
            assertThat(result.expression()).isEqualTo("EXP(x)");
        }

        @Test
        void shouldNotChangeLn() {
            var result = VensimExprTranslator.translate("LN(x)", "var", EMPTY_NAMES);
            assertThat(result.expression()).isEqualTo("LN(x)");
        }

        @Test
        void shouldNotChangeSqrt() {
            var result = VensimExprTranslator.translate("SQRT(x)", "var", EMPTY_NAMES);
            assertThat(result.expression()).isEqualTo("SQRT(x)");
        }

        @Test
        void shouldNotChangeMinMax() {
            var result = VensimExprTranslator.translate("MIN(a, MAX(b, c))", "var", EMPTY_NAMES);
            assertThat(result.expression()).isEqualTo("MIN(a, MAX(b, c))");
        }
    }

    @Nested
    @DisplayName("Edge cases")
    class EdgeCases {

        @Test
        void shouldHandleNullExpression() {
            var result = VensimExprTranslator.translate(null, "var", EMPTY_NAMES);
            assertThat(result.expression()).isNull();
        }

        @Test
        void shouldHandleBlankExpression() {
            var result = VensimExprTranslator.translate("   ", "var", EMPTY_NAMES);
            assertThat(result.expression()).isEqualTo("   ");
        }

        @Test
        void shouldHandleComplexExpression() {
            Set<String> names = Set.of("Contact Rate", "Total Population");
            var result = VensimExprTranslator.translate(
                    "IF THEN ELSE(Contact Rate > 0 :AND: Total Population > 100, "
                            + "ZIDZ(Contact Rate, Total Population), 0)",
                    "var", names);
            assertThat(result.expression()).contains("IF(");
            assertThat(result.expression()).contains("Contact_Rate");
            assertThat(result.expression()).contains("Total_Population");
            assertThat(result.expression()).contains("&&");
        }
    }
}
