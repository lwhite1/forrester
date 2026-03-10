package systems.courant.forrester.model.def;

import java.util.List;
import java.util.Map;

/**
 * An observed or expected time-series dataset used as a reference mode for
 * model validation and calibration. Reference data can be overlaid on
 * simulation results to visually compare model output against real-world
 * observations.
 *
 * <p>The time column provides the independent variable (matching simulation
 * steps or time values). Each entry in {@code columns} maps a variable name
 * to its observed values, one per time point. Column names should match
 * model variable names where possible for automatic alignment.
 *
 * @param name        display name for this dataset (e.g., "Historical Data 1990-2020")
 * @param timeValues  the time points (one per observation, must be non-empty)
 * @param columns     map from variable name to observed values (each array same length as timeValues)
 */
public record ReferenceDataset(String name, double[] timeValues, Map<String, double[]> columns) {

    public ReferenceDataset {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Reference dataset name must not be blank");
        }
        if (timeValues == null || timeValues.length == 0) {
            throw new IllegalArgumentException("Reference dataset must have at least one time value");
        }
        timeValues = timeValues.clone();
        if (columns == null || columns.isEmpty()) {
            throw new IllegalArgumentException("Reference dataset must have at least one data column");
        }
        // Defensive copy of all column arrays
        var copy = new java.util.LinkedHashMap<String, double[]>();
        for (var entry : columns.entrySet()) {
            if (entry.getValue().length != timeValues.length) {
                throw new IllegalArgumentException(
                        "Column '" + entry.getKey() + "' has " + entry.getValue().length
                                + " values but expected " + timeValues.length);
            }
            copy.put(entry.getKey(), entry.getValue().clone());
        }
        columns = Map.copyOf(copy);
    }

    /**
     * Returns the variable names in this dataset.
     */
    public List<String> variableNames() {
        return List.copyOf(columns.keySet());
    }

    /**
     * Returns the number of time points (observations) in this dataset.
     */
    public int size() {
        return timeValues.length;
    }
}
