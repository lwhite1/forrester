package systems.courant.sd.model;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import systems.courant.sd.model.compile.Resettable;

import java.util.function.DoubleSupplier;
import java.util.function.LongSupplier;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Parameterized test that verifies all stateful formula classes produce consistent
 * results regardless of savePer (step increment). Compares savePer=1 baseline
 * against savePer=2, where the input changes only at save points so that
 * zero-order hold produces exact results.
 *
 * <p>This is the structural regression test for issue #1061.
 */
public class StatefulFormulaCatchUpTest {

    private static final int TOTAL_STEPS = 20;
    private static final double TOLERANCE = 1e-9;

    /**
     * Input function that changes only at even steps (save points for savePer=2).
     * This ensures the zero-order hold is exact: at odd (intermediate) steps,
     * the held value IS the correct value.
     */
    private static double inputAtStep(long step) {
        // Piecewise constant: changes at even steps only
        long savePoint = (step / 2) * 2; // round down to nearest even
        return 50 + savePoint * 5;       // 50, 50, 60, 60, 70, 70, ...
    }

    /**
     * A condition function for SampleIfTrue that is true at even steps.
     */
    private static double conditionAtStep(long step) {
        return (step % 4 == 0) ? 1.0 : 0.0;
    }

    record FormulaFactory(String name,
                          java.util.function.BiFunction<DoubleSupplier, LongSupplier, Formula> create) {
        @Override
        public String toString() {
            return name;
        }
    }

    static Stream<Arguments> formulaFactories() {
        return Stream.of(
                Arguments.of(new FormulaFactory("Delay1", (input, step) ->
                        Delay1.of(input, 6, step))),
                Arguments.of(new FormulaFactory("Delay1I", (input, step) ->
                        Delay1.of(input, 6, 50, step))),
                Arguments.of(new FormulaFactory("Delay3", (input, step) ->
                        Delay3.of(input, 6, step))),
                Arguments.of(new FormulaFactory("Delay3I", (input, step) ->
                        Delay3.of(input, 6, 50, step))),
                Arguments.of(new FormulaFactory("Smooth", (input, step) ->
                        Smooth.of(input, 5, step))),
                Arguments.of(new FormulaFactory("SmoothI", (input, step) ->
                        Smooth.of(input, 5, 50, step))),
                Arguments.of(new FormulaFactory("Smooth3", (input, step) ->
                        Smooth3.of(input, 5, step))),
                Arguments.of(new FormulaFactory("Smooth3I", (input, step) ->
                        Smooth3.of(input, 5, 50, step))),
                Arguments.of(new FormulaFactory("Trend", (input, step) ->
                        Trend.of(input, 5, 0, step))),
                Arguments.of(new FormulaFactory("Forecast", (input, step) ->
                        Forecast.of(input, 5, 3, 0, step))),
                Arguments.of(new FormulaFactory("Npv", (input, step) ->
                        Npv.of(input, 0.05, step))),
                Arguments.of(new FormulaFactory("SampleIfTrue", (input, step) -> {
                    long[] currentStep = {0};
                    // Wire condition to the same step counter
                    DoubleSupplier condition = () -> conditionAtStep(currentStep[0]);
                    // We need a way to sync currentStep — use a wrapper
                    LongSupplier stepWrapper = () -> {
                        currentStep[0] = step.getAsLong();
                        return currentStep[0];
                    };
                    return SampleIfTrue.of(condition, input, 0, stepWrapper);
                }))
        );
    }

    @ParameterizedTest(name = "{0}: savePer=1 vs savePer=2")
    @MethodSource("formulaFactories")
    void shouldMatchBaselineWhenSavePerIs2(FormulaFactory factory) {
        // --- Run with savePer=1 (baseline) ---
        long[] stepHolder1 = {0};
        Formula baseline = factory.create().apply(
                () -> inputAtStep(stepHolder1[0]),
                () -> stepHolder1[0]);

        double[] baselineResults = new double[TOTAL_STEPS + 1];
        baselineResults[0] = baseline.getCurrentValue(); // step 0
        for (int s = 1; s <= TOTAL_STEPS; s++) {
            stepHolder1[0] = s;
            baselineResults[s] = baseline.getCurrentValue();
        }

        // --- Run with savePer=2 ---
        long[] stepHolder2 = {0};
        Formula catchUp = factory.create().apply(
                () -> inputAtStep(stepHolder2[0]),
                () -> stepHolder2[0]);

        double[] catchUpResults = new double[TOTAL_STEPS + 1];
        catchUpResults[0] = catchUp.getCurrentValue(); // step 0
        for (int s = 2; s <= TOTAL_STEPS; s += 2) {
            stepHolder2[0] = s;
            catchUpResults[s] = catchUp.getCurrentValue();
        }

        // Compare at save points (every 2 steps)
        for (int s = 0; s <= TOTAL_STEPS; s += 2) {
            assertEquals(baselineResults[s], catchUpResults[s], TOLERANCE,
                    factory.name() + " mismatch at step " + s
                            + ": baseline=" + baselineResults[s]
                            + " catchUp=" + catchUpResults[s]);
        }
    }

    @ParameterizedTest(name = "{0}: savePer=1 vs savePer=4")
    @MethodSource("formulaFactories")
    void shouldMatchBaselineWhenSavePerIs4(FormulaFactory factory) {
        // Use input that changes only at multiples of 4
        java.util.function.LongFunction<Double> input4 = step -> {
            long savePoint = (step / 4) * 4;
            return 50 + savePoint * 5.0;
        };
        java.util.function.LongFunction<Double> condition4 = step ->
                (step % 8 == 0) ? 1.0 : 0.0;

        // --- Run with savePer=1 (baseline) ---
        long[] stepHolder1 = {0};
        Formula baseline;
        if (factory.name().equals("SampleIfTrue")) {
            baseline = SampleIfTrue.of(
                    () -> condition4.apply(stepHolder1[0]),
                    () -> input4.apply(stepHolder1[0]),
                    0, () -> stepHolder1[0]);
        } else {
            baseline = factory.create().apply(
                    () -> input4.apply(stepHolder1[0]),
                    () -> stepHolder1[0]);
        }

        int totalSteps = 20;
        double[] baselineResults = new double[totalSteps + 1];
        baselineResults[0] = baseline.getCurrentValue();
        for (int s = 1; s <= totalSteps; s++) {
            stepHolder1[0] = s;
            baselineResults[s] = baseline.getCurrentValue();
        }

        // --- Run with savePer=4 ---
        long[] stepHolder2 = {0};
        Formula catchUp;
        if (factory.name().equals("SampleIfTrue")) {
            catchUp = SampleIfTrue.of(
                    () -> condition4.apply(stepHolder2[0]),
                    () -> input4.apply(stepHolder2[0]),
                    0, () -> stepHolder2[0]);
        } else {
            catchUp = factory.create().apply(
                    () -> input4.apply(stepHolder2[0]),
                    () -> stepHolder2[0]);
        }

        double[] catchUpResults = new double[totalSteps + 1];
        catchUpResults[0] = catchUp.getCurrentValue();
        for (int s = 4; s <= totalSteps; s += 4) {
            stepHolder2[0] = s;
            catchUpResults[s] = catchUp.getCurrentValue();
        }

        // Compare at save points (every 4 steps)
        for (int s = 0; s <= totalSteps; s += 4) {
            assertEquals(baselineResults[s], catchUpResults[s], TOLERANCE,
                    factory.name() + " mismatch at step " + s
                            + ": baseline=" + baselineResults[s]
                            + " catchUp=" + catchUpResults[s]);
        }
    }
}
