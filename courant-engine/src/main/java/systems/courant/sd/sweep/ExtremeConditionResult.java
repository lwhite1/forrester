package systems.courant.sd.sweep;

import java.util.List;

/**
 * The result of running extreme-condition tests on a model.
 *
 * @param findings      the list of findings (anomalies detected)
 * @param runsCompleted the number of simulation runs completed (includes failed runs)
 * @param totalRuns     the total number of runs attempted
 */
public record ExtremeConditionResult(
        List<ExtremeConditionFinding> findings,
        int runsCompleted,
        int totalRuns
) {

    public ExtremeConditionResult {
        findings = List.copyOf(findings);
    }
}
