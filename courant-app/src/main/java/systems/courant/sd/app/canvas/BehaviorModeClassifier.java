package systems.courant.sd.app.canvas;

/**
 * Heuristic classifier that detects the dominant behavior mode of a time series.
 * Recognizes the canonical system dynamics behavior modes: exponential growth,
 * exponential decay, goal-seeking, linear growth/decline, oscillation,
 * S-shaped growth, and overshoot-and-collapse.
 */
final class BehaviorModeClassifier {

    private BehaviorModeClassifier() {
    }

    /**
     * Classifies a time series into a behavior mode label.
     *
     * @param values the time-series values (at least 4 data points required)
     * @return a short label describing the behavior, or empty string if unclassifiable
     */
    static String classify(double[] values) {
        if (values.length < 4) {
            return "";
        }

        if (isConstant(values)) {
            return "Equilibrium";
        }

        if (isOscillating(values)) {
            return "Oscillation";
        }

        boolean monotoneUp = isMonotoneIncreasing(values);
        boolean monotoneDown = isMonotoneDecreasing(values);

        if (monotoneUp) {
            if (isSShape(values)) {
                return "S-shaped growth";
            }
            if (isConvex(values)) {
                return "Exponential growth";
            }
            if (isConcave(values)) {
                return "Goal-seeking";
            }
            return "Linear growth";
        }

        if (monotoneDown) {
            if (isConvex(values)) {
                // Convex + decreasing: either exponential decay (draining toward zero)
                // or goal-seeking (approaching a nonzero goal from above).
                // If the value at the end is near zero relative to the start, it's decay.
                double first = values[0];
                double last = values[values.length - 1];
                if (first != 0 && last / first < 0.1) {
                    return "Exponential decay";
                }
                return "Goal-seeking";
            }
            if (isConcave(values)) {
                return "Accelerating decline";
            }
            return "Linear decline";
        }

        if (isOvershootAndCollapse(values)) {
            return "Overshoot & collapse";
        }

        return "";
    }

    private static boolean isConstant(double[] values) {
        double range = range(values);
        double scale = Math.max(Math.abs(values[0]), Math.abs(values[values.length - 1]));
        if (scale == 0) {
            return range < 1e-10;
        }
        return range / scale < 0.001;
    }

    private static boolean isMonotoneIncreasing(double[] values) {
        int violations = 0;
        for (int i = 1; i < values.length; i++) {
            if (values[i] < values[i - 1] - 1e-10) {
                violations++;
            }
        }
        return violations <= values.length / 20; // allow 5% noise
    }

    private static boolean isMonotoneDecreasing(double[] values) {
        int violations = 0;
        for (int i = 1; i < values.length; i++) {
            if (values[i] > values[i - 1] + 1e-10) {
                violations++;
            }
        }
        return violations <= values.length / 20;
    }

    private static boolean isConvex(double[] values) {
        int n = values.length;
        int convex = 0;
        int concave = 0;
        for (int i = 1; i < n - 1; i++) {
            double secondDeriv = values[i + 1] - 2 * values[i] + values[i - 1];
            if (secondDeriv > 1e-10) {
                convex++;
            } else if (secondDeriv < -1e-10) {
                concave++;
            }
        }
        return convex > concave * 2 && convex > n / 4;
    }

    private static boolean isConcave(double[] values) {
        int n = values.length;
        int convex = 0;
        int concave = 0;
        for (int i = 1; i < n - 1; i++) {
            double secondDeriv = values[i + 1] - 2 * values[i] + values[i - 1];
            if (secondDeriv > 1e-10) {
                convex++;
            } else if (secondDeriv < -1e-10) {
                concave++;
            }
        }
        return concave > convex * 2 && concave > n / 4;
    }

    private static boolean isSShape(double[] values) {
        // S-shaped: first half convex (accelerating), second half concave (decelerating)
        int n = values.length;
        int mid = n / 2;
        int firstHalfConvex = 0;
        int firstHalfConcave = 0;
        for (int i = 1; i < mid && i < n - 1; i++) {
            double d2 = values[i + 1] - 2 * values[i] + values[i - 1];
            if (d2 > 1e-10) {
                firstHalfConvex++;
            } else if (d2 < -1e-10) {
                firstHalfConcave++;
            }
        }
        int secondHalfConvex = 0;
        int secondHalfConcave = 0;
        for (int i = mid; i < n - 1; i++) {
            double d2 = values[i + 1] - 2 * values[i] + values[i - 1];
            if (d2 > 1e-10) {
                secondHalfConvex++;
            } else if (d2 < -1e-10) {
                secondHalfConcave++;
            }
        }
        return firstHalfConvex > firstHalfConcave && secondHalfConcave > secondHalfConvex;
    }

    private static boolean isOscillating(double[] values) {
        // Count direction changes (peaks and troughs)
        int directionChanges = 0;
        int direction = 0; // 0=unknown, 1=up, -1=down
        for (int i = 1; i < values.length; i++) {
            double diff = values[i] - values[i - 1];
            if (Math.abs(diff) < 1e-10) {
                continue;
            }
            int newDir = diff > 0 ? 1 : -1;
            if (direction != 0 && newDir != direction) {
                directionChanges++;
            }
            direction = newDir;
        }
        // Need at least 3 direction changes for oscillation (e.g., up-down-up-down)
        return directionChanges >= 3;
    }

    private static boolean isOvershootAndCollapse(double[] values) {
        // Rises to a peak then falls significantly
        int peakIndex = 0;
        double peakValue = values[0];
        for (int i = 1; i < values.length; i++) {
            if (values[i] > peakValue) {
                peakValue = values[i];
                peakIndex = i;
            }
        }
        // Peak should be in roughly the first 2/3 of the series
        if (peakIndex < 1 || peakIndex > values.length * 2 / 3) {
            return false;
        }
        // After the peak, the value should drop significantly
        double lastValue = values[values.length - 1];
        double rise = peakValue - values[0];
        double fall = peakValue - lastValue;
        return rise > 0 && fall > rise * 0.3;
    }

    private static double range(double[] values) {
        double min = values[0];
        double max = values[0];
        for (double v : values) {
            if (v < min) {
                min = v;
            }
            if (v > max) {
                max = v;
            }
        }
        return max - min;
    }
}
