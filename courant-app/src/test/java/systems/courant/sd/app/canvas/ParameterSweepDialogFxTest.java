package systems.courant.sd.app.canvas;

import javafx.application.Platform;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DialogPane;
import javafx.scene.control.TextField;
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
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("ParameterSweepDialog (TestFX)")
@ExtendWith(ApplicationExtension.class)
class ParameterSweepDialogFxTest {

    private ParameterSweepDialog dialog;
    private DialogPane dialogPane;

    @Start
    void start(Stage stage) {
        stage.setScene(new Scene(new StackPane(), 200, 200));
        stage.show();
    }

    private void showDialog(List<String> constants, List<String> trackables) {
        Platform.runLater(() -> {
            dialog = new ParameterSweepDialog(constants, trackables);
            dialogPane = dialog.getDialogPane();
            dialog.show();
        });
        WaitForAsyncUtils.waitForFxEvents();
    }

    private Node okButton() {
        return dialogPane.lookupButton(
                dialogPane.getButtonTypes().stream()
                        .filter(bt -> bt.getButtonData().isDefaultButton())
                        .findFirst().orElseThrow());
    }

    @Test
    @DisplayName("Dialog opens with default values and OK enabled")
    void defaultValuesOkEnabled(FxRobot robot) {
        showDialog(List.of("alpha", "beta"), List.of("Population", "Revenue"));

        assertThat(okButton().isDisabled()).isFalse();

        // Verify combos have default selections
        Set<ComboBox> combos = robot.lookup(".combo-box").queryAllAs(ComboBox.class);
        assertThat(combos).isNotEmpty();
    }

    @Test
    @DisplayName("OK disabled when start > end")
    void okDisabledWhenStartGtEnd(FxRobot robot) {
        showDialog(List.of("alpha"), List.of("Pop"));

        // Find text fields — start(0), end(10), step(1)
        Set<TextField> fields = robot.lookup(".text-field").queryAllAs(TextField.class);
        // Find the field with "0" (start) and set it to 20
        TextField startField = fields.stream()
                .filter(f -> "0".equals(f.getText()))
                .findFirst().orElseThrow();
        robot.clickOn(startField).eraseText(1).write("20");
        WaitForAsyncUtils.waitForFxEvents();

        assertThat(okButton().isDisabled()).isTrue();
    }

    @Test
    @DisplayName("OK enabled when start equals end (single-point sweep)")
    void okEnabledWhenStartEqualsEnd(FxRobot robot) {
        showDialog(List.of("alpha"), List.of("Pop"));

        Set<TextField> fields = robot.lookup(".text-field").queryAllAs(TextField.class);
        // Set end to 0 so start == end == 0
        TextField endField = fields.stream()
                .filter(f -> "10".equals(f.getText()))
                .findFirst().orElseThrow();
        robot.clickOn(endField).eraseText(2).write("0");
        WaitForAsyncUtils.waitForFxEvents();

        assertThat(okButton().isDisabled()).isFalse();
    }

    @Test
    @DisplayName("OK disabled when step is zero")
    void okDisabledWhenStepZero(FxRobot robot) {
        showDialog(List.of("alpha"), List.of("Pop"));

        Set<TextField> fields = robot.lookup(".text-field").queryAllAs(TextField.class);
        TextField stepField = fields.stream()
                .filter(f -> "1".equals(f.getText()))
                .findFirst().orElseThrow();
        robot.clickOn(stepField).eraseText(1).write("0");
        WaitForAsyncUtils.waitForFxEvents();

        assertThat(okButton().isDisabled()).isTrue();
    }

    @Test
    @DisplayName("OK disabled when fields are non-numeric")
    void okDisabledWhenNonNumeric(FxRobot robot) {
        showDialog(List.of("alpha"), List.of("Pop"));

        Set<TextField> fields = robot.lookup(".text-field").queryAllAs(TextField.class);
        TextField startField = fields.stream()
                .filter(f -> "0".equals(f.getText()))
                .findFirst().orElseThrow();
        robot.clickOn(startField).eraseText(1).write("abc");
        WaitForAsyncUtils.waitForFxEvents();

        assertThat(okButton().isDisabled()).isTrue();
    }

    @Test
    @DisplayName("OK disabled when no constants provided")
    void okDisabledWhenNoCombos(FxRobot robot) {
        showDialog(List.of(), List.of("Pop"));

        assertThat(okButton().isDisabled()).isTrue();
    }
}
