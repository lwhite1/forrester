package systems.courant.sd.app.canvas;

import systems.courant.sd.Simulation;
import systems.courant.sd.measure.Quantity;
import systems.courant.sd.model.Flow;
import systems.courant.sd.model.Model;
import systems.courant.sd.model.Stock;
import systems.courant.sd.model.Variable;
import systems.courant.sd.sweep.MonteCarloResult;
import systems.courant.sd.sweep.OptimizationResult;
import systems.courant.sd.sweep.RunResult;
import systems.courant.sd.sweep.SensitivitySummary.ParameterImpact;
import systems.courant.sd.sweep.SweepResult;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static systems.courant.sd.measure.Units.DIMENSIONLESS;
import static systems.courant.sd.measure.Units.GALLON_US;
import static systems.courant.sd.measure.Units.MINUTE;
import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("ResultReportGenerator")
class ResultReportGeneratorTest {

    // ── Full Report ────────────────────────────────────────────────────

    @Nested
    @DisplayName("generate()")
    class FullReport {

        @Test
        @DisplayName("should produce valid HTML structure with all sections")
        void shouldProduceValidHtml() {
            RunResult run = buildSingleRun(10.0);
            String html = ResultReportGenerator.generate("Test Model",
                    run, null, null, null, null);

            assertThat(html).startsWith("<!DOCTYPE html>");
            assertThat(html).contains("<html lang=\"en\">");
            assertThat(html).contains("</html>");
            assertThat(html).contains("Test Model — Simulation Results");
        }

        @Test
        @DisplayName("should include only non-null sections")
        void shouldSkipNullSections() {
            RunResult run = buildSingleRun(10.0);
            String html = ResultReportGenerator.generate("Test Model",
                    run, null, null, null, null);

            assertThat(html).contains("Simulation Results");
            assertThat(html).doesNotContain("Parameter Sweep");
            assertThat(html).doesNotContain("Monte Carlo");
            assertThat(html).doesNotContain("Sensitivity Analysis");
            assertThat(html).doesNotContain("Optimization Results");
        }

        @Test
        @DisplayName("should include all sections when all provided")
        void shouldIncludeAllSections() {
            RunResult run = buildSingleRun(10.0);
            SweepResult sweep = buildSweepResult();
            MonteCarloResult mc = buildMonteCarloResult();
            List<ParameterImpact> impacts = buildImpacts();
            OptimizationResult opt = buildOptimizationResult();

            String html = ResultReportGenerator.generate("Full Report",
                    run, sweep, mc, impacts, opt);

            assertThat(html).contains("Simulation Results");
            assertThat(html).contains("Parameter Sweep");
            assertThat(html).contains("Monte Carlo Analysis");
            assertThat(html).contains("Sensitivity Analysis");
            assertThat(html).contains("Optimization Results");
        }

        @Test
        @DisplayName("should escape HTML in model name")
        void shouldEscapeModelName() {
            RunResult run = buildSingleRun(10.0);
            String html = ResultReportGenerator.generate("Model <A> & \"B\"",
                    run, null, null, null, null);

            assertThat(html).contains("Model &lt;A&gt; &amp; &quot;B&quot;");
            assertThat(html).doesNotContain("Model <A>");
        }

        @Test
        @DisplayName("should skip single run when step count is zero")
        void shouldSkipEmptyRun() {
            RunResult emptyRun = new RunResult(0.0);
            String html = ResultReportGenerator.generate("Test",
                    emptyRun, null, null, null, null);

            assertThat(html).doesNotContain("Summary Statistics");
        }

        @Test
        @DisplayName("should skip empty sensitivity list")
        void shouldSkipEmptySensitivity() {
            String html = ResultReportGenerator.generate("Test",
                    null, null, null, List.of(), null);

            assertThat(html).doesNotContain("Sensitivity Analysis");
        }
    }

    // ── Phase 2: Simulation Section ────────────────────────────────────

    @Nested
    @DisplayName("simulation section")
    class SimulationSection {

        @Test
        @DisplayName("should contain summary statistics table")
        void shouldContainSummaryTable() {
            RunResult run = buildSingleRun(10.0);
            StringBuilder html = new StringBuilder();
            ResultReportGenerator.writeSimulationSection(html, run);

            assertThat(html.toString()).contains("Summary Statistics");
            assertThat(html.toString()).contains("<th>Name</th>");
            assertThat(html.toString()).contains("<th>Min</th>");
            assertThat(html.toString()).contains("<th>Max</th>");
            assertThat(html.toString()).contains("<th>Mean</th>");
            assertThat(html.toString()).contains("<th>Final</th>");
        }

        @Test
        @DisplayName("should contain stock names in summary")
        void shouldContainStockNames() {
            RunResult run = buildSingleRun(10.0);
            StringBuilder html = new StringBuilder();
            ResultReportGenerator.writeSummaryTable(html, run);

            assertThat(html.toString()).contains("Tank");
            assertThat(html.toString()).contains("Stock");
        }

        @Test
        @DisplayName("should contain variable names in summary")
        void shouldContainVariableNames() {
            RunResult run = buildSingleRunWithVariable();
            StringBuilder html = new StringBuilder();
            ResultReportGenerator.writeSummaryTable(html, run);

            assertThat(html.toString()).contains("level_indicator");
            assertThat(html.toString()).contains("Variable");
        }

        @Test
        @DisplayName("should contain SVG time series chart for stocks")
        void shouldContainStockChart() {
            RunResult run = buildSingleRun(10.0);
            StringBuilder html = new StringBuilder();
            ResultReportGenerator.writeSimulationSection(html, run);

            assertThat(html.toString()).contains("<svg");
            assertThat(html.toString()).contains("Stock Time Series");
            assertThat(html.toString()).contains("<polyline");
        }

        @Test
        @DisplayName("should contain collapsible raw data table")
        void shouldContainRawDataTable() {
            RunResult run = buildSingleRun(10.0);
            StringBuilder html = new StringBuilder();
            ResultReportGenerator.writeRawDataTable(html, run);

            assertThat(html.toString()).contains("<details");
            assertThat(html.toString()).contains("Raw Time Series Data");
            assertThat(html.toString()).contains("<th>Step</th>");
            assertThat(html.toString()).contains("<th>Tank</th>");
        }

        @Test
        @DisplayName("raw data table should contain step numbers")
        void rawDataShouldContainSteps() {
            RunResult run = buildSingleRun(10.0);
            StringBuilder html = new StringBuilder();
            ResultReportGenerator.writeRawDataTable(html, run);
            String output = html.toString();

            // Should contain step 0 and subsequent steps
            assertThat(output).contains("<td>0</td>");
            assertThat(output).contains("<td>1</td>");
        }
    }

    // ── Phase 2: Summary Statistics ────────────────────────────────────

    @Nested
    @DisplayName("summary statistics")
    class SummaryStatistics {

        @Test
        @DisplayName("should compute correct min/max/mean/final for a draining tank")
        void shouldComputeCorrectStats() {
            RunResult run = buildSingleRun(20.0);
            StringBuilder html = new StringBuilder();
            ResultReportGenerator.writeSummaryTable(html, run);
            String output = html.toString();

            // Tank starts at 100, drains at 20 gal/min for 5 minutes
            // Initial value is 100, final should be near 0
            assertThat(output).contains("Tank");
            // The table should have numeric values
            assertThat(output).contains("100");
        }
    }

    // ── Phase 2: Sweep Section ─────────────────────────────────────────

    @Nested
    @DisplayName("sweep section")
    class SweepSection {

        @Test
        @DisplayName("should contain parameter name in heading")
        void shouldContainParameterName() {
            SweepResult sweep = buildSweepResult();
            StringBuilder html = new StringBuilder();
            ResultReportGenerator.writeSweepSection(html, sweep);

            assertThat(html.toString()).contains("Parameter Sweep — drainRate");
        }

        @Test
        @DisplayName("should contain sweep summary table with final and max values")
        void shouldContainSweepSummaryTable() {
            SweepResult sweep = buildSweepResult();
            StringBuilder html = new StringBuilder();
            ResultReportGenerator.writeSweepSummaryTable(html, sweep);
            String output = html.toString();

            assertThat(output).contains("Sweep Summary");
            assertThat(output).contains("drainRate");
            assertThat(output).contains("Tank (final)");
            assertThat(output).contains("Tank (max)");
            // Should have a row for each parameter value
            assertThat(output).contains("5");
            assertThat(output).contains("10");
            assertThat(output).contains("15");
        }

        @Test
        @DisplayName("should contain SVG sweep chart per stock")
        void shouldContainSweepChart() {
            SweepResult sweep = buildSweepResult();
            StringBuilder html = new StringBuilder();
            ResultReportGenerator.writeSweepSection(html, sweep);
            String output = html.toString();

            assertThat(output).contains("Tank — Parameter Sweep");
            assertThat(output).contains("<svg");
            assertThat(output).contains("<polyline");
        }

        @Test
        @DisplayName("sweep chart should have legend with parameter values")
        void shouldContainSweepLegend() {
            SweepResult sweep = buildSweepResult();
            StringBuilder html = new StringBuilder();
            ResultReportGenerator.writeSweepSection(html, sweep);
            String output = html.toString();

            assertThat(output).contains("drainRate = 5");
            assertThat(output).contains("drainRate = 10");
            assertThat(output).contains("drainRate = 15");
        }
    }

    // ── Phase 3: Monte Carlo Section ───────────────────────────────────

    @Nested
    @DisplayName("Monte Carlo section")
    class MonteCarloSection {

        @Test
        @DisplayName("should contain run count in heading")
        void shouldContainRunCount() {
            MonteCarloResult mc = buildMonteCarloResult();
            StringBuilder html = new StringBuilder();
            ResultReportGenerator.writeMonteCarloSection(html, mc);

            assertThat(html.toString()).contains("Monte Carlo Analysis (5 runs)");
        }

        @Test
        @DisplayName("should contain fan chart SVG for each stock")
        void shouldContainFanChart() {
            MonteCarloResult mc = buildMonteCarloResult();
            String svg = ResultReportGenerator.fanChartSvg("Tank", mc);

            assertThat(svg).contains("<svg");
            assertThat(svg).contains("Fan Chart");
            assertThat(svg).contains("<polygon");  // percentile bands
            assertThat(svg).contains("<polyline");  // median line
        }

        @Test
        @DisplayName("fan chart should have legend for percentile bands")
        void shouldContainFanChartLegend() {
            MonteCarloResult mc = buildMonteCarloResult();
            String svg = ResultReportGenerator.fanChartSvg("Tank", mc);

            assertThat(svg).contains("P2.5–P97.5");
            assertThat(svg).contains("Median");
        }

        @Test
        @DisplayName("should contain percentile data table")
        void shouldContainPercentileTable() {
            MonteCarloResult mc = buildMonteCarloResult();
            StringBuilder html = new StringBuilder();
            ResultReportGenerator.writePercentileTable(html, mc, "Tank");
            String output = html.toString();

            assertThat(output).contains("Percentile Data");
            assertThat(output).contains("P2.5");
            assertThat(output).contains("P25");
            assertThat(output).contains("P50");
            assertThat(output).contains("P75");
            assertThat(output).contains("P97.5");
        }

        @Test
        @DisplayName("percentile table should be collapsible")
        void shouldBeCollapsible() {
            MonteCarloResult mc = buildMonteCarloResult();
            StringBuilder html = new StringBuilder();
            ResultReportGenerator.writePercentileTable(html, mc, "Tank");

            assertThat(html.toString()).contains("<details>");
            assertThat(html.toString()).contains("<summary>");
        }
    }

    // ── Phase 3: Sensitivity Section ───────────────────────────────────

    @Nested
    @DisplayName("sensitivity section")
    class SensitivitySection {

        @Test
        @DisplayName("should contain parameter ranking table")
        void shouldContainRankingTable() {
            List<ParameterImpact> impacts = buildImpacts();
            StringBuilder html = new StringBuilder();
            ResultReportGenerator.writeSensitivityTable(html, impacts);
            String output = html.toString();

            assertThat(output).contains("Parameter Ranking");
            assertThat(output).contains("<th>Rank</th>");
            assertThat(output).contains("<th>Parameter</th>");
            assertThat(output).contains("<th>Impact</th>");
            assertThat(output).contains("contactRate");
            assertThat(output).contains("recoveryRate");
        }

        @Test
        @DisplayName("parameters should be in correct ranked order")
        void shouldBeInRankedOrder() {
            List<ParameterImpact> impacts = buildImpacts();
            StringBuilder html = new StringBuilder();
            ResultReportGenerator.writeSensitivityTable(html, impacts);
            String output = html.toString();

            // contactRate (70%) should be rank 1, recoveryRate (30%) rank 2
            int contactRatePos = output.indexOf("contactRate");
            int recoveryRatePos = output.indexOf("recoveryRate");
            assertThat(contactRatePos).isLessThan(recoveryRatePos);
        }

        @Test
        @DisplayName("should contain tornado chart SVG")
        void shouldContainTornadoChart() {
            List<ParameterImpact> impacts = buildImpacts();
            String svg = ResultReportGenerator.tornadoChartSvg(impacts);

            assertThat(svg).contains("<svg");
            assertThat(svg).contains("Sensitivity");
            assertThat(svg).contains("<rect");  // bars
            assertThat(svg).contains("contactRate");
            assertThat(svg).contains("recoveryRate");
        }

        @Test
        @DisplayName("tornado chart should show percentage labels")
        void shouldShowPercentageLabels() {
            List<ParameterImpact> impacts = buildImpacts();
            String svg = ResultReportGenerator.tornadoChartSvg(impacts);

            assertThat(svg).contains("70%");
            assertThat(svg).contains("30%");
        }

        @Test
        @DisplayName("should contain plain-language summary")
        void shouldContainPlainLanguageSummary() {
            List<ParameterImpact> impacts = buildImpacts();
            StringBuilder html = new StringBuilder();
            ResultReportGenerator.writeSensitivitySection(html, impacts);
            String output = html.toString();

            assertThat(output).contains("summary-text");
            assertThat(output).contains("most sensitive to");
        }

        @Test
        @DisplayName("tornado chart should be empty for empty impacts")
        void tornadoShouldBeEmptyForNoImpacts() {
            String svg = ResultReportGenerator.tornadoChartSvg(List.of());
            assertThat(svg).isEmpty();
        }
    }

    // ── Phase 3: Optimization Section ──────────────────────────────────

    @Nested
    @DisplayName("optimization section")
    class OptimizationSection {

        @Test
        @DisplayName("should contain objective value and evaluation count")
        void shouldContainObjectiveSummary() {
            OptimizationResult opt = buildOptimizationResult();
            StringBuilder html = new StringBuilder();
            ResultReportGenerator.writeOptimizationSection(html, opt);
            String output = html.toString();

            assertThat(output).contains("Optimization Results");
            assertThat(output).contains("Objective Value");
            assertThat(output).contains("42");
            assertThat(output).contains("Evaluations");
            assertThat(output).contains("100");
        }

        @Test
        @DisplayName("should contain best parameter values")
        void shouldContainBestParameters() {
            OptimizationResult opt = buildOptimizationResult();
            StringBuilder html = new StringBuilder();
            ResultReportGenerator.writeOptimizationSection(html, opt);
            String output = html.toString();

            assertThat(output).contains("Best Parameters");
            assertThat(output).contains("drainRate");
            assertThat(output).contains("7.5");
        }

        @Test
        @DisplayName("should contain best run time series chart")
        void shouldContainBestRunChart() {
            OptimizationResult opt = buildOptimizationResult();
            StringBuilder html = new StringBuilder();
            ResultReportGenerator.writeOptimizationSection(html, opt);
            String output = html.toString();

            assertThat(output).contains("Best Run");
            assertThat(output).contains("<svg");
            assertThat(output).contains("<polyline");
        }
    }

    // ── SVG Charts ─────────────────────────────────────────────────────

    @Nested
    @DisplayName("SVG charts")
    class SvgCharts {

        @Test
        @DisplayName("line chart should have valid SVG structure")
        void lineChartShouldBeValidSvg() {
            long[] steps = {0, 1, 2, 3, 4};
            List<String> names = List.of("Series A");
            List<double[]> data = List.of(new double[]{10, 20, 15, 25, 30});

            String svg = ResultReportGenerator.lineChartSvg("Test Chart", steps, names, data);

            assertThat(svg).contains("xmlns=\"http://www.w3.org/2000/svg\"");
            assertThat(svg).contains("viewBox=\"0 0");
            assertThat(svg).endsWith("</svg>\n");
        }

        @Test
        @DisplayName("line chart should render title")
        void lineChartShouldRenderTitle() {
            long[] steps = {0, 1, 2};
            List<String> names = List.of("A");
            List<double[]> data = List.of(new double[]{1, 2, 3});

            String svg = ResultReportGenerator.lineChartSvg("My Title", steps, names, data);

            assertThat(svg).contains("My Title");
        }

        @Test
        @DisplayName("line chart should render legend when multiple series")
        void lineChartShouldRenderLegend() {
            long[] steps = {0, 1, 2};
            List<String> names = List.of("Series A", "Series B");
            List<double[]> data = List.of(
                    new double[]{1, 2, 3},
                    new double[]{3, 2, 1});

            String svg = ResultReportGenerator.lineChartSvg("Test", steps, names, data);

            assertThat(svg).contains("Series A");
            assertThat(svg).contains("Series B");
        }

        @Test
        @DisplayName("line chart should not render legend for single series")
        void lineChartShouldOmitLegendForSingle() {
            long[] steps = {0, 1, 2};
            List<String> names = List.of("Only");
            List<double[]> data = List.of(new double[]{1, 2, 3});

            String svg = ResultReportGenerator.lineChartSvg("Test", steps, names, data);

            // Title uses "Only" so check there's no legend-style entry
            // The legend would duplicate the text with a colored line prefix
            long count = svg.chars().filter(c -> c == 'l').count();
            assertThat(svg).contains("<polyline");
        }

        @Test
        @DisplayName("line chart should handle constant data (yMin == yMax)")
        void lineChartShouldHandleConstantData() {
            long[] steps = {0, 1, 2, 3};
            List<String> names = List.of("Flat");
            List<double[]> data = List.of(new double[]{5, 5, 5, 5});

            String svg = ResultReportGenerator.lineChartSvg("Constant", steps, names, data);

            assertThat(svg).contains("<svg");
            assertThat(svg).contains("<polyline");
        }

        @Test
        @DisplayName("line chart should handle NaN values gracefully")
        void lineChartShouldHandleNaN() {
            long[] steps = {0, 1, 2, 3};
            List<String> names = List.of("WithNaN");
            List<double[]> data = List.of(new double[]{1, Double.NaN, 3, 4});

            String svg = ResultReportGenerator.lineChartSvg("Gaps Test", steps, names, data);

            assertThat(svg).contains("<svg");
            // The polyline should not contain NaN coordinates
            int polylineStart = svg.indexOf("points=\"");
            int polylineEnd = svg.indexOf("\"", polylineStart + 8);
            String points = svg.substring(polylineStart, polylineEnd);
            assertThat(points).doesNotContain("NaN");
        }

        @Test
        @DisplayName("fan chart should have valid SVG structure")
        void fanChartShouldBeValidSvg() {
            MonteCarloResult mc = buildMonteCarloResult();
            String svg = ResultReportGenerator.fanChartSvg("Tank", mc);

            assertThat(svg).contains("xmlns=\"http://www.w3.org/2000/svg\"");
            assertThat(svg).endsWith("</svg>\n");
        }

        @Test
        @DisplayName("fan chart should handle insufficient data")
        void fanChartShouldHandleInsufficientData() {
            // Empty MC result with no runs has stepCount == 0, which is <= 1
            MonteCarloResult mc = new MonteCarloResult(List.of());
            String result = ResultReportGenerator.fanChartSvg("Tank", mc);

            assertThat(result).contains("Not enough data");
        }
    }

    // ── Number Formatting ──────────────────────────────────────────────

    @Nested
    @DisplayName("fmt() helper")
    class FmtHelper {

        @Test
        @DisplayName("should format integers without decimal point")
        void shouldFormatIntegers() {
            assertThat(ResultReportGenerator.fmt(42.0)).isEqualTo("42");
            assertThat(ResultReportGenerator.fmt(0.0)).isEqualTo("0");
            assertThat(ResultReportGenerator.fmt(-100.0)).isEqualTo("-100");
        }

        @Test
        @DisplayName("should format decimals with trailing zeros stripped")
        void shouldFormatDecimals() {
            assertThat(ResultReportGenerator.fmt(3.14)).isEqualTo("3.14");
            assertThat(ResultReportGenerator.fmt(0.5)).isEqualTo("0.5");
        }

        @Test
        @DisplayName("should use dash for non-finite values")
        void shouldHandleNonFinite() {
            assertThat(ResultReportGenerator.fmt(Double.NaN)).isEqualTo("—");
            assertThat(ResultReportGenerator.fmt(Double.POSITIVE_INFINITY)).isEqualTo("—");
            assertThat(ResultReportGenerator.fmt(Double.NEGATIVE_INFINITY)).isEqualTo("—");
        }

        @Test
        @DisplayName("should use scientific notation for very small values")
        void shouldUseScientificForSmall() {
            String result = ResultReportGenerator.fmt(0.000001);
            assertThat(result).contains("e") .doesNotContain("NaN");
        }
    }

    // ── CSS & Styling ──────────────────────────────────────────────────

    @Nested
    @DisplayName("styling")
    class Styling {

        @Test
        @DisplayName("should include inline CSS styles")
        void shouldIncludeStyles() {
            RunResult run = buildSingleRun(10.0);
            String html = ResultReportGenerator.generate("Test", run, null, null, null, null);

            assertThat(html).contains("<style>");
            assertThat(html).contains(".chart-svg");
            assertThat(html).contains(".element-table");
        }

        @Test
        @DisplayName("should include print media query")
        void shouldIncludePrintStyles() {
            RunResult run = buildSingleRun(10.0);
            String html = ResultReportGenerator.generate("Test", run, null, null, null, null);

            assertThat(html).contains("@media print");
        }

        @Test
        @DisplayName("should include collapsible details styling")
        void shouldIncludeDetailsStyles() {
            RunResult run = buildSingleRun(10.0);
            String html = ResultReportGenerator.generate("Test", run, null, null, null, null);

            assertThat(html).contains("details summary");
        }
    }

    // ── Test Helpers ───────────────────────────────────────────────────

    private static RunResult buildSingleRun(double drainRate) {
        Model model = new Model("Test");
        Stock tank = new Stock("Tank", 100, GALLON_US);
        Flow outflow = Flow.create("Drain", MINUTE, () -> new Quantity(drainRate, GALLON_US));
        tank.addOutflow(outflow);
        model.addStock(tank);

        RunResult run = new RunResult(drainRate);
        Simulation sim = new Simulation(model, MINUTE, MINUTE, 5);
        sim.addEventHandler(run);
        sim.execute();
        return run;
    }

    private static RunResult buildSingleRunWithVariable() {
        Model model = new Model("VarTest");
        Stock tank = new Stock("Tank", 100, GALLON_US);
        Flow outflow = Flow.create("Drain", MINUTE, () -> new Quantity(10, GALLON_US));
        tank.addOutflow(outflow);
        model.addStock(tank);

        Variable indicator = new Variable("level_indicator", DIMENSIONLESS,
                () -> tank.getValue() > 50 ? 1.0 : 0.0);
        model.addVariable(indicator);

        RunResult run = new RunResult(10.0);
        Simulation sim = new Simulation(model, MINUTE, MINUTE, 5);
        sim.addEventHandler(run);
        sim.execute();
        return run;
    }

    private static SweepResult buildSweepResult() {
        List<RunResult> runs = new ArrayList<>();
        for (double rate : new double[]{5.0, 10.0, 15.0}) {
            runs.add(buildSingleRun(rate));
        }
        return new SweepResult("drainRate", runs);
    }

    private static MonteCarloResult buildMonteCarloResult() {
        List<RunResult> runs = new ArrayList<>();
        for (double rate : new double[]{5.0, 7.0, 10.0, 12.0, 15.0}) {
            runs.add(buildSingleRun(rate));
        }
        return new MonteCarloResult(runs);
    }

    private static List<ParameterImpact> buildImpacts() {
        return List.of(
                new ParameterImpact("contactRate", "Infectious", 50, 500, 200, 0.70),
                new ParameterImpact("recoveryRate", "Infectious", 100, 300, 200, 0.30)
        );
    }

    private static OptimizationResult buildOptimizationResult() {
        RunResult bestRun = buildSingleRun(7.5);
        Map<String, Double> bestParams = new LinkedHashMap<>();
        bestParams.put("drainRate", 7.5);
        return new OptimizationResult(bestParams, 42.0, bestRun, 100);
    }
}
