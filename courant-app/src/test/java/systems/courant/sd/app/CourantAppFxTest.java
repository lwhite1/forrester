package systems.courant.sd.app;

import javafx.geometry.Rectangle2D;
import javafx.stage.Screen;
import javafx.stage.Stage;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.testfx.framework.junit5.ApplicationExtension;
import org.testfx.framework.junit5.Start;
import org.testfx.util.WaitForAsyncUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

@DisplayName("CourantApp window positioning (TestFX)")
@ExtendWith(ApplicationExtension.class)
class CourantAppFxTest {

    private Stage stage;
    private CourantApp app;

    @Start
    void start(Stage stage) {
        this.stage = stage;
        this.app = new CourantApp();
        app.start(stage);
    }

    @Test
    @DisplayName("window title bar and menu bar should be visible on screen after startup")
    void shouldPlaceWindowOnScreenAfterStartup() {
        WaitForAsyncUtils.waitForFxEvents();

        Rectangle2D primaryBounds = Screen.getPrimary().getVisualBounds();

        assertThat(stage.getY())
                .as("window content Y should be within the visible screen area")
                .isGreaterThanOrEqualTo(primaryBounds.getMinY());

        assertThat(stage.getX())
                .as("window X should be within the visible screen area")
                .isGreaterThanOrEqualTo(primaryBounds.getMinX());
    }

    @Test
    @DisplayName("should reposition window to primary screen when moved far offscreen")
    void shouldRepositionWindowWhenTitleBarIsOffscreen() {
        WaitForAsyncUtils.waitForFxEvents();

        WaitForAsyncUtils.asyncFx(() -> {
            stage.setX(-5000);
            stage.setY(-5000);
        });
        WaitForAsyncUtils.waitForFxEvents();

        WaitForAsyncUtils.asyncFx(() -> app.ensureWindowOnScreen(stage));
        WaitForAsyncUtils.waitForFxEvents();

        Rectangle2D primaryBounds = Screen.getPrimary().getVisualBounds();

        assertThat(stage.getX())
                .as("X should be repositioned onto the primary screen")
                .isGreaterThanOrEqualTo(primaryBounds.getMinX());
        assertThat(stage.getY())
                .as("Y should be repositioned onto the primary screen")
                .isGreaterThanOrEqualTo(primaryBounds.getMinY());
    }

    @Test
    @DisplayName("should not reposition window when it is already well within the visible area")
    void shouldNotRepositionWindowWhenTitleBarIsVisible() {
        WaitForAsyncUtils.waitForFxEvents();

        Rectangle2D primaryBounds = Screen.getPrimary().getVisualBounds();
        double targetX = primaryBounds.getMinX() + 100;
        double targetY = primaryBounds.getMinY() + 100;

        WaitForAsyncUtils.asyncFx(() -> {
            stage.setX(targetX);
            stage.setY(targetY);
        });
        WaitForAsyncUtils.waitForFxEvents();

        WaitForAsyncUtils.asyncFx(() -> app.ensureWindowOnScreen(stage));
        WaitForAsyncUtils.waitForFxEvents();

        assertThat(stage.getX()).isCloseTo(targetX, within(1.0));
        assertThat(stage.getY()).isCloseTo(targetY, within(1.0));
    }

    @Test
    @DisplayName("centerOnPrimaryScreen should place window within screen bounds")
    void shouldCenterOnPrimaryScreen() {
        WaitForAsyncUtils.waitForFxEvents();

        WaitForAsyncUtils.asyncFx(() -> app.centerOnPrimaryScreen(stage));
        WaitForAsyncUtils.waitForFxEvents();

        Rectangle2D primaryBounds = Screen.getPrimary().getVisualBounds();

        assertThat(stage.getX())
                .as("centered X should be within screen bounds")
                .isGreaterThanOrEqualTo(primaryBounds.getMinX());
        assertThat(stage.getY())
                .as("centered Y should be within screen bounds")
                .isGreaterThanOrEqualTo(primaryBounds.getMinY());
    }

    @Test
    @DisplayName("ensureWindowOnScreen should handle NaN stage coordinates gracefully")
    void shouldHandleNaNCoordinates() {
        WaitForAsyncUtils.waitForFxEvents();

        WaitForAsyncUtils.asyncFx(() -> {
            stage.setX(Double.NaN);
            stage.setY(Double.NaN);
        });
        WaitForAsyncUtils.waitForFxEvents();

        WaitForAsyncUtils.asyncFx(() -> app.ensureWindowOnScreen(stage));
        WaitForAsyncUtils.waitForFxEvents();

        Rectangle2D primaryBounds = Screen.getPrimary().getVisualBounds();

        assertThat(stage.getX())
                .as("X should be repositioned after NaN")
                .isGreaterThanOrEqualTo(primaryBounds.getMinX());
        assertThat(stage.getY())
                .as("Y should be repositioned after NaN")
                .isGreaterThanOrEqualTo(primaryBounds.getMinY());
        assertThat(Double.isNaN(stage.getX())).isFalse();
        assertThat(Double.isNaN(stage.getY())).isFalse();
    }
}
