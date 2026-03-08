package systems.courant.forrester.model;

import systems.courant.forrester.measure.Unit;
import com.google.common.base.Preconditions;

/**
 *  An unchanging exogenous value used in a model
 */
public class Constant extends Element {

    private final double currentValue;
    private final Unit unit;

    /**
     * Creates a new constant with the given name, unit, and value.
     *
     * @param name         the constant name
     * @param unit         the unit of measure for this constant
     * @param currentValue the fixed value of this constant
     */
    public Constant(String name, Unit unit, double currentValue) {
        super(name);
        Preconditions.checkNotNull(unit, "unit must not be null");
        this.unit = unit;
        this.currentValue = currentValue;
    }

    /**
     * Returns the value of this constant.
     */
    public double getValue() {
        return currentValue;
    }

    /**
     * Returns the value of this constant rounded to the nearest integer.
     * Values outside the int range are clamped to {@link Integer#MAX_VALUE}
     * or {@link Integer#MIN_VALUE}.
     */
    public int getIntValue() {
        long rounded = Math.round(currentValue);
        return (int) Math.max(Integer.MIN_VALUE, Math.min(Integer.MAX_VALUE, rounded));
    }

    /**
     * Returns the unit of measure for this constant.
     */
    public Unit getUnit() {
        return unit;
    }
}
