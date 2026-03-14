package systems.courant.sd.app.canvas;

import systems.courant.sd.model.def.ElementType;

import javafx.application.Platform;
import javafx.event.Event;
import javafx.scene.Scene;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.testfx.framework.junit5.ApplicationExtension;
import org.testfx.framework.junit5.Start;
import org.testfx.util.WaitForAsyncUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

/**
 * Tests that {@link InputDispatcher#handleMouseExited} clears hover state
 * when the mouse leaves the canvas (#382).
 */
@DisplayName("InputDispatcher mouse-exit clears hover (#382)")
@ExtendWith(ApplicationExtension.class)
class InputDispatcherMouseExitFxTest {

    private ModelCanvas canvas;
    private InputDispatcher inputDispatcher;

    @Start
    void start(Stage stage) {
        canvas = new ModelCanvas(new Clipboard());
        canvas.setUndoManager(new UndoManager());

        ModelEditor editor = new ModelEditor();
        editor.addStock(); // Stock 1
        CanvasState state = new CanvasState();
        state.addElement("Stock 1", ElementType.STOCK, 100, 200);
        canvas.setModel(editor, state.toViewDef());

        StackPane root = new StackPane(canvas);
        stage.setScene(new Scene(root, 800, 600));
        stage.show();

        // Access the InputDispatcher via reflection for state verification
        try {
            var field = ModelCanvas.class.getDeclaredField("inputDispatcher");
            field.setAccessible(true);
            inputDispatcher = (InputDispatcher) field.get(canvas);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    @DisplayName("mouse exit should clear hoveredElement")
    void shouldClearHoveredElementOnMouseExit() {
        Platform.runLater(() -> {
            // Move mouse over the element to set hover state
            fireMouseMoved(100, 200);
            // Verify hover was set (element at 100,200 in world coords, viewport at 1:1)
            // The hit test depends on viewport transform, but at minimum hover should be non-null
            // if the element is under the cursor

            // Now fire mouse exit
            fireMouseExited();

            assertThat(inputDispatcher.getHoveredElement()).isNull();
        });
        WaitForAsyncUtils.waitForFxEvents();
    }

    @Test
    @DisplayName("mouse exit should clear hoveredConnection")
    void shouldClearHoveredConnectionOnMouseExit() {
        Platform.runLater(() -> {
            fireMouseExited();
            assertThat(inputDispatcher.getHoveredConnection()).isNull();
        });
        WaitForAsyncUtils.waitForFxEvents();
    }

    @Test
    @DisplayName("mouse exit should not throw when no element is hovered")
    void shouldNotThrowWhenNoHoverOnMouseExit() {
        Platform.runLater(() -> {
            assertThatCode(this::fireMouseExited).doesNotThrowAnyException();
        });
        WaitForAsyncUtils.waitForFxEvents();
    }

    @Test
    @DisplayName("setOnMouseExited handler is registered on canvas")
    void shouldHaveMouseExitedHandlerRegistered() {
        assertThat(canvas.getOnMouseExited()).isNotNull();
    }

    private void fireMouseMoved(double x, double y) {
        Event.fireEvent(canvas, new MouseEvent(
                MouseEvent.MOUSE_MOVED,
                x, y, x, y,
                MouseButton.NONE, 0,
                false, false, false, false,
                false, false, false,
                false, false, false, null));
    }

    private void fireMouseExited() {
        Event.fireEvent(canvas, new MouseEvent(
                MouseEvent.MOUSE_EXITED,
                -1, -1, -1, -1,
                MouseButton.NONE, 0,
                false, false, false, false,
                false, false, false,
                false, false, false, null));
    }
}
