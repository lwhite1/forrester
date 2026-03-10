package systems.courant.forrester.sweep;

import java.util.Collections;
import java.util.List;

/**
 * Aggregates the results of a parameter sweep: one {@link RunResult} per parameter value,
 * plus the parameter name. Provides convenience methods for CSV export and result access.
 */
public class SweepResult {

    private final String parameterName;
    private final List<RunResult> results;

    /**
     * Creates a new sweep result.
     *
     * @param parameterName the name of the swept parameter
     * @param results       the list of run results, one per parameter value
     */
    public SweepResult(String parameterName, List<RunResult> results) {
        this.parameterName = parameterName;
        this.results = results;
    }

    /**
     * Returns the name of the swept parameter.
     *
     * @return the parameter name
     */
    public String getParameterName() {
        return parameterName;
    }

    /**
     * Returns the number of simulation runs (one per parameter value).
     *
     * @return the run count
     */
    public int getRunCount() {
        return results.size();
    }

    /**
     * Returns the run result at the given index.
     *
     * @param index the zero-based index
     * @return the run result for the corresponding parameter value
     */
    public RunResult getResult(int index) {
        return results.get(index);
    }

    /**
     * Returns all run results.
     *
     * @return an unmodifiable list of run results, one per parameter value
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
