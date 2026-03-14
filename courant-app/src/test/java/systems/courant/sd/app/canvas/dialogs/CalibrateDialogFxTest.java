package systems.courant.sd.app.canvas.dialogs;

import javafx.application.Platform;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DialogPane;
import javafx.scene.control.Label;
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

@DisplayName("CalibrateDialog (TestFX)")
@ExtendWith(ApplicationExtension.class)
class CalibrateDialogFxTest {

    private CalibrateDialog dialog;
    private DialogPane dialogPane;

    @Start
    void start(Stage stage) {
        stage.setScene(new Scene(new StackPane(), 200, 200));
        stage.show();
    }

    private void showDialog(List<String> constants, List<String> stocks) {
        Platform.runLater(() -> {
            dialog = new CalibrateDialog(constants, stocks);
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
    @DisplayName("Dialog opens with OK disabled (no CSV imported)")
    void okDisabledInitially(FxRobot robot) {
        showDialog(List.of("alpha"), List.of("Population"));

        assertThat(okButton().isDisabled()).isTrue();
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
    @DisplayName("Validation shows 'Import CSV observed data first' initially")
    void validationShowsImportMessage(FxRobot robot) {
        showDialog(List.of("alpha"), List.of("Population"));

        Label validationLabel = robot.lookup("#calibValidationLabel").queryAs(Label.class);
        assertThat(validationLabel.getText()).contains("Import CSV observed data first");
    }

    @Test
    @DisplayName("OK disabled when max evaluations is non-numeric")
    void okDisabledWhenMaxEvalsNonNumeric(FxRobot robot) {
        showDialog(List.of("alpha"), List.of("Pop"));

        TextField maxEvalsField = robot.lookup("#calibMaxEvals").queryAs(TextField.class);
        robot.clickOn(maxEvalsField).eraseText(4).write("abc");
        WaitForAsyncUtils.waitForFxEvents();

        assertThat(okButton().isDisabled()).isTrue();
    }

    @Test
    @DisplayName("Dataset label shows 'No data loaded' initially")
    void datasetLabelInitial(FxRobot robot) {
        showDialog(List.of("alpha"), List.of("Pop"));

        Label datasetLabel = robot.lookup("#calibDatasetLabel").queryAs(Label.class);
        assertThat(datasetLabel.getText()).isEqualTo("No data loaded");
    }
}
