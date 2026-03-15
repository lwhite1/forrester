package systems.courant.sd.tools.importer;

import systems.courant.sd.io.ImportResult;
import systems.courant.sd.io.ModelImporter;
import systems.courant.sd.io.vensim.VensimImporter;
import systems.courant.sd.io.xmile.XmileImporter;
import systems.courant.sd.model.compile.CompiledModel;
import systems.courant.sd.model.compile.ModelCompiler;
import systems.courant.sd.model.def.DefinitionValidator;
import systems.courant.sd.model.def.ModelDefinition;
import systems.courant.sd.io.json.ModelDefinitionSerializer;

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
    private static final String BASE_PACKAGE = "systems.courant.sd.demo";

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

        // Stage 4: Generate output (Java source or JSON)
        String source;
        String outputExtension;
        if (config.generateCode()) {
            log.info("Generating Java class: {}", config.className());
            String packageName = resolvePackageName(config.category());
            Path srcFileName = config.sourceFile().getFileName();
            source = new DemoClassGenerator().generate(
                    definition, config.metadata(), config.className(), packageName,
                    srcFileName != null ? srcFileName.toString() : config.sourceFile().toString(),
                    importWarnings, validationErrors);
            outputExtension = ".java";
        } else {
            log.info("Generating JSON model definition");
            // Attach metadata to the definition for JSON output
            ModelDefinition defWithMeta = definition.toBuilder()
                    .metadata(config.metadata())
                    .build();
            source = new ModelDefinitionSerializer().toJson(defWithMeta);
            outputExtension = ".json";
        }

        // Stage 5: Write
        Path outputFile = null;
        if (config.dryRun()) {
            log.info("Dry run — skipping file write");
        } else {
            if (config.generateCode()) {
                String packageName = resolvePackageName(config.category());
                outputFile = resolveOutputPath(config.outputDir(), packageName, config.className());
            } else {
                outputFile = config.outputDir().resolve(config.className() + outputExtension);
            }
            if (Files.exists(outputFile) && !config.overwrite()) {
                throw new IOException("Output file already exists (use --overwrite to replace): " + outputFile);
            }
            Path parentDir = outputFile.getParent();
            if (parentDir != null) {
                Files.createDirectories(parentDir);
            }
            Files.writeString(outputFile, source, StandardCharsets.UTF_8);
            log.info("Wrote {}", outputFile);
        }

        return new PipelineResult(
                definition, importWarnings, validationErrors, trialCompileErrors,
                source, outputFile);
    }

    private ImportResult importModel(Path sourceFile) throws IOException {
        Path fileNamePath = sourceFile.getFileName();
        if (fileNamePath == null) {
            throw new IllegalArgumentException("Source file path has no file name: " + sourceFile);
        }
        String fileName = fileNamePath.toString().toLowerCase();
        int dotIndex = fileName.lastIndexOf('.');
        if (dotIndex < 0) {
            throw new IllegalArgumentException(
                    "File has no extension: " + sourceFile.getFileName()
                    + ". Expected one of: .xmile, .stmx, .xml, .mdl");
        }
        String ext = fileName.substring(dotIndex);

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
        } catch (systems.courant.sd.model.compile.CompilationException
                 | systems.courant.sd.model.expr.ParseException e) {
            errors.add(e.getClass().getSimpleName() + ": " + e.getMessage());
        } catch (IllegalStateException e) {
            log.warn("Unexpected IllegalStateException during trial compile", e);
            errors.add(e.getClass().getSimpleName() + ": " + e.getMessage());
        }
        return errors;
    }

    static String resolvePackageName(String category) {
        if (category == null || category.isBlank()) {
            return BASE_PACKAGE;
        }
        String segment = JavaSourceEscaper.toPackageSegment(category);
        if (segment.isEmpty()) {
            return BASE_PACKAGE;
        }
        return BASE_PACKAGE + "." + segment;
    }

    static Path resolveOutputPath(Path outputDir, String packageName, String className) {
        String packagePath = packageName.replace('.', '/');
        return outputDir.resolve(packagePath).resolve(className + ".java");
    }
}
