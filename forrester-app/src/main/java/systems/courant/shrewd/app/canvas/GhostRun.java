package systems.courant.forrester.app.canvas;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * A previous simulation run retained for ghost overlay comparison.
 * Captures the simulation result, a descriptive name, a color index
 * into the ghost palette, and the parameter snapshot used for that run.
 *
 * @param result     the simulation time-series data
 * @param name       display name (auto-generated or user-edited)
 * @param colorIndex index into {@link ChartUtils#GHOST_COLORS}
 * @param parameters parameter name-to-value snapshot at time of run
 */
record GhostRun(SimulationRunner.SimulationResult result,
                String name,
                int colorIndex,
                Map<String, Double> parameters) {

    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm:ss");
    private static final int MAX_SHOWN_PARAMS = 2;

    GhostRun {
        parameters = parameters == null ? Map.of() : Map.copyOf(parameters);
    }

    /**
     * Returns a new GhostRun with the given name, preserving all other fields.
     */
    GhostRun withName(String newName) {
        return new GhostRun(result, newName, colorIndex, parameters);
    }

    /**
     * Generates a descriptive run name by diffing the current parameters against
     * the previous run's parameters. If parameters changed, the name lists the
     * changed values (e.g. "Run 3: rate=0.05, delay=2"). If nothing changed or
     * there are no parameters, falls back to "Run N (HH:mm:ss)".
     *
     * @param runNumber       sequential run number
     * @param currentParams   parameter snapshot for the current run
     * @param previousParams  parameter snapshot from the previous run (may be empty)
     * @return a descriptive run name
     */
    static String generateName(int runNumber, Map<String, Double> currentParams,
                               Map<String, Double> previousParams) {
        if (currentParams == null || currentParams.isEmpty()) {
            return "Run " + runNumber + " (" + LocalTime.now().format(TIME_FMT) + ")";
        }

        Map<String, Double> changed = new LinkedHashMap<>();
        for (Map.Entry<String, Double> entry : currentParams.entrySet()) {
            Double prev = previousParams == null ? null : previousParams.get(entry.getKey());
            if (prev == null || Double.compare(prev, entry.getValue()) != 0) {
                changed.put(entry.getKey(), entry.getValue());
            }
        }

        if (changed.isEmpty()) {
            return "Run " + runNumber + " (" + LocalTime.now().format(TIME_FMT) + ")";
        }

        String paramSummary = changed.entrySet().stream()
                .limit(MAX_SHOWN_PARAMS)
                .map(e -> e.getKey() + "=" + ChartUtils.formatNumber(e.getValue()))
                .collect(Collectors.joining(", "));

        if (changed.size() > MAX_SHOWN_PARAMS) {
            paramSummary += " (+" + (changed.size() - MAX_SHOWN_PARAMS) + " more)";
        }

        return "Run " + runNumber + ": " + paramSummary;
    }

    /**
     * Builds a tooltip string showing the run name and all parameter values.
     */
    String tooltipText() {
        if (parameters.isEmpty()) {
            return name;
        }
        String params = parameters.entrySet().stream()
                .map(e -> "  " + e.getKey() + " = " + ChartUtils.formatNumber(e.getValue()))
                .collect(Collectors.joining("\n"));
        return name + "\n" + params;
    }
}
