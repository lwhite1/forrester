package systems.courant.shrewd.app.canvas;

import javafx.scene.Cursor;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("ResizeController")
class ResizeControllerTest {

    @Nested
    @DisplayName("cursorFor()")
    class CursorFor {

        @Test
        @DisplayName("TOP_LEFT returns NW_RESIZE cursor")
        void topLeftReturnsNwResize() {
            assertThat(ResizeController.cursorFor(ResizeHandle.TOP_LEFT))
                    .isEqualTo(Cursor.NW_RESIZE);
        }

        @Test
        @DisplayName("BOTTOM_RIGHT returns NW_RESIZE cursor")
        void bottomRightReturnsNwResize() {
            assertThat(ResizeController.cursorFor(ResizeHandle.BOTTOM_RIGHT))
                    .isEqualTo(Cursor.NW_RESIZE);
        }

        @Test
        @DisplayName("TOP_RIGHT returns NE_RESIZE cursor")
        void topRightReturnsNeResize() {
            assertThat(ResizeController.cursorFor(ResizeHandle.TOP_RIGHT))
                    .isEqualTo(Cursor.NE_RESIZE);
        }

        @Test
        @DisplayName("BOTTOM_LEFT returns NE_RESIZE cursor")
        void bottomLeftReturnsNeResize() {
            assertThat(ResizeController.cursorFor(ResizeHandle.BOTTOM_LEFT))
                    .isEqualTo(Cursor.NE_RESIZE);
        }
    }

    @Nested
    @DisplayName("lifecycle")
    class Lifecycle {

        @Test
        @DisplayName("New controller is not active")
        void newControllerNotActive() {
            ResizeController controller = new ResizeController();
            assertThat(controller.isActive()).isFalse();
            assertThat(controller.getTarget()).isNull();
            assertThat(controller.getHandle()).isNull();
        }

        @Test
        @DisplayName("end() resets state to inactive")
        void endResetsState() {
            ResizeController controller = new ResizeController();
            controller.end();
            assertThat(controller.isActive()).isFalse();
            assertThat(controller.getTarget()).isNull();
            assertThat(controller.getHandle()).isNull();
        }

        @Test
        @DisplayName("cancel() without undo saved does not invoke undo")
        void cancelWithoutUndoDoesNotInvokeUndo() {
            ResizeController controller = new ResizeController();
            boolean[] undoCalled = {false};
            controller.cancel(() -> undoCalled[0] = true);
            assertThat(undoCalled[0]).isFalse();
            assertThat(controller.isActive()).isFalse();
        }
    }
}
