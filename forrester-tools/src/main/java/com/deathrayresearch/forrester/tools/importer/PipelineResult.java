package com.deathrayresearch.forrester.tools.importer;

import com.deathrayresearch.forrester.model.def.ModelDefinition;

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
}
