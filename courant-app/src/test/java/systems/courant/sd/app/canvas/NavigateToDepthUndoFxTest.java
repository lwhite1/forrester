package systems.courant.sd.app.canvas;

import systems.courant.sd.model.def.ElementType;
import systems.courant.sd.model.def.ModelDefinition;
import systems.courant.sd.model.def.ModelDefinitionBuilder;

import javafx.scene.Scene;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.testfx.framework.junit5.ApplicationExtension;
import org.testfx.framework.junit5.Start;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

/**
 * Verifies that {@link ModelCanvas#navigateToDepth(int)} creates a single undo
 * entry regardless of how many navigation levels are traversed (#383).
 */
@DisplayName("navigateToDepth undo consolidation (#383)")
@ExtendWith(ApplicationExtension.class)
class NavigateToDepthUndoFxTest {

    private ModelCanvas canvas;

    @Start
    void start(Stage stage) {
        canvas = new ModelCanvas(new Clipboard());
        canvas.setUndoManager(new UndoManager());
        stage.setScene(new Scene(new StackPane(canvas), 800, 600));
        stage.show();
    }

    /**
     * Builds a model three levels deep:
     * Root -> Module 1 -> Module 1 (inner)
     *
     * Each module contains a sub-module so we can drill into it.
     */
    private void loadNestedModules() {
        // Innermost module definition (depth 2) — leaf, no sub-modules
        ModelDefinition innerDef = new ModelDefinitionBuilder()
                .name("Inner")
                .stock("InnerStock", 10, "units")
                .build();

        // Middle module definition (depth 1) — contains the inner module
        ModelDefinition middleDef = new ModelDefinitionBuilder()
                .name("Middle")
                .stock("MiddleStock", 20, "units")
                .module("Module 1", innerDef, Map.of(), Map.of())
                .build();

        // Root model (depth 0) — contains the middle module
        ModelDefinition rootDef = new ModelDefinitionBuilder()
                .name("Root")
                .stock("RootStock", 30, "units")
                .module("Module 1", middleDef, Map.of(), Map.of())
                .build();

        ModelEditor editor = new ModelEditor();
        editor.loadFrom(rootDef);

        CanvasState state = new CanvasState();
        state.addElement("RootStock", ElementType.STOCK, 100, 200);
        state.addElement("Module 1", ElementType.MODULE, 300, 200);

        canvas.setModel(editor, state.toViewDef());
    }

    /**
     * Drills into modules to reach the specified depth.
     */
    private void drillToDepth(int depth) {
        for (int i = 0; i < depth; i++) {
            canvas.drillInto("Module 1");
        }
    }

    @Test
    @DisplayName("navigateToDepth from depth 3 to 0 should create exactly one undo entry")
    void shouldCreateSingleUndoEntryForMultipleLevelNavigation() {
        loadNestedModules();
        drillToDepth(3);

        assertThat(canvas.getNavigationPath()).hasSize(4); // Root + 3 levels

        // The root-level undo manager was saved in the nav frame when we drilled in.
        // navigateToDepth should restore it and push exactly one undo entry.
        canvas.navigateToDepth(0);

        assertThat(canvas.isInsideModule()).isFalse();
        UndoManager rootUndo = canvas.getUndoManager();
        assertThat(rootUndo.undoDepth()).isEqualTo(1);
    }

    @Test
    @DisplayName("navigateToDepth from depth 2 to 0 should create exactly one undo entry")
    void shouldCreateSingleUndoEntryForTwoLevelNavigation() {
        loadNestedModules();
        drillToDepth(2);

        assertThat(canvas.getNavigationPath()).hasSize(3); // Root + 2 levels

        canvas.navigateToDepth(0);

        assertThat(canvas.isInsideModule()).isFalse();
        UndoManager rootUndo = canvas.getUndoManager();
        assertThat(rootUndo.undoDepth()).isEqualTo(1);
    }

    @Test
    @DisplayName("navigateToDepth from depth 1 to 0 should create exactly one undo entry")
    void shouldCreateSingleUndoEntryForSingleLevelNavigation() {
        loadNestedModules();
        drillToDepth(1);

        assertThat(canvas.getNavigationPath()).hasSize(2); // Root + 1 level

        canvas.navigateToDepth(0);

        assertThat(canvas.isInsideModule()).isFalse();
        UndoManager rootUndo = canvas.getUndoManager();
        assertThat(rootUndo.undoDepth()).isEqualTo(1);
    }

    @Test
    @DisplayName("navigateToDepth to current depth should not create any undo entry")
    void shouldNotCreateUndoEntryWhenAlreadyAtTargetDepth() {
        loadNestedModules();
        drillToDepth(2);

        canvas.navigateToDepth(2); // already at depth 2

        UndoManager currentUndo = canvas.getUndoManager();
        assertThat(currentUndo.undoDepth()).isZero();
    }

    @Test
    @DisplayName("navigateBack should still create exactly one undo entry")
    void navigateBackShouldStillCreateOneUndoEntry() {
        loadNestedModules();
        drillToDepth(1);

        canvas.navigateBack();

        assertThat(canvas.isInsideModule()).isFalse();
        UndoManager rootUndo = canvas.getUndoManager();
        assertThat(rootUndo.undoDepth()).isEqualTo(1);
    }

    @Test
    @DisplayName("navigateToDepth with no editor should not throw")
    void shouldNotThrowWhenEditorIsNull() {
        // canvas has no model set yet
        assertThatCode(() -> canvas.navigateToDepth(0)).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("navigateToDepth to intermediate depth should create one undo entry")
    void shouldCreateSingleUndoEntryForPartialNavigation() {
        loadNestedModules();
        drillToDepth(3);

        // Navigate from depth 3 to depth 1 (back 2 levels, but stay inside module)
        canvas.navigateToDepth(1);

        assertThat(canvas.isInsideModule()).isTrue();
        assertThat(canvas.getNavigationPath()).hasSize(2); // Root + 1 level
        UndoManager midUndo = canvas.getUndoManager();
        assertThat(midUndo.undoDepth()).isEqualTo(1);
    }
}
