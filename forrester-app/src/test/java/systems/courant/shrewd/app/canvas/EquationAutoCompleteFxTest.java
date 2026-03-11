package systems.courant.shrewd.app.canvas;

import systems.courant.shrewd.model.def.ModelDefinitionBuilder;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.Scene;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.testfx.api.FxRobot;
import org.testfx.framework.junit5.ApplicationExtension;
import org.testfx.framework.junit5.Start;
import org.testfx.util.WaitForAsyncUtils;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * TestFX tests for autocomplete Enter-key selection (issue #5).
 */
@DisplayName("EquationAutoComplete (TestFX)")
@ExtendWith(ApplicationExtension.class)
class EquationAutoCompleteFxTest {

    private TextField equationField;
    private ModelEditor editor;
    private boolean committed;

    @Start
    void start(Stage stage) {
        equationField = new TextField();
        equationField.setId("equationField");
        // The onAction handler must be set BEFORE attach() — attach wraps it
        equationField.setOnAction(e -> committed = true);

        editor = new ModelEditor();
        editor.loadFrom(new ModelDefinitionBuilder()
                .name("Test")
                .stock("Population", 1000, "people")
                .stock("Pollution", 0, "tons")
                .flow("Birth Rate", "Population * 0.03", "year", null, "Population")
                .aux("Contact Rate", "5", "contacts/day")
                .constant("Growth Factor", 1.5, "dimensionless")
                .build());

        EquationAutoComplete.attach(equationField, editor, "Birth Rate");

        VBox root = new VBox(equationField);
        stage.setScene(new Scene(root, 400, 200));
        stage.show();
    }

    @Test
    @DisplayName("handleKey ENTER inserts suggestion when popup visible")
    void handleKeyEnterInsertsSuggestion(FxRobot robot) {
        committed = false;

        // Type "Pop" to trigger autocomplete
        robot.clickOn("#equationField");
        robot.write("Pop");
        WaitForAsyncUtils.waitForFxEvents();

        assertThat(EquationAutoComplete.isPopupShowing(equationField))
                .as("Popup should be showing after typing 'Pop'")
                .isTrue();

        // Navigate down to ensure an item is highlighted
        // Invoke the key filter directly via the state, since TestFX headless
        // doesn't deliver KEY_PRESSED events through the filter chain
        Platform.runLater(() -> {
            fireKeyOnField(KeyCode.DOWN);
            fireKeyOnField(KeyCode.ENTER);
        });
        WaitForAsyncUtils.waitForFxEvents();

        assertThat(equationField.getText())
                .as("Suggestion should replace the prefix typed")
                .containsAnyOf("Population", "Pollution");
        assertThat(committed)
                .as("onAction should NOT have committed the field")
                .isFalse();
    }

    @Test
    @DisplayName("Enter commits when popup is not showing")
    void enterCommitsWhenNoPopup(FxRobot robot) {
        committed = false;
        robot.clickOn("#equationField");
        robot.write("42");
        WaitForAsyncUtils.waitForFxEvents();

        assertThat(EquationAutoComplete.isPopupShowing(equationField))
                .as("Popup should NOT be showing after typing '42'")
                .isFalse();

        // Directly invoke the wrapped onAction — simulates what TextField does on Enter
        Object origSaved = equationField.getProperties().get("EquationAutoComplete.originalAction");
        Platform.runLater(() -> {
            EventHandler<ActionEvent> onAction = equationField.getOnAction();
            onAction.handle(new ActionEvent(equationField, equationField));
        });
        WaitForAsyncUtils.waitForFxEvents();

        assertThat(origSaved)
                .as("Original onAction should have been saved by attach()")
                .isNotNull();

        assertThat(committed)
                .as("onAction should fire when no popup is showing")
                .isTrue();
    }

    @Test
    @DisplayName("Escape closes popup without inserting or committing")
    void escapeClosesPopup(FxRobot robot) {
        committed = false;
        robot.clickOn("#equationField");
        robot.write("Pop");
        WaitForAsyncUtils.waitForFxEvents();

        assertThat(EquationAutoComplete.isPopupShowing(equationField)).isTrue();

        Platform.runLater(() -> fireKeyOnField(KeyCode.ESCAPE));
        WaitForAsyncUtils.waitForFxEvents();

        assertThat(committed).isFalse();
        assertThat(equationField.getText()).isEqualTo("Pop");
        assertThat(EquationAutoComplete.isPopupShowing(equationField)).isFalse();
    }

    @Test
    @DisplayName("Tab inserts suggestion like Enter")
    void tabInsertsSuggestion(FxRobot robot) {
        committed = false;
        robot.clickOn("#equationField");
        robot.write("Pop");
        WaitForAsyncUtils.waitForFxEvents();

        assertThat(EquationAutoComplete.isPopupShowing(equationField)).isTrue();

        Platform.runLater(() -> fireKeyOnField(KeyCode.TAB));
        WaitForAsyncUtils.waitForFxEvents();

        assertThat(equationField.getText())
                .containsAnyOf("Population", "Pollution");
    }

    @Test
    @DisplayName("Function suggestion appends opening parenthesis")
    void functionSuggestionAppendsParenthesis(FxRobot robot) {
        robot.clickOn("#equationField");
        robot.write("STE");
        WaitForAsyncUtils.waitForFxEvents();

        assertThat(EquationAutoComplete.isPopupShowing(equationField)).isTrue();

        Platform.runLater(() -> fireKeyOnField(KeyCode.ENTER));
        WaitForAsyncUtils.waitForFxEvents();

        assertThat(equationField.getText()).isEqualTo("STEP(");
    }

    /**
     * Simulates a key press by invoking the autocomplete's event filter directly,
     * then firing the event on the field for any remaining handlers.
     * This is necessary because TestFX headless mode does not deliver KEY_PRESSED
     * events through the Node event filter chain.
     */
    @SuppressWarnings("unchecked")
    private void fireKeyOnField(KeyCode code) {
        KeyEvent event = new KeyEvent(
                KeyEvent.KEY_PRESSED, "", "", code,
                false, false, false, false);

        // Invoke the autocomplete filter directly (TestFX headless doesn't
        // deliver KEY_PRESSED through the event filter chain)
        Object stateObj = equationField.getProperties().get("EquationAutoComplete.state");
        if (stateObj != null) {
            try {
                var keyFilterField = stateObj.getClass().getDeclaredField("keyFilter");
                keyFilterField.setAccessible(true);
                EventHandler<KeyEvent> filter =
                        (EventHandler<KeyEvent>) keyFilterField.get(stateObj);
                filter.handle(event);
            } catch (ReflectiveOperationException e) {
                throw new RuntimeException(e);
            }
        }

        // If the autocomplete didn't consume the event, fire onAction for Enter
        // (simulates what JavaFX's TextField does internally)
        if (!event.isConsumed() && code == KeyCode.ENTER) {
            EventHandler<ActionEvent> onAction = equationField.getOnAction();
            if (onAction != null) {
                onAction.handle(new ActionEvent(equationField, equationField));
            }
        }
    }
}
