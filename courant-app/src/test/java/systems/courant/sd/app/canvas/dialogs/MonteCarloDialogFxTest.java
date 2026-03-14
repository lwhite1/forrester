package systems.courant.sd.app.canvas.dialogs;

import javafx.application.Platform;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
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

@DisplayName("MonteCarloDialog (TestFX)")
@ExtendWith(ApplicationExtension.class)
class MonteCarloDialogFxTest {

    private MonteCarloDialog dialog;
    private DialogPane dialogPane;

    @Start
    void start(Stage stage) {
        stage.setScene(new Scene(new StackPane(), 200, 200));
        stage.show();
    }

    private void showDialog(List<String> constants) {
        Platform.runLater(() -> {
            dialog = new MonteCarloDialog(constants);
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
        showDialog(List.of("alpha", "beta"));

        assertThat(okButton().isDisabled()).isFalse();
    }

    @Test
    @DisplayName("Default iterations is 200")
    void defaultIterations(FxRobot robot) {
        showDialog(List.of("alpha"));

        Set<TextField> fields = robot.lookup(".text-field").queryAllAs(TextField.class);
        boolean found200 = fields.stream().anyMatch(f -> "200".equals(f.getText()));
        assertThat(found200).isTrue();
    }

    @Test
    @DisplayName("Default sampling is LATIN_HYPERCUBE")
    void defaultSampling(FxRobot robot) {
        showDialog(List.of("alpha"));

        Set<ComboBox> combos = robot.lookup(".combo-box").queryAllAs(ComboBox.class);
        boolean foundLH = combos.stream()
                .anyMatch(c -> "LATIN_HYPERCUBE".equals(c.getValue()));
        assertThat(foundLH).isTrue();
    }

    @Test
    @DisplayName("OK disabled when iterations is zero")
    void okDisabledWhenIterationsZero(FxRobot robot) {
        showDialog(List.of("alpha"));

        Set<TextField> fields = robot.lookup(".text-field").queryAllAs(TextField.class);
        TextField iterField = fields.stream()
                .filter(f -> "200".equals(f.getText()))
                .findFirst().orElseThrow();
        robot.clickOn(iterField).eraseText(3).write("0");
        WaitForAsyncUtils.waitForFxEvents();

        assertThat(okButton().isDisabled()).isTrue();
    }

    @Test
    @DisplayName("OK disabled when iterations is non-numeric")
    void okDisabledWhenIterationsNonNumeric(FxRobot robot) {
        showDialog(List.of("alpha"));

        Set<TextField> fields = robot.lookup(".text-field").queryAllAs(TextField.class);
        TextField iterField = fields.stream()
                .filter(f -> "200".equals(f.getText()))
                .findFirst().orElseThrow();
        robot.clickOn(iterField).eraseText(3).write("abc");
        WaitForAsyncUtils.waitForFxEvents();

        assertThat(okButton().isDisabled()).isTrue();
    }

    @Test
    @DisplayName("OK disabled when no constants provided")
    void okDisabledWhenNoConstants(FxRobot robot) {
        showDialog(List.of());

        // No parameter row can be valid without a constant name
        assertThat(okButton().isDisabled()).isTrue();
    }

    @Test
    @DisplayName("No event filter alerts on OK button (#407)")
    void noEventFilterOnOkButton(FxRobot robot) {
        showDialog(List.of("alpha"));

        Button ok = (Button) okButton();
        assertThat(ok.isDisabled()).isFalse();
        assertThat(ok.disableProperty().isBound()).isTrue();
    }

    @Test
    @DisplayName("Validation label shows message when no valid params (#407)")
    void validationLabelShowsMessageWhenInvalid(FxRobot robot) {
        showDialog(List.of());

        Label validationLabel = robot.lookup("#mcValidationLabel").queryAs(Label.class);
        assertThat(validationLabel.getText()).isNotEmpty();
        assertThat(okButton().isDisabled()).isTrue();
    }

    @Test
    @DisplayName("Default seed is 12345")
    void defaultSeed(FxRobot robot) {
        showDialog(List.of("alpha"));

        Set<TextField> fields = robot.lookup(".text-field").queryAllAs(TextField.class);
        boolean foundSeed = fields.stream().anyMatch(f -> "12345".equals(f.getText()));
        assertThat(foundSeed).isTrue();
    }
}
