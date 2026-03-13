package systems.courant.sd.sweep;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Static factory methods for common {@link ObjectiveFunction} implementations.
 * All objectives follow the convention that lower values are better (minimization).
 */
public final class Objectives {

    private static final Logger log = LoggerFactory.getLogger(Objectives.class);

    private Objectives() {
    }

    /**
     * Returns an objective that minimizes the sum of squared errors between the simulated
     * stock time series and the observed data.
     *
     * @param stockName    the name of the stock to compare
     * @param observedData the observed values (one per simulation step)
     * @return an objective function computing SSE
     */
    public static ObjectiveFunction fitToTimeSeries(String stockName, double[] observedData) {
        return runResult -> {
            double[] simulated = runResult.getStockSeries(stockName);
            if (simulated.length != observedData.length) {
                log.warn("fitToTimeSeries: simulated length ({}) differs from observed length ({}); "
                        + "comparing only the first {} steps",
                        simulated.length, observedData.length,
                        Math.min(simulated.length, observedData.length));
            }
            int length = Math.min(simulated.length, observedData.length);
            double sse = 0.0;
            for (int i = 0; i < length; i++) {
                double diff = simulated[i] - observedData[i];
                sse += diff * diff;
            }
            return sse;
        };
    }

    /**
     * Returns an objective that minimizes the final value of the named stock.
     *
     * @param stockName the stock to minimize
     * @return an objective function returning the final stock value
     */
    public static ObjectiveFunction minimize(String stockName) {
        return runResult -> runResult.getFinalStockValue(stockName);
    }

    /**
     * Returns an objective that maximizes the final value of the named stock.
     * Implemented by negating the final value (since the optimizer minimizes).
     *
     * @param stockName the stock to maximize
     * @return an objective function returning the negated final stock value
     */
    public static ObjectiveFunction maximize(String stockName) {
        return runResult -> -runResult.getFinalStockValue(stockName);
    }

    /**
     * Returns an objective that drives the final stock value toward a target.
     * Computes {@code (final - target)^2}.
     *
     * @param stockName   the stock to target
     * @param targetValue the desired final value
     * @return an objective function returning the squared deviation from target
     */
    public static ObjectiveFunction target(String stockName, double targetValue) {
        return runResult -> {
            double diff = runResult.getFinalStockValue(stockName) - targetValue;
            return diff * diff;
        };
    }

    /**
     * Returns an objective that minimizes the peak (maximum) value of the named stock
     * across all simulation steps.
     *
     * @param stockName the stock whose peak to minimize
     * @return an objective function returning the max stock value
     */
    public static ObjectiveFunction minimizePeak(String stockName) {
        return runResult -> runResult.getMaxStockValue(stockName);
    }
}
