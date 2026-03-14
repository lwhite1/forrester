package systems.courant.sd.app.canvas;

import systems.courant.sd.model.def.FlowDef;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import systems.courant.sd.app.canvas.charts.SimulationResultPane;

@DisplayName("SimulationResultPane.computeNetFlows")
class NetFlowComputationTest {

    @Test
    @DisplayName("returns empty map when flows list is null")
    void shouldReturnEmptyForNullFlows() {
        Map<String, double[]> result = SimulationResultPane.computeNetFlows(
                List.of("Step", "Population"), List.of(new double[]{0, 100}),
                null, Set.of("Population"));
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("returns empty map when flows list is empty")
    void shouldReturnEmptyForEmptyFlows() {
        Map<String, double[]> result = SimulationResultPane.computeNetFlows(
                List.of("Step", "Population"), List.of(new double[]{0, 100}),
                List.of(), Set.of("Population"));
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("returns empty map when stockNames is empty")
    void shouldReturnEmptyForNoStocks() {
        FlowDef flow = new FlowDef("births", "10", "Year", null, "Population");
        Map<String, double[]> result = SimulationResultPane.computeNetFlows(
                List.of("Step", "Population", "births"),
                List.of(new double[]{0, 100, 10}),
                List.of(flow), Set.of());
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("computes net flow for single stock with one inflow")
    void shouldComputeSingleInflow() {
        FlowDef births = new FlowDef("births", "10", "Year", null, "Population");
        List<String> columns = List.of("Step", "Population", "births");
        List<double[]> rows = List.of(
                new double[]{0, 100, 10},
                new double[]{1, 110, 12},
                new double[]{2, 122, 14}
        );

        Map<String, double[]> result = SimulationResultPane.computeNetFlows(
                columns, rows, List.of(births), Set.of("Population"));

        assertThat(result).containsKey("Population");
        double[] netFlow = result.get("Population");
        assertThat(netFlow).hasSize(3);
        assertThat(netFlow[0]).isCloseTo(10.0, within(1e-9));
        assertThat(netFlow[1]).isCloseTo(12.0, within(1e-9));
        assertThat(netFlow[2]).isCloseTo(14.0, within(1e-9));
    }

    @Test
    @DisplayName("computes net flow for single stock with one outflow")
    void shouldComputeSingleOutflow() {
        FlowDef deaths = new FlowDef("deaths", "5", "Year", "Population", null);
        List<String> columns = List.of("Step", "Population", "deaths");
        List<double[]> rows = List.of(
                new double[]{0, 100, 5},
                new double[]{1, 95, 6}
        );

        Map<String, double[]> result = SimulationResultPane.computeNetFlows(
                columns, rows, List.of(deaths), Set.of("Population"));

        assertThat(result).containsKey("Population");
        double[] netFlow = result.get("Population");
        assertThat(netFlow[0]).isCloseTo(-5.0, within(1e-9));
        assertThat(netFlow[1]).isCloseTo(-6.0, within(1e-9));
    }

    @Test
    @DisplayName("computes net flow with both inflow and outflow")
    void shouldComputeNetWithBothFlows() {
        FlowDef births = new FlowDef("births", "10", "Year", null, "Population");
        FlowDef deaths = new FlowDef("deaths", "3", "Year", "Population", null);
        List<String> columns = List.of("Step", "Population", "births", "deaths");
        List<double[]> rows = List.of(
                new double[]{0, 100, 10, 3},
                new double[]{1, 107, 12, 4},
                new double[]{2, 115, 11, 5}
        );

        Map<String, double[]> result = SimulationResultPane.computeNetFlows(
                columns, rows, List.of(births, deaths), Set.of("Population"));

        double[] netFlow = result.get("Population");
        assertThat(netFlow[0]).isCloseTo(7.0, within(1e-9));
        assertThat(netFlow[1]).isCloseTo(8.0, within(1e-9));
        assertThat(netFlow[2]).isCloseTo(6.0, within(1e-9));
    }

    @Nested
    @DisplayName("Multi-stock models")
    class MultiStock {

        @Test
        @DisplayName("computes net flows for multiple stocks independently")
        void shouldComputeForMultipleStocks() {
            FlowDef infection = new FlowDef("infection", "5", "Year", "Susceptible", "Infected");
            FlowDef recovery = new FlowDef("recovery", "2", "Year", "Infected", null);
            List<String> columns = List.of(
                    "Step", "Susceptible", "Infected", "infection", "recovery");
            List<double[]> rows = List.of(
                    new double[]{0, 990, 10, 5, 2}
            );

            Map<String, double[]> result = SimulationResultPane.computeNetFlows(
                    columns, rows, List.of(infection, recovery),
                    Set.of("Susceptible", "Infected"));

            // Susceptible: infection drains it (source=Susceptible), net = -5
            assertThat(result.get("Susceptible")[0]).isCloseTo(-5.0, within(1e-9));
            // Infected: infection feeds it (sink=Infected) minus recovery drains it
            assertThat(result.get("Infected")[0]).isCloseTo(3.0, within(1e-9));
        }
    }

    @Test
    @DisplayName("skips stocks with no connected flows")
    void shouldSkipStocksWithNoFlows() {
        FlowDef births = new FlowDef("births", "10", "Year", null, "Population");
        List<String> columns = List.of("Step", "Population", "Orphan", "births");
        List<double[]> rows = List.of(new double[]{0, 100, 50, 10});

        Map<String, double[]> result = SimulationResultPane.computeNetFlows(
                columns, rows, List.of(births), Set.of("Population", "Orphan"));

        assertThat(result).containsKey("Population");
        assertThat(result).doesNotContainKey("Orphan");
    }

    @Test
    @DisplayName("skips flows not present in simulation columns")
    void shouldSkipFlowsNotInColumns() {
        FlowDef births = new FlowDef("births", "10", "Year", null, "Population");
        FlowDef phantom = new FlowDef("phantom", "0", "Year", null, "Population");
        List<String> columns = List.of("Step", "Population", "births");
        List<double[]> rows = List.of(new double[]{0, 100, 10});

        Map<String, double[]> result = SimulationResultPane.computeNetFlows(
                columns, rows, List.of(births, phantom), Set.of("Population"));

        assertThat(result.get("Population")[0]).isCloseTo(10.0, within(1e-9));
    }
}
