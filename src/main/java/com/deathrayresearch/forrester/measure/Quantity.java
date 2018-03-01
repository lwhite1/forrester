package com.deathrayresearch.forrester.measure;

import com.google.common.base.Preconditions;

import javax.annotation.concurrent.Immutable;

/**
 * A dimension-aware amount of something. It consists of a dimension (like time, mass, length, money, etc.)
 * and a value (like 2.54). Individual values are expressed in Units (like hours), and the units have
 * the specific dimension (in this case time).
 *
 * It lets you express things like 2.54 miles, and add that distance to 1,312 meters, handling the units conversion for
 * you. It also lets you compare two quantities in the same dimension to assert that 2.54 miles > 1,312 meters, again
 * handling any conversions, while preventing you from comparing 2.54 miles with 2.54 pounds.
 */
@Immutable
public final class Quantity {

    private static final String INCOMPATIBLE_ERROR_MESSAGE = "Combined quantities must have compatible units";

    private double value;
    private final Unit unit;

    public Quantity(double value, Unit unit) {
        this.value = value;
        this.unit = unit;
    }

    public double getValue() {
        return value;
    }

    public Unit getUnit() {
        return unit;
    }

    public Quantity inBaseUnits() {
        return unit.toBaseUnits(this);
    }

    public Quantity multiply(double d) {
        return new Quantity(d * getValue(), this.getUnit());
    }

    public Quantity divide(double d) {
        return new Quantity( getValue() / d, this.getUnit());
    }

    public Quantity add(Quantity other) {
        Preconditions.checkArgument(other.isCompatibleWith(this), INCOMPATIBLE_ERROR_MESSAGE);

        Quantity otherInBaseUnits = other.inBaseUnits();
        Quantity thisInBaseUnits = inBaseUnits();
        Quantity result = new Quantity(otherInBaseUnits.getValue() + thisInBaseUnits.getValue(),
                this.getUnit().getBaseUnit());
        return getUnit().fromBaseUnits(result);
    }

    public Quantity subtract(Quantity other) {
        Preconditions.checkArgument(other.isCompatibleWith(this), INCOMPATIBLE_ERROR_MESSAGE);

        Quantity otherInBaseUnits = other.inBaseUnits();
        Quantity thisInBaseUnits = inBaseUnits();
        Quantity result = new Quantity(thisInBaseUnits.getValue() - otherInBaseUnits.getValue(),
                this.getUnit().getBaseUnit());
        return getUnit().fromBaseUnits(result);
    }

    public void setValue(double value) {
        this.value = value;
    }

    @Override
    public String toString() {
        return "" + value +
                " " + unit.getName() +
                "(s)";
    }

    public boolean isLessThan(Quantity other) {
        return Double.compare(inBaseUnits().getValue(), other.inBaseUnits().getValue()) < 0;
    }

    public boolean isLessThanOrEqualTo(Quantity other) {
        return Double.compare(inBaseUnits().getValue(), other.inBaseUnits().getValue()) <= 0;
    }

    public boolean isGreaterThan(Quantity other) {
        return Double.compare(inBaseUnits().getValue(), other.inBaseUnits().getValue()) > 0;
    }

    public boolean isGreaterThanOrEqualTo(Quantity other) {
        return Double.compare(inBaseUnits().getValue(), other.inBaseUnits().getValue()) >= 0;
    }

    // TODO(lwhite): Extend this to handle inherited compatibility
    public boolean isCompatibleWith(Quantity other) {
        return other.getDimension().equals(this.getDimension());
    }

    /**
     * Returns true if this quantity is equal to the other quantity in base units
     */
    public boolean isEqual(Quantity other) {
        return Double.compare(inBaseUnits().getValue(), other.inBaseUnits().getValue()) == 0;
    }

    public Dimension getDimension() {
        return getUnit().getDimension();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Quantity quantity = (Quantity) o;

        if (Double.compare(quantity.inBaseUnits().getValue(), inBaseUnits().getValue()) != 0) return false;
        return getUnit().equals(quantity.getUnit());
    }

    @Override
    public int hashCode() {
        int result;
        long temp;
        temp = Double.doubleToLongBits(getValue());
        result = (int) (temp ^ (temp >>> 32));
        result = 31 * result + getUnit().hashCode();
        return result;
    }
}
