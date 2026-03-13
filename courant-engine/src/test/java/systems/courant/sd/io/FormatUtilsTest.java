package systems.courant.sd.io;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("FormatUtils")
class FormatUtilsTest {

    @Nested
    @DisplayName("formatDouble")
    class FormatDouble {

        @Test
        void shouldFormatIntegerWithoutDecimalPoint() {
            assertThat(FormatUtils.formatDouble(42.0)).isEqualTo("42");
        }

        @Test
        void shouldFormatFractionalValue() {
            assertThat(FormatUtils.formatDouble(3.14)).isEqualTo("3.14");
        }

        @Test
        void shouldFormatZero() {
            assertThat(FormatUtils.formatDouble(0.0)).isEqualTo("0");
        }

        @Test
        void shouldFormatNegativeInteger() {
            assertThat(FormatUtils.formatDouble(-7.0)).isEqualTo("-7");
        }

        @Test
        void shouldFormatInfinity() {
            assertThat(FormatUtils.formatDouble(Double.POSITIVE_INFINITY)).isEqualTo("Infinity");
        }

        @Test
        void shouldFormatLargeValueWithDecimal() {
            assertThat(FormatUtils.formatDouble(1e16)).isEqualTo("1.0E16");
        }
    }

    @Nested
    @DisplayName("findTopLevelComma")
    class FindTopLevelComma {

        @Test
        void shouldFindFirstComma() {
            assertThat(FormatUtils.findTopLevelComma("a, b, c")).isEqualTo(1);
        }

        @Test
        void shouldSkipCommaInsideParens() {
            assertThat(FormatUtils.findTopLevelComma("f(a, b), c")).isEqualTo(7);
        }

        @Test
        void shouldReturnMinusOneIfNoComma() {
            assertThat(FormatUtils.findTopLevelComma("abc")).isEqualTo(-1);
        }

        @Test
        void shouldReturnMinusOneOnClosingParenAtDepthZero() {
            assertThat(FormatUtils.findTopLevelComma("abc)", 0)).isEqualTo(-1);
        }

        @Test
        void shouldFindCommaFromStartPos() {
            assertThat(FormatUtils.findTopLevelComma("a, b, c", 2)).isEqualTo(4);
        }

        @Test
        void shouldHandleNestedParens() {
            assertThat(FormatUtils.findTopLevelComma("f(g(a, b), c), d"))
                    .isEqualTo(13);
        }
    }
}
