package com.deathrayresearch.forrester.app.canvas;

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
}
