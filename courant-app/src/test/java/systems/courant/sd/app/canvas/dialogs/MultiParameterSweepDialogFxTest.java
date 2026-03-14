package systems.courant.sd.app.canvas.dialogs;

import javafx.application.Platform;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
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

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("MultiParameterSweepDialog (TestFX)")
@ExtendWith(ApplicationExtension.class)
class MultiParameterSweepDialogFxTest {

    private MultiParameterSweepDialog dialog;
    private DialogPane dialogPane;

    @Start
    void start(Stage stage) {
        stage.setScene(new Scene(new StackPane(), 200, 200));
        stage.show();
    }

    private void showDialog(List<String> constants) {
        Platform.runLater(() -> {
            dialog = new MultiParameterSweepDialog(constants);
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
    @DisplayName("Dialog opens with two default rows and OK enabled")
    void defaultTwoRowsOkEnabled(FxRobot robot) {
        showDialog(List.of("alpha", "beta"));

        assertThat(okButton().isDisabled()).isFalse();
    }

    @Test
    @DisplayName("Combination count label shows correct default count")
    void combinationCountDefault(FxRobot robot) {
        showDialog(List.of("alpha", "beta"));

        Label countLabel = robot.lookup("#multiSweepCombinationCount").queryAs(Label.class);
        // Each row: start=0, end=10, step=1 → 11 points. 11 * 11 = 121
        assertThat(countLabel.getText()).isEqualTo("121 combinations");
    }

    @Test
    @DisplayName("Combination count updates when step changes")
    void combinationCountUpdatesOnStepChange(FxRobot robot) {
        showDialog(List.of("alpha", "beta"));

        // Change step of first row from 1 to 5
        TextField step0 = robot.lookup("#multiSweepStep0").queryAs(TextField.class);
        robot.clickOn(step0).eraseText(1).write("5");
        WaitForAsyncUtils.waitForFxEvents();

        Label countLabel = robot.lookup("#multiSweepCombinationCount").queryAs(Label.class);
        // Row 0: start=0, end=10, step=5 → 3 points. Row 1: 11 points. 3 * 11 = 33
        assertThat(countLabel.getText()).isEqualTo("33 combinations");
    }

    @Test
    @DisplayName("OK disabled with fewer than 2 valid parameters")
    void okDisabledWithOneConstant(FxRobot robot) {
        // Only 1 constant means both rows get same name → still 2 valid rows
        // but with only 1 constant name available, both select "alpha"
        // Let's test with empty constants list so no rows can be valid
        showDialog(List.of());

        assertThat(okButton().isDisabled()).isTrue();
    }

    @Test
    @DisplayName("Add parameter button exists")
    void addParamButton(FxRobot robot) {
        showDialog(List.of("alpha", "beta"));

        Node addBtn = robot.lookup("#multiSweepAddParam").query();
        assertThat(addBtn).isNotNull();
    }

    @Test
    @DisplayName("Default field values are 0, 10, 1")
    void defaultFieldValues(FxRobot robot) {
        showDialog(List.of("alpha", "beta"));

        TextField start0 = robot.lookup("#multiSweepStart0").queryAs(TextField.class);
        TextField end0 = robot.lookup("#multiSweepEnd0").queryAs(TextField.class);
        TextField step0 = robot.lookup("#multiSweepStep0").queryAs(TextField.class);

        assertThat(start0.getText()).isEqualTo("0");
        assertThat(end0.getText()).isEqualTo("10");
        assertThat(step0.getText()).isEqualTo("1");
    }

    @Test
    @DisplayName("No event filter alerts on OK button (#407)")
    void noEventFilterOnOkButton(FxRobot robot) {
        showDialog(List.of("alpha", "beta"));

        Button ok = (Button) okButton();
        // Validation is handled solely by the disable binding — no event filters should be present
        assertThat(ok.isDisabled()).isFalse();
        assertThat(ok.disableProperty().isBound()).isTrue();
    }

    @Test
    @DisplayName("Validation label shows message when fewer than 2 valid params (#407)")
    void validationLabelShowsMessageWhenInvalid(FxRobot robot) {
        showDialog(List.of());

        Label validationLabel = robot.lookup("#multiSweepValidationLabel").queryAs(Label.class);
        assertThat(validationLabel.getText()).isNotEmpty();
        assertThat(okButton().isDisabled()).isTrue();
    }

    @Test
    @DisplayName("Combination count shows 0 when steps are non-numeric")
    void combinationCountZeroWhenStepNonNumeric(FxRobot robot) {
        showDialog(List.of("alpha", "beta"));

        // Corrupt both rows' step fields
        TextField step0 = robot.lookup("#multiSweepStep0").queryAs(TextField.class);
        TextField step1 = robot.lookup("#multiSweepStep1").queryAs(TextField.class);
        robot.clickOn(step0).eraseText(1).write("x");
        robot.clickOn(step1).eraseText(1).write("y");
        WaitForAsyncUtils.waitForFxEvents();

        Label countLabel = robot.lookup("#multiSweepCombinationCount").queryAs(Label.class);
        assertThat(countLabel.getText()).isEqualTo("0 combinations");
    }
}
