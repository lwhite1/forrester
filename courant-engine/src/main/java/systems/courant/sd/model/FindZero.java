package systems.courant.sd.model;

import com.google.common.base.Preconditions;

import java.util.function.DoubleSupplier;

/**
 * Implements the Vensim FIND ZERO function using bisection.
 *
 * <p>{@code FIND ZERO(expression, variable, lo, hi)} finds the value of {@code variable}
 * in the range {@code [lo, hi]} that makes {@code expression} evaluate to zero.
 *
 * <p>At runtime, writes to a dedicated mutable holder (single-element {@code double[]})
 * that the compiled expression reads via the compilation context. This avoids mutating
 * shared state and is safe for concurrent evaluation.
 */
public class FindZero implements Formula {

    private static final int MAX_ITERATIONS = 50;
    private static final double TOLERANCE = 1e-10;

    private final DoubleSupplier expression;
    private final DoubleSupplier loBound;
    private final DoubleSupplier hiBound;
    private final double[] variableHolder;

    private FindZero(DoubleSupplier expression, double[] variableHolder,
                     DoubleSupplier loBound, DoubleSupplier hiBound) {
        Preconditions.checkNotNull(expression, "expression supplier must not be null");
        Preconditions.checkNotNull(variableHolder, "variableHolder must not be null");
        Preconditions.checkNotNull(loBound, "loBound supplier must not be null");
        Preconditions.checkNotNull(hiBound, "hiBound supplier must not be null");
        this.expression = expression;
        this.variableHolder = variableHolder;
        this.loBound = loBound;
        this.hiBound = hiBound;
    }

    /**
     * Creates a FIND ZERO formula.
     *
     * @param expression     the compiled expression to find the zero of
     * @param variableHolder a single-element array used to set the loop variable value;
     *                       the compiled expression reads from this holder via the context
     * @param loBound        supplies the lower bound of the search range
     * @param hiBound        supplies the upper bound of the search range
     * @return a new FindZero formula
     */
    public static FindZero of(DoubleSupplier expression, double[] variableHolder,
                               DoubleSupplier loBound, DoubleSupplier hiBound) {
        return new FindZero(expression, variableHolder, loBound, hiBound);
    }

    @Override
    public double getCurrentValue() {
        double lo = loBound.getAsDouble();
        double hi = hiBound.getAsDouble();

        // Evaluate expression at lo
        variableHolder[0] = lo;
        double fLo = expression.getAsDouble();

        // Evaluate expression at hi
        variableHolder[0] = hi;
        double fHi = expression.getAsDouble();

        // If signs are the same, return the endpoint closer to zero
        if (fLo * fHi > 0) {
            double result = Math.abs(fLo) < Math.abs(fHi) ? lo : hi;
            variableHolder[0] = result;
            return result;
        }

        // Bisection
        for (int i = 0; i < MAX_ITERATIONS; i++) {
            double mid = (lo + hi) / 2.0;
            variableHolder[0] = mid;
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
        variableHolder[0] = mid;
        return mid;
    }
}
