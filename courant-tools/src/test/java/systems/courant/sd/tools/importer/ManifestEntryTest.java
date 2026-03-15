package systems.courant.sd.tools.importer;

import systems.courant.sd.model.ModelMetadata;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("ManifestEntry")
class ManifestEntryTest {

    private static final ModelMetadata METADATA = ModelMetadata.builder()
            .license("CC-BY-SA-4.0").build();

    @Nested
    @DisplayName("Validation")
    class Validation {

        @Test
        void shouldRejectNullUrl() {
            assertThatThrownBy(() -> new ManifestEntry(null, "Demo", null, null, null, METADATA))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("url");
        }

        @Test
        void shouldRejectBlankUrl() {
            assertThatThrownBy(() -> new ManifestEntry("  ", "Demo", null, null, null, METADATA))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("url");
        }

        @Test
        void shouldRejectNullClassName() {
            assertThatThrownBy(() -> new ManifestEntry("http://example.com/m.mdl", null, null, null, null, METADATA))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("className");
        }

        @Test
        void shouldRejectBlankClassName() {
            assertThatThrownBy(() -> new ManifestEntry("http://example.com/m.mdl", "", null, null, null, METADATA))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("className");
        }

        @Test
        void shouldRejectNullMetadata() {
            assertThatThrownBy(() -> new ManifestEntry("http://example.com/m.mdl", "Demo", null, null, null, null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("metadata");
        }
    }

    @Nested
    @DisplayName("Construction")
    class Construction {

        @Test
        void shouldCreateValidEntry() {
            ManifestEntry entry = new ManifestEntry(
                    "http://example.com/model.mdl", "SirDemo", "sir-epidemic",
                    "epidemiology", "SIR model", METADATA);

            assertThat(entry.url()).isEqualTo("http://example.com/model.mdl");
            assertThat(entry.className()).isEqualTo("SirDemo");
            assertThat(entry.id()).isEqualTo("sir-epidemic");
            assertThat(entry.category()).isEqualTo("epidemiology");
            assertThat(entry.comment()).isEqualTo("SIR model");
            assertThat(entry.metadata()).isEqualTo(METADATA);
        }

        @Test
        void shouldAcceptNullCategory() {
            ManifestEntry entry = new ManifestEntry(
                    "http://example.com/model.mdl", "Demo", null, null, null, METADATA);
            assertThat(entry.category()).isNull();
        }

        @Test
        void shouldAcceptNullComment() {
            ManifestEntry entry = new ManifestEntry(
                    "http://example.com/model.mdl", "Demo", null, null, null, METADATA);
            assertThat(entry.comment()).isNull();
        }
    }
}
