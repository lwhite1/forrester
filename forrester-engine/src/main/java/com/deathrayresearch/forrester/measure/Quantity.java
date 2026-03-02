package com.deathrayresearch.forrester.measure;

import com.google.common.base.Preconditions;

/**
 * A dimension-aware amount of something. It consists of a dimension (like time, mass, length, money, etc.)
 * and a value (like 2.54). Individual values are expressed in Units (like hours), and the units have
 * the specific dimension (in this case time).
 * <p>
 * It lets you express things like 2.54 miles, and add that distance to 1,312 meters, handling the units conversion for
 * you. It also lets you compare two quantities in the same dimension to assert that 2.54 miles > 1,312 meters, again
 * handling any conversions, while preventing you from comparing 2.54 miles with 2.54 pounds.
 */
public final class Quantity {

    private static final String INCOMPATIBLE_ERROR_MESSAGE = "Combined quantities must have compatible units";

    private final double value;
    private final Unit unit;

    public Quantity(double value, Unit unit) {
        if (unit == null) {
            throw new IllegalArgumentException("unit must not be null");
        }
        this.value = value;
        this.unit = unit;
    }

    public double getValue() {
        return value;
    }

    public Unit getUnit() {
        return unit;
    }

    /**
     * Returns this quantity converted to the dimension's base unit.
     */
    public Quantity inBaseUnits() {
        return unit.toBaseUnits(this);
    }

    /**
     * Returns a new quantity whose value is this quantity's value multiplied by the given scalar.
     *
     * @param d the scalar multiplier
     */
    public Quantity multiply(double d) {
        return new Quantity(d * getValue(), this.getUnit());
    }

    /**
     * Returns a new quantity whose value is this quantity's value divided by the given scalar.
     *
     * @param d the divisor
     */
    public Quantity divide(double d) {
        if (d == 0) {
            throw new ArithmeticException("Cannot divide quantity by zero");
        }
        return new Quantity(getValue() / d, this.getUnit());
    }

    /**
     * Adds another quantity to this one, converting units through the base unit if necessary.
     * Both quantities must share the same dimension.
     *
     * @param other the quantity to add
     * @return the sum, expressed in this quantity's unit
     * @throws IllegalArgumentException if the dimensions are incompatible
     */
    public Quantity add(Quantity other) {
        Preconditions.checkArgument(other.isCompatibleWith(this), INCOMPATIBLE_ERROR_MESSAGE);

        Quantity otherInBaseUnits = other.inBaseUnits();
        Quantity thisInBaseUnits = inBaseUnits();
        Quantity result = new Quantity(otherInBaseUnits.getValue() + thisInBaseUnits.getValue(),
                this.getUnit().getBaseUnit());
        return getUnit().fromBaseUnits(result);
    }

    /**
     * Subtracts another quantity from this one, converting units through the base unit if necessary.
     * Both quantities must share the same dimension.
     *
     * @param other the quantity to subtract
     * @return the difference, expressed in this quantity's unit
     * @throws IllegalArgumentException if the dimensions are incompatible
     */
    public Quantity subtract(Quantity other) {
        Preconditions.checkArgument(other.isCompatibleWith(this), INCOMPATIBLE_ERROR_MESSAGE);

        Quantity otherInBaseUnits = other.inBaseUnits();
        Quantity thisInBaseUnits = inBaseUnits();
        Quantity result = new Quantity(thisInBaseUnits.getValue() - otherInBaseUnits.getValue(),
                this.getUnit().getBaseUnit());
        return getUnit().fromBaseUnits(result);
    }

    @Override
    public String toString() {
        return value +
                " " + unit.getName() +
                "(s)";
    }

    /**
     * Returns {@code true} if this quantity is strictly less than the other quantity.
     * When both quantities share the same unit, values are compared directly;
     * otherwise, both are converted to base units for comparison.
     */
    public boolean isLessThan(Quantity other) {
        Preconditions.checkArgument(other.isCompatibleWith(this), INCOMPATIBLE_ERROR_MESSAGE);
        if (unit.equals(other.unit)) {
            return Double.compare(value, other.value) < 0;
        }
        return Double.compare(inBaseUnits().getValue(), other.inBaseUnits().getValue()) < 0;
    }

    /**
     * Returns {@code true} if this quantity is less than or equal to the other quantity.
     * When both quantities share the same unit, values are compared directly;
     * otherwise, both are converted to base units for comparison.
     */
    public boolean isLessThanOrEqualTo(Quantity other) {
        Preconditions.checkArgument(other.isCompatibleWith(this), INCOMPATIBLE_ERROR_MESSAGE);
        if (unit.equals(other.unit)) {
            return Double.compare(value, other.value) <= 0;
        }
        return Double.compare(inBaseUnits().getValue(), other.inBaseUnits().getValue()) <= 0;
    }

    /**
     * Returns {@code true} if this quantity is strictly greater than the other quantity.
     * When both quantities share the same unit, values are compared directly;
     * otherwise, both are converted to base units for comparison.
     */
    public boolean isGreaterThan(Quantity other) {
        Preconditions.checkArgument(other.isCompatibleWith(this), INCOMPATIBLE_ERROR_MESSAGE);
        if (unit.equals(other.unit)) {
            return Double.compare(value, other.value) > 0;
        }
        return Double.compare(inBaseUnits().getValue(), other.inBaseUnits().getValue()) > 0;
    }

    /**
     * Returns {@code true} if this quantity is greater than or equal to the other quantity.
     * When both quantities share the same unit, values are compared directly;
     * otherwise, both are converted to base units for comparison.
     */
    public boolean isGreaterThanOrEqualTo(Quantity other) {
        Preconditions.checkArgument(other.isCompatibleWith(this), INCOMPATIBLE_ERROR_MESSAGE);
        if (unit.equals(other.unit)) {
            return Double.compare(value, other.value) >= 0;
        }
        return Double.compare(inBaseUnits().getValue(), other.inBaseUnits().getValue()) >= 0;
    }

    /**
     * Returns {@code true} if this quantity shares the same dimension as the other quantity,
     * meaning they can be added, subtracted, or compared.
     */
    // TODO(lwhite): Extend this to handle inherited compatibility
    public boolean isCompatibleWith(Quantity other) {
        return other.getDimension().equals(this.getDimension());
    }

    /**
     * Returns true if this quantity is equal to the other quantity.
     * When both quantities share the same unit, values are compared directly;
     * otherwise, both are converted to base units for comparison.
     */
    public boolean isEqual(Quantity other) {
        Preconditions.checkArgument(other.isCompatibleWith(this), INCOMPATIBLE_ERROR_MESSAGE);
        if (unit.equals(other.unit)) {
            return Double.compare(value, other.value) == 0;
        }
        return Double.compare(inBaseUnits().getValue(), other.inBaseUnits().getValue()) == 0;
    }

    public Dimension getDimension() {
        return getUnit().getDimension();
    }

    /**
     * Two quantities are equal if they have the same dimension and represent the same
     * physical amount (equal values when converted to base units). The specific unit
     * does not matter — 1000 meters equals 1 kilometer.
     *
     * <p>When both quantities share the same unit, values are compared directly without
     * base-unit conversion. This allows equality checks for units that do not support
     * ratio-based conversion (e.g., Fahrenheit).
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Quantity quantity = (Quantity) o;

        if (!getDimension().equals(quantity.getDimension())) return false;
        if (unit.equals(quantity.unit)) {
            return Double.compare(value, quantity.value) == 0;
        }
        return Double.compare(quantity.inBaseUnits().getValue(), inBaseUnits().getValue()) == 0;
    }

    @Override
    public int hashCode() {
        try {
            double baseValue = inBaseUnits().getValue();
            return 31 * Double.hashCode(baseValue) + getDimension().hashCode();
        } catch (UnsupportedOperationException e) {
            // Units that don't support base-unit conversion (e.g., Fahrenheit)
            return 31 * Double.hashCode(value) + unit.hashCode();
        }
    }

    /**
     * Converts this quantity to the specified unit within the same dimension.
     * Returns {@code this} if the target unit is the same as the current unit.
     *
     * @param newUnit the target unit
     * @return an equivalent quantity expressed in the new unit
     */
    public Quantity convertUnits(Unit newUnit) {
        if (unit.equals(newUnit)) {
            return this;
        }
        return unit.getDimension().getConverter().convert(this, newUnit);
    }
}
