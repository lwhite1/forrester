package com.deathrayresearch.forrester.tools.importer;

import com.deathrayresearch.forrester.io.ImportResult;
import com.deathrayresearch.forrester.io.ModelImporter;
import com.deathrayresearch.forrester.io.vensim.VensimImporter;
import com.deathrayresearch.forrester.io.xmile.XmileImporter;
import com.deathrayresearch.forrester.model.compile.CompiledModel;
import com.deathrayresearch.forrester.model.compile.ModelCompiler;
import com.deathrayresearch.forrester.model.def.DefinitionValidator;
import com.deathrayresearch.forrester.model.def.ModelDefinition;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Orchestrates the model import pipeline: parse, validate, trial-compile, generate, write.
 */
public class ImportPipeline {

    private static final Logger log = LoggerFactory.getLogger(ImportPipeline.class);

    private static final Set<String> XMILE_EXTENSIONS = Set.of(".xmile", ".stmx", ".xml");
    private static final Set<String> VENSIM_EXTENSIONS = Set.of(".mdl");
    private static final String BASE_PACKAGE = "com.deathrayresearch.forrester.demo";

    /**
     * Executes the full import pipeline.
     */
    public PipelineResult execute(PipelineConfig config) throws IOException {
        // Stage 1: Import
        log.info("Importing model from {}", config.sourceFile());
        ImportResult importResult = importModel(config.sourceFile());
        ModelDefinition definition = importResult.definition();
        List<String> importWarnings = importResult.warnings();

        if (!importWarnings.isEmpty()) {
            log.warn("{} import warning(s):", importWarnings.size());
            importWarnings.forEach(w -> log.warn("  - {}", w));
        }

        // Stage 2: Validate
        log.info("Validating model structure");
        List<String> validationErrors = DefinitionValidator.validate(definition);
        if (!validationErrors.isEmpty()) {
            log.warn("{} validation error(s):", validationErrors.size());
            validationErrors.forEach(e -> log.warn("  - {}", e));
        }

        // Stage 3: Trial compile
        log.info("Trial-compiling model");
        List<String> trialCompileErrors = trialCompile(definition);
        if (!trialCompileErrors.isEmpty()) {
            log.warn("{} trial-compile error(s):", trialCompileErrors.size());
            trialCompileErrors.forEach(e -> log.warn("  - {}", e));
        }

        // Stage 4: Generate Java source
        log.info("Generating Java class: {}", config.className());
        String packageName = resolvePackageName(config.category());
        String source = new DemoClassGenerator().generate(
                definition, config.metadata(), config.className(), packageName,
                config.sourceFile().getFileName().toString(),
                importWarnings, validationErrors);

        // Stage 5: Write
        Path outputFile = null;
        if (config.dryRun()) {
            log.info("Dry run — skipping file write");
        } else {
            outputFile = resolveOutputPath(config.outputDir(), packageName, config.className());
            if (Files.exists(outputFile) && !config.overwrite()) {
                throw new IOException("Output file already exists (use --overwrite to replace): " + outputFile);
            }
            Files.createDirectories(outputFile.getParent());
            Files.writeString(outputFile, source, StandardCharsets.UTF_8);
            log.info("Wrote {}", outputFile);
        }

        return new PipelineResult(
                definition, importWarnings, validationErrors, trialCompileErrors,
                source, outputFile);
    }

    private ImportResult importModel(Path sourceFile) throws IOException {
        String fileName = sourceFile.getFileName().toString().toLowerCase();
        String ext = fileName.substring(fileName.lastIndexOf('.'));

        ModelImporter importer;
        if (XMILE_EXTENSIONS.contains(ext)) {
            importer = new XmileImporter();
        } else if (VENSIM_EXTENSIONS.contains(ext)) {
            importer = new VensimImporter();
        } else {
            throw new IllegalArgumentException("Unsupported file extension: " + ext
                    + ". Expected one of: .xmile, .stmx, .xml, .mdl");
        }

        return importer.importModel(sourceFile);
    }

    private List<String> trialCompile(ModelDefinition definition) {
        List<String> errors = new ArrayList<>();
        try {
            ModelCompiler compiler = new ModelCompiler();
            CompiledModel compiled = compiler.compile(definition);
            // Verify simulation can be created (resolves all references)
            if (definition.defaultSimulation() != null) {
                compiled.createSimulation();
            }
        } catch (Exception e) {
            errors.add(e.getClass().getSimpleName() + ": " + e.getMessage());
        }
        return errors;
    }

    static String resolvePackageName(String category) {
        if (category == null || category.isBlank()) {
            return BASE_PACKAGE;
        }
        String segment = JavaSourceEscaper.toPackageSegment(category);
        return BASE_PACKAGE + "." + segment;
    }

    static Path resolveOutputPath(Path outputDir, String packageName, String className) {
        String packagePath = packageName.replace('.', '/');
        return outputDir.resolve(packagePath).resolve(className + ".java");
    }
}
