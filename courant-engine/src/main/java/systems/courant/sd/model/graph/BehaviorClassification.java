package systems.courant.sd.model.graph;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Classifies the behavior mode of a time series using heuristic analysis of
 * derivatives, zero crossings, and inflection points.
 *
 * <p>Supported modes:
 * <ul>
 *   <li>{@link Mode#EXPONENTIAL_GROWTH} — monotonically increasing with accelerating rate</li>
 *   <li>{@link Mode#EXPONENTIAL_DECAY} — monotonically decreasing with decelerating rate</li>
 *   <li>{@link Mode#GOAL_SEEKING} — monotonically approaching an asymptote</li>
 *   <li>{@link Mode#OSCILLATION} — multiple direction changes around a mean</li>
 *   <li>{@link Mode#S_SHAPED_GROWTH} — accelerating then decelerating growth (inflection point)</li>
 *   <li>{@link Mode#OVERSHOOT_AND_COLLAPSE} — rises past equilibrium then falls</li>
 *   <li>{@link Mode#EQUILIBRIUM} — approximately constant</li>
 *   <li>{@link Mode#LINEAR_GROWTH} — monotonically increasing at constant rate</li>
 *   <li>{@link Mode#LINEAR_DECLINE} — monotonically decreasing at constant rate</li>
 * </ul>
 */
public final class BehaviorClassification {

    /** Minimum number of data points required for classification. */
    private static final int MIN_POINTS = 4;

    /** Fraction of total range below which a series is considered flat. */
    private static final double FLAT_THRESHOLD = 0.01;

    /** Fraction of derivative-sign changes relative to series length for oscillation. */
    private static final double OSCILLATION_THRESHOLD = 0.15;

    private BehaviorClassification() {
    }

    /**
     * Behavior mode of a time series.
     */
    public enum Mode {
        EXPONENTIAL_GROWTH("Exponential Growth"),
        EXPONENTIAL_DECAY("Exponential Decay"),
        GOAL_SEEKING("Goal-Seeking"),
        OSCILLATION("Oscillation"),
        S_SHAPED_GROWTH("S-Shaped Growth"),
        OVERSHOOT_AND_COLLAPSE("Overshoot & Collapse"),
        EQUILIBRIUM("Equilibrium"),
        LINEAR_GROWTH("Linear Growth"),
        LINEAR_DECLINE("Linear Decline");

        private final String displayName;

        Mode(String displayName) {
            this.displayName = displayName;
        }

        public String displayName() {
            return displayName;
        }
    }

    /**
     * Classification result for a single variable.
     *
     * @param variableName the variable name
     * @param mode         the classified behavior mode
     */
    public record Result(String variableName, Mode mode) {
    }

    /**
     * Classifies the behavior mode of a single time series.
     *
     * @param values the time series data (at least 4 points for meaningful classification)
     * @return the detected behavior mode
     */
    public static Mode classify(double[] values) {
        if (values == null || values.length < MIN_POINTS) {
            return Mode.EQUILIBRIUM;
        }

        // Filter out non-finite values
        double[] clean = filterFinite(values);
        if (clean.length < MIN_POINTS) {
            return Mode.EQUILIBRIUM;
        }

        // Check for flat series
        double min = clean[0];
        double max = clean[0];
        for (double v : clean) {
            if (v < min) {
                min = v;
            }
            if (v > max) {
                max = v;
            }
        }
        double range = max - min;
        double scale = Math.max(Math.abs(min), Math.abs(max));
        if (scale > 0 && range / scale < FLAT_THRESHOLD) {
            return Mode.EQUILIBRIUM;
        }
        if (range == 0) {
            return Mode.EQUILIBRIUM;
        }

        // Compute first derivative (rate of change)
        double[] d1 = derivative(clean);
        // Compute second derivative (acceleration)
        double[] d2 = derivative(d1);

        // Count sign changes in first derivative (direction reversals)
        int signChanges = countSignChanges(d1);
        double signChangeRatio = (double) signChanges / d1.length;

        // Check for oscillation: multiple direction reversals
        if (signChanges >= 3 && signChangeRatio > OSCILLATION_THRESHOLD) {
            return Mode.OSCILLATION;
        }

        boolean monotoneIncreasing = isMonotone(d1, true);
        boolean monotoneDecreasing = isMonotone(d1, false);

        // Check for overshoot-and-collapse: rises then falls (single peak)
        if (signChanges == 1 && !monotoneIncreasing && !monotoneDecreasing) {
            int peakIdx = findPeakIndex(clean);
            boolean risesFirst = peakIdx > 0 && peakIdx < clean.length - 1;
            if (risesFirst && clean[peakIdx] > clean[0] && clean[peakIdx] > clean[clean.length - 1]) {
                return Mode.OVERSHOOT_AND_COLLAPSE;
            }
        }

        // Check for S-shaped growth: inflection point in accelerating-then-decelerating growth
        if (monotoneIncreasing || isWeaklyMonotone(d1, true)) {
            int inflections = countSignChanges(d2);
            if (inflections >= 1) {
                // Second derivative changes sign: acceleration → deceleration
                double d2FirstHalf = averageFirstHalf(d2);
                double d2SecondHalf = averageSecondHalf(d2);
                if (d2FirstHalf > 0 && d2SecondHalf < 0) {
                    return Mode.S_SHAPED_GROWTH;
                }
            }
        }

        // Monotone increasing
        if (monotoneIncreasing || isWeaklyMonotone(d1, true)) {
            double avgD2 = average(d2);
            double avgStep = range / clean.length;
            double d2Magnitude = avgStep > 0 ? Math.abs(avgD2) / avgStep : 0;
            if (d2Magnitude < 0.1) {
                return Mode.LINEAR_GROWTH;
            }
            if (avgD2 > 0) {
                return Mode.EXPONENTIAL_GROWTH;
            }
            return Mode.GOAL_SEEKING;
        }

        // Monotone decreasing
        if (monotoneDecreasing || isWeaklyMonotone(d1, false)) {
            double avgD2 = average(d2);
            double avgStep = range / clean.length;
            double d2Magnitude = avgStep > 0 ? Math.abs(avgD2) / avgStep : 0;
            if (d2Magnitude < 0.1) {
                return Mode.LINEAR_DECLINE;
            }
            if (avgD2 < 0) {
                return Mode.EXPONENTIAL_DECAY;
            }
            return Mode.GOAL_SEEKING;
        }

        // Fallback
        return Mode.GOAL_SEEKING;
    }

    /**
     * Classifies multiple named time series.
     *
     * @param stockNames  stock variable names
     * @param stockSeries corresponding time series data
     * @return unmodifiable map of variable name to classification result
     */
    public static Map<String, Result> classifyAll(List<String> stockNames,
                                                   List<double[]> stockSeries) {
        if (stockNames.size() != stockSeries.size()) {
            throw new IllegalArgumentException("Names and series lists must have the same size");
        }
        Map<String, Result> results = new LinkedHashMap<>();
        for (int i = 0; i < stockNames.size(); i++) {
            String name = stockNames.get(i);
            Mode mode = classify(stockSeries.get(i));
            results.put(name, new Result(name, mode));
        }
        return Collections.unmodifiableMap(results);
    }

    /**
     * Returns a list of classification results from a RunResult-like data source.
     *
     * @param names  the variable names
     * @param series the time series data for each variable
     * @return unmodifiable list of results
     */
    public static List<Result> classifyToList(List<String> names, List<double[]> series) {
        Map<String, Result> map = classifyAll(names, series);
        return Collections.unmodifiableList(new ArrayList<>(map.values()));
    }

    // ── Internal helpers ──────────────────────────────────────────────

    static double[] filterFinite(double[] values) {
        int count = 0;
        for (double v : values) {
            if (Double.isFinite(v)) {
                count++;
            }
        }
        if (count == values.length) {
            return values;
        }
        double[] result = new double[count];
        int idx = 0;
        for (double v : values) {
            if (Double.isFinite(v)) {
                result[idx++] = v;
            }
        }
        return result;
    }

    static double[] derivative(double[] values) {
        double[] d = new double[values.length - 1];
        for (int i = 0; i < d.length; i++) {
            d[i] = values[i + 1] - values[i];
        }
        return d;
    }

    static int countSignChanges(double[] values) {
        int changes = 0;
        int prevSign = 0;
        for (double v : values) {
            int sign = v > 0 ? 1 : (v < 0 ? -1 : 0);
            if (sign != 0 && prevSign != 0 && sign != prevSign) {
                changes++;
            }
            if (sign != 0) {
                prevSign = sign;
            }
        }
        return changes;
    }

    static boolean isMonotone(double[] values, boolean increasing) {
        for (double v : values) {
            if (increasing && v < 0) {
                return false;
            }
            if (!increasing && v > 0) {
                return false;
            }
        }
        return true;
    }

    /**
     * Checks weak monotonicity: allows a small fraction of violations.
     */
    static boolean isWeaklyMonotone(double[] values, boolean increasing) {
        int violations = 0;
        for (double v : values) {
            if (increasing && v < 0) {
                violations++;
            }
            if (!increasing && v > 0) {
                violations++;
            }
        }
        return violations <= Math.max(1, values.length / 10);
    }

    static int findPeakIndex(double[] values) {
        int peakIdx = 0;
        double peakVal = values[0];
        for (int i = 1; i < values.length; i++) {
            if (values[i] > peakVal) {
                peakVal = values[i];
                peakIdx = i;
            }
        }
        return peakIdx;
    }

    static double average(double[] values) {
        if (values.length == 0) {
            return 0;
        }
        double sum = 0;
        for (double v : values) {
            sum += v;
        }
        return sum / values.length;
    }

    static double averageFirstHalf(double[] values) {
        int mid = values.length / 2;
        if (mid == 0) {
            return 0;
        }
        double sum = 0;
        for (int i = 0; i < mid; i++) {
            sum += values[i];
        }
        return sum / mid;
    }

    static double averageSecondHalf(double[] values) {
        int mid = values.length / 2;
        int count = values.length - mid;
        if (count == 0) {
            return 0;
        }
        double sum = 0;
        for (int i = mid; i < values.length; i++) {
            sum += values[i];
        }
        return sum / count;
    }
}
