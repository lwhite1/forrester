package systems.courant.sd.app.canvas.charts;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import systems.courant.sd.app.canvas.SimulationRunner;

@DisplayName("PhasePlotPane")
class PhasePlotPaneTest {

    private SimulationRunner.SimulationResult twoStockResult() {
        return new SimulationRunner.SimulationResult(
                List.of("Step", "Prey", "Predator"),
                List.of(
                        new double[]{0, 100, 10},
                        new double[]{1, 120, 12},
                        new double[]{2, 110, 18},
                        new double[]{3, 90, 20},
                        new double[]{4, 80, 15}
                ),
                Map.of("Prey", "animals", "Predator", "animals"),
                Set.of("Prey", "Predator")
        );
    }

    private SimulationRunner.SimulationResult singleStockResult() {
        return new SimulationRunner.SimulationResult(
                List.of("Step", "Population"),
                List.of(
                        new double[]{0, 100},
                        new double[]{1, 110}
                )
        );
    }

    private SimulationRunner.SimulationResult threeVarResult() {
        return new SimulationRunner.SimulationResult(
                List.of("Step", "A", "B", "C"),
                List.of(
                        new double[]{0, 1, 2, 3},
                        new double[]{1, 4, 5, 6},
                        new double[]{2, 7, 8, 9}
                ),
                Map.of(),
                Set.of("A")
        );
    }

    @Nested
    @DisplayName("getVariableNames")
    class GetVariableNames {

        @Test
        @DisplayName("should return all columns except Step")
        void shouldReturnAllColumnsExceptStep() {
            List<String> names = PhasePlotPane.getVariableNames(twoStockResult());
            assertThat(names).containsExactly("Prey", "Predator");
        }

        @Test
        @DisplayName("should return single variable for single-stock result")
        void shouldReturnSingleVariable() {
            List<String> names = PhasePlotPane.getVariableNames(singleStockResult());
            assertThat(names).containsExactly("Population");
        }

        @Test
        @DisplayName("should return all three variables for three-var result")
        void shouldReturnThreeVariables() {
            List<String> names = PhasePlotPane.getVariableNames(threeVarResult());
            assertThat(names).containsExactly("A", "B", "C");
        }
    }

    @Nested
    @DisplayName("extractColumn")
    class ExtractColumn {

        @Test
        @DisplayName("should extract correct values for first variable")
        void shouldExtractFirstVariable() {
            double[] values = PhasePlotPane.extractColumn(twoStockResult(), "Prey");
            assertThat(values).containsExactly(100, 120, 110, 90, 80);
        }

        @Test
        @DisplayName("should extract correct values for second variable")
        void shouldExtractSecondVariable() {
            double[] values = PhasePlotPane.extractColumn(twoStockResult(), "Predator");
            assertThat(values).containsExactly(10, 12, 18, 20, 15);
        }

        @Test
        @DisplayName("should return null for unknown variable")
        void shouldReturnNullForUnknownVariable() {
            double[] values = PhasePlotPane.extractColumn(twoStockResult(), "Unknown");
            assertThat(values).isNull();
        }

        @Test
        @DisplayName("should return null for Step column name")
        void shouldReturnStepColumn() {
            // Step is at index 0, should still extract it if requested
            double[] values = PhasePlotPane.extractColumn(twoStockResult(), "Step");
            assertThat(values).containsExactly(0, 1, 2, 3, 4);
        }

        @Test
        @DisplayName("should handle empty rows")
        void shouldHandleEmptyRows() {
            SimulationRunner.SimulationResult emptyResult =
                    new SimulationRunner.SimulationResult(
                            List.of("Step", "X"),
                            List.of()
                    );
            double[] values = PhasePlotPane.extractColumn(emptyResult, "X");
            assertThat(values).isEmpty();
        }
    }
}
