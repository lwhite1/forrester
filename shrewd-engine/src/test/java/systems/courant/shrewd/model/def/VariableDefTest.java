package systems.courant.shrewd.model.def;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("VariableDef")
class VariableDefTest {

    @Nested
    @DisplayName("isLiteral / literalValue (#329)")
    class LiteralDetection {

        @Test
        void shouldDetectPositiveInteger() {
            VariableDef v = new VariableDef("x", "42", "widgets");
            assertThat(v.isLiteral()).isTrue();
            assertThat(v.literalValue()).isEqualTo(42.0);
        }

        @Test
        void shouldDetectNegativeNumber() {
            VariableDef v = new VariableDef("x", "-3.5", "widgets");
            assertThat(v.isLiteral()).isTrue();
            assertThat(v.literalValue()).isEqualTo(-3.5);
        }

        @Test
        void shouldDetectZero() {
            VariableDef v = new VariableDef("x", "0", "widgets");
            assertThat(v.isLiteral()).isTrue();
            assertThat(v.literalValue()).isEqualTo(0.0);
        }

        @Test
        void shouldDetectDecimalFraction() {
            VariableDef v = new VariableDef("x", "0.03", "widgets");
            assertThat(v.isLiteral()).isTrue();
            assertThat(v.literalValue()).isEqualTo(0.03);
        }

        @Test
        void shouldDetectScientificNotation() {
            VariableDef v = new VariableDef("x", "1.5E3", "widgets");
            assertThat(v.isLiteral()).isTrue();
            assertThat(v.literalValue()).isEqualTo(1500.0);
        }

        @Test
        void shouldRejectFormulaExpression() {
            VariableDef v = new VariableDef("x", "Population * birth_rate", "widgets");
            assertThat(v.isLiteral()).isFalse();
        }

        @Test
        void shouldRejectVariableReference() {
            VariableDef v = new VariableDef("x", "other_var", "widgets");
            assertThat(v.isLiteral()).isFalse();
        }

        @Test
        void shouldRejectFunctionCall() {
            VariableDef v = new VariableDef("x", "MAX(a, b)", "widgets");
            assertThat(v.isLiteral()).isFalse();
        }

        @Test
        void shouldHandleWhitespace() {
            VariableDef v = new VariableDef("x", " 42 ", "widgets");
            assertThat(v.isLiteral()).isTrue();
            assertThat(v.literalValue()).isEqualTo(42.0);
        }

        @Test
        void shouldThrowOnLiteralValueForNonLiteral() {
            VariableDef v = new VariableDef("x", "a + b", "widgets");
            assertThatThrownBy(v::literalValue)
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("not a literal");
        }
    }


    @Nested
    @DisplayName("formatValue (#421)")
    class FormatValue {

        @Test
        void shouldFormatIntegerWithoutDecimalPoint() {
            assertThat(VariableDef.formatValue(42.0)).isEqualTo("42");
        }

        @Test
        void shouldFormatNegativeInteger() {
            assertThat(VariableDef.formatValue(-7.0)).isEqualTo("-7");
        }

        @Test
        void shouldFormatZero() {
            assertThat(VariableDef.formatValue(0.0)).isEqualTo("0");
        }

        @Test
        void shouldPreserveFractionalPart() {
            assertThat(VariableDef.formatValue(3.14)).isEqualTo("3.14");
        }

        @Test
        void shouldNotOverflowForLargeDouble() {
            assertThat(VariableDef.formatValue(1e19)).isEqualTo("1.0E19");
        }

        @Test
        void shouldNotOverflowForLargeNegativeDouble() {
            assertThat(VariableDef.formatValue(-1e19)).isEqualTo("-1.0E19");
        }

        @Test
        void shouldHandlePositiveInfinity() {
            assertThat(VariableDef.formatValue(Double.POSITIVE_INFINITY)).isEqualTo("Infinity");
        }

        @Test
        void shouldHandleNegativeInfinity() {
            assertThat(VariableDef.formatValue(Double.NEGATIVE_INFINITY)).isEqualTo("-Infinity");
        }

        @Test
        void shouldHandleNaN() {
            assertThat(VariableDef.formatValue(Double.NaN)).isEqualTo("NaN");
        }

        @Test
        void shouldHandleMaxLongBoundary() {
            // Long.MAX_VALUE is exactly representable as a double
            double atBoundary = (double) Long.MAX_VALUE;
            String result = VariableDef.formatValue(atBoundary);
            assertThat(result).doesNotContain(".");
        }

        @Test
        void shouldHandleValueJustBeyondLongMax() {
            double beyondMax = (double) Long.MAX_VALUE * 2;
            String result = VariableDef.formatValue(beyondMax);
            assertThat(result).isEqualTo("1.8446744073709552E19");
        }
    }
}
