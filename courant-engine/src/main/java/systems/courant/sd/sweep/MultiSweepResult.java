package systems.courant.sd.sweep;

import java.util.Collections;
import java.util.List;

/**
 * Aggregates the results of a multi-parameter sweep: one {@link RunResult} per parameter
 * combination, plus the ordered list of parameter names. Provides convenience methods
 * for CSV export and result access.
 */
public class MultiSweepResult {

    private final List<String> parameterNames;
    private final List<RunResult> results;

    /**
     * Creates a new multi-sweep result.
     *
     * @param parameterNames the ordered list of swept parameter names
     * @param results        the list of run results, one per parameter combination
     */
    public MultiSweepResult(List<String> parameterNames, List<RunResult> results) {
        this.parameterNames = parameterNames;
        this.results = results;
    }

    /**
     * Returns the ordered list of parameter names.
     */
    public List<String> getParameterNames() {
        return Collections.unmodifiableList(parameterNames);
    }

    /**
     * Returns the number of simulation runs (one per parameter combination).
     */
    public int getRunCount() {
        return results.size();
    }

    /**
     * Returns the run result at the given index.
     */
    public RunResult getResult(int index) {
        return results.get(index);
    }

    /**
     * Returns all run results.
     */
    public List<RunResult> getResults() {
        return Collections.unmodifiableList(results);
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
     * Writes the full time series for all runs to a CSV file.
     *
     * @param filePath the output file path
     */
    public void writeTimeSeriesCsv(String filePath) {
        SweepCsvWriter.writeTimeSeries(this, filePath);
    }

    /**
     * Writes a summary row per run (final and peak stock values) to a CSV file.
     *
     * @param filePath the output file path
     */
    public void writeSummaryCsv(String filePath) {
        SweepCsvWriter.writeSummary(this, filePath);
    }
}
