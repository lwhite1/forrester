package systems.courant.forrester.model.def;

/**
 * Definition of a constant (fixed exogenous value) in a model.
 *
 * @param name the constant name
 * @param comment optional description
 * @param value the numeric value
 * @param unit the unit name
 */
public record ConstantDef(
        String name,
        String comment,
        double value,
        String unit
) implements ElementDef {

    public ConstantDef {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Constant name must not be blank");
        }
        if (Double.isNaN(value) || Double.isInfinite(value)) {
            throw new IllegalArgumentException(
                    "Constant '" + name + "' value must be finite, got " + value);
        }
    }

    /**
     * Convenience constructor that creates a constant definition without a comment.
     *
     * @param name  the constant name
     * @param value the numeric value
     * @param unit  the unit name
     */
    public ConstantDef(String name, double value, String unit) {
        this(name, null, value, unit);
    }
}
