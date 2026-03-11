package systems.courant.shrewd.app.canvas;

import javafx.scene.Scene;
import javafx.scene.control.ToggleButton;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.testfx.api.FxRobot;
import org.testfx.framework.junit5.ApplicationExtension;
import org.testfx.framework.junit5.Start;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Comment Element (TestFX)")
@ExtendWith(ApplicationExtension.class)
class CommentElementFxTest {

    private CanvasToolBar toolBar;

    @Start
    void start(Stage stage) {
        toolBar = new CanvasToolBar();
        stage.setScene(new Scene(new StackPane(toolBar), 1400, 100));
        stage.show();
    }

    @Test
    @DisplayName("Comment button exists and activates PLACE_COMMENT tool")
    void clickCommentButton(FxRobot robot) {
        robot.clickOn("#toolComment");
        assertThat(toolBar.getActiveTool()).isEqualTo(CanvasToolBar.Tool.PLACE_COMMENT);
    }

    @Test
    @DisplayName("Comment button is mutually exclusive with other tools")
    void commentButtonMutuallyExclusive(FxRobot robot) {
        robot.clickOn("#toolStock");
        assertThat(toolBar.getActiveTool()).isEqualTo(CanvasToolBar.Tool.PLACE_STOCK);

        robot.clickOn("#toolComment");
        assertThat(toolBar.getActiveTool()).isEqualTo(CanvasToolBar.Tool.PLACE_COMMENT);

        ToggleButton stockBtn = robot.lookup("#toolStock").queryAs(ToggleButton.class);
        assertThat(stockBtn.isSelected()).isFalse();
    }

    @Test
    @DisplayName("Comment tool has correct label for undo history")
    void commentToolLabel() {
        assertThat(CanvasToolBar.Tool.PLACE_COMMENT.label()).isEqualTo("comment");
    }
}
