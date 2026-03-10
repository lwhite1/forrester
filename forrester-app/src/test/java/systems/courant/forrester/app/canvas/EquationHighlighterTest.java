package systems.courant.forrester.app.canvas;

import org.fxmisc.richtext.model.StyleSpans;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Collection;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("EquationHighlighter")
class EquationHighlighterTest {

    @Nested
    @DisplayName("computeHighlighting")
    class ComputeHighlighting {

        @Test
        @DisplayName("should highlight built-in function keywords")
        void shouldHighlightKeywords() {
            StyleSpans<Collection<String>> spans =
                    EquationHighlighter.computeHighlighting("SMOOTH(x, 3)");

            // First span should be the keyword "SMOOTH"
            assertThat(spans.getStyleSpan(0).getStyle()).contains("keyword");
            assertThat(spans.getStyleSpan(0).getLength()).isEqualTo(6);
        }

        @Test
        @DisplayName("should highlight keywords case-insensitively")
        void shouldHighlightCaseInsensitive() {
            StyleSpans<Collection<String>> spans =
                    EquationHighlighter.computeHighlighting("smooth(x, 3)");

            assertThat(spans.getStyleSpan(0).getStyle()).contains("keyword");
            assertThat(spans.getStyleSpan(0).getLength()).isEqualTo(6);
        }

        @Test
        @DisplayName("should highlight numeric literals")
        void shouldHighlightNumbers() {
            StyleSpans<Collection<String>> spans =
                    EquationHighlighter.computeHighlighting("42.5");

            assertThat(spans.getStyleSpan(0).getStyle()).contains("number");
        }

        @Test
        @DisplayName("should highlight scientific notation")
        void shouldHighlightScientificNotation() {
            StyleSpans<Collection<String>> spans =
                    EquationHighlighter.computeHighlighting("1.5e-3");

            assertThat(spans.getStyleSpan(0).getStyle()).contains("number");
        }

        @Test
        @DisplayName("should highlight operators")
        void shouldHighlightOperators() {
            StyleSpans<Collection<String>> spans =
                    EquationHighlighter.computeHighlighting("a + b");

            // "a" → no style (1 char), " " → no style (1 char), "+" → operator, ...
            // Find the operator span
            boolean foundOperator = false;
            for (var span : spans) {
                if (span.getStyle().contains("operator")) {
                    foundOperator = true;
                    break;
                }
            }
            assertThat(foundOperator).isTrue();
        }

        @Test
        @DisplayName("should highlight parentheses")
        void shouldHighlightParentheses() {
            StyleSpans<Collection<String>> spans =
                    EquationHighlighter.computeHighlighting("(x)");

            assertThat(spans.getStyleSpan(0).getStyle()).contains("paren");
        }

        @Test
        @DisplayName("should handle empty text")
        void shouldHandleEmptyText() {
            StyleSpans<Collection<String>> spans =
                    EquationHighlighter.computeHighlighting("");

            assertThat(spans).isNotNull();
        }

        @Test
        @DisplayName("should handle plain identifiers without style")
        void shouldLeaveIdentifiersUnstyled() {
            StyleSpans<Collection<String>> spans =
                    EquationHighlighter.computeHighlighting("Population");

            assertThat(spans.getStyleSpan(0).getStyle()).isEmpty();
        }

        @Test
        @DisplayName("should handle complex expression with mixed tokens")
        void shouldHandleComplexExpression() {
            String expr = "SMOOTH(Population * 0.03, Adjustment_Time)";
            StyleSpans<Collection<String>> spans =
                    EquationHighlighter.computeHighlighting(expr);

            assertThat(spans).isNotNull();
            assertThat(spans.length()).isEqualTo(expr.length());

            // First token should be keyword SMOOTH
            assertThat(spans.getStyleSpan(0).getStyle()).contains("keyword");
        }

        @Test
        @DisplayName("should highlight comma separator")
        void shouldHighlightComma() {
            StyleSpans<Collection<String>> spans =
                    EquationHighlighter.computeHighlighting("a,b");

            boolean foundComma = false;
            for (var span : spans) {
                if (span.getStyle().contains("comma")) {
                    foundComma = true;
                    break;
                }
            }
            assertThat(foundComma).isTrue();
        }

        @Test
        @DisplayName("should highlight comparison operators")
        void shouldHighlightComparisonOperators() {
            StyleSpans<Collection<String>> spans =
                    EquationHighlighter.computeHighlighting("a >= b");

            boolean foundOperator = false;
            for (var span : spans) {
                if (span.getStyle().contains("operator")) {
                    foundOperator = true;
                    break;
                }
            }
            assertThat(foundOperator).isTrue();
        }
    }
}
