package systems.courant.sd.app.canvas;

import systems.courant.sd.sweep.MonteCarloResult;
import systems.courant.sd.sweep.MultiSweepResult;
import systems.courant.sd.sweep.OptimizationResult;
import systems.courant.sd.sweep.RunResult;
import systems.courant.sd.sweep.SweepResult;

import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;

import java.util.List;
import java.util.Map;
import java.util.StringJoiner;

/**
 * Formats simulation results as tab-separated text and copies to the system clipboard.
 * Tab-separated format is recognized by Excel, Google Sheets, and most spreadsheet tools
 * when pasting.
 */
public final class ClipboardExporter {

    private ClipboardExporter() {
    }

    // ── Copy methods (format + clipboard) ──────────────────────────────

    public static void copySimulationResult(SimulationRunner.SimulationResult result) {
        copyToClipboard(formatSimulationResult(result));
    }

    public static void copySweepTimeSeries(SweepResult result) {
        copyToClipboard(formatSweepTimeSeries(result));
    }

    public static void copySweepSummary(SweepResult result) {
        copyToClipboard(formatSweepSummary(result));
    }

    public static void copyMonteCarloPercentiles(MonteCarloResult result, String variableName) {
        String text = formatMonteCarloPercentiles(result, variableName);
        if (text != null) {
            copyToClipboard(text);
        }
    }

    public static void copyMultiSweepSummary(MultiSweepResult result) {
        copyToClipboard(formatMultiSweepSummary(result));
    }

    public static void copyOptimizationBestRun(OptimizationResult result) {
        copyToClipboard(formatOptimizationBestRun(result));
    }

    public static void copyCalibrationBestRun(OptimizationResult result) {
        copyToClipboard(formatOptimizationBestRun(result));
    }

    // ── Format methods (pure functions, testable) ──────────────────────

    /**
     * Formats a single simulation result as tab-separated text.
     */
    public static String formatSimulationResult(SimulationRunner.SimulationResult result) {
        StringBuilder sb = new StringBuilder();
        sb.append(String.join("\t", result.columnNames())).append('\n');
        for (double[] row : result.rows()) {
            StringJoiner joiner = new StringJoiner("\t");
            for (int i = 0; i < row.length; i++) {
                joiner.add(i == 0 ? String.valueOf((int) row[i]) : String.valueOf(row[i]));
            }
            sb.append(joiner).append('\n');
        }
        return sb.toString();
    }

    /**
     * Formats a parameter sweep time-series as tab-separated text.
     */
    public static String formatSweepTimeSeries(SweepResult result) {
        StringBuilder sb = new StringBuilder();
        StringJoiner header = new StringJoiner("\t");
        header.add(result.getParameterName());
        header.add("Step");
        result.getStockNames().forEach(header::add);
        result.getVariableNames().forEach(header::add);
        sb.append(header).append('\n');

        for (RunResult run : result.getResults()) {
            String paramVal = String.valueOf(run.getParameterValue());
            for (int i = 0; i < run.getStepCount(); i++) {
                StringJoiner joiner = new StringJoiner("\t");
                joiner.add(paramVal);
                joiner.add(String.valueOf(run.getStep(i)));
                for (double v : run.getStockValuesAtStep(i)) {
                    joiner.add(String.valueOf(v));
                }
                for (double v : run.getVariableValuesAtStep(i)) {
                    joiner.add(String.valueOf(v));
                }
                sb.append(joiner).append('\n');
            }
        }
        return sb.toString();
    }

    /**
     * Formats a parameter sweep summary as tab-separated text.
     */
    public static String formatSweepSummary(SweepResult result) {
        StringBuilder sb = new StringBuilder();
        StringJoiner header = new StringJoiner("\t");
        header.add(result.getParameterName());
        for (String stockName : result.getStockNames()) {
            header.add(stockName + "_final");
            header.add(stockName + "_max");
        }
        sb.append(header).append('\n');

        for (RunResult run : result.getResults()) {
            StringJoiner joiner = new StringJoiner("\t");
            joiner.add(String.valueOf(run.getParameterValue()));
            for (String stockName : run.getStockNames()) {
                joiner.add(String.valueOf(run.getFinalStockValue(stockName)));
                joiner.add(String.valueOf(run.getMaxStockValue(stockName)));
            }
            sb.append(joiner).append('\n');
        }
        return sb.toString();
    }

    /**
     * Formats Monte Carlo percentile data as tab-separated text, or returns null
     * if no variable is selected.
     */
    public static String formatMonteCarloPercentiles(MonteCarloResult result, String variableName) {
        if (variableName == null) {
            return null;
        }
        double[] percentiles = {2.5, 25, 50, 75, 97.5};
        Map<Double, double[]> pctMap = result.getPercentileSeries(variableName, percentiles);
        int stepCount = result.getStepCount();

        StringBuilder sb = new StringBuilder();
        StringJoiner header = new StringJoiner("\t");
        header.add("Step");
        for (double p : percentiles) {
            header.add("p" + ChartUtils.formatNumber(p));
        }
        sb.append(header).append('\n');

        for (int s = 0; s < stepCount; s++) {
            StringJoiner joiner = new StringJoiner("\t");
            joiner.add(String.valueOf(s));
            for (double p : percentiles) {
                joiner.add(String.valueOf(pctMap.get(p)[s]));
            }
            sb.append(joiner).append('\n');
        }
        return sb.toString();
    }

    /**
     * Formats a multi-parameter sweep summary as tab-separated text.
     */
    public static String formatMultiSweepSummary(MultiSweepResult result) {
        StringBuilder sb = new StringBuilder();
        StringJoiner header = new StringJoiner("\t");
        result.getParameterNames().forEach(header::add);
        for (String stockName : result.getStockNames()) {
            header.add(stockName + "_final");
            header.add(stockName + "_max");
        }
        sb.append(header).append('\n');

        List<String> paramNames = result.getParameterNames();
        for (RunResult run : result.getResults()) {
            StringJoiner joiner = new StringJoiner("\t");
            Map<String, Double> paramMap = run.getParameterMap();
            for (String name : paramNames) {
                joiner.add(String.valueOf(paramMap.get(name)));
            }
            for (String stockName : run.getStockNames()) {
                joiner.add(String.valueOf(run.getFinalStockValue(stockName)));
                joiner.add(String.valueOf(run.getMaxStockValue(stockName)));
            }
            sb.append(joiner).append('\n');
        }
        return sb.toString();
    }

    /**
     * Formats optimization best-run time series as tab-separated text.
     */
    public static String formatOptimizationBestRun(OptimizationResult result) {
        RunResult bestRun = result.getBestRunResult();
        StringBuilder sb = new StringBuilder();
        StringJoiner header = new StringJoiner("\t");
        header.add("Step");
        bestRun.getStockNames().forEach(header::add);
        bestRun.getVariableNames().forEach(header::add);
        sb.append(header).append('\n');

        for (int s = 0; s < bestRun.getStepCount(); s++) {
            StringJoiner joiner = new StringJoiner("\t");
            joiner.add(String.valueOf(bestRun.getStep(s)));
            for (double v : bestRun.getStockValuesAtStep(s)) {
                joiner.add(String.valueOf(v));
            }
            for (double v : bestRun.getVariableValuesAtStep(s)) {
                joiner.add(String.valueOf(v));
            }
            sb.append(joiner).append('\n');
        }
        return sb.toString();
    }

    // ── Private ────────────────────────────────────────────────────────

    private static void copyToClipboard(String text) {
        ClipboardContent content = new ClipboardContent();
        content.putString(text);
        Clipboard.getSystemClipboard().setContent(content);
    }
}
