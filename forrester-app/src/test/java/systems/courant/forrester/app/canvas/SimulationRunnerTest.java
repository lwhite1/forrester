package systems.courant.forrester.app.canvas;

import systems.courant.forrester.model.def.ModelDefinition;
import systems.courant.forrester.model.def.ModelDefinitionBuilder;
import systems.courant.forrester.model.def.SimulationSettings;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;

@DisplayName("SimulationRunner")
class SimulationRunnerTest {

    private SimulationRunner runner;

    @BeforeEach
    void setUp() {
        runner = new SimulationRunner();
    }

    @Nested
    @DisplayName("column names")
    class ColumnNames {

        @Test
        void shouldIncludeStepAndStockNames() {
            ModelDefinition def = new ModelDefinitionBuilder()
                    .name("Test")
                    .stock("Population", 100, "people")
                    .build();
            SimulationSettings settings = new SimulationSettings("Day", 5, "Day");

            SimulationRunner.SimulationResult result = runner.run(def, settings);

            assertThat(result.columnNames()).contains("Step", "Population");
        }

        @Test
        void shouldIncludeVariableNames() {
            ModelDefinition def = new ModelDefinitionBuilder()
                    .name("Test")
                    .stock("S", 100, "units")
                    .constant("Rate", 0.1, "units")
                    .aux("Computed", "S * Rate", "units")
                    .build();
            SimulationSettings settings = new SimulationSettings("Day", 3, "Day");

            SimulationRunner.SimulationResult result = runner.run(def, settings);

            assertThat(result.columnNames()).contains("Computed");
        }
    }

    @Nested
    @DisplayName("row data")
    class RowData {

        @Test
        void shouldCaptureCorrectRowCount() {
            ModelDefinition def = new ModelDefinitionBuilder()
                    .name("Test")
                    .stock("S", 50, "units")
                    .build();
            SimulationSettings settings = new SimulationSettings("Day", 10, "Day");

            SimulationRunner.SimulationResult result = runner.run(def, settings);

            // N+1 rows for an N-step simulation (step 0 through step N)
            assertThat(result.rows()).hasSize(11);
        }

        @Test
        void shouldCaptureInitialStockValueInFirstRow() {
            ModelDefinition def = new ModelDefinitionBuilder()
                    .name("Test")
                    .stock("S", 200, "units")
                    .build();
            SimulationSettings settings = new SimulationSettings("Day", 3, "Day");

            SimulationRunner.SimulationResult result = runner.run(def, settings);

            double[] firstRow = result.rows().get(0);
            // firstRow[0] = step (0), firstRow[1] = stock value (200)
            assertThat(firstRow[0]).isEqualTo(0);
            assertThat(firstRow[1]).isCloseTo(200.0, within(0.001));
        }

        @Test
        void shouldCaptureVariableValues() {
            ModelDefinition def = new ModelDefinitionBuilder()
                    .name("Test")
                    .stock("S", 100, "units")
                    .constant("K", 2, "units")
                    .aux("Double", "S * K", "units")
                    .build();
            SimulationSettings settings = new SimulationSettings("Day", 1, "Day");

            SimulationRunner.SimulationResult result = runner.run(def, settings);

            // Find the column index for "Double"
            int doubleIdx = result.columnNames().indexOf("Double");
            assertThat(doubleIdx).isGreaterThan(0);

            // At step 0, S=100, K=2, so Double should be 200
            double[] firstRow = result.rows().get(0);
            assertThat(firstRow[doubleIdx]).isCloseTo(200.0, within(0.001));
        }

        @Test
        void shouldHandleFlowDrainOverTime() {
            ModelDefinition def = new ModelDefinitionBuilder()
                    .name("Drain")
                    .stock("Tank", 100, "units")
                    .flow("Outflow", "10", "day", "Tank", null)
                    .build();
            SimulationSettings settings = new SimulationSettings("Day", 5, "Day");

            SimulationRunner.SimulationResult result = runner.run(def, settings);

            // After 5 days draining 10/day, stock should be 50
            double[] lastRow = result.rows().get(result.rows().size() - 1);
            int tankIdx = result.columnNames().indexOf("Tank");
            assertThat(lastRow[tankIdx]).isCloseTo(50.0, within(0.001));
        }
    }

    @Nested
    @DisplayName("error handling")
    class ErrorHandling {

        @Test
        void shouldThrowOnInvalidTimeStepUnit() {
            ModelDefinition def = new ModelDefinitionBuilder()
                    .name("Test")
                    .stock("S", 0, "units")
                    .build();
            SimulationSettings settings = new SimulationSettings("InvalidUnit", 10, "Day");

            assertThatThrownBy(() -> runner.run(def, settings))
                    .isInstanceOf(RuntimeException.class);
        }

        @Test
        void shouldThrowOnBadEquationReference() {
            ModelDefinition def = new ModelDefinitionBuilder()
                    .name("Test")
                    .stock("S", 100, "units")
                    .flow("F", "NonExistent * 0.1", "day", "S", null)
                    .build();
            SimulationSettings settings = new SimulationSettings("Day", 5, "Day");

            assertThatThrownBy(() -> runner.run(def, settings))
                    .isInstanceOf(RuntimeException.class);
        }
    }

    @Nested
    @DisplayName("empty model")
    class EmptyModel {

        @Test
        void shouldProduceResultsForModelWithNoElements() {
            ModelDefinition def = new ModelDefinitionBuilder()
                    .name("Empty")
                    .build();
            SimulationSettings settings = new SimulationSettings("Day", 3, "Day");

            SimulationRunner.SimulationResult result = runner.run(def, settings);

            // Should have Step column at minimum
            assertThat(result.columnNames()).contains("Step");
            assertThat(result.rows()).isNotEmpty();
        }
    }
}
