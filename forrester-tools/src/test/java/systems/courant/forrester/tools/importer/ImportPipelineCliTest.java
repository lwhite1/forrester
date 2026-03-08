package systems.courant.forrester.tools.importer;

import org.junit.jupiter.api.Test;

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
        assertThat(parsed.outputDir).isEqualTo("forrester-demos/src/main/java");
    }
}
