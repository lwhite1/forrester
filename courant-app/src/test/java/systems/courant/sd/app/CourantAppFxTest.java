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
    @DisplayName("window should be within screen bounds after startup")
    void shouldPlaceWindowOnScreenAfterStartup() {
        WaitForAsyncUtils.waitForFxEvents();

        Rectangle2D titleBar = new Rectangle2D(
                stage.getX(), stage.getY(), stage.getWidth(), 30);

        boolean onScreen = Screen.getScreens().stream()
                .map(Screen::getVisualBounds)
                .anyMatch(bounds -> bounds.intersects(titleBar));

        assertThat(onScreen)
                .as("title bar should intersect at least one screen's visual bounds")
                .isTrue();
    }

    @Test
    @DisplayName("should reposition window to primary screen when title bar is offscreen")
    void shouldRepositionWindowWhenTitleBarIsOffscreen() {
        WaitForAsyncUtils.waitForFxEvents();

        // Move the window far offscreen (above and to the left of any display)
        WaitForAsyncUtils.asyncFx(() -> {
            stage.setX(-5000);
            stage.setY(-5000);
        });
        WaitForAsyncUtils.waitForFxEvents();

        // Call ensureWindowOnScreen
        WaitForAsyncUtils.asyncFx(() -> app.ensureWindowOnScreen(stage));
        WaitForAsyncUtils.waitForFxEvents();

        Rectangle2D primaryBounds = Screen.getPrimary().getVisualBounds();

        assertThat(stage.getX()).isGreaterThanOrEqualTo(primaryBounds.getMinX());
        assertThat(stage.getY()).isGreaterThanOrEqualTo(primaryBounds.getMinY());
        assertThat(stage.getX() + stage.getWidth())
                .isLessThanOrEqualTo(primaryBounds.getMaxX() + 1);
        assertThat(stage.getY() + stage.getHeight())
                .isLessThanOrEqualTo(primaryBounds.getMaxY() + 1);
    }

    @Test
    @DisplayName("should not reposition window when title bar is visible")
    void shouldNotRepositionWindowWhenTitleBarIsVisible() {
        WaitForAsyncUtils.waitForFxEvents();

        // Place window at a known on-screen position
        Rectangle2D primaryBounds = Screen.getPrimary().getVisualBounds();
        double targetX = primaryBounds.getMinX() + 50;
        double targetY = primaryBounds.getMinY() + 50;

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
}
