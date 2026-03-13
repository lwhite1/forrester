package systems.courant.sd.sweep;

import org.apache.commons.math3.stat.correlation.SpearmansCorrelation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Computes a ranked sensitivity summary from sweep or Monte Carlo results,
 * identifying which parameters have the greatest impact on a target variable.
 *
 * <p>Impact is expressed as <em>proportion of variance explained</em> (0 to 1),
 * so values are always bounded and sum to approximately 1.0 across all parameters.
 */
public final class SensitivitySummary {

    private SensitivitySummary() {
    }

    /**
     * A single parameter's impact on a target variable.
     *
     * @param parameterName  the parameter that was varied
     * @param targetVariable the output variable being measured
     * @param minOutput      the minimum output value observed across runs
     * @param maxOutput      the maximum output value observed across runs
     * @param baselineOutput the mean output value across runs
     * @param impactFraction proportion of output variance explained by this parameter (0 to 1)
     */
    public record ParameterImpact(
            String parameterName,
            String targetVariable,
            double minOutput,
            double maxOutput,
            double baselineOutput,
            double impactFraction) implements Comparable<ParameterImpact> {

        public ParameterImpact {
            if (Double.isNaN(impactFraction) || impactFraction < 0.0) {
                impactFraction = 0.0;
            } else if (impactFraction > 1.0) {
                impactFraction = 1.0;
            }
        }

        @Override
        public int compareTo(ParameterImpact other) {
            return Double.compare(other.impactFraction, this.impactFraction);
        }
    }

    /**
     * Computes sensitivity from a single-parameter sweep. Since only one parameter
     * is varied, it explains 100% of the output variance by definition.
     *
     * @param result         the sweep result
     * @param targetVariable the stock or variable name to measure
     * @return a list with a single {@link ParameterImpact} entry (impact = 1.0)
     */
    public static List<ParameterImpact> fromSweep(SweepResult result, String targetVariable) {
        if (result.getRunCount() == 0) {
            return Collections.emptyList();
        }

        double[] finalValues = extractFinalValues(result.getResults(), targetVariable);
        double[] minMax = minMax(finalValues);
        double baseline = mean(finalValues);

        return List.of(new ParameterImpact(
                result.getParameterName(), targetVariable,
                minMax[0], minMax[1], baseline, 1.0));
    }

    /**
     * Computes sensitivity from a multi-parameter sweep using variance decomposition.
     * For each parameter, holds others at their median values and measures the output
     * variance. Results are normalized so impact fractions sum to 1.0.
     *
     * @param result         the multi-sweep result
     * @param targetVariable the stock or variable name to measure
     * @return a sorted list of {@link ParameterImpact} entries, most impactful first
     */
    public static List<ParameterImpact> fromMultiSweep(MultiSweepResult result, String targetVariable) {
        if (result.getRunCount() == 0) {
            return Collections.emptyList();
        }

        List<String> paramNames = result.getParameterNames();
        List<RunResult> runs = result.getResults();

        // Collect unique sorted values for each parameter
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

        // For each parameter, compute variance of output when only it varies
        double[] variances = new double[paramNames.size()];
        double[][] paramFinalValues = new double[paramNames.size()][];
        for (int p = 0; p < paramNames.size(); p++) {
            int paramIndex = p;

            List<RunResult> filtered = new ArrayList<>();
            for (RunResult run : runs) {
                Map<String, Double> map = run.getParameterMap();
                boolean othersAtMedian = true;
                for (int j = 0; j < paramNames.size(); j++) {
                    if (j != paramIndex) {
                        if (Double.compare(map.get(paramNames.get(j)), medians[j]) != 0) {
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
                variances[p] = 0;
                paramFinalValues[p] = new double[0];
                continue;
            }

            paramFinalValues[p] = extractFinalValues(filtered, targetVariable);
            variances[p] = variance(paramFinalValues[p]);
        }

        // Normalize variances to sum to 1.0
        double totalVariance = 0;
        for (double v : variances) {
            totalVariance += v;
        }

        List<ParameterImpact> impacts = new ArrayList<>();
        for (int p = 0; p < paramNames.size(); p++) {
            double fraction = totalVariance > 0 ? variances[p] / totalVariance : 0.0;
            double[] vals = paramFinalValues[p];
            double[] minMax = vals.length > 0 ? minMax(vals) : new double[]{0, 0};
            double baseline = vals.length > 0 ? mean(vals) : 0;

            impacts.add(new ParameterImpact(
                    paramNames.get(p), targetVariable,
                    minMax[0], minMax[1], baseline, fraction));
        }

        Collections.sort(impacts);
        return Collections.unmodifiableList(impacts);
    }

    /**
     * Computes sensitivity from Monte Carlo results using squared Spearman rank
     * correlation (ρ²) between each input parameter and the final value of the
     * target variable. Results are normalized so impact fractions sum to 1.0.
     *
     * @param result         the Monte Carlo result
     * @param targetVariable the stock or variable name to measure
     * @return a sorted list of {@link ParameterImpact} entries, most impactful first
     */
    public static List<ParameterImpact> fromMonteCarlo(MonteCarloResult result, String targetVariable) {
        List<RunResult> runs = result.getResults();
        if (runs.isEmpty()) {
            return Collections.emptyList();
        }

        Map<String, Double> firstMap = runs.getFirst().getParameterMap();
        if (firstMap.isEmpty()) {
            return Collections.emptyList();
        }

        List<String> paramNames = new ArrayList<>(firstMap.keySet());
        double[] finalValues = extractFinalValues(runs, targetVariable);
        double[] minMax = minMax(finalValues);
        double baseline = mean(finalValues);
        int n = runs.size();

        // Filter out runs with NaN final values (truncated/empty runs)
        List<Integer> validIndices = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            if (!Double.isNaN(finalValues[i])) {
                validIndices.add(i);
            }
        }
        double[] validFinals = new double[validIndices.size()];
        for (int i = 0; i < validIndices.size(); i++) {
            validFinals[i] = finalValues[validIndices.get(i)];
        }

        SpearmansCorrelation spearman = new SpearmansCorrelation();
        double[] rhoSquared = new double[paramNames.size()];

        if (validIndices.size() >= 3) {
            for (int p = 0; p < paramNames.size(); p++) {
                double[] paramValues = new double[validIndices.size()];
                for (int i = 0; i < validIndices.size(); i++) {
                    paramValues[i] = runs.get(validIndices.get(i))
                            .getParameterMap().get(paramNames.get(p));
                }

                double rho = spearman.correlation(paramValues, validFinals);
                rhoSquared[p] = Double.isNaN(rho) ? 0.0 : rho * rho;
            }
        }

        // Normalize ρ² values to sum to 1.0
        double totalRhoSq = 0;
        for (double rs : rhoSquared) {
            totalRhoSq += rs;
        }

        List<ParameterImpact> impacts = new ArrayList<>();
        for (int p = 0; p < paramNames.size(); p++) {
            double fraction = totalRhoSq > 0 ? rhoSquared[p] / totalRhoSq : 0.0;
            impacts.add(new ParameterImpact(
                    paramNames.get(p), targetVariable,
                    minMax[0], minMax[1], baseline, fraction));
        }

        Collections.sort(impacts);
        return Collections.unmodifiableList(impacts);
    }

    /**
     * Generates a plain-language sensitivity summary from a ranked list of impacts.
     */
    public static String toPlainLanguage(List<ParameterImpact> impacts) {
        if (impacts.isEmpty()) {
            return "No sensitivity data available.";
        }

        if (impacts.size() == 1) {
            ParameterImpact only = impacts.getFirst();
            return String.format(Locale.US, "%s varies from %s to %s across the sweep of %s.",
                    only.targetVariable(),
                    formatValue(only.minOutput()),
                    formatValue(only.maxOutput()),
                    only.parameterName());
        }

        ParameterImpact first = impacts.getFirst();
        StringBuilder sb = new StringBuilder();
        sb.append(first.targetVariable())
                .append(" is most sensitive to ")
                .append(first.parameterName())
                .append(" (")
                .append(formatPercent(first.impactFraction()))
                .append(" of variance)");

        for (int i = 1; i < impacts.size(); i++) {
            ParameterImpact impact = impacts.get(i);
            if (i == impacts.size() - 1 && impacts.size() > 2) {
                sb.append(", and ");
            } else {
                sb.append(", followed by ");
            }
            sb.append(impact.parameterName())
                    .append(" (")
                    .append(formatPercent(impact.impactFraction()))
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
            if (run.getStepCount() == 0) {
                values[i] = Double.NaN;
                continue;
            }
            int lastStep = run.getStepCount() - 1;
            if (isStock) {
                double[] snapshot = run.getStockValuesAtStep(lastStep);
                if (colIndex >= snapshot.length) {
                    values[i] = Double.NaN;
                } else {
                    values[i] = snapshot[colIndex];
                }
            } else {
                double[] snapshot = run.getVariableValuesAtStep(lastStep);
                if (colIndex >= snapshot.length) {
                    values[i] = Double.NaN;
                } else {
                    values[i] = snapshot[colIndex];
                }
            }
        }
        return values;
    }

    private static double[] minMax(double[] values) {
        double min = Double.MAX_VALUE;
        double max = -Double.MAX_VALUE;
        for (double v : values) {
            if (Double.isNaN(v)) { continue; }
            if (v < min) { min = v; }
            if (v > max) { max = v; }
        }
        if (min == Double.MAX_VALUE) {
            return new double[]{0, 0};
        }
        return new double[]{min, max};
    }

    private static double mean(double[] values) {
        double sum = 0;
        int count = 0;
        for (double v : values) {
            if (Double.isNaN(v)) { continue; }
            sum += v;
            count++;
        }
        return count > 0 ? sum / count : 0;
    }

    private static double variance(double[] values) {
        double m = mean(values);
        double sumSq = 0;
        int count = 0;
        for (double v : values) {
            if (Double.isNaN(v)) { continue; }
            double diff = v - m;
            sumSq += diff * diff;
            count++;
        }
        return count > 0 ? sumSq / count : 0;
    }

    private static String formatPercent(double fraction) {
        if (fraction <= 0.0) {
            return "0%";
        }
        if (fraction >= 1.0) {
            return "100%";
        }
        double pct = fraction * 100.0;
        if (pct >= 1.0) {
            return String.format(Locale.US, "%.0f%%", pct);
        }
        if (pct >= 0.1) {
            return String.format(Locale.US, "%.1f%%", pct);
        }
        return "<0.1%";
    }

    private static String formatValue(double value) {
        if (value == Math.floor(value) && Double.isFinite(value)
                && Math.abs(value) <= Long.MAX_VALUE) {
            return String.valueOf((long) value);
        }
        return String.format(Locale.US, "%.2f", value);
    }
}
