package com.deathrayresearch.forrester.model.def;

/**
 * A single validation issue found during model validation.
 *
 * @param severity the issue severity (ERROR or WARNING)
 * @param elementName the name of the element involved, or null for model-level issues
 * @param message a human-readable description of the issue
 */
public record ValidationIssue(
        Severity severity,
        String elementName,
        String message
) {

    /**
     * Severity level for a validation issue.
     */
    public enum Severity {
        ERROR,
        WARNING
    }

    public ValidationIssue {
        if (severity == null) {
            throw new IllegalArgumentException("Severity must not be null");
        }
        if (message == null || message.isBlank()) {
            throw new IllegalArgumentException("Message must not be blank");
        }
    }
}
