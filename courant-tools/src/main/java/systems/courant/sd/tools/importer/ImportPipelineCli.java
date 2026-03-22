package systems.courant.sd.tools.importer;

import static systems.courant.sd.io.json.JsonNodeHelper.textOrNull;

import systems.courant.sd.model.ModelMetadata;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.Closeable;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Scanner;

/**
 * CLI entry point for the model import pipeline.
 *
 * <p>Usage:
 * <pre>
 *   java ImportPipelineCli --file model.xmile --class-name SirDemo \
 *       --license "CC-BY-SA-4.0" --source "Kermack &amp; McKendrick (1927)" \
 *       [--author "..."] [--url "..."] \
 *       [--category epidemiology] [--json-name my-model] \
 *       [--output-dir path/to/src/main/java] \
 *       [--metadata-file metadata.json] \
 *       [--dry-run] [--overwrite]
 * </pre>
 */
public class ImportPipelineCli implements Closeable {

    public static void main(String[] args) {
        try (ImportPipelineCli cli = new ImportPipelineCli()) {
            int exitCode = cli.run(args);
            System.exit(exitCode);
        } catch (IllegalArgumentException e) {
            System.err.println(e.getMessage());
            printUsage();
            System.exit(1);
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace(System.err);
            System.exit(1);
        }
    }

    @Override
    public void close() {
        // Do not close stdinScanner — it wraps System.in, and Scanner.close()
        // would close the underlying stream, breaking later stdin reads in the JVM.
        stdinScanner = null;
    }

    int run(String[] args) throws IOException {
        CliArgs parsed = parseArgs(args);

        if (parsed.helpRequested) {
            printUsage();
            return 0;
        }

        if (parsed.file == null) {
            printUsage();
            return 1;
        }

        if (parsed.className == null) {
            System.err.println("Error: --class-name is required");
            return 1;
        }

        // Load metadata from file if provided, then overlay CLI flags
        ModelMetadata metadata = buildMetadata(parsed);

        if (metadata.license() == null) {
            System.err.println("Error: --license is required (use \"unknown\" to explicitly skip)");
            return 1;
        }

        PipelineConfig config = new PipelineConfig(
                Path.of(parsed.file),
                metadata,
                parsed.category,
                parsed.className,
                parsed.jsonName,
                Path.of(parsed.outputDir),
                parsed.dryRun,
                parsed.overwrite,
                !parsed.jsonOnly);

        ImportPipeline pipeline = new ImportPipeline();
        PipelineResult result = pipeline.execute(config);

        if (parsed.dryRun) {
            System.out.println(result.generatedSource());
        }

        System.err.println();
        System.err.println("=== Import Summary ===");
        result.printReport(System.err);

        return result.hasValidationErrors() || result.hasTrialCompileErrors() ? 1 : 0;
    }

    private ModelMetadata buildMetadata(CliArgs parsed) throws IOException {
        String author = null;
        String source = null;
        String license = null;
        String url = null;

        // Load from metadata file first
        if (parsed.metadataFile != null) {
            ObjectMapper mapper = new ObjectMapper();
            JsonNode node = mapper.readTree(Files.readString(Path.of(parsed.metadataFile)));
            author = textOrNull(node, "author");
            source = textOrNull(node, "source");
            license = textOrNull(node, "license");
            url = textOrNull(node, "url");
        }

        // CLI flags override metadata file
        if (parsed.author != null) {
            author = parsed.author;
        }
        if (parsed.source != null) {
            source = parsed.source;
        }
        if (parsed.license != null) {
            license = parsed.license;
        }
        if (parsed.url != null) {
            url = parsed.url;
        }

        // Interactive prompts for missing required fields
        if (license == null && System.console() != null) {
            license = prompt("License (required): ");
            if (license != null && license.isBlank()) {
                license = null;
            }
        }

        return ModelMetadata.builder()
                .author(author)
                .source(source)
                .license(license)
                .url(url)
                .build();
    }


    /** Lazily initialized Scanner for reading from System.in when System.console() is null. */
    private Scanner stdinScanner;

    String prompt(String message) {
        System.err.print(message);
        if (System.console() != null) {
            return System.console().readLine();
        }
        if (stdinScanner == null) {
            stdinScanner = new Scanner(System.in, java.nio.charset.StandardCharsets.UTF_8);
        }
        return stdinScanner.hasNextLine() ? stdinScanner.nextLine() : null;
    }

    static CliArgs parseArgs(String[] args) {
        CliArgs parsed = new CliArgs();
        for (int i = 0; i < args.length; i++) {
            switch (args[i]) {
                case "--file" -> { parsed.file = requireValue(args, i); i++; }
                case "--class-name" -> { parsed.className = requireValue(args, i); i++; }
                case "--category" -> { parsed.category = requireValue(args, i); i++; }
                case "--author" -> { parsed.author = requireValue(args, i); i++; }
                case "--source" -> { parsed.source = requireValue(args, i); i++; }
                case "--license" -> { parsed.license = requireValue(args, i); i++; }
                case "--url" -> { parsed.url = requireValue(args, i); i++; }
                case "--json-name" -> { parsed.jsonName = requireValue(args, i); i++; }
                case "--output-dir" -> { parsed.outputDir = requireValue(args, i); i++; }
                case "--metadata-file" -> { parsed.metadataFile = requireValue(args, i); i++; }
                case "--dry-run" -> parsed.dryRun = true;
                case "--overwrite" -> parsed.overwrite = true;
                case "--json-only" -> parsed.jsonOnly = true;
                case "--help", "-h" -> parsed.helpRequested = true;
                default -> throw new IllegalArgumentException(
                        "Unknown option: " + args[i]);
            }
        }
        return parsed;
    }

    private static String requireValue(String[] args, int flagIndex) {
        int valueIndex = flagIndex + 1;
        if (valueIndex >= args.length) {
            throw new IllegalArgumentException(
                    args[flagIndex] + " requires a value");
        }
        return args[valueIndex];
    }

    private static void printUsage() {
        System.err.println("""
                Usage: ImportPipelineCli --file <path> --class-name <name> --license <id> [options]

                Required:
                  --file <path>           Model file to import (.xmile, .stmx, .mdl)
                  --class-name <name>     Java class name for the generated demo
                  --license <id>          License identifier (e.g. "CC-BY-SA-4.0")

                Metadata:
                  --author <name>         Model author(s)
                  --source <ref>          Bibliographic reference or origin
                  --url <url>             URL to original publication or source
                  --metadata-file <path>  JSON file with metadata fields (CLI flags override)

                Output:
                  --category <name>       Target sub-package (e.g. "epidemiology")
                  --json-name <name>      Custom JSON filename (default: derived from class name)
                  --output-dir <path>     Root source directory (default: courant-demos/src/main/java)
                  --json-only             Write model definition as JSON instead of generating Java code
                  --dry-run               Print generated source without writing to disk
                  --overwrite             Overwrite existing generated class
                """);
    }

    static class CliArgs {
        String file;
        String className;
        String category;
        String author;
        String source;
        String license;
        String url;
        String jsonName;
        String metadataFile;
        String outputDir = "courant-demos/src/main/java";
        boolean dryRun;
        boolean overwrite;
        boolean jsonOnly;
        boolean helpRequested;
    }
}
