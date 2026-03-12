package systems.courant.shrewd.model.expr;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;

/**
 * Walks an {@link Expr} AST and extracts all {@link Expr.Ref} names.
 * This provides the foundation for dependency analysis and influence arrows
 * in graphical model views.
 */
public final class ExprDependencies {

    /** Built-in function names (uppercase) that should not be treated as model element references. */
    private static final Set<String> BUILTIN_FUNCTIONS = Set.of(
            "ABS", "SQRT", "LN", "EXP", "LOG", "SIN", "COS", "TAN",
            "ARCSIN", "ARCCOS", "ARCTAN", "SIGN",
            "INT", "ROUND", "MODULO", "QUANTUM", "POWER", "MIN", "MAX",
            "SUM", "MEAN", "VMIN", "VMAX", "PROD",
            "INITIAL",
            "SMOOTH", "SMOOTHI", "SMOOTH3", "SMOOTH3I",
            "DELAY1", "DELAY1I", "DELAY3", "DELAY3I", "DELAY_FIXED",
            "STEP", "RAMP", "PULSE", "PULSE_TRAIN",
            "TREND", "FORECAST", "NPV",
            "RANDOM_NORMAL", "RANDOM_UNIFORM",
            "XIDZ", "ZIDZ",
            "SAMPLE_IF_TRUE", "FIND_ZERO",
            "NOT", "OR", "AND", "TRUE", "FALSE",
            "LOOKUP_AREA",
            "LOOKUP", "IF", "TIME", "DT", "PI"
    );

    private ExprDependencies() {
    }

    /**
     * Returns the set of all referenced element names in the given expression.
     */
    public static Set<String> extract(Expr expr) {
        Set<String> deps = new LinkedHashSet<>();
        walk(expr, deps);
        return Collections.unmodifiableSet(deps);
    }

    private static void walk(Expr expr, Set<String> deps) {
        if (expr instanceof Expr.Literal) {
            // no dependencies
        } else if (expr instanceof Expr.Ref ref) {
            deps.add(ref.name());
        } else if (expr instanceof Expr.BinaryOp bin) {
            walk(bin.left(), deps);
            walk(bin.right(), deps);
        } else if (expr instanceof Expr.UnaryOp un) {
            walk(un.operand(), deps);
        } else if (expr instanceof Expr.FunctionCall call) {
            // For single-argument calls, the function name may be a lookup table
            // reference using Vensim's table(input) syntax. Add it as a dependency
            // so influence arrows and dependency analysis capture the relationship.
            // Exclude known built-in function names to avoid false positives.
            if (call.arguments().size() == 1
                    && !BUILTIN_FUNCTIONS.contains(call.name().toUpperCase(Locale.ROOT))) {
                deps.add(call.name());
            }
            for (Expr arg : call.arguments()) {
                walk(arg, deps);
            }
        } else if (expr instanceof Expr.Conditional cond) {
            walk(cond.condition(), deps);
            walk(cond.thenExpr(), deps);
            walk(cond.elseExpr(), deps);
        } else {
            throw new IllegalArgumentException("Unknown Expr type: " + expr.getClass());
        }
    }
}
