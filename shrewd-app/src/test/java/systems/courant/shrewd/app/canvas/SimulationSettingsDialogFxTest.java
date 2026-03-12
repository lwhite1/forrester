package systems.courant.shrewd.app.canvas;

import systems.courant.shrewd.model.def.SimulationSettings;

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

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("SimulationSettingsDialog (TestFX)")
@ExtendWith(ApplicationExtension.class)
class SimulationSettingsDialogFxTest {

    private SimulationSettingsDialog dialog;
    private DialogPane dialogPane;

    @Start
    void start(Stage stage) {
        stage.setScene(new Scene(new StackPane(), 200, 200));
        stage.show();
    }

    private void showDialog(SimulationSettings existing) {
        Platform.runLater(() -> {
            dialog = new SimulationSettingsDialog(existing);
            dialogPane = dialog.getDialogPane();
            dialog.show();
        });
        WaitForAsyncUtils.waitForFxEvents();
    }

    @Test
    @DisplayName("New dialog has default values when no existing settings")
    void defaultValuesWhenNull(FxRobot robot) {
        showDialog(null);

        ComboBox<?> timeStep = robot.lookup("#simTimeStep").queryAs(ComboBox.class);
        TextField duration = robot.lookup("#simDuration").queryAs(TextField.class);
        ComboBox<?> durationUnit = robot.lookup("#simDurationUnit").queryAs(ComboBox.class);
        TextField dt = robot.lookup("#simDt").queryAs(TextField.class);

        assertThat(timeStep.getValue()).isEqualTo("Day");
        assertThat(duration.getText()).isEqualTo("100");
        assertThat(durationUnit.getValue()).isEqualTo("Day");
        assertThat(dt.getText()).isEqualTo("1");
    }

    @Test
    @DisplayName("Dialog populates from existing settings")
    void populatesFromExisting(FxRobot robot) {
        showDialog(new SimulationSettings("Week", 52, "Week"));

        ComboBox<?> timeStep = robot.lookup("#simTimeStep").queryAs(ComboBox.class);
        TextField duration = robot.lookup("#simDuration").queryAs(TextField.class);
        ComboBox<?> durationUnit = robot.lookup("#simDurationUnit").queryAs(ComboBox.class);
        TextField dt = robot.lookup("#simDt").queryAs(TextField.class);

        assertThat(timeStep.getValue()).isEqualTo("Week");
        assertThat(duration.getText()).isEqualTo("52");
        assertThat(durationUnit.getValue()).isEqualTo("Week");
        assertThat(dt.getText()).isEqualTo("1");
    }

    @Test
    @DisplayName("Dialog populates fractional DT from existing settings")
    void populatesFractionalDt(FxRobot robot) {
        showDialog(new SimulationSettings("Day", 100, "Day", 0.25));

        TextField dt = robot.lookup("#simDt").queryAs(TextField.class);
        assertThat(dt.getText()).isEqualTo("0.25");
    }

    @Test
    @DisplayName("OK button is disabled when duration is empty")
    void okDisabledWhenEmpty(FxRobot robot) {
        showDialog(null);

        TextField duration = robot.lookup("#simDuration").queryAs(TextField.class);
        robot.clickOn(duration).eraseText(duration.getText().length());
        WaitForAsyncUtils.waitForFxEvents();

        Node okButton = dialogPane.lookupButton(
                dialogPane.getButtonTypes().stream()
                        .filter(bt -> bt.getButtonData().isDefaultButton())
                        .findFirst().orElseThrow());
        assertThat(okButton.isDisabled()).isTrue();
    }

    @Test
    @DisplayName("OK button is disabled when duration is negative")
    void okDisabledWhenNegative(FxRobot robot) {
        showDialog(null);

        TextField duration = robot.lookup("#simDuration").queryAs(TextField.class);
        robot.clickOn(duration).eraseText(duration.getText().length()).write("-5");
        WaitForAsyncUtils.waitForFxEvents();

        Node okButton = dialogPane.lookupButton(
                dialogPane.getButtonTypes().stream()
                        .filter(bt -> bt.getButtonData().isDefaultButton())
                        .findFirst().orElseThrow());
        assertThat(okButton.isDisabled()).isTrue();
    }

    @Test
    @DisplayName("OK button is disabled when duration is not a number")
    void okDisabledWhenNotNumeric(FxRobot robot) {
        showDialog(null);

        TextField duration = robot.lookup("#simDuration").queryAs(TextField.class);
        robot.clickOn(duration).eraseText(duration.getText().length()).write("abc");
        WaitForAsyncUtils.waitForFxEvents();

        Node okButton = dialogPane.lookupButton(
                dialogPane.getButtonTypes().stream()
                        .filter(bt -> bt.getButtonData().isDefaultButton())
                        .findFirst().orElseThrow());
        assertThat(okButton.isDisabled()).isTrue();
    }

    @Test
    @DisplayName("OK button is enabled when duration is a valid positive number")
    void okEnabledWhenValid(FxRobot robot) {
        showDialog(null);

        TextField duration = robot.lookup("#simDuration").queryAs(TextField.class);
        robot.clickOn(duration).eraseText(duration.getText().length()).write("365");
        WaitForAsyncUtils.waitForFxEvents();

        Node okButton = dialogPane.lookupButton(
                dialogPane.getButtonTypes().stream()
                        .filter(bt -> bt.getButtonData().isDefaultButton())
                        .findFirst().orElseThrow());
        assertThat(okButton.isDisabled()).isFalse();
    }

    @Test
    @DisplayName("OK button is disabled when DT is empty")
    void okDisabledWhenDtEmpty(FxRobot robot) {
        showDialog(null);

        TextField dt = robot.lookup("#simDt").queryAs(TextField.class);
        robot.clickOn(dt).eraseText(dt.getText().length());
        WaitForAsyncUtils.waitForFxEvents();

        Node okButton = dialogPane.lookupButton(
                dialogPane.getButtonTypes().stream()
                        .filter(bt -> bt.getButtonData().isDefaultButton())
                        .findFirst().orElseThrow());
        assertThat(okButton.isDisabled()).isTrue();
    }

    @Test
    @DisplayName("OK button is disabled when DT is negative")
    void okDisabledWhenDtNegative(FxRobot robot) {
        showDialog(null);

        TextField dt = robot.lookup("#simDt").queryAs(TextField.class);
        robot.clickOn(dt).eraseText(dt.getText().length()).write("-0.5");
        WaitForAsyncUtils.waitForFxEvents();

        Node okButton = dialogPane.lookupButton(
                dialogPane.getButtonTypes().stream()
                        .filter(bt -> bt.getButtonData().isDefaultButton())
                        .findFirst().orElseThrow());
        assertThat(okButton.isDisabled()).isTrue();
    }

    @Test
    @DisplayName("Changing duration field updates text")
    void changeDuration(FxRobot robot) {
        showDialog(null);

        TextField duration = robot.lookup("#simDuration").queryAs(TextField.class);
        robot.clickOn(duration).eraseText(duration.getText().length()).write("250");

        assertThat(duration.getText()).isEqualTo("250");
    }
}
