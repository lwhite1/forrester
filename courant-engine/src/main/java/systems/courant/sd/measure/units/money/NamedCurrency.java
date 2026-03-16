package systems.courant.sd.measure.units.money;

import systems.courant.sd.measure.Dimension;
import systems.courant.sd.measure.Unit;

/**
 * A dynamic currency unit for the {@link Dimension#MONEY} dimension,
 * parallel to {@link systems.courant.sd.measure.units.item.ItemUnit} for items.
 *
 * <p>Imported models carry currency names like "EURO", "GBP", "JPY" that don't
 * map to the built-in {@link MoneyUnits} enum. This class allows them to be
 * created dynamically while remaining in the MONEY dimension (rather than
 * falling back to ITEM).
 *
 * <p>All named currencies use ratio 1.0 (no exchange-rate conversion).
 * Two instances with the same name are considered equal.
 */
public class NamedCurrency implements Unit {

    private final String name;

    /**
     * Creates a new named currency unit.
     *
     * @param name the display name (e.g., "EUR", "EURO", "GBP")
     */
    public NamedCurrency(String name) {
        this.name = name;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public Dimension getDimension() {
        return Dimension.MONEY;
    }

    @Override
    public double ratioToBaseUnit() {
        return 1.0;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        NamedCurrency that = (NamedCurrency) o;
        return name.equals(that.name);
    }

    @Override
    public int hashCode() {
        return name.hashCode();
    }

    @Override
    public String toString() {
        return name;
    }
}
