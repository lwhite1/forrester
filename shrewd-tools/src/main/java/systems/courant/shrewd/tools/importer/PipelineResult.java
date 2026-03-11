package systems.courant.shrewd.tools.importer;

import systems.courant.shrewd.model.def.ModelDefinition;

import java.io.PrintStream;
import java.nio.file.Path;
import java.util.List;

/**
 * Result of an import pipeline run.
 *
 * @param definition         the parsed model definition
 * @param importWarnings     warnings from the import stage
 * @param validationErrors   structural validation errors
 * @param trialCompileErrors errors from trial compilation (empty if succeeded)
 * @param generatedSource    the generated Java source code
 * @param outputFile         the path where the source was written (null if dry-run)
 */
public record PipelineResult(
        ModelDefinition definition,
        List<String> importWarnings,
        List<String> validationErrors,
        List<String> trialCompileErrors,
        String generatedSource,
        Path outputFile
) {

    public boolean hasValidationErrors() {
        return !validationErrors.isEmpty();
    }

    public boolean hasTrialCompileErrors() {
        return !trialCompileErrors.isEmpty();
    }

    public boolean isClean() {
        return importWarnings.isEmpty() && validationErrors.isEmpty()
                && trialCompileErrors.isEmpty();
    }

    /**
     * Prints a human-readable import report to the given stream.
     * Shows counts and individual messages for any warnings or errors.
     */
    public void printReport(PrintStream out) {
        out.println("Model:            " + definition.name());
        if (outputFile != null) {
            out.println("Output:           " + outputFile);
        }

        printSection(out, "Import warnings", importWarnings);
        printSection(out, "Validation errors", validationErrors);

        if (trialCompileErrors.isEmpty()) {
            out.println("Trial compile:    OK");
        } else {
            printSection(out, "Trial compile errors", trialCompileErrors);
        }
    }

    private static void printSection(PrintStream out, String label, List<String> items) {
        if (items.isEmpty()) {
            out.println(label + ":  0");
        } else {
            out.println(label + ":  " + items.size());
            for (String item : items) {
                out.println("  - " + item);
            }
        }
    }
}
