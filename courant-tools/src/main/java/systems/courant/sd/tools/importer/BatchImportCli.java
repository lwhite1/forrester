package systems.courant.sd.tools.importer;

import systems.courant.sd.model.ModelMetadata;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * CLI tool that reads a manifest of remote model URLs, downloads each model,
 * and runs it through the {@link ImportPipeline}.
 *
 * <p>Usage:
 * <pre>
 *   java BatchImportCli --manifest models.json \
 *       [--output-dir path/to/output] \
 *       [--json-only] [--dry-run] [--overwrite]
 * </pre>
 */
public class BatchImportCli {

    private static final Logger log = LoggerFactory.getLogger(BatchImportCli.class);
    private static final Duration DOWNLOAD_TIMEOUT = Duration.ofSeconds(30);
    private static final long MAX_DOWNLOAD_BYTES = 10 * 1024 * 1024; // 10 MB

    public static void main(String[] args) {
        try {
            int exitCode = new BatchImportCli().run(args);
            System.exit(exitCode);
        } catch (IllegalArgumentException e) {
            System.err.println(e.getMessage());
            printUsage();
            System.exit(1);
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            System.exit(1);
        }
    }

    int run(String[] args) throws IOException {
        CliArgs parsed = parseArgs(args);

        if (parsed.helpRequested) {
            printUsage();
            return 0;
        }

        if (parsed.manifestFile == null) {
            printUsage();
            return 1;
        }

        List<ManifestEntry> entries = readManifest(Path.of(parsed.manifestFile));
        log.info("Loaded {} manifest entries", entries.size());

        ImportPipeline pipeline = new ImportPipeline();
        int succeeded = 0;
        int failed = 0;
        int skipped = 0;
        List<String> failures = new ArrayList<>();

        for (int i = 0; i < entries.size(); i++) {
            ManifestEntry entry = entries.get(i);
            log.info("[{}/{}] Processing: {} ({})",
                    i + 1, entries.size(), entry.className(), entry.url());

            Path tempFile = null;
            try {
                // Download
                tempFile = downloadToTemp(entry.url());
                log.info("  Downloaded to {}", tempFile);

                // Check if output already exists
                Path outputDir = Path.of(parsed.outputDir);
                String extension = parsed.jsonOnly ? ".json" : ".java";
                Path expectedOutput;
                if (parsed.jsonOnly) {
                    expectedOutput = outputDir.resolve(entry.className() + extension);
                } else {
                    String packageName = ImportPipeline.resolvePackageName(entry.category());
                    expectedOutput = ImportPipeline.resolveOutputPath(
                            outputDir, packageName, entry.className());
                }
                if (Files.exists(expectedOutput) && !parsed.overwrite) {
                    log.info("  Skipping — output already exists: {}", expectedOutput);
                    skipped++;
                    continue;
                }

                // Build pipeline config
                PipelineConfig config = new PipelineConfig(
                        tempFile,
                        entry.metadata(),
                        entry.category(),
                        entry.className(),
                        outputDir,
                        parsed.dryRun,
                        parsed.overwrite,
                        !parsed.jsonOnly);

                // Execute pipeline
                PipelineResult result = pipeline.execute(config);

                if (!result.isClean()) {
                    System.err.println();
                    System.err.println("--- " + entry.className() + " ---");
                    result.printReport(System.err);
                }

                if (parsed.dryRun) {
                    System.out.println("=== " + entry.className() + " ===");
                    System.out.println(result.generatedSource());
                    System.out.println();
                }

                succeeded++;
                log.info("  OK: {}", entry.className());
            } catch (Exception e) {
                failed++;
                String msg = entry.className() + ": " + e.getMessage();
                failures.add(msg);
                log.error("  FAILED: {}", msg);
            } finally {
                if (tempFile != null) {
                    try {
                        Files.deleteIfExists(tempFile);
                        Path tempParent = tempFile.getParent();
                        if (tempParent != null) {
                            Files.deleteIfExists(tempParent);
                        }
                    } catch (IOException e) {
                        log.warn("  Could not delete temp file: {}", tempFile);
                    }
                }
            }
        }

        // Summary
        System.err.println();
        System.err.println("=== Batch Import Summary ===");
        System.err.println("Total:     " + entries.size());
        System.err.println("Succeeded: " + succeeded);
        System.err.println("Skipped:   " + skipped);
        System.err.println("Failed:    " + failed);
        if (!failures.isEmpty()) {
            System.err.println();
            System.err.println("Failures:");
            failures.forEach(f -> System.err.println("  - " + f));
        }

        return failed > 0 ? 1 : 0;
    }

    List<ManifestEntry> readManifest(Path manifestPath) throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        JsonNode root = mapper.readTree(Files.readString(manifestPath));
        if (!root.isArray()) {
            throw new IllegalArgumentException(
                    "Manifest must be a JSON array, got: " + root.getNodeType());
        }

        List<ManifestEntry> entries = new ArrayList<>();
        for (JsonNode node : root) {
            String url = requiredText(node, "url");
            String className = requiredText(node, "className");
            String category = textOrNull(node, "category");
            String comment = textOrNull(node, "comment");

            JsonNode metaNode = node.get("metadata");
            if (metaNode == null || metaNode.isNull()) {
                throw new IllegalArgumentException(
                        "Manifest entry '" + className + "' is missing 'metadata'");
            }

            String license = textOrNull(metaNode, "license");
            if (license == null) {
                throw new IllegalArgumentException(
                        "Manifest entry '" + className + "' metadata is missing 'license'");
            }

            ModelMetadata metadata = ModelMetadata.builder()
                    .author(textOrNull(metaNode, "author"))
                    .source(textOrNull(metaNode, "source"))
                    .license(license)
                    .url(textOrNull(metaNode, "url"))
                    .build();

            entries.add(new ManifestEntry(url, className, category, comment, metadata));
        }
        return entries;
    }

    static Path downloadToTemp(String url) throws IOException {
        URI uri = URI.create(url);
        String scheme = uri.getScheme();
        if (scheme == null || !(scheme.equalsIgnoreCase("http") || scheme.equalsIgnoreCase("https"))) {
            throw new IOException("Only http and https URLs are allowed, got: " + url);
        }
        String rawName = uri.getPath().substring(uri.getPath().lastIndexOf('/') + 1);
        // Sanitize: extract leaf name only to prevent path traversal (e.g. "../")
        String fileName = "model.mdl";
        if (!rawName.isBlank()) {
            Path leaf = Path.of(rawName).getFileName();
            if (leaf != null && !leaf.toString().isBlank()) {
                fileName = leaf.toString();
            }
        }

        Path tempDir = Files.createTempDirectory("courant-download-");
        Path tempFile = tempDir.resolve(fileName);

        try (HttpClient client = HttpClient.newBuilder()
                .followRedirects(HttpClient.Redirect.NORMAL)
                .connectTimeout(DOWNLOAD_TIMEOUT)
                .build()) {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(uri)
                    .timeout(DOWNLOAD_TIMEOUT)
                    .GET()
                    .build();
            HttpResponse<InputStream> response = client.send(request,
                    HttpResponse.BodyHandlers.ofInputStream());

            if (response.statusCode() != 200) {
                throw new IOException("HTTP " + response.statusCode() + " for " + url);
            }

            try (InputStream in = response.body()) {
                long copied = copyWithLimit(in, tempFile, MAX_DOWNLOAD_BYTES);
                log.debug("Downloaded {} bytes to {}", copied, tempFile);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            cleanupTempFile(tempFile, tempDir);
            throw new IOException("Download interrupted: " + url, e);
        } catch (IOException e) {
            cleanupTempFile(tempFile, tempDir);
            throw e;
        }

        return tempFile;
    }

    private static void cleanupTempFile(Path tempFile, Path tempDir) {
        try {
            Files.deleteIfExists(tempFile);
            Files.deleteIfExists(tempDir);
        } catch (IOException ex) {
            log.warn("Could not clean up temp file on error: {}", tempFile);
        }
    }

    private static long copyWithLimit(InputStream in, Path target, long maxBytes)
            throws IOException {
        try (var out = Files.newOutputStream(target)) {
            byte[] buf = new byte[8192];
            long total = 0;
            int n;
            while ((n = in.read(buf)) > 0) {
                total += n;
                if (total > maxBytes) {
                    throw new IOException(
                            "Download exceeds maximum size of " + maxBytes + " bytes");
                }
                out.write(buf, 0, n);
            }
            return total;
        }
    }

    static CliArgs parseArgs(String[] args) {
        CliArgs parsed = new CliArgs();
        for (int i = 0; i < args.length; i++) {
            switch (args[i]) {
                case "--manifest" -> parsed.manifestFile = requireValue(args, i++);
                case "--output-dir" -> parsed.outputDir = requireValue(args, i++);
                case "--json-only" -> parsed.jsonOnly = true;
                case "--dry-run" -> parsed.dryRun = true;
                case "--overwrite" -> parsed.overwrite = true;
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
                Usage: BatchImportCli --manifest <path> [options]

                Required:
                  --manifest <path>     JSON manifest file listing models to import

                Options:
                  --output-dir <path>   Output directory (default: courant-demos/src/main/java)
                  --json-only           Write model definitions as JSON instead of Java code
                  --dry-run             Download and process but do not write output files
                  --overwrite           Overwrite existing output files
                """);
    }

    private static String requiredText(JsonNode node, String field) {
        JsonNode child = node.get(field);
        if (child == null || child.isNull()) {
            throw new IllegalArgumentException("Missing required field: " + field);
        }
        return child.asText();
    }

    private static String textOrNull(JsonNode node, String field) {
        JsonNode child = node.get(field);
        return (child != null && !child.isNull()) ? child.asText() : null;
    }

    static class CliArgs {
        String manifestFile;
        String outputDir = "courant-demos/src/main/java";
        boolean jsonOnly;
        boolean dryRun;
        boolean overwrite;
        boolean helpRequested;
    }
}
