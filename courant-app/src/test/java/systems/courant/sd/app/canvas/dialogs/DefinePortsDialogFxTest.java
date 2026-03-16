package systems.courant.sd.app.canvas.dialogs;

import systems.courant.sd.model.def.ModuleInterface;
import systems.courant.sd.model.def.PortDef;

import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.DialogPane;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
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

@DisplayName("DefinePortsDialog (TestFX)")
@ExtendWith(ApplicationExtension.class)
class DefinePortsDialogFxTest {

    private DialogPane dialogPane;
    private DefinePortsDialog dialog;

    @Start
    void start(Stage stage) {
        dialog = new DefinePortsDialog("TestModule", null);
        dialogPane = dialog.getDialogPane();
        stage.setScene(new Scene(new StackPane(dialogPane), 500, 600));
        stage.show();
    }

    @Test
    @DisplayName("Dialog title contains module name")
    void titleContainsModuleName(FxRobot robot) {
        assertThat(dialog.getTitle()).contains("TestModule");
    }

    @Test
    @DisplayName("Header text references module name")
    void headerTextContainsModuleName(FxRobot robot) {
        assertThat(dialog.getHeaderText()).contains("TestModule");
    }

    @Test
    @DisplayName("Dialog has Help, OK, and Cancel buttons")
    void hasHelpOkAndCancelButtons(FxRobot robot) {
        assertThat(dialogPane.getButtonTypes()).hasSize(3);
    }

    @Test
    @DisplayName("Empty dialog has no port rows initially")
    void emptyDialogNoPortRows(FxRobot robot) {
        // With null ModuleInterface, no port rows should exist
        List<TextField> textFields = robot.lookup(".text-field").queryAllAs(TextField.class)
                .stream().toList();
        assertThat(textFields).isEmpty();
    }

    @Test
    @DisplayName("Clicking Add Input Port creates a new row with text fields")
    void addInputPortCreatesRow(FxRobot robot) {
        Button addInput = robot.lookup("+ Add Input Port").queryAs(Button.class);
        Platform.runLater(() -> addInput.fire());
        WaitForAsyncUtils.waitForFxEvents();

        List<TextField> textFields = robot.lookup(".text-field").queryAllAs(TextField.class)
                .stream().toList();
        // Each row has 2 text fields: name and unit
        assertThat(textFields).hasSize(2);
    }

    @Test
    @DisplayName("Clicking Add Output Port creates a new row")
    void addOutputPortCreatesRow(FxRobot robot) {
        Button addOutput = robot.lookup("+ Add Output Port").queryAs(Button.class);
        Platform.runLater(() -> addOutput.fire());
        WaitForAsyncUtils.waitForFxEvents();

        List<TextField> textFields = robot.lookup(".text-field").queryAllAs(TextField.class)
                .stream().toList();
        assertThat(textFields).hasSize(2);
    }

    @Test
    @DisplayName("Adding multiple ports creates multiple rows")
    void multiplePortRows(FxRobot robot) {
        Button addInput = robot.lookup("+ Add Input Port").queryAs(Button.class);
        Button addOutput = robot.lookup("+ Add Output Port").queryAs(Button.class);
        Platform.runLater(() -> { addInput.fire(); addInput.fire(); addOutput.fire(); });
        WaitForAsyncUtils.waitForFxEvents();

        List<TextField> textFields = robot.lookup(".text-field").queryAllAs(TextField.class)
                .stream().toList();
        // 3 rows x 2 fields = 6
        assertThat(textFields).hasSize(6);
    }

    @Test
    @DisplayName("Remove button deletes its port row")
    void removeButtonDeletesRow(FxRobot robot) {
        Button addInput = robot.lookup("+ Add Input Port").queryAs(Button.class);
        Platform.runLater(() -> { addInput.fire(); addInput.fire(); });
        WaitForAsyncUtils.waitForFxEvents();

        // Should have 4 text fields (2 rows x 2)
        assertThat(robot.lookup(".text-field").queryAllAs(TextField.class)).hasSize(4);

        // Click the first remove button (the X button)
        Button removeBtn = robot.lookup("\u2715").queryAs(Button.class);
        Platform.runLater(() -> removeBtn.fire());
        WaitForAsyncUtils.waitForFxEvents();

        // Should now have 2 text fields (1 row)
        assertThat(robot.lookup(".text-field").queryAllAs(TextField.class)).hasSize(2);
    }
}
