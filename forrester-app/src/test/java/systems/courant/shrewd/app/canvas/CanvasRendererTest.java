package systems.courant.shrewd.app.canvas;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

@DisplayName("FlowGeometry")
class CanvasRendererTest {

    @Nested
    @DisplayName("clipToBorder")
    class ClipToBorder {

        @Test
        void shouldClipToRightEdge() {
            FlowGeometry.Point2D result = FlowGeometry.clipToBorder(100, 100, 50, 30, 200, 100);

            assertThat(result.x()).isCloseTo(150, within(0.001));
            assertThat(result.y()).isCloseTo(100, within(0.001));
        }

        @Test
        void shouldClipToLeftEdge() {
            FlowGeometry.Point2D result = FlowGeometry.clipToBorder(100, 100, 50, 30, 0, 100);

            assertThat(result.x()).isCloseTo(50, within(0.001));
            assertThat(result.y()).isCloseTo(100, within(0.001));
        }

        @Test
        void shouldClipToTopEdge() {
            FlowGeometry.Point2D result = FlowGeometry.clipToBorder(100, 100, 50, 30, 100, 0);

            assertThat(result.x()).isCloseTo(100, within(0.001));
            assertThat(result.y()).isCloseTo(70, within(0.001));
        }

        @Test
        void shouldClipToBottomEdge() {
            FlowGeometry.Point2D result = FlowGeometry.clipToBorder(100, 100, 50, 30, 100, 200);

            assertThat(result.x()).isCloseTo(100, within(0.001));
            assertThat(result.y()).isCloseTo(130, within(0.001));
        }

        @Test
        void shouldClipDiagonalToCorrectEdge() {
            // Target at 45 degrees — wide rect should clip to side
            FlowGeometry.Point2D result = FlowGeometry.clipToBorder(100, 100, 70, 30, 200, 200);

            // dx=100, dy=100. scaleX = 70/100 = 0.7, scaleY = 30/100 = 0.3
            // min scale = 0.3, so clipped to top/bottom edge
            assertThat(result.x()).isCloseTo(130, within(0.001));
            assertThat(result.y()).isCloseTo(130, within(0.001));
        }

        @Test
        void shouldReturnCenterWhenTargetIsCenter() {
            FlowGeometry.Point2D result = FlowGeometry.clipToBorder(100, 100, 50, 30, 100, 100);

            assertThat(result.x()).isCloseTo(100, within(0.001));
            assertThat(result.y()).isCloseTo(100, within(0.001));
        }
    }

    @Nested
    @DisplayName("clipToRhombus")
    class ClipToRhombus {

        @Test
        void shouldClipToRightVertex() {
            // Target directly to the right: scale = 1 / (|15|/15 + 0/15) = 1
            FlowGeometry.Point2D result = FlowGeometry.clipToRhombus(100, 100, 15, 15, 200, 100);

            assertThat(result.x()).isCloseTo(115, within(0.001));
            assertThat(result.y()).isCloseTo(100, within(0.001));
        }

        @Test
        void shouldClipToTopVertex() {
            FlowGeometry.Point2D result = FlowGeometry.clipToRhombus(100, 100, 15, 15, 100, 0);

            assertThat(result.x()).isCloseTo(100, within(0.001));
            assertThat(result.y()).isCloseTo(85, within(0.001));
        }

        @Test
        void shouldClipDiagonalToRhombusEdge() {
            // 45-degree target: scale = 1 / (1/15 + 1/15) = 15/2 = 7.5
            // Clipped point = (100 + 7.5, 100 + 7.5) = (107.5, 107.5)
            FlowGeometry.Point2D result = FlowGeometry.clipToRhombus(100, 100, 15, 15, 200, 200);

            assertThat(result.x()).isCloseTo(107.5, within(0.001));
            assertThat(result.y()).isCloseTo(107.5, within(0.001));
        }

        @Test
        void shouldDifferFromRectangularClipOnDiagonal() {
            // For a 30x30 diamond, rectangular clip at 45 degrees gives (115, 115)
            // Rhombus clip at 45 degrees gives (107.5, 107.5) — closer to center
            FlowGeometry.Point2D rect = FlowGeometry.clipToBorder(100, 100, 15, 15, 200, 200);
            FlowGeometry.Point2D rhombus = FlowGeometry.clipToRhombus(100, 100, 15, 15, 200, 200);

            // Rhombus point should be strictly closer to center
            double rectDist = Math.sqrt(Math.pow(rect.x() - 100, 2) + Math.pow(rect.y() - 100, 2));
            double rhombusDist = Math.sqrt(Math.pow(rhombus.x() - 100, 2) + Math.pow(rhombus.y() - 100, 2));
            assertThat(rhombusDist).isLessThan(rectDist);
        }

        @Test
        void shouldReturnCenterWhenTargetIsCenter() {
            FlowGeometry.Point2D result = FlowGeometry.clipToRhombus(100, 100, 15, 15, 100, 100);

            assertThat(result.x()).isCloseTo(100, within(0.001));
            assertThat(result.y()).isCloseTo(100, within(0.001));
        }
    }
}
