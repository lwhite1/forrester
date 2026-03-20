package systems.courant.sd.measure;

import systems.courant.sd.model.expr.Expr;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Walks an {@link Expr} AST and infers the {@link CompositeUnit} for each node,
 * producing warnings when dimensional mismatches are detected.
 *
 * <p>Operates purely on the definition layer — no compilation or UI dependency.
 */
public class DimensionalAnalyzer {

    /**
     * Result of dimensional analysis on an expression.
     *
     * @param inferredUnit the composite unit inferred from the expression, or null if undetermined
     * @param warnings     any dimensional mismatch warnings found during analysis
     */
    public record AnalysisResult(CompositeUnit inferredUnit, List<DimensionWarning> warnings) {

        /** Returns true if no warnings were found. */
        public boolean isConsistent() {
            return warnings.isEmpty();
        }
    }

    /**
     * A warning about a dimensional mismatch in an expression.
     *
     * @param message a human-readable description of the mismatch
     */
    public record DimensionWarning(String message) {
    }

    /**
     * Context for resolving element names to their composite units.
     */
    public interface UnitContext {
        /**
         * Returns the composite unit for the named model element, or empty if unknown.
         */
        Optional<CompositeUnit> resolveUnit(String elementName);
    }

    /** Functions that preserve dimensions of their first argument. */
    private static final Set<String> DIMENSION_PRESERVING = Set.of(
            "ABS", "MIN", "MAX", "SMOOTH", "DELAY3", "DELAY3I", "DELAY_FIXED",
            "ROUND", "INT", "SUM", "MEAN"
    );

    /** Functions that require dimensionless input and return dimensionless. */
    private static final Set<String> DIMENSIONLESS_FUNCTIONS = Set.of(
            "LN", "EXP", "LOG", "SIN", "COS", "TAN"
    );

    /** Functions that return a TIME dimension. */
    private static final Set<String> TIME_FUNCTIONS = Set.of("TIME", "DT");

    private final UnitContext context;

    /**
     * Creates a new analyzer with the given unit context.
     *
     * @param context provides name-to-unit mappings for model elements
     */
    public DimensionalAnalyzer(UnitContext context) {
        this.context = context;
    }

    /**
     * Analyzes an expression and infers its composite unit.
     *
     * @param expr the expression to analyze
     * @return the analysis result with inferred unit and any warnings
     */
    public AnalysisResult analyze(Expr expr) {
        List<DimensionWarning> warnings = new ArrayList<>();
        CompositeUnit unit = infer(expr, warnings);
        return new AnalysisResult(unit, List.copyOf(warnings));
    }

    private CompositeUnit infer(Expr expr, List<DimensionWarning> warnings) {
        return switch (expr) {
            case Expr.Literal _ -> CompositeUnit.dimensionless();

            case Expr.Ref ref -> context.resolveUnit(ref.name())
                    .orElse(null);

            case Expr.BinaryOp op -> inferBinaryOp(op, warnings);

            case Expr.UnaryOp op -> inferUnaryOp(op, warnings);

            case Expr.FunctionCall call -> inferFunctionCall(call, warnings);

            case Expr.Conditional cond -> inferConditional(cond, warnings);
        };
    }

    private CompositeUnit inferBinaryOp(Expr.BinaryOp op, List<DimensionWarning> warnings) {
        CompositeUnit left = infer(op.left(), warnings);
        CompositeUnit right = infer(op.right(), warnings);

        if (left == null || right == null) {
            // Unknown dimension — can't check
            return left != null ? left : right;
        }

        return switch (op.operator()) {
            case ADD, SUB -> {
                if (!left.isCompatibleWith(right)) {
                    warnings.add(new DimensionWarning(
                            "Adding/subtracting incompatible units: "
                            + left.displayString() + " and " + right.displayString()));
                }
                yield left;
            }
            case MUL -> left.multiply(right);
            case DIV -> left.divide(right);
            case MOD -> left;
            case POW -> {
                if (op.right() instanceof Expr.Literal lit) {
                    int exp = (int) lit.value();
                    if (exp == lit.value()) {
                        yield left.power(exp);
                    }
                }
                // Non-integer exponent: require dimensionless base
                if (!left.isDimensionless()) {
                    warnings.add(new DimensionWarning(
                            "Non-integer power of dimensioned quantity: "
                            + left.displayString()));
                }
                yield CompositeUnit.dimensionless();
            }
            case EQ, NE, LT, LE, GT, GE -> {
                if (!left.isCompatibleWith(right)) {
                    warnings.add(new DimensionWarning(
                            "Comparing incompatible units: "
                            + left.displayString() + " and " + right.displayString()));
                }
                yield CompositeUnit.dimensionless();
            }
            case AND, OR -> CompositeUnit.dimensionless();
        };
    }

    private CompositeUnit inferUnaryOp(Expr.UnaryOp op, List<DimensionWarning> warnings) {
        CompositeUnit operand = infer(op.operand(), warnings);
        return switch (op.operator()) {
            case NEGATE -> operand;
            case NOT -> CompositeUnit.dimensionless();
        };
    }

    private CompositeUnit inferFunctionCall(Expr.FunctionCall call, List<DimensionWarning> warnings) {
        String name = call.name().toUpperCase();

        if (TIME_FUNCTIONS.contains(name)) {
            return new CompositeUnit(Map.of(Dimension.TIME, 1));
        }

        if (DIMENSIONLESS_FUNCTIONS.contains(name)) {
            return CompositeUnit.dimensionless();
        }

        if ("SQRT".equals(name)) {
            if (!call.arguments().isEmpty()) {
                CompositeUnit arg = infer(call.arguments().getFirst(), warnings);
                if (arg != null) {
                    // Check if all exponents are even
                    boolean allEven = arg.exponents().values().stream()
                            .allMatch(e -> e % 2 == 0);
                    if (allEven) {
                        return new CompositeUnit(halveExponents(arg.exponents()));
                    }
                    // Odd exponents — result has fractional dimensions; propagate the
                    // original unit so downstream operations still produce mismatch warnings
                    if (!arg.isDimensionless()) {
                        warnings.add(new DimensionWarning(
                                "SQRT of non-even-dimensioned quantity: "
                                + arg.displayString()));
                        return arg;
                    }
                }
            }
            return CompositeUnit.dimensionless();
        }

        if ("POWER".equals(name) || "MODULO".equals(name)) {
            if (!call.arguments().isEmpty()) {
                CompositeUnit base = infer(call.arguments().getFirst(), warnings);
                if ("POWER".equals(name) && call.arguments().size() >= 2
                        && call.arguments().get(1) instanceof Expr.Literal lit) {
                    int exp = (int) lit.value();
                    if (base != null && exp == lit.value()) {
                        return base.power(exp);
                    }
                }
                return base;
            }
            return CompositeUnit.dimensionless();
        }

        if (DIMENSION_PRESERVING.contains(name)) {
            if (!call.arguments().isEmpty()) {
                CompositeUnit first = infer(call.arguments().getFirst(), warnings);
                // For MIN/MAX, check second argument compatibility
                if (("MIN".equals(name) || "MAX".equals(name))
                        && call.arguments().size() >= 2) {
                    CompositeUnit second = infer(call.arguments().get(1), warnings);
                    if (first != null && second != null && !first.isCompatibleWith(second)) {
                        warnings.add(new DimensionWarning(
                                name + " arguments have incompatible units: "
                                + first.displayString() + " and " + second.displayString()));
                    }
                }
                return first;
            }
            return CompositeUnit.dimensionless();
        }

        if ("LOOKUP".equals(name) || "LOOKUP_AREA".equals(name)) {
            // Resolve the table's declared output unit from the first argument (table name)
            if (!call.arguments().isEmpty()
                    && call.arguments().getFirst() instanceof Expr.Ref tableRef) {
                CompositeUnit tableUnit = context.resolveUnit(tableRef.name()).orElse(null);
                if (tableUnit != null) {
                    return tableUnit;
                }
            }
            return CompositeUnit.dimensionless();
        }

        // STEP, RAMP, PULSE, PULSE_TRAIN, TREND, FORECAST, NPV,
        // RANDOM_NORMAL, RANDOM_UNIFORM — treat as dimensionless
        return CompositeUnit.dimensionless();
    }

    private CompositeUnit inferConditional(Expr.Conditional cond, List<DimensionWarning> warnings) {
        // Condition is consumed for its truth value
        infer(cond.condition(), warnings);

        CompositeUnit thenUnit = infer(cond.thenExpr(), warnings);
        CompositeUnit elseUnit = infer(cond.elseExpr(), warnings);

        if (thenUnit == null) {
            return elseUnit;
        }
        if (elseUnit == null) {
            return thenUnit;
        }
        if (!thenUnit.isCompatibleWith(elseUnit)) {
            warnings.add(new DimensionWarning(
                    "IF branches have incompatible units: "
                    + thenUnit.displayString() + " and " + elseUnit.displayString()));
        }
        return thenUnit;
    }

    /**
     * Halves all exponents in the map. Only call when all exponents are even.
     */
    private static Map<Dimension, Integer> halveExponents(Map<Dimension, Integer> exponents) {
        var result = new java.util.LinkedHashMap<Dimension, Integer>();
        for (Map.Entry<Dimension, Integer> e : exponents.entrySet()) {
            int halved = e.getValue() / 2;
            if (halved != 0) {
                result.put(e.getKey(), halved);
            }
        }
        return result;
    }
}
