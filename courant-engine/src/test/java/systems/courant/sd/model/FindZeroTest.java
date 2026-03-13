package systems.courant.sd.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

@DisplayName("FindZero")
class FindZeroTest {

    @Nested
    @DisplayName("bisection convergence")
    class BisectionConvergence {

        @Test
        @DisplayName("should find root of linear expression x - 5 = 0")
        void shouldFindRootOfLinearExpression() {
            double[] holder = {0.0};
            // expression: holder[0] - 5
            FindZero fz = FindZero.of(() -> holder[0] - 5.0, holder, () -> 0.0, () -> 10.0);

            double result = fz.getCurrentValue();

            assertThat(result).isCloseTo(5.0, within(1e-9));
        }

        @Test
        @DisplayName("should find root of quadratic expression x^2 - 4 = 0 in [0,10]")
        void shouldFindRootOfQuadratic() {
            double[] holder = {0.0};
            FindZero fz = FindZero.of(() -> holder[0] * holder[0] - 4.0, holder,
                    () -> 0.0, () -> 10.0);

            double result = fz.getCurrentValue();

            assertThat(result).isCloseTo(2.0, within(1e-9));
        }

        @Test
        @DisplayName("should find negative root when range brackets it")
        void shouldFindNegativeRoot() {
            double[] holder = {0.0};
            FindZero fz = FindZero.of(() -> holder[0] + 3.0, holder, () -> -10.0, () -> 0.0);

            double result = fz.getCurrentValue();

            assertThat(result).isCloseTo(-3.0, within(1e-9));
        }

        @Test
        @DisplayName("should converge for expression with root near lower bound")
        void shouldConvergeNearLowerBound() {
            double[] holder = {0.0};
            // expression: holder[0] - 0.001
            FindZero fz = FindZero.of(() -> holder[0] - 0.001, holder, () -> 0.0, () -> 10.0);

            double result = fz.getCurrentValue();

            assertThat(result).isCloseTo(0.001, within(1e-9));
        }
    }

    @Nested
    @DisplayName("no bracket fallback")
    class NoBracketFallback {

        @Test
        @DisplayName("should return lo when both endpoints positive and lo is closer to zero")
        void shouldReturnCloserEndpointLo() {
            double[] holder = {0.0};
            // expression: holder[0] + 1, always positive in [1, 10]
            FindZero fz = FindZero.of(() -> holder[0] + 1.0, holder, () -> 1.0, () -> 10.0);

            double result = fz.getCurrentValue();

            // f(1)=2, f(10)=11 → lo is closer to zero
            assertThat(result).isCloseTo(1.0, within(1e-9));
        }

        @Test
        @DisplayName("should return hi when both endpoints negative and hi is closer to zero")
        void shouldReturnCloserEndpointHi() {
            double[] holder = {0.0};
            // expression: holder[0] - 20, always negative in [0, 10]
            FindZero fz = FindZero.of(() -> holder[0] - 20.0, holder, () -> 0.0, () -> 10.0);

            double result = fz.getCurrentValue();

            // f(0)=-20, f(10)=-10 → hi is closer to zero
            assertThat(result).isCloseTo(10.0, within(1e-9));
        }
    }

    @Nested
    @DisplayName("thread safety — isolated holders")
    class ThreadSafety {

        @Test
        @DisplayName("two FindZero instances with separate holders do not interfere")
        void shouldNotInterfereWithSeparateHolders() {
            double[] holder1 = {0.0};
            double[] holder2 = {0.0};

            // fz1 finds root of x - 3 = 0
            FindZero fz1 = FindZero.of(() -> holder1[0] - 3.0, holder1, () -> 0.0, () -> 10.0);
            // fz2 finds root of x - 7 = 0
            FindZero fz2 = FindZero.of(() -> holder2[0] - 7.0, holder2, () -> 0.0, () -> 10.0);

            double result1 = fz1.getCurrentValue();
            double result2 = fz2.getCurrentValue();

            assertThat(result1).isCloseTo(3.0, within(1e-9));
            assertThat(result2).isCloseTo(7.0, within(1e-9));
        }

        @Test
        @DisplayName("holder value is set to final result after getCurrentValue")
        void shouldLeaveHolderAtResult() {
            double[] holder = {0.0};
            FindZero fz = FindZero.of(() -> holder[0] - 5.0, holder, () -> 0.0, () -> 10.0);

            fz.getCurrentValue();

            assertThat(holder[0]).isCloseTo(5.0, within(1e-9));
        }
    }

    @Nested
    @DisplayName("dynamic bounds")
    class DynamicBounds {

        @Test
        @DisplayName("should use current bound values on each call")
        void shouldUseDynamicBounds() {
            double[] holder = {0.0};
            double[] loBound = {0.0};
            double[] hiBound = {10.0};

            FindZero fz = FindZero.of(() -> holder[0] - 5.0, holder,
                    () -> loBound[0], () -> hiBound[0]);

            assertThat(fz.getCurrentValue()).isCloseTo(5.0, within(1e-9));

            // Shift bounds — root is still 5 but now within [2, 8]
            loBound[0] = 2.0;
            hiBound[0] = 8.0;
            assertThat(fz.getCurrentValue()).isCloseTo(5.0, within(1e-9));
        }
    }
}
