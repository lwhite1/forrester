package systems.courant.shrewd.model.def;

import java.util.List;

/**
 * Immutable result of model validation, containing all discovered issues.
 *
 * @param issues the list of validation issues found
 */
public record ValidationResult(List<ValidationIssue> issues) {

    public ValidationResult {
        issues = issues == null ? List.of() : List.copyOf(issues);
    }

    /**
     * Returns the number of ERROR-severity issues.
     */
    public int errorCount() {
        int count = 0;
        for (ValidationIssue issue : issues) {
            if (issue.severity() == ValidationIssue.Severity.ERROR) {
                count++;
            }
        }
        return count;
    }

    /**
     * Returns the number of WARNING-severity issues.
     */
    public int warningCount() {
        int count = 0;
        for (ValidationIssue issue : issues) {
            if (issue.severity() == ValidationIssue.Severity.WARNING) {
                count++;
            }
        }
        return count;
    }

    /**
     * Returns true if there are no issues at all.
     */
    public boolean isClean() {
        return issues.isEmpty();
    }
}
