package systems.courant.sd.app.canvas;

import systems.courant.sd.model.def.ElementType;

import javafx.scene.text.Font;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.testfx.framework.junit5.ApplicationExtension;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("LayoutMetrics")
@ExtendWith(ApplicationExtension.class)
class LayoutMetricsTest {

    @Nested
    @DisplayName("font size constants (#67)")
    class FontSizeConstants {

        @Test
        @DisplayName("STOCK_NAME_FONT size matches STOCK_NAME_FONT_SIZE constant")
        void stockNameFontSizeMatchesConstant() {
            assertThat(LayoutMetrics.STOCK_NAME_FONT.getSize())
                    .isEqualTo(LayoutMetrics.STOCK_NAME_FONT_SIZE);
        }

        @Test
        @DisplayName("AUX_NAME_FONT size matches AUX_NAME_FONT_SIZE constant")
        void auxNameFontSizeMatchesConstant() {
            assertThat(LayoutMetrics.AUX_NAME_FONT.getSize())
                    .isEqualTo(LayoutMetrics.AUX_NAME_FONT_SIZE);
        }

        @Test
        @DisplayName("MODULE_NAME_FONT size matches MODULE_NAME_FONT_SIZE constant")
        void moduleNameFontSizeMatchesConstant() {
            assertThat(LayoutMetrics.MODULE_NAME_FONT.getSize())
                    .isEqualTo(LayoutMetrics.MODULE_NAME_FONT_SIZE);
        }

        @Test
        @DisplayName("BADGE_FONT size matches BADGE_FONT_SIZE constant")
        void badgeFontSizeMatchesConstant() {
            assertThat(LayoutMetrics.BADGE_FONT.getSize())
                    .isEqualTo(LayoutMetrics.BADGE_FONT_SIZE);
        }

        @Test
        @DisplayName("FLOW_NAME_FONT size matches FLOW_NAME_FONT_SIZE constant")
        void flowNameFontSizeMatchesConstant() {
            assertThat(LayoutMetrics.FLOW_NAME_FONT.getSize())
                    .isEqualTo(LayoutMetrics.FLOW_NAME_FONT_SIZE);
        }

        @Test
        @DisplayName("LOOKUP_NAME_FONT size matches LOOKUP_NAME_FONT_SIZE constant")
        void lookupNameFontSizeMatchesConstant() {
            assertThat(LayoutMetrics.LOOKUP_NAME_FONT.getSize())
                    .isEqualTo(LayoutMetrics.LOOKUP_NAME_FONT_SIZE);
        }

        @Test
        @DisplayName("COMMENT_TEXT_FONT size matches COMMENT_TEXT_FONT_SIZE constant")
        void commentTextFontSizeMatchesConstant() {
            assertThat(LayoutMetrics.COMMENT_TEXT_FONT.getSize())
                    .isEqualTo(LayoutMetrics.COMMENT_TEXT_FONT_SIZE);
        }

        @Test
        @DisplayName("CAUSAL_POLARITY_FONT size matches CAUSAL_POLARITY_FONT_SIZE constant")
        void causalPolarityFontSizeMatchesConstant() {
            assertThat(LayoutMetrics.CAUSAL_POLARITY_FONT.getSize())
                    .isEqualTo(LayoutMetrics.CAUSAL_POLARITY_FONT_SIZE);
        }

        @Test
        @DisplayName("all font size constants are positive")
        void allFontSizeConstantsArePositive() {
            assertThat(LayoutMetrics.STOCK_NAME_FONT_SIZE).isPositive();
            assertThat(LayoutMetrics.AUX_NAME_FONT_SIZE).isPositive();
            assertThat(LayoutMetrics.MODULE_NAME_FONT_SIZE).isPositive();
            assertThat(LayoutMetrics.BADGE_FONT_SIZE).isPositive();
            assertThat(LayoutMetrics.FLOW_NAME_FONT_SIZE).isPositive();
            assertThat(LayoutMetrics.LOOKUP_NAME_FONT_SIZE).isPositive();
            assertThat(LayoutMetrics.COMMENT_TEXT_FONT_SIZE).isPositive();
            assertThat(LayoutMetrics.CAUSAL_POLARITY_FONT_SIZE).isPositive();
        }
    }

    @Nested
    @DisplayName("loop rendering constants (#67)")
    class LoopConstants {

        @Test
        @DisplayName("LOOP_GLOW_PADDING is positive")
        void loopGlowPaddingIsPositive() {
            assertThat(LayoutMetrics.LOOP_GLOW_PADDING).isPositive();
        }

        @Test
        @DisplayName("LOOP_GLOW_LINE_WIDTH is positive")
        void loopGlowLineWidthIsPositive() {
            assertThat(LayoutMetrics.LOOP_GLOW_LINE_WIDTH).isPositive();
        }

        @Test
        @DisplayName("LOOP_EDGE_LINE_WIDTH is positive")
        void loopEdgeLineWidthIsPositive() {
            assertThat(LayoutMetrics.LOOP_EDGE_LINE_WIDTH).isPositive();
        }

        @Test
        @DisplayName("LOOP_LABEL_FONT_SIZE is positive")
        void loopLabelFontSizeIsPositive() {
            assertThat(LayoutMetrics.LOOP_LABEL_FONT_SIZE).isPositive();
        }

        @Test
        @DisplayName("LOOP_LABEL_PADDING is positive")
        void loopLabelPaddingIsPositive() {
            assertThat(LayoutMetrics.LOOP_LABEL_PADDING).isPositive();
        }
    }

    @Nested
    @DisplayName("widthFor and heightFor")
    class DimensionLookups {

        @Test
        @DisplayName("widthFor returns positive values for all element types")
        void widthForAllTypes() {
            for (ElementType type : ElementType.values()) {
                assertThat(LayoutMetrics.widthFor(type))
                        .as("width for %s", type)
                        .isPositive();
            }
        }

        @Test
        @DisplayName("heightFor returns positive values for all element types")
        void heightForAllTypes() {
            for (ElementType type : ElementType.values()) {
                assertThat(LayoutMetrics.heightFor(type))
                        .as("height for %s", type)
                        .isPositive();
            }
        }

        @Test
        @DisplayName("minWidthFor is less than or equal to widthFor for box-shaped types")
        void minWidthNotGreaterThanDefault() {
            for (ElementType type : ElementType.values()) {
                if (type == ElementType.FLOW) {
                    continue; // FLOW is a diamond; min resize width exceeds indicator size
                }
                assertThat(LayoutMetrics.minWidthFor(type))
                        .as("minWidth vs width for %s", type)
                        .isLessThanOrEqualTo(LayoutMetrics.widthFor(type));
            }
        }

        @Test
        @DisplayName("minHeightFor is less than or equal to heightFor for box-shaped types")
        void minHeightNotGreaterThanDefault() {
            for (ElementType type : ElementType.values()) {
                if (type == ElementType.FLOW) {
                    continue; // FLOW is a diamond; min resize height exceeds indicator size
                }
                assertThat(LayoutMetrics.minHeightFor(type))
                        .as("minHeight vs height for %s", type)
                        .isLessThanOrEqualTo(LayoutMetrics.heightFor(type));
            }
        }
    }
}
