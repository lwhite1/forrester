package systems.courant.sd.app.canvas;

import javafx.scene.text.Font;
import javafx.scene.text.Text;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("ElementRenderer")
class ElementRendererTest {

    @Nested
    @DisplayName("isDisplayableEquation")
    class IsDisplayableEquation {

        @Test
        void shouldRejectNull() {
            assertThat(ElementRenderer.isDisplayableEquation(null)).isFalse();
        }

        @Test
        void shouldRejectBlank() {
            assertThat(ElementRenderer.isDisplayableEquation("")).isFalse();
            assertThat(ElementRenderer.isDisplayableEquation("   ")).isFalse();
        }

        @Test
        void shouldRejectDefaultZero() {
            assertThat(ElementRenderer.isDisplayableEquation("0")).isFalse();
            assertThat(ElementRenderer.isDisplayableEquation(" 0 ")).isFalse();
        }

        @Test
        void shouldAcceptRealEquation() {
            assertThat(ElementRenderer.isDisplayableEquation("Stock_1 * 0.5")).isTrue();
        }

        @Test
        void shouldAcceptZeroInExpression() {
            assertThat(ElementRenderer.isDisplayableEquation("0.5")).isTrue();
            assertThat(ElementRenderer.isDisplayableEquation("x + 0")).isTrue();
        }
    }

    @Nested
    @DisplayName("Badge labels")
    class BadgeLabels {

        @Test
        void shouldUseFullWordForLookupBadge() {
            assertThat(ElementRenderer.BADGE_LOOKUP).isEqualTo("Table");
        }

        @Test
        void shouldUseFullWordForModuleBadge() {
            assertThat(ElementRenderer.BADGE_MODULE).isEqualTo("Module");
        }

        @Test
        void shouldUseShortLabelForFormulaBadge() {
            assertThat(ElementRenderer.BADGE_FORMULA).isEqualTo("fx");
        }
    }

    @Nested
    @DisplayName("formatValue")
    class FormatValue {

        @Test
        void shouldFormatWholeNumberWithoutDecimal() {
            assertThat(ElementRenderer.formatValue(42.0)).isEqualTo("42");
        }

        @Test
        void shouldFormatZero() {
            assertThat(ElementRenderer.formatValue(0.0)).isEqualTo("0");
        }

        @Test
        void shouldFormatNegativeWholeNumber() {
            assertThat(ElementRenderer.formatValue(-5.0)).isEqualTo("-5");
        }

        @Test
        void shouldPreserveDecimalForFractionalValues() {
            assertThat(ElementRenderer.formatValue(3.14)).isEqualTo("3.14");
        }

        @Test
        void shouldPreserveSmallDecimal() {
            assertThat(ElementRenderer.formatValue(0.1)).isEqualTo("0.1");
        }

        @Test
        void shouldFormatLargeWholeNumber() {
            assertThat(ElementRenderer.formatValue(1000.0)).isEqualTo("1000");
        }

        @Test
        @DisplayName("should handle NaN (#467)")
        void shouldFormatNaN() {
            assertThat(ElementRenderer.formatValue(Double.NaN)).isEqualTo("NaN");
        }

        @Test
        @DisplayName("should handle positive infinity (#467)")
        void shouldFormatPositiveInfinity() {
            assertThat(ElementRenderer.formatValue(Double.POSITIVE_INFINITY))
                    .isEqualTo(String.valueOf(Double.POSITIVE_INFINITY));
        }

        @Test
        @DisplayName("should handle negative infinity (#467)")
        void shouldFormatNegativeInfinity() {
            assertThat(ElementRenderer.formatValue(Double.NEGATIVE_INFINITY))
                    .isEqualTo(String.valueOf(Double.NEGATIVE_INFINITY));
        }

        @Test
        @DisplayName("should not overflow for values exceeding Long.MAX_VALUE (#467)")
        void shouldNotOverflowForHugeValues() {
            double huge = 1e19; // > Long.MAX_VALUE (~9.2e18)
            String result = ElementRenderer.formatValue(huge);
            assertThat(result).isEqualTo(String.valueOf(huge));
            assertThat(result).doesNotContain("-"); // would be negative if overflow occurred
        }

        @Test
        @DisplayName("should not overflow for values below -Long.MAX_VALUE (#467)")
        void shouldNotOverflowForHugeNegativeValues() {
            double hugeNeg = -1e19;
            String result = ElementRenderer.formatValue(hugeNeg);
            assertThat(result).isEqualTo(String.valueOf(hugeNeg));
        }

        @Test
        @DisplayName("should still format whole numbers at Long.MAX_VALUE boundary (#467)")
        void shouldFormatAtLongMaxBoundary() {
            // Long.MAX_VALUE is 9223372036854775807 — exact double representation may differ
            // but values well within range should still format as integers
            assertThat(ElementRenderer.formatValue(1e15)).isEqualTo("1000000000000000");
        }
    }

    @Nested
    @DisplayName("MEASURE_TEXT reuse (#311)")
    class MeasureTextReuse {

        @Test
        void shouldHaveStaticFinalMeasureTextField() throws NoSuchFieldException {
            Field field = ElementRenderer.class.getDeclaredField("MEASURE_TEXT");
            assertThat(Modifier.isStatic(field.getModifiers()))
                    .as("MEASURE_TEXT should be static").isTrue();
            assertThat(Modifier.isFinal(field.getModifiers()))
                    .as("MEASURE_TEXT should be final").isTrue();
            assertThat(field.getType())
                    .as("MEASURE_TEXT should be a Text node").isEqualTo(Text.class);
        }

        @Test
        void shouldReuseTheSameTextNodeAcrossCalls() throws Exception {
            Field field = ElementRenderer.class.getDeclaredField("MEASURE_TEXT");
            field.setAccessible(true);
            Text first = (Text) field.get(null);
            Text second = (Text) field.get(null);
            assertThat(first).isSameAs(second);
        }
    }

    @Nested
    @DisplayName("Status update decoupled from redraw (#312)")
    class StatusDecoupled {

        @Test
        void shouldExposeFireStatusChangedAsPackagePrivate() throws NoSuchMethodException {
            var method = ModelCanvas.class.getDeclaredMethod("fireStatusChanged");
            int mods = method.getModifiers();
            // Package-private: not public, not protected, not private
            assertThat(Modifier.isPublic(mods)).as("fireStatusChanged should not be public").isFalse();
            assertThat(Modifier.isProtected(mods)).as("fireStatusChanged should not be protected").isFalse();
            assertThat(Modifier.isPrivate(mods)).as("fireStatusChanged should not be private").isFalse();
        }
    }

    @Nested
    @DisplayName("wrapText (#459)")
    class WrapText {

        private final Font font = LayoutMetrics.COMMENT_TEXT_FONT;

        @Test
        void shouldReturnEmptyListForNull() {
            List<String> lines = ElementRenderer.wrapText(null, font, 200);
            assertThat(lines).isEmpty();
        }

        @Test
        void shouldReturnEmptyListForEmptyString() {
            List<String> lines = ElementRenderer.wrapText("", font, 200);
            assertThat(lines).isEmpty();
        }

        @Test
        void shouldReturnSingleLineForShortText() {
            List<String> lines = ElementRenderer.wrapText("Hello", font, 200);
            assertThat(lines).containsExactly("Hello");
        }

        @Test
        void shouldRespectExplicitNewlines() {
            List<String> lines = ElementRenderer.wrapText("Line1\nLine2\nLine3", font, 500);
            assertThat(lines).containsExactly("Line1", "Line2", "Line3");
        }

        @Test
        void shouldPreserveEmptyLinesFromNewlines() {
            List<String> lines = ElementRenderer.wrapText("Above\n\nBelow", font, 500);
            assertThat(lines).containsExactly("Above", "", "Below");
        }

        @Test
        void shouldWrapLongTextToMultipleLines() {
            // Use a very narrow width to force wrapping
            List<String> lines = ElementRenderer.wrapText("one two three four five", font, 30);
            assertThat(lines.size()).isGreaterThan(1);
            // All original words should appear across the lines
            String rejoined = String.join(" ", lines);
            assertThat(rejoined).isEqualTo("one two three four five");
        }
    }

    @Nested
    @DisplayName("measureLineHeight (#459)")
    class MeasureLineHeight {

        @Test
        void shouldReturnPositiveHeight() {
            double height = ElementRenderer.measureLineHeight(LayoutMetrics.COMMENT_TEXT_FONT);
            assertThat(height).isGreaterThan(0);
        }

        @Test
        void shouldReturnConsistentResults() {
            double h1 = ElementRenderer.measureLineHeight(LayoutMetrics.COMMENT_TEXT_FONT);
            double h2 = ElementRenderer.measureLineHeight(LayoutMetrics.COMMENT_TEXT_FONT);
            assertThat(h1).isEqualTo(h2);
        }
    }

    @Nested
    @DisplayName("computeCommentSize (#459)")
    class ComputeCommentSize {

        @Test
        void shouldReturnMinSizeForNullText() {
            double[] size = ElementRenderer.computeCommentSize(null);
            assertThat(size).hasSize(2);
            assertThat(size[0]).isGreaterThan(0);
            assertThat(size[1]).isGreaterThan(0);
        }

        @Test
        void shouldReturnMinSizeForBlankText() {
            double[] size = ElementRenderer.computeCommentSize("   ");
            assertThat(size).hasSize(2);
            assertThat(size[0]).isGreaterThan(0);
            assertThat(size[1]).isGreaterThan(0);
        }

        @Test
        void shouldReturnCompactSizeForShortText() {
            double[] size = ElementRenderer.computeCommentSize("Hi");
            assertThat(size[0]).isLessThanOrEqualTo(LayoutMetrics.COMMENT_WIDTH);
        }

        @Test
        void shouldGrowHeightForMultilineText() {
            double[] shortSize = ElementRenderer.computeCommentSize("Short");
            double[] longSize = ElementRenderer.computeCommentSize(
                    "This is a much longer comment\nthat spans multiple lines\nand should be taller");
            assertThat(longSize[1]).isGreaterThanOrEqualTo(shortSize[1]);
        }

        @Test
        void shouldNotExceedCommentWidth() {
            double[] size = ElementRenderer.computeCommentSize(
                    "A very long comment text that goes on and on and should be clamped to max width");
            assertThat(size[0]).isLessThanOrEqualTo(LayoutMetrics.COMMENT_WIDTH);
        }
    }
}
