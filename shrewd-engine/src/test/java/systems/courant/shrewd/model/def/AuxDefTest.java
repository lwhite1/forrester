package systems.courant.shrewd.model.def;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("AuxDef")
class AuxDefTest {

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
