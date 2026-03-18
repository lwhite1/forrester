package systems.courant.sd.app.canvas;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.data.Offset.offset;

@DisplayName("ArrowheadGeometry")
class ArrowheadGeometryTest {

    private static final double EPSILON = 1e-9;

    @Nested
    @DisplayName("fromLine")
    class FromLine {

        @Test
        void shouldReturnNullWhenDistanceLessThanOne() {
            ArrowheadGeometry result = ArrowheadGeometry.fromLine(0, 0, 0.5, 0, 10, 6);
            assertThat(result).isNull();
        }

        @Test
        void shouldReturnNullWhenCoincident() {
            ArrowheadGeometry result = ArrowheadGeometry.fromLine(5, 5, 5, 5, 10, 6);
            assertThat(result).isNull();
        }

        @Test
        void shouldComputeArrowheadPointingRight() {
            // Line from (0,0) to (100,0), arrowhead length=10, width=6
            ArrowheadGeometry ah = ArrowheadGeometry.fromLine(0, 0, 100, 0, 10, 6);

            assertThat(ah).isNotNull();
            assertThat(ah.tipX()).isCloseTo(100, offset(EPSILON));
            assertThat(ah.tipY()).isCloseTo(0, offset(EPSILON));
            // Base is 10 units behind the tip
            assertThat(ah.baseLeftX()).isCloseTo(90, offset(EPSILON));
            assertThat(ah.baseLeftY()).isCloseTo(3, offset(EPSILON));
            assertThat(ah.baseRightX()).isCloseTo(90, offset(EPSILON));
            assertThat(ah.baseRightY()).isCloseTo(-3, offset(EPSILON));
        }

        @Test
        void shouldComputeArrowheadPointingUp() {
            // Line from (0,100) to (0,0), arrowhead length=10, width=6
            ArrowheadGeometry ah = ArrowheadGeometry.fromLine(0, 100, 0, 0, 10, 6);

            assertThat(ah).isNotNull();
            assertThat(ah.tipX()).isCloseTo(0, offset(EPSILON));
            assertThat(ah.tipY()).isCloseTo(0, offset(EPSILON));
            // Base is 10 units below the tip (in the +y direction)
            // perpX = -uy * w/2 = 3, perpY = ux * w/2 = 0
            assertThat(ah.baseLeftX()).isCloseTo(3, offset(EPSILON));
            assertThat(ah.baseLeftY()).isCloseTo(10, offset(EPSILON));
            assertThat(ah.baseRightX()).isCloseTo(-3, offset(EPSILON));
            assertThat(ah.baseRightY()).isCloseTo(10, offset(EPSILON));
        }

        @Test
        void shouldComputeArrowheadDiagonal() {
            // Line from (0,0) to (100,100), arrowhead length=10, width=6
            ArrowheadGeometry ah = ArrowheadGeometry.fromLine(0, 0, 100, 100, 10, 6);

            assertThat(ah).isNotNull();
            assertThat(ah.tipX()).isCloseTo(100, offset(EPSILON));
            assertThat(ah.tipY()).isCloseTo(100, offset(EPSILON));

            // Verify base center is 10 units behind tip along the line
            double baseCx = (ah.baseLeftX() + ah.baseRightX()) / 2;
            double baseCy = (ah.baseLeftY() + ah.baseRightY()) / 2;
            double distToTip = Math.sqrt(
                    Math.pow(100 - baseCx, 2) + Math.pow(100 - baseCy, 2));
            assertThat(distToTip).isCloseTo(10, offset(EPSILON));

            // Verify base width is 6
            double baseWidth = Math.sqrt(
                    Math.pow(ah.baseLeftX() - ah.baseRightX(), 2)
                    + Math.pow(ah.baseLeftY() - ah.baseRightY(), 2));
            assertThat(baseWidth).isCloseTo(6, offset(EPSILON));
        }
    }

    @Nested
    @DisplayName("fromTangent")
    class FromTangent {

        @Test
        void shouldReturnNullWhenTangentIsZero() {
            ArrowheadGeometry result = ArrowheadGeometry.fromTangent(50, 50, 0, 0, 10, 6);
            assertThat(result).isNull();
        }

        @Test
        void shouldReturnNullWhenTangentIsNearZero() {
            ArrowheadGeometry result = ArrowheadGeometry.fromTangent(50, 50, 1e-10, 0, 10, 6);
            assertThat(result).isNull();
        }

        @Test
        void shouldComputeArrowheadFromUnnormalizedTangent() {
            // Tangent (200, 0) should be normalized to (1, 0)
            ArrowheadGeometry ah = ArrowheadGeometry.fromTangent(100, 0, 200, 0, 10, 6);

            assertThat(ah).isNotNull();
            assertThat(ah.tipX()).isCloseTo(100, offset(EPSILON));
            assertThat(ah.tipY()).isCloseTo(0, offset(EPSILON));
            assertThat(ah.baseLeftX()).isCloseTo(90, offset(EPSILON));
        }

        @Test
        void shouldMatchFromLineForStraightSegments() {
            // For a straight horizontal line, fromTangent with tangent (1,0) should match fromLine
            ArrowheadGeometry fromLine = ArrowheadGeometry.fromLine(0, 0, 100, 0, 10, 6);
            ArrowheadGeometry fromTan = ArrowheadGeometry.fromTangent(100, 0, 1, 0, 10, 6);

            assertThat(fromLine).isNotNull();
            assertThat(fromTan).isNotNull();
            assertThat(fromTan.tipX()).isCloseTo(fromLine.tipX(), offset(EPSILON));
            assertThat(fromTan.tipY()).isCloseTo(fromLine.tipY(), offset(EPSILON));
            assertThat(fromTan.baseLeftX()).isCloseTo(fromLine.baseLeftX(), offset(EPSILON));
            assertThat(fromTan.baseLeftY()).isCloseTo(fromLine.baseLeftY(), offset(EPSILON));
            assertThat(fromTan.baseRightX()).isCloseTo(fromLine.baseRightX(), offset(EPSILON));
            assertThat(fromTan.baseRightY()).isCloseTo(fromLine.baseRightY(), offset(EPSILON));
        }
    }

    @Nested
    @DisplayName("lineStopPoint")
    class LineStopPoint {

        @Test
        void shouldReturnToPointWhenLineShorterThanArrow() {
            double[] stop = ArrowheadGeometry.lineStopPoint(0, 0, 5, 0, 10);
            assertThat(stop[0]).isCloseTo(5, offset(EPSILON));
            assertThat(stop[1]).isCloseTo(0, offset(EPSILON));
        }

        @Test
        void shouldReturnToPointWhenLineEqualsArrow() {
            double[] stop = ArrowheadGeometry.lineStopPoint(0, 0, 10, 0, 10);
            assertThat(stop[0]).isCloseTo(10, offset(EPSILON));
            assertThat(stop[1]).isCloseTo(0, offset(EPSILON));
        }

        @Test
        void shouldStopBeforeArrowheadBase() {
            double[] stop = ArrowheadGeometry.lineStopPoint(0, 0, 100, 0, 14);
            assertThat(stop[0]).isCloseTo(86, offset(EPSILON));
            assertThat(stop[1]).isCloseTo(0, offset(EPSILON));
        }

        @Test
        void shouldHandleVerticalLine() {
            double[] stop = ArrowheadGeometry.lineStopPoint(0, 0, 0, 100, 8);
            assertThat(stop[0]).isCloseTo(0, offset(EPSILON));
            assertThat(stop[1]).isCloseTo(92, offset(EPSILON));
        }

        @Test
        void shouldHandleDiagonalLine() {
            double[] stop = ArrowheadGeometry.lineStopPoint(0, 0, 100, 100, 10);
            // Direction is (1/sqrt(2), 1/sqrt(2)), stop point should be at
            // (100 - 10/sqrt(2), 100 - 10/sqrt(2))
            double offset45 = 10.0 / Math.sqrt(2);
            assertThat(stop[0]).isCloseTo(100 - offset45, offset(EPSILON));
            assertThat(stop[1]).isCloseTo(100 - offset45, offset(EPSILON));
        }
    }
}
