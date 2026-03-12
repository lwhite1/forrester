package systems.courant.shrewd.tools.importer;

import systems.courant.shrewd.model.def.ModelDefinitionBuilder;
import systems.courant.shrewd.model.def.ModelDefinition;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("PipelineResult")
class PipelineResultTest {

    private static final ModelDefinition MINIMAL_DEF = new ModelDefinitionBuilder()
            .name("Test")
            .build();

    @Nested
    @DisplayName("Predicates")
    class Predicates {

        @Test
        void shouldBeCleanWhenAllListsEmpty() {
            PipelineResult result = new PipelineResult(
                    MINIMAL_DEF, List.of(), List.of(), List.of(), "source", null);
            assertThat(result.isClean()).isTrue();
            assertThat(result.hasValidationErrors()).isFalse();
            assertThat(result.hasTrialCompileErrors()).isFalse();
        }

        @Test
        void shouldNotBeCleanWithImportWarnings() {
            PipelineResult result = new PipelineResult(
                    MINIMAL_DEF, List.of("warning"), List.of(), List.of(), "source", null);
            assertThat(result.isClean()).isFalse();
            assertThat(result.hasValidationErrors()).isFalse();
            assertThat(result.hasTrialCompileErrors()).isFalse();
        }

        @Test
        void shouldDetectValidationErrors() {
            PipelineResult result = new PipelineResult(
                    MINIMAL_DEF, List.of(), List.of("error"), List.of(), "source", null);
            assertThat(result.isClean()).isFalse();
            assertThat(result.hasValidationErrors()).isTrue();
            assertThat(result.hasTrialCompileErrors()).isFalse();
        }

        @Test
        void shouldDetectTrialCompileErrors() {
            PipelineResult result = new PipelineResult(
                    MINIMAL_DEF, List.of(), List.of(), List.of("compile error"), "source", null);
            assertThat(result.isClean()).isFalse();
            assertThat(result.hasValidationErrors()).isFalse();
            assertThat(result.hasTrialCompileErrors()).isTrue();
        }

        @Test
        void shouldNotBeCleanWhenAllListsHaveEntries() {
            PipelineResult result = new PipelineResult(
                    MINIMAL_DEF, List.of("w"), List.of("v"), List.of("c"), "source", null);
            assertThat(result.isClean()).isFalse();
            assertThat(result.hasValidationErrors()).isTrue();
            assertThat(result.hasTrialCompileErrors()).isTrue();
        }
    }

    @Nested
    @DisplayName("Report printing")
    class ReportPrinting {

        private String captureReport(PipelineResult result) {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            result.printReport(new PrintStream(baos, true, StandardCharsets.UTF_8));
            return baos.toString(StandardCharsets.UTF_8);
        }

        @Test
        void shouldPrintCleanReport() {
            PipelineResult result = new PipelineResult(
                    MINIMAL_DEF, List.of(), List.of(), List.of(), "source",
                    Path.of("/output/TestDemo.java"));
            String report = captureReport(result);

            assertThat(report).contains("Model:            Test");
            assertThat(report).contains("Output:");
            assertThat(report).contains("TestDemo.java");
            assertThat(report).contains("Import warnings:  0");
            assertThat(report).contains("Validation errors:  0");
            assertThat(report).contains("Trial compile:    OK");
        }

        @Test
        void shouldPrintWarningsAndErrors() {
            PipelineResult result = new PipelineResult(
                    MINIMAL_DEF,
                    List.of("Unsupported function: DELAY N", "Skipped data variable"),
                    List.of("Missing reference: x"),
                    List.of("CompilationException: unknown function"),
                    "source", null);
            String report = captureReport(result);

            assertThat(report).contains("Import warnings:  2");
            assertThat(report).contains("  - Unsupported function: DELAY N");
            assertThat(report).contains("  - Skipped data variable");
            assertThat(report).contains("Validation errors:  1");
            assertThat(report).contains("  - Missing reference: x");
            assertThat(report).contains("Trial compile errors:  1");
            assertThat(report).contains("  - CompilationException: unknown function");
        }

        @Test
        void shouldOmitOutputLineForDryRun() {
            PipelineResult result = new PipelineResult(
                    MINIMAL_DEF, List.of(), List.of(), List.of(), "source", null);
            String report = captureReport(result);

            assertThat(report).doesNotContain("Output:");
        }
    }
}
