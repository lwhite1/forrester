package systems.courant.sd.tools.importer;

import systems.courant.sd.model.ModelMetadata;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

@DisplayName("ImportPipeline")
class ImportPipelineTest {

    @TempDir
    Path tempDir;

    @Test
    void shouldImportXmileModel() throws IOException {
        Path xmileFile = Path.of("../courant-engine/src/test/resources/xmile/sir.xmile")
                .toAbsolutePath().normalize();

        assumeTrue(Files.exists(xmileFile), "XMILE test fixture not available");

        ModelMetadata metadata = ModelMetadata.builder()
                .source("Kermack & McKendrick SIR model (1927)")
                .license("CC-BY-SA-4.0")
                .build();

        PipelineConfig config = new PipelineConfig(
                xmileFile, metadata, "epidemiology", "SirXmileDemo",
                tempDir, false, false);

        ImportPipeline pipeline = new ImportPipeline();
        PipelineResult result = pipeline.execute(config);

        assertThat(result.definition()).isNotNull();
        assertThat(result.generatedSource()).contains("public class SirXmileDemo");
        assertThat(result.generatedSource()).contains("package systems.courant.sd.demo.epidemiology");
        assertThat(result.outputFile()).isNotNull();
        assertThat(Files.exists(result.outputFile())).isTrue();
    }

    @Test
    void shouldImportVensimModel() throws IOException {
        Path mdlFile = Path.of("../courant-engine/src/test/resources/vensim/sir.mdl")
                .toAbsolutePath().normalize();

        assumeTrue(Files.exists(mdlFile), "Vensim test fixture not available");

        ModelMetadata metadata = ModelMetadata.builder()
                .license("CC-BY-SA-4.0")
                .build();

        PipelineConfig config = new PipelineConfig(
                mdlFile, metadata, null, "SirVensimDemo",
                tempDir, false, false);

        ImportPipeline pipeline = new ImportPipeline();
        PipelineResult result = pipeline.execute(config);

        assertThat(result.definition()).isNotNull();
        assertThat(result.generatedSource()).contains("public class SirVensimDemo");
        assertThat(result.generatedSource()).contains("package systems.courant.sd.demo;");
    }

    @Test
    void shouldSupportDryRun() throws IOException {
        Path xmileFile = Path.of("../courant-engine/src/test/resources/xmile/sir.xmile")
                .toAbsolutePath().normalize();

        assumeTrue(Files.exists(xmileFile), "XMILE test fixture not available");

        ModelMetadata metadata = ModelMetadata.builder()
                .license("CC-BY-SA-4.0")
                .build();

        PipelineConfig config = new PipelineConfig(
                xmileFile, metadata, null, "SirDryRunDemo",
                tempDir, true, false);

        PipelineResult result = new ImportPipeline().execute(config);

        assertThat(result.generatedSource()).isNotEmpty();
        assertThat(result.outputFile()).isNull();
    }

    @Test
    void shouldRefuseOverwriteByDefault() throws IOException {
        Path xmileFile = Path.of("../courant-engine/src/test/resources/xmile/sir.xmile")
                .toAbsolutePath().normalize();

        assumeTrue(Files.exists(xmileFile), "XMILE test fixture not available");

        ModelMetadata metadata = ModelMetadata.builder().license("CC-BY-SA-4.0").build();

        PipelineConfig config = new PipelineConfig(
                xmileFile, metadata, null, "SirOverwriteDemo",
                tempDir, false, false);

        ImportPipeline pipeline = new ImportPipeline();
        pipeline.execute(config);

        // Second run should fail
        assertThatThrownBy(() -> pipeline.execute(config))
                .isInstanceOf(IOException.class)
                .hasMessageContaining("already exists");
    }

    @Test
    void shouldAllowOverwriteWhenFlagSet() throws IOException {
        Path xmileFile = Path.of("../courant-engine/src/test/resources/xmile/sir.xmile")
                .toAbsolutePath().normalize();

        assumeTrue(Files.exists(xmileFile), "XMILE test fixture not available");

        ModelMetadata metadata = ModelMetadata.builder().license("CC-BY-SA-4.0").build();

        PipelineConfig firstConfig = new PipelineConfig(
                xmileFile, metadata, null, "SirOverwriteOkDemo",
                tempDir, false, false);

        ImportPipeline pipeline = new ImportPipeline();
        pipeline.execute(firstConfig);

        PipelineConfig overwriteConfig = new PipelineConfig(
                xmileFile, metadata, null, "SirOverwriteOkDemo",
                tempDir, false, true);

        PipelineResult result = pipeline.execute(overwriteConfig);
        assertThat(result.outputFile()).isNotNull();
    }

    @Test
    void shouldRejectUnsupportedExtension() {
        ModelMetadata metadata = ModelMetadata.builder().license("CC-BY-SA-4.0").build();

        PipelineConfig config = new PipelineConfig(
                Path.of("model.txt"), metadata, null, "TestDemo",
                tempDir, false, false);

        assertThatThrownBy(() -> new ImportPipeline().execute(config))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Unsupported file extension");
    }

    @Test
    void shouldRejectFileWithNoExtension() {
        ModelMetadata metadata = ModelMetadata.builder().license("CC-BY-SA-4.0").build();

        PipelineConfig config = new PipelineConfig(
                Path.of("model_without_extension"), metadata, null, "TestDemo",
                tempDir, false, false);

        assertThatThrownBy(() -> new ImportPipeline().execute(config))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("no extension");
    }

    @Test
    void shouldGenerateJsonWhenGenerateCodeIsFalse() throws IOException {
        Path xmileFile = Path.of("../courant-engine/src/test/resources/xmile/sir.xmile")
                .toAbsolutePath().normalize();

        assumeTrue(Files.exists(xmileFile), "XMILE test fixture not available");

        ModelMetadata metadata = ModelMetadata.builder()
                .license("CC-BY-SA-4.0")
                .build();

        PipelineConfig config = new PipelineConfig(
                xmileFile, metadata, null, "SirModel",
                tempDir, false, false, false);

        PipelineResult result = new ImportPipeline().execute(config);

        assertThat(result.generatedSource()).contains("\"name\"");
        assertThat(result.outputFile()).isNotNull();
        assertThat(result.outputFile().toString()).endsWith(".json");
        assertThat(Files.exists(result.outputFile())).isTrue();
    }

    @Test
    void shouldGenerateJsonDryRun() throws IOException {
        Path xmileFile = Path.of("../courant-engine/src/test/resources/xmile/sir.xmile")
                .toAbsolutePath().normalize();

        assumeTrue(Files.exists(xmileFile), "XMILE test fixture not available");

        ModelMetadata metadata = ModelMetadata.builder()
                .license("CC-BY-SA-4.0")
                .build();

        PipelineConfig config = new PipelineConfig(
                xmileFile, metadata, null, "SirModel",
                tempDir, true, false, false);

        PipelineResult result = new ImportPipeline().execute(config);

        assertThat(result.generatedSource()).isNotEmpty();
        assertThat(result.outputFile()).isNull();
    }

    @Nested
    @DisplayName("Package and path resolution")
    class PackageAndPathResolution {

        @Test
        void shouldResolvePackageNameWithCategory() {
            assertThat(ImportPipeline.resolvePackageName("epidemiology"))
                    .isEqualTo("systems.courant.sd.demo.epidemiology");
        }

        @Test
        void shouldResolvePackageNameWithoutCategory() {
            assertThat(ImportPipeline.resolvePackageName(null))
                    .isEqualTo("systems.courant.sd.demo");
        }

        @Test
        void shouldResolvePackageNameWithBlankCategory() {
            assertThat(ImportPipeline.resolvePackageName("  "))
                    .isEqualTo("systems.courant.sd.demo");
        }

        @Test
        void shouldFallBackToBasePackageWhenCategoryYieldsEmptySegment() {
            // A category like "!!!" strips to empty after toPackageSegment
            assertThat(ImportPipeline.resolvePackageName("!!!"))
                    .isEqualTo("systems.courant.sd.demo");
        }

        @Test
        void shouldResolveOutputPath() {
            Path result = ImportPipeline.resolveOutputPath(
                    Path.of("/src"), "systems.courant.sd.demo", "SirDemo");
            assertThat(result.toString()).contains("systems");
            assertThat(result.toString()).endsWith("SirDemo.java");
        }

        @Test
        void shouldResolveOutputPathWithCategory() {
            Path result = ImportPipeline.resolveOutputPath(
                    Path.of("/src"), "systems.courant.sd.demo.epidemiology", "SirDemo");
            assertThat(result.toString()).contains("epidemiology");
            assertThat(result.toString()).endsWith("SirDemo.java");
        }
    }
}
