package systems.courant.sd.model.def;

import java.util.Arrays;
import java.util.Objects;

/**
 * Definition of a lookup table (nonlinear table function) in a model.
 *
 * @param name the table name (referenced in formulas as {@code LOOKUP(name, input)})
 * @param comment optional description
 * @param xValues the x-axis data points
 * @param yValues the y-axis data points (same length as xValues)
 * @param interpolation the interpolation method: "LINEAR" or "SPLINE"
 * @param unit the output unit of the lookup table, or {@code null} if unspecified
 */
public record LookupTableDef(
        String name,
        String comment,
        double[] xValues,
        double[] yValues,
        String interpolation,
        String unit
) implements ElementDef {

    public LookupTableDef {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("LookupTable name must not be blank");
        }
        if (xValues == null || yValues == null) {
            throw new IllegalArgumentException("LookupTable values must not be null");
        }
        if (xValues.length != yValues.length) {
            throw new IllegalArgumentException(
                    "xValues and yValues must have the same length");
        }
        if (xValues.length < 2) {
            throw new IllegalArgumentException(
                    "LookupTable must have at least 2 data points");
        }
        for (double v : xValues) {
            if (Double.isNaN(v) || Double.isInfinite(v)) {
                throw new IllegalArgumentException("xValues must be finite, got " + v);
            }
        }
        for (double v : yValues) {
            if (Double.isNaN(v) || Double.isInfinite(v)) {
                throw new IllegalArgumentException("yValues must be finite, got " + v);
            }
        }
        for (int i = 1; i < xValues.length; i++) {
            if (xValues[i] <= xValues[i - 1]) {
                throw new IllegalArgumentException(
                        "xValues must be strictly increasing, but xValues[" + (i - 1) + "]="
                                + xValues[i - 1] + " >= xValues[" + i + "]=" + xValues[i]);
            }
        }
        if (interpolation == null || interpolation.isBlank()) {
            throw new IllegalArgumentException("Interpolation method must not be blank");
        }
        if (!"LINEAR".equalsIgnoreCase(interpolation)
                && !"SPLINE".equalsIgnoreCase(interpolation)) {
            throw new IllegalArgumentException(
                    "Interpolation must be 'LINEAR' or 'SPLINE', got '" + interpolation + "'");
        }
        xValues = xValues.clone();
        yValues = yValues.clone();
    }

    /**
     * Returns a defensive copy of the x-axis data points.
     */
    @Override
    public double[] xValues() {
        return xValues.clone();
    }

    /**
     * Returns a defensive copy of the y-axis data points.
     */
    @Override
    public double[] yValues() {
        return yValues.clone();
    }

    /**
     * Backward-compatible constructor without unit.
     *
     * @param name          the table name
     * @param comment       optional description
     * @param xValues       the x-axis data points (must be strictly increasing)
     * @param yValues       the y-axis data points (same length as xValues)
     * @param interpolation the interpolation method: {@code "LINEAR"} or {@code "SPLINE"}
     */
    public LookupTableDef(String name, String comment, double[] xValues, double[] yValues,
                           String interpolation) {
        this(name, comment, xValues, yValues, interpolation, null);
    }

    /**
     * Convenience constructor without comment or unit.
     *
     * @param name          the table name
     * @param xValues       the x-axis data points (must be strictly increasing)
     * @param yValues       the y-axis data points (same length as xValues)
     * @param interpolation the interpolation method: {@code "LINEAR"} or {@code "SPLINE"}
     */
    public LookupTableDef(String name, double[] xValues, double[] yValues, String interpolation) {
        this(name, null, xValues, yValues, interpolation, null);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof LookupTableDef that)) {
            return false;
        }
        return Objects.equals(name, that.name)
                && Objects.equals(comment, that.comment)
                && Arrays.equals(xValues, that.xValues)
                && Arrays.equals(yValues, that.yValues)
                && Objects.equals(interpolation, that.interpolation)
                && Objects.equals(unit, that.unit);
    }

    @Override
    public int hashCode() {
        int result = Objects.hash(name, comment, interpolation, unit);
        result = 31 * result + Arrays.hashCode(xValues);
        result = 31 * result + Arrays.hashCode(yValues);
        return result;
    }
}
