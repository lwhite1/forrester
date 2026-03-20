package systems.courant.sd.io;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

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
        void shouldRejectNaN() {
            assertThatIllegalArgumentException()
                    .isThrownBy(() -> FormatUtils.formatDouble(Double.NaN))
                    .withMessageContaining("NaN");
        }

        @Test
        void shouldRejectPositiveInfinity() {
            assertThatIllegalArgumentException()
                    .isThrownBy(() -> FormatUtils.formatDouble(Double.POSITIVE_INFINITY))
                    .withMessageContaining("Infinity");
        }

        @Test
        void shouldRejectNegativeInfinity() {
            assertThatIllegalArgumentException()
                    .isThrownBy(() -> FormatUtils.formatDouble(Double.NEGATIVE_INFINITY))
                    .withMessageContaining("Infinity");
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
        void shouldIgnoreUnbalancedClosingParenAtDepthZero() {
            // Unbalanced ) should be ignored, not abort the search
            assertThat(FormatUtils.findTopLevelComma("abc), d", 0)).isEqualTo(4);
        }

        @Test
        void shouldReturnMinusOneWhenNoCommaAfterUnbalancedParen() {
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

    @Nested
    @DisplayName("findMatchingCloseParen")
    class FindMatchingCloseParen {

        @Test
        void shouldFindMatchingParen() {
            // "LOOKUP(name, x)" — ( at 6, matching ) at 14
            assertThat(FormatUtils.findMatchingCloseParen("LOOKUP(name, x)", 6)).isEqualTo(14);
        }

        @Test
        void shouldHandleNestedParens() {
            // "LOOKUP(name, max(a, b))" — ( at 6, matching ) at 22
            assertThat(FormatUtils.findMatchingCloseParen("LOOKUP(name, max(a, b))", 6)).isEqualTo(22);
        }

        @Test
        void shouldStopAtMatchingParen() {
            // "LOOKUP(name, x) + foo(y)" — ( at 6, matching ) at 14 (not the last one)
            assertThat(FormatUtils.findMatchingCloseParen("LOOKUP(name, x) + foo(y)", 6)).isEqualTo(14);
        }

        @Test
        void shouldReturnMinusOneIfUnmatched() {
            assertThat(FormatUtils.findMatchingCloseParen("LOOKUP(name, x", 6)).isEqualTo(-1);
        }

        @Test
        void shouldHandleDeeplyNested() {
            // "f(g(h(x)))" — ( at 1, matching ) at 9
            assertThat(FormatUtils.findMatchingCloseParen("f(g(h(x)))", 1)).isEqualTo(9);
        }
    }
}
