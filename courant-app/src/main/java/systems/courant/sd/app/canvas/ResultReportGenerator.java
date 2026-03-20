package systems.courant.sd.app.canvas;

import systems.courant.sd.model.graph.BehaviorClassification;
import systems.courant.sd.model.graph.BehaviorClassification.Mode;
import systems.courant.sd.model.graph.FeedbackAnalysis;
import systems.courant.sd.model.graph.LoopDominanceAnalysis;
import systems.courant.sd.sweep.MonteCarloResult;
import systems.courant.sd.sweep.OptimizationResult;
import systems.courant.sd.sweep.RunResult;
import systems.courant.sd.sweep.SensitivitySummary;
import systems.courant.sd.sweep.SensitivitySummary.ParameterImpact;
import systems.courant.sd.sweep.SweepResult;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Generates self-contained HTML report sections for simulation results,
 * parameter sweeps, Monte Carlo analyses, sensitivity rankings, and
 * optimization outcomes. Charts are rendered as inline SVG for vector quality.
 *
 * <p>Covers report generation phases 2, 3, and 4:
 * <ul>
 *   <li>Phase 2: time series charts, summary statistics, raw data tables</li>
 *   <li>Phase 3: sweep charts, fan charts, tornado charts, optimization summary</li>
 *   <li>Phase 4: loop dominance charts, behavior mode classification, custom templates</li>
 * </ul>
 */
public final class ResultReportGenerator {

    private static final int CHART_WIDTH = 800;
    private static final int CHART_HEIGHT = 400;
    private static final int MARGIN_LEFT = 70;
    private static final int MARGIN_RIGHT = 30;
    private static final int MARGIN_TOP = 40;
    private static final int MARGIN_BOTTOM = 50;

    private static final String[] SERIES_COLORS = {
            "#2c5282", "#d6604d", "#4393c3", "#f4a582", "#92c5de",
            "#b2182b", "#2166ac", "#d1e5f0", "#fddbc7", "#67001f"
    };

    private static final int TORNADO_BAR_HEIGHT = 28;
    private static final int TORNADO_LEFT_MARGIN = 200;
    private static final int TORNADO_RIGHT_MARGIN = 60;
    private static final int TORNADO_TOP = 40;
    private static final int TORNADO_BAR_GAP = 6;

    private static final String[] REINFORCING_COLORS = {"#27ae60", "#2ecc71", "#1abc9c", "#16a085"};
    private static final String[] BALANCING_COLORS = {"#2980b9", "#3498db", "#2c3e50", "#34495e"};
    private static final String[] NEUTRAL_COLORS = {"#7f8c8d", "#95a5a6", "#bdc3c7"};

    private static final int AREA_CHART_HEIGHT = 350;

    private ResultReportGenerator() {
    }

    // ── Public API ──────────────────────────────────────────────────────

    /**
     * Generates a full HTML result report. Null parameters are skipped.
     *
     * @param modelName     the model name for the report title
     * @param singleRun     single simulation run result (null to omit)
     * @param sweep         parameter sweep result (null to omit)
     * @param monteCarlo    Monte Carlo result (null to omit)
     * @param sensitivity   sensitivity impacts list (null or empty to omit)
     * @param optimization  optimization result (null to omit)
     * @return self-contained HTML string
     */
    public static String generate(String modelName,
                                  RunResult singleRun,
                                  SweepResult sweep,
                                  MonteCarloResult monteCarlo,
                                  List<ParameterImpact> sensitivity,
                                  OptimizationResult optimization) {
        return generate(modelName, singleRun, sweep, monteCarlo, sensitivity,
                optimization, null, null, null);
    }

    /**
     * Generates a full HTML result report with phase 4 features.
     *
     * @param modelName     the model name for the report title
     * @param singleRun     single simulation run result (null to omit)
     * @param sweep         parameter sweep result (null to omit)
     * @param monteCarlo    Monte Carlo result (null to omit)
     * @param sensitivity   sensitivity impacts list (null or empty to omit)
     * @param optimization  optimization result (null to omit)
     * @param dominance     loop dominance analysis (null to omit)
     * @param behaviors     behavior classifications (null or empty to omit)
     * @param template      report template for custom styling (null for defaults)
     * @return self-contained HTML string
     */
    public static String generate(String modelName,
                                  RunResult singleRun,
                                  SweepResult sweep,
                                  MonteCarloResult monteCarlo,
                                  List<ParameterImpact> sensitivity,
                                  OptimizationResult optimization,
                                  LoopDominanceAnalysis dominance,
                                  List<BehaviorClassification.Result> behaviors,
                                  ReportTemplate template) {
        StringBuilder html = new StringBuilder(16384);
        writeHeader(html, modelName, template);
        html.append("<body>\n<div class=\"container\">\n");

        if (template != null && template.headerHtml() != null && !template.headerHtml().isBlank()) {
            html.append("<div class=\"report-header\">").append(template.headerHtml()).append("</div>\n");
        }

        html.append("<h1>").append(esc(modelName)).append(" — Simulation Results</h1>\n");

        if (singleRun != null && singleRun.getStepCount() > 0) {
            writeSimulationSection(html, singleRun);
        }
        if (behaviors != null && !behaviors.isEmpty()) {
            writeBehaviorSection(html, behaviors);
        }
        if (sweep != null && sweep.getRunCount() > 0) {
            writeSweepSection(html, sweep);
        }
        if (monteCarlo != null && monteCarlo.getRunCount() > 0) {
            writeMonteCarloSection(html, monteCarlo);
        }
        if (sensitivity != null && !sensitivity.isEmpty()) {
            writeSensitivitySection(html, sensitivity);
        }
        if (dominance != null && dominance.loopCount() > 0) {
            writeLoopDominanceSection(html, dominance);
        }
        if (optimization != null) {
            writeOptimizationSection(html, optimization);
        }

        if (template != null && template.footerHtml() != null && !template.footerHtml().isBlank()) {
            html.append("<div class=\"report-footer\">").append(template.footerHtml()).append("</div>\n");
        }

        html.append("</div>\n</body>\n</html>\n");
        return html.toString();
    }

    /**
     * Generates only the result section HTML fragments (no HTML/head/body wrapper).
     * Used by {@link ReportExporter} to embed result sections into a combined report.
     *
     * @param singleRun     single simulation run result (null to omit)
     * @param sweep         parameter sweep result (null to omit)
     * @param monteCarlo    Monte Carlo result (null to omit)
     * @param sensitivity   sensitivity impacts list (null or empty to omit)
     * @param optimization  optimization result (null to omit)
     * @param dominance     loop dominance analysis (null to omit)
     * @return HTML section fragments
     */
    static String generateSections(RunResult singleRun,
                                   SweepResult sweep,
                                   MonteCarloResult monteCarlo,
                                   List<ParameterImpact> sensitivity,
                                   OptimizationResult optimization,
                                   LoopDominanceAnalysis dominance) {
        StringBuilder html = new StringBuilder(8192);
        if (singleRun != null && singleRun.getStepCount() > 0) {
            writeSimulationSection(html, singleRun);
        }
        if (sweep != null && sweep.getRunCount() > 0) {
            writeSweepSection(html, sweep);
        }
        if (monteCarlo != null && monteCarlo.getRunCount() > 0) {
            writeMonteCarloSection(html, monteCarlo);
        }
        if (sensitivity != null && !sensitivity.isEmpty()) {
            writeSensitivitySection(html, sensitivity);
        }
        if (dominance != null && dominance.loopCount() > 0) {
            writeLoopDominanceSection(html, dominance);
        }
        if (optimization != null) {
            writeOptimizationSection(html, optimization);
        }
        return html.toString();
    }

    /**
     * Returns the additional CSS rules needed for result report sections
     * (charts, collapsible details, behavior badges, etc.) that are not
     * included in the base model report CSS.
     */
    static String getResultsCSS() {
        return RESULTS_EXTRA_CSS;
    }

    // ── Phase 2: Single Run ────────────────────────────────────────────

    static void writeSimulationSection(StringBuilder html, RunResult run) {
        html.append("<section>\n<h2>Simulation Results</h2>\n");
        writeSummaryTable(html, run);

        List<String> stockNames = run.getStockNames();
        if (!stockNames.isEmpty()) {
            long[] steps = extractSteps(run);
            List<double[]> stockSeries = new ArrayList<>();
            for (String name : stockNames) {
                stockSeries.add(run.getStockSeries(name));
            }
            html.append(lineChartSvg("Stock Time Series", steps, stockNames, stockSeries));
        }

        List<String> varNames = run.getVariableNames();
        if (!varNames.isEmpty()) {
            long[] steps = extractSteps(run);
            List<double[]> varSeries = new ArrayList<>();
            for (String name : varNames) {
                varSeries.add(variableSeries(run, name));
            }
            html.append(lineChartSvg("Variable Time Series", steps, varNames, varSeries));
        }

        writeRawDataTable(html, run);
        html.append("</section>\n\n");
    }

    static void writeSummaryTable(StringBuilder html, RunResult run) {
        html.append("<h3>Summary Statistics</h3>\n");
        html.append("<table class=\"element-table\">\n");
        html.append("<thead><tr><th>Name</th><th>Type</th><th>Min</th><th>Max</th>");
        html.append("<th>Mean</th><th>Final</th></tr></thead>\n");
        html.append("<tbody>\n");

        for (String name : run.getStockNames()) {
            double[] series = run.getStockSeries(name);
            writeStatsRow(html, name, "Stock", series);
        }
        for (String name : run.getVariableNames()) {
            double[] series = variableSeries(run, name);
            writeStatsRow(html, name, "Variable", series);
        }

        html.append("</tbody></table>\n");
    }

    static void writeRawDataTable(StringBuilder html, RunResult run) {
        html.append("<details class=\"raw-data\">\n");
        html.append("<summary>Raw Time Series Data</summary>\n");
        html.append("<table class=\"element-table\">\n");

        html.append("<thead><tr><th>Step</th>");
        for (String name : run.getStockNames()) {
            html.append("<th>").append(esc(name)).append("</th>");
        }
        for (String name : run.getVariableNames()) {
            html.append("<th>").append(esc(name)).append("</th>");
        }
        html.append("</tr></thead>\n<tbody>\n");

        for (int i = 0; i < run.getStepCount(); i++) {
            html.append("<tr><td>").append(run.getStep(i)).append("</td>");
            double[] stocks = run.getStockValuesAtStep(i);
            for (double v : stocks) {
                html.append("<td class=\"code\">").append(fmt(v)).append("</td>");
            }
            double[] vars = run.getVariableValuesAtStep(i);
            for (double v : vars) {
                html.append("<td class=\"code\">").append(fmt(v)).append("</td>");
            }
            html.append("</tr>\n");
        }

        html.append("</tbody></table>\n</details>\n");
    }

    // ── Phase 2: Sweep ─────────────────────────────────────────────────

    static void writeSweepSection(StringBuilder html, SweepResult sweep) {
        html.append("<section>\n<h2>Parameter Sweep — ")
                .append(esc(sweep.getParameterName())).append("</h2>\n");

        writeSweepSummaryTable(html, sweep);

        for (String stockName : sweep.getStockNames()) {
            writeSweepChart(html, sweep, stockName);
        }

        html.append("</section>\n\n");
    }

    static void writeSweepSummaryTable(StringBuilder html, SweepResult sweep) {
        html.append("<h3>Sweep Summary</h3>\n");
        html.append("<table class=\"element-table\">\n");
        html.append("<thead><tr><th>").append(esc(sweep.getParameterName())).append("</th>");
        for (String name : sweep.getStockNames()) {
            html.append("<th>").append(esc(name)).append(" (final)</th>");
            html.append("<th>").append(esc(name)).append(" (max)</th>");
        }
        html.append("</tr></thead>\n<tbody>\n");

        for (int r = 0; r < sweep.getRunCount(); r++) {
            RunResult run = sweep.getResult(r);
            html.append("<tr><td class=\"code\">").append(fmt(run.getParameterValue())).append("</td>");
            for (String name : sweep.getStockNames()) {
                html.append("<td class=\"code\">").append(fmt(run.getFinalStockValue(name))).append("</td>");
                html.append("<td class=\"code\">").append(fmt(run.getMaxStockValue(name))).append("</td>");
            }
            html.append("</tr>\n");
        }

        html.append("</tbody></table>\n");
    }

    private static void writeSweepChart(StringBuilder html, SweepResult sweep, String stockName) {
        List<String> seriesNames = new ArrayList<>();
        List<double[]> seriesData = new ArrayList<>();

        for (int r = 0; r < sweep.getRunCount(); r++) {
            RunResult run = sweep.getResult(r);
            seriesNames.add(sweep.getParameterName() + " = " + fmt(run.getParameterValue()));
            seriesData.add(run.getStockSeries(stockName));
        }

        long[] steps = extractSteps(sweep.getResult(0));
        html.append(lineChartSvg(stockName + " — Parameter Sweep", steps, seriesNames, seriesData));
    }

    // ── Phase 3: Monte Carlo ──────────────────────────────────────────

    static void writeMonteCarloSection(StringBuilder html, MonteCarloResult mc) {
        html.append("<section>\n<h2>Monte Carlo Analysis (")
                .append(mc.getRunCount()).append(" runs)</h2>\n");

        List<String> allNames = new ArrayList<>(mc.getStockNames());
        allNames.addAll(mc.getVariableNames());

        for (String name : allNames) {
            html.append(fanChartSvg(name, mc));
            writePercentileTable(html, mc, name);
        }

        html.append("</section>\n\n");
    }

    static void writePercentileTable(StringBuilder html, MonteCarloResult mc, String variableName) {
        double[] percentiles = {2.5, 25, 50, 75, 97.5};
        Map<Double, double[]> data = mc.getPercentileSeries(variableName, percentiles);

        html.append("<details>\n<summary>").append(esc(variableName))
                .append(" — Percentile Data</summary>\n");
        html.append("<table class=\"element-table\">\n");
        html.append("<thead><tr><th>Step</th>");
        for (double p : percentiles) {
            html.append("<th>P").append(fmtPct(p)).append("</th>");
        }
        html.append("</tr></thead>\n<tbody>\n");

        for (int step = 0; step < mc.getStepCount(); step++) {
            html.append("<tr><td>").append(step).append("</td>");
            for (double p : percentiles) {
                html.append("<td class=\"code\">").append(fmt(data.get(p)[step])).append("</td>");
            }
            html.append("</tr>\n");
        }

        html.append("</tbody></table>\n</details>\n");
    }

    // ── Phase 3: Sensitivity ──────────────────────────────────────────

    static void writeSensitivitySection(StringBuilder html, List<ParameterImpact> impacts) {
        html.append("<section>\n<h2>Sensitivity Analysis</h2>\n");

        writeSensitivityTable(html, impacts);
        html.append(tornadoChartSvg(impacts));
        html.append("<p class=\"summary-text\">")
                .append(esc(SensitivitySummary.toPlainLanguage(impacts)))
                .append("</p>\n");

        html.append("</section>\n\n");
    }

    static void writeSensitivityTable(StringBuilder html, List<ParameterImpact> impacts) {
        html.append("<h3>Parameter Ranking</h3>\n");
        html.append("<table class=\"element-table\">\n");
        html.append("<thead><tr><th>Rank</th><th>Parameter</th><th>Impact</th>");
        html.append("<th>Min Output</th><th>Max Output</th><th>Baseline</th></tr></thead>\n");
        html.append("<tbody>\n");

        for (int i = 0; i < impacts.size(); i++) {
            ParameterImpact p = impacts.get(i);
            html.append("<tr><td>").append(i + 1).append("</td>");
            html.append("<td class=\"name\">").append(esc(p.parameterName())).append("</td>");
            html.append("<td class=\"code\">").append(fmtPercent(p.impactFraction())).append("</td>");
            html.append("<td class=\"code\">").append(fmt(p.minOutput())).append("</td>");
            html.append("<td class=\"code\">").append(fmt(p.maxOutput())).append("</td>");
            html.append("<td class=\"code\">").append(fmt(p.baselineOutput())).append("</td>");
            html.append("</tr>\n");
        }

        html.append("</tbody></table>\n");
    }

    // ── Phase 4: Behavior Classification ────────────────────────────────

    static void writeBehaviorSection(StringBuilder html,
                                     List<BehaviorClassification.Result> behaviors) {
        html.append("<section>\n<h2>Behavior Mode Classification</h2>\n");
        html.append("<table class=\"element-table\">\n");
        html.append("<thead><tr><th>Variable</th><th>Behavior Mode</th></tr></thead>\n");
        html.append("<tbody>\n");

        for (BehaviorClassification.Result result : behaviors) {
            html.append("<tr>");
            html.append("<td class=\"name\">").append(esc(result.variableName())).append("</td>");
            html.append("<td><span class=\"behavior-badge behavior-")
                    .append(badgeClass(result.mode())).append("\">")
                    .append(esc(result.mode().displayName()))
                    .append("</span></td>");
            html.append("</tr>\n");
        }

        html.append("</tbody></table>\n");
        html.append("</section>\n\n");
    }

    private static String badgeClass(Mode mode) {
        return switch (mode) {
            case EXPONENTIAL_GROWTH, LINEAR_GROWTH -> "growth";
            case EXPONENTIAL_DECAY, LINEAR_DECLINE -> "decay";
            case GOAL_SEEKING -> "goal";
            case OSCILLATION -> "oscillation";
            case S_SHAPED_GROWTH -> "s-shaped";
            case OVERSHOOT_AND_COLLAPSE -> "overshoot";
            case EQUILIBRIUM -> "equilibrium";
        };
    }

    // ── Phase 4: Loop Dominance ──────────────────────────────────────

    static void writeLoopDominanceSection(StringBuilder html,
                                          LoopDominanceAnalysis dominance) {
        html.append("<section>\n<h2>Loop Dominance</h2>\n");
        html.append(stackedAreaChartSvg(dominance));
        writeDominanceTransitionTable(html, dominance);
        html.append("</section>\n\n");
    }

    static void writeDominanceTransitionTable(StringBuilder html,
                                              LoopDominanceAnalysis dominance) {
        if (dominance.stepCount() == 0) {
            return;
        }

        html.append("<h3>Dominance Transitions</h3>\n");
        html.append("<table class=\"element-table\">\n");
        html.append("<thead><tr><th>Step Range</th><th>Dominant Loop</th>");
        html.append("<th>Type</th></tr></thead>\n");
        html.append("<tbody>\n");

        int prevDominant = dominance.dominantLoopAt(0);
        int rangeStart = 0;

        for (int step = 1; step < dominance.stepCount(); step++) {
            int dominant = dominance.dominantLoopAt(step);
            if (dominant != prevDominant) {
                writeTransitionRow(html, dominance, rangeStart, step - 1, prevDominant);
                prevDominant = dominant;
                rangeStart = step;
            }
        }
        // Write final range
        writeTransitionRow(html, dominance, rangeStart, dominance.stepCount() - 1, prevDominant);

        html.append("</tbody></table>\n");
    }

    private static void writeTransitionRow(StringBuilder html,
                                           LoopDominanceAnalysis dominance,
                                           int from, int to, int loopIdx) {
        html.append("<tr>");
        if (from == to) {
            html.append("<td>").append(from).append("</td>");
        } else {
            html.append("<td>").append(from).append("–").append(to).append("</td>");
        }
        if (loopIdx >= 0 && loopIdx < dominance.loopCount()) {
            html.append("<td class=\"name\">")
                    .append(esc(dominance.loopLabels().get(loopIdx))).append("</td>");
            FeedbackAnalysis.LoopType type = dominance.loopTypes().get(loopIdx);
            html.append("<td>").append(type != null ? type.label() : "—").append("</td>");
        } else {
            html.append("<td class=\"name\">None</td><td>—</td>");
        }
        html.append("</tr>\n");
    }

    // ── SVG: Stacked Area Chart ─────────────────────────────────────

    static String stackedAreaChartSvg(LoopDominanceAnalysis dominance) {
        int loopCount = dominance.loopCount();
        int stepCount = dominance.stepCount();
        if (loopCount == 0 || stepCount <= 1) {
            return "<p>Not enough data for loop dominance chart.</p>\n";
        }

        // Normalize activity at each step (stacked to 100%)
        double[][] normalized = new double[loopCount][stepCount];
        for (int step = 0; step < stepCount; step++) {
            double total = 0;
            for (int loop = 0; loop < loopCount; loop++) {
                total += dominance.score(loop, step);
            }
            for (int loop = 0; loop < loopCount; loop++) {
                normalized[loop][step] = total > 0
                        ? dominance.score(loop, step) / total
                        : 0;
            }
        }

        // Sample for large step counts
        int sampleInterval = Math.max(1, stepCount / 500);
        int sampleCount = 0;
        for (int s = 0; s < stepCount; s += sampleInterval) {
            sampleCount++;
        }

        ChartScaffold chart = new ChartScaffold(
                MARGIN_LEFT, CHART_WIDTH - MARGIN_RIGHT,
                MARGIN_TOP, AREA_CHART_HEIGHT - MARGIN_BOTTOM,
                CHART_WIDTH - MARGIN_RIGHT - MARGIN_LEFT,
                AREA_CHART_HEIGHT - MARGIN_BOTTOM - MARGIN_TOP,
                0, 1.0);

        StringBuilder svg = new StringBuilder(8192);
        svgLine(svg,
                "<svg xmlns=\"http://www.w3.org/2000/svg\" viewBox=\"0 0 %d %d\" "
                        + "class=\"chart-svg\">",
                CHART_WIDTH, AREA_CHART_HEIGHT);

        chart.writeBackground(svg, "#fafafa");

        // Draw stacked areas from bottom to top
        for (int loop = loopCount - 1; loop >= 0; loop--) {
            String color = loopColor(dominance, loop);
            StringBuilder points = new StringBuilder();

            // Top edge (cumulative from loop 0..loop)
            int sampleIdx = 0;
            for (int step = 0; step < stepCount; step += sampleInterval) {
                double cumulative = 0;
                for (int l = 0; l <= loop; l++) {
                    cumulative += normalized[l][step];
                }
                double x = chart.mapX((double) sampleIdx / Math.max(1, sampleCount - 1));
                double y = chart.mapY(cumulative);
                if (!points.isEmpty()) {
                    points.append(' ');
                }
                points.append(String.format(Locale.US, "%.1f,%.1f", x, y));
                sampleIdx++;
            }

            // Bottom edge (cumulative from loop 0..loop-1, right to left)
            // Mirror top edge sampling: iterate same sample indices in reverse
            for (sampleIdx = sampleCount - 1; sampleIdx >= 0; sampleIdx--) {
                int step = Math.min(sampleIdx * sampleInterval, stepCount - 1);
                double cumulative = 0;
                for (int l = 0; l < loop; l++) {
                    cumulative += normalized[l][step];
                }
                double x = chart.mapX((double) sampleIdx / Math.max(1, sampleCount - 1));
                double y = chart.mapY(cumulative);
                points.append(' ');
                points.append(String.format(Locale.US, "%.1f,%.1f", x, y));
            }

            svgLine(svg,
                    "<polygon points=\"%s\" fill=\"%s\" fill-opacity=\"0.6\" stroke=\"%s\" "
                            + "stroke-width=\"0.5\"/>",
                    points, color, color);
        }

        chart.writeAxes(svg);

        // Y-axis ticks as percentages
        for (int i = 0; i <= 5; i++) {
            double frac = i / 5.0;
            double y = chart.mapY(frac);
            svgLine(svg,
                    "<text x=\"%d\" y=\"%.1f\" text-anchor=\"end\" "
                            + "font-size=\"11\" fill=\"#4a5568\">%d%%</text>",
                    MARGIN_LEFT - 8, y + 4, (int) (frac * 100));
        }

        // X-axis: step labels
        int xTicks = Math.min(10, sampleCount - 1);
        if (xTicks > 0) {
            for (int i = 0; i <= xTicks; i++) {
                int step = (int) Math.round((double) i * (stepCount - 1) / xTicks);
                double x = chart.mapX((double) i / xTicks);
                svgLine(svg,
                        "<text x=\"%.1f\" y=\"%d\" text-anchor=\"middle\" "
                                + "font-size=\"11\" fill=\"#4a5568\">%d</text>",
                        x, AREA_CHART_HEIGHT - MARGIN_BOTTOM + 18, step);
            }
        }

        chart.writeTitle(svg, "Loop Dominance Over Time");

        // Step label
        svgLine(svg,
                "<text x=\"%d\" y=\"%d\" text-anchor=\"middle\" "
                        + "font-size=\"12\" fill=\"#4a5568\">Step</text>",
                MARGIN_LEFT + chart.plotWidth() / 2, AREA_CHART_HEIGHT - 5);

        // Legend
        int legendX = chart.plotRight() - 160;
        int legendY = chart.plotTop() + 10;
        for (int i = 0; i < loopCount && i < 10; i++) {
            String color = loopColor(dominance, i);
            int ly = legendY + i * 16;
            svgLine(svg,
                    "<rect x=\"%d\" y=\"%d\" width=\"14\" height=\"10\" "
                            + "fill=\"%s\" fill-opacity=\"0.6\"/>",
                    legendX, ly, color);
            String label = dominance.loopLabels().get(i);
            FeedbackAnalysis.LoopType type = dominance.loopTypes().get(i);
            String typeLabel = type != null ? " (" + type.label() + ")" : "";
            svgLine(svg,
                    "<text x=\"%d\" y=\"%d\" font-size=\"10\" fill=\"#4a5568\">%s</text>",
                    legendX + 18, ly + 9, esc(label + typeLabel));
        }

        svg.append("</svg>\n");
        return svg.toString();
    }

    private static String loopColor(LoopDominanceAnalysis dominance, int loopIdx) {
        FeedbackAnalysis.LoopType type = dominance.loopTypes().get(loopIdx);
        if (type == FeedbackAnalysis.LoopType.REINFORCING) {
            return REINFORCING_COLORS[loopIdx % REINFORCING_COLORS.length];
        } else if (type == FeedbackAnalysis.LoopType.BALANCING) {
            return BALANCING_COLORS[loopIdx % BALANCING_COLORS.length];
        }
        return NEUTRAL_COLORS[loopIdx % NEUTRAL_COLORS.length];
    }

    // ── Phase 3: Optimization ─────────────────────────────────────────

    static void writeOptimizationSection(StringBuilder html, OptimizationResult opt) {
        html.append("<section>\n<h2>Optimization Results</h2>\n");

        html.append("<table class=\"info-table\">\n");
        html.append("<tr><th>Objective Value</th><td>")
                .append(fmt(opt.getBestObjectiveValue())).append("</td></tr>\n");
        html.append("<tr><th>Evaluations</th><td>")
                .append(opt.getEvaluationCount()).append("</td></tr>\n");
        html.append("</table>\n");

        html.append("<h3>Best Parameters</h3>\n");
        html.append("<table class=\"element-table\">\n");
        html.append("<thead><tr><th>Parameter</th><th>Value</th></tr></thead>\n");
        html.append("<tbody>\n");
        for (Map.Entry<String, Double> entry : opt.getBestParameters().entrySet()) {
            html.append("<tr><td class=\"name\">").append(esc(entry.getKey())).append("</td>");
            html.append("<td class=\"code\">").append(fmt(entry.getValue())).append("</td></tr>\n");
        }
        html.append("</tbody></table>\n");

        RunResult bestRun = opt.getBestRunResult();
        if (bestRun != null && bestRun.getStepCount() > 0) {
            html.append("<h3>Best Run — Time Series</h3>\n");
            List<String> stockNames = bestRun.getStockNames();
            if (!stockNames.isEmpty()) {
                long[] steps = extractSteps(bestRun);
                List<double[]> series = new ArrayList<>();
                for (String name : stockNames) {
                    series.add(bestRun.getStockSeries(name));
                }
                html.append(lineChartSvg("Best Run", steps, stockNames, series));
            }
        }

        html.append("</section>\n\n");
    }

    // ── SVG: Chart Scaffold ────────────────────────────────────────────

    /**
     * Computed plot-area coordinates and Y-axis range for standard charts.
     * Eliminates repeated margin/tick/axis scaffolding from individual
     * chart methods.
     */
    private record ChartScaffold(int plotLeft, int plotRight, int plotTop, int plotBottom,
                                  int plotWidth, int plotHeight, double yMin, double yMax) {

        static ChartScaffold of(double dataMin, double dataMax) {
            int pLeft = MARGIN_LEFT;
            int pRight = CHART_WIDTH - MARGIN_RIGHT;
            int pTop = MARGIN_TOP;
            int pBottom = CHART_HEIGHT - MARGIN_BOTTOM;
            double yLo = dataMin;
            double yHi = dataMax;
            if (yLo == Double.MAX_VALUE) {
                yLo = 0;
                yHi = 1;
            }
            if (yLo == yHi) {
                yHi = yLo + 1;
            }
            double yPad = (yHi - yLo) * 0.05;
            return new ChartScaffold(pLeft, pRight, pTop, pBottom,
                    pRight - pLeft, pBottom - pTop,
                    yLo - yPad, yHi + yPad);
        }

        double mapY(double value) {
            return plotTop + plotHeight - (value - yMin) / (yMax - yMin) * plotHeight;
        }

        double mapX(double fraction) {
            return plotLeft + fraction * plotWidth;
        }

        void writeOpen(StringBuilder svg) {
            svgLine(svg,
                    "<svg xmlns=\"http://www.w3.org/2000/svg\" viewBox=\"0 0 %d %d\" "
                            + "class=\"chart-svg\">",
                    CHART_WIDTH, CHART_HEIGHT);
        }

        void writeBackground(StringBuilder svg, String fill) {
            svgLine(svg,
                    "<rect x=\"%d\" y=\"%d\" width=\"%d\" height=\"%d\" fill=\"%s\" stroke=\"#e2e8f0\"/>",
                    plotLeft, plotTop, plotWidth, plotHeight, fill);
        }

        void writeAxes(StringBuilder svg) {
            svgLine(svg,
                    "<line x1=\"%d\" y1=\"%d\" x2=\"%d\" y2=\"%d\" stroke=\"#1a1a1a\" stroke-width=\"1\"/>",
                    plotLeft, plotTop, plotLeft, plotBottom);
            svgLine(svg,
                    "<line x1=\"%d\" y1=\"%d\" x2=\"%d\" y2=\"%d\" stroke=\"#1a1a1a\" stroke-width=\"1\"/>",
                    plotLeft, plotBottom, plotRight, plotBottom);
        }

        void writeYTicks(StringBuilder svg, int ticks, boolean gridLines) {
            for (int i = 0; i <= ticks; i++) {
                double y = plotTop + plotHeight - (plotHeight * (double) i / ticks);
                double val = yMin + (yMax - yMin) * i / ticks;
                if (gridLines) {
                    svgLine(svg,
                            "<line x1=\"%d\" y1=\"%.1f\" x2=\"%d\" y2=\"%.1f\" "
                                    + "stroke=\"#edf2f7\" stroke-width=\"1\"/>",
                            plotLeft, y, plotRight, y);
                }
                svgLine(svg,
                        "<text x=\"%d\" y=\"%.1f\" text-anchor=\"end\" "
                                + "font-size=\"11\" fill=\"#4a5568\">%s</text>",
                        plotLeft - 8, y + 4, fmt(val));
            }
        }

        void writeXTicksByStep(StringBuilder svg, long[] steps) {
            long stepMin = steps[0];
            long stepMax = steps[steps.length - 1];
            if (stepMin == stepMax) {
                stepMax = stepMin + 1;
            }
            int xTicks = Math.min(10, steps.length - 1);
            if (xTicks > 0) {
                for (int i = 0; i <= xTicks; i++) {
                    int stepIdx = (int) Math.round((double) i * (steps.length - 1) / xTicks);
                    long step = steps[stepIdx];
                    double x = plotLeft + (double) (step - stepMin) / (stepMax - stepMin) * plotWidth;
                    svgLine(svg,
                            "<text x=\"%.1f\" y=\"%d\" text-anchor=\"middle\" "
                                    + "font-size=\"11\" fill=\"#4a5568\">%d</text>",
                            x, plotBottom + 18, step);
                }
            }
        }

        void writeXTicksByIndex(StringBuilder svg, int stepCount) {
            int xTicks = Math.min(10, stepCount - 1);
            if (xTicks > 0) {
                for (int i = 0; i <= xTicks; i++) {
                    int step = (int) Math.round((double) i * (stepCount - 1) / xTicks);
                    double x = plotLeft + (double) step / (stepCount - 1) * plotWidth;
                    svgLine(svg,
                            "<text x=\"%.1f\" y=\"%d\" text-anchor=\"middle\" "
                                    + "font-size=\"11\" fill=\"#4a5568\">%d</text>",
                            x, plotBottom + 18, step);
                }
            }
        }

        void writeTitle(StringBuilder svg, String title) {
            svgLine(svg,
                    "<text x=\"%d\" y=\"%d\" text-anchor=\"middle\" "
                            + "font-size=\"14\" font-weight=\"600\" fill=\"#2c5282\">%s</text>",
                    plotLeft + plotWidth / 2, 20, esc(title));
        }

        void writeStepLabel(StringBuilder svg) {
            svgLine(svg,
                    "<text x=\"%d\" y=\"%d\" text-anchor=\"middle\" "
                            + "font-size=\"12\" fill=\"#4a5568\">Step</text>",
                    plotLeft + plotWidth / 2, CHART_HEIGHT - 5);
        }
    }

    // ── SVG: Line Chart ─────────────────────────────────────────────────

    static String lineChartSvg(String title, long[] steps,
                               List<String> seriesNames, List<double[]> seriesData) {
        double yMin = Double.MAX_VALUE;
        double yMax = -Double.MAX_VALUE;
        for (double[] data : seriesData) {
            for (double v : data) {
                if (Double.isFinite(v)) {
                    if (v < yMin) {
                        yMin = v;
                    }
                    if (v > yMax) {
                        yMax = v;
                    }
                }
            }
        }
        ChartScaffold chart = ChartScaffold.of(yMin, yMax);

        long stepMin = steps[0];
        long stepMax = steps[steps.length - 1];
        if (stepMin == stepMax) {
            stepMax = stepMin + 1;
        }

        StringBuilder svg = new StringBuilder(4096);
        chart.writeOpen(svg);
        chart.writeBackground(svg, "#fafafa");
        chart.writeYTicks(svg, 5, true);

        // Series polylines
        for (int s = 0; s < seriesData.size(); s++) {
            double[] data = seriesData.get(s);
            String color = SERIES_COLORS[s % SERIES_COLORS.length];
            StringBuilder points = new StringBuilder();
            for (int i = 0; i < data.length; i++) {
                if (!Double.isFinite(data[i])) {
                    continue;
                }
                double x = chart.mapX((double) (steps[i] - stepMin) / (stepMax - stepMin));
                double y = chart.mapY(data[i]);
                if (!points.isEmpty()) {
                    points.append(' ');
                }
                points.append(String.format(Locale.US, "%.1f,%.1f", x, y));
            }
            svgLine(svg,
                    "<polyline points=\"%s\" fill=\"none\" stroke=\"%s\" stroke-width=\"1.5\"/>",
                    points, color);
        }

        chart.writeAxes(svg);
        chart.writeXTicksByStep(svg, steps);
        chart.writeStepLabel(svg);
        chart.writeTitle(svg, title);

        // Legend (only when multiple series)
        if (seriesNames.size() > 1) {
            int legendX = chart.plotRight() - 180;
            int legendY = chart.plotTop() + 10;
            for (int s = 0; s < seriesNames.size() && s < SERIES_COLORS.length; s++) {
                String color = SERIES_COLORS[s % SERIES_COLORS.length];
                int ly = legendY + s * 16;
                svgLine(svg,
                        "<line x1=\"%d\" y1=\"%d\" x2=\"%d\" y2=\"%d\" "
                                + "stroke=\"%s\" stroke-width=\"2\"/>",
                        legendX, ly, legendX + 14, ly, color);
                svgLine(svg,
                        "<text x=\"%d\" y=\"%d\" font-size=\"10\" fill=\"#4a5568\">%s</text>",
                        legendX + 18, ly + 4, esc(seriesNames.get(s)));
            }
        }

        svg.append("</svg>\n");
        return svg.toString();
    }

    // ── SVG: Fan Chart ──────────────────────────────────────────────────

    static String fanChartSvg(String variableName, MonteCarloResult mc) {
        int stepCount = mc.getStepCount();
        if (stepCount <= 1) {
            return "<p>Not enough data points for fan chart.</p>\n";
        }

        double[] percentileValues = {2.5, 12.5, 25.0, 50.0, 75.0, 87.5, 97.5};
        Map<Double, double[]> pctMap = mc.getPercentileSeries(variableName, percentileValues);
        double[] pct2 = pctMap.get(2.5);
        double[] pct97 = pctMap.get(97.5);
        double[] pct12 = pctMap.get(12.5);
        double[] pct87 = pctMap.get(87.5);
        double[] p25 = pctMap.get(25.0);
        double[] p75 = pctMap.get(75.0);
        double[] median = pctMap.get(50.0);

        double[][] lowerBands = {pct2, pct12, p25};
        double[][] upperBands = {pct97, pct87, p75};
        double[] opacities = {0.15, 0.25, 0.35};

        double yMin = Double.MAX_VALUE;
        double yMax = -Double.MAX_VALUE;
        for (int i = 0; i < stepCount; i++) {
            yMin = Math.min(yMin, pct2[i]);
            yMax = Math.max(yMax, pct97[i]);
        }
        ChartScaffold chart = ChartScaffold.of(yMin, yMax);

        StringBuilder svg = new StringBuilder(8192);
        chart.writeOpen(svg);
        chart.writeBackground(svg, "white");

        // Percentile bands
        for (int b = 0; b < lowerBands.length; b++) {
            StringBuilder points = new StringBuilder();
            // Upper edge (left to right)
            for (int i = 0; i < stepCount; i++) {
                double x = chart.mapX((double) i / (stepCount - 1));
                double y = chart.mapY(upperBands[b][i]);
                if (!points.isEmpty()) {
                    points.append(' ');
                }
                points.append(String.format(Locale.US, "%.1f,%.1f", x, y));
            }
            // Lower edge (right to left)
            for (int i = stepCount - 1; i >= 0; i--) {
                double x = chart.mapX((double) i / (stepCount - 1));
                double y = chart.mapY(lowerBands[b][i]);
                points.append(' ');
                points.append(String.format(Locale.US, "%.1f,%.1f", x, y));
            }
            svgLine(svg,
                    "<polygon points=\"%s\" fill=\"rgba(70,130,180,%.2f)\" stroke=\"none\"/>",
                    points, opacities[b]);
        }

        // Median line
        StringBuilder medianPoints = new StringBuilder();
        for (int i = 0; i < stepCount; i++) {
            double x = chart.mapX((double) i / (stepCount - 1));
            double y = chart.mapY(median[i]);
            if (!medianPoints.isEmpty()) {
                medianPoints.append(' ');
            }
            medianPoints.append(String.format(Locale.US, "%.1f,%.1f", x, y));
        }
        svgLine(svg,
                "<polyline points=\"%s\" fill=\"none\" stroke=\"rgba(70,130,180,0.9)\" "
                        + "stroke-width=\"2\"/>",
                medianPoints);

        chart.writeAxes(svg);
        chart.writeYTicks(svg, 5, false);
        chart.writeXTicksByIndex(svg, stepCount);
        chart.writeStepLabel(svg);
        chart.writeTitle(svg, variableName + " — Fan Chart (" + mc.getRunCount() + " runs)");

        // Legend
        int legendX = chart.plotRight() - 140;
        int legendY = chart.plotTop() + 10;
        String[] bandLabels = {"P2.5–P97.5", "P12.5–P87.5", "P25–P75"};
        for (int b = 0; b < bandLabels.length; b++) {
            svgLine(svg,
                    "<rect x=\"%d\" y=\"%d\" width=\"14\" height=\"10\" "
                            + "fill=\"rgba(70,130,180,%.2f)\"/>",
                    legendX, legendY + b * 16, opacities[b]);
            svgLine(svg,
                    "<text x=\"%d\" y=\"%d\" font-size=\"10\" fill=\"#4a5568\">%s</text>",
                    legendX + 18, legendY + b * 16 + 9, bandLabels[b]);
        }
        svgLine(svg,
                "<line x1=\"%d\" y1=\"%d\" x2=\"%d\" y2=\"%d\" "
                        + "stroke=\"rgba(70,130,180,0.9)\" stroke-width=\"2\"/>",
                legendX, legendY + 3 * 16, legendX + 14, legendY + 3 * 16);
        svgLine(svg,
                "<text x=\"%d\" y=\"%d\" font-size=\"10\" fill=\"#4a5568\">Median</text>",
                legendX + 18, legendY + 3 * 16 + 4);

        svg.append("</svg>\n");
        return svg.toString();
    }

    // ── SVG: Tornado Chart ──────────────────────────────────────────────

    static String tornadoChartSvg(List<ParameterImpact> impacts) {
        if (impacts.isEmpty()) {
            return "";
        }

        int barCount = impacts.size();
        int chartHeight = TORNADO_TOP + barCount * (TORNADO_BAR_HEIGHT + TORNADO_BAR_GAP) + 30;
        int barAreaWidth = CHART_WIDTH - TORNADO_LEFT_MARGIN - TORNADO_RIGHT_MARGIN;

        StringBuilder svg = new StringBuilder(2048);
        svgLine(svg,
                "<svg xmlns=\"http://www.w3.org/2000/svg\" viewBox=\"0 0 %d %d\" "
                        + "class=\"chart-svg\">",
                CHART_WIDTH, chartHeight);

        // Title
        svgLine(svg,
                "<text x=\"%d\" y=\"20\" font-size=\"14\" font-weight=\"600\" "
                        + "fill=\"#2c5282\">Sensitivity — Variance Decomposition</text>",
                TORNADO_LEFT_MARGIN);

        // Bars (impacts are already sorted descending)
        for (int i = 0; i < barCount; i++) {
            ParameterImpact impact = impacts.get(i);
            double pct = impact.impactFraction() * 100.0;
            int barWidth = (int) Math.round(pct / 100.0 * barAreaWidth);
            int y = TORNADO_TOP + i * (TORNADO_BAR_HEIGHT + TORNADO_BAR_GAP);

            // Label
            svgLine(svg,
                    "<text x=\"%d\" y=\"%d\" text-anchor=\"end\" "
                            + "font-size=\"12\" fill=\"#1a1a1a\">%s</text>",
                    TORNADO_LEFT_MARGIN - 8, y + TORNADO_BAR_HEIGHT / 2 + 4,
                    esc(impact.parameterName()));

            // Bar
            svgLine(svg,
                    "<rect x=\"%d\" y=\"%d\" width=\"%d\" height=\"%d\" "
                            + "fill=\"#4393c3\" rx=\"2\"/>",
                    TORNADO_LEFT_MARGIN, y, Math.max(barWidth, 1), TORNADO_BAR_HEIGHT);

            // Percentage label on bar
            if (pct >= 3.0) {
                svgLine(svg,
                        "<text x=\"%d\" y=\"%d\" font-size=\"11\" fill=\"white\" "
                                + "font-weight=\"600\">%s</text>",
                        TORNADO_LEFT_MARGIN + 6, y + TORNADO_BAR_HEIGHT / 2 + 4,
                        fmtPercent(impact.impactFraction()));
            } else {
                svgLine(svg,
                        "<text x=\"%d\" y=\"%d\" font-size=\"11\" fill=\"#4a5568\">%s</text>",
                        TORNADO_LEFT_MARGIN + barWidth + 4, y + TORNADO_BAR_HEIGHT / 2 + 4,
                        fmtPercent(impact.impactFraction()));
            }
        }

        // Baseline axis
        svgLine(svg,
                "<line x1=\"%d\" y1=\"%d\" x2=\"%d\" y2=\"%d\" "
                        + "stroke=\"#1a1a1a\" stroke-width=\"1\"/>",
                TORNADO_LEFT_MARGIN, TORNADO_TOP - 5,
                TORNADO_LEFT_MARGIN, chartHeight - 10);

        svg.append("</svg>\n");
        return svg.toString();
    }

    // ── HTML Header & CSS ───────────────────────────────────────────────

    private static void writeHeader(StringBuilder html, String title,
                                    ReportTemplate template) {
        html.append("""
                <!DOCTYPE html>
                <html lang="en">
                <head>
                <meta charset="UTF-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                """);
        html.append("<title>").append(esc(title)).append(" — Simulation Results</title>\n");
        html.append(CSS);
        if (template != null && template.customCss() != null && !template.customCss().isBlank()) {
            html.append("<style>\n").append(template.customCss()).append("\n</style>\n");
        }
        html.append("</head>\n");
    }

    // ── Helpers ─────────────────────────────────────────────────────────

    static String esc(String text) {
        return ReportGenerator.esc(text);
    }

    /**
     * Formats an SVG element line using {@link Locale#US} and appends a newline.
     * Keeps the literal {@code \n} out of the format string to avoid the SpotBugs
     * VA_FORMAT_STRING_USES_NEWLINE warning.
     */
    private static void svgLine(StringBuilder sb, String format, Object... args) {
        sb.append(String.format(Locale.US, format, args)).append('\n');
    }

    private static void writeStatsRow(StringBuilder html, String name, String type, double[] series) {
        double min = Double.MAX_VALUE;
        double max = -Double.MAX_VALUE;
        double sum = 0;
        for (double v : series) {
            if (v < min) {
                min = v;
            }
            if (v > max) {
                max = v;
            }
            sum += v;
        }
        double mean = series.length > 0 ? sum / series.length : 0;
        double finalVal = series.length > 0 ? series[series.length - 1] : 0;

        html.append("<tr>");
        html.append("<td class=\"name\">").append(esc(name)).append("</td>");
        html.append("<td>").append(type).append("</td>");
        html.append("<td class=\"code\">").append(fmt(min)).append("</td>");
        html.append("<td class=\"code\">").append(fmt(max)).append("</td>");
        html.append("<td class=\"code\">").append(fmt(mean)).append("</td>");
        html.append("<td class=\"code\">").append(fmt(finalVal)).append("</td>");
        html.append("</tr>\n");
    }

    static String fmt(double value) {
        if (!Double.isFinite(value)) {
            return "—";
        }
        if (value == Math.floor(value) && Math.abs(value) < 1e15) {
            return String.format(Locale.US, "%.0f", value);
        }
        if (Math.abs(value) >= 0.01 && Math.abs(value) < 1e6) {
            return String.format(Locale.US, "%.4f", value).replaceAll("0+$", "")
                    .replaceAll("\\.$", "");
        }
        return String.format(Locale.US, "%.6g", value);
    }

    private static String fmtPct(double percentile) {
        if (percentile == Math.floor(percentile)) {
            return String.valueOf((int) percentile);
        }
        return String.valueOf(percentile);
    }

    private static String fmtPercent(double fraction) {
        double pct = fraction * 100.0;
        if (pct >= 1.0) {
            return String.format(Locale.US, "%.0f%%", pct);
        }
        if (pct >= 0.1) {
            return String.format(Locale.US, "%.1f%%", pct);
        }
        if (pct > 0) {
            return "<0.1%";
        }
        return "0%";
    }

    private static long[] extractSteps(RunResult run) {
        long[] steps = new long[run.getStepCount()];
        for (int i = 0; i < steps.length; i++) {
            steps[i] = run.getStep(i);
        }
        return steps;
    }

    private static double[] variableSeries(RunResult run, String name) {
        List<String> names = run.getVariableNames();
        int index = names.indexOf(name);
        if (index < 0) {
            throw new IllegalArgumentException("Unknown variable: " + name);
        }
        double[] series = new double[run.getStepCount()];
        for (int i = 0; i < series.length; i++) {
            series[i] = run.getVariableValuesAtStep(i)[index];
        }
        return series;
    }

    // ── CSS ─────────────────────────────────────────────────────────────

    /** Extra CSS rules needed by result sections but not present in ReportGenerator. */
    private static final String RESULTS_EXTRA_CSS = """
            h3 {
                font-size: 1rem;
                color: #4a5568;
                margin-top: 1rem;
                margin-bottom: 0.5rem;
            }
            .chart-svg {
                width: 100%;
                max-width: 800px;
                height: auto;
                margin: 1rem 0;
                font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif;
            }
            details {
                margin: 0.75rem 0;
                border: 1px solid #e2e8f0;
                border-radius: 4px;
            }
            details summary {
                cursor: pointer;
                padding: 0.5rem 0.75rem;
                font-weight: 600;
                color: #4a5568;
                background: #f7fafc;
                user-select: none;
            }
            details summary:hover { background: #edf2f7; }
            details[open] summary { border-bottom: 1px solid #e2e8f0; }
            details .element-table { margin-bottom: 0; }
            .summary-text {
                margin: 0.75rem 0;
                font-size: 0.95rem;
                color: #4a5568;
                line-height: 1.5;
            }
            .behavior-badge {
                display: inline-block;
                padding: 0.15rem 0.5rem;
                border-radius: 3px;
                font-size: 0.8rem;
                font-weight: 600;
                color: white;
            }
            .behavior-growth { background: #27ae60; }
            .behavior-decay { background: #e67e22; }
            .behavior-goal { background: #2980b9; }
            .behavior-oscillation { background: #8e44ad; }
            .behavior-s-shaped { background: #16a085; }
            .behavior-overshoot { background: #c0392b; }
            .behavior-equilibrium { background: #95a5a6; }
            """;

    private static final String CSS = """
            <style>
            * { box-sizing: border-box; margin: 0; padding: 0; }
            body {
                font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif;
                line-height: 1.6;
                color: #1a1a1a;
                background: #fff;
                padding: 2rem;
            }
            .container { max-width: 960px; margin: 0 auto; }
            h1 {
                font-size: 1.8rem;
                border-bottom: 2px solid #2c5282;
                padding-bottom: 0.5rem;
                margin-bottom: 1.5rem;
                color: #2c5282;
            }
            h2 {
                font-size: 1.2rem;
                color: #2c5282;
                margin-top: 1.5rem;
                margin-bottom: 0.75rem;
                border-bottom: 1px solid #e2e8f0;
                padding-bottom: 0.3rem;
            }
            h3 {
                font-size: 1rem;
                color: #4a5568;
                margin-top: 1rem;
                margin-bottom: 0.5rem;
            }
            section { margin-bottom: 1.5rem; }
            table { width: 100%; border-collapse: collapse; margin-bottom: 1rem; }
            .info-table th {
                text-align: left;
                width: 160px;
                padding: 0.3rem 0.75rem;
                color: #4a5568;
                font-weight: 600;
                vertical-align: top;
            }
            .info-table td { padding: 0.3rem 0.75rem; }
            .element-table th {
                text-align: left;
                padding: 0.4rem 0.75rem;
                background: #f7fafc;
                border-bottom: 2px solid #e2e8f0;
                font-weight: 600;
                color: #4a5568;
                font-size: 0.85rem;
            }
            .element-table td {
                padding: 0.35rem 0.75rem;
                border-bottom: 1px solid #edf2f7;
                font-size: 0.9rem;
            }
            .element-table tbody tr:hover { background: #f7fafc; }
            .name { font-weight: 600; }
            .code { font-family: "SFMono-Regular", Consolas, monospace; font-size: 0.85rem; }
            .chart-svg {
                width: 100%;
                max-width: 800px;
                height: auto;
                margin: 1rem 0;
                font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif;
            }
            details {
                margin: 0.75rem 0;
                border: 1px solid #e2e8f0;
                border-radius: 4px;
            }
            details summary {
                cursor: pointer;
                padding: 0.5rem 0.75rem;
                font-weight: 600;
                color: #4a5568;
                background: #f7fafc;
                user-select: none;
            }
            details summary:hover { background: #edf2f7; }
            details[open] summary { border-bottom: 1px solid #e2e8f0; }
            details .element-table { margin-bottom: 0; }
            .summary-text {
                margin: 0.75rem 0;
                font-size: 0.95rem;
                color: #4a5568;
                line-height: 1.5;
            }
            .behavior-badge {
                display: inline-block;
                padding: 0.15rem 0.5rem;
                border-radius: 3px;
                font-size: 0.8rem;
                font-weight: 600;
                color: white;
            }
            .behavior-growth { background: #27ae60; }
            .behavior-decay { background: #e67e22; }
            .behavior-goal { background: #2980b9; }
            .behavior-oscillation { background: #8e44ad; }
            .behavior-s-shaped { background: #16a085; }
            .behavior-overshoot { background: #c0392b; }
            .behavior-equilibrium { background: #95a5a6; }
            .report-header {
                margin-bottom: 1.5rem;
                padding-bottom: 0.5rem;
                border-bottom: 1px solid #e2e8f0;
            }
            .report-footer {
                margin-top: 2rem;
                padding-top: 0.5rem;
                border-top: 1px solid #e2e8f0;
                font-size: 0.85rem;
                color: #718096;
            }
            @media print {
                body { padding: 0; font-size: 10pt; }
                .container { max-width: 100%; }
                h1 { font-size: 16pt; }
                h2 { font-size: 12pt; page-break-after: avoid; }
                table { page-break-inside: avoid; }
                details { border: none; }
                details[open] summary { display: none; }
                .element-table tbody tr:hover { background: none; }
            }
            </style>
            """;
}
