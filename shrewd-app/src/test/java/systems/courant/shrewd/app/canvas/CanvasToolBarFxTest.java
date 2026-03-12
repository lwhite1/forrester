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

@DisplayName("CanvasToolBar (TestFX)")
@ExtendWith(ApplicationExtension.class)
class CanvasToolBarFxTest {

    private CanvasToolBar toolBar;

    @Start
    void start(Stage stage) {
        toolBar = new CanvasToolBar();
        stage.setScene(new Scene(new StackPane(toolBar), 1400, 100));
        stage.show();
    }

    @Test
    @DisplayName("Select tool is active by default")
    void selectToolActiveByDefault(FxRobot robot) {
        assertThat(toolBar.getActiveTool()).isEqualTo(CanvasToolBar.Tool.SELECT);
        ToggleButton selectBtn = robot.lookup("#toolSelect").queryAs(ToggleButton.class);
        assertThat(selectBtn.isSelected()).isTrue();
    }

    @Test
    @DisplayName("Clicking Stock button activates PLACE_STOCK tool")
    void clickStockButton(FxRobot robot) {
        robot.clickOn("#toolStock");
        assertThat(toolBar.getActiveTool()).isEqualTo(CanvasToolBar.Tool.PLACE_STOCK);
    }

    @Test
    @DisplayName("Clicking Flow button activates PLACE_FLOW tool")
    void clickFlowButton(FxRobot robot) {
        robot.clickOn("#toolFlow");
        assertThat(toolBar.getActiveTool()).isEqualTo(CanvasToolBar.Tool.PLACE_FLOW);
    }

    @Test
    @DisplayName("Clicking Auxiliary button activates PLACE_VARIABLE tool")
    void clickAuxButton(FxRobot robot) {
        robot.clickOn("#toolAux");
        assertThat(toolBar.getActiveTool()).isEqualTo(CanvasToolBar.Tool.PLACE_VARIABLE);
    }

    @Test
    @DisplayName("Clicking Module button activates PLACE_MODULE tool")
    void clickModuleButton(FxRobot robot) {
        robot.clickOn("#toolModule");
        assertThat(toolBar.getActiveTool()).isEqualTo(CanvasToolBar.Tool.PLACE_MODULE);
    }

    @Test
    @DisplayName("Clicking Lookup button activates PLACE_LOOKUP tool")
    void clickLookupButton(FxRobot robot) {
        robot.clickOn("#toolLookup");
        assertThat(toolBar.getActiveTool()).isEqualTo(CanvasToolBar.Tool.PLACE_LOOKUP);
    }

    @Test
    @DisplayName("Only one tool button is selected at a time")
    void mutuallyExclusiveSelection(FxRobot robot) {
        robot.clickOn("#toolStock");
        ToggleButton selectBtn = robot.lookup("#toolSelect").queryAs(ToggleButton.class);
        ToggleButton stockBtn = robot.lookup("#toolStock").queryAs(ToggleButton.class);
        assertThat(selectBtn.isSelected()).isFalse();
        assertThat(stockBtn.isSelected()).isTrue();

        robot.clickOn("#toolFlow");
        ToggleButton flowBtn = robot.lookup("#toolFlow").queryAs(ToggleButton.class);
        assertThat(stockBtn.isSelected()).isFalse();
        assertThat(flowBtn.isSelected()).isTrue();
    }

    @Test
    @DisplayName("Deselecting active tool reverts to SELECT")
    void deselectRevertsToSelect(FxRobot robot) {
        robot.clickOn("#toolStock");
        assertThat(toolBar.getActiveTool()).isEqualTo(CanvasToolBar.Tool.PLACE_STOCK);

        robot.clickOn("#toolStock");
        assertThat(toolBar.getActiveTool()).isEqualTo(CanvasToolBar.Tool.SELECT);
    }

    @Test
    @DisplayName("Tool change callback fires on click")
    void toolChangeCallbackFires(FxRobot robot) {
        CanvasToolBar.Tool[] received = {null};
        toolBar.setOnToolChanged(tool -> received[0] = tool);

        robot.clickOn("#toolFlow");
        assertThat(received[0]).isEqualTo(CanvasToolBar.Tool.PLACE_FLOW);
    }

    @Test
    @DisplayName("Loop toggle callback fires on click")
    void loopToggleCallbackFires(FxRobot robot) {
        Boolean[] received = {null};
        toolBar.setOnLoopToggleChanged(active -> received[0] = active);

        robot.clickOn("#toolLoops");
        assertThat(received[0]).isTrue();

        robot.clickOn("#toolLoops");
        assertThat(received[0]).isFalse();
    }

    @Test
    @DisplayName("Validate callback fires on click")
    void validateCallbackFires(FxRobot robot) {
        boolean[] fired = {false};
        toolBar.setOnValidateClicked(() -> fired[0] = true);

        robot.clickOn("#toolValidate");
        assertThat(fired[0]).isTrue();
    }

    @Test
    @DisplayName("Loops toggle does not affect tool selection")
    void loopsToggleIndependent(FxRobot robot) {
        robot.clickOn("#toolStock");
        assertThat(toolBar.getActiveTool()).isEqualTo(CanvasToolBar.Tool.PLACE_STOCK);

        robot.clickOn("#toolLoops");
        assertThat(toolBar.getActiveTool()).isEqualTo(CanvasToolBar.Tool.PLACE_STOCK);
    }
}
