package systems.courant.sd.app.canvas;

import systems.courant.sd.Simulation;
import systems.courant.sd.measure.Quantity;
import systems.courant.sd.model.Flow;
import systems.courant.sd.model.Model;
import systems.courant.sd.model.Stock;
import systems.courant.sd.sweep.MonteCarloResult;
import systems.courant.sd.sweep.MultiSweepResult;
import systems.courant.sd.sweep.OptimizationResult;
import systems.courant.sd.sweep.RunResult;
import systems.courant.sd.sweep.SweepResult;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static systems.courant.sd.measure.Units.MINUTE;
import static systems.courant.sd.measure.Units.THING;

@DisplayName("ClipboardExporter formatting")
class ClipboardExporterTest {

    @Nested
    @DisplayName("formatSimulationResult")
    class FormatSimulationResult {

        @Test
        @DisplayName("should produce tab-separated header and data rows")
        void shouldFormatSimulationResult() {
            SimulationRunner.SimulationResult result = new SimulationRunner.SimulationResult(
                    List.of("Step", "Population", "GrowthRate"),
                    List.of(
                            new double[]{0, 100.0, 5.0},
                            new double[]{1, 105.0, 5.25},
                            new double[]{2, 110.25, 5.5125}
                    )
            );

            String text = ClipboardExporter.formatSimulationResult(result);

            String[] lines = text.split("\n");
            assertThat(lines).hasSize(4); // header + 3 data rows
            assertThat(lines[0]).isEqualTo("Step\tPopulation\tGrowthRate");
            assertThat(lines[1]).startsWith("0\t100.0\t5.0");
            assertThat(lines[2]).startsWith("1\t105.0\t5.25");
        }

        @Test
        @DisplayName("should format step as integer")
        void shouldFormatStepAsInteger() {
            SimulationRunner.SimulationResult result = new SimulationRunner.SimulationResult(
                    List.of("Step", "Value"),
                    List.of(new double[]{42, 3.14})
            );

            String text = ClipboardExporter.formatSimulationResult(result);

            assertThat(text).contains("42\t3.14");
            // Step should NOT be "42.0"
            assertThat(text).doesNotContain("42.0");
        }

        @Test
        @DisplayName("should preserve fractional time steps (#641)")
        void shouldPreserveFractionalTimeSteps() {
            SimulationRunner.SimulationResult result = new SimulationRunner.SimulationResult(
                    List.of("Step", "Value"),
                    List.of(
                            new double[]{0.0, 10.0},
                            new double[]{0.5, 15.0},
                            new double[]{1.0, 20.0},
                            new double[]{1.5, 25.0}
                    )
            );

            String text = ClipboardExporter.formatSimulationResult(result);

            String[] lines = text.split("\n");
            assertThat(lines[1]).isEqualTo("0\t10.0");
            assertThat(lines[2]).isEqualTo("0.5\t15.0");
            assertThat(lines[3]).isEqualTo("1\t20.0");
            assertThat(lines[4]).isEqualTo("1.5\t25.0");
        }

        @Test
        @DisplayName("should handle empty rows")
        void shouldHandleEmptyRows() {
            SimulationRunner.SimulationResult result = new SimulationRunner.SimulationResult(
                    List.of("Step", "Value"),
                    List.of()
            );

            String text = ClipboardExporter.formatSimulationResult(result);

            assertThat(text.split("\n")).hasSize(1); // header only
            assertThat(text).startsWith("Step\tValue");
        }
    }

    @Nested
    @DisplayName("formatSweepTimeSeries")
    class FormatSweepTimeSeries {

        @Test
        @DisplayName("should include parameter column and all runs")
        void shouldFormatSweepTimeSeries() {
            SweepResult result = buildSweepResult("rate", new double[]{0.1, 0.2}, 3);

            String text = ClipboardExporter.formatSweepTimeSeries(result);

            String[] lines = text.split("\n");
            assertThat(lines[0]).startsWith("rate\tStep");
            // 2 runs × 4 steps (0..3) = 8 data rows + 1 header
            assertThat(lines).hasSize(9);
            // First run starts with parameter value 0.1
            assertThat(lines[1]).startsWith("0.1\t");
        }
    }

    @Nested
    @DisplayName("formatSweepSummary")
    class FormatSweepSummary {

        @Test
        @DisplayName("should include final and max columns per stock")
        void shouldFormatSweepSummary() {
            SweepResult result = buildSweepResult("rate", new double[]{0.05, 0.1}, 5);

            String text = ClipboardExporter.formatSweepSummary(result);

            String[] lines = text.split("\n");
            // Header should have parameter, then stock_final + stock_max
            assertThat(lines[0]).contains("rate");
            assertThat(lines[0]).contains("_final");
            assertThat(lines[0]).contains("_max");
            // 2 runs = 2 data rows
            assertThat(lines).hasSize(3);
        }
    }

    @Nested
    @DisplayName("formatMonteCarloPercentiles")
    class FormatMonteCarloPercentiles {

        @Test
        @DisplayName("should include percentile columns")
        void shouldFormatPercentiles() {
            MonteCarloResult result = buildMonteCarloResult(20, 5);

            String text = ClipboardExporter.formatMonteCarloPercentiles(result, "Tank");

            assertThat(text).isNotNull();
            String[] lines = text.split("\n");
            // Header: Step, p2.5, p25, p50, p75, p97.5
            assertThat(lines[0]).contains("Step");
            assertThat(lines[0]).contains("p2.5");
            assertThat(lines[0]).contains("p50");
            assertThat(lines[0]).contains("p97.5");
            // 6 steps (0..5) = 6 data rows + header
            assertThat(lines).hasSize(7);
        }

        @Test
        @DisplayName("should return null when variable name is null")
        void shouldReturnNullForNullVariable() {
            MonteCarloResult result = buildMonteCarloResult(5, 3);
            assertThat(ClipboardExporter.formatMonteCarloPercentiles(result, null)).isNull();
        }
    }

    @Nested
    @DisplayName("formatMultiSweepSummary")
    class FormatMultiSweepSummary {

        @Test
        @DisplayName("should include all parameter columns")
        void shouldFormatMultiSweepSummary() {
            MultiSweepResult result = buildMultiSweepResult();

            String text = ClipboardExporter.formatMultiSweepSummary(result);

            String[] lines = text.split("\n");
            assertThat(lines[0]).contains("alpha");
            assertThat(lines[0]).contains("beta");
            assertThat(lines[0]).contains("_final");
        }
    }

    @Nested
    @DisplayName("formatOptimizationBestRun")
    class FormatOptimizationBestRun {

        @Test
        @DisplayName("should format best run time series")
        void shouldFormatOptimizationBestRun() {
            RunResult bestRun = buildRunResult(0.1, 5);
            OptimizationResult result = new OptimizationResult(
                    Map.of("rate", 0.1), 42.0, bestRun, 100);

            String text = ClipboardExporter.formatOptimizationBestRun(result);

            String[] lines = text.split("\n");
            assertThat(lines[0]).startsWith("Step");
            assertThat(lines[0]).contains("Tank");
            // 6 steps (0..5) + header
            assertThat(lines).hasSize(7);
        }
    }

    // ── Helpers ────────────────────────────────────────────────────────

    private static RunResult buildRunResult(double paramValue, int steps) {
        Model model = new Model("TestModel");
        Stock tank = new Stock("Tank", 100, THING);
        Flow inflow = Flow.create("Inflow", MINUTE,
                () -> new Quantity(tank.getValue() * paramValue, THING));
        tank.addInflow(inflow);
        model.addStock(tank);
        model.addFlow(inflow);

        RunResult rr = new RunResult(paramValue);
        Simulation sim = new Simulation(model, MINUTE, MINUTE, steps);
        sim.addEventHandler(rr);
        sim.execute();
        return rr;
    }

    private static SweepResult buildSweepResult(String paramName, double[] values, int steps) {
        List<RunResult> runs = new ArrayList<>();
        for (double v : values) {
            runs.add(buildRunResult(v, steps));
        }
        return new SweepResult(paramName, runs);
    }

    private static MonteCarloResult buildMonteCarloResult(int runCount, int steps) {
        List<RunResult> runs = new ArrayList<>();
        for (int r = 0; r < runCount; r++) {
            double rate = 0.05 + r * 0.005;
            runs.add(buildRunResult(rate, steps));
        }
        return new MonteCarloResult(runs);
    }

    private static MultiSweepResult buildMultiSweepResult() {
        List<RunResult> runs = new ArrayList<>();
        for (double alpha : new double[]{0.05, 0.1}) {
            for (double beta : new double[]{0.01, 0.02}) {
                Model model = new Model("TestModel");
                Stock tank = new Stock("Tank", 100, THING);
                Flow inflow = Flow.create("Inflow", MINUTE,
                        () -> new Quantity(tank.getValue() * alpha, THING));
                Flow outflow = Flow.create("Outflow", MINUTE,
                        () -> new Quantity(tank.getValue() * beta, THING));
                tank.addInflow(inflow);
                tank.addOutflow(outflow);
                model.addStock(tank);
                model.addFlow(inflow);
                model.addFlow(outflow);

                RunResult rr = new RunResult(Map.of("alpha", alpha, "beta", beta));
                Simulation sim = new Simulation(model, MINUTE, MINUTE, 3);
                sim.addEventHandler(rr);
                sim.execute();
                runs.add(rr);
            }
        }
        return new MultiSweepResult(List.of("alpha", "beta"), runs);
    }
}
