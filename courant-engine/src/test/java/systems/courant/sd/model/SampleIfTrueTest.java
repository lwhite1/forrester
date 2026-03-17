package systems.courant.sd.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("SampleIfTrue")
class SampleIfTrueTest {

    @Nested
    @DisplayName("Step 0 behavior (#534)")
    class StepZero {

        @Test
        @DisplayName("should sample input at step 0 when condition is true")
        void shouldSampleInputAtStepZeroWhenConditionTrue() {
            int[] step = {0};
            double[] input = {42.0};
            double[] cond = {1.0};
            SampleIfTrue formula = SampleIfTrue.of(
                    () -> cond[0], () -> input[0], 99.0, () -> step[0]);

            assertThat(formula.getCurrentValue()).isEqualTo(42.0);
        }

        @Test
        @DisplayName("should return initial value at step 0 when condition is false")
        void shouldReturnInitialAtStepZeroWhenConditionFalse() {
            int[] step = {0};
            double[] input = {42.0};
            double[] cond = {0.0};
            SampleIfTrue formula = SampleIfTrue.of(
                    () -> cond[0], () -> input[0], 99.0, () -> step[0]);

            assertThat(formula.getCurrentValue()).isEqualTo(99.0);
        }
    }

    @Nested
    @DisplayName("Hold behavior")
    class Hold {

        @Test
        @DisplayName("should hold last sampled value when condition becomes false")
        void shouldHoldWhenConditionBecomesFalse() {
            int[] step = {0};
            double[] input = {10.0};
            double[] cond = {1.0};
            SampleIfTrue formula = SampleIfTrue.of(
                    () -> cond[0], () -> input[0], 0.0, () -> step[0]);

            // Step 0: condition true, sample 10
            assertThat(formula.getCurrentValue()).isEqualTo(10.0);

            // Step 1: condition false, input changes to 20, should hold 10
            step[0] = 1;
            cond[0] = 0.0;
            input[0] = 20.0;
            assertThat(formula.getCurrentValue()).isEqualTo(10.0);

            // Step 2: condition true, input 30, should sample 30
            step[0] = 2;
            cond[0] = 1.0;
            input[0] = 30.0;
            assertThat(formula.getCurrentValue()).isEqualTo(30.0);

            // Step 3: condition false again, should hold 30
            step[0] = 3;
            cond[0] = 0.0;
            input[0] = 99.0;
            assertThat(formula.getCurrentValue()).isEqualTo(30.0);
        }

        @Test
        @DisplayName("should hold initial value when condition is always false")
        void shouldHoldInitialWhenAlwaysFalse() {
            int[] step = {0};
            double[] input = {42.0};
            SampleIfTrue formula = SampleIfTrue.of(
                    () -> 0.0, () -> input[0], 5.0, () -> step[0]);

            assertThat(formula.getCurrentValue()).isEqualTo(5.0);

            step[0] = 1;
            input[0] = 100.0;
            assertThat(formula.getCurrentValue()).isEqualTo(5.0);

            step[0] = 2;
            assertThat(formula.getCurrentValue()).isEqualTo(5.0);
        }

        @Test
        @DisplayName("should always sample when condition is always true")
        void shouldAlwaysSampleWhenAlwaysTrue() {
            int[] step = {0};
            double[] input = {10.0};
            SampleIfTrue formula = SampleIfTrue.of(
                    () -> 1.0, () -> input[0], 0.0, () -> step[0]);

            assertThat(formula.getCurrentValue()).isEqualTo(10.0);

            step[0] = 1;
            input[0] = 20.0;
            assertThat(formula.getCurrentValue()).isEqualTo(20.0);

            step[0] = 2;
            input[0] = 30.0;
            assertThat(formula.getCurrentValue()).isEqualTo(30.0);
        }
    }

    @Nested
    @DisplayName("Idempotency")
    class Idempotency {

        @Test
        @DisplayName("multiple calls within same step return same value")
        void shouldBeIdempotentWithinSameStep() {
            int[] step = {0};
            double[] input = {10.0};
            double[] cond = {1.0};
            SampleIfTrue formula = SampleIfTrue.of(
                    () -> cond[0], () -> input[0], 0.0, () -> step[0]);

            double first = formula.getCurrentValue();
            // Change input between calls — should not re-sample within same step
            input[0] = 999.0;
            double second = formula.getCurrentValue();
            assertThat(second).isEqualTo(first);
        }
    }

    @Nested
    @DisplayName("Reset")
    class ResetBehavior {

        @Test
        @DisplayName("reset clears state and allows re-initialization")
        void shouldResetState() {
            int[] step = {0};
            double[] input = {10.0};
            double[] cond = {1.0};
            SampleIfTrue formula = SampleIfTrue.of(
                    () -> cond[0], () -> input[0], 0.0, () -> step[0]);

            assertThat(formula.getCurrentValue()).isEqualTo(10.0);

            formula.reset();
            step[0] = 0;
            cond[0] = 0.0;
            input[0] = 50.0;
            // After reset, should re-initialize: condition false -> initial value 0
            assertThat(formula.getCurrentValue()).isEqualTo(0.0);
        }
    }

    @Nested
    @DisplayName("NaN condition (#871)")
    class NanCondition {

        @Test
        @DisplayName("should treat NaN condition as false at step 0")
        void shouldTreatNanAsFalseAtStepZero() {
            int[] step = {0};
            double[] input = {42.0};
            SampleIfTrue formula = SampleIfTrue.of(
                    () -> Double.NaN, () -> input[0], 99.0, () -> step[0]);

            assertThat(formula.getCurrentValue()).isEqualTo(99.0);
        }

        @Test
        @DisplayName("should treat NaN condition as false and hold previous value")
        void shouldTreatNanAsFalseAndHold() {
            int[] step = {0};
            double[] input = {10.0};
            double[] cond = {1.0};
            SampleIfTrue formula = SampleIfTrue.of(
                    () -> cond[0], () -> input[0], 0.0, () -> step[0]);

            // Step 0: condition true, sample 10
            assertThat(formula.getCurrentValue()).isEqualTo(10.0);

            // Step 1: condition NaN, input changes to 20, should hold 10
            step[0] = 1;
            cond[0] = Double.NaN;
            input[0] = 20.0;
            assertThat(formula.getCurrentValue()).isEqualTo(10.0);
        }
    }

    @Nested
    @DisplayName("Null guards")
    class NullGuards {

        @Test
        @DisplayName("null condition supplier throws NullPointerException")
        void shouldRejectNullCondition() {
            assertThatThrownBy(() ->
                    SampleIfTrue.of(null, () -> 1.0, 0.0, () -> 0))
                    .isInstanceOf(NullPointerException.class);
        }

        @Test
        @DisplayName("null input supplier throws NullPointerException")
        void shouldRejectNullInput() {
            assertThatThrownBy(() ->
                    SampleIfTrue.of(() -> 1.0, null, 0.0, () -> 0))
                    .isInstanceOf(NullPointerException.class);
        }

        @Test
        @DisplayName("null currentStep supplier throws NullPointerException")
        void shouldRejectNullStep() {
            assertThatThrownBy(() ->
                    SampleIfTrue.of(() -> 1.0, () -> 1.0, 0.0, null))
                    .isInstanceOf(NullPointerException.class);
        }
    }
}
