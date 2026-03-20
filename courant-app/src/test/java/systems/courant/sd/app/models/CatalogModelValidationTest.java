package systems.courant.sd.app.models;

import systems.courant.sd.io.json.ModelDefinitionSerializer;
import systems.courant.sd.model.def.ModelDefinition;
import systems.courant.sd.model.def.ModelValidator;
import systems.courant.sd.model.def.ValidationIssue;
import systems.courant.sd.model.def.ValidationIssue.Severity;
import systems.courant.sd.model.def.ValidationResult;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assumptions.assumeThat;

/**
 * Validates every bundled model in the catalog by loading the JSON and
 * running {@link ModelValidator}. Errors fail the test. Warnings are
 * collected and written to a report file after all tests complete.
 *
 * <p>Run with {@code mvn test -Dvalidate.catalog=true} to enable.
 * Not part of the regular build because it validates data, not code.
 */
@DisplayName("Catalog model validation")
class CatalogModelValidationTest {

    private static final ModelDefinitionSerializer SERIALIZER = new ModelDefinitionSerializer();
    private static final List<String> REPORT = new ArrayList<>();
    private static int totalModels;
    private static int modelsClean;
    private static int totalErrors;
    private static int totalWarnings;
    private static int modelsWithErrors;
    private static int modelsWithWarnings;

    @BeforeAll
    static void onlyWhenRequested() {
        assumeThat(Boolean.getBoolean("validate.catalog"))
                .as("Run with -Dvalidate.catalog=true to validate catalog models")
                .isTrue();
    }

    record CatalogEntry(String id, String path) {
        @Override
        public String toString() {
            return id;
        }
    }

    static Stream<CatalogEntry> catalogModels() throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        try (InputStream in = CatalogModelValidationTest.class
                .getResourceAsStream("/models/catalog.json")) {
            if (in == null) {
                return Stream.empty();
            }
            JsonNode root = mapper.readTree(in);
            List<CatalogEntry> entries = new ArrayList<>();
            for (JsonNode node : root.get("models")) {
                entries.add(new CatalogEntry(
                        node.get("id").asText(),
                        node.get("path").asText()));
            }
            return entries.stream();
        }
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("catalogModels")
    @DisplayName("has zero validation errors")
    void shouldHaveNoValidationErrors(CatalogEntry entry) throws IOException {
        ModelDefinition def = loadModel(entry.path);
        ValidationResult result = ModelValidator.validate(def);
        synchronized (REPORT) {
            totalModels++;
        }

        List<ValidationIssue> errors = result.issues().stream()
                .filter(i -> i.severity() == Severity.ERROR).toList();
        List<ValidationIssue> warnings = result.issues().stream()
                .filter(i -> i.severity() == Severity.WARNING).toList();

        if (errors.isEmpty() && warnings.isEmpty()) {
            synchronized (REPORT) {
                modelsClean++;
            }
        } else {
            synchronized (REPORT) {
                REPORT.add(entry.id + " (" + errors.size() + " errors, "
                        + warnings.size() + " warnings):");
                for (ValidationIssue e : errors) {
                    REPORT.add("  [ERROR]   " + e.elementName() + ": " + e.message());
                }
                for (ValidationIssue w : warnings) {
                    REPORT.add("  [WARNING] " + w.elementName() + ": " + w.message());
                }
                REPORT.add("");
                totalErrors += errors.size();
                totalWarnings += warnings.size();
                if (!errors.isEmpty()) {
                    modelsWithErrors++;
                }
                if (!warnings.isEmpty()) {
                    modelsWithWarnings++;
                }
            }
        }

        assertThat(errors)
                .as("Model '%s' should have no validation errors, but found %d:\n%s",
                        entry.id, errors.size(), formatIssues(errors))
                .isEmpty();
    }

    @AfterAll
    static void writeReport() throws IOException {
        if (REPORT.isEmpty()) {
            return;
        }
        Path reportDir = resolveReportDir();
        Files.createDirectories(reportDir);
        Path reportFile = reportDir.resolve("catalog-validation-report.txt");

        List<String> lines = new ArrayList<>();
        lines.add("Catalog Model Validation Report");
        lines.add("Generated: " + LocalDateTime.now()
                .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        lines.add("=".repeat(60));
        lines.add("");
        lines.add("Total models:         " + totalModels);
        lines.add("Models clean:         " + modelsClean);
        lines.add("Models with errors:   " + modelsWithErrors);
        lines.add("Models with warnings: " + modelsWithWarnings);
        lines.add("Total errors:         " + totalErrors);
        lines.add("Total warnings:       " + totalWarnings);
        lines.add("");
        lines.add("=".repeat(60));
        lines.add("");
        lines.addAll(REPORT);
        Files.write(reportFile, lines);
    }

    private ModelDefinition loadModel(String path) throws IOException {
        try (InputStream in = getClass().getResourceAsStream("/models/" + path)) {
            if (in == null) {
                throw new IOException("Model resource not found: /models/" + path);
            }
            String json = new String(in.readAllBytes());
            return SERIALIZER.fromJson(json);
        }
    }

    /**
     * Resolves the report output directory. Uses {@code git rev-parse --git-common-dir}
     * to find the main repo root (works in both worktrees and the main checkout).
     */
    private static Path resolveReportDir() {
        try {
            Process p = new ProcessBuilder("git", "rev-parse", "--git-common-dir")
                    .redirectErrorStream(true).start();
            String output = new String(p.getInputStream().readAllBytes()).strip();
            p.waitFor();
            if (!output.isBlank()) {
                // --git-common-dir returns path to .git; parent is repo root
                return Path.of(output).getParent().resolve("devdocs/quality/demos");
            }
        } catch (Exception ignored) {
        }
        return Path.of("../devdocs/quality/demos");
    }

    private static String formatIssues(List<ValidationIssue> issues) {
        if (issues.isEmpty()) {
            return "(none)";
        }
        var sb = new StringBuilder();
        for (ValidationIssue issue : issues) {
            sb.append("  [").append(issue.severity()).append("] ");
            if (issue.elementName() != null) {
                sb.append(issue.elementName()).append(": ");
            }
            sb.append(issue.message()).append("\n");
        }
        return sb.toString();
    }
}
