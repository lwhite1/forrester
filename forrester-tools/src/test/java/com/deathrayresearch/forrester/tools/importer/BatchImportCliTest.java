package com.deathrayresearch.forrester.tools.importer;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class BatchImportCliTest {

    @Test
    void shouldParseAllFlags() {
        String[] args = {
                "--manifest", "models.json",
                "--output-dir", "/tmp/out",
                "--json-only",
                "--dry-run",
                "--overwrite"
        };

        BatchImportCli.CliArgs parsed = BatchImportCli.parseArgs(args);

        assertThat(parsed.manifestFile).isEqualTo("models.json");
        assertThat(parsed.outputDir).isEqualTo("/tmp/out");
        assertThat(parsed.jsonOnly).isTrue();
        assertThat(parsed.dryRun).isTrue();
        assertThat(parsed.overwrite).isTrue();
    }

    @Test
    void shouldDefaultFlags() {
        BatchImportCli.CliArgs parsed = BatchImportCli.parseArgs(new String[]{});
        assertThat(parsed.manifestFile).isNull();
        assertThat(parsed.outputDir).isEqualTo("forrester-demos/src/main/java");
        assertThat(parsed.jsonOnly).isFalse();
        assertThat(parsed.dryRun).isFalse();
        assertThat(parsed.overwrite).isFalse();
    }

    @Test
    void shouldReadManifest(@TempDir Path tempDir) throws IOException {
        String json = """
                [
                  {
                    "url": "https://example.com/model.mdl",
                    "className": "TestDemo",
                    "category": "ecology",
                    "comment": "A test model",
                    "metadata": {
                      "author": "Test Author",
                      "source": "Test Source",
                      "license": "CC-BY-SA-4.0",
                      "url": "https://example.com"
                    }
                  }
                ]
                """;
        Path manifest = tempDir.resolve("manifest.json");
        Files.writeString(manifest, json);

        BatchImportCli cli = new BatchImportCli();
        List<ManifestEntry> entries = cli.readManifest(manifest);

        assertThat(entries).hasSize(1);
        ManifestEntry entry = entries.getFirst();
        assertThat(entry.url()).isEqualTo("https://example.com/model.mdl");
        assertThat(entry.className()).isEqualTo("TestDemo");
        assertThat(entry.category()).isEqualTo("ecology");
        assertThat(entry.comment()).isEqualTo("A test model");
        assertThat(entry.metadata().author()).isEqualTo("Test Author");
        assertThat(entry.metadata().license()).isEqualTo("CC-BY-SA-4.0");
    }

    @Test
    void shouldRejectManifestWithoutLicense(@TempDir Path tempDir) throws IOException {
        String json = """
                [
                  {
                    "url": "https://example.com/model.mdl",
                    "className": "TestDemo",
                    "metadata": {
                      "author": "Test Author"
                    }
                  }
                ]
                """;
        Path manifest = tempDir.resolve("manifest.json");
        Files.writeString(manifest, json);

        BatchImportCli cli = new BatchImportCli();
        assertThatThrownBy(() -> cli.readManifest(manifest))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("license");
    }

    @Test
    void shouldRejectNonArrayManifest(@TempDir Path tempDir) throws IOException {
        Path manifest = tempDir.resolve("manifest.json");
        Files.writeString(manifest, "{}");

        BatchImportCli cli = new BatchImportCli();
        assertThatThrownBy(() -> cli.readManifest(manifest))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("JSON array");
    }
}
