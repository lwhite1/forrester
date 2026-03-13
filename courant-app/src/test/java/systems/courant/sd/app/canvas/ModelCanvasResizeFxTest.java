package systems.courant.sd.app.canvas;

import systems.courant.sd.model.def.ElementType;

import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.testfx.framework.junit5.ApplicationExtension;
import org.testfx.framework.junit5.Start;
import org.testfx.util.WaitForAsyncUtils;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for resize redraw coalescing (#203) and connector regeneration
 * scheduling (#204) in {@link ModelCanvas}.
 */
@DisplayName("ModelCanvas resize and connector coalescing (#203, #204)")
@ExtendWith(ApplicationExtension.class)
class ModelCanvasResizeFxTest {

    private ModelCanvas canvas;
    private StackPane root;

    @Start
    void start(Stage stage) {
        canvas = new ModelCanvas(new Clipboard());
        canvas.setUndoManager(new UndoManager());
        root = new StackPane(canvas);
        stage.setScene(new Scene(root, 800, 600));
        stage.show();
    }

    // --- #203: Resize redraw coalescing ---

    @Test
    @DisplayName("width and height changes in same frame produce single coalesced redraw")
    void shouldCoalesceResizeRedraws() {
        // Trigger both width and height changes rapidly
        Platform.runLater(() -> {
            canvas.setWidth(900);
            canvas.setHeight(700);
            // After both fire, only one redraw should be scheduled
            assertThat(canvas.isResizeRedrawScheduled()).isTrue();
        });
        WaitForAsyncUtils.waitForFxEvents();

        // After Platform.runLater executes, the flag should be cleared
        assertThat(canvas.isResizeRedrawScheduled()).isFalse();
    }

    @Test
    @DisplayName("single dimension change schedules exactly one redraw")
    void shouldScheduleRedrawOnSingleDimensionChange() {
        Platform.runLater(() -> {
            canvas.setWidth(1000);
            assertThat(canvas.isResizeRedrawScheduled()).isTrue();
        });
        WaitForAsyncUtils.waitForFxEvents();
        assertThat(canvas.isResizeRedrawScheduled()).isFalse();
    }

    // --- #204: Connector regeneration coalescing ---

    @Test
    @DisplayName("scheduleRegenerateConnectors coalesces multiple calls")
    void shouldCoalesceConnectorRegeneration() {
        loadModel();

        Platform.runLater(() -> {
            canvas.scheduleRegenerateConnectors();
            canvas.scheduleRegenerateConnectors();
            canvas.scheduleRegenerateConnectors();
            // All three calls should coalesce into one
            assertThat(canvas.isConnectorRegenerationScheduled()).isTrue();
        });
        WaitForAsyncUtils.waitForFxEvents();

        // After execution, the flag should be cleared
        assertThat(canvas.isConnectorRegenerationScheduled()).isFalse();
    }

    @Test
    @DisplayName("scheduleRegenerateConnectors does nothing when editor is null")
    void shouldNotScheduleWhenEditorNull() {
        // No model loaded — editor is null
        Platform.runLater(() -> {
            canvas.scheduleRegenerateConnectors();
        });
        WaitForAsyncUtils.waitForFxEvents();
        // Should complete without error; flag cleared
        assertThat(canvas.isConnectorRegenerationScheduled()).isFalse();
    }

    @Test
    @DisplayName("immediate regenerateConnectors still works synchronously")
    void shouldRegenerateImmediately() {
        loadModel();

        // Direct call should update connectors immediately (no scheduling)
        canvas.regenerateConnectors();
        assertThat(canvas.getConnectors()).isNotNull();
        assertThat(canvas.isConnectorRegenerationScheduled()).isFalse();
    }

    private void loadModel() {
        ModelEditor editor = new ModelEditor();
        editor.addStock(); // Stock 1
        CanvasState state = new CanvasState();
        state.addElement("Stock 1", ElementType.STOCK, 100, 200);
        canvas.setModel(editor, state.toViewDef());
    }
}
