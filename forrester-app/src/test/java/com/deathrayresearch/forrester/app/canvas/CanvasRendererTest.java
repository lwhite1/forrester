package com.deathrayresearch.forrester.app.canvas;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

@DisplayName("CanvasRenderer")
class CanvasRendererTest {

    @Nested
    @DisplayName("clipToBorder")
    class ClipToBorder {

        @Test
        void shouldClipToRightEdge() {
            double[] result = CanvasRenderer.clipToBorder(100, 100, 50, 30, 200, 100);

            assertThat(result[0]).isCloseTo(150, within(0.001));
            assertThat(result[1]).isCloseTo(100, within(0.001));
        }

        @Test
        void shouldClipToLeftEdge() {
            double[] result = CanvasRenderer.clipToBorder(100, 100, 50, 30, 0, 100);

            assertThat(result[0]).isCloseTo(50, within(0.001));
            assertThat(result[1]).isCloseTo(100, within(0.001));
        }

        @Test
        void shouldClipToTopEdge() {
            double[] result = CanvasRenderer.clipToBorder(100, 100, 50, 30, 100, 0);

            assertThat(result[0]).isCloseTo(100, within(0.001));
            assertThat(result[1]).isCloseTo(70, within(0.001));
        }

        @Test
        void shouldClipToBottomEdge() {
            double[] result = CanvasRenderer.clipToBorder(100, 100, 50, 30, 100, 200);

            assertThat(result[0]).isCloseTo(100, within(0.001));
            assertThat(result[1]).isCloseTo(130, within(0.001));
        }

        @Test
        void shouldClipDiagonalToCorrectEdge() {
            // Target at 45 degrees — wide rect should clip to side
            double[] result = CanvasRenderer.clipToBorder(100, 100, 70, 30, 200, 200);

            // dx=100, dy=100. scaleX = 70/100 = 0.7, scaleY = 30/100 = 0.3
            // min scale = 0.3, so clipped to top/bottom edge
            assertThat(result[0]).isCloseTo(130, within(0.001));
            assertThat(result[1]).isCloseTo(130, within(0.001));
        }

        @Test
        void shouldReturnCenterWhenTargetIsCenter() {
            double[] result = CanvasRenderer.clipToBorder(100, 100, 50, 30, 100, 100);

            assertThat(result[0]).isCloseTo(100, within(0.001));
            assertThat(result[1]).isCloseTo(100, within(0.001));
        }
    }
}
