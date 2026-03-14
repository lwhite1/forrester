package systems.courant.sd.app.canvas.controllers;

import systems.courant.sd.model.def.ElementType;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import systems.courant.sd.app.canvas.CanvasState;
import systems.courant.sd.app.canvas.Viewport;

@DisplayName("DragController")
class DragControllerTest {

    private DragController controller;
    private CanvasState state;
    private Viewport viewport;

    @BeforeEach
    void setUp() {
        controller = new DragController();
        state = new CanvasState();
        viewport = new Viewport();

        state.addElement("A", ElementType.STOCK, 100, 200);
        state.addElement("B", ElementType.STOCK, 300, 400);
    }

    @Nested
    @DisplayName("initial state")
    class InitialState {

        @Test
        void shouldNotBeDragging() {
            assertThat(controller.isDragging()).isFalse();
        }

        @Test
        void shouldHaveNullDragTarget() {
            assertThat(controller.getDragTarget()).isNull();
        }

        @Test
        void shouldNotHaveMoved() {
            assertThat(controller.hasMoved()).isFalse();
        }
    }

    @Nested
    @DisplayName("start")
    class Start {

        @Test
        void shouldSetDraggingTrue() {
            state.select("A");
            controller.start("A", 50, 60, state);

            assertThat(controller.isDragging()).isTrue();
        }

        @Test
        void shouldSetDragTarget() {
            state.select("A");
            controller.start("A", 50, 60, state);

            assertThat(controller.getDragTarget()).isEqualTo("A");
        }

        @Test
        void shouldNotHaveMoved() {
            state.select("A");
            controller.start("A", 50, 60, state);

            assertThat(controller.hasMoved()).isFalse();
        }
    }

    @Nested
    @DisplayName("drag")
    class Drag {

        @Test
        void shouldMoveSelectedElementByWorldDelta() {
            state.select("A");
            controller.start("A", 50, 60, state);

            controller.drag(70, 80, state, viewport, () -> {});

            // World delta = (70-50)/1.0 = 20, (80-60)/1.0 = 20
            assertThat(state.getX("A")).isCloseTo(120, within(0.001));
            assertThat(state.getY("A")).isCloseTo(220, within(0.001));
        }

        @Test
        void shouldMoveAllSelectedElements() {
            state.select("A");
            state.addToSelection("B");
            controller.start("A", 50, 60, state);

            controller.drag(70, 80, state, viewport, () -> {});

            assertThat(state.getX("A")).isCloseTo(120, within(0.001));
            assertThat(state.getY("A")).isCloseTo(220, within(0.001));
            assertThat(state.getX("B")).isCloseTo(320, within(0.001));
            assertThat(state.getY("B")).isCloseTo(420, within(0.001));
        }

        @Test
        void shouldApplyScaleToWorldDelta() {
            viewport.zoomAt(0, 0, 2.0); // scale = 2.0
            state.select("A");
            controller.start("A", 50, 60, state);

            controller.drag(70, 80, state, viewport, () -> {});

            // World delta = (70-50)/2.0 = 10, (80-60)/2.0 = 10
            assertThat(state.getX("A")).isCloseTo(110, within(0.001));
            assertThat(state.getY("A")).isCloseTo(210, within(0.001));
        }

        @Test
        void shouldSaveUndoOnFirstDrag() {
            AtomicInteger undoCount = new AtomicInteger(0);
            state.select("A");
            controller.start("A", 50, 60, state);

            controller.drag(60, 70, state, viewport, undoCount::incrementAndGet);

            assertThat(undoCount.get()).isEqualTo(1);
        }

        @Test
        void shouldNotSaveUndoOnSubsequentDrags() {
            AtomicInteger undoCount = new AtomicInteger(0);
            state.select("A");
            controller.start("A", 50, 60, state);

            controller.drag(60, 70, state, viewport, undoCount::incrementAndGet);
            controller.drag(80, 90, state, viewport, undoCount::incrementAndGet);
            controller.drag(100, 110, state, viewport, undoCount::incrementAndGet);

            assertThat(undoCount.get()).isEqualTo(1);
        }

        @Test
        void shouldReportHasMoved() {
            state.select("A");
            controller.start("A", 50, 60, state);

            controller.drag(70, 80, state, viewport, () -> {});

            assertThat(controller.hasMoved()).isTrue();
        }

        @Test
        void shouldDoNothing_whenNotDragging() {
            state.select("A");
            double originalX = state.getX("A");

            controller.drag(100, 200, state, viewport, () -> {});

            assertThat(state.getX("A")).isCloseTo(originalX, within(0.001));
        }

        @Test
        void shouldComputeDeltaRelativeToStartPosition() {
            state.select("A");
            controller.start("A", 50, 60, state);

            // Multiple drag calls should all compute delta from original start
            controller.drag(60, 70, state, viewport, () -> {});
            assertThat(state.getX("A")).isCloseTo(110, within(0.001));

            controller.drag(70, 80, state, viewport, () -> {});
            assertThat(state.getX("A")).isCloseTo(120, within(0.001));
        }
    }

    @Nested
    @DisplayName("end")
    class End {

        @Test
        void shouldSetDraggingFalse() {
            state.select("A");
            controller.start("A", 50, 60, state);
            controller.drag(70, 80, state, viewport, () -> {});

            controller.end();

            assertThat(controller.isDragging()).isFalse();
        }

        @Test
        void shouldClearDragTarget() {
            state.select("A");
            controller.start("A", 50, 60, state);

            controller.end();

            assertThat(controller.getDragTarget()).isNull();
        }

        @Test
        void shouldPreserveFinalPositions() {
            state.select("A");
            controller.start("A", 50, 60, state);
            controller.drag(70, 80, state, viewport, () -> {});

            controller.end();

            assertThat(state.getX("A")).isCloseTo(120, within(0.001));
            assertThat(state.getY("A")).isCloseTo(220, within(0.001));
        }
    }

    @Nested
    @DisplayName("lifecycle")
    class Lifecycle {

        @Test
        void shouldAllowRestartAfterEnd() {
            state.select("A");
            controller.start("A", 50, 60, state);
            controller.drag(70, 80, state, viewport, () -> {});
            controller.end();

            // Start a new drag
            controller.start("A", 0, 0, state);

            assertThat(controller.isDragging()).isTrue();
            assertThat(controller.getDragTarget()).isEqualTo("A");
        }

        @Test
        void shouldSaveUndoAgainOnNewDrag() {
            AtomicInteger undoCount = new AtomicInteger(0);
            state.select("A");

            controller.start("A", 50, 60, state);
            controller.drag(70, 80, state, viewport, undoCount::incrementAndGet);
            controller.end();

            controller.start("A", 0, 0, state);
            controller.drag(10, 10, state, viewport, undoCount::incrementAndGet);

            assertThat(undoCount.get()).isEqualTo(2);
        }
    }
}
