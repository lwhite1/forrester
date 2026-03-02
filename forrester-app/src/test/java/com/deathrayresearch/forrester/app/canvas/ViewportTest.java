package com.deathrayresearch.forrester.app.canvas;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

@DisplayName("Viewport")
class ViewportTest {

    private Viewport viewport;

    @BeforeEach
    void setUp() {
        viewport = new Viewport();
    }

    @Nested
    @DisplayName("default state")
    class DefaultState {

        @Test
        void shouldHaveIdentityTransform() {
            assertThat(viewport.getTranslateX()).isEqualTo(0);
            assertThat(viewport.getTranslateY()).isEqualTo(0);
            assertThat(viewport.getScale()).isEqualTo(1.0);
        }

        @Test
        void shouldReturnSameCoordinatesForWorldAndScreen() {
            assertThat(viewport.toWorldX(100)).isEqualTo(100);
            assertThat(viewport.toWorldY(200)).isEqualTo(200);
            assertThat(viewport.toScreenX(100)).isEqualTo(100);
            assertThat(viewport.toScreenY(200)).isEqualTo(200);
        }
    }

    @Nested
    @DisplayName("coordinate transforms")
    class CoordinateTransforms {

        @Test
        void shouldRoundTripScreenToWorldAndBack() {
            viewport.pan(50, -30);
            viewport.zoomAt(200, 200, 2.0);

            double screenX = 300;
            double screenY = 400;

            double worldX = viewport.toWorldX(screenX);
            double worldY = viewport.toWorldY(screenY);
            double backToScreenX = viewport.toScreenX(worldX);
            double backToScreenY = viewport.toScreenY(worldY);

            assertThat(backToScreenX).isCloseTo(screenX, within(0.001));
            assertThat(backToScreenY).isCloseTo(screenY, within(0.001));
        }

        @Test
        void shouldRoundTripWorldToScreenAndBack() {
            viewport.pan(-100, 75);
            viewport.zoomAt(400, 300, 0.5);

            double worldX = 250;
            double worldY = 180;

            double screenX = viewport.toScreenX(worldX);
            double screenY = viewport.toScreenY(worldY);
            double backToWorldX = viewport.toWorldX(screenX);
            double backToWorldY = viewport.toWorldY(screenY);

            assertThat(backToWorldX).isCloseTo(worldX, within(0.001));
            assertThat(backToWorldY).isCloseTo(worldY, within(0.001));
        }

        @Test
        void shouldAccountForPanOffset() {
            viewport.pan(100, 50);

            // screen(0,0) should map to world(-100,-50)
            assertThat(viewport.toWorldX(0)).isCloseTo(-100, within(0.001));
            assertThat(viewport.toWorldY(0)).isCloseTo(-50, within(0.001));
        }
    }

    @Nested
    @DisplayName("pan")
    class Pan {

        @Test
        void shouldAccumulatePanDeltas() {
            viewport.pan(10, 20);
            viewport.pan(30, -5);

            assertThat(viewport.getTranslateX()).isEqualTo(40);
            assertThat(viewport.getTranslateY()).isEqualTo(15);
        }
    }

    @Nested
    @DisplayName("zoomAt")
    class ZoomAt {

        @Test
        void shouldKeepPivotPointStable() {
            double pivotScreenX = 400;
            double pivotScreenY = 300;

            double worldBeforeX = viewport.toWorldX(pivotScreenX);
            double worldBeforeY = viewport.toWorldY(pivotScreenY);

            viewport.zoomAt(pivotScreenX, pivotScreenY, 2.0);

            double worldAfterX = viewport.toWorldX(pivotScreenX);
            double worldAfterY = viewport.toWorldY(pivotScreenY);

            assertThat(worldAfterX).isCloseTo(worldBeforeX, within(0.001));
            assertThat(worldAfterY).isCloseTo(worldBeforeY, within(0.001));
        }

        @Test
        void shouldKeepPivotStableAfterMultipleZooms() {
            double pivotX = 300;
            double pivotY = 250;

            double worldX = viewport.toWorldX(pivotX);
            double worldY = viewport.toWorldY(pivotY);

            viewport.zoomAt(pivotX, pivotY, 1.5);
            viewport.zoomAt(pivotX, pivotY, 0.8);
            viewport.zoomAt(pivotX, pivotY, 2.0);

            assertThat(viewport.toWorldX(pivotX)).isCloseTo(worldX, within(0.001));
            assertThat(viewport.toWorldY(pivotY)).isCloseTo(worldY, within(0.001));
        }

        @Test
        void shouldClampScaleToMinimum() {
            // Zoom out aggressively — scale should not go below 0.1
            viewport.zoomAt(0, 0, 0.01);

            assertThat(viewport.getScale()).isGreaterThanOrEqualTo(0.1);
        }

        @Test
        void shouldClampScaleToMaximum() {
            // Zoom in aggressively — scale should not exceed 5.0
            viewport.zoomAt(0, 0, 100.0);

            assertThat(viewport.getScale()).isLessThanOrEqualTo(5.0);
        }

        @Test
        void shouldMultiplyScale() {
            viewport.zoomAt(0, 0, 2.0);
            assertThat(viewport.getScale()).isCloseTo(2.0, within(0.001));

            viewport.zoomAt(0, 0, 1.5);
            assertThat(viewport.getScale()).isCloseTo(3.0, within(0.001));
        }
    }
}
