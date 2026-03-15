package systems.courant.sd.demo;

import systems.courant.sd.Simulation;
import systems.courant.sd.measure.units.time.Times;
import systems.courant.sd.model.Model;
import systems.courant.sd.model.Stock;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static systems.courant.sd.measure.Units.DAY;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.data.Offset.offset;

@DisplayName("SirModelBuilder")
class SirModelBuilderTest {

    @Nested
    @DisplayName("computeNewInfections")
    class ComputeNewInfections {

        @Test
        @DisplayName("shouldReturnZero_whenTotalPopulationIsZero")
        void shouldReturnZero_whenTotalPopulationIsZero() {
            double result = SirModelBuilder.computeNewInfections(8, 0.1, 0, 0, 0);
            assertThat(result).isZero();
        }

        @Test
        @DisplayName("shouldReturnZero_whenNoInfectiousPresent")
        void shouldReturnZero_whenNoInfectiousPresent() {
            double result = SirModelBuilder.computeNewInfections(8, 0.1, 1000, 0, 0);
            assertThat(result).isZero();
        }

        @Test
        @DisplayName("shouldReturnZero_whenNoSusceptiblePresent")
        void shouldReturnZero_whenNoSusceptiblePresent() {
            double result = SirModelBuilder.computeNewInfections(8, 0.1, 0, 100, 900);
            assertThat(result).isZero();
        }

        @Test
        @DisplayName("shouldComputeCorrectInfections_forTypicalValues")
        void shouldComputeCorrectInfections_forTypicalValues() {
            // contactRate=8, infectivity=0.1, S=1000, I=10, R=0
            // totalPop=1010, infectiousFraction=10/1010, infections=8*(10/1010)*0.1*1000
            double expected = 8.0 * (10.0 / 1010.0) * 0.1 * 1000.0;
            double result = SirModelBuilder.computeNewInfections(8, 0.1, 1000, 10, 0);
            assertThat(result).isCloseTo(expected, offset(1e-10));
        }

        @Test
        @DisplayName("shouldClampToSusceptible_whenInfectionsExceedSusceptible")
        void shouldClampToSusceptible_whenInfectionsExceedSusceptible() {
            // Very high contact rate to force infections > susceptible
            double result = SirModelBuilder.computeNewInfections(1000, 1.0, 5, 500, 500);
            assertThat(result).isCloseTo(5.0, offset(1e-10));
        }
    }

    @Nested
    @DisplayName("build")
    class Build {

        @Test
        @DisplayName("shouldCreateModelWithThreeStocks")
        void shouldCreateModelWithThreeStocks() {
            Model model = SirModelBuilder.build("Test SIR", 8, 0.1, 1000, 10, 0, 0.2);
            assertThat(model.getStocks()).hasSize(3);
            assertThat(model.getStockNames())
                    .containsExactlyInAnyOrder("Susceptible", "Infectious", "Recovered");
        }

        @Test
        @DisplayName("shouldSetModelName")
        void shouldSetModelName() {
            Model model = SirModelBuilder.build("Custom Name", 8, 0.1, 1000, 10, 0, 0.2);
            assertThat(model.getName()).isEqualTo("Custom Name");
        }

        @Test
        @DisplayName("shouldSetInitialStockValues")
        void shouldSetInitialStockValues() {
            Model model = SirModelBuilder.build("Test", 8, 0.1, 500, 20, 30, 0.2);
            assertThat(model.getStockValues()).containsExactly(500.0, 20.0, 30.0);
        }

        @Test
        @DisplayName("shouldSimulateEpidemicCurve")
        void shouldSimulateEpidemicCurve() {
            Model model = SirModelBuilder.build("Epidemic Test", 8, 0.1, 1000, 10, 0, 0.2);
            new Simulation(model, DAY, Times.weeks(8)).execute();

            Stock susceptible = model.getStocks().stream()
                    .filter(s -> s.getName().equals("Susceptible")).findFirst().orElseThrow();
            Stock recovered = model.getStocks().stream()
                    .filter(s -> s.getName().equals("Recovered")).findFirst().orElseThrow();

            // After epidemic, susceptible should decrease and recovered should increase
            assertThat(susceptible.getValue()).isLessThan(1000);
            assertThat(recovered.getValue()).isGreaterThan(0);
        }

        @Test
        @DisplayName("shouldConserveTotalPopulation")
        void shouldConserveTotalPopulation() {
            double initialTotal = 1000 + 10 + 0;
            Model model = SirModelBuilder.build("Conservation Test", 8, 0.1, 1000, 10, 0, 0.2);
            new Simulation(model, DAY, Times.weeks(8)).execute();

            double finalTotal = model.getStockValues().stream()
                    .mapToDouble(Double::doubleValue).sum();
            assertThat(finalTotal).isCloseTo(initialTotal,
                    offset(1.0));
        }
    }
}
