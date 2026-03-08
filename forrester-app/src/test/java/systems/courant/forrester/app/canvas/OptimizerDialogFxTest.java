package systems.courant.forrester.app.canvas;

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

@DisplayName("OptimizerDialog (TestFX)")
@ExtendWith(ApplicationExtension.class)
class OptimizerDialogFxTest {

    private OptimizerDialog dialog;
    private DialogPane dialogPane;

    @Start
    void start(Stage stage) {
        stage.setScene(new Scene(new StackPane(), 200, 200));
        stage.show();
    }

    private void showDialog(List<String> constants, List<String> stocks) {
        Platform.runLater(() -> {
            dialog = new OptimizerDialog(constants, stocks);
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
    @DisplayName("Dialog opens with defaults and OK enabled")
    void defaultsOkEnabled(FxRobot robot) {
        showDialog(List.of("alpha"), List.of("Population"));

        assertThat(okButton().isDisabled()).isFalse();
    }

    @Test
    @DisplayName("Default objective is MINIMIZE")
    void defaultObjective(FxRobot robot) {
        showDialog(List.of("alpha"), List.of("Pop"));

        Set<ComboBox> combos = robot.lookup(".combo-box").queryAllAs(ComboBox.class);
        boolean foundMinimize = combos.stream()
                .anyMatch(c -> OptimizerDialog.ObjectiveType.MINIMIZE.equals(c.getValue()));
        assertThat(foundMinimize).isTrue();
    }

    @Test
    @DisplayName("Default algorithm is NELDER_MEAD")
    void defaultAlgorithm(FxRobot robot) {
        showDialog(List.of("alpha"), List.of("Pop"));

        Set<ComboBox> combos = robot.lookup(".combo-box").queryAllAs(ComboBox.class);
        boolean foundNM = combos.stream()
                .anyMatch(c -> "NELDER_MEAD".equals(c.getValue()));
        assertThat(foundNM).isTrue();
    }

    @Test
    @DisplayName("Default max evaluations is 1000")
    void defaultMaxEvals(FxRobot robot) {
        showDialog(List.of("alpha"), List.of("Pop"));

        Set<TextField> fields = robot.lookup(".text-field").queryAllAs(TextField.class);
        boolean found1000 = fields.stream().anyMatch(f -> "1000".equals(f.getText()));
        assertThat(found1000).isTrue();
    }

    @Test
    @DisplayName("OK disabled when no stocks provided")
    void okDisabledWhenNoStocks(FxRobot robot) {
        showDialog(List.of("alpha"), List.of());

        assertThat(okButton().isDisabled()).isTrue();
    }

    @Test
    @DisplayName("OK disabled when max evaluations is zero")
    void okDisabledWhenMaxEvalsZero(FxRobot robot) {
        showDialog(List.of("alpha"), List.of("Pop"));

        Set<TextField> fields = robot.lookup(".text-field").queryAllAs(TextField.class);
        TextField maxEvalsField = fields.stream()
                .filter(f -> "1000".equals(f.getText()))
                .findFirst().orElseThrow();
        robot.clickOn(maxEvalsField).eraseText(4).write("0");
        WaitForAsyncUtils.waitForFxEvents();

        assertThat(okButton().isDisabled()).isTrue();
    }

    @Test
    @DisplayName("OK disabled when max evaluations is non-numeric")
    void okDisabledWhenMaxEvalsNonNumeric(FxRobot robot) {
        showDialog(List.of("alpha"), List.of("Pop"));

        Set<TextField> fields = robot.lookup(".text-field").queryAllAs(TextField.class);
        TextField maxEvalsField = fields.stream()
                .filter(f -> "1000".equals(f.getText()))
                .findFirst().orElseThrow();
        robot.clickOn(maxEvalsField).eraseText(4).write("abc");
        WaitForAsyncUtils.waitForFxEvents();

        assertThat(okButton().isDisabled()).isTrue();
    }
}
