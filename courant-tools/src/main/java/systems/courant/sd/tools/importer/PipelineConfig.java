package systems.courant.sd.tools.importer;

import systems.courant.sd.model.ModelMetadata;

import java.nio.file.Path;

/**
 * Configuration for a single import pipeline run.
 *
 * @param sourceFile    the model file to import (.xmile, .stmx, .mdl)
 * @param metadata      attribution and licensing metadata for the imported model
 * @param category      target sub-package under demo (e.g. "epidemiology"), or null for root
 * @param className     the Java class name to generate (e.g. "SirXmileDemo")
 * @param jsonFileName  optional kebab-case name for JSON output (e.g. "sir-epidemic");
 *                      if null, className is used as the JSON filename
 * @param outputDir     root source directory for generated code
 * @param dryRun        if true, print generated source but do not write to disk
 * @param overwrite     if true, overwrite existing generated class
 * @param generateCode  if true (default), generate a Java demo class; if false, write JSON only
 */
public record PipelineConfig(
        Path sourceFile,
        ModelMetadata metadata,
        String category,
        String className,
        String jsonFileName,
        Path outputDir,
        boolean dryRun,
        boolean overwrite,
        boolean generateCode
) {

    /**
     * Backward-compatible constructor that defaults generateCode to true and jsonFileName to null.
     */
    public PipelineConfig(Path sourceFile, ModelMetadata metadata, String category,
                          String className, Path outputDir, boolean dryRun, boolean overwrite) {
        this(sourceFile, metadata, category, className, null, outputDir, dryRun, overwrite, true);
    }

    /**
     * Backward-compatible constructor that defaults jsonFileName to null.
     */
    public PipelineConfig(Path sourceFile, ModelMetadata metadata, String category,
                          String className, Path outputDir, boolean dryRun, boolean overwrite,
                          boolean generateCode) {
        this(sourceFile, metadata, category, className, null, outputDir, dryRun, overwrite,
                generateCode);
    }

    public PipelineConfig {
        if (sourceFile == null) {
            throw new IllegalArgumentException("sourceFile must not be null");
        }
        if (metadata == null) {
            throw new IllegalArgumentException("metadata must not be null");
        }
        if (className == null || className.isBlank()) {
            throw new IllegalArgumentException("className must not be blank");
        }
        if (outputDir == null) {
            throw new IllegalArgumentException("outputDir must not be null");
        }
    }
}
