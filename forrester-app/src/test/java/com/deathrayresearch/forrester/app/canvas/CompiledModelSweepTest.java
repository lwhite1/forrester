package com.deathrayresearch.forrester.app.canvas;

import com.deathrayresearch.forrester.model.compile.CompiledModel;
import com.deathrayresearch.forrester.model.compile.ModelCompiler;
import com.deathrayresearch.forrester.model.def.ModelDefinition;
import com.deathrayresearch.forrester.model.def.ModelDefinitionBuilder;
import com.deathrayresearch.forrester.model.def.SimulationSettings;
import com.deathrayresearch.forrester.sweep.MultiParameterSweep;
import com.deathrayresearch.forrester.sweep.MultiSweepResult;
import com.deathrayresearch.forrester.sweep.ParameterSweep;
import com.deathrayresearch.forrester.sweep.RunResult;
import com.deathrayresearch.forrester.sweep.SweepResult;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.function.DoubleFunction;
import java.util.function.Function;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies that parameter sweeps using compiled models (from ModelDefinitions)
 * produce different results for different parameter values. This is a regression
 * test for issue #119 where the step counter was not synchronized, causing
 * STEP/RAMP/PULSE functions to malfunction during sweeps.
 */
@DisplayName("Compiled model sweep step synchronization")
class CompiledModelSweepTest {

    private static ModelDefinition bathtub() {
        return new ModelDefinitionBuilder()
                .name("Bathtub")
                .stock("Water_in_Tub", 50, "Gallon")
                .flow("Outflow", "MIN(Outflow_Rate, Water_in_Tub)", "Minute",
                        "Water_in_Tub", null)
                .flow("Inflow", "STEP(Inflow_Rate, 5)", "Minute",
                        null, "Water_in_Tub")
                .constant("Outflow_Rate", 5, "Gallon per Minute")
                .constant("Inflow_Rate", 5, "Gallon per Minute")
                .defaultSimulation("Minute", 10, "Minute")
                .build();
    }

    @Test
    @DisplayName("multi-parameter sweep produces different results per combination")
    void multiParameterSweepProducesDifferentResults() {
        ModelDefinition def = bathtub();
        SimulationSettings settings = def.defaultSimulation();
        Function<Map<String, Double>, CompiledModel> factory =
                ModelDefinitionFactory.createFactory(def, settings);

        MultiSweepResult result = MultiParameterSweep.builder()
                .compiledModelFactory(factory)
                .parameter("Outflow_Rate", new double[]{0, 5, 10})
                .parameter("Inflow_Rate", new double[]{0, 5, 10})
                .timeStep(ModelDefinitionFactory.resolveTimeStep(settings))
                .duration(ModelDefinitionFactory.resolveDuration(settings))
                .build()
                .execute();

        assertThat(result.getRunCount()).isEqualTo(9);

        // With Outflow=0, Inflow=0: water stays at 50 (no flows active)
        // With Outflow=10, Inflow=0: water drains to 0
        // With Outflow=0, Inflow=10: water increases after step 5
        // These must be different
        RunResult noFlow = findRun(result, 0, 0);
        RunResult drainOnly = findRun(result, 10, 0);
        RunResult fillOnly = findRun(result, 0, 10);

        double noFlowFinal = noFlow.getFinalStockValue("Water_in_Tub");
        double drainFinal = drainOnly.getFinalStockValue("Water_in_Tub");
        double fillFinal = fillOnly.getFinalStockValue("Water_in_Tub");

        assertThat(noFlowFinal)
                .as("No-flow tub should stay at 50")
                .isEqualTo(50.0);
        assertThat(drainFinal)
                .as("Drain-only tub should be less than 50")
                .isLessThan(50.0);
        assertThat(fillFinal)
                .as("Fill-only tub should be greater than 50 (STEP fires at step 5)")
                .isGreaterThan(50.0);
    }

    @Test
    @DisplayName("single-parameter sweep produces different results per value")
    void singleParameterSweepProducesDifferentResults() {
        ModelDefinition def = bathtub();
        SimulationSettings settings = def.defaultSimulation();
        DoubleFunction<CompiledModel> factory =
                ModelDefinitionFactory.createSingleParamFactory(def, settings, "Outflow_Rate");

        SweepResult result = ParameterSweep.builder()
                .parameterName("Outflow_Rate")
                .parameterValues(new double[]{0, 5, 10})
                .compiledModelFactory(factory)
                .timeStep(ModelDefinitionFactory.resolveTimeStep(settings))
                .duration(ModelDefinitionFactory.resolveDuration(settings))
                .build()
                .execute();

        assertThat(result.getResults()).hasSize(3);

        double finalAt0 = result.getResults().get(0).getFinalStockValue("Water_in_Tub");
        double finalAt5 = result.getResults().get(1).getFinalStockValue("Water_in_Tub");
        double finalAt10 = result.getResults().get(2).getFinalStockValue("Water_in_Tub");

        assertThat(finalAt0).isGreaterThan(finalAt5);
        assertThat(finalAt5).isGreaterThan(finalAt10);
    }

    @Test
    @DisplayName("STEP function fires at correct time in compiled sweep")
    void stepFunctionFiresAtCorrectTime() {
        ModelDefinition def = bathtub();
        SimulationSettings settings = def.defaultSimulation();

        // Use a model with zero outflow to isolate inflow behavior
        Function<Map<String, Double>, CompiledModel> factory =
                ModelDefinitionFactory.createFactory(def, settings);

        MultiSweepResult result = MultiParameterSweep.builder()
                .compiledModelFactory(factory)
                .parameter("Outflow_Rate", new double[]{0})
                .parameter("Inflow_Rate", new double[]{10})
                .timeStep(ModelDefinitionFactory.resolveTimeStep(settings))
                .duration(ModelDefinitionFactory.resolveDuration(settings))
                .build()
                .execute();

        RunResult run = result.getResult(0);

        // STEP(10, 5) means inflow=0 for steps 0-4, inflow=10 for steps 5+
        // With no outflow, stock should be 50 at steps 0-4
        double valueAtStep4 = run.getStockValuesAtStep(4)[0];
        assertThat(valueAtStep4)
                .as("Stock should be 50 before STEP fires at step 5")
                .isEqualTo(50.0);

        // After step 5, inflow=10 per step, so stock should increase
        double finalValue = run.getFinalStockValue("Water_in_Tub");
        assertThat(finalValue)
                .as("Stock should be > 50 after STEP inflow fires")
                .isGreaterThan(50.0);
    }

    private RunResult findRun(MultiSweepResult result, double outflowRate, double inflowRate) {
        for (int i = 0; i < result.getRunCount(); i++) {
            RunResult run = result.getResult(i);
            Map<String, Double> params = run.getParameterMap();
            if (params.get("Outflow_Rate") == outflowRate
                    && params.get("Inflow_Rate") == inflowRate) {
                return run;
            }
        }
        throw new IllegalStateException(
                "Run not found for Outflow_Rate=" + outflowRate + ", Inflow_Rate=" + inflowRate);
    }
}
