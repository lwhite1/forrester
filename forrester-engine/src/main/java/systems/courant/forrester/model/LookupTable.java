package systems.courant.forrester.model;

import com.google.common.base.Preconditions;
import org.apache.commons.math3.analysis.UnivariateFunction;
import org.apache.commons.math3.analysis.interpolation.LinearInterpolator;
import org.apache.commons.math3.analysis.interpolation.SplineInterpolator;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.DoubleSupplier;

/**
 * A piecewise interpolation curve that implements {@link Formula}, providing a standard
 * system dynamics "lookup table" (also called a "table function" or "graph function").
 *
 * <p>A lookup table maps an input value (supplied by a {@link DoubleSupplier}) to an output
 * value via interpolation between defined data points. This is the standard SD mechanism for
 * modeling nonlinear effects such as "effect of crowding on birth rate".
 *
 * <p>Two interpolation modes are supported:
 * <ul>
 *     <li><b>Linear</b> — straight-line segments between points. Use when the relationship
 *         is angular or when smoothness is not critical.</li>
 *     <li><b>Spline</b> — cubic spline interpolation producing a smooth curve. Use for
 *         naturally smooth relationships. <em>Note:</em> spline interpolation can overshoot
 *         or oscillate between data points, especially with steep transitions. If the curve
 *         misbehaves, prefer linear interpolation or add more data points.</li>
 * </ul>
 *
 * <p><b>Out-of-range handling:</b> Input values below the minimum x or above the maximum x
 * are clamped to the corresponding y value (standard SD convention). This prevents
 * {@link org.apache.commons.math3.exception.OutOfRangeException} from Commons Math.
 *
 * <p>Usage via static factories:
 * <pre>{@code
 * LookupTable effect = LookupTable.linear(
 *     new double[]{0, 0.5, 1.0, 1.5, 2.0},
 *     new double[]{1.2, 1.0, 0.5, 0.1, 0.0},
 *     () -> population.getValue() / carryingCapacity);
 * }</pre>
 *
 * <p>Usage via builder (auto-sorts by x):
 * <pre>{@code
 * LookupTable effect = LookupTable.builder()
 *     .at(0.0, 1.2)
 *     .at(0.5, 1.0)
 *     .at(1.0, 0.5)
 *     .buildLinear(() -> population.getValue() / carryingCapacity);
 * }</pre>
 */
public class LookupTable implements Formula {

    private final UnivariateFunction interpolation;
    private final DoubleSupplier inputSupplier;
    private final double xMin;
    private final double xMax;
    private final double yAtXMin;
    private final double yAtXMax;

    private LookupTable(UnivariateFunction interpolation, DoubleSupplier inputSupplier,
                        double[] xValues, double[] yValues) {
        this.interpolation = interpolation;
        this.inputSupplier = inputSupplier;
        this.xMin = xValues[0];
        this.xMax = xValues[xValues.length - 1];
        this.yAtXMin = yValues[0];
        this.yAtXMax = yValues[yValues.length - 1];
    }

    private LookupTable(UnivariateFunction interpolation, DoubleSupplier inputSupplier,
                        double xMin, double xMax, double yAtXMin, double yAtXMax) {
        this.interpolation = interpolation;
        this.inputSupplier = inputSupplier;
        this.xMin = xMin;
        this.xMax = xMax;
        this.yAtXMin = yAtXMin;
        this.yAtXMax = yAtXMax;
    }

    /**
     * Returns a new LookupTable that shares this table's interpolation function
     * but uses a different input supplier. This prevents cross-formula interference
     * when multiple formulas reference the same lookup table.
     */
    public LookupTable withInput(DoubleSupplier newInputSupplier) {
        return new LookupTable(interpolation, newInputSupplier, xMin, xMax, yAtXMin, yAtXMax);
    }

    /**
     * Creates a lookup table using linear interpolation between data points.
     *
     * @param xValues       the independent-variable breakpoints (must be strictly ascending)
     * @param yValues       the dependent-variable values at each breakpoint
     * @param inputSupplier supplies the current input value to look up
     * @return a new LookupTable
     * @throws IllegalArgumentException if arrays differ in length, have fewer than 2 points,
     *                                  or x values are not strictly ascending
     */
    public static LookupTable linear(double[] xValues, double[] yValues, DoubleSupplier inputSupplier) {
        validateArrays(xValues, yValues, 2);
        UnivariateFunction function = new LinearInterpolator().interpolate(xValues, yValues);
        return new LookupTable(function, inputSupplier, xValues, yValues);
    }

    /**
     * Creates a lookup table using cubic spline interpolation for a smooth curve.
     *
     * <p><em>Caveat:</em> Spline interpolation can overshoot or oscillate between data points,
     * especially with steep transitions. If the curve misbehaves, prefer linear interpolation
     * or add more data points to constrain the spline.
     *
     * @param xValues       the independent-variable breakpoints (must be strictly ascending)
     * @param yValues       the dependent-variable values at each breakpoint
     * @param inputSupplier supplies the current input value to look up
     * @return a new LookupTable
     * @throws IllegalArgumentException if arrays differ in length, have fewer than 3 points,
     *                                  or x values are not strictly ascending
     */
    public static LookupTable spline(double[] xValues, double[] yValues, DoubleSupplier inputSupplier) {
        validateArrays(xValues, yValues, 3);
        UnivariateFunction function = new SplineInterpolator().interpolate(xValues, yValues);
        return new LookupTable(function, inputSupplier, xValues, yValues);
    }

    /**
     * Returns a new builder for constructing a lookup table from individual points.
     * Points may be added in any order; they are sorted by x before interpolation.
     */
    public static Builder builder() {
        return new Builder();
    }

    @Override
    public double getCurrentValue() {
        double input = inputSupplier.getAsDouble();
        if (Double.isNaN(input)) {
            return Double.NaN;
        }
        if (input <= xMin) {
            return yAtXMin;
        }
        if (input >= xMax) {
            return yAtXMax;
        }
        return interpolation.value(input);
    }

    private static void validateArrays(double[] xValues, double[] yValues, int minPoints) {
        Preconditions.checkArgument(xValues.length == yValues.length,
                "x and y arrays must have the same length, but got %s and %s",
                xValues.length, yValues.length);
        Preconditions.checkArgument(xValues.length >= minPoints,
                "At least %s data points are required, but got %s",
                minPoints, xValues.length);
        for (int i = 1; i < xValues.length; i++) {
            Preconditions.checkArgument(xValues[i] > xValues[i - 1],
                    "x values must be strictly ascending, but x[%s]=%s is not greater than x[%s]=%s",
                    i, xValues[i], i - 1, xValues[i - 1]);
        }
    }

    /**
     * Fluent builder for constructing a {@link LookupTable} from individual (x, y) points.
     * Points may be added in any order; the builder sorts them by x before building.
     */
    public static class Builder {

        private final List<double[]> points = new ArrayList<>();

        private Builder() {}

        /**
         * Adds a data point to the lookup table.
         *
         * @param x the independent-variable value
         * @param y the dependent-variable value at x
         * @return this builder
         */
        public Builder at(double x, double y) {
            points.add(new double[]{x, y});
            return this;
        }

        /**
         * Builds a lookup table using linear interpolation.
         *
         * @param inputSupplier supplies the current input value to look up
         * @return a new LookupTable
         */
        public LookupTable buildLinear(DoubleSupplier inputSupplier) {
            double[][] sorted = sortedPoints();
            return LookupTable.linear(sorted[0], sorted[1], inputSupplier);
        }

        /**
         * Builds a lookup table using cubic spline interpolation.
         *
         * @param inputSupplier supplies the current input value to look up
         * @return a new LookupTable
         */
        public LookupTable buildSpline(DoubleSupplier inputSupplier) {
            double[][] sorted = sortedPoints();
            return LookupTable.spline(sorted[0], sorted[1], inputSupplier);
        }

        private double[][] sortedPoints() {
            points.sort(Comparator.comparingDouble(p -> p[0]));
            double[] x = new double[points.size()];
            double[] y = new double[points.size()];
            for (int i = 0; i < points.size(); i++) {
                x[i] = points.get(i)[0];
                y[i] = points.get(i)[1];
            }
            return new double[][]{x, y};
        }
    }
}
