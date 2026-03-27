package systems.courant.sd.app.canvas;

import systems.courant.sd.model.graph.FeedbackAnalysis;
import systems.courant.sd.model.graph.LoopDominanceAnalysis;

import java.util.Locale;

import static systems.courant.sd.app.canvas.ResultReportGenerator.CHART_WIDTH;
import static systems.courant.sd.app.canvas.ResultReportGenerator.MARGIN_LEFT;
import static systems.courant.sd.app.canvas.ResultReportGenerator.MARGIN_RIGHT;
import static systems.courant.sd.app.canvas.ResultReportGenerator.MARGIN_TOP;
import static systems.courant.sd.app.canvas.ResultReportGenerator.MARGIN_BOTTOM;
import static systems.courant.sd.app.canvas.ResultReportGenerator.AREA_CHART_HEIGHT;
import static systems.courant.sd.app.canvas.ResultReportGenerator.esc;
import static systems.courant.sd.app.canvas.ResultReportGenerator.svgLine;

/**
 * Generates the loop dominance report section: stacked area chart,
 * dominance transition table, and loop legends.
 */
final class LoopDominanceReportSection {

    private static final String[] REINFORCING_COLORS = {"#27ae60", "#2ecc71", "#1abc9c", "#16a085"};
    private static final String[] BALANCING_COLORS = {"#2980b9", "#3498db", "#2c3e50", "#34495e"};
    private static final String[] NEUTRAL_COLORS = {"#7f8c8d", "#95a5a6", "#bdc3c7"};

    private LoopDominanceReportSection() {
    }

    static void write(StringBuilder html, LoopDominanceAnalysis dominance) {
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
            html.append("<td>").append(from).append("\u2013").append(to).append("</td>");
        }
        if (loopIdx >= 0 && loopIdx < dominance.loopCount()) {
            html.append("<td class=\"name\">")
                    .append(esc(dominance.loopLabels().get(loopIdx))).append("</td>");
            FeedbackAnalysis.LoopType type = dominance.loopTypes().get(loopIdx);
            html.append("<td>").append(type != null ? type.label() : "\u2014").append("</td>");
        } else {
            html.append("<td class=\"name\">None</td><td>\u2014</td>");
        }
        html.append("</tr>\n");
    }

    static String stackedAreaChartSvg(LoopDominanceAnalysis dominance) {
        int loopCount = dominance.loopCount();
        int stepCount = dominance.stepCount();
        if (loopCount == 0 || stepCount <= 1) {
            return "<p>Not enough data for loop dominance chart.</p>\n";
        }

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

        int sampleInterval = Math.max(1, stepCount / 500);
        int sampleCount = 0;
        for (int s = 0; s < stepCount; s += sampleInterval) {
            sampleCount++;
        }

        ResultReportGenerator.ChartScaffold chart = new ResultReportGenerator.ChartScaffold(
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

        for (int loop = loopCount - 1; loop >= 0; loop--) {
            String color = loopColor(dominance, loop);
            StringBuilder points = new StringBuilder();

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

        for (int i = 0; i <= 5; i++) {
            double frac = i / 5.0;
            double y = chart.mapY(frac);
            svgLine(svg,
                    "<text x=\"%d\" y=\"%.1f\" text-anchor=\"end\" "
                            + "font-size=\"11\" fill=\"#4a5568\">%d%%</text>",
                    MARGIN_LEFT - 8, y + 4, (int) (frac * 100));
        }

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

        svgLine(svg,
                "<text x=\"%d\" y=\"%d\" text-anchor=\"middle\" "
                        + "font-size=\"12\" fill=\"#4a5568\">Step</text>",
                MARGIN_LEFT + chart.plotWidth() / 2, AREA_CHART_HEIGHT - 5);

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
}
