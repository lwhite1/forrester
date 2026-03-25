package systems.courant.sd.tools.importer;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.net.URI;
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
        assertThat(parsed.outputDir).isEqualTo("courant-demos/src/main/java");
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

    @Nested
    @DisplayName("isRemoteUrl (#1059)")
    class IsRemoteUrl {

        @Test
        void shouldReturnTrueForHttpUrl() {
            assertThat(BatchImportCli.isRemoteUrl("http://example.com/model.mdl")).isTrue();
        }

        @Test
        void shouldReturnTrueForHttpsUrl() {
            assertThat(BatchImportCli.isRemoteUrl("https://example.com/model.mdl")).isTrue();
        }

        @Test
        void shouldReturnFalseForLocalPath() {
            assertThat(BatchImportCli.isRemoteUrl("/tmp/model.mdl")).isFalse();
        }

        @Test
        void shouldReturnFalseForRelativePath() {
            assertThat(BatchImportCli.isRemoteUrl("models/model.mdl")).isFalse();
        }
    }

    @Nested
    @DisplayName("rejectPrivateAddress SSRF prevention (#957)")
    class RejectPrivateAddress {

        @Test
        void shouldRejectLocalhostUrl() {
            assertThatThrownBy(() ->
                    BatchImportCli.rejectPrivateAddress(URI.create("http://localhost/model.mdl")))
                    .isInstanceOf(java.io.IOException.class)
                    .hasMessageContaining("private/reserved");
        }

        @Test
        void shouldRejectLoopbackIp() {
            assertThatThrownBy(() ->
                    BatchImportCli.rejectPrivateAddress(URI.create("http://127.0.0.1/model.mdl")))
                    .isInstanceOf(java.io.IOException.class)
                    .hasMessageContaining("private/reserved");
        }

        @Test
        void shouldRejectUrlWithNoHost() {
            assertThatThrownBy(() ->
                    BatchImportCli.rejectPrivateAddress(URI.create("http:///model.mdl")))
                    .isInstanceOf(java.io.IOException.class)
                    .hasMessageContaining("no host");
        }

        @Test
        void shouldAcceptPublicUrl() throws Exception {
            // google.com resolves to a public IP; should not throw
            BatchImportCli.rejectPrivateAddress(URI.create("https://google.com/model.mdl"));
        }
    }

    @Nested
    @DisplayName("downloadToTemp redirect safety (#1376)")
    class DownloadRedirectSafety {

        @Test
        void shouldHaveReasonableMaxRedirectLimit() {
            assertThat(BatchImportCli.MAX_REDIRECTS)
                    .as("max redirect hops should be positive and bounded")
                    .isBetween(1, 10);
        }

        @Test
        void shouldStillRejectPrivateAddressInInitialUrl() {
            assertThatThrownBy(() -> BatchImportCli.downloadToTemp("http://127.0.0.1/model.mdl"))
                    .isInstanceOf(IOException.class)
                    .hasMessageContaining("private/reserved");
        }

        @Test
        void shouldStillRejectLocalhostInInitialUrl() {
            assertThatThrownBy(() -> BatchImportCli.downloadToTemp("http://localhost/model.mdl"))
                    .isInstanceOf(IOException.class)
                    .hasMessageContaining("private/reserved");
        }
    }

    @Nested
    @DisplayName("parseArgs error paths (#333)")
    class ParseArgsErrorPaths {

        @Test
        void shouldThrowOnUnknownOption() {
            assertThatThrownBy(() -> BatchImportCli.parseArgs(
                    new String[]{"--bogus"}))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Unknown option")
                    .hasMessageContaining("--bogus");
        }

        @Test
        void shouldThrowWhenManifestMissingValue() {
            assertThatThrownBy(() -> BatchImportCli.parseArgs(
                    new String[]{"--manifest"}))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("--manifest")
                    .hasMessageContaining("requires a value");
        }

        @Test
        void shouldThrowWhenOutputDirMissingValue() {
            assertThatThrownBy(() -> BatchImportCli.parseArgs(
                    new String[]{"--output-dir"}))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("--output-dir")
                    .hasMessageContaining("requires a value");
        }

        @Test
        void shouldSetHelpRequestedOnHelpFlag() {
            BatchImportCli.CliArgs parsed = BatchImportCli.parseArgs(
                    new String[]{"--help"});
            assertThat(parsed.helpRequested).isTrue();
        }

        @Test
        void shouldSetHelpRequestedOnShortFlag() {
            BatchImportCli.CliArgs parsed = BatchImportCli.parseArgs(
                    new String[]{"-h"});
            assertThat(parsed.helpRequested).isTrue();
        }

        @Test
        void shouldReturnZeroForHelpViaRun() {
            BatchImportCli cli = new BatchImportCli();
            int exitCode = cli.run(new String[]{"--help"});
            assertThat(exitCode).isZero();
        }

        @Test
        void shouldReturnOneForUnknownOptionViaRun() {
            BatchImportCli cli = new BatchImportCli();
            int exitCode = cli.run(new String[]{"--bogus"});
            assertThat(exitCode).isEqualTo(1);
        }

        @Test
        void shouldReturnOneForMissingManifest() {
            BatchImportCli cli = new BatchImportCli();
            int exitCode = cli.run(new String[]{"--manifest", "nonexistent.json"});
            assertThat(exitCode).isEqualTo(1);
        }
    }
}
