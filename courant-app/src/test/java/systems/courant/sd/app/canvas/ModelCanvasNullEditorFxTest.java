package systems.courant.sd.app.canvas;

import javafx.scene.Scene;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.testfx.framework.junit5.ApplicationExtension;
import org.testfx.framework.junit5.Start;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Verifies that {@link ModelCanvas} does not throw NPE when methods are called
 * before {@link ModelCanvas#setModel} has been invoked (#528).
 */
@DisplayName("ModelCanvas null editor safety (#528)")
@ExtendWith(ApplicationExtension.class)
class ModelCanvasNullEditorFxTest {

    private ModelCanvas canvas;

    @Start
    void start(Stage stage) {
        canvas = new ModelCanvas(new Clipboard());
        canvas.undo().setUndoManager(new UndoManager());
        stage.setScene(new Scene(new StackPane(canvas), 400, 300));
        stage.show();
    }

    @Test
    @DisplayName("deleteSelectedElements should not throw when editor is null")
    void deleteSelectedElementsSafe() {
        assertThatCode(() -> canvas.elements().deleteSelectedElements()).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("renameElement should not throw when editor is null")
    void renameElementSafe() {
        assertThatCode(() -> canvas.elements().renameElement("A", "B")).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("copySelection should not throw when editor is null")
    void copySelectionSafe() {
        assertThatCode(() -> canvas.elements().copySelection()).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("cutSelection should not throw when editor is null")
    void cutSelectionSafe() {
        assertThatCode(() -> canvas.elements().cutSelection()).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("pasteClipboard should return empty set when editor is null")
    void pasteClipboardSafe() {
        Set<String> result = canvas.elements().pasteClipboard();
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("performUndo should not throw when editor is null")
    void performUndoSafe() {
        assertThatCode(() -> canvas.undo().performUndo()).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("performRedo should not throw when editor is null")
    void performRedoSafe() {
        assertThatCode(() -> canvas.undo().performRedo()).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("toModelDefinition should throw IllegalStateException when editor is null")
    void toModelDefinitionSafe() {
        assertThatThrownBy(() -> canvas.navigation().toModelDefinition())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("No model loaded");
    }

    @Test
    @DisplayName("isModelLoaded should return false when editor is null")
    void isModelLoadedFalse() {
        assertThat(canvas.isModelLoaded()).isFalse();
    }

    @Test
    @DisplayName("handleFlowClick should not throw when editor is null")
    void handleFlowClickSafe() {
        assertThatCode(() -> canvas.elements().handleFlowClick(100, 100)).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("handleCausalLinkClick should not throw when editor is null")
    void handleCausalLinkClickSafe() {
        assertThatCode(() -> canvas.elements().handleCausalLinkClick(100, 100)).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("handleInfoLinkClick should not throw when editor is null")
    void handleInfoLinkClickSafe() {
        assertThatCode(() -> canvas.elements().handleInfoLinkClick(100, 100)).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("createElementAt should not throw when editor is null")
    void createElementAtSafe() {
        assertThatCode(() -> canvas.elements().createElementAt(200, 200)).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("startInlineEdit should not throw when editor is null")
    void startInlineEditSafe() {
        assertThatCode(() -> canvas.elements().startInlineEdit("Stock 1")).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("zoomToFit should not throw when editor is null")
    void zoomToFitSafe() {
        assertThatCode(() -> canvas.zoomToFit()).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("navigateBack should not throw when editor is null")
    void navigateBackSafe() {
        assertThatCode(() -> canvas.navigation().navigateBack()).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("whereUsed should return empty set when editor is null")
    void whereUsedSafe() {
        assertThat(canvas.analysis().whereUsed("X")).isEmpty();
    }

    @Test
    @DisplayName("uses should return empty set when editor is null")
    void usesSafe() {
        assertThat(canvas.analysis().uses("X")).isEmpty();
    }

    @Test
    @DisplayName("showWhereUsed should not throw when editor is null")
    void showWhereUsedSafe() {
        assertThatCode(() -> canvas.analysis().showWhereUsed("X")).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("showUses should not throw when editor is null")
    void showUsesSafe() {
        assertThatCode(() -> canvas.analysis().showUses("X")).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("deleteSelectedOrConnection should not throw when editor is null")
    void deleteSelectedOrConnectionSafe() {
        assertThatCode(() -> canvas.elements().deleteSelectedOrConnection()).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("setLoopHighlightActive should not throw when editor is null")
    void setLoopHighlightActiveSafe() {
        assertThatCode(() -> canvas.analysis().setLoopHighlightActive(true)).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("traceUpstream should not throw when editor is null")
    void traceUpstreamSafe() {
        assertThatCode(() -> canvas.analysis().traceUpstream("X")).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("traceDownstream should not throw when editor is null")
    void traceDownstreamSafe() {
        assertThatCode(() -> canvas.analysis().traceDownstream("X")).doesNotThrowAnyException();
    }
}
