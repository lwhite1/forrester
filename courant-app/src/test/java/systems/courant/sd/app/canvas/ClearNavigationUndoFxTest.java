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

/**
 * Verifies that {@link ModelCanvas#clearNavigation()} closes module-level
 * UndoManagers stored in navigation frames, preventing executor thread leaks (#885).
 */
@DisplayName("clearNavigation closes module UndoManagers (#885)")
@ExtendWith(ApplicationExtension.class)
class ClearNavigationUndoFxTest {

    private ModelCanvas canvas;
    private UndoManager rootUndoManager;

    @Start
    void start(Stage stage) {
        canvas = new ModelCanvas(new Clipboard());
        rootUndoManager = new UndoManager();
        canvas.setUndoManager(rootUndoManager);
        stage.setScene(new Scene(new StackPane(canvas), 800, 600));
        stage.show();
    }

    private void loadNestedModules() {
        ModelDefinition innerDef = new ModelDefinitionBuilder()
                .name("Inner")
                .stock("InnerStock", 10, "units")
                .build();

        ModelDefinition middleDef = new ModelDefinitionBuilder()
                .name("Middle")
                .stock("MiddleStock", 20, "units")
                .module("Module 1", innerDef, Map.of(), Map.of())
                .build();

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

    @Test
    @DisplayName("clearNavigation after drill-in should close module UndoManager and restore root")
    void shouldCloseModuleUndoManagerOnClear() {
        loadNestedModules();
        canvas.drillInto("Module 1");

        assertThat(canvas.isInsideModule()).isTrue();
        UndoManager moduleUm = canvas.getUndoManager();
        assertThat(moduleUm).isNotSameAs(rootUndoManager);

        canvas.clearNavigation();

        assertThat(moduleUm.isClosed()).isTrue();
        assertThat(canvas.getUndoManager()).isSameAs(rootUndoManager);
        assertThat(rootUndoManager.isClosed()).isFalse();
    }

    @Test
    @DisplayName("clearNavigation after multi-level drill-in should close all module UndoManagers")
    void shouldCloseAllModuleUndoManagersOnClear() {
        loadNestedModules();
        canvas.drillInto("Module 1");
        UndoManager level1Um = canvas.getUndoManager();

        canvas.drillInto("Module 1");
        UndoManager level2Um = canvas.getUndoManager();

        assertThat(level1Um).isNotSameAs(rootUndoManager);
        assertThat(level2Um).isNotSameAs(level1Um);

        canvas.clearNavigation();

        assertThat(level2Um.isClosed()).isTrue();
        assertThat(level1Um.isClosed()).isTrue();
        assertThat(canvas.getUndoManager()).isSameAs(rootUndoManager);
        assertThat(rootUndoManager.isClosed()).isFalse();
    }

    @Test
    @DisplayName("clearNavigation at root level should not close root UndoManager")
    void shouldNotCloseRootUndoManagerWhenAtRoot() {
        loadNestedModules();

        canvas.clearNavigation();

        assertThat(canvas.getUndoManager()).isSameAs(rootUndoManager);
        assertThat(rootUndoManager.isClosed()).isFalse();
    }
}
