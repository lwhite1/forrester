package systems.courant.shrewd.model.def;

import systems.courant.shrewd.model.expr.Expr;
import systems.courant.shrewd.model.expr.ExprParser;
import systems.courant.shrewd.model.expr.ParseException;
import systems.courant.shrewd.model.expr.UnaryOperator;

import java.util.List;

/**
 * Definition of an auxiliary variable in a model. Auxiliaries can hold either a computed
 * formula expression (e.g. {@code "Population * birth_rate"}) or a literal numeric value
 * (e.g. {@code "0.03"}). Literal-valued auxiliaries serve as tunable parameters for
 * sweep, Monte Carlo, and optimization analyses.
 *
 * @param name the variable name
 * @param comment optional description
 * @param equation the formula expression string (may be a numeric literal)
 * @param unit the unit name
 * @param subscripts dimension names this auxiliary is subscripted over (empty for scalar)
 */
public record AuxDef(
        String name,
        String comment,
        String equation,
        String unit,
        List<String> subscripts
) implements ElementDef {

    public AuxDef {
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
    public AuxDef(String name, String comment, String equation, String unit) {
        this(name, comment, equation, unit, List.of());
    }

    /**
     * Convenience constructor that creates an auxiliary definition without a comment.
     */
    public AuxDef(String name, String equation, String unit) {
        this(name, null, equation, unit, List.of());
    }

    /**
     * Creates a literal-valued auxiliary (parameter) from a numeric value.
     *
     * @param name  the variable name
     * @param comment optional description
     * @param value the numeric value (must be finite)
     * @param unit  the unit name
     */
    public AuxDef(String name, String comment, double value, String unit) {
        this(name, comment, formatValue(value), unit, List.of());
    }

    /**
     * Returns {@code true} if this auxiliary's equation is a numeric literal
     * (possibly negated). Literal-valued auxiliaries are treated as tunable
     * parameters for sweep, Monte Carlo, and optimization analyses.
     */
    public boolean isLiteral() {
        try {
            Expr expr = ExprParser.parse(equation);
            return isLiteralExpr(expr);
        } catch (ParseException e) {
            return false;
        }
    }

    /**
     * Returns the numeric value of this literal-valued auxiliary.
     *
     * @throws IllegalStateException if {@link #isLiteral()} returns {@code false}
     */
    public double literalValue() {
        try {
            Expr expr = ExprParser.parse(equation);
            return extractLiteralValue(expr);
        } catch (ParseException e) {
            throw new IllegalStateException(
                    "Auxiliary '" + name + "' equation is not a literal: " + equation, e);
        }
    }

    private static boolean isLiteralExpr(Expr expr) {
        if (expr instanceof Expr.Literal) {
            return true;
        }
        if (expr instanceof Expr.UnaryOp un
                && un.operator() == UnaryOperator.NEGATE) {
            return un.operand() instanceof Expr.Literal;
        }
        return false;
    }

    private static double extractLiteralValue(Expr expr) {
        if (expr instanceof Expr.Literal lit) {
            return lit.value();
        }
        if (expr instanceof Expr.UnaryOp un
                && un.operator() == UnaryOperator.NEGATE
                && un.operand() instanceof Expr.Literal lit) {
            return -lit.value();
        }
        throw new IllegalStateException("Expression is not a literal: " + expr);
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
