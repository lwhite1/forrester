package systems.courant.shrewd.model;

import com.google.common.base.Preconditions;

import systems.courant.shrewd.model.compile.CompilationContext;

import java.util.function.DoubleSupplier;

/**
 * Implements the Vensim FIND ZERO function using bisection.
 *
 * <p>{@code FIND ZERO(expression, variable, lo, hi)} finds the value of {@code variable}
 * in the range {@code [lo, hi]} that makes {@code expression} evaluate to zero.
 *
 * <p>At runtime, temporarily overrides the loop variable in the compilation context
 * and uses the bisection method to converge on the root.
 */
public class FindZero implements Formula {

    private static final int MAX_ITERATIONS = 50;
    private static final double TOLERANCE = 1e-10;

    private final DoubleSupplier expression;
    private final String variableName;
    private final DoubleSupplier loBound;
    private final DoubleSupplier hiBound;
    private final CompilationContext context;

    private FindZero(DoubleSupplier expression, String variableName,
                     DoubleSupplier loBound, DoubleSupplier hiBound,
                     CompilationContext context) {
        Preconditions.checkNotNull(expression, "expression supplier must not be null");
        Preconditions.checkNotNull(variableName, "variableName must not be null");
        Preconditions.checkNotNull(loBound, "loBound supplier must not be null");
        Preconditions.checkNotNull(hiBound, "hiBound supplier must not be null");
        Preconditions.checkNotNull(context, "context must not be null");
        this.expression = expression;
        this.variableName = variableName;
        this.loBound = loBound;
        this.hiBound = hiBound;
        this.context = context;
    }

    /**
     * Creates a FIND ZERO formula.
     *
     * @param expression   the compiled expression to find the zero of
     * @param variableName the name of the loop variable to vary
     * @param loBound      supplies the lower bound of the search range
     * @param hiBound      supplies the upper bound of the search range
     * @param context      the compilation context (for overriding the loop variable)
     * @return a new FindZero formula
     */
    public static FindZero of(DoubleSupplier expression, String variableName,
                               DoubleSupplier loBound, DoubleSupplier hiBound,
                               CompilationContext context) {
        return new FindZero(expression, variableName, loBound, hiBound, context);
    }

    @Override
    public double getCurrentValue() {
        double lo = loBound.getAsDouble();
        double hi = hiBound.getAsDouble();

        // Evaluate expression at lo
        context.addLiteralConstant(variableName, lo);
        double fLo = expression.getAsDouble();

        // Evaluate expression at hi
        context.addLiteralConstant(variableName, hi);
        double fHi = expression.getAsDouble();

        // If signs are the same, return the endpoint closer to zero
        if (fLo * fHi > 0) {
            double result = Math.abs(fLo) < Math.abs(fHi) ? lo : hi;
            context.addLiteralConstant(variableName, result);
            return result;
        }

        // Bisection
        for (int i = 0; i < MAX_ITERATIONS; i++) {
            double mid = (lo + hi) / 2.0;
            context.addLiteralConstant(variableName, mid);
            double fMid = expression.getAsDouble();

            if (Math.abs(fMid) < TOLERANCE || (hi - lo) / 2.0 < TOLERANCE) {
                return mid;
            }

            if (fLo * fMid < 0) {
                hi = mid;
            } else {
                lo = mid;
                fLo = fMid;
            }
        }

        // Return best estimate
        double mid = (lo + hi) / 2.0;
        context.addLiteralConstant(variableName, mid);
        return mid;
    }
}
