package systems.courant.shrewd.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("DelayFixed")
class DelayFixedTest {

    @Nested
    @DisplayName("Initialization")
    class Initialization {

        @Test
        @DisplayName("should return initial value at step 0")
        void shouldReturnInitialValueAtStepZero() {
            int[] step = {0};
            DelayFixed delay = DelayFixed.of(() -> 100, 3, 50, () -> step[0]);
            assertThat(delay.getCurrentValue()).isEqualTo(50);
        }

        @Test
        @DisplayName("should reject non-positive delaySteps")
        void shouldRejectNonPositiveDelay() {
            assertThatThrownBy(() -> DelayFixed.of(() -> 100, 0, 50, () -> 0))
                    .isInstanceOf(IllegalArgumentException.class);
            assertThatThrownBy(() -> DelayFixed.of(() -> 100, -1, 50, () -> 0))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("should support dynamic initial value supplier")
        void shouldUseDynamicInitialValue() {
            int[] step = {0};
            double[] initVal = {42};
            DelayFixed delay = DelayFixed.of(() -> 100, 3, () -> initVal[0], () -> step[0]);
            assertThat(delay.getCurrentValue()).isEqualTo(42);
        }
    }

    @Nested
    @DisplayName("Off-by-one fix (#418)")
    class OffByOneFix {

        @Test
        @DisplayName("should output input(0) after exactly delaySteps steps, not delaySteps+1")
        void shouldHaveCorrectDelay() {
            int[] step = {0};
            double[] input = {100};
            DelayFixed delay = DelayFixed.of(() -> input[0], 3, 0, () -> step[0]);

            // Step 0: input=100, output=initial(0) — delay hasn't elapsed
            assertThat(delay.getCurrentValue()).isEqualTo(0);

            // Steps 1-2: change input, output still initial
            input[0] = 200;
            step[0] = 1;
            assertThat(delay.getCurrentValue()).isEqualTo(0);

            step[0] = 2;
            assertThat(delay.getCurrentValue()).isEqualTo(0);

            // Step 3: exactly delaySteps=3 steps after step 0, output should be input(0)=100
            step[0] = 3;
            assertThat(delay.getCurrentValue()).isEqualTo(100);
        }

        @Test
        @DisplayName("should output step-shifted copy of input for delay=1")
        void shouldDelayByOneStep() {
            int[] step = {0};
            double[] input = {10};
            DelayFixed delay = DelayFixed.of(() -> input[0], 1, 0, () -> step[0]);

            // Step 0: output=initial
            assertThat(delay.getCurrentValue()).isEqualTo(0);

            // Step 1: output=input(0)=10
            input[0] = 20;
            step[0] = 1;
            assertThat(delay.getCurrentValue()).isEqualTo(10);

            // Step 2: output=input(1)=20
            input[0] = 30;
            step[0] = 2;
            assertThat(delay.getCurrentValue()).isEqualTo(20);
        }

        @Test
        @DisplayName("should produce correct pipeline over many steps")
        void shouldProduceCorrectPipeline() {
            int[] step = {0};
            double[] input = {0};
            int delaySteps = 5;
            DelayFixed delay = DelayFixed.of(() -> input[0], delaySteps, 0, () -> step[0]);

            // Record inputs and verify outputs are delayed by exactly delaySteps
            double[] inputs = new double[15];
            for (int i = 0; i < 15; i++) {
                input[0] = (i + 1) * 10;
                inputs[i] = input[0];
                step[0] = i;
                double output = delay.getCurrentValue();

                if (i < delaySteps) {
                    assertThat(output).as("Step %d: output should be initial value", i)
                            .isEqualTo(0);
                } else {
                    assertThat(output).as("Step %d: output should be input(%d)=%f",
                                    i, i - delaySteps, inputs[i - delaySteps])
                            .isEqualTo(inputs[i - delaySteps]);
                }
            }
        }
    }

    @Nested
    @DisplayName("Catch-up loop fix (#418)")
    class CatchUpLoopFix {

        @Test
        @DisplayName("should read input exactly once regardless of delta")
        void shouldReadInputOnce() {
            int[] step = {0};
            int[] readCount = {0};
            DelayFixed delay = DelayFixed.of(
                    () -> { readCount[0]++; return 100; }, 5, 100, () -> step[0]);

            delay.getCurrentValue(); // initialize (reads once)
            readCount[0] = 0;

            // Jump by 4 steps — input should be read exactly once
            step[0] = 4;
            delay.getCurrentValue();
            assertThat(readCount[0]).as("Input should be read once, not once per delta")
                    .isEqualTo(1);
        }

        @Test
        @DisplayName("should use last-known input for missed slots and current input for final slot")
        void shouldUseLastKnownInputForMissedSlots() {
            int[] step = {0};
            double[] input = {0};
            DelayFixed delay = DelayFixed.of(() -> input[0], 3, 0, () -> step[0]);

            // Step 0: input=0, buffer=[0,0,0,0], write 0 at 0
            delay.getCurrentValue();

            // Jump to step 3 (delta=3): input=50
            // Missed slots (steps 1,2) filled with last-known input (0)
            // Current step (3) gets input=50
            input[0] = 50;
            step[0] = 3;
            assertThat(delay.getCurrentValue()).as("step 3: oldest entry is input(0)=0").isEqualTo(0);

            // Steps 4,5: output is the last-known fill (0) from missed steps 1,2
            input[0] = 99;
            step[0] = 4;
            assertThat(delay.getCurrentValue()).as("step 4: held value from missed step 1").isEqualTo(0);
            step[0] = 5;
            assertThat(delay.getCurrentValue()).as("step 5: held value from missed step 2").isEqualTo(0);

            // Step 6: output is the input written at step 3 (50)
            step[0] = 6;
            assertThat(delay.getCurrentValue()).as("step 6: input from step 3").isEqualTo(50);
        }

        @Test
        @DisplayName("should match step-by-step when delta is always 1")
        void shouldMatchStepByStepWithNoCatchUp() {
            int[] step = {0};
            double[] input = {10};
            DelayFixed delay = DelayFixed.of(() -> input[0], 2, 0, () -> step[0]);

            // Step 0: write 10
            assertThat(delay.getCurrentValue()).isEqualTo(0);

            // Step 1: write 20
            input[0] = 20;
            step[0] = 1;
            assertThat(delay.getCurrentValue()).isEqualTo(0);

            // Step 2: output = input(0) = 10
            input[0] = 30;
            step[0] = 2;
            assertThat(delay.getCurrentValue()).isEqualTo(10);

            // Step 3: output = input(1) = 20
            input[0] = 40;
            step[0] = 3;
            assertThat(delay.getCurrentValue()).isEqualTo(20);
        }
    }

    @Nested
    @DisplayName("Same-step idempotency")
    class SameStepIdempotency {

        @Test
        @DisplayName("should return same value when called multiple times at same step")
        void shouldBeIdempotentWithinStep() {
            int[] step = {0};
            DelayFixed delay = DelayFixed.of(() -> 100, 3, 50, () -> step[0]);

            double first = delay.getCurrentValue();
            double second = delay.getCurrentValue();
            double third = delay.getCurrentValue();
            assertThat(first).isEqualTo(second).isEqualTo(third);
        }
    }

    @Nested
    @DisplayName("Reset")
    class ResetTests {

        @Test
        @DisplayName("should return initial value after reset")
        void shouldResetToInitialState() {
            int[] step = {0};
            DelayFixed delay = DelayFixed.of(() -> 100, 3, 0, () -> step[0]);

            delay.getCurrentValue();
            step[0] = 1;
            delay.getCurrentValue();

            delay.reset();
            step[0] = 0;
            assertThat(delay.getCurrentValue()).isEqualTo(0);
        }
    }

    @Nested
    @DisplayName("Constant input")
    class ConstantInput {

        @Test
        @DisplayName("should remain stable when input equals initial value")
        void shouldRemainStable() {
            int[] step = {0};
            DelayFixed delay = DelayFixed.of(() -> 50, 3, 50, () -> step[0]);

            for (int i = 0; i <= 10; i++) {
                step[0] = i;
                assertThat(delay.getCurrentValue()).as("Step %d", i).isEqualTo(50);
            }
        }
    }
}
