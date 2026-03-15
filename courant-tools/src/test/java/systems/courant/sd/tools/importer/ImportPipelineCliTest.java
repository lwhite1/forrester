package systems.courant.sd.tools.importer;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayInputStream;
import java.io.Closeable;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ImportPipelineCliTest {

    @Nested
    @DisplayName("parseArgs")
    class ParseArgs {

        @Test
        void shouldParseAllFlags() {
            String[] args = {
                    "--file", "model.xmile",
                    "--class-name", "MyDemo",
                    "--category", "epidemiology",
                    "--author", "John Doe",
                    "--source", "Some Paper (2024)",
                    "--license", "CC-BY-SA-4.0",
                    "--url", "https://example.com",
                    "--output-dir", "/tmp/src",
                    "--metadata-file", "meta.json",
                    "--dry-run",
                    "--overwrite",
                    "--json-only"
            };

            ImportPipelineCli.CliArgs parsed = ImportPipelineCli.parseArgs(args);

            assertThat(parsed.file).isEqualTo("model.xmile");
            assertThat(parsed.className).isEqualTo("MyDemo");
            assertThat(parsed.category).isEqualTo("epidemiology");
            assertThat(parsed.author).isEqualTo("John Doe");
            assertThat(parsed.source).isEqualTo("Some Paper (2024)");
            assertThat(parsed.license).isEqualTo("CC-BY-SA-4.0");
            assertThat(parsed.url).isEqualTo("https://example.com");
            assertThat(parsed.outputDir).isEqualTo("/tmp/src");
            assertThat(parsed.metadataFile).isEqualTo("meta.json");
            assertThat(parsed.dryRun).isTrue();
            assertThat(parsed.overwrite).isTrue();
            assertThat(parsed.jsonOnly).isTrue();
        }

        @Test
        void shouldDefaultJsonOnlyToFalse() {
            ImportPipelineCli.CliArgs parsed = ImportPipelineCli.parseArgs(new String[]{});
            assertThat(parsed.jsonOnly).isFalse();
        }

        @Test
        void shouldUseDefaultOutputDir() {
            ImportPipelineCli.CliArgs parsed = ImportPipelineCli.parseArgs(new String[]{});
            assertThat(parsed.outputDir).isEqualTo("courant-demos/src/main/java");
        }
    }

    @Nested
    @DisplayName("parseArgs error paths (#283)")
    class ParseArgsErrorPaths {

        @Test
        void shouldThrowOnUnknownOption() {
            assertThatThrownBy(() -> ImportPipelineCli.parseArgs(
                    new String[]{"--bogus"}))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Unknown option")
                    .hasMessageContaining("--bogus");
        }

        @Test
        void shouldThrowWhenFileMissingValue() {
            assertThatThrownBy(() -> ImportPipelineCli.parseArgs(
                    new String[]{"--file"}))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("--file")
                    .hasMessageContaining("requires a value");
        }

        @Test
        void shouldThrowWhenClassNameMissingValue() {
            assertThatThrownBy(() -> ImportPipelineCli.parseArgs(
                    new String[]{"--class-name"}))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("--class-name")
                    .hasMessageContaining("requires a value");
        }

        @Test
        void shouldThrowWhenLicenseMissingValue() {
            assertThatThrownBy(() -> ImportPipelineCli.parseArgs(
                    new String[]{"--license"}))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("--license")
                    .hasMessageContaining("requires a value");
        }

        @Test
        void shouldSetHelpRequestedOnHelpFlag() {
            ImportPipelineCli.CliArgs parsed = ImportPipelineCli.parseArgs(
                    new String[]{"--help"});
            assertThat(parsed.helpRequested).isTrue();
        }

        @Test
        void shouldSetHelpRequestedOnShortFlag() {
            ImportPipelineCli.CliArgs parsed = ImportPipelineCli.parseArgs(
                    new String[]{"-h"});
            assertThat(parsed.helpRequested).isTrue();
        }

        @Test
        void shouldReturnZeroForHelpViaRun() throws IOException {
            try (ImportPipelineCli cli = new ImportPipelineCli()) {
                int exitCode = cli.run(new String[]{"--help"});
                assertThat(exitCode).isZero();
            }
        }
    }

    @Nested
    @DisplayName("run (#571)")
    class Run {

        @Test
        void shouldReturnOneWhenFileMissing() throws IOException {
            try (ImportPipelineCli cli = new ImportPipelineCli()) {
                int exitCode = cli.run(new String[]{});
                assertThat(exitCode).isEqualTo(1);
            }
        }

        @Test
        void shouldReturnOneWhenLicenseMissing(@TempDir Path tempDir) throws IOException {
            Path model = copyTestModel(tempDir);
            try (ImportPipelineCli cli = new ImportPipelineCli()) {
                int exitCode = cli.run(new String[]{
                        "--file", model.toString(),
                        "--class-name", "TeacupDemo"
                });
                assertThat(exitCode).isEqualTo(1);
            }
        }

        @Test
        void shouldReturnOneWhenClassNameMissing(@TempDir Path tempDir) throws IOException {
            Path model = copyTestModel(tempDir);
            try (ImportPipelineCli cli = new ImportPipelineCli()) {
                int exitCode = cli.run(new String[]{
                        "--file", model.toString(),
                        "--license", "CC-BY-SA-4.0"
                });
                assertThat(exitCode).isEqualTo(1);
            }
        }

        @Test
        void shouldRunDryRunWithoutWritingFiles(@TempDir Path tempDir) throws IOException {
            Path model = copyTestModel(tempDir);
            long fileCountBefore = Files.list(tempDir).count();
            try (ImportPipelineCli cli = new ImportPipelineCli()) {
                int exitCode = cli.run(new String[]{
                        "--file", model.toString(),
                        "--class-name", "TeacupDemo",
                        "--license", "CC-BY-SA-4.0",
                        "--output-dir", tempDir.toString(),
                        "--dry-run"
                });
                assertThat(exitCode).isZero();
            }
            // Dry run should not create new directories or files beyond what was there
            long fileCountAfter = Files.list(tempDir).count();
            assertThat(fileCountAfter).isEqualTo(fileCountBefore);
        }

        @Test
        void shouldWriteJavaOutputFile(@TempDir Path tempDir) throws IOException {
            Path model = copyTestModel(tempDir);
            try (ImportPipelineCli cli = new ImportPipelineCli()) {
                int exitCode = cli.run(new String[]{
                        "--file", model.toString(),
                        "--class-name", "TeacupDemo",
                        "--license", "CC-BY-SA-4.0",
                        "--output-dir", tempDir.toString(),
                        "--overwrite"
                });
                assertThat(exitCode).isZero();
            }
            // Verify Java file was created under the expected package path
            String packageName = ImportPipeline.resolvePackageName(null);
            Path expectedFile = ImportPipeline.resolveOutputPath(
                    tempDir, packageName, "TeacupDemo");
            assertThat(expectedFile).exists();
            assertThat(Files.readString(expectedFile)).contains("class TeacupDemo");
        }

        @Test
        void shouldWriteJsonOnlyOutput(@TempDir Path tempDir) throws IOException {
            Path model = copyTestModel(tempDir);
            try (ImportPipelineCli cli = new ImportPipelineCli()) {
                int exitCode = cli.run(new String[]{
                        "--file", model.toString(),
                        "--class-name", "TeacupDemo",
                        "--license", "CC-BY-SA-4.0",
                        "--output-dir", tempDir.toString(),
                        "--json-only",
                        "--overwrite"
                });
                assertThat(exitCode).isZero();
            }
            // JSON-only mode writes a .json file, not a .java file
            Path expectedJson = tempDir.resolve("TeacupDemo.json");
            assertThat(expectedJson).exists();
            assertThat(Files.readString(expectedJson)).contains("Teacup");
        }
    }

    @Nested
    @DisplayName("buildMetadata (#571)")
    class BuildMetadata {

        @Test
        void shouldLoadMetadataFromFile(@TempDir Path tempDir) throws IOException {
            Path model = copyTestModel(tempDir);
            Path metadataFile = tempDir.resolve("metadata.json");
            Files.writeString(metadataFile, """
                    {
                      "author": "File Author",
                      "source": "File Source",
                      "license": "MIT",
                      "url": "https://file.example.com"
                    }
                    """);

            try (ImportPipelineCli cli = new ImportPipelineCli()) {
                int exitCode = cli.run(new String[]{
                        "--file", model.toString(),
                        "--class-name", "TeacupDemo",
                        "--metadata-file", metadataFile.toString(),
                        "--output-dir", tempDir.toString(),
                        "--overwrite"
                });
                assertThat(exitCode).isZero();
            }

            // Verify metadata from file was applied to generated source
            String packageName = ImportPipeline.resolvePackageName(null);
            Path outputFile = ImportPipeline.resolveOutputPath(
                    tempDir, packageName, "TeacupDemo");
            String source = Files.readString(outputFile);
            assertThat(source).contains("File Author");
            assertThat(source).contains("MIT");
        }

        @Test
        void shouldOverrideMetadataFileWithCliFlags(@TempDir Path tempDir) throws IOException {
            Path model = copyTestModel(tempDir);
            Path metadataFile = tempDir.resolve("metadata.json");
            Files.writeString(metadataFile, """
                    {
                      "author": "File Author",
                      "license": "MIT"
                    }
                    """);

            try (ImportPipelineCli cli = new ImportPipelineCli()) {
                int exitCode = cli.run(new String[]{
                        "--file", model.toString(),
                        "--class-name", "TeacupDemo",
                        "--metadata-file", metadataFile.toString(),
                        "--license", "CC-BY-SA-4.0",
                        "--author", "CLI Author",
                        "--output-dir", tempDir.toString(),
                        "--overwrite"
                });
                assertThat(exitCode).isZero();
            }

            // CLI flags should override metadata file values
            String packageName = ImportPipeline.resolvePackageName(null);
            Path outputFile = ImportPipeline.resolveOutputPath(
                    tempDir, packageName, "TeacupDemo");
            String source = Files.readString(outputFile);
            assertThat(source).contains("CLI Author");
            assertThat(source).contains("CC-BY-SA-4.0");
            assertThat(source).doesNotContain("File Author");
            assertThat(source).doesNotContain("MIT");
        }

        @Test
        void shouldReturnOneWhenMetadataFileLacksLicense(@TempDir Path tempDir) throws IOException {
            Path model = copyTestModel(tempDir);
            Path metadataFile = tempDir.resolve("metadata.json");
            Files.writeString(metadataFile, """
                    {
                      "author": "File Author"
                    }
                    """);

            try (ImportPipelineCli cli = new ImportPipelineCli()) {
                int exitCode = cli.run(new String[]{
                        "--file", model.toString(),
                        "--class-name", "TeacupDemo",
                        "--metadata-file", metadataFile.toString(),
                        "--output-dir", tempDir.toString(),
                        "--dry-run"
                });
                assertThat(exitCode).isEqualTo(1);
            }
        }
    }

    @Nested
    @DisplayName("Closeable and prompt")
    class CloseableAndPrompt {

        @Test
        void shouldReuseScannerAcrossMultiplePromptCalls() {
            java.io.InputStream originalIn = System.in;
            String input = "first\nsecond\nthird\n";
            System.setIn(new ByteArrayInputStream(input.getBytes(StandardCharsets.UTF_8)));
            try (ImportPipelineCli cli = new ImportPipelineCli()) {
                String r1 = cli.prompt("Q1: ");
                String r2 = cli.prompt("Q2: ");
                String r3 = cli.prompt("Q3: ");

                assertThat(r1).isEqualTo("first");
                assertThat(r2).isEqualTo("second");
                assertThat(r3).isEqualTo("third");
            } finally {
                System.setIn(originalIn);
            }
        }

        @Test
        void shouldImplementCloseable() {
            assertThat(Closeable.class).isAssignableFrom(ImportPipelineCli.class);
        }

        @Test
        void shouldCloseWithoutErrorWhenScannerNotCreated() {
            ImportPipelineCli cli = new ImportPipelineCli();
            cli.close();
        }

        @Test
        void shouldCloseCleanlyAfterPromptCalls() {
            java.io.InputStream originalIn = System.in;
            System.setIn(new ByteArrayInputStream("test\n".getBytes(StandardCharsets.UTF_8)));
            try {
                ImportPipelineCli cli = new ImportPipelineCli();
                cli.prompt("Q: ");
                cli.close();
                cli.close(); // double-close should be safe
            } finally {
                System.setIn(originalIn);
            }
        }
    }

    private static Path copyTestModel(Path targetDir) throws IOException {
        try (var in = ImportPipelineCliTest.class.getResourceAsStream("/xmile/teacup.xmile")) {
            if (in == null) {
                throw new IOException("Test resource /xmile/teacup.xmile not found on classpath");
            }
            Path target = targetDir.resolve("teacup.xmile");
            Files.copy(in, target);
            return target;
        }
    }
}
