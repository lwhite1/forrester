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
        canvas.setUndoManager(new UndoManager());
        stage.setScene(new Scene(new StackPane(canvas), 400, 300));
        stage.show();
    }

    @Test
    @DisplayName("deleteSelectedElements should not throw when editor is null")
    void deleteSelectedElementsSafe() {
        assertThatCode(() -> canvas.deleteSelectedElements()).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("renameElement should not throw when editor is null")
    void renameElementSafe() {
        assertThatCode(() -> canvas.renameElement("A", "B")).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("copySelection should not throw when editor is null")
    void copySelectionSafe() {
        assertThatCode(() -> canvas.copySelection()).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("cutSelection should not throw when editor is null")
    void cutSelectionSafe() {
        assertThatCode(() -> canvas.cutSelection()).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("pasteClipboard should return empty set when editor is null")
    void pasteClipboardSafe() {
        Set<String> result = canvas.pasteClipboard();
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("performUndo should not throw when editor is null")
    void performUndoSafe() {
        assertThatCode(() -> canvas.performUndo()).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("performRedo should not throw when editor is null")
    void performRedoSafe() {
        assertThatCode(() -> canvas.performRedo()).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("toModelDefinition should throw IllegalStateException when editor is null")
    void toModelDefinitionSafe() {
        assertThatThrownBy(() -> canvas.toModelDefinition())
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
        assertThatCode(() -> canvas.handleFlowClick(100, 100)).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("handleCausalLinkClick should not throw when editor is null")
    void handleCausalLinkClickSafe() {
        assertThatCode(() -> canvas.handleCausalLinkClick(100, 100)).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("handleInfoLinkClick should not throw when editor is null")
    void handleInfoLinkClickSafe() {
        assertThatCode(() -> canvas.handleInfoLinkClick(100, 100)).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("createElementAt should not throw when editor is null")
    void createElementAtSafe() {
        assertThatCode(() -> canvas.createElementAt(200, 200)).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("startInlineEdit should not throw when editor is null")
    void startInlineEditSafe() {
        assertThatCode(() -> canvas.startInlineEdit("Stock 1")).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("zoomToFit should not throw when editor is null")
    void zoomToFitSafe() {
        assertThatCode(() -> canvas.zoomToFit()).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("navigateBack should not throw when editor is null")
    void navigateBackSafe() {
        assertThatCode(() -> canvas.navigateBack()).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("whereUsed should return empty set when editor is null")
    void whereUsedSafe() {
        assertThat(canvas.whereUsed("X")).isEmpty();
    }

    @Test
    @DisplayName("uses should return empty set when editor is null")
    void usesSafe() {
        assertThat(canvas.uses("X")).isEmpty();
    }

    @Test
    @DisplayName("showWhereUsed should not throw when editor is null")
    void showWhereUsedSafe() {
        assertThatCode(() -> canvas.showWhereUsed("X")).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("showUses should not throw when editor is null")
    void showUsesSafe() {
        assertThatCode(() -> canvas.showUses("X")).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("deleteSelectedOrConnection should not throw when editor is null")
    void deleteSelectedOrConnectionSafe() {
        assertThatCode(() -> canvas.deleteSelectedOrConnection()).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("setLoopHighlightActive should not throw when editor is null")
    void setLoopHighlightActiveSafe() {
        assertThatCode(() -> canvas.setLoopHighlightActive(true)).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("traceUpstream should not throw when editor is null")
    void traceUpstreamSafe() {
        assertThatCode(() -> canvas.traceUpstream("X")).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("traceDownstream should not throw when editor is null")
    void traceDownstreamSafe() {
        assertThatCode(() -> canvas.traceDownstream("X")).doesNotThrowAnyException();
    }
}
