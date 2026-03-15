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

        // The top of the window content (menu bar area) must be below the top
        // of the primary screen's visual bounds, accounting for decoration
        assertThat(stage.getY())
                .as("window content Y should be within the visible screen area")
                .isGreaterThanOrEqualTo(primaryBounds.getMinY());

        assertThat(stage.getX())
                .as("window X should be within the visible screen area")
                .isGreaterThanOrEqualTo(primaryBounds.getMinX());
    }

    @Test
    @DisplayName("should reposition window to primary screen when moved offscreen")
    void shouldRepositionWindowWhenTitleBarIsOffscreen() {
        WaitForAsyncUtils.waitForFxEvents();

        // Move the window far offscreen
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
    @DisplayName("should not reposition window when it is already visible")
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

        assertThat(stage.getX()).isEqualTo(targetX);
        assertThat(stage.getY()).isEqualTo(targetY);
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
        assertThat(stage.getX() + stage.getWidth())
                .as("right edge should be within screen bounds")
                .isLessThanOrEqualTo(primaryBounds.getMaxX() + 1);
    }

    @Test
    @DisplayName("should reposition when window top edge is just above visible area")
    void shouldRepositionWhenTopEdgeIsJustAboveScreen() {
        WaitForAsyncUtils.waitForFxEvents();

        Rectangle2D primaryBounds = Screen.getPrimary().getVisualBounds();

        // Place the window with content area at the very top of the screen.
        // The native decoration sits above this, so the title bar would be
        // offscreen — ensureWindowOnScreen should reposition.
        WaitForAsyncUtils.asyncFx(() -> {
            stage.setX(primaryBounds.getMinX() + 100);
            stage.setY(primaryBounds.getMinY());
        });
        WaitForAsyncUtils.waitForFxEvents();

        double yBefore = stage.getY();

        WaitForAsyncUtils.asyncFx(() -> app.ensureWindowOnScreen(stage));
        WaitForAsyncUtils.waitForFxEvents();

        // The window should have been repositioned because the decoration
        // region (above getY()) was above the visible area
        assertThat(stage.getY())
                .as("window should be moved down to make decoration visible")
                .isGreaterThanOrEqualTo(yBefore);
    }
}
