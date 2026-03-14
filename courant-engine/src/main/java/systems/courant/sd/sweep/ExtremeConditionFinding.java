package systems.courant.sd.sweep;

/**
 * A single finding from an extreme-condition test run. All findings are informational —
 * extreme conditions are expected to produce unusual behavior. The goal is awareness,
 * not enforcement.
 *
 * @param parameterName        the parameter that was set to an extreme value
 * @param baselineValue        the original parameter value
 * @param condition            the extreme condition applied (ZERO, TEN_X, NEGATIVE)
 * @param appliedValue         the numeric value used for the test
 * @param affectedVariable     the stock, flow, or variable where the problem occurred
 * @param stepNumber           the simulation step where the problem was detected (-1 if N/A)
 * @param description          a human-readable description of the finding
 */
public record ExtremeConditionFinding(
        String parameterName,
        double baselineValue,
        ExtremeCondition condition,
        double appliedValue,
        String affectedVariable,
        long stepNumber,
        String description
) {
}
