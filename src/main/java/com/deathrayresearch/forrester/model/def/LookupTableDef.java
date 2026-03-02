package com.deathrayresearch.forrester.model.def;

/**
 * Definition of a lookup table (nonlinear table function) in a model.
 *
 * @param name the table name (referenced in formulas as {@code LOOKUP(name, input)})
 * @param comment optional description
 * @param xValues the x-axis data points
 * @param yValues the y-axis data points (same length as xValues)
 * @param interpolation the interpolation method: "LINEAR" or "SPLINE"
 */
public record LookupTableDef(
        String name,
        String comment,
        double[] xValues,
        double[] yValues,
        String interpolation
) {

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
        if (interpolation == null || interpolation.isBlank()) {
            throw new IllegalArgumentException("Interpolation method must not be blank");
        }
        xValues = xValues.clone();
        yValues = yValues.clone();
    }

    public LookupTableDef(String name, double[] xValues, double[] yValues, String interpolation) {
        this(name, null, xValues, yValues, interpolation);
    }
}
