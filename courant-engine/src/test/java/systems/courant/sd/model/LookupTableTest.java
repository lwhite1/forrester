package systems.courant.sd.model;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static systems.courant.sd.measure.Units.DIMENSIONLESS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class LookupTableTest {

    private static final double[] X = {0, 1, 2, 3, 4};
    private static final double[] Y = {0, 10, 20, 30, 40};

    @Nested
    class LinearInterpolation {

        @Test
        public void shouldReturnInterpolatedValueForMidpoint() {
            LookupTable table = LookupTable.linear(X, Y, () -> 1.5);
            assertEquals(15.0, table.getCurrentValue(), 0.01);
        }

        @Test
        public void shouldReturnExactValueAtDataPoint() {
            LookupTable table = LookupTable.linear(X, Y, () -> 2.0);
            assertEquals(20.0, table.getCurrentValue(), 0.01);
        }

        @Test
        public void shouldClampBelowRange() {
            LookupTable table = LookupTable.linear(X, Y, () -> -5.0);
            assertEquals(0.0, table.getCurrentValue(), 0.01);
        }

        @Test
        public void shouldClampAboveRange() {
            LookupTable table = LookupTable.linear(X, Y, () -> 100.0);
            assertEquals(40.0, table.getCurrentValue(), 0.01);
        }
    }

    @Nested
    class SplineInterpolation {

        private static final double[] SPLINE_X = {0, 1, 2, 3, 4};
        private static final double[] SPLINE_Y = {0, 1, 4, 9, 16};

        @Test
        public void shouldReturnInterpolatedValueForSpline() {
            LookupTable table = LookupTable.spline(SPLINE_X, SPLINE_Y, () -> 2.0);
            assertEquals(4.0, table.getCurrentValue(), 0.01);
        }

        @Test
        public void shouldClampBelowRangeSpline() {
            LookupTable table = LookupTable.spline(SPLINE_X, SPLINE_Y, () -> -1.0);
            assertEquals(0.0, table.getCurrentValue(), 0.01);
        }

        @Test
        public void shouldClampAboveRangeSpline() {
            LookupTable table = LookupTable.spline(SPLINE_X, SPLINE_Y, () -> 10.0);
            assertEquals(16.0, table.getCurrentValue(), 0.01);
        }
    }

    @Nested
    class BuilderTests {

        @Test
        public void shouldBuildFromUnorderedPoints() {
            LookupTable table = LookupTable.builder()
                    .at(3, 30)
                    .at(1, 10)
                    .at(4, 40)
                    .at(0, 0)
                    .at(2, 20)
                    .buildLinear(() -> 2.5);

            assertEquals(25.0, table.getCurrentValue(), 0.01);
        }
    }

    @Nested
    class Validation {

        @Test
        public void shouldRejectMismatchedArrayLengths() {
            assertThrows(IllegalArgumentException.class, () ->
                    LookupTable.linear(new double[]{0, 1}, new double[]{0, 1, 2}, () -> 0));
        }

        @Test
        public void shouldRejectSingleDataPoint() {
            assertThrows(IllegalArgumentException.class, () ->
                    LookupTable.linear(new double[]{0}, new double[]{0}, () -> 0));
        }

        @Test
        public void shouldRejectNonAscendingXValues() {
            assertThrows(IllegalArgumentException.class, () ->
                    LookupTable.linear(new double[]{0, 2, 1}, new double[]{0, 1, 2}, () -> 0));
        }

        @Test
        public void shouldRejectSplineWithTwoPoints() {
            assertThrows(IllegalArgumentException.class, () ->
                    LookupTable.spline(new double[]{0, 1}, new double[]{0, 1}, () -> 0));
        }
    }

    @Nested
    class Integration {

        @Test
        public void shouldWorkAsFormulaInVariable() {
            LookupTable table = LookupTable.linear(
                    new double[]{0, 50, 100},
                    new double[]{1.0, 0.5, 0.0},
                    () -> 25.0);

            Variable effect = new Variable("Effect", DIMENSIONLESS, table);
            assertEquals(0.75, effect.getValue(), 0.01);
        }
    }
}
