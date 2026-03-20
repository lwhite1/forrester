package systems.courant.sd.model.expr;

import java.util.Set;

/**
 * Canonical set of built-in function and keyword names (uppercase).
 * Used by {@link ExprDependencies} and {@link ExprRenamer} to distinguish
 * built-in names from model element references and lookup tables.
 */
public final class BuiltinFunctions {

    /** Built-in function and keyword names (uppercase) that are not model elements. */
    public static final Set<String> NAMES = Set.of(
            "ABS", "SQRT", "LN", "EXP", "LOG", "SIN", "COS", "TAN",
            "ARCSIN", "ARCCOS", "ARCTAN", "SIGN", "PI",
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
            "LOOKUP_AREA", "LOOKUP",
            "IF", "TIME", "DT",
            "NOT", "OR", "AND", "TRUE", "FALSE"
    );

    private BuiltinFunctions() {
    }
}
