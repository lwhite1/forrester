package systems.courant.sd.model.expr;

import java.util.Locale;
import java.util.Set;

/**
 * Walks an {@link Expr} AST and detects whether the expression contains any
 * delay or smoothing function calls (SMOOTH, DELAY3, DELAY_FIXED, etc.).
 */
public final class DelayDetector {

    /** Delay-family function names (uppercase) recognized by the detector. */
    private static final Set<String> DELAY_FUNCTIONS = Set.of(
            "SMOOTH", "SMOOTHI", "SMOOTH3", "SMOOTH3I",
            "DELAY1", "DELAY1I", "DELAY3", "DELAY3I", "DELAY_FIXED"
    );

    private DelayDetector() {
    }

    /**
     * Returns {@code true} if the expression tree contains at least one
     * delay-family function call.
     */
    public static boolean containsDelay(Expr expr) {
        return switch (expr) {
            case Expr.Literal ignored -> false;
            case Expr.Ref ignored -> false;
            case Expr.UnaryOp un -> containsDelay(un.operand());
            case Expr.BinaryOp bin -> containsDelay(bin.left()) || containsDelay(bin.right());
            case Expr.FunctionCall call -> {
                if (DELAY_FUNCTIONS.contains(call.name().toUpperCase(Locale.ROOT))) {
                    yield true;
                }
                for (Expr arg : call.arguments()) {
                    if (containsDelay(arg)) {
                        yield true;
                    }
                }
                yield false;
            }
            case Expr.Conditional cond ->
                    containsDelay(cond.condition())
                            || containsDelay(cond.thenExpr())
                            || containsDelay(cond.elseExpr());
        };
    }

    /**
     * Parses the equation string and returns {@code true} if it contains
     * a delay-family function call. Returns {@code false} if the equation
     * is null, blank, or fails to parse.
     */
    public static boolean equationContainsDelay(String equation) {
        if (equation == null || equation.isBlank()) {
            return false;
        }
        try {
            Expr ast = ExprParser.parse(equation);
            return containsDelay(ast);
        } catch (Exception e) {
            return false;
        }
    }
}
