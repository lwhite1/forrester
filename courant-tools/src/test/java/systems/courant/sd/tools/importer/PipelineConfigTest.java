package systems.courant.sd.tools.importer;

import systems.courant.sd.model.ModelMetadata;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("PipelineConfig")
class PipelineConfigTest {

    private static final Path SOURCE = Path.of("model.mdl");
    private static final Path OUTPUT = Path.of("/tmp/out");
    private static final ModelMetadata METADATA = ModelMetadata.builder()
            .license("CC-BY-SA-4.0").build();

    @Nested
    @DisplayName("Validation")
    class Validation {

        @Test
        void shouldRejectNullSourceFile() {
            assertThatThrownBy(() -> new PipelineConfig(
                    null, METADATA, null, "Demo", OUTPUT, false, false))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("sourceFile");
        }

        @Test
        void shouldRejectNullMetadata() {
            assertThatThrownBy(() -> new PipelineConfig(
                    SOURCE, null, null, "Demo", OUTPUT, false, false))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("metadata");
        }

        @Test
        void shouldRejectNullClassName() {
            assertThatThrownBy(() -> new PipelineConfig(
                    SOURCE, METADATA, null, null, OUTPUT, false, false))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("className");
        }

        @Test
        void shouldRejectBlankClassName() {
            assertThatThrownBy(() -> new PipelineConfig(
                    SOURCE, METADATA, null, "  ", OUTPUT, false, false))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("className");
        }

        @Test
        void shouldRejectClassNameStartingWithLowercase() {
            assertThatThrownBy(() -> new PipelineConfig(
                    SOURCE, METADATA, null, "demo", OUTPUT, false, false))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("className");
        }

        @Test
        void shouldRejectClassNameWithHyphen() {
            assertThatThrownBy(() -> new PipelineConfig(
                    SOURCE, METADATA, null, "My-Demo", OUTPUT, false, false))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("className");
        }

        @Test
        void shouldRejectClassNameWithSpaces() {
            assertThatThrownBy(() -> new PipelineConfig(
                    SOURCE, METADATA, null, "My Demo", OUTPUT, false, false))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("className");
        }

        @Test
        void shouldRejectClassNameWithDot() {
            assertThatThrownBy(() -> new PipelineConfig(
                    SOURCE, METADATA, null, "My.Demo", OUTPUT, false, false))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("className");
        }

        @Test
        void shouldAcceptClassNameWithUnderscore() {
            PipelineConfig config = new PipelineConfig(
                    SOURCE, METADATA, null, "My_Demo", OUTPUT, false, false);
            assertThat(config.className()).isEqualTo("My_Demo");
        }

        @Test
        void shouldRejectNullOutputDir() {
            assertThatThrownBy(() -> new PipelineConfig(
                    SOURCE, METADATA, null, "Demo", null, false, false))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("outputDir");
        }
    }

    @Nested
    @DisplayName("Construction")
    class Construction {

        @Test
        void shouldCreateValidConfig() {
            PipelineConfig config = new PipelineConfig(
                    SOURCE, METADATA, "ecology", "EcoDemo", OUTPUT, true, true);

            assertThat(config.sourceFile()).isEqualTo(SOURCE);
            assertThat(config.metadata()).isEqualTo(METADATA);
            assertThat(config.category()).isEqualTo("ecology");
            assertThat(config.className()).isEqualTo("EcoDemo");
            assertThat(config.outputDir()).isEqualTo(OUTPUT);
            assertThat(config.dryRun()).isTrue();
            assertThat(config.overwrite()).isTrue();
        }

        @Test
        void shouldAcceptNullCategory() {
            PipelineConfig config = new PipelineConfig(
                    SOURCE, METADATA, null, "Demo", OUTPUT, false, false);
            assertThat(config.category()).isNull();
        }

        @Test
        void shouldDefaultGenerateCodeToTrue() {
            PipelineConfig config = new PipelineConfig(
                    SOURCE, METADATA, null, "Demo", OUTPUT, false, false);
            assertThat(config.generateCode()).isTrue();
        }

        @Test
        void shouldAcceptExplicitGenerateCodeFalse() {
            PipelineConfig config = new PipelineConfig(
                    SOURCE, METADATA, null, "Demo", OUTPUT, false, false, false);
            assertThat(config.generateCode()).isFalse();
        }
    }
}
