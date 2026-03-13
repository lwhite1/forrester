package systems.courant.sd.app;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("AppVersion")
class AppVersionTest {

    @Test
    void shouldReturnNonNullVersion() {
        String version = AppVersion.get();

        assertThat(version).isNotNull();
    }

    @Test
    void shouldReturnNonEmptyVersion() {
        String version = AppVersion.get();

        assertThat(version).isNotEmpty();
    }

    @Test
    void shouldReturnConsistentValue() {
        String first = AppVersion.get();
        String second = AppVersion.get();

        assertThat(first).isEqualTo(second);
    }
}
