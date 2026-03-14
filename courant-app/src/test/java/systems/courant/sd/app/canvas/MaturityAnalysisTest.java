package systems.courant.sd.app.canvas;

import systems.courant.sd.model.def.ModelDefinition;
import systems.courant.sd.model.def.ModelDefinitionBuilder;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("MaturityAnalysis")
class MaturityAnalysisTest {

    @Nested
    @DisplayName("missing equation detection")
    class MissingEquation {

        @Test
        @DisplayName("should flag flow with placeholder equation '0'")
        void shouldFlagFlowWithPlaceholderEquation() {
            ModelDefinition def = new ModelDefinitionBuilder()
                    .name("Test")
                    .stock("Stock1", 100, "Person")
                    .flow("Flow1", "0", "Day", null, "Stock1")
                    .defaultSimulation("Day", 10, "Day")
                    .build();

            MaturityAnalysis result = MaturityAnalysis.analyze(def);

            assertThat(result.missingEquation()).contains("Flow1");
        }

        @Test
        @DisplayName("should flag variable with placeholder equation '0'")
        void shouldFlagVariableWithPlaceholderEquation() {
            ModelDefinition def = new ModelDefinitionBuilder()
                    .name("Test")
                    .stock("Stock1", 100, "Person")
                    .variable("Var1", "0", "Dimensionless unit")
                    .flow("Flow1", "Stock1 * Var1", "Day", "Stock1", null)
                    .defaultSimulation("Day", 10, "Day")
                    .build();

            MaturityAnalysis result = MaturityAnalysis.analyze(def);

            assertThat(result.missingEquation()).contains("Var1");
        }

        @Test
        @DisplayName("should not flag flow with real equation")
        void shouldNotFlagFlowWithRealEquation() {
            ModelDefinition def = new ModelDefinitionBuilder()
                    .name("Test")
                    .stock("Stock1", 100, "Person")
                    .flow("Flow1", "Stock1 * 0.5", "Day", "Stock1", null)
                    .defaultSimulation("Day", 10, "Day")
                    .build();

            MaturityAnalysis result = MaturityAnalysis.analyze(def);

            assertThat(result.missingEquation()).doesNotContain("Flow1");
        }

        @Test
        @DisplayName("should not flag literal-valued constant")
        void shouldNotFlagLiteralConstant() {
            ModelDefinition def = new ModelDefinitionBuilder()
                    .name("Test")
                    .stock("Stock1", 100, "Person")
                    .constant("Rate", 0.05, "Dimensionless unit")
                    .flow("Flow1", "Stock1 * Rate", "Day", "Stock1", null)
                    .defaultSimulation("Day", 10, "Day")
                    .build();

            MaturityAnalysis result = MaturityAnalysis.analyze(def);

            assertThat(result.missingEquation()).doesNotContain("Rate");
        }
    }

    @Nested
    @DisplayName("missing unit detection")
    class MissingUnit {

        @Test
        @DisplayName("should flag stock with default placeholder unit 'units'")
        void shouldFlagStockWithPlaceholderUnit() {
            ModelDefinition def = new ModelDefinitionBuilder()
                    .name("Test")
                    .stock("Stock1", 100, "units")
                    .flow("Flow1", "Stock1 * 0.1", "Day", "Stock1", null)
                    .defaultSimulation("Day", 10, "Day")
                    .build();

            MaturityAnalysis result = MaturityAnalysis.analyze(def);

            assertThat(result.missingUnit()).contains("Stock1");
        }

        @Test
        @DisplayName("should flag variable with default placeholder unit 'units'")
        void shouldFlagVariableWithPlaceholderUnit() {
            ModelDefinition def = new ModelDefinitionBuilder()
                    .name("Test")
                    .stock("Stock1", 100, "Person")
                    .variable("Var1", "Stock1 * 0.1", "units")
                    .flow("Flow1", "Var1", "Day", "Stock1", null)
                    .defaultSimulation("Day", 10, "Day")
                    .build();

            MaturityAnalysis result = MaturityAnalysis.analyze(def);

            assertThat(result.missingUnit()).contains("Var1");
        }

        @Test
        @DisplayName("should not flag stock with meaningful unit")
        void shouldNotFlagStockWithMeaningfulUnit() {
            ModelDefinition def = new ModelDefinitionBuilder()
                    .name("Test")
                    .stock("Population", 100, "Person")
                    .flow("Births", "Population * 0.04", "Day", null, "Population")
                    .defaultSimulation("Day", 10, "Day")
                    .build();

            MaturityAnalysis result = MaturityAnalysis.analyze(def);

            assertThat(result.missingUnit()).doesNotContain("Population");
        }
    }

    @Nested
    @DisplayName("unit mismatch detection")
    class UnitMismatch {

        @Test
        @DisplayName("should flag flow when material unit differs from connected stock")
        void shouldFlagMismatchedFlow() {
            ModelDefinition def = new ModelDefinitionBuilder()
                    .name("Test")
                    .stock("WaterTank", 100, "Gallon")
                    .flow("Drain", "WaterTank * 0.1", "Day", "WaterTank", null)
                    .defaultSimulation("Day", 10, "Day")
                    .build();

            // Manually build with explicit mismatched materialUnit
            var builder = def.toBuilder();
            builder.clearFlows();
            builder.flow(new systems.courant.sd.model.def.FlowDef(
                    "Drain", null, "WaterTank * 0.1", "Day", "Liter",
                    "WaterTank", null, java.util.List.of()));
            ModelDefinition mismatchDef = builder.build();

            MaturityAnalysis result = MaturityAnalysis.analyze(mismatchDef);

            assertThat(result.unitMismatchFlows()).contains("Drain");
        }

        @Test
        @DisplayName("should not flag flow when material unit matches stock")
        void shouldNotFlagMatchedFlow() {
            var builder = new ModelDefinitionBuilder()
                    .name("Test")
                    .stock("WaterTank", 100, "Gallon");

            // Build with matching materialUnit
            ModelDefinition def = builder
                    .flow(new systems.courant.sd.model.def.FlowDef(
                            "Drain", null, "WaterTank * 0.1", "Day", "Gallon",
                            "WaterTank", null, java.util.List.of()))
                    .defaultSimulation("Day", 10, "Day")
                    .build();

            MaturityAnalysis result = MaturityAnalysis.analyze(def);

            assertThat(result.unitMismatchFlows()).doesNotContain("Drain");
        }
    }

    @Nested
    @DisplayName("fully specified model")
    class FullySpecified {

        @Test
        @DisplayName("should report fully specified for canonical exponential growth model")
        void shouldBeFullySpecifiedForCanonicalModel() {
            ModelDefinition def = systems.courant.sd.app.models.CanonicalModels.exponentialGrowth();

            MaturityAnalysis result = MaturityAnalysis.analyze(def);

            assertThat(result.isFullySpecified()).isTrue();
        }

        @Test
        @DisplayName("should report incomplete for model with placeholder elements")
        void shouldBeIncompleteForPlaceholderModel() {
            ModelDefinition def = new ModelDefinitionBuilder()
                    .name("Test")
                    .stock("Stock1", 0, "units")
                    .flow("Flow1", "0", "Day", null, "Stock1")
                    .variable("Var1", "0", "units")
                    .defaultSimulation("Day", 10, "Day")
                    .build();

            MaturityAnalysis result = MaturityAnalysis.analyze(def);

            assertThat(result.isFullySpecified()).isFalse();
            assertThat(result.missingEquation()).containsExactlyInAnyOrder("Flow1", "Var1");
            assertThat(result.missingUnit()).containsExactlyInAnyOrder("Stock1", "Var1");
        }
    }

    @Nested
    @DisplayName("isIncomplete")
    class IsIncomplete {

        @Test
        @DisplayName("should return true for elements with any issue")
        void shouldReturnTrueForIncompleteElement() {
            ModelDefinition def = new ModelDefinitionBuilder()
                    .name("Test")
                    .stock("Stock1", 100, "Person")
                    .flow("Flow1", "0", "Day", null, "Stock1")
                    .defaultSimulation("Day", 10, "Day")
                    .build();

            MaturityAnalysis result = MaturityAnalysis.analyze(def);

            assertThat(result.isIncomplete("Flow1")).isTrue();
            assertThat(result.isIncomplete("Stock1")).isFalse();
        }
    }

    @Nested
    @DisplayName("edge cases")
    class EdgeCases {

        @Test
        @DisplayName("isMissingEquation handles null and blank")
        void shouldHandleNullAndBlankEquation() {
            assertThat(MaturityAnalysis.isMissingEquation(null)).isTrue();
            assertThat(MaturityAnalysis.isMissingEquation("")).isTrue();
            assertThat(MaturityAnalysis.isMissingEquation("  ")).isTrue();
            assertThat(MaturityAnalysis.isMissingEquation(" 0 ")).isTrue();
            assertThat(MaturityAnalysis.isMissingEquation("0")).isTrue();
            assertThat(MaturityAnalysis.isMissingEquation("0.5")).isFalse();
            assertThat(MaturityAnalysis.isMissingEquation("Stock * 0.1")).isFalse();
        }

        @Test
        @DisplayName("isMissingUnit handles null, blank, and placeholder")
        void shouldHandleNullAndBlankUnit() {
            assertThat(MaturityAnalysis.isMissingUnit(null)).isTrue();
            assertThat(MaturityAnalysis.isMissingUnit("")).isTrue();
            assertThat(MaturityAnalysis.isMissingUnit("  ")).isTrue();
            assertThat(MaturityAnalysis.isMissingUnit("units")).isTrue();
            assertThat(MaturityAnalysis.isMissingUnit("Units")).isTrue();
            assertThat(MaturityAnalysis.isMissingUnit("Person")).isFalse();
            assertThat(MaturityAnalysis.isMissingUnit("Gallon")).isFalse();
        }
    }
}
