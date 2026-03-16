package systems.courant.sd.measure;

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

    /**
     * Creates a new quantity with the given numeric value and unit.
     *
     * @param value the numeric value of the quantity
     * @param unit  the unit of measure (must not be {@code null})
     * @throws IllegalArgumentException if {@code unit} is {@code null}
     */
    public Quantity(double value, Unit unit) {
        if (unit == null) {
            throw new IllegalArgumentException("unit must not be null");
        }
        this.value = value;
        this.unit = unit;
    }

    /**
     * Returns the numeric value of this quantity, expressed in its current unit.
     *
     * @return the numeric value
     */
    public double getValue() {
        return value;
    }

    /**
     * Returns the unit of measure for this quantity.
     *
     * @return the unit
     */
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

        if (unit.equals(other.unit)) {
            return new Quantity(value + other.value, unit);
        }
        try {
            Quantity otherInBaseUnits = other.inBaseUnits();
            Quantity thisInBaseUnits = inBaseUnits();
            Quantity result = new Quantity(otherInBaseUnits.getValue() + thisInBaseUnits.getValue(),
                    this.getUnit().getBaseUnit());
            return getUnit().fromBaseUnits(result);
        } catch (UnsupportedOperationException e) {
            throw new UnsupportedOperationException(
                    "Cannot add " + unit.getName() + " and " + other.unit.getName()
                            + " — base-unit conversion not supported", e);
        }
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

        if (unit.equals(other.unit)) {
            return new Quantity(value - other.value, unit);
        }
        try {
            Quantity otherInBaseUnits = other.inBaseUnits();
            Quantity thisInBaseUnits = inBaseUnits();
            Quantity result = new Quantity(thisInBaseUnits.getValue() - otherInBaseUnits.getValue(),
                    this.getUnit().getBaseUnit());
            return getUnit().fromBaseUnits(result);
        } catch (UnsupportedOperationException e) {
            throw new UnsupportedOperationException(
                    "Cannot subtract " + unit.getName() + " and " + other.unit.getName()
                            + " — base-unit conversion not supported", e);
        }
    }

    /**
     * Returns a human-readable string of this quantity in the form {@code "value unitName(s)"},
     * for example {@code "2.5 Meter(s)"}.
     */
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
        try {
            return Double.compare(inBaseUnits().getValue(), other.inBaseUnits().getValue()) < 0;
        } catch (UnsupportedOperationException e) {
            throw new UnsupportedOperationException(
                    "Cannot compare " + unit.getName() + " and " + other.unit.getName()
                            + " — base-unit conversion not supported", e);
        }
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
        try {
            return Double.compare(inBaseUnits().getValue(), other.inBaseUnits().getValue()) <= 0;
        } catch (UnsupportedOperationException e) {
            throw new UnsupportedOperationException(
                    "Cannot compare " + unit.getName() + " and " + other.unit.getName()
                            + " — base-unit conversion not supported", e);
        }
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
        try {
            return Double.compare(inBaseUnits().getValue(), other.inBaseUnits().getValue()) > 0;
        } catch (UnsupportedOperationException e) {
            throw new UnsupportedOperationException(
                    "Cannot compare " + unit.getName() + " and " + other.unit.getName()
                            + " — base-unit conversion not supported", e);
        }
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
        try {
            return Double.compare(inBaseUnits().getValue(), other.inBaseUnits().getValue()) >= 0;
        } catch (UnsupportedOperationException e) {
            throw new UnsupportedOperationException(
                    "Cannot compare " + unit.getName() + " and " + other.unit.getName()
                            + " — base-unit conversion not supported", e);
        }
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
        try {
            return Double.compare(inBaseUnits().getValue(), other.inBaseUnits().getValue()) == 0;
        } catch (UnsupportedOperationException e) {
            throw new UnsupportedOperationException(
                    "Cannot compare " + unit.getName() + " and " + other.unit.getName()
                            + " — base-unit conversion not supported", e);
        }
    }

    /**
     * Returns the dimension of this quantity, derived from its unit.
     *
     * @return the dimension (e.g., TIME, MASS, LENGTH)
     */
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
        try {
            return Double.compare(quantity.inBaseUnits().getValue(), inBaseUnits().getValue()) == 0;
        } catch (UnsupportedOperationException e) {
            // Units that don't support base-unit conversion (e.g., Fahrenheit vs Celsius)
            return false;
        }
    }

    /**
     * Returns a hash code consistent with {@link #equals(Object)}. Quantities are hashed by
     * their base-unit value and dimension. For units that do not support base-unit conversion
     * (e.g., Fahrenheit), the raw value and unit are used instead.
     */
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
