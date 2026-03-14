package systems.courant.sd.sweep;

import systems.courant.sd.model.compile.CompiledModel;
import systems.courant.sd.model.compile.ModelCompiler;
import systems.courant.sd.model.def.ModelDefinition;
import systems.courant.sd.model.def.ModelDefinitionBuilder;
import systems.courant.sd.model.def.VariableDef;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("ExtremeConditionTest")
class ExtremeConditionTestTest {

    /**
     * Creates a compiled model factory that applies parameter overrides to the given
     * base definition, recompiling each time.
     */
    private static Function<Map<String, Double>, CompiledModel> factoryFor(ModelDefinition baseDef) {
        return overrides -> {
            List<VariableDef> updatedVars = new ArrayList<>();
            for (VariableDef v : baseDef.variables()) {
                if (overrides.containsKey(v.name())) {
                    updatedVars.add(new VariableDef(v.name(), v.comment(),
                            VariableDef.formatValue(overrides.get(v.name())), v.unit()));
                } else {
                    updatedVars.add(v);
                }
            }
            ModelDefinitionBuilder b = baseDef.toBuilder();
            b.clearVariables();
            updatedVars.forEach(b::variable);
            ModelDefinition overridden = b.build();
            return new ModelCompiler().compile(overridden);
        };
    }

    @Nested
    @DisplayName("NaN/Infinity detection")
    class NanDetection {

        @Test
        @DisplayName("should detect NaN from division by zero when parameter is set to zero")
        void shouldDetectNaNFromDivisionByZero() {
            // Model: Stock "Population" with inflow "growth" = Population * growth_rate / divisor
            // When divisor = 0, flow produces NaN/Infinity
            ModelDefinition def = new ModelDefinitionBuilder()
                    .name("DivByZero")
                    .stock("Population", 100, "People")
                    .flow("growth", "Population * growth_rate / divisor", "Day",
                            null, "Population")
                    .variable("growth_rate", "0.1", "1/Day")
                    .variable("divisor", "10", "Dimensionless")
                    .defaultSimulation("Day", 10, "Day")
                    .build();

            ExtremeConditionResult result = ExtremeConditionTest.builder()
                    .compiledModelFactory(factoryFor(def))
                    .parameter("divisor", 10.0)
                    .timeStep(resolveTimeStep(def))
                    .duration(resolveDuration(def))
                    .build()
                    .execute();

            // The ZERO condition should trigger a NaN/Infinity finding
            assertThat(result.findings()).anySatisfy(f -> {
                assertThat(f.condition()).isEqualTo(ExtremeCondition.ZERO);
                assertThat(f.parameterName()).isEqualTo("divisor");
                assertThat(f.appliedValue()).isEqualTo(0.0);
            });
        }

        @Test
        @DisplayName("should detect Infinity from unbounded growth with 10x parameter")
        void shouldDetectInfinityFromUnboundedGrowth() {
            // Model with exponential growth: growth = Population * rate
            // With rate = 10x, growth is extremely fast and can overflow
            ModelDefinition def = new ModelDefinitionBuilder()
                    .name("ExponentialGrowth")
                    .stock("Population", 1000, "People")
                    .flow("growth", "Population * rate", "Day",
                            null, "Population")
                    .variable("rate", "0.5", "1/Day")
                    .defaultSimulation("Day", 100, "Day")
                    .build();

            ExtremeConditionResult result = ExtremeConditionTest.builder()
                    .compiledModelFactory(factoryFor(def))
                    .parameter("rate", 0.5)
                    .timeStep(resolveTimeStep(def))
                    .duration(resolveDuration(def))
                    .boundThreshold(1e12)
                    .build()
                    .execute();

            // With rate = 5.0 (10x), exponential growth should exceed bounds
            assertThat(result.findings()).anySatisfy(f -> {
                assertThat(f.condition()).isEqualTo(ExtremeCondition.TEN_X);
                assertThat(f.parameterName()).isEqualTo("rate");
            });
        }
    }

    @Nested
    @DisplayName("Negative stock detection")
    class NegativeStockDetection {

        @Test
        @DisplayName("should detect negative stock when outflow exceeds stock value")
        void shouldDetectNegativeStock() {
            // Model: Stock "Inventory" with outflow "sales" = sales_rate
            // When sales_rate goes 10x, it drains inventory below zero
            // Must use ALLOW policy since the default clamps to zero
            ModelDefinition def = new ModelDefinitionBuilder()
                    .name("Inventory")
                    .stock("Inventory", null, 100, "Units", "ALLOW")
                    .flow("sales", "sales_rate", "Day",
                            "Inventory", null)
                    .variable("sales_rate", "5", "Units/Day")
                    .defaultSimulation("Day", 30, "Day")
                    .build();

            ExtremeConditionResult result = ExtremeConditionTest.builder()
                    .compiledModelFactory(factoryFor(def))
                    .parameter("sales_rate", 5.0)
                    .timeStep(resolveTimeStep(def))
                    .duration(resolveDuration(def))
                    .build()
                    .execute();

            // 10x sales_rate = 50/day for 30 days should drain inventory
            assertThat(result.findings()).anySatisfy(f -> {
                assertThat(f.condition()).isEqualTo(ExtremeCondition.TEN_X);
                assertThat(f.parameterName()).isEqualTo("sales_rate");
                assertThat(f.affectedVariable()).isEqualTo("Inventory");
                assertThat(f.description()).contains("negative");
            });
        }
    }

    @Nested
    @DisplayName("Edge cases in extreme value generation")
    class EdgeCases {

        @Test
        @DisplayName("should handle zero baseline: 10x becomes 10.0, negative becomes -1.0")
        void shouldHandleZeroBaseline() {
            assertThat(ExtremeCondition.TEN_X.apply(0.0)).isEqualTo(10.0);
            assertThat(ExtremeCondition.NEGATIVE.apply(0.0)).isEqualTo(-1.0);
            assertThat(ExtremeCondition.ZERO.apply(0.0)).isEqualTo(0.0);
        }

        @Test
        @DisplayName("should handle negative baseline: negative flips to positive")
        void shouldHandleNegativeBaseline() {
            assertThat(ExtremeCondition.NEGATIVE.apply(-5.0)).isEqualTo(5.0);
            assertThat(ExtremeCondition.TEN_X.apply(-5.0)).isEqualTo(-50.0);
            assertThat(ExtremeCondition.ZERO.apply(-5.0)).isEqualTo(0.0);
        }

        @Test
        @DisplayName("should handle positive baseline normally")
        void shouldHandlePositiveBaseline() {
            assertThat(ExtremeCondition.ZERO.apply(10.0)).isEqualTo(0.0);
            assertThat(ExtremeCondition.TEN_X.apply(10.0)).isEqualTo(100.0);
            assertThat(ExtremeCondition.NEGATIVE.apply(10.0)).isEqualTo(-10.0);
        }
    }

    @Nested
    @DisplayName("Robust model")
    class RobustModel {

        @Test
        @DisplayName("should return empty findings for a model that survives all extreme conditions")
        void shouldReturnEmptyFindingsForRobustModel() {
            // Simple linear decay: Stock = 100, outflow = MIN(Stock, rate)
            // Clamped outflow so it never goes negative, bounded, and no division
            ModelDefinition def = new ModelDefinitionBuilder()
                    .name("Robust")
                    .stock("Level", 100, "Units")
                    .flow("drain", "MIN(Level, drain_rate)", "Day",
                            "Level", null)
                    .variable("drain_rate", "5", "Units/Day")
                    .defaultSimulation("Day", 10, "Day")
                    .build();

            ExtremeConditionResult result = ExtremeConditionTest.builder()
                    .compiledModelFactory(factoryFor(def))
                    .parameter("drain_rate", 5.0)
                    .timeStep(resolveTimeStep(def))
                    .duration(resolveDuration(def))
                    .build()
                    .execute();

            assertThat(result.findings()).isEmpty();
            assertThat(result.runsCompleted()).isEqualTo(3);
            assertThat(result.totalRuns()).isEqualTo(3);
        }
    }

    @Nested
    @DisplayName("Run counts")
    class RunCounts {

        @Test
        @DisplayName("should report correct run counts for multiple parameters")
        void shouldReportCorrectRunCounts() {
            ModelDefinition def = new ModelDefinitionBuilder()
                    .name("TwoParam")
                    .stock("X", 50, "Units")
                    .flow("change", "rate_a + rate_b", "Day", null, "X")
                    .variable("rate_a", "1", "Units/Day")
                    .variable("rate_b", "2", "Units/Day")
                    .defaultSimulation("Day", 5, "Day")
                    .build();

            ExtremeConditionResult result = ExtremeConditionTest.builder()
                    .compiledModelFactory(factoryFor(def))
                    .parameter("rate_a", 1.0)
                    .parameter("rate_b", 2.0)
                    .timeStep(resolveTimeStep(def))
                    .duration(resolveDuration(def))
                    .build()
                    .execute();

            // 2 parameters × 3 conditions = 6 runs
            assertThat(result.runsCompleted()).isEqualTo(6);
            assertThat(result.totalRuns()).isEqualTo(6);
        }
    }

    @Nested
    @DisplayName("Builder validation")
    class BuilderValidation {

        @Test
        @DisplayName("should reject missing compiled model factory")
        void shouldRejectMissingFactory() {
            assertThatThrownBy(() -> ExtremeConditionTest.builder()
                    .parameter("x", 1.0)
                    .timeStep(systems.courant.sd.measure.Units.DAY)
                    .duration(systems.courant.sd.measure.units.time.Times.days(10))
                    .build())
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("compiledModelFactory");
        }

        @Test
        @DisplayName("should reject empty parameters")
        void shouldRejectEmptyParameters() {
            assertThatThrownBy(() -> ExtremeConditionTest.builder()
                    .compiledModelFactory(params -> null)
                    .timeStep(systems.courant.sd.measure.Units.DAY)
                    .duration(systems.courant.sd.measure.units.time.Times.days(10))
                    .build())
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("parameter");
        }

        @Test
        @DisplayName("should reject missing time step")
        void shouldRejectMissingTimeStep() {
            assertThatThrownBy(() -> ExtremeConditionTest.builder()
                    .compiledModelFactory(params -> null)
                    .parameter("x", 1.0)
                    .duration(systems.courant.sd.measure.units.time.Times.days(10))
                    .build())
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("timeStep");
        }

        @Test
        @DisplayName("should reject missing duration")
        void shouldRejectMissingDuration() {
            assertThatThrownBy(() -> ExtremeConditionTest.builder()
                    .compiledModelFactory(params -> null)
                    .parameter("x", 1.0)
                    .timeStep(systems.courant.sd.measure.Units.DAY)
                    .build())
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("duration");
        }
    }

    // --- Utility methods ---

    private static systems.courant.sd.measure.TimeUnit resolveTimeStep(ModelDefinition def) {
        return new systems.courant.sd.measure.UnitRegistry()
                .resolveTimeUnit(def.defaultSimulation().timeStep());
    }

    private static systems.courant.sd.measure.Quantity resolveDuration(ModelDefinition def) {
        systems.courant.sd.measure.UnitRegistry registry = new systems.courant.sd.measure.UnitRegistry();
        systems.courant.sd.measure.TimeUnit durUnit = registry.resolveTimeUnit(
                def.defaultSimulation().durationUnit());
        return new systems.courant.sd.measure.Quantity(def.defaultSimulation().duration(), durUnit);
    }
}
