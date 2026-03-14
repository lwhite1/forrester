package systems.courant.sd.sweep;

import systems.courant.sd.NonFiniteValueException;
import systems.courant.sd.Simulation;
import systems.courant.sd.SimulationCancelledException;
import systems.courant.sd.measure.Quantity;
import systems.courant.sd.measure.TimeUnit;
import systems.courant.sd.model.compile.CompiledModel;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Runs extreme-condition tests on a model by systematically setting each parameter to
 * extreme values (zero, 10x baseline, negative) and inspecting the results for anomalies
 * such as NaN, Infinity, or negative stock values.
 *
 * <p>This implements Forrester &amp; Senge's extreme-condition validation test:
 * "Does the model behave reasonably when inputs are zero? Very large? Negative?"
 *
 * <pre>{@code
 * ExtremeConditionResult result = ExtremeConditionTest.builder()
 *     .compiledModelFactory(ModelDefinitionFactory.createFactory(def, settings))
 *     .parameters(def.parameters())
 *     .timeStep(timeStep)
 *     .duration(duration)
 *     .build()
 *     .execute();
 * }</pre>
 */
public class ExtremeConditionTest {

    private static final Logger log = LoggerFactory.getLogger(ExtremeConditionTest.class);

    /** Default threshold for "unreasonably large" values. */
    private static final double DEFAULT_BOUND_THRESHOLD = 1e15;

    private static final Pattern STOCK_NAME_PATTERN = Pattern.compile("Stock '([^']+)'");
    private static final Pattern FLOW_NAME_PATTERN = Pattern.compile("Flow '([^']+)'");
    private static final Pattern STEP_PATTERN = Pattern.compile("at step (\\d+)");

    private final Function<Map<String, Double>, CompiledModel> compiledModelFactory;
    private final List<ParameterInfo> parameters;
    private final TimeUnit timeStep;
    private final Quantity duration;
    private final double boundThreshold;

    private ExtremeConditionTest(Builder builder) {
        this.compiledModelFactory = builder.compiledModelFactory;
        this.parameters = List.copyOf(builder.parameters);
        this.timeStep = builder.timeStep;
        this.duration = builder.duration;
        this.boundThreshold = builder.boundThreshold;
    }

    /**
     * Executes extreme-condition tests for all parameters and conditions.
     *
     * @return the test results containing all findings
     * @throws SimulationCancelledException if the thread is interrupted during execution
     */
    public ExtremeConditionResult execute() {
        List<ExtremeConditionFinding> findings = new ArrayList<>();
        int totalRuns = parameters.size() * ExtremeCondition.values().length;
        int completed = 0;

        for (ParameterInfo param : parameters) {
            for (ExtremeCondition condition : ExtremeCondition.values()) {
                if (Thread.interrupted()) {
                    throw new SimulationCancelledException(
                            "Extreme condition test cancelled at run " + completed + "/" + totalRuns);
                }

                double extremeValue = condition.apply(param.baselineValue());
                runSingleTest(param, condition, extremeValue, findings);
                completed++;
            }
        }

        return new ExtremeConditionResult(findings, completed, totalRuns);
    }

    private void runSingleTest(ParameterInfo param, ExtremeCondition condition,
                               double extremeValue, List<ExtremeConditionFinding> findings) {
        try {
            CompiledModel compiled = compiledModelFactory.apply(
                    Map.of(param.name(), extremeValue));
            Simulation simulation = compiled.createSimulation(timeStep, duration);
            simulation.setStrictMode(true);

            RunResult runResult = new RunResult(Map.of(param.name(), extremeValue));
            simulation.addEventHandler(runResult);
            simulation.execute();

            // Post-run inspection: check for negative stocks and extreme values
            inspectRunResult(param, condition, extremeValue, runResult, findings);
        } catch (NonFiniteValueException e) {
            String affectedVar = extractVariableName(e.getMessage());
            long step = extractStepNumber(e.getMessage());
            findings.add(new ExtremeConditionFinding(
                    param.name(), param.baselineValue(), condition, extremeValue,
                    affectedVar, step, e.getMessage()));
        } catch (SimulationCancelledException e) {
            throw e;
        } catch (Exception e) {
            log.debug("Extreme condition test failed for {} = {} ({}): {}",
                    param.name(), extremeValue, condition.label(), e.getMessage());
            findings.add(new ExtremeConditionFinding(
                    param.name(), param.baselineValue(), condition, extremeValue,
                    "", -1, "Simulation failed: " + e.getMessage()));
        }
    }

    private void inspectRunResult(ParameterInfo param, ExtremeCondition condition,
                                  double extremeValue, RunResult runResult,
                                  List<ExtremeConditionFinding> findings) {
        List<String> stockNames = runResult.getStockNames();
        List<String> variableNames = runResult.getVariableNames();

        for (int stepIdx = 0; stepIdx < runResult.getStepCount(); stepIdx++) {
            long step = runResult.getStep(stepIdx);
            double[] stockValues = runResult.getStockValuesAtStep(stepIdx);
            double[] varValues = runResult.getVariableValuesAtStep(stepIdx);

            // Check stocks for negative values and extreme magnitudes
            for (int s = 0; s < stockNames.size(); s++) {
                double value = stockValues[s];
                if (value < 0) {
                    findings.add(new ExtremeConditionFinding(
                            param.name(), param.baselineValue(), condition, extremeValue,
                            stockNames.get(s), step,
                            "Stock '" + stockNames.get(s) + "' went negative ("
                                    + value + ") at step " + step));
                    // Only report first negative occurrence per stock per run
                    return;
                }
                if (Math.abs(value) > boundThreshold) {
                    findings.add(new ExtremeConditionFinding(
                            param.name(), param.baselineValue(), condition, extremeValue,
                            stockNames.get(s), step,
                            "Stock '" + stockNames.get(s) + "' exceeded bound threshold ("
                                    + value + ") at step " + step));
                    return;
                }
            }

            // Check variables for extreme magnitudes
            for (int v = 0; v < variableNames.size(); v++) {
                double value = varValues[v];
                if (!Double.isFinite(value)) {
                    findings.add(new ExtremeConditionFinding(
                            param.name(), param.baselineValue(), condition, extremeValue,
                            variableNames.get(v), step,
                            "Variable '" + variableNames.get(v) + "' became "
                                    + value + " at step " + step));
                    return;
                }
                if (Math.abs(value) > boundThreshold) {
                    findings.add(new ExtremeConditionFinding(
                            param.name(), param.baselineValue(), condition, extremeValue,
                            variableNames.get(v), step,
                            "Variable '" + variableNames.get(v) + "' exceeded bound threshold ("
                                    + value + ") at step " + step));
                    return;
                }
            }
        }
    }

    private static String extractVariableName(String message) {
        if (message == null) {
            return "";
        }
        Matcher stockMatcher = STOCK_NAME_PATTERN.matcher(message);
        if (stockMatcher.find()) {
            return stockMatcher.group(1);
        }
        Matcher flowMatcher = FLOW_NAME_PATTERN.matcher(message);
        if (flowMatcher.find()) {
            return flowMatcher.group(1);
        }
        return "";
    }

    private static long extractStepNumber(String message) {
        if (message == null) {
            return -1;
        }
        Matcher matcher = STEP_PATTERN.matcher(message);
        if (matcher.find()) {
            return Long.parseLong(matcher.group(1));
        }
        return -1;
    }

    /**
     * Returns a new builder for configuring an extreme-condition test.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Holds name and baseline value for a parameter under test.
     */
    record ParameterInfo(String name, double baselineValue) {
    }

    /**
     * Builder for {@link ExtremeConditionTest}.
     */
    public static class Builder {

        private Function<Map<String, Double>, CompiledModel> compiledModelFactory;
        private final List<ParameterInfo> parameters = new ArrayList<>();
        private TimeUnit timeStep;
        private Quantity duration;
        private double boundThreshold = DEFAULT_BOUND_THRESHOLD;

        private Builder() {
        }

        /**
         * Sets the factory function that builds a fresh compiled model with parameter overrides.
         *
         * @param compiledModelFactory a function that receives parameter overrides and returns a compiled model
         * @return this builder
         */
        public Builder compiledModelFactory(
                Function<Map<String, Double>, CompiledModel> compiledModelFactory) {
            this.compiledModelFactory = compiledModelFactory;
            return this;
        }

        /**
         * Adds a parameter to test with its baseline value.
         *
         * @param name          the parameter name
         * @param baselineValue the original parameter value
         * @return this builder
         */
        public Builder parameter(String name, double baselineValue) {
            this.parameters.add(new ParameterInfo(name, baselineValue));
            return this;
        }

        /**
         * Sets the simulation time step.
         *
         * @param timeStep the time unit for each step
         * @return this builder
         */
        public Builder timeStep(TimeUnit timeStep) {
            this.timeStep = timeStep;
            return this;
        }

        /**
         * Sets the simulation duration.
         *
         * @param duration the total simulation time
         * @return this builder
         */
        public Builder duration(Quantity duration) {
            this.duration = duration;
            return this;
        }

        /**
         * Sets the threshold for detecting unreasonably large values.
         * Defaults to 1e15.
         *
         * @param boundThreshold the threshold value
         * @return this builder
         */
        public Builder boundThreshold(double boundThreshold) {
            this.boundThreshold = boundThreshold;
            return this;
        }

        /**
         * Builds the {@link ExtremeConditionTest} instance.
         *
         * @return a configured test ready to execute
         * @throws IllegalStateException if any required field is missing
         */
        public ExtremeConditionTest build() {
            if (compiledModelFactory == null) {
                throw new IllegalStateException("compiledModelFactory is required");
            }
            if (parameters.isEmpty()) {
                throw new IllegalStateException("At least one parameter is required");
            }
            if (timeStep == null) {
                throw new IllegalStateException("timeStep is required");
            }
            if (duration == null) {
                throw new IllegalStateException("duration is required");
            }
            return new ExtremeConditionTest(this);
        }
    }
}
