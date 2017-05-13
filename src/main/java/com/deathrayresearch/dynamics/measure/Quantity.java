package com.deathrayresearch.dynamics.measure;

/**
 * A dimension-aware amount of something. It consists of a dimension (like time, mass, length, money, etc.)
 * and a value (like 2.54). Individual values are expressed in Units (like hours), and the units have
 * the specific dimension (in this case time).
 *
 * It lets you express things like 2.54 miles, and add that distance to 1,312 meters, handling the units conversion for
 * you. It also lets you compare two quantities in the same dimension to assert that 2.54 miles > 1,312 meters, again
 * handling any conversions, while preventing you from comparing 2.54 miles with 2.54 pounds.
 */
public class Quantity <E extends Dimension> {

    private double value;
    private Unit<E> unit;

    public Quantity(double value, Unit<E> unit) {
        this.value = value;
        this.unit = unit;
    }

    public double getValue() {
        return value;
    }

    public Unit<E> getUnit() {
        return unit;
    }

    public Quantity<E> inBaseUnits() {
        return new Quantity<>(unit.ratioToBaseUnit() * getValue(), unit.getBaseUnit());
    }

    public Quantity<E> multiply(double d) {
        return new Quantity<>(d * getValue(), this.getUnit());
    }

    public Quantity<E> add(double d) {
        return new Quantity<>(d + getValue(), this.getUnit());
    }

    public Quantity<E> add(Quantity<E> other) {
        Quantity<E> otherInBaseUnits = other.inBaseUnits();
        Quantity<E> thisInBaseUnits = inBaseUnits();
        Quantity<E> result = new Quantity<>(otherInBaseUnits.getValue() + thisInBaseUnits.getValue(),
                this.getUnit().getBaseUnit());
        return getUnit().fromBaseUnits(result);
    }

    public Quantity<E> subtract(Quantity<E> other) {
        Quantity<E> otherInBaseUnits = other.inBaseUnits();
        Quantity<E> thisInBaseUnits = inBaseUnits();
        Quantity<E> result = new Quantity<>(thisInBaseUnits.getValue() - otherInBaseUnits.getValue(),
                this.getUnit().getBaseUnit());
        return getUnit().fromBaseUnits(result);
    }

    public Quantity<E> subtract(double d) {
        return new Quantity<>(getValue() - d, this.getUnit());
    }

    public Quantity<E> divide(double d) {
        return new Quantity<>(getValue() / d, this.getUnit());
    }

    @Override
    public String toString() {
        return "" + value +
                " " + unit.getName() +
                "(s)";
    }

    public boolean isLessThan(Quantity<E> other) {
        return Double.compare(inBaseUnits().getValue(), other.inBaseUnits().getValue()) < 0;
    }

    public boolean isLessThanOrEqualTo(Quantity<E> other) {
        return Double.compare(inBaseUnits().getValue(), other.inBaseUnits().getValue()) <= 0;
    }

    public boolean isGreaterThan(Quantity<E> other) {
        return Double.compare(inBaseUnits().getValue(), other.inBaseUnits().getValue()) > 0;
    }

    public boolean isGreaterThanOrEqualTo(Quantity<E> other) {
        return Double.compare(inBaseUnits().getValue(), other.inBaseUnits().getValue()) >= 0;
    }

    /**
     * Returns true if this quantity is equal to the other quantity in base units
     */
    public boolean isEqual(Quantity<E> other) {
        return Double.compare(inBaseUnits().getValue(), other.inBaseUnits().getValue()) == 0;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Quantity<?> quantity = (Quantity<?>) o;

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
