package systems.courant.shrewd.tools.importer;

import systems.courant.shrewd.model.ModelMetadata;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

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
 *       [--category epidemiology] \
 *       [--output-dir path/to/src/main/java] \
 *       [--metadata-file metadata.json] \
 *       [--dry-run] [--overwrite]
 * </pre>
 */
public class ImportPipelineCli {

    public static void main(String[] args) {
        try {
            int exitCode = new ImportPipelineCli().run(args);
            System.exit(exitCode);
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            System.exit(1);
        }
    }

    int run(String[] args) throws IOException {
        CliArgs parsed = parseArgs(args);

        if (parsed.file == null) {
            printUsage();
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

        return 0;
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

    private static String textOrNull(JsonNode node, String field) {
        JsonNode child = node.get(field);
        return (child != null && !child.isNull()) ? child.asText() : null;
    }

    private static String prompt(String message) {
        System.err.print(message);
        if (System.console() != null) {
            return System.console().readLine();
        }
        Scanner scanner = new Scanner(System.in, java.nio.charset.StandardCharsets.UTF_8);
        return scanner.hasNextLine() ? scanner.nextLine() : null;
    }

    static CliArgs parseArgs(String[] args) {
        CliArgs parsed = new CliArgs();
        for (int i = 0; i < args.length; i++) {
            switch (args[i]) {
                case "--file" -> parsed.file = requireValue(args, i++);
                case "--class-name" -> parsed.className = requireValue(args, i++);
                case "--category" -> parsed.category = requireValue(args, i++);
                case "--author" -> parsed.author = requireValue(args, i++);
                case "--source" -> parsed.source = requireValue(args, i++);
                case "--license" -> parsed.license = requireValue(args, i++);
                case "--url" -> parsed.url = requireValue(args, i++);
                case "--output-dir" -> parsed.outputDir = requireValue(args, i++);
                case "--metadata-file" -> parsed.metadataFile = requireValue(args, i++);
                case "--dry-run" -> parsed.dryRun = true;
                case "--overwrite" -> parsed.overwrite = true;
                case "--json-only" -> parsed.jsonOnly = true;
                case "--help", "-h" -> {
                    printUsage();
                    System.exit(0);
                }
                default -> {
                    System.err.println("Unknown option: " + args[i]);
                    printUsage();
                    System.exit(1);
                }
            }
        }
        return parsed;
    }

    private static String requireValue(String[] args, int flagIndex) {
        int valueIndex = flagIndex + 1;
        if (valueIndex >= args.length) {
            System.err.println("Missing value for " + args[flagIndex]);
            printUsage();
            System.exit(1);
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
                  --output-dir <path>     Root source directory (default: forrester-demos/src/main/java)
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
        String metadataFile;
        String outputDir = "forrester-demos/src/main/java";
        boolean dryRun;
        boolean overwrite;
        boolean jsonOnly;
    }
}
