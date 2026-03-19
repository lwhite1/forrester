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
    private static final List<String> WARNING_REPORT = new ArrayList<>();

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

        // Collect warnings for the report (don't fail on them)
        List<ValidationIssue> warnings = result.issues().stream()
                .filter(i -> i.severity() == Severity.WARNING)
                .toList();
        if (!warnings.isEmpty()) {
            synchronized (WARNING_REPORT) {
                WARNING_REPORT.add(entry.id + " (" + warnings.size() + " warnings):");
                for (ValidationIssue w : warnings) {
                    WARNING_REPORT.add("  [WARNING] " + w.elementName() + ": " + w.message());
                }
                WARNING_REPORT.add("");
            }
        }

        // Errors fail the test — include full details in the assertion message
        List<ValidationIssue> errors = result.issues().stream()
                .filter(i -> i.severity() == Severity.ERROR)
                .toList();
        assertThat(errors)
                .as("Model '%s' should have no validation errors, but found %d:\n%s",
                        entry.id, errors.size(), formatIssues(errors))
                .isEmpty();
    }

    @AfterAll
    static void writeWarningReport() throws IOException {
        if (WARNING_REPORT.isEmpty()) {
            return;
        }
        Path reportDir = Path.of("target");
        Files.createDirectories(reportDir);
        Path reportFile = reportDir.resolve("catalog-validation-warnings.txt");

        List<String> lines = new ArrayList<>();
        lines.add("Catalog Model Validation Warnings");
        lines.add("Generated: " + LocalDateTime.now()
                .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        lines.add("=".repeat(60));
        lines.add("");
        lines.addAll(WARNING_REPORT);
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
