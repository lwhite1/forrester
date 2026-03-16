package systems.courant.sd.io.vensim;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
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
        void shouldPreferExactCaseMatchOverCaseInsensitive() {
            // Two names that differ only by case should each be replaced with their
            // own normalized form, not conflated by the case-insensitive fallback.
            Set<String> names = Set.of("Time Step", "time step");
            var result = VensimExprTranslator.translate(
                    "Time Step + time step", "var", names);
            assertThat(result.expression()).isEqualTo("Time_Step + time_step");
        }

        @Test
        void shouldFallBackToCaseInsensitiveWhenNoExactMatch() {
            // If only "Contact Rate" is known but expression uses "contact rate",
            // the case-insensitive fallback should still replace it.
            Set<String> names = Set.of("Contact Rate");
            var result = VensimExprTranslator.translate(
                    "contact rate * 2", "var", names);
            assertThat(result.expression()).isEqualTo("Contact_Rate * 2");
        }

        @Test
        void shouldNotReplacePartialMatches() {
            Set<String> names = Set.of("Contact Rate");
            var result = VensimExprTranslator.translate(
                    "MyContact Rate2", "var", names);
            // "Contact Rate" should not match due to word boundaries, but the
            // fallback consecutive-identifier merge joins "MyContact" + "Rate2"
            // since two identifiers can't be adjacent without an operator
            assertThat(result.expression()).isEqualTo("MyContact_Rate2");
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
    @DisplayName("Not-equal operator translation (#492)")
    class NotEqualOperator {

        @Test
        void shouldTranslateNotEqualOperator() {
            var result = VensimExprTranslator.translate("x <> 0", "var", EMPTY_NAMES);
            assertThat(result.expression()).isEqualTo("x != 0");
        }

        @Test
        void shouldTranslateNotEqualInIfThenElse() {
            var result = VensimExprTranslator.translate(
                    "IF THEN ELSE(adjust <> 0, 1, 0)", "var", EMPTY_NAMES);
            assertThat(result.expression()).isEqualTo("IF(adjust != 0, 1, 0)");
        }

        @Test
        void shouldTranslateMultipleNotEquals() {
            var result = VensimExprTranslator.translate(
                    "IF THEN ELSE(x <> 0 :AND: y <> 0, 1, 0)", "var", EMPTY_NAMES);
            assertThat(result.expression()).contains("!=");
            assertThat(result.expression()).doesNotContain("<>");
        }

        @Test
        void shouldNotAffectLessThanOrGreaterThan() {
            var result = VensimExprTranslator.translate("x < 0", "var", EMPTY_NAMES);
            assertThat(result.expression()).isEqualTo("x < 0");
        }
    }

    @Nested
    @DisplayName("Subscript bracket translation (#495)")
    class SubscriptBrackets {

        @Test
        void shouldTranslateSimpleSubscriptBracket() {
            var result = VensimExprTranslator.translate(
                    "Population[North]", "var", EMPTY_NAMES);
            assertThat(result.expression()).isEqualTo("Population_North");
        }

        @Test
        void shouldTranslateMultipleSubscriptBrackets() {
            var result = VensimExprTranslator.translate(
                    "inflow[tub] + outflow[tub]", "var", EMPTY_NAMES);
            assertThat(result.expression()).isEqualTo("inflow_tub + outflow_tub");
        }

        @Test
        void shouldTranslateSubscriptWithSpaces() {
            var result = VensimExprTranslator.translate(
                    "x[low tub]", "var", EMPTY_NAMES);
            assertThat(result.expression()).isEqualTo("x_low_tub");
        }

        @Test
        void shouldTranslateSubscriptInComplexExpression() {
            var result = VensimExprTranslator.translate(
                    "rate[Region] * Population[Region] / total", "var", EMPTY_NAMES);
            assertThat(result.expression()).isEqualTo(
                    "rate_Region * Population_Region / total");
        }

        @Test
        void shouldNotAffectArrayIndexInFunctions() {
            // Built-in functions should pass through; brackets only affect identifiers
            var result = VensimExprTranslator.translate(
                    "MAX(x[a], y[b])", "var", EMPTY_NAMES);
            assertThat(result.expression()).isEqualTo("MAX(x_a, y_b)");
        }
    }

    @Nested
    @DisplayName("MESSAGE and SIMULTANEOUS no-ops (#498)")
    class MessageAndSimultaneous {

        @Test
        void shouldStripMessageToZero() {
            var result = VensimExprTranslator.translate("MESSAGE(text, 1)", "var", EMPTY_NAMES);
            assertThat(result.expression()).isEqualTo("0");
        }

        @Test
        void shouldStripMessageInsideExpression() {
            var result = VensimExprTranslator.translate(
                    "x + MESSAGE(alert, y)", "var", EMPTY_NAMES);
            assertThat(result.expression()).isEqualTo("x + 0");
        }

        @Test
        void shouldStripSimultaneousToZero() {
            var result = VensimExprTranslator.translate(
                    "SIMULTANEOUS(0, 2)", "var", EMPTY_NAMES);
            assertThat(result.expression()).isEqualTo("0");
        }

        @Test
        void shouldStripSimultaneousInsideExpression() {
            var result = VensimExprTranslator.translate(
                    "IF THEN ELSE(SIMULTANEOUS(0, 1) > 0, 1, 0)", "var", EMPTY_NAMES);
            assertThat(result.expression()).contains("IF(0 > 0, 1, 0)");
        }

        @Test
        void shouldBeCaseInsensitiveForMessage() {
            var result = VensimExprTranslator.translate("message(x, 0)", "var", EMPTY_NAMES);
            assertThat(result.expression()).isEqualTo("0");
        }

        @Test
        void shouldBeCaseInsensitiveForSimultaneous() {
            var result = VensimExprTranslator.translate("simultaneous(1, 3)", "var", EMPTY_NAMES);
            assertThat(result.expression()).isEqualTo("0");
        }
    }

    @Nested
    @DisplayName("SAMPLE IF TRUE and FIND ZERO translation (#512)")
    class SampleIfTrueAndFindZero {

        @Test
        void shouldTranslateSampleIfTrue() {
            var result = VensimExprTranslator.translate(
                    "SAMPLE IF TRUE(x > 0, input, 0)", "var", EMPTY_NAMES);
            assertThat(result.expression()).isEqualTo("SAMPLE_IF_TRUE(x > 0, input, 0)");
            assertThat(result.warnings()).noneMatch(w -> w.contains("SAMPLE IF TRUE"));
        }

        @Test
        void shouldTranslateSampleIfTrueCaseInsensitive() {
            var result = VensimExprTranslator.translate(
                    "sample if true(cond, val, 0)", "var", EMPTY_NAMES);
            assertThat(result.expression()).isEqualTo("SAMPLE_IF_TRUE(cond, val, 0)");
        }

        @Test
        void shouldTranslateFindZero() {
            var result = VensimExprTranslator.translate(
                    "FIND ZERO(expr, x, 0, 100)", "var", EMPTY_NAMES);
            assertThat(result.expression()).isEqualTo("FIND_ZERO(expr, x, 0, 100)");
            assertThat(result.warnings()).noneMatch(w -> w.contains("FIND ZERO"));
        }

        @Test
        void shouldTranslateFindZeroCaseInsensitive() {
            var result = VensimExprTranslator.translate(
                    "find zero(f, y, -10, 10)", "var", EMPTY_NAMES);
            assertThat(result.expression()).isEqualTo("FIND_ZERO(f, y, -10, 10)");
        }

        @Test
        void shouldNoLongerWarnForSampleIfTrue() {
            var result = VensimExprTranslator.translate(
                    "SAMPLE IF TRUE(x > 0, y, 0)", "var", EMPTY_NAMES);
            assertThat(result.warnings()).noneMatch(w -> w.contains("Unsupported"));
        }

        @Test
        void shouldNoLongerWarnForFindZero() {
            var result = VensimExprTranslator.translate(
                    "FIND ZERO(x - 5, x, 0, 10)", "var", EMPTY_NAMES);
            assertThat(result.warnings()).noneMatch(w -> w.contains("Unsupported"));
        }
    }

    @Nested
    @DisplayName("ACTIVE INITIAL translation (#513)")
    class ActiveInitial {

        @Test
        void shouldExtractFirstArgument() {
            var result = VensimExprTranslator.translate(
                    "ACTIVE INITIAL(x + y, 0)", "var", EMPTY_NAMES);
            assertThat(result.expression()).isEqualTo("x + y");
        }

        @Test
        void shouldBeCaseInsensitive() {
            var result = VensimExprTranslator.translate(
                    "active initial(x, 0)", "var", EMPTY_NAMES);
            assertThat(result.expression()).isEqualTo("x");
        }

        @Test
        void shouldHandleNestedFunctions() {
            Set<String> names = Set.of("Potential Customers", "Waiting Customers");
            var result = VensimExprTranslator.translate(
                    "ACTIVE INITIAL(Potential Customers + Waiting Customers, Potential Customers)",
                    "var", names);
            assertThat(result.expression())
                    .isEqualTo("Potential_Customers + Waiting_Customers");
        }

        @Test
        void shouldHandleActiveInitialWithIfThenElse() {
            var result = VensimExprTranslator.translate(
                    "ACTIVE INITIAL(IF THEN ELSE(x > 0, x, 0), 0)", "var", EMPTY_NAMES);
            assertThat(result.expression()).isEqualTo("IF(x > 0, x, 0)");
        }

        @Test
        void shouldHandleActiveInitialInLargerExpression() {
            var result = VensimExprTranslator.translate(
                    "ACTIVE INITIAL(a * b, 0) + c", "var", EMPTY_NAMES);
            assertThat(result.expression()).isEqualTo("a * b + c");
        }
    }

    @Nested
    @DisplayName("Equality operator translation (#596)")
    class EqualityOperator {

        @Test
        void shouldTranslateSingleEqualsToDoubleEquals() {
            var result = VensimExprTranslator.translate(
                    "IF THEN ELSE(x = 0, 1, 0)", "var", EMPTY_NAMES);
            assertThat(result.expression()).isEqualTo("IF(x == 0, 1, 0)");
        }

        @Test
        void shouldNotDoubleExistingDoubleEquals() {
            var result = VensimExprTranslator.translate(
                    "IF THEN ELSE(x == 0, 1, 0)", "var", EMPTY_NAMES);
            assertThat(result.expression()).isEqualTo("IF(x == 0, 1, 0)");
        }

        @Test
        void shouldNotAffectNotEquals() {
            var result = VensimExprTranslator.translate("x != 0", "var", EMPTY_NAMES);
            assertThat(result.expression()).isEqualTo("x != 0");
        }

        @Test
        void shouldNotAffectLessEquals() {
            var result = VensimExprTranslator.translate("x <= 5", "var", EMPTY_NAMES);
            assertThat(result.expression()).isEqualTo("x <= 5");
        }

        @Test
        void shouldNotAffectGreaterEquals() {
            var result = VensimExprTranslator.translate("x >= 5", "var", EMPTY_NAMES);
            assertThat(result.expression()).isEqualTo("x >= 5");
        }

        @Test
        void shouldHandleMultipleEqualityChecks() {
            var result = VensimExprTranslator.translate(
                    "IF THEN ELSE(scenario = 0 :OR: scenario = 1, a, b)", "var", EMPTY_NAMES);
            assertThat(result.expression()).contains("scenario == 0");
            assertThat(result.expression()).contains("scenario == 1");
        }

        @Test
        void shouldNotCorruptEqualityAfterSubscriptBracketTranslation() {
            // Equality replacement must happen after subscript brackets are removed,
            // so any = inside brackets won't be doubled
            var result = VensimExprTranslator.translate(
                    "IF THEN ELSE(x[Region] = 0, 1, 0)", "var", EMPTY_NAMES);
            assertThat(result.expression()).contains("== 0");
            // Should not contain === (tripled equals from double replacement)
            assertThat(result.expression()).doesNotContain("===");
        }
    }

    @Nested
    @DisplayName("DELAY MATERIAL translation (#596)")
    class DelayMaterial {

        @Test
        void shouldTranslateDelayMaterialToDelayFixed() {
            var result = VensimExprTranslator.translate(
                    "DELAY MATERIAL(input, 5, init, 3)", "var", EMPTY_NAMES);
            assertThat(result.expression()).isEqualTo("DELAY_FIXED(input, 5, init)");
        }

        @Test
        void shouldBeCaseInsensitive() {
            var result = VensimExprTranslator.translate(
                    "delay material(x, 10, 0, 5)", "var", EMPTY_NAMES);
            assertThat(result.expression()).isEqualTo("DELAY_FIXED(x, 10, 0)");
        }

        @Test
        void shouldHandleComplexArguments() {
            var result = VensimExprTranslator.translate(
                    "DELAY MATERIAL(a + b, c * d, e, f)", "var", EMPTY_NAMES);
            assertThat(result.expression()).isEqualTo("DELAY_FIXED(a + b, c * d, e)");
        }
    }

    @Nested
    @DisplayName("RANDOM 0 1 translation (#596)")
    class Random01 {

        @Test
        void shouldTranslateRandom01() {
            var result = VensimExprTranslator.translate(
                    "RANDOM 0 1()", "var", EMPTY_NAMES);
            assertThat(result.expression()).isEqualTo("RANDOM_UNIFORM(0, 1, 0)");
        }

        @Test
        void shouldBeCaseInsensitive() {
            var result = VensimExprTranslator.translate(
                    "random 0 1()", "var", EMPTY_NAMES);
            assertThat(result.expression()).isEqualTo("RANDOM_UNIFORM(0, 1, 0)");
        }

        @Test
        void shouldHandleRandom01InExpression() {
            var result = VensimExprTranslator.translate(
                    "x + RANDOM 0 1() * 10", "var", EMPTY_NAMES);
            assertThat(result.expression()).isEqualTo("x + RANDOM_UNIFORM(0, 1, 0) * 10");
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

    @Nested
    @DisplayName("LOOKUP AREA translation")
    class LookupAreaTranslation {

        @Test
        void shouldTranslateLookupArea() {
            var result = VensimExprTranslator.translate(
                    "LOOKUP AREA(my_table, 0, 10)", "var", EMPTY_NAMES);
            assertThat(result.expression()).isEqualTo("LOOKUP_AREA(my_table, 0, 10)");
        }

        @Test
        void shouldTranslateLookupAreaCaseInsensitive() {
            var result = VensimExprTranslator.translate(
                    "lookup area(my_table, x1, x2)", "var", EMPTY_NAMES);
            assertThat(result.expression()).isEqualTo("LOOKUP_AREA(my_table, x1, x2)");
        }

        @Test
        void shouldTranslateLookupAreaWithExtraSpaces() {
            var result = VensimExprTranslator.translate(
                    "LOOKUP   AREA  (tbl, 0, 5)", "var", EMPTY_NAMES);
            assertThat(result.expression()).isEqualTo("LOOKUP_AREA(tbl, 0, 5)");
        }
    }

    @Nested
    @DisplayName("GET external data functions (#480)")
    class GetExternalDataFunctions {

        @Test
        void shouldReplaceGetXlsDataWithZero() {
            var result = VensimExprTranslator.translate(
                    "GET XLS DATA('data.xlsx', 'Sheet1', 'A', 'B2')", "var", EMPTY_NAMES);
            assertThat(result.expression()).isEqualTo("0");
            assertThat(result.warnings()).anyMatch(w -> w.contains("GET XLS DATA"));
            assertThat(result.warnings()).anyMatch(w -> w.contains("data.xlsx"));
        }

        @Test
        void shouldReplaceGetDirectDataWithZero() {
            var result = VensimExprTranslator.translate(
                    "GET DIRECT DATA('input.csv', ',', 'A', 'B2')", "var", EMPTY_NAMES);
            assertThat(result.expression()).isEqualTo("0");
            assertThat(result.warnings()).anyMatch(w -> w.contains("GET DIRECT DATA"));
            assertThat(result.warnings()).anyMatch(w -> w.contains("input.csv"));
        }

        @Test
        void shouldReplaceGetXlsConstantsWithZero() {
            var result = VensimExprTranslator.translate(
                    "GET XLS CONSTANTS('params.xlsx', 'Sheet1', 'B2')", "var", EMPTY_NAMES);
            assertThat(result.expression()).isEqualTo("0");
            assertThat(result.warnings()).anyMatch(w -> w.contains("GET XLS CONSTANTS"));
        }

        @Test
        void shouldReplaceGetDirectConstantsWithZero() {
            var result = VensimExprTranslator.translate(
                    "GET DIRECT CONSTANTS('config.csv', ',', 'B2')", "var", EMPTY_NAMES);
            assertThat(result.expression()).isEqualTo("0");
            assertThat(result.warnings()).anyMatch(w -> w.contains("GET DIRECT CONSTANTS"));
        }

        @Test
        void shouldReplaceGetXlsLookupsWithZero() {
            var result = VensimExprTranslator.translate(
                    "GET XLS LOOKUPS('tables.xlsx', 'Sheet1', 'A', 'B2')", "var", EMPTY_NAMES);
            assertThat(result.expression()).isEqualTo("0");
            assertThat(result.warnings()).anyMatch(w -> w.contains("GET XLS LOOKUPS"));
        }

        @Test
        void shouldReplaceGetDirectLookupsWithZero() {
            var result = VensimExprTranslator.translate(
                    "GET DIRECT LOOKUPS('tables.csv', ',', 'A', 'B2')", "var", EMPTY_NAMES);
            assertThat(result.expression()).isEqualTo("0");
            assertThat(result.warnings()).anyMatch(w -> w.contains("GET DIRECT LOOKUPS"));
        }

        @Test
        void shouldHandleGetFunctionInLargerExpression() {
            var result = VensimExprTranslator.translate(
                    "x + GET XLS DATA('file.xlsx', 'S', 'A', 'B') * 2", "var", EMPTY_NAMES);
            assertThat(result.expression()).isEqualTo("x + 0 * 2");
            assertThat(result.warnings()).anyMatch(w -> w.contains("GET XLS DATA"));
        }

        @Test
        void shouldHandleCaseInsensitiveGetFunctions() {
            var result = VensimExprTranslator.translate(
                    "get direct data('file.csv', ',', 'A', 'B')", "var", EMPTY_NAMES);
            assertThat(result.expression()).isEqualTo("0");
            assertThat(result.warnings()).anyMatch(w -> w.contains("GET DIRECT DATA"));
        }

        @Test
        void shouldExtractFirstArgument() {
            assertThat(VensimExprTranslator.extractFirstArgument(
                    "'file.csv', ',', 'A', 'B2'")).isEqualTo("file.csv");
            assertThat(VensimExprTranslator.extractFirstArgument(
                    "\"file.xlsx\", 'Sheet'")).isEqualTo("file.xlsx");
            assertThat(VensimExprTranslator.extractFirstArgument(
                    "single_arg")).isEqualTo("single_arg");
        }
    }

    @Nested
    @DisplayName("SUM expansion (#664)")
    class SumExpansion {

        private static final Map<String, List<String>> TASK_DIMS = Map.of(
                "task", List.of("design", "prototype", "build"));

        @Test
        void shouldExpandSumWithBangDimension() {
            var result = VensimExprTranslator.translate(
                    "SUM(Work_Done[task!])", "var", EMPTY_NAMES, Set.of(), TASK_DIMS);
            assertThat(result.expression()).isEqualTo(
                    "(Work_Done_design + Work_Done_prototype + Work_Done_build)");
        }

        @Test
        void shouldBeCaseInsensitive() {
            var result = VensimExprTranslator.translate(
                    "sum(x[task!])", "var", EMPTY_NAMES, Set.of(), TASK_DIMS);
            assertThat(result.expression()).isEqualTo(
                    "(x_design + x_prototype + x_build)");
        }

        @Test
        void shouldHandleSumInLargerExpression() {
            var result = VensimExprTranslator.translate(
                    "2 * SUM(x[task!]) + 1", "var", EMPTY_NAMES, Set.of(), TASK_DIMS);
            assertThat(result.expression()).isEqualTo(
                    "2 * (x_design + x_prototype + x_build) + 1");
        }

        @Test
        void shouldHandleSingleLabelDimension() {
            Map<String, List<String>> dims = Map.of("d", List.of("only"));
            var result = VensimExprTranslator.translate(
                    "SUM(x[d!])", "var", EMPTY_NAMES, Set.of(), dims);
            assertThat(result.expression()).isEqualTo("(x_only)");
        }

        @Test
        void shouldHandleSumWithoutDimensions() {
            // Without dimension info, SUM is left as-is
            var result = VensimExprTranslator.translate(
                    "SUM(x[task!])", "var", EMPTY_NAMES);
            assertThat(result.expression()).isEqualTo("SUM(x_task)");
        }
    }

    @Nested
    @DisplayName("VMIN expansion (#664)")
    class VminExpansion {

        private static final Map<String, List<String>> TASK_DIMS = Map.of(
                "prereqtask", List.of("design", "prototype", "build"));

        @Test
        void shouldExpandVminToNestedMin() {
            var result = VensimExprTranslator.translate(
                    "VMIN(x[prereqtask!])", "var", EMPTY_NAMES, Set.of(), TASK_DIMS);
            assertThat(result.expression()).isEqualTo(
                    "MIN(x_design, MIN(x_prototype, x_build))");
        }

        @Test
        void shouldHandleTwoElementVmin() {
            Map<String, List<String>> dims = Map.of("d", List.of("a", "b"));
            var result = VensimExprTranslator.translate(
                    "VMIN(x[d!])", "var", EMPTY_NAMES, Set.of(), dims);
            assertThat(result.expression()).isEqualTo("MIN(x_a, x_b)");
        }

        @Test
        void shouldHandleSingleLabelVmin() {
            Map<String, List<String>> dims = Map.of("d", List.of("only"));
            var result = VensimExprTranslator.translate(
                    "VMIN(x[d!])", "var", EMPTY_NAMES, Set.of(), dims);
            assertThat(result.expression()).isEqualTo("x_only");
        }

        @Test
        void shouldExpandVminWithComplexInnerExpression() {
            var result = VensimExprTranslator.translate(
                    "VMIN(IF(y[prereqtask!], z[prereqtask!], 1))",
                    "var", EMPTY_NAMES, Set.of(), TASK_DIMS);
            assertThat(result.expression()).contains("MIN(");
            assertThat(result.expression()).contains("z_design");
            assertThat(result.expression()).contains("z_prototype");
            assertThat(result.expression()).contains("z_build");
        }
    }

    @Nested
    @DisplayName("Multi-dimensional subscript brackets (#664)")
    class MultiDimSubscripts {

        @Test
        void shouldTranslateCommaSeparatedSubscripts() {
            var result = VensimExprTranslator.translate(
                    "x[design,prototype]", "var", EMPTY_NAMES);
            assertThat(result.expression()).isEqualTo("x_design_prototype");
        }

        @Test
        void shouldTranslateMultipleBracketedReferences() {
            var result = VensimExprTranslator.translate(
                    "x[a,b] + y[c,d]", "var", EMPTY_NAMES);
            assertThat(result.expression()).isEqualTo("x_a_b + y_c_d");
        }
    }

    @Nested
    @DisplayName("Unknown multi-word name fallback (#665)")
    class UnknownMultiWordNames {

        @Test
        void shouldUnderscoreConsecutiveIdentifiers() {
            var result = VensimExprTranslator.translate(
                    "Fish * HATCH FRACTION", "var", EMPTY_NAMES);
            assertThat(result.expression()).isEqualTo("Fish * HATCH_FRACTION");
        }

        @Test
        void shouldHandleThreeWordNames() {
            var result = VensimExprTranslator.translate(
                    "total number of HB", "var", EMPTY_NAMES);
            assertThat(result.expression()).isEqualTo("total_number_of_HB");
        }

        @Test
        void shouldHandleMultipleUnknownNames() {
            var result = VensimExprTranslator.translate(
                    "fish death rate + total catch per year", "var", EMPTY_NAMES);
            assertThat(result.expression()).isEqualTo(
                    "fish_death_rate + total_catch_per_year");
        }

        @Test
        void shouldNotAffectSingleWordNames() {
            var result = VensimExprTranslator.translate(
                    "x * y + z", "var", EMPTY_NAMES);
            assertThat(result.expression()).isEqualTo("x * y + z");
        }

        @Test
        void shouldNotMergeAcrossOperators() {
            var result = VensimExprTranslator.translate(
                    "a + b * c", "var", EMPTY_NAMES);
            assertThat(result.expression()).isEqualTo("a + b * c");
        }

        @Test
        void shouldCombineWithKnownNameReplacement() {
            Set<String> names = Set.of("fish death rate");
            var result = VensimExprTranslator.translate(
                    "fish death rate + UNIT SHIP OPERATING COST", "var", names);
            assertThat(result.expression()).isEqualTo(
                    "fish_death_rate + UNIT_SHIP_OPERATING_COST");
        }

        @Test
        void shouldHandleExpressionWithFunctions() {
            var result = VensimExprTranslator.translate(
                    "MAX(0, annual profits * FRACTION INVESTED / SHIP COST)",
                    "var", EMPTY_NAMES);
            assertThat(result.expression()).isEqualTo(
                    "MAX(0, annual_profits * FRACTION_INVESTED / SHIP_COST)");
        }
    }

    @Nested
    @DisplayName("Trailing :NOT: handling (#645)")
    class TrailingNot {

        @Test
        void shouldNotCrashOnTrailingNot() {
            // A trailing :NOT: with no operand should not cause infinite loop or error
            var result = VensimExprTranslator.translate(
                    "x > 0 :AND: :NOT:", "var", EMPTY_NAMES);
            // Should contain the not literally or handle gracefully
            assertThat(result.expression()).isNotNull();
        }

        @Test
        void shouldNotCrashOnStandaloneNot() {
            var result = VensimExprTranslator.translate(":NOT:", "var", EMPTY_NAMES);
            assertThat(result.expression()).isNotNull();
        }
    }
}
