package systems.courant.sd.io.vensim;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("VensimSubscriptExpander")
class VensimSubscriptExpanderTest {

    @Nested
    @DisplayName("splitSubscriptValues")
    class SplitSubscriptValues {

        @Test
        @DisplayName("should return null for blank expression")
        void shouldReturnNullForBlankExpression() {
            assertThat(VensimSubscriptExpander.splitSubscriptValues("", 3)).isNull();
            assertThat(VensimSubscriptExpander.splitSubscriptValues("  ", 3)).isNull();
            assertThat(VensimSubscriptExpander.splitSubscriptValues(null, 3)).isNull();
        }

        @Test
        @DisplayName("should split numeric comma-separated values when count matches")
        void shouldSplitNumericValues() {
            List<String> result = VensimSubscriptExpander.splitSubscriptValues("1, 2, 3", 3);
            assertThat(result).containsExactly("1", " 2", " 3");
        }

        @Test
        @DisplayName("should split non-numeric comma-separated values when count matches")
        void shouldSplitNonNumericValues() {
            List<String> result = VensimSubscriptExpander.splitSubscriptValues("a, b, c", 3);
            assertThat(result).containsExactly("a", " b", " c");
        }

        @Test
        @DisplayName("should split mixed numeric and variable reference values")
        void shouldSplitMixedValues() {
            List<String> result = VensimSubscriptExpander.splitSubscriptValues(
                    "0, INITIAL TIME, 100", 3);
            assertThat(result).containsExactly("0", " INITIAL TIME", " 100");
        }

        @Test
        @DisplayName("should split expression values when count matches")
        void shouldSplitExpressionValues() {
            List<String> result = VensimSubscriptExpander.splitSubscriptValues(
                    "a + b, c * d, e / f", 3);
            assertThat(result).containsExactly("a + b", " c * d", " e / f");
        }

        @Test
        @DisplayName("should return null when count does not match")
        void shouldReturnNullWhenCountMismatch() {
            assertThat(VensimSubscriptExpander.splitSubscriptValues("1, 2, 3", 2)).isNull();
            assertThat(VensimSubscriptExpander.splitSubscriptValues("a, b", 3)).isNull();
        }

        @Test
        @DisplayName("should not split commas inside parentheses")
        void shouldNotSplitCommasInsideParens() {
            // With expectedCount=1, verifies the whole expression is returned as one part
            // (i.e., commas inside parens are not treated as top-level separators)
            List<String> result = VensimSubscriptExpander.splitSubscriptValues(
                    "IF(a>b, 1, 0)", 1);
            assertThat(result).containsExactly("IF(a>b, 1, 0)");
        }

        @Test
        @DisplayName("should handle single value with no commas")
        void shouldHandleSingleValue() {
            List<String> result = VensimSubscriptExpander.splitSubscriptValues("42", 1);
            assertThat(result).containsExactly("42");
        }

        @Test
        @DisplayName("should split function calls separated by top-level commas")
        void shouldSplitTopLevelFunctionCalls() {
            List<String> result = VensimSubscriptExpander.splitSubscriptValues(
                    "IF(a>b, 1, 0), IF(c>d, 2, 3)", 2);
            assertThat(result).containsExactly("IF(a>b, 1, 0)", " IF(c>d, 2, 3)");
        }
    }
}
