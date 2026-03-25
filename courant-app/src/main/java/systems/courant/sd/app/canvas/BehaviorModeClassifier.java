package systems.courant.sd.app.canvas;

import systems.courant.sd.model.graph.BehaviorClassification;

/**
 * Heuristic classifier that detects the dominant behavior mode of a time series.
 * Delegates to {@link BehaviorClassification} for the actual classification and
 * returns the mode's display name as a string.
 */
public final class BehaviorModeClassifier {

    private BehaviorModeClassifier() {
    }

    /**
     * Classifies a time series into a behavior mode label.
     *
     * @param values the time-series values (at least 4 data points required)
     * @return a short label describing the behavior, or empty string if unclassifiable
     */
    public static String classify(double[] values) {
        if (values == null || values.length < 4) {
            return "";
        }
        if (containsNonFinite(values)) {
            return "";
        }
        return BehaviorClassification.classify(values).displayName();
    }

    private static boolean containsNonFinite(double[] values) {
        for (double v : values) {
            if (!Double.isFinite(v)) {
                return true;
            }
        }
        return false;
    }
}
