package systems.courant.shrewd.sweep;

import com.opencsv.CSVWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Static utility methods for writing {@link SweepResult} data to CSV files using OpenCSV.
 */
public final class SweepCsvWriter {

    private static final Logger logger = LoggerFactory.getLogger(SweepCsvWriter.class);

    private SweepCsvWriter() {
        // utility class
    }

    /**
     * Writes the full time series for all runs to a CSV file. Each row contains the parameter
     * value, step number, and all stock and variable values at that step.
     *
     * @param sweepResult the sweep result to write
     * @param filePath    the output file path
     */
    public static void writeTimeSeries(SweepResult sweepResult, String filePath) {
        ensureParentDir(filePath);

        try (CSVWriter writer = new CSVWriter(new OutputStreamWriter(
                    Files.newOutputStream(Paths.get(filePath)), StandardCharsets.UTF_8))) {
            // Header
            List<String> header = new ArrayList<>();
            header.add(sweepResult.getParameterName());
            header.add("Step");
            header.addAll(sweepResult.getStockNames());
            header.addAll(sweepResult.getVariableNames());
            writer.writeNext(header.toArray(new String[0]));

            // Data rows
            for (RunResult run : sweepResult.getResults()) {
                String paramVal = String.valueOf(run.getParameterValue());
                for (int i = 0; i < run.getStepCount(); i++) {
                    List<String> row = new ArrayList<>();
                    row.add(paramVal);
                    row.add(String.valueOf(run.getStep(i)));
                    for (double v : run.getStockValuesAtStep(i)) {
                        row.add(String.valueOf(v));
                    }
                    for (double v : run.getVariableValuesAtStep(i)) {
                        row.add(String.valueOf(v));
                    }
                    writer.writeNext(row.toArray(new String[0]));
                }
            }

            writer.flush();
            logger.info("Wrote time series CSV to {}", filePath);
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to write time series CSV: " + filePath, e);
        }
    }

    /**
     * Writes a summary CSV with one row per run. Each row contains the parameter value
     * and the final and maximum values for each stock.
     *
     * @param sweepResult the sweep result to write
     * @param filePath    the output file path
     */
    public static void writeSummary(SweepResult sweepResult, String filePath) {
        ensureParentDir(filePath);

        try (CSVWriter writer = new CSVWriter(new OutputStreamWriter(
                    Files.newOutputStream(Paths.get(filePath)), StandardCharsets.UTF_8))) {
            // Header
            List<String> header = new ArrayList<>();
            header.add(sweepResult.getParameterName());
            for (String stockName : sweepResult.getStockNames()) {
                header.add(stockName + "_final");
                header.add(stockName + "_max");
            }
            writer.writeNext(header.toArray(new String[0]));

            // Data rows
            for (RunResult run : sweepResult.getResults()) {
                List<String> row = new ArrayList<>();
                row.add(String.valueOf(run.getParameterValue()));
                for (String stockName : run.getStockNames()) {
                    row.add(String.valueOf(run.getFinalStockValue(stockName)));
                    row.add(String.valueOf(run.getMaxStockValue(stockName)));
                }
                writer.writeNext(row.toArray(new String[0]));
            }

            writer.flush();
            logger.info("Wrote summary CSV to {}", filePath);
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to write summary CSV: " + filePath, e);
        }
    }

    /**
     * Writes the full time series for all runs of a multi-parameter sweep to a CSV file.
     * Each row contains the parameter values, step number, and all stock and variable values.
     *
     * @param result   the multi-sweep result to write
     * @param filePath the output file path
     */
    public static void writeTimeSeries(MultiSweepResult result, String filePath) {
        ensureParentDir(filePath);

        try (CSVWriter writer = new CSVWriter(new OutputStreamWriter(
                    Files.newOutputStream(Paths.get(filePath)), StandardCharsets.UTF_8))) {
            // Header
            List<String> header = new ArrayList<>();
            header.addAll(result.getParameterNames());
            header.add("Step");
            header.addAll(result.getStockNames());
            header.addAll(result.getVariableNames());
            writer.writeNext(header.toArray(new String[0]));

            // Data rows
            List<String> paramNames = result.getParameterNames();
            for (RunResult run : result.getResults()) {
                Map<String, Double> paramMap = run.getParameterMap();
                for (int i = 0; i < run.getStepCount(); i++) {
                    List<String> row = new ArrayList<>();
                    for (String name : paramNames) {
                        row.add(String.valueOf(paramMap.get(name)));
                    }
                    row.add(String.valueOf(run.getStep(i)));
                    for (double v : run.getStockValuesAtStep(i)) {
                        row.add(String.valueOf(v));
                    }
                    for (double v : run.getVariableValuesAtStep(i)) {
                        row.add(String.valueOf(v));
                    }
                    writer.writeNext(row.toArray(new String[0]));
                }
            }

            writer.flush();
            logger.info("Wrote multi-sweep time series CSV to {}", filePath);
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to write time series CSV: " + filePath, e);
        }
    }

    /**
     * Writes a summary CSV for a multi-parameter sweep with one row per parameter combination.
     * Each row contains the parameter values and the final and maximum values for each stock.
     *
     * @param result   the multi-sweep result to write
     * @param filePath the output file path
     */
    public static void writeSummary(MultiSweepResult result, String filePath) {
        ensureParentDir(filePath);

        try (CSVWriter writer = new CSVWriter(new OutputStreamWriter(
                    Files.newOutputStream(Paths.get(filePath)), StandardCharsets.UTF_8))) {
            // Header
            List<String> header = new ArrayList<>();
            header.addAll(result.getParameterNames());
            for (String stockName : result.getStockNames()) {
                header.add(stockName + "_final");
                header.add(stockName + "_max");
            }
            writer.writeNext(header.toArray(new String[0]));

            // Data rows
            List<String> paramNames = result.getParameterNames();
            for (RunResult run : result.getResults()) {
                List<String> row = new ArrayList<>();
                Map<String, Double> paramMap = run.getParameterMap();
                for (String name : paramNames) {
                    row.add(String.valueOf(paramMap.get(name)));
                }
                for (String stockName : run.getStockNames()) {
                    row.add(String.valueOf(run.getFinalStockValue(stockName)));
                    row.add(String.valueOf(run.getMaxStockValue(stockName)));
                }
                writer.writeNext(row.toArray(new String[0]));
            }

            writer.flush();
            logger.info("Wrote multi-sweep summary CSV to {}", filePath);
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to write summary CSV: " + filePath, e);
        }
    }

    private static void ensureParentDir(String filePath) {
        File parent = Paths.get(filePath).toFile().getParentFile();
        if (parent != null && !parent.mkdirs() && !parent.isDirectory()) {
            throw new UncheckedIOException(new IOException(
                    "Failed to create directory: " + parent.getAbsolutePath()));
        }
    }
}
