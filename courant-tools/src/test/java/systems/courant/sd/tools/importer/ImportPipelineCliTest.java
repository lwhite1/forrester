package systems.courant.sd.tools.importer;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

class ImportPipelineCliTest {

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

    @Test
    void shouldReuseScannerAcrossMultiplePromptCalls() {
        java.io.InputStream originalIn = System.in;
        String input = "first\nsecond\nthird\n";
        System.setIn(new ByteArrayInputStream(input.getBytes(StandardCharsets.UTF_8)));
        try {
            ImportPipelineCli cli = new ImportPipelineCli();
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
}
