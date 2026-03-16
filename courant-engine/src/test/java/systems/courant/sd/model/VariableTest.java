package systems.courant.sd.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static systems.courant.sd.measure.Units.THING;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class VariableTest {

    @Test
    public void shouldReturnFormulaValue() {
        Variable var = new Variable("V1", THING, () -> 42.0);
        assertEquals(42.0, var.getValue(), 0.0);
    }

    @Test
    public void shouldReturnDynamicFormulaValue() {
        double[] counter = {0};
        Variable var = new Variable("Counter", THING, () -> counter[0]++);
        assertEquals(0.0, var.getValue(), 0.0);
        assertEquals(1.0, var.getValue(), 0.0);
        assertEquals(2.0, var.getValue(), 0.0);
    }

    @Test
    public void shouldRecordHistory() {
        Variable var = new Variable("V1", THING, () -> 10.0);
        var.recordValue();
        var.recordValue();
        assertEquals(10.0, var.getHistoryAtTimeStep(0), 0.0);
        assertEquals(10.0, var.getHistoryAtTimeStep(1), 0.0);
    }

    @Test
    public void shouldReturnZeroForOutOfBoundsHistory() {
        Variable var = new Variable("V1", THING, () -> 10.0);
        assertEquals(0.0, var.getHistoryAtTimeStep(-1), 0.0);
        assertEquals(0.0, var.getHistoryAtTimeStep(0), 0.0);
        assertEquals(0.0, var.getHistoryAtTimeStep(100), 0.0);
    }

    @Test
    public void shouldReturnZeroForLongIndexBeyondIntRange() {
        Variable var = new Variable("V1", THING, () -> 10.0);
        var.recordValue();
        assertEquals(0.0, var.getHistoryAtTimeStep(Integer.MAX_VALUE + 1L), 0.0);
        assertEquals(0.0, var.getHistoryAtTimeStep(Long.MAX_VALUE), 0.0);
    }

    @Test
    public void shouldReturnUnit() {
        Variable var = new Variable("V1", THING, () -> 0);
        assertEquals(THING, var.getUnit());
    }

    @Nested
    @DisplayName("Algebraic loop re-entrancy guard (#456)")
    class AlgebraicLoopGuard {

        @Test
        @DisplayName("should return cached value on re-entrant call to break cycle")
        void shouldReturnCachedValueOnReentrantCall() {
            // Simulate A = B + 1, B = A + 1 (algebraic loop)
            Variable[] vars = new Variable[2];
            vars[0] = new Variable("A", THING, () -> vars[1].getValue() + 1);
            vars[1] = new Variable("B", THING, () -> vars[0].getValue() + 1);

            // First call: A evaluates B, which tries to evaluate A (re-entrant).
            // Re-entrant call returns cachedValue (0.0), so B = 0 + 1 = 1, A = 1 + 1 = 2
            double a = vars[0].getValue();
            assertThat(a).isFinite();
            // No StackOverflowError — the guard broke the cycle
        }

        @Test
        @DisplayName("should converge over multiple evaluations")
        void shouldConvergeOverMultipleEvaluations() {
            // A = B * 0.5, B = A * 0.5 + 10
            // Fixed point: A = (A*0.25 + 5), so A = 5/0.75 ≈ 6.667, B ≈ 13.333
            Variable[] vars = new Variable[2];
            vars[0] = new Variable("A", THING, () -> vars[1].getValue() * 0.5);
            vars[1] = new Variable("B", THING, () -> vars[0].getValue() * 0.5 + 10);

            // Simulate multiple timesteps — each call updates the cached value
            for (int i = 0; i < 20; i++) {
                vars[0].getValue();
                vars[1].getValue();
            }
            double a = vars[0].getValue();
            double b = vars[1].getValue();
            assertThat(a).isCloseTo(6.667, org.assertj.core.data.Offset.offset(0.01));
            assertThat(b).isCloseTo(13.333, org.assertj.core.data.Offset.offset(0.01));
        }

        @Test
        @DisplayName("should not affect non-cyclic variables")
        void shouldNotAffectNonCyclicVariables() {
            Variable v = new Variable("X", THING, () -> 42.0);
            assertThat(v.getValue()).isEqualTo(42.0);
            assertThat(v.getValue()).isEqualTo(42.0);
        }

        @Test
        @DisplayName("clearHistory should reset cached value")
        void clearHistoryShouldResetCachedValue() {
            Variable[] vars = new Variable[2];
            vars[0] = new Variable("A", THING, () -> vars[1].getValue() + 1);
            vars[1] = new Variable("B", THING, () -> vars[0].getValue() + 1);

            // Build up cached values
            vars[0].getValue();
            vars[1].getValue();

            // Reset
            vars[0].clearHistory();
            vars[1].clearHistory();

            // After reset, re-entrant call should return 0 (default cachedValue)
            // A evaluates B, B tries to evaluate A (re-entrant, returns 0),
            // B = 0 + 1 = 1, A = 1 + 1 = 2
            assertThat(vars[0].getValue()).isEqualTo(2.0);
        }

        @Test
        @DisplayName("three-variable cycle should not stack overflow")
        void threeVariableCycleShouldNotStackOverflow() {
            Variable[] vars = new Variable[3];
            vars[0] = new Variable("A", THING, () -> vars[2].getValue() + 1);
            vars[1] = new Variable("B", THING, () -> vars[0].getValue() + 1);
            vars[2] = new Variable("C", THING, () -> vars[1].getValue() + 1);

            // Should not throw StackOverflowError
            double a = vars[0].getValue();
            assertThat(a).isFinite();
        }
    }
}
