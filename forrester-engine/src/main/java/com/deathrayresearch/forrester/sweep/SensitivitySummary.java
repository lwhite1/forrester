package com.deathrayresearch.forrester.sweep;

import org.apache.commons.math3.stat.correlation.SpearmansCorrelation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Computes a ranked sensitivity summary from sweep or Monte Carlo results,
 * identifying which parameters have the greatest impact on a target variable.
 */
public final class SensitivitySummary {

    private SensitivitySummary() {
    }

    /**
     * A single parameter's impact on a target variable.
     *
     * @param parameterName the parameter that was varied
     * @param targetVariable the output variable being measured
     * @param minOutput the minimum output value observed across runs
     * @param maxOutput the maximum output value observed across runs
     * @param baselineOutput the output value at the midpoint or mean parameter value
     * @param impactFraction the relative impact as a fraction of the baseline (0.40 = ±40%)
     */
    public record ParameterImpact(
            String parameterName,
            String targetVariable,
            double minOutput,
            double maxOutput,
            double baselineOutput,
            double impactFraction) implements Comparable<ParameterImpact> {

        @Override
        public int compareTo(ParameterImpact other) {
            return Double.compare(Math.abs(other.impactFraction), Math.abs(this.impactFraction));
        }
    }

    /**
     * Computes sensitivity from a single-parameter sweep by measuring the output range
     * of the target variable across all parameter values.
     *
     * @param result the sweep result
     * @param targetVariable the stock or variable name to measure
     * @return a list with a single {@link ParameterImpact} entry
     */
    public static List<ParameterImpact> fromSweep(SweepResult result, String targetVariable) {
        if (result.getRunCount() == 0) {
            return Collections.emptyList();
        }

        double[] finalValues = extractFinalValues(result.getResults(), targetVariable);
        double[] minMax = minMax(finalValues);
        double baseline = mean(finalValues);
        double impact = computeImpactFraction(minMax[0], minMax[1], baseline);

        return List.of(new ParameterImpact(
                result.getParameterName(), targetVariable, minMax[0], minMax[1], baseline, impact));
    }

    /**
     * Computes sensitivity from a multi-parameter sweep using one-at-a-time analysis.
     * For each parameter, holds others at their median values and measures the output range.
     *
     * @param result the multi-sweep result
     * @param targetVariable the stock or variable name to measure
     * @return a sorted list of {@link ParameterImpact} entries, most impactful first
     */
    public static List<ParameterImpact> fromMultiSweep(MultiSweepResult result, String targetVariable) {
        if (result.getRunCount() == 0) {
            return Collections.emptyList();
        }

        List<String> paramNames = result.getParameterNames();
        List<RunResult> runs = result.getResults();

        // Collect unique values for each parameter
        List<List<Double>> paramValueSets = new ArrayList<>();
        for (String name : paramNames) {
            List<Double> values = new ArrayList<>();
            for (RunResult run : runs) {
                double v = run.getParameterMap().get(name);
                if (!values.contains(v)) {
                    values.add(v);
                }
            }
            Collections.sort(values);
            paramValueSets.add(values);
        }

        // Compute median values for each parameter
        double[] medians = new double[paramNames.size()];
        for (int i = 0; i < paramNames.size(); i++) {
            List<Double> vals = paramValueSets.get(i);
            medians[i] = vals.get(vals.size() / 2);
        }

        List<ParameterImpact> impacts = new ArrayList<>();

        for (int p = 0; p < paramNames.size(); p++) {
            String paramName = paramNames.get(p);
            int paramIndex = p;

            // Filter runs where all OTHER parameters are at their median
            List<RunResult> filtered = new ArrayList<>();
            for (RunResult run : runs) {
                Map<String, Double> map = run.getParameterMap();
                boolean othersAtMedian = true;
                for (int j = 0; j < paramNames.size(); j++) {
                    if (j != paramIndex) {
                        double val = map.get(paramNames.get(j));
                        if (Double.compare(val, medians[j]) != 0) {
                            othersAtMedian = false;
                            break;
                        }
                    }
                }
                if (othersAtMedian) {
                    filtered.add(run);
                }
            }

            if (filtered.isEmpty()) {
                continue;
            }

            double[] finalValues = extractFinalValues(filtered, targetVariable);
            double[] minMax = minMax(finalValues);
            double baseline = mean(finalValues);
            double impact = computeImpactFraction(minMax[0], minMax[1], baseline);
            impacts.add(new ParameterImpact(paramName, targetVariable, minMax[0], minMax[1], baseline, impact));
        }

        Collections.sort(impacts);
        return Collections.unmodifiableList(impacts);
    }

    /**
     * Computes sensitivity from Monte Carlo results using Spearman rank correlation
     * between each input parameter and the final value of the target variable.
     * The absolute correlation coefficient is used as the impact fraction.
     *
     * @param result the Monte Carlo result
     * @param targetVariable the stock or variable name to measure
     * @return a sorted list of {@link ParameterImpact} entries, most impactful first
     */
    public static List<ParameterImpact> fromMonteCarlo(MonteCarloResult result, String targetVariable) {
        List<RunResult> runs = result.getResults();
        if (runs.isEmpty()) {
            return Collections.emptyList();
        }

        // All runs should have the same parameter names
        Map<String, Double> firstMap = runs.getFirst().getParameterMap();
        if (firstMap.isEmpty()) {
            return Collections.emptyList();
        }

        List<String> paramNames = new ArrayList<>(firstMap.keySet());
        double[] finalValues = extractFinalValues(runs, targetVariable);
        int n = runs.size();

        SpearmansCorrelation spearman = new SpearmansCorrelation();
        List<ParameterImpact> impacts = new ArrayList<>();

        for (String paramName : paramNames) {
            double[] paramValues = new double[n];
            for (int i = 0; i < n; i++) {
                paramValues[i] = runs.get(i).getParameterMap().get(paramName);
            }

            double correlation = spearman.correlation(paramValues, finalValues);
            if (Double.isNaN(correlation)) {
                correlation = 0.0;
            }

            double[] minMax = minMax(finalValues);
            double baseline = mean(finalValues);

            impacts.add(new ParameterImpact(
                    paramName, targetVariable, minMax[0], minMax[1], baseline, correlation));
        }

        Collections.sort(impacts);
        return Collections.unmodifiableList(impacts);
    }

    /**
     * Generates a plain-language sensitivity summary from a ranked list of impacts.
     *
     * @param impacts the ranked parameter impacts
     * @return a human-readable summary string
     */
    public static String toPlainLanguage(List<ParameterImpact> impacts) {
        if (impacts.isEmpty()) {
            return "No sensitivity data available.";
        }

        ParameterImpact first = impacts.getFirst();
        StringBuilder sb = new StringBuilder();
        sb.append(first.targetVariable())
                .append(" is most sensitive to ")
                .append(first.parameterName())
                .append(" (")
                .append(formatImpact(first.impactFraction()))
                .append(")");

        for (int i = 1; i < impacts.size(); i++) {
            ParameterImpact impact = impacts.get(i);
            if (i == impacts.size() - 1 && impacts.size() > 2) {
                sb.append(", and ");
            } else if (i > 0) {
                sb.append(", followed by ");
            }
            sb.append(impact.parameterName())
                    .append(" (")
                    .append(formatImpact(impact.impactFraction()))
                    .append(")");
        }

        sb.append(".");
        return sb.toString();
    }

    private static double[] extractFinalValues(List<RunResult> runs, String targetVariable) {
        double[] values = new double[runs.size()];
        RunResult sample = runs.getFirst();
        boolean isStock = sample.getStockNames().contains(targetVariable);
        int colIndex;
        if (isStock) {
            colIndex = sample.getStockNames().indexOf(targetVariable);
        } else {
            colIndex = sample.getVariableNames().indexOf(targetVariable);
            if (colIndex < 0) {
                throw new IllegalArgumentException("Unknown stock or variable: " + targetVariable);
            }
        }

        for (int i = 0; i < runs.size(); i++) {
            RunResult run = runs.get(i);
            int lastStep = run.getStepCount() - 1;
            if (isStock) {
                values[i] = run.getStockValuesAtStep(lastStep)[colIndex];
            } else {
                values[i] = run.getVariableValuesAtStep(lastStep)[colIndex];
            }
        }
        return values;
    }

    private static double computeImpactFraction(double min, double max, double meanBaseline) {
        double range = max - min;
        if (range == 0) {
            return 0.0;
        }
        if (meanBaseline == 0) {
            // Output crosses zero — use range relative to itself as a marker
            return 1.0;
        }
        return range / (2.0 * Math.abs(meanBaseline));
    }

    private static double[] minMax(double[] values) {
        double min = Double.MAX_VALUE;
        double max = -Double.MAX_VALUE;
        for (double v : values) {
            if (v < min) { min = v; }
            if (v > max) { max = v; }
        }
        return new double[]{min, max};
    }

    private static double mean(double[] values) {
        double sum = 0;
        for (double v : values) {
            sum += v;
        }
        return sum / values.length;
    }

    private static String formatImpact(double fraction) {
        // For Monte Carlo, fraction is a correlation coefficient (-1 to 1)
        // For sweeps, it's a ± percentage
        double pct = Math.abs(fraction) * 100.0;
        if (pct >= 1.0) {
            return String.format(Locale.US, "\u00B1%.0f%%", pct);
        }
        return String.format(Locale.US, "\u00B1%.1f%%", pct);
    }
}
