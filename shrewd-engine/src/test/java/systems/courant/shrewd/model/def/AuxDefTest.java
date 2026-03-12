package systems.courant.shrewd.model.def;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("AuxDef")
class AuxDefTest {

    @Nested
    @DisplayName("isLiteral / literalValue (#329)")
    class LiteralDetection {

        @Test
        void shouldDetectPositiveInteger() {
            AuxDef aux = new AuxDef("x", "42", "widgets");
            assertThat(aux.isLiteral()).isTrue();
            assertThat(aux.literalValue()).isEqualTo(42.0);
        }

        @Test
        void shouldDetectNegativeNumber() {
            AuxDef aux = new AuxDef("x", "-3.5", "widgets");
            assertThat(aux.isLiteral()).isTrue();
            assertThat(aux.literalValue()).isEqualTo(-3.5);
        }

        @Test
        void shouldDetectZero() {
            AuxDef aux = new AuxDef("x", "0", "widgets");
            assertThat(aux.isLiteral()).isTrue();
            assertThat(aux.literalValue()).isEqualTo(0.0);
        }

        @Test
        void shouldDetectDecimalFraction() {
            AuxDef aux = new AuxDef("x", "0.03", "widgets");
            assertThat(aux.isLiteral()).isTrue();
            assertThat(aux.literalValue()).isEqualTo(0.03);
        }

        @Test
        void shouldDetectScientificNotation() {
            AuxDef aux = new AuxDef("x", "1.5E3", "widgets");
            assertThat(aux.isLiteral()).isTrue();
            assertThat(aux.literalValue()).isEqualTo(1500.0);
        }

        @Test
        void shouldRejectFormulaExpression() {
            AuxDef aux = new AuxDef("x", "Population * birth_rate", "widgets");
            assertThat(aux.isLiteral()).isFalse();
        }

        @Test
        void shouldRejectVariableReference() {
            AuxDef aux = new AuxDef("x", "other_var", "widgets");
            assertThat(aux.isLiteral()).isFalse();
        }

        @Test
        void shouldRejectFunctionCall() {
            AuxDef aux = new AuxDef("x", "MAX(a, b)", "widgets");
            assertThat(aux.isLiteral()).isFalse();
        }

        @Test
        void shouldHandleWhitespace() {
            AuxDef aux = new AuxDef("x", " 42 ", "widgets");
            assertThat(aux.isLiteral()).isTrue();
            assertThat(aux.literalValue()).isEqualTo(42.0);
        }

        @Test
        void shouldThrowOnLiteralValueForNonLiteral() {
            AuxDef aux = new AuxDef("x", "a + b", "widgets");
            assertThatThrownBy(aux::literalValue)
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("not a literal");
        }
    }


    @Nested
    @DisplayName("formatValue (#421)")
    class FormatValue {

        @Test
        void shouldFormatIntegerWithoutDecimalPoint() {
            assertThat(AuxDef.formatValue(42.0)).isEqualTo("42");
        }

        @Test
        void shouldFormatNegativeInteger() {
            assertThat(AuxDef.formatValue(-7.0)).isEqualTo("-7");
        }

        @Test
        void shouldFormatZero() {
            assertThat(AuxDef.formatValue(0.0)).isEqualTo("0");
        }

        @Test
        void shouldPreserveFractionalPart() {
            assertThat(AuxDef.formatValue(3.14)).isEqualTo("3.14");
        }

        @Test
        void shouldNotOverflowForLargeDouble() {
            assertThat(AuxDef.formatValue(1e19)).isEqualTo("1.0E19");
        }

        @Test
        void shouldNotOverflowForLargeNegativeDouble() {
            assertThat(AuxDef.formatValue(-1e19)).isEqualTo("-1.0E19");
        }

        @Test
        void shouldHandlePositiveInfinity() {
            assertThat(AuxDef.formatValue(Double.POSITIVE_INFINITY)).isEqualTo("Infinity");
        }

        @Test
        void shouldHandleNegativeInfinity() {
            assertThat(AuxDef.formatValue(Double.NEGATIVE_INFINITY)).isEqualTo("-Infinity");
        }

        @Test
        void shouldHandleNaN() {
            assertThat(AuxDef.formatValue(Double.NaN)).isEqualTo("NaN");
        }

        @Test
        void shouldHandleMaxLongBoundary() {
            // Long.MAX_VALUE is exactly representable as a double
            double atBoundary = (double) Long.MAX_VALUE;
            String result = AuxDef.formatValue(atBoundary);
            assertThat(result).doesNotContain(".");
        }

        @Test
        void shouldHandleValueJustBeyondLongMax() {
            double beyondMax = (double) Long.MAX_VALUE * 2;
            String result = AuxDef.formatValue(beyondMax);
            assertThat(result).isEqualTo("1.8446744073709552E19");
        }
    }
}
