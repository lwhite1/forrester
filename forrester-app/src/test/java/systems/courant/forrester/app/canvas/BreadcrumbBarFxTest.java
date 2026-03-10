package systems.courant.forrester.app.canvas;

import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.testfx.api.FxRobot;
import org.testfx.framework.junit5.ApplicationExtension;
import org.testfx.framework.junit5.Start;
import org.testfx.util.WaitForAsyncUtils;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("BreadcrumbBar (TestFX)")
@ExtendWith(ApplicationExtension.class)
class BreadcrumbBarFxTest {

    private BreadcrumbBar breadcrumbBar;

    @Start
    void start(Stage stage) {
        breadcrumbBar = new BreadcrumbBar();
        stage.setScene(new Scene(new StackPane(breadcrumbBar), 600, 50));
        stage.show();
    }

    @Test
    @DisplayName("Hidden when at root level (single path segment)")
    void hiddenAtRoot(FxRobot robot) {
        Platform.runLater(() -> breadcrumbBar.update(List.of("Root")));
        WaitForAsyncUtils.waitForFxEvents();

        assertThat(breadcrumbBar.isVisible()).isFalse();
    }

    @Test
    @DisplayName("Hidden when path is null")
    void hiddenWhenNull(FxRobot robot) {
        Platform.runLater(() -> breadcrumbBar.update(null));
        WaitForAsyncUtils.waitForFxEvents();

        assertThat(breadcrumbBar.isVisible()).isFalse();
    }

    @Test
    @DisplayName("Visible when path has multiple segments")
    void visibleWhenNested(FxRobot robot) {
        Platform.runLater(() -> breadcrumbBar.update(List.of("Root", "Module A")));
        WaitForAsyncUtils.waitForFxEvents();

        assertThat(breadcrumbBar.isVisible()).isTrue();
    }

    @Test
    @DisplayName("Shows current level as non-clickable label")
    void currentLevelLabel(FxRobot robot) {
        Platform.runLater(() -> breadcrumbBar.update(List.of("Root", "Module A")));
        WaitForAsyncUtils.waitForFxEvents();

        Label current = robot.lookup("#breadcrumbCurrent").queryAs(Label.class);
        assertThat(current.getText()).isEqualTo("Module A");
    }

    @Test
    @DisplayName("Shows ancestor as clickable button")
    void ancestorButton(FxRobot robot) {
        Platform.runLater(() -> breadcrumbBar.update(List.of("Root", "Module A")));
        WaitForAsyncUtils.waitForFxEvents();

        Button ancestorBtn = robot.lookup(".button").queryAs(Button.class);
        assertThat(ancestorBtn.getText()).isEqualTo("Root");
    }

    @Test
    @DisplayName("Deep path shows all ancestors and current")
    void deepPath(FxRobot robot) {
        Platform.runLater(() -> breadcrumbBar.update(
                List.of("Root", "Level 1", "Level 2")));
        WaitForAsyncUtils.waitForFxEvents();

        List<Button> buttons = robot.lookup(".button").queryAllAs(Button.class)
                .stream().toList();
        assertThat(buttons).hasSize(2);
        assertThat(buttons.get(0).getText()).isEqualTo("Root");
        assertThat(buttons.get(1).getText()).isEqualTo("Level 1");

        Label current = robot.lookup("#breadcrumbCurrent").queryAs(Label.class);
        assertThat(current.getText()).isEqualTo("Level 2");
    }

    @Test
    @DisplayName("Clicking ancestor fires navigation to correct depth")
    void navigateToAncestor(FxRobot robot) {
        int[] navigatedDepth = {-1};
        breadcrumbBar.setOnNavigateTo(depth -> navigatedDepth[0] = depth);

        Platform.runLater(() -> breadcrumbBar.update(
                List.of("Root", "Level 1", "Level 2")));
        WaitForAsyncUtils.waitForFxEvents();

        List<Button> buttons = robot.lookup(".button").queryAllAs(Button.class)
                .stream().toList();
        robot.clickOn(buttons.get(0));

        assertThat(navigatedDepth[0]).isEqualTo(0);
    }

    @Test
    @DisplayName("Clicking deeper ancestor fires correct depth")
    void navigateToDeeperAncestor(FxRobot robot) {
        int[] navigatedDepth = {-1};
        breadcrumbBar.setOnNavigateTo(depth -> navigatedDepth[0] = depth);

        Platform.runLater(() -> breadcrumbBar.update(
                List.of("Root", "Level 1", "Level 2")));
        WaitForAsyncUtils.waitForFxEvents();

        List<Button> buttons = robot.lookup(".button").queryAllAs(Button.class)
                .stream().toList();
        robot.clickOn(buttons.get(1));

        assertThat(navigatedDepth[0]).isEqualTo(1);
    }
}
