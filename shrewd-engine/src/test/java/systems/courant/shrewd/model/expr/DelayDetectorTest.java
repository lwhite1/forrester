package systems.courant.shrewd.model.expr;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

class DelayDetectorTest {

    @Nested
    @DisplayName("containsDelay (AST)")
    class ContainsDelayTest {

        @Test
        void shouldDetectSmooth() {
            Expr ast = ExprParser.parse("SMOOTH(input, 5)");
            assertThat(DelayDetector.containsDelay(ast)).isTrue();
        }

        @Test
        void shouldDetectDelay3() {
            Expr ast = ExprParser.parse("DELAY3(output, 10)");
            assertThat(DelayDetector.containsDelay(ast)).isTrue();
        }

        @Test
        void shouldDetectDelayFixed() {
            Expr ast = ExprParser.parse("DELAY_FIXED(input, 3, 0)");
            assertThat(DelayDetector.containsDelay(ast)).isTrue();
        }

        @Test
        void shouldDetectNestedDelay() {
            Expr ast = ExprParser.parse("MAX(SMOOTH(x, 5), 0)");
            assertThat(DelayDetector.containsDelay(ast)).isTrue();
        }

        @Test
        void shouldDetectDelayInBinaryOp() {
            Expr ast = ExprParser.parse("SMOOTH(x, 3) + DELAY3(y, 2)");
            assertThat(DelayDetector.containsDelay(ast)).isTrue();
        }

        @Test
        void shouldDetectDelayInConditional() {
            Expr ast = ExprParser.parse("IF(flag, SMOOTH(x, 5), 0)");
            assertThat(DelayDetector.containsDelay(ast)).isTrue();
        }

        @Test
        void shouldReturnFalseForLiteral() {
            Expr ast = ExprParser.parse("42");
            assertThat(DelayDetector.containsDelay(ast)).isFalse();
        }

        @Test
        void shouldReturnFalseForSimpleFormula() {
            Expr ast = ExprParser.parse("a + b * c");
            assertThat(DelayDetector.containsDelay(ast)).isFalse();
        }

        @Test
        void shouldReturnFalseForNonDelayFunction() {
            Expr ast = ExprParser.parse("MAX(a, b)");
            assertThat(DelayDetector.containsDelay(ast)).isFalse();
        }

        @Test
        void shouldBeCaseInsensitive() {
            Expr ast = ExprParser.parse("smooth(input, 5)");
            assertThat(DelayDetector.containsDelay(ast)).isTrue();
        }
    }

    @Nested
    @DisplayName("equationContainsDelay (string)")
    class EquationContainsDelayTest {

        @ParameterizedTest
        @ValueSource(strings = {
                "SMOOTH(input, 5)",
                "SMOOTH3(input, 5)",
                "DELAY1(input, 5)",
                "DELAY3(output, 10)",
                "DELAY3I(output, 10, 0)",
                "DELAY_FIXED(input, 3, 0)"
        })
        void shouldDetectAllDelayFunctions(String equation) {
            assertThat(DelayDetector.equationContainsDelay(equation)).isTrue();
        }

        @Test
        void shouldReturnFalseForNull() {
            assertThat(DelayDetector.equationContainsDelay(null)).isFalse();
        }

        @Test
        void shouldReturnFalseForBlank() {
            assertThat(DelayDetector.equationContainsDelay("  ")).isFalse();
        }

        @Test
        void shouldReturnFalseForInvalidEquation() {
            assertThat(DelayDetector.equationContainsDelay("+++")).isFalse();
        }

        @Test
        void shouldReturnFalseForNonDelayEquation() {
            assertThat(DelayDetector.equationContainsDelay("a * b + c")).isFalse();
        }
    }
}
