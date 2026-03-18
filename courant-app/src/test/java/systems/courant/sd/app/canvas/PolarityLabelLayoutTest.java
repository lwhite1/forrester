package systems.courant.sd.app.canvas;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import systems.courant.sd.model.def.CausalLinkDef;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.data.Offset.offset;

@DisplayName("PolarityLabelLayout")
class PolarityLabelLayoutTest {

    private static final double EPSILON = 1e-6;

    @Nested
    @DisplayName("forQuadratic")
    class ForQuadratic {

        @Test
        void shouldReturnInvalidWhenEndpointsCoincident() {
            PolarityLabelLayout layout = PolarityLabelLayout.forQuadratic(
                    50, 50, 60, 40, 50, 50);
            assertThat(layout.valid()).isFalse();
        }

        @Test
        void shouldReturnValidForSeparatedEndpoints() {
            PolarityLabelLayout layout = PolarityLabelLayout.forQuadratic(
                    0, 0, 50, -30, 100, 0);
            assertThat(layout.valid()).isTrue();
        }

        @Test
        void shouldPlaceLabelOffsetFromCurve() {
            // Straight-ish curve from (0,0) to (100,0) with control point at (50, -30)
            PolarityLabelLayout layout = PolarityLabelLayout.forQuadratic(
                    0, 0, 50, -30, 100, 0);
            assertThat(layout.valid()).isTrue();
            // Label should be somewhere near t=0.8, offset perpendicularly
            assertThat(layout.x()).isGreaterThan(50);
            assertThat(layout.x()).isLessThan(120);
        }
    }

    @Nested
    @DisplayName("forSelfLoop")
    class ForSelfLoop {

        @Test
        void shouldReturnValidLayout() {
            double[] loopPts = CausalLinkGeometry.selfLoopPoints(100, 200, 30, 20);
            PolarityLabelLayout layout = PolarityLabelLayout.forSelfLoop(loopPts);
            assertThat(layout.valid()).isTrue();
        }

        @Test
        void shouldPlaceLabelAboveElement() {
            double[] loopPts = CausalLinkGeometry.selfLoopPoints(100, 200, 30, 20);
            PolarityLabelLayout layout = PolarityLabelLayout.forSelfLoop(loopPts);
            // Label should be above the element center (lower y value)
            assertThat(layout.y()).isLessThan(200);
        }
    }

    @Nested
    @DisplayName("colorFor")
    class ColorFor {

        @Test
        void shouldReturnPositiveColor() {
            assertThat(PolarityLabelLayout.colorFor(CausalLinkDef.Polarity.POSITIVE))
                    .isEqualTo(ColorPalette.CAUSAL_POSITIVE);
        }

        @Test
        void shouldReturnNegativeColor() {
            assertThat(PolarityLabelLayout.colorFor(CausalLinkDef.Polarity.NEGATIVE))
                    .isEqualTo(ColorPalette.CAUSAL_NEGATIVE);
        }

        @Test
        void shouldReturnUnknownColor() {
            assertThat(PolarityLabelLayout.colorFor(CausalLinkDef.Polarity.UNKNOWN))
                    .isEqualTo(ColorPalette.CAUSAL_UNKNOWN);
        }
    }
}
