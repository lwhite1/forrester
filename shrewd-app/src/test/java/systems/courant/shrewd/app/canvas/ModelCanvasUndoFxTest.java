package systems.courant.shrewd.app.canvas;

import systems.courant.shrewd.model.def.ElementType;

import javafx.scene.Scene;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.testfx.framework.junit5.ApplicationExtension;
import org.testfx.framework.junit5.Start;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("ModelCanvas undo entries on flow/causal-link creation (#424)")
@ExtendWith(ApplicationExtension.class)
class ModelCanvasUndoFxTest {

    private ModelCanvas canvas;
    private UndoManager undoManager;

    @Start
    void start(Stage stage) {
        canvas = new ModelCanvas(new Clipboard());
        undoManager = new UndoManager();
        canvas.setUndoManager(undoManager);
        stage.setScene(new Scene(new StackPane(canvas), 800, 600));
        stage.show();
    }

    private void loadTwoStocks() {
        ModelEditor editor = new ModelEditor();
        editor.addStock(); // Stock 1
        editor.addStock(); // Stock 2
        CanvasState state = new CanvasState();
        state.addElement("Stock 1", ElementType.STOCK, 100, 200);
        state.addElement("Stock 2", ElementType.STOCK, 400, 200);
        canvas.setModel(editor, state.toViewDef());
    }

    private void loadTwoCldVariables() {
        ModelEditor editor = new ModelEditor();
        editor.addCldVariable(); // Variable 1
        editor.addCldVariable(); // Variable 2
        CanvasState state = new CanvasState();
        state.addElement("Variable 1", ElementType.CLD_VARIABLE, 100, 200);
        state.addElement("Variable 2", ElementType.CLD_VARIABLE, 400, 200);
        canvas.setModel(editor, state.toViewDef());
    }

    @Test
    @DisplayName("rejected flow (self-loop) should not push undo entry")
    void shouldNotPushUndoOnRejectedFlowSelfLoop() {
        loadTwoStocks();

        canvas.handleFlowClick(100, 200); // Stock 1 as source
        canvas.handleFlowClick(100, 200); // Stock 1 again → rejected

        assertThat(undoManager.canUndo()).isFalse();
        assertThat(undoManager.undoLabels()).isEmpty();
    }

    @Test
    @DisplayName("rejected flow (cloud-to-cloud) should not push undo entry")
    void shouldNotPushUndoOnRejectedFlowCloudToCloud() {
        loadTwoStocks();

        canvas.handleFlowClick(250, 100); // empty space → cloud source
        canvas.handleFlowClick(300, 300); // empty space → cloud sink → rejected

        assertThat(undoManager.canUndo()).isFalse();
        assertThat(undoManager.undoLabels()).isEmpty();
    }

    @Test
    @DisplayName("successful flow creation should push undo entry")
    void shouldPushUndoOnSuccessfulFlowCreation() {
        loadTwoStocks();

        canvas.handleFlowClick(100, 200); // Stock 1 as source
        canvas.handleFlowClick(400, 200); // Stock 2 as sink → success

        assertThat(undoManager.canUndo()).isTrue();
        assertThat(undoManager.undoLabels()).containsExactly("Add flow");
    }

    @Test
    @DisplayName("rejected causal link (no target) should not push undo entry")
    void shouldNotPushUndoOnRejectedCausalLinkNoTarget() {
        loadTwoCldVariables();

        canvas.handleCausalLinkClick(100, 200); // Variable 1 as source
        canvas.handleCausalLinkClick(300, 300); // empty space → rejected

        assertThat(undoManager.canUndo()).isFalse();
        assertThat(undoManager.undoLabels()).isEmpty();
    }

    @Test
    @DisplayName("rejected causal link (duplicate) should not push additional undo entry")
    void shouldNotPushUndoOnRejectedDuplicateCausalLink() {
        loadTwoCldVariables();

        // Create first link successfully
        canvas.handleCausalLinkClick(100, 200);
        canvas.handleCausalLinkClick(400, 200);
        int undoCountAfterFirst = undoManager.undoLabels().size();

        // Try to create duplicate link
        canvas.handleCausalLinkClick(100, 200);
        canvas.handleCausalLinkClick(400, 200);

        assertThat(undoManager.undoLabels()).hasSize(undoCountAfterFirst);
    }

    @Test
    @DisplayName("successful causal link creation should push undo entry")
    void shouldPushUndoOnSuccessfulCausalLinkCreation() {
        loadTwoCldVariables();

        canvas.handleCausalLinkClick(100, 200); // Variable 1 as source
        canvas.handleCausalLinkClick(400, 200); // Variable 2 as target → success

        assertThat(undoManager.canUndo()).isTrue();
        assertThat(undoManager.undoLabels()).containsExactly("Add causal link");
    }

    @Test
    @DisplayName("rename via renameElement should push undo entry (#331)")
    void shouldPushUndoOnRename() {
        loadTwoStocks();

        canvas.renameElement("Stock 1", "Population");

        assertThat(undoManager.canUndo()).isTrue();
        assertThat(undoManager.undoLabels()).containsExactly("Rename Stock 1 → Population");
    }

    @Test
    @DisplayName("rename to existing name should not push undo entry")
    void shouldNotPushUndoOnRejectedRenameDuplicate() {
        loadTwoStocks();

        canvas.renameElement("Stock 1", "Stock 2"); // already exists → rejected

        assertThat(undoManager.canUndo()).isFalse();
        assertThat(undoManager.undoLabels()).isEmpty();
    }

    @Test
    @DisplayName("rename to same name should not push undo entry")
    void shouldNotPushUndoOnNoOpRename() {
        loadTwoStocks();

        canvas.renameElement("Stock 1", "Stock 1"); // no-op

        assertThat(undoManager.canUndo()).isFalse();
        assertThat(undoManager.undoLabels()).isEmpty();
    }
}
