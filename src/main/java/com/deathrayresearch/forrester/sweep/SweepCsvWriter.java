package com.deathrayresearch.forrester.sweep;

import com.opencsv.CSVWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

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

        try (CSVWriter writer = new CSVWriter(new FileWriter(filePath))) {
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
            throw new RuntimeException("Failed to write time series CSV: " + filePath, e);
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

        try (CSVWriter writer = new CSVWriter(new FileWriter(filePath))) {
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
            throw new RuntimeException("Failed to write summary CSV: " + filePath, e);
        }
    }

    private static void ensureParentDir(String filePath) {
        File parent = Paths.get(filePath).toFile().getParentFile();
        if (parent != null) {
            parent.mkdirs();
        }
    }
}
