package systems.courant.shrewd.app.canvas;

import javafx.scene.text.Text;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

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
}
