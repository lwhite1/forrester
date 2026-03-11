package systems.courant.shrewd.sweep;

import com.opencsv.CSVWriter;

import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Aggregates the results of a Monte Carlo simulation and computes percentile envelopes
 * across runs at each timestep. Provides methods for extracting percentile and mean series,
 * as well as CSV export.
 */
public class MonteCarloResult {

    private static final Logger logger = LoggerFactory.getLogger(MonteCarloResult.class);

    private final List<RunResult> results;

    /**
     * Creates a new Monte Carlo result from the given list of run results.
     *
     * @param results the list of run results, one per iteration
     */
    public MonteCarloResult(List<RunResult> results) {
        this.results = results;
    }

    /**
     * Returns the number of simulation runs.
     */
    public int getRunCount() {
        return results.size();
    }

    /**
     * Returns the number of timesteps in each run.
     */
    public int getStepCount() {
        if (results.isEmpty()) {
            return 0;
        }
        return results.get(0).getStepCount();
    }

    /**
     * Returns the stock names from the first run result.
     */
    public List<String> getStockNames() {
        if (results.isEmpty()) {
            return Collections.emptyList();
        }
        return results.get(0).getStockNames();
    }

    /**
     * Returns the variable names from the first run result.
     */
    public List<String> getVariableNames() {
        if (results.isEmpty()) {
            return Collections.emptyList();
        }
        return results.get(0).getVariableNames();
    }

    /**
     * Returns the individual run results.
     */
    public List<RunResult> getResults() {
        return Collections.unmodifiableList(results);
    }

    /**
     * Computes the percentile series for a stock or variable across all runs at each timestep.
     *
     * @param name       the stock or variable name
     * @param percentile the percentile to compute (0-100, e.g. 50 for median)
     * @return an array with one value per timestep
     * @throws IllegalArgumentException if the name is not found
     */
    public double[] getPercentileSeries(String name, double percentile) {
        int stepCount = getStepCount();
        double[] series = new double[stepCount];
        boolean isStock = isStockName(name);
        int columnIndex = getColumnIndex(name, isStock);

        for (int step = 0; step < stepCount; step++) {
            DescriptiveStatistics stats = new DescriptiveStatistics();
            for (RunResult run : results) {
                double value;
                if (isStock) {
                    value = run.getStockValuesAtStep(step)[columnIndex];
                } else {
                    value = run.getVariableValuesAtStep(step)[columnIndex];
                }
                stats.addValue(value);
            }
            series[step] = stats.getPercentile(percentile);
        }

        return series;
    }

    /**
     * Computes the mean series for a stock or variable across all runs at each timestep.
     *
     * @param name the stock or variable name
     * @return an array with one value per timestep
     * @throws IllegalArgumentException if the name is not found
     */
    public double[] getMeanSeries(String name) {
        int stepCount = getStepCount();
        double[] series = new double[stepCount];
        boolean isStock = isStockName(name);
        int columnIndex = getColumnIndex(name, isStock);

        for (int step = 0; step < stepCount; step++) {
            double sum = 0;
            for (RunResult run : results) {
                double value;
                if (isStock) {
                    value = run.getStockValuesAtStep(step)[columnIndex];
                } else {
                    value = run.getVariableValuesAtStep(step)[columnIndex];
                }
                sum += value;
            }
            series[step] = sum / results.size();
        }

        return series;
    }

    /**
     * Writes percentile data for a stock or variable to a CSV file.
     *
     * @param filePath    the output file path
     * @param name        the stock or variable name
     * @param percentiles the percentiles to include (e.g. 2.5, 25, 50, 75, 97.5)
     */
    public void writePercentileCsv(String filePath, String name, double... percentiles) {
        ensureParentDir(filePath);

        // Pre-compute all percentile series
        double[][] seriesData = new double[percentiles.length][];
        for (int p = 0; p < percentiles.length; p++) {
            seriesData[p] = getPercentileSeries(name, percentiles[p]);
        }

        try (CSVWriter writer = new CSVWriter(new FileWriter(filePath, java.nio.charset.StandardCharsets.UTF_8))) {
            // Header
            List<String> header = new ArrayList<>();
            header.add("Step");
            for (double pct : percentiles) {
                header.add("P" + formatPercentile(pct));
            }
            writer.writeNext(header.toArray(new String[0]));

            // Data rows
            int stepCount = getStepCount();
            for (int step = 0; step < stepCount; step++) {
                List<String> row = new ArrayList<>();
                row.add(String.valueOf(step));
                for (int p = 0; p < percentiles.length; p++) {
                    row.add(String.valueOf(seriesData[p][step]));
                }
                writer.writeNext(row.toArray(new String[0]));
            }

            writer.flush();
            logger.info("Wrote percentile CSV to {}", filePath);
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to write percentile CSV: " + filePath, e);
        }
    }

    private boolean isStockName(String name) {
        return getStockNames().contains(name);
    }

    private int getColumnIndex(String name, boolean isStock) {
        List<String> names = isStock ? getStockNames() : getVariableNames();
        int index = names.indexOf(name);
        if (index < 0) {
            throw new IllegalArgumentException("Unknown stock or variable: " + name);
        }
        return index;
    }

    private String formatPercentile(double percentile) {
        if (percentile == Math.floor(percentile)) {
            return String.valueOf((int) percentile);
        }
        return String.valueOf(percentile);
    }

    private void ensureParentDir(String filePath) {
        File parent = Paths.get(filePath).toFile().getParentFile();
        if (parent != null && !parent.mkdirs() && !parent.isDirectory()) {
            throw new UncheckedIOException(new IOException(
                    "Failed to create directory: " + parent.getAbsolutePath()));
        }
    }
}
