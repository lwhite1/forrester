package systems.courant.shrewd.io.vensim;

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
    @DisplayName("Display name normalization")
    class DisplayNameNormalization {

        @Test
        void shouldPreserveSpaces() {
            assertThat(VensimExprTranslator.normalizeDisplayName("Contact Rate"))
                    .isEqualTo("Contact Rate");
        }

        @Test
        void shouldCollapseMultipleSpaces() {
            assertThat(VensimExprTranslator.normalizeDisplayName("Contact  Rate"))
                    .isEqualTo("Contact Rate");
        }

        @Test
        void shouldTrimWhitespace() {
            assertThat(VensimExprTranslator.normalizeDisplayName("  x  ")).isEqualTo("x");
        }

        @Test
        void shouldPreserveMultipleWords() {
            assertThat(VensimExprTranslator.normalizeDisplayName("My Long Variable Name"))
                    .isEqualTo("My Long Variable Name");
        }

        @Test
        void shouldRemoveSpecialCharacters() {
            assertThat(VensimExprTranslator.normalizeDisplayName("Rate$#")).isEqualTo("Rate");
        }

        @Test
        void shouldStripQuotes() {
            assertThat(VensimExprTranslator.normalizeDisplayName("\"electric vehicles (EV)\""))
                    .isEqualTo("electric vehicles EV");
        }

        @Test
        void shouldHandleEmptyString() {
            assertThat(VensimExprTranslator.normalizeDisplayName("")).isEmpty();
        }

        @Test
        void shouldHandleNull() {
            assertThat(VensimExprTranslator.normalizeDisplayName(null)).isEmpty();
        }

        @Test
        void shouldPrefixDigitStartingName() {
            assertThat(VensimExprTranslator.normalizeDisplayName("123abc")).isEqualTo("_123abc");
        }

        @Test
        void shouldPreserveUnderscores() {
            assertThat(VensimExprTranslator.normalizeDisplayName("my_var")).isEqualTo("my_var");
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
            assertThat(result.expression()).isEqualTo("x > 0  and  y > 0");
        }

        @Test
        void shouldTranslateOr() {
            var result = VensimExprTranslator.translate("x > 0 :OR: y > 0", "var", EMPTY_NAMES);
            assertThat(result.expression()).isEqualTo("x > 0  or  y > 0");
        }

        @Test
        void shouldTranslateNot() {
            var result = VensimExprTranslator.translate(":NOT: x > 0", "var", EMPTY_NAMES);
            assertThat(result.expression()).isEqualTo("not(x > 0)");
        }

        @Test
        void shouldTranslateNotWithAndBoundary() {
            var result = VensimExprTranslator.translate(
                    ":NOT: x > 0 :AND: y < 10", "var", EMPTY_NAMES);
            // :NOT: captures up to the and boundary
            assertThat(result.expression()).startsWith("not(x > 0)");
            assertThat(result.expression()).contains("and");
            assertThat(result.expression()).contains("y < 10");
        }
    }

    @Nested
    @DisplayName("XIDZ and ZIDZ translation")
    class DivisionFunctions {

        @Test
        void shouldTranslateXidz() {
            var result = VensimExprTranslator.translate("XIDZ(a, b, c)", "var", EMPTY_NAMES);
            assertThat(result.expression()).isEqualTo("IF((b) == 0, c, (a) / (b))");
        }

        @Test
        void shouldTranslateZidz() {
            var result = VensimExprTranslator.translate("ZIDZ(a, b)", "var", EMPTY_NAMES);
            assertThat(result.expression()).isEqualTo("IF((b) == 0, 0, (a) / (b))");
        }

        @Test
        void shouldHandleNestedXidz() {
            var result = VensimExprTranslator.translate(
                    "XIDZ(XIDZ(a, b, 0), c, 1)", "var", EMPTY_NAMES);
            assertThat(result.expression()).isEqualTo(
                    "IF((c) == 0, 1, (IF((b) == 0, 0, (a) / (b))) / (c))");
        }

        @Test
        void shouldParenthesizeComplexXidzArgs() {
            var result = VensimExprTranslator.translate(
                    "XIDZ(a + b, c * d, 0)", "var", EMPTY_NAMES);
            assertThat(result.expression()).isEqualTo(
                    "IF((c * d) == 0, 0, (a + b) / (c * d))");
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
            // "Time" inside "TimeStep" and "Overtime" should not be affected due to word boundaries
            assertThat(result.expression()).doesNotContain("OverTIME");
            assertThat(result.expression()).doesNotContain("TIMEStep");
        }

        @Test
        void shouldTranslateLowercaseTime() {
            var result = VensimExprTranslator.translate("time + 1", "var", EMPTY_NAMES);
            assertThat(result.expression()).isEqualTo("TIME + 1");
        }

        @Test
        void shouldNotTranslateTimeWhenItIsAKnownUserVariable() {
            Set<String> knownNames = Set.of("Time");
            var result = VensimExprTranslator.translate("Time + 1", "var", knownNames);
            assertThat(result.expression()).isEqualTo("Time + 1");
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
        void shouldPassThroughSmooth3() {
            var result = VensimExprTranslator.translate("SMOOTH3(input, 5)", "var", EMPTY_NAMES);
            assertThat(result.expression()).isEqualTo("SMOOTH3(input, 5)");
            assertThat(result.warnings()).noneMatch(w -> w.contains("SMOOTH3"));
        }

        @Test
        void shouldPassThroughSmoothI() {
            var result = VensimExprTranslator.translate("SMOOTHI(input, 5, 0)", "var", EMPTY_NAMES);
            assertThat(result.expression()).isEqualTo("SMOOTHI(input, 5, 0)");
            assertThat(result.warnings()).noneMatch(w -> w.contains("SMOOTHI"));
        }

        @Test
        void shouldPassThroughSmooth3I() {
            var result = VensimExprTranslator.translate("SMOOTH3I(input, 5, 0)", "var", EMPTY_NAMES);
            assertThat(result.expression()).isEqualTo("SMOOTH3I(input, 5, 0)");
            assertThat(result.warnings()).noneMatch(w -> w.contains("SMOOTH3I"));
        }

        @Test
        void shouldTranslateDelay1Natively() {
            var result = VensimExprTranslator.translate("DELAY1(input, 3)", "var", EMPTY_NAMES);
            assertThat(result.expression()).isEqualTo("DELAY1(input, 3)");
            assertThat(result.warnings()).noneMatch(w -> w.contains("DELAY1"));
        }

        @Test
        void shouldTranslateDelay1iNatively() {
            var result = VensimExprTranslator.translate("DELAY1I(input, 3, 50)", "var", EMPTY_NAMES);
            assertThat(result.expression()).isEqualTo("DELAY1I(input, 3, 50)");
            assertThat(result.warnings()).noneMatch(w -> w.contains("DELAY1I"));
        }
    }

    @Nested
    @DisplayName("Unsupported functions")
    class UnsupportedFunctions {

        @Test
        void shouldNotWarnOnPulseSinceItIsNowSupported() {
            var result = VensimExprTranslator.translate("PULSE(10, 5)", "var", EMPTY_NAMES);
            assertThat(result.warnings()).noneMatch(w -> w.contains("PULSE"));
        }

        @Test
        void shouldStripGameWrapper() {
            var result = VensimExprTranslator.translate("GAME(x)", "var", EMPTY_NAMES);
            assertThat(result.expression()).isEqualTo("x");
            assertThat(result.warnings()).noneMatch(w -> w.contains("GAME"));
        }

        @Test
        void shouldNotWarnOnVariableNamedPulse() {
            // A variable named "Pulse" (not a function call) should not trigger a warning
            var result = VensimExprTranslator.translate("Pulse * 2", "var", EMPTY_NAMES);
            assertThat(result.warnings()).noneMatch(w -> w.contains("PULSE"));
        }

        @Test
        void shouldNotWarnOnVariableNamedGame() {
            var result = VensimExprTranslator.translate("Game + 1", "var", EMPTY_NAMES);
            assertThat(result.warnings()).noneMatch(w -> w.contains("GAME"));
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
    @DisplayName("Quoted name translation (#419)")
    class QuotedNames {

        @Test
        void shouldTranslateQuotedNameWithDollarSign() {
            var result = VensimExprTranslator.translate(
                    "\"Cost$Per Unit\" * 10", "var", EMPTY_NAMES);
            assertThat(result.expression()).isEqualTo("CostPer_Unit * 10");
        }

        @Test
        void shouldTranslateQuotedNameWithBackslash() {
            var result = VensimExprTranslator.translate(
                    "\"path\\to\\value\" + 1", "var", EMPTY_NAMES);
            assertThat(result.expression()).isEqualTo("pathtovalue + 1");
        }

        @Test
        void shouldTranslateQuotedNameWithParentheses() {
            var result = VensimExprTranslator.translate(
                    "\"electric vehicles (EV)\" * 2", "var", EMPTY_NAMES);
            assertThat(result.expression()).isEqualTo("electric_vehicles_EV * 2");
        }

        @Test
        void shouldTranslateMultipleQuotedNames() {
            var result = VensimExprTranslator.translate(
                    "\"Cost$Per Unit\" + \"Revenue$Per Unit\"", "var", EMPTY_NAMES);
            assertThat(result.expression()).isEqualTo("CostPer_Unit + RevenuePer_Unit");
        }
    }

    @Nested
    @DisplayName("Multi-word function translation")
    class MultiWordFunctions {

        @Test
        void shouldTranslateRandomNormal() {
            var result = VensimExprTranslator.translate(
                    "RANDOM NORMAL(0, 100, 50, 10, 1)", "var", EMPTY_NAMES);
            assertThat(result.expression()).isEqualTo("RANDOM_NORMAL(0, 100, 50, 10, 1)");
        }

        @Test
        void shouldTranslateRandomNormalCaseInsensitive() {
            var result = VensimExprTranslator.translate(
                    "random normal(0, 1, 0.5, 0.1, 0)", "var", EMPTY_NAMES);
            assertThat(result.expression()).isEqualTo("RANDOM_NORMAL(0, 1, 0.5, 0.1, 0)");
        }

        @Test
        void shouldTranslateRandomUniform() {
            var result = VensimExprTranslator.translate(
                    "RANDOM UNIFORM(0, 1, 42)", "var", EMPTY_NAMES);
            assertThat(result.expression()).isEqualTo("RANDOM_UNIFORM(0, 1, 42)");
        }

        @Test
        void shouldTranslatePulseTrain() {
            var result = VensimExprTranslator.translate(
                    "PULSE TRAIN(0, 1, 5, 100)", "var", EMPTY_NAMES);
            assertThat(result.expression()).isEqualTo("PULSE_TRAIN(0, 1, 5, 100)");
        }

        @Test
        void shouldTranslateDelayFixed() {
            var result = VensimExprTranslator.translate(
                    "DELAY FIXED(input, 3, 0)", "var", EMPTY_NAMES);
            assertThat(result.expression()).isEqualTo("DELAY_FIXED(input, 3, 0)");
        }
    }

    @Nested
    @DisplayName("Flat CSV lookup parsing (#490)")
    class FlatCsvLookup {

        @Test
        void shouldParseFlatCsvLookupData() {
            // Format used by BURNOUT.MDL: x values then y values, no parentheses
            var result = VensimExprTranslator.parseLookupPoints(
                    "0,0.2,0.4,0.6,0.8,1,0,0.2,0.4,0.6,0.8,1");
            assertThat(result).isPresent();
            double[][] points = result.get();
            assertThat(points[0]).containsExactly(0, 0.2, 0.4, 0.6, 0.8, 1);
            assertThat(points[1]).containsExactly(0, 0.2, 0.4, 0.6, 0.8, 1);
        }

        @Test
        void shouldParseFlatCsvWithNewlines() {
            // As it appears in .mdl files: x values on one line, y on next
            var result = VensimExprTranslator.parseLookupPoints(
                    "0, 1, 2, 3, 4, 5,\n1.05, 1, 0.9, 0.7, 0.6, 0.55");
            assertThat(result).isPresent();
            double[][] points = result.get();
            assertThat(points[0]).hasSize(6);
            assertThat(points[1]).hasSize(6);
            assertThat(points[0][0]).isEqualTo(0);
            assertThat(points[0][5]).isEqualTo(5);
            assertThat(points[1][0]).isEqualTo(1.05);
            assertThat(points[1][5]).isEqualTo(0.55);
        }

        @Test
        void shouldPreferPairFormatOverFlatCsv() {
            // When (x,y) pairs are present, use that format
            var result = VensimExprTranslator.parseLookupPoints(
                    "(0,0),(1,1),(2,4)");
            assertThat(result).isPresent();
            double[][] points = result.get();
            assertThat(points[0]).containsExactly(0, 1, 2);
            assertThat(points[1]).containsExactly(0, 1, 4);
        }

        @Test
        void shouldRejectOddNumberOfFlatCsvValues() {
            var result = VensimExprTranslator.parseLookupPoints("0, 1, 2");
            assertThat(result).isEmpty();
        }

        @Test
        void shouldRejectTooFewFlatCsvValues() {
            var result = VensimExprTranslator.parseLookupPoints("0, 1");
            assertThat(result).isEmpty();
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
            assertThat(result.expression()).contains("and");
        }
    }
}
