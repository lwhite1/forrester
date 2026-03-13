package systems.courant.sd.measure;

import systems.courant.sd.measure.dimension.Dimensionless;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.StringJoiner;

/**
 * Represents a compound unit as a map of {@link Dimension} to integer exponents.
 * For example, "Person per Day" is {@code {ITEM: 1, TIME: -1}}.
 * Dimensionless quantities have an empty exponent map.
 *
 * <p>This is the core type for dimensional analysis of equations.
 */
public record CompositeUnit(Map<Dimension, Integer> exponents) {

    /**
     * Canonical constructor that defensively copies the exponent map.
     */
    public CompositeUnit {
        exponents = Map.copyOf(exponents);
    }

    /**
     * Creates a dimensionless composite unit (no dimensions).
     */
    public static CompositeUnit dimensionless() {
        return new CompositeUnit(Map.of());
    }

    /**
     * Creates a composite unit from a single unit (exponent = 1).
     * Returns dimensionless if the unit's dimension is {@link Dimensionless}.
     */
    public static CompositeUnit of(Unit unit) {
        if (unit == null || unit.getDimension() instanceof Dimensionless) {
            return dimensionless();
        }
        return new CompositeUnit(Map.of(unit.getDimension(), 1));
    }

    /**
     * Creates a rate composite unit: material / time (e.g., Person per Day).
     *
     * @param material the material unit (numerator)
     * @param time     the time unit (denominator)
     */
    public static CompositeUnit ofRate(Unit material, TimeUnit time) {
        Map<Dimension, Integer> exp = new LinkedHashMap<>();
        if (material != null && !(material.getDimension() instanceof Dimensionless)) {
            exp.put(material.getDimension(), 1);
        }
        if (time != null) {
            exp.merge(time.getDimension(), -1, Integer::sum);
        }
        return new CompositeUnit(normalize(exp));
    }

    /**
     * Returns true if this composite unit has no dimensions (is a pure number).
     */
    public boolean isDimensionless() {
        return exponents.isEmpty();
    }

    /**
     * Multiplies this unit by another (adds exponents).
     */
    public CompositeUnit multiply(CompositeUnit other) {
        Map<Dimension, Integer> result = new LinkedHashMap<>(exponents);
        for (Map.Entry<Dimension, Integer> e : other.exponents.entrySet()) {
            result.merge(e.getKey(), e.getValue(), Integer::sum);
        }
        return new CompositeUnit(normalize(result));
    }

    /**
     * Divides this unit by another (subtracts exponents).
     */
    public CompositeUnit divide(CompositeUnit other) {
        Map<Dimension, Integer> result = new LinkedHashMap<>(exponents);
        for (Map.Entry<Dimension, Integer> e : other.exponents.entrySet()) {
            result.merge(e.getKey(), -e.getValue(), Integer::sum);
        }
        return new CompositeUnit(normalize(result));
    }

    /**
     * Raises this unit to a power (multiplies all exponents by n).
     */
    public CompositeUnit power(int n) {
        if (n == 0) {
            return dimensionless();
        }
        Map<Dimension, Integer> result = new LinkedHashMap<>();
        for (Map.Entry<Dimension, Integer> e : exponents.entrySet()) {
            result.put(e.getKey(), e.getValue() * n);
        }
        return new CompositeUnit(normalize(result));
    }

    /**
     * Returns true if this composite unit has the same dimensional signature as the other.
     */
    public boolean isCompatibleWith(CompositeUnit other) {
        return exponents.equals(other.exponents);
    }

    /**
     * Returns a human-readable display string for this composite unit.
     * Examples: "Person / Day", "Dimensionless", "USD / Day^2".
     */
    public String displayString() {
        if (exponents.isEmpty()) {
            return "Dimensionless";
        }

        StringJoiner numerator = new StringJoiner(" * ");
        StringJoiner denominator = new StringJoiner(" * ");

        for (Map.Entry<Dimension, Integer> e : exponents.entrySet()) {
            String dimName = e.getKey().getBaseUnit().getName();
            int exp = e.getValue();
            if (exp > 0) {
                numerator.add(exp == 1 ? dimName : dimName + "^" + exp);
            } else {
                int absExp = -exp;
                denominator.add(absExp == 1 ? dimName : dimName + "^" + absExp);
            }
        }

        if (numerator.length() == 0) {
            numerator.add("1");
        }

        if (denominator.length() == 0) {
            return numerator.toString();
        }

        return numerator + " / " + denominator;
    }

    @Override
    public String toString() {
        return displayString();
    }

    /**
     * Removes zero-exponent entries from a mutable map.
     */
    private static Map<Dimension, Integer> normalize(Map<Dimension, Integer> map) {
        map.entrySet().removeIf(e -> e.getValue() == 0);
        return map;
    }
}
