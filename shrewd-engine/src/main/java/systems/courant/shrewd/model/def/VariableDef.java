package systems.courant.shrewd.model.def;

import java.util.List;

/**
 * Definition of a variable variable in a model. Variables can hold either a computed
 * formula expression (e.g. {@code "Population * birth_rate"}) or a literal numeric value
 * (e.g. {@code "0.03"}). Literal-valued variables serve as tunable parameters for
 * sweep, Monte Carlo, and optimization analyses.
 *
 * @param name the variable name
 * @param comment optional description
 * @param equation the formula expression string (may be a numeric literal)
 * @param unit the unit name
 * @param subscripts dimension names this variable is subscripted over (empty for scalar)
 */
public record VariableDef(
        String name,
        String comment,
        String equation,
        String unit,
        List<String> subscripts
) implements ElementDef {

    public VariableDef {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Auxiliary name must not be blank");
        }
        if (equation == null || equation.isBlank()) {
            throw new IllegalArgumentException("Auxiliary equation must not be blank");
        }
        subscripts = subscripts == null ? List.of() : List.copyOf(subscripts);
    }

    /**
     * Backward-compatible constructor without subscripts.
     */
    public VariableDef(String name, String comment, String equation, String unit) {
        this(name, comment, equation, unit, List.of());
    }

    /**
     * Convenience constructor that creates a variable definition without a comment.
     */
    public VariableDef(String name, String equation, String unit) {
        this(name, null, equation, unit, List.of());
    }

    /**
     * Creates a literal-valued variable (parameter) from a numeric value.
     *
     * @param name  the variable name
     * @param comment optional description
     * @param value the numeric value (must be finite)
     * @param unit  the unit name
     */
    public VariableDef(String name, String comment, double value, String unit) {
        this(name, comment, formatValue(value), unit, List.of());
    }

    /**
     * Returns {@code true} if this auxiliary's equation is a numeric literal
     * (possibly negated). Literal-valued variables are treated as tunable
     * parameters for sweep, Monte Carlo, and optimization analyses.
     */
    public boolean isLiteral() {
        try {
            Double.parseDouble(equation.strip());
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    /**
     * Returns the numeric value of this literal-valued variable.
     *
     * @throws IllegalStateException if {@link #isLiteral()} returns {@code false}
     */
    public double literalValue() {
        try {
            return Double.parseDouble(equation.strip());
        } catch (NumberFormatException e) {
            throw new IllegalStateException(
                    "Auxiliary '" + name + "' equation is not a literal: " + equation, e);
        }
    }

    /**
     * Formats a double value as a clean equation string, avoiding unnecessary
     * trailing zeros (e.g. {@code 42.0} becomes {@code "42"}).
     */
    public static String formatValue(double value) {
        if (!Double.isInfinite(value) && !Double.isNaN(value)
                && Double.compare(value, Math.rint(value)) == 0
                && Math.abs(value) <= Long.MAX_VALUE) {
            return Long.toString((long) value);
        }
        return Double.toString(value);
    }
}
