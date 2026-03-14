package systems.courant.sd.app.canvas;

import javafx.stage.Stage;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.testfx.framework.junit5.ApplicationExtension;
import org.testfx.framework.junit5.Start;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Styles")
@ExtendWith(ApplicationExtension.class)
class StylesTest {

    @Start
    void start(Stage stage) {
        // JavaFX toolkit must be initialized for Screen.getPrimary()
    }

    @Nested
    @DisplayName("screenAwareWidth")
    class ScreenAwareWidth {

        @Test
        @DisplayName("returns desired width when it fits on screen")
        void returnsDesiredWhenSmall() {
            double result = Styles.screenAwareWidth(100);
            assertThat(result).isEqualTo(100);
        }

        @Test
        @DisplayName("clamps to 80% of screen width when desired exceeds screen")
        void clampsToScreenBound() {
            double result = Styles.screenAwareWidth(100_000);
            assertThat(result).isLessThan(100_000);
            assertThat(result).isGreaterThan(0);
        }

        @Test
        @DisplayName("returns positive value")
        void returnsPositive() {
            assertThat(Styles.screenAwareWidth(520)).isGreaterThan(0);
        }
    }

    @Nested
    @DisplayName("screenAwareHeight")
    class ScreenAwareHeight {

        @Test
        @DisplayName("returns desired height when it fits on screen")
        void returnsDesiredWhenSmall() {
            double result = Styles.screenAwareHeight(100);
            assertThat(result).isEqualTo(100);
        }

        @Test
        @DisplayName("clamps to 80% of screen height when desired exceeds screen")
        void clampsToScreenBound() {
            double result = Styles.screenAwareHeight(100_000);
            assertThat(result).isLessThan(100_000);
            assertThat(result).isGreaterThan(0);
        }
    }

    @Nested
    @DisplayName("CONFIG_DIALOG_WIDTH")
    class ConfigDialogWidth {

        @Test
        @DisplayName("is 520 pixels")
        void is520() {
            assertThat(Styles.CONFIG_DIALOG_WIDTH).isEqualTo(520);
        }
    }
}
