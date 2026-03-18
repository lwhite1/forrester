package systems.courant.sd.app.canvas;

import systems.courant.sd.model.def.ElementType;

import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.testfx.framework.junit5.ApplicationExtension;
import org.testfx.framework.junit5.Start;
import org.testfx.util.WaitForAsyncUtils;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("PropertiesPanel undo (#706)")
@ExtendWith(ApplicationExtension.class)
class PropertiesPanelUndoFxTest {

    private ModelCanvas canvas;
    private ModelEditor editor;
    private UndoManager undoManager;
    private PropertiesPanel panel;

    @Start
    void start(Stage stage) {
        canvas = new ModelCanvas(new Clipboard());
        undoManager = new UndoManager();
        canvas.undo().setUndoManager(undoManager);
        panel = new PropertiesPanel();

        editor = new ModelEditor();
        editor.setModelName("TestModel");
        editor.setModelComment("Original comment");

        CanvasState state = new CanvasState();
        canvas.setModel(editor, state.toViewDef());

        // Show model summary immediately
        panel.updateSelection(canvas, editor);

        StackPane root = new StackPane(canvas, panel);
        stage.setScene(new Scene(root, 800, 600));
        stage.show();
    }

    @Test
    @DisplayName("model name change should save undo state")
    void shouldSaveUndoOnModelNameChange() {
        Platform.runLater(() -> {
            TextField nameField = (TextField) panel.lookup("#modelNameField");
            nameField.setText("NewName");
            nameField.fireEvent(new javafx.event.ActionEvent());
        });
        WaitForAsyncUtils.waitForFxEvents();

        assertThat(undoManager.canUndo()).isTrue();
        assertThat(undoManager.undoLabels()).contains("Rename model");
    }

    @Test
    @DisplayName("unchanged model name should not save undo state")
    void shouldNotSaveUndoWhenNameUnchanged() {
        Platform.runLater(() -> {
            TextField nameField = (TextField) panel.lookup("#modelNameField");
            nameField.setText("TestModel");
            nameField.fireEvent(new javafx.event.ActionEvent());
        });
        WaitForAsyncUtils.waitForFxEvents();

        assertThat(undoManager.canUndo()).isFalse();
    }

    @Test
    @DisplayName("model comment change should save undo state")
    void shouldSaveUndoOnCommentChange() {
        Platform.runLater(() -> {
            TextArea descArea = (TextArea) panel.lookup("#modelDescField");
            descArea.requestFocus();
        });
        WaitForAsyncUtils.waitForFxEvents();

        Platform.runLater(() -> {
            TextArea descArea = (TextArea) panel.lookup("#modelDescField");
            descArea.setText("New description");
        });
        WaitForAsyncUtils.waitForFxEvents();

        // Move focus away to trigger commit
        Platform.runLater(() -> canvas.requestFocus());
        WaitForAsyncUtils.waitForFxEvents();

        assertThat(undoManager.canUndo()).isTrue();
        assertThat(undoManager.undoLabels()).contains("Edit description");
    }

    @Test
    @DisplayName("unchanged model comment should not save undo state")
    void shouldNotSaveUndoWhenCommentUnchanged() {
        Platform.runLater(() -> {
            TextArea descArea = (TextArea) panel.lookup("#modelDescField");
            descArea.requestFocus();
        });
        WaitForAsyncUtils.waitForFxEvents();

        Platform.runLater(() -> {
            TextArea descArea = (TextArea) panel.lookup("#modelDescField");
            descArea.setText("Original comment");
        });
        WaitForAsyncUtils.waitForFxEvents();

        Platform.runLater(() -> canvas.requestFocus());
        WaitForAsyncUtils.waitForFxEvents();

        assertThat(undoManager.canUndo()).isFalse();
    }

    @Test
    @DisplayName("polarity change should save undo state")
    void shouldSaveUndoOnPolarityChange() {
        Platform.runLater(() -> {
            editor.addCldVariable();
            editor.addCldVariable();
            CanvasState state = new CanvasState();
            state.addElement("Variable 1", ElementType.CLD_VARIABLE, 100, 200);
            state.addElement("Variable 2", ElementType.CLD_VARIABLE, 400, 200);
            canvas.setModel(editor, state.toViewDef());

            canvas.elements().handleCausalLinkClick(100, 200);
            canvas.elements().handleCausalLinkClick(400, 200);

            canvas.setSelectedConnection(
                    new ConnectionId("Variable 1", "Variable 2"), true);
            panel.updateSelection(canvas, editor);
        });
        WaitForAsyncUtils.waitForFxEvents();

        int undoDepthBefore = undoManager.undoDepth();

        Platform.runLater(() -> {
            @SuppressWarnings("unchecked")
            ComboBox<String> polarityBox = (ComboBox<String>) panel.lookup("#propPolarity");
            assertThat(polarityBox).as("polarity combo box").isNotNull();
            polarityBox.getSelectionModel().select(1);
        });
        WaitForAsyncUtils.waitForFxEvents();

        assertThat(undoManager.undoDepth()).isGreaterThan(undoDepthBefore);
        assertThat(undoManager.undoLabels().getFirst()).contains("polarity");
    }
}
