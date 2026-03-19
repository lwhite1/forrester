package systems.courant.sd.app.canvas;

import com.opencsv.CSVWriter;

import systems.courant.sd.sweep.RunResult;

import java.io.File;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

/**
 * Shared CSV export utilities that eliminate duplication across result pane classes.
 *
 * <p>Each method writes a specific data shape to a {@link File} and throws {@link IOException}
 * on failure, making them compatible with {@link ChartUtils#showCsvSaveDialog}.
 */
public final class CsvExportHelper {

    private CsvExportHelper() {
    }

    /**
     * Writes a {@link RunResult} to CSV with columns for Step, all stocks, and all variables.
     * Used by both optimization and calibration result panes to export the best-run time series.
     *
     * @param file the target CSV file
     * @param run  the run result to export
     * @throws IOException if writing fails
     */
    public static void writeRunResult(File file, RunResult run) throws IOException {
        List<String> stockNames = run.getStockNames();
        List<String> varNames = run.getVariableNames();

        try (CSVWriter writer = new CSVWriter(new OutputStreamWriter(
                Files.newOutputStream(file.toPath()), StandardCharsets.UTF_8))) {
            List<String> header = new ArrayList<>();
            header.add("Step");
            header.addAll(stockNames);
            header.addAll(varNames);
            writer.writeNext(header.toArray(new String[0]));

            for (int s = 0; s < run.getStepCount(); s++) {
                List<String> row = new ArrayList<>();
                row.add(String.valueOf(run.getStep(s)));
                for (double v : run.getStockValuesAtStep(s)) {
                    row.add(String.valueOf(v));
                }
                for (double v : run.getVariableValuesAtStep(s)) {
                    row.add(String.valueOf(v));
                }
                writer.writeNext(row.toArray(new String[0]));
            }
        }
    }
}
