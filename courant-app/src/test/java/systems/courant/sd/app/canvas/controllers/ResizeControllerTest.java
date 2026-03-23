package systems.courant.sd.app.canvas.controllers;

import javafx.scene.Cursor;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import systems.courant.sd.app.canvas.CanvasState;
import systems.courant.sd.app.canvas.LayoutMetrics;
import systems.courant.sd.app.canvas.ResizeHandle;
import systems.courant.sd.app.canvas.renderers.SelectionRenderer;
import systems.courant.sd.model.def.ElementType;

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

    @Nested
    @DisplayName("drag() anchor stability")
    class DragAnchorStability {

        private static final double PAD = SelectionRenderer.SELECTION_PADDING;
        private static final double TOLERANCE = 0.01;

        private CanvasState stateWithStock(double cx, double cy) {
            CanvasState state = new CanvasState();
            state.addElement("s", ElementType.STOCK, cx, cy);
            return state;
        }

        private double anchorCornerX(CanvasState state) {
            return state.getX("s") - (LayoutMetrics.effectiveWidth(state, "s") / 2 + PAD);
        }

        private double anchorCornerY(CanvasState state) {
            return state.getY("s") - (LayoutMetrics.effectiveHeight(state, "s") / 2 + PAD);
        }

        @Test
        @DisplayName("Dragging BOTTOM_RIGHT keeps TOP_LEFT corner fixed")
        void shouldKeepTopLeftFixedWhenDraggingBottomRight() {
            double cx = 200, cy = 200;
            CanvasState state = stateWithStock(cx, cy);
            double halfW = LayoutMetrics.effectiveWidth(state, "s") / 2 + PAD;
            double halfH = LayoutMetrics.effectiveHeight(state, "s") / 2 + PAD;

            // Top-left corner position before resize
            double topLeftX = cx - halfW;
            double topLeftY = cy - halfH;

            ResizeController controller = new ResizeController();
            controller.start(new ResizeHandle.HandleHit("s", ResizeHandle.BOTTOM_RIGHT), state);

            // Drag bottom-right corner outward by 30px
            double cursorX = cx + halfW + 30;
            double cursorY = cy + halfH + 30;
            controller.drag(cursorX, cursorY, state, () -> {});

            // Top-left corner must remain where it was
            double newTopLeftX = state.getX("s") - (LayoutMetrics.effectiveWidth(state, "s") / 2 + PAD);
            double newTopLeftY = state.getY("s") - (LayoutMetrics.effectiveHeight(state, "s") / 2 + PAD);
            assertThat(newTopLeftX).isCloseTo(topLeftX, within(TOLERANCE));
            assertThat(newTopLeftY).isCloseTo(topLeftY, within(TOLERANCE));
        }

        @Test
        @DisplayName("Dragging TOP_LEFT keeps BOTTOM_RIGHT corner fixed")
        void shouldKeepBottomRightFixedWhenDraggingTopLeft() {
            double cx = 200, cy = 200;
            CanvasState state = stateWithStock(cx, cy);
            double halfW = LayoutMetrics.effectiveWidth(state, "s") / 2 + PAD;
            double halfH = LayoutMetrics.effectiveHeight(state, "s") / 2 + PAD;

            // Bottom-right corner position before resize
            double bottomRightX = cx + halfW;
            double bottomRightY = cy + halfH;

            ResizeController controller = new ResizeController();
            controller.start(new ResizeHandle.HandleHit("s", ResizeHandle.TOP_LEFT), state);

            // Drag top-left corner outward by 20px
            double cursorX = cx - halfW - 20;
            double cursorY = cy - halfH - 20;
            controller.drag(cursorX, cursorY, state, () -> {});

            // Bottom-right corner must remain where it was
            double newBottomRightX = state.getX("s") + (LayoutMetrics.effectiveWidth(state, "s") / 2 + PAD);
            double newBottomRightY = state.getY("s") + (LayoutMetrics.effectiveHeight(state, "s") / 2 + PAD);
            assertThat(newBottomRightX).isCloseTo(bottomRightX, within(TOLERANCE));
            assertThat(newBottomRightY).isCloseTo(bottomRightY, within(TOLERANCE));
        }

        @Test
        @DisplayName("Dragging TOP_RIGHT keeps BOTTOM_LEFT corner fixed")
        void shouldKeepBottomLeftFixedWhenDraggingTopRight() {
            double cx = 200, cy = 200;
            CanvasState state = stateWithStock(cx, cy);
            double halfW = LayoutMetrics.effectiveWidth(state, "s") / 2 + PAD;
            double halfH = LayoutMetrics.effectiveHeight(state, "s") / 2 + PAD;

            double bottomLeftX = cx - halfW;
            double bottomLeftY = cy + halfH;

            ResizeController controller = new ResizeController();
            controller.start(new ResizeHandle.HandleHit("s", ResizeHandle.TOP_RIGHT), state);

            controller.drag(cx + halfW + 15, cy - halfH - 15, state, () -> {});

            double newBottomLeftX = state.getX("s") - (LayoutMetrics.effectiveWidth(state, "s") / 2 + PAD);
            double newBottomLeftY = state.getY("s") + (LayoutMetrics.effectiveHeight(state, "s") / 2 + PAD);
            assertThat(newBottomLeftX).isCloseTo(bottomLeftX, within(TOLERANCE));
            assertThat(newBottomLeftY).isCloseTo(bottomLeftY, within(TOLERANCE));
        }

        @Test
        @DisplayName("Dragging BOTTOM_LEFT keeps TOP_RIGHT corner fixed")
        void shouldKeepTopRightFixedWhenDraggingBottomLeft() {
            double cx = 200, cy = 200;
            CanvasState state = stateWithStock(cx, cy);
            double halfW = LayoutMetrics.effectiveWidth(state, "s") / 2 + PAD;
            double halfH = LayoutMetrics.effectiveHeight(state, "s") / 2 + PAD;

            double topRightX = cx + halfW;
            double topRightY = cy - halfH;

            ResizeController controller = new ResizeController();
            controller.start(new ResizeHandle.HandleHit("s", ResizeHandle.BOTTOM_LEFT), state);

            controller.drag(cx - halfW - 25, cy + halfH + 25, state, () -> {});

            double newTopRightX = state.getX("s") + (LayoutMetrics.effectiveWidth(state, "s") / 2 + PAD);
            double newTopRightY = state.getY("s") - (LayoutMetrics.effectiveHeight(state, "s") / 2 + PAD);
            assertThat(newTopRightX).isCloseTo(topRightX, within(TOLERANCE));
            assertThat(newTopRightY).isCloseTo(topRightY, within(TOLERANCE));
        }

        @Test
        @DisplayName("No-move drag preserves original center position")
        void shouldPreserveCenterWhenNotMoved() {
            double cx = 150, cy = 150;
            CanvasState state = stateWithStock(cx, cy);
            double halfW = LayoutMetrics.effectiveWidth(state, "s") / 2 + PAD;
            double halfH = LayoutMetrics.effectiveHeight(state, "s") / 2 + PAD;

            ResizeController controller = new ResizeController();
            controller.start(new ResizeHandle.HandleHit("s", ResizeHandle.BOTTOM_RIGHT), state);

            // Drag to exactly the current handle position (no movement)
            controller.drag(cx + halfW, cy + halfH, state, () -> {});

            assertThat(state.getX("s")).isCloseTo(cx, within(TOLERANCE));
            assertThat(state.getY("s")).isCloseTo(cy, within(TOLERANCE));
        }
    }
}
