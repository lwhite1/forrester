package systems.courant.sd.app.canvas;

import javafx.scene.paint.Color;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

@DisplayName("ColorPalette (#77)")
class ColorPaletteTest {

    @Nested
    @DisplayName("Selection/interaction color family (#77)")
    class SelectionColorFamily {

        @Test
        @DisplayName("All selection colors share the same base hue #4A90D9")
        void allSelectionColorsShareBaseHue() {
            Color base = Color.web("#4A90D9");
            double expectedRed = base.getRed();
            double expectedGreen = base.getGreen();
            double expectedBlue = base.getBlue();

            assertColorRgb(ColorPalette.SELECTION, expectedRed, expectedGreen, expectedBlue);
            assertColorRgb(ColorPalette.RUBBER_BAND, expectedRed, expectedGreen, expectedBlue);
            assertColorRgb(ColorPalette.HOVER, expectedRed, expectedGreen, expectedBlue);
            assertColorRgb(ColorPalette.MARQUEE_FILL, expectedRed, expectedGreen, expectedBlue);
            assertColorRgb(ColorPalette.PORT_HOVER, expectedRed, expectedGreen, expectedBlue);
            assertColorRgb(ColorPalette.SAME_DIRECTION, expectedRed, expectedGreen, expectedBlue);
        }

        @Test
        @DisplayName("Selection has highest opacity (0.8)")
        void selectionHighestOpacity() {
            assertThat(ColorPalette.SELECTION.getOpacity()).isCloseTo(0.8, within(1e-6));
        }

        @Test
        @DisplayName("Rubber band opacity is 0.6")
        void rubberBandOpacity() {
            assertThat(ColorPalette.RUBBER_BAND.getOpacity()).isCloseTo(0.6, within(1e-6));
        }

        @Test
        @DisplayName("Hover opacity is 0.4")
        void hoverOpacity() {
            assertThat(ColorPalette.HOVER.getOpacity()).isCloseTo(0.4, within(1e-6));
        }

        @Test
        @DisplayName("Port hover opacity is 0.35")
        void portHoverOpacity() {
            assertThat(ColorPalette.PORT_HOVER.getOpacity()).isCloseTo(0.35, within(1e-6));
        }

        @Test
        @DisplayName("Marquee fill has lowest opacity (0.1)")
        void marqueeFillLowestOpacity() {
            assertThat(ColorPalette.MARQUEE_FILL.getOpacity()).isCloseTo(0.1, within(1e-6));
        }
    }

    @Nested
    @DisplayName("Core element colors")
    class CoreElementColors {

        @Test
        @DisplayName("All required color constants are non-null")
        void allColorsNonNull() {
            assertThat(ColorPalette.STOCK_BORDER).isNotNull();
            assertThat(ColorPalette.STOCK_FILL).isNotNull();
            assertThat(ColorPalette.AUX_BORDER).isNotNull();
            assertThat(ColorPalette.AUX_LITERAL_BORDER).isNotNull();
            assertThat(ColorPalette.MATERIAL_FLOW).isNotNull();
            assertThat(ColorPalette.INFO_LINK).isNotNull();
            assertThat(ColorPalette.BACKGROUND).isNotNull();
            assertThat(ColorPalette.TEXT).isNotNull();
            assertThat(ColorPalette.TEXT_SECONDARY).isNotNull();
            assertThat(ColorPalette.CLOUD).isNotNull();
            assertThat(ColorPalette.COMMENT_FILL).isNotNull();
            assertThat(ColorPalette.COMMENT_BORDER).isNotNull();
            assertThat(ColorPalette.DELAY_BADGE).isNotNull();
            assertThat(ColorPalette.VARIABLE_FILL).isNotNull();
            assertThat(ColorPalette.LOOKUP_FILL).isNotNull();
            assertThat(ColorPalette.HOVER_FILL).isNotNull();
            assertThat(ColorPalette.COMMENT_TEXT).isNotNull();
            assertThat(ColorPalette.COMMENT_ACCENT).isNotNull();
        }

        @Test
        @DisplayName("Stock fill is a light blue tint (#EBF5FB)")
        void stockFillIsLightBlueTint() {
            assertThat(ColorPalette.STOCK_FILL).isEqualTo(Color.web("#EBF5FB"));
        }

        @Test
        @DisplayName("Element fill is white (non-stock elements)")
        void elementFillIsWhite() {
            assertThat(ColorPalette.ELEMENT_FILL).isEqualTo(Color.WHITE);
        }
    }

    private void assertColorRgb(Color actual, double expectedRed, double expectedGreen, double expectedBlue) {
        assertThat(actual.getRed()).as("red channel of " + actual).isEqualTo(expectedRed);
        assertThat(actual.getGreen()).as("green channel of " + actual).isEqualTo(expectedGreen);
        assertThat(actual.getBlue()).as("blue channel of " + actual).isEqualTo(expectedBlue);
    }
}
