package systems.courant.sd.app.canvas.controllers;

import systems.courant.sd.model.def.ElementType;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import systems.courant.sd.app.canvas.CanvasState;
import systems.courant.sd.app.canvas.renderers.CanvasRenderer;

@DisplayName("MarqueeController")
class MarqueeControllerTest {

    private MarqueeController controller;
    private CanvasState state;

    @BeforeEach
    void setUp() {
        controller = new MarqueeController();
        state = new CanvasState();

        state.addElement("A", ElementType.STOCK, 100, 100);
        state.addElement("B", ElementType.STOCK, 200, 200);
        state.addElement("C", ElementType.AUX, 300, 300);
        state.addElement("D", ElementType.AUX, 500, 500);
    }

    @Nested
    @DisplayName("initial state")
    class InitialState {

        @Test
        void shouldNotBeActive() {
            assertThat(controller.isActive()).isFalse();
        }

        @Test
        void shouldReturnIdleRenderState() {
            CanvasRenderer.MarqueeState renderState = controller.toRenderState();

            assertThat(renderState.active()).isFalse();
        }
    }

    @Nested
    @DisplayName("start")
    class Start {

        @Test
        void shouldBecomeActive() {
            controller.start(50, 50, state, false, false);

            assertThat(controller.isActive()).isTrue();
        }

        @Test
        void shouldClearSelection_whenNoShift() {
            state.select("A");

            controller.start(50, 50, state, false, false);

            assertThat(state.getSelection()).isEmpty();
        }

        @Test
        void shouldPreserveSelection_whenShiftDown() {
            state.select("A");

            controller.start(50, 50, state, true, false);

            assertThat(state.isSelected("A")).isTrue();
        }
    }

    @Nested
    @DisplayName("drag")
    class Drag {

        @Test
        void shouldSelectElementsInsideRectangle() {
            controller.start(50, 50, state, false, false);

            controller.drag(250, 250, state);

            assertThat(state.isSelected("A")).isTrue();
            assertThat(state.isSelected("B")).isTrue();
            assertThat(state.isSelected("C")).isFalse();
            assertThat(state.isSelected("D")).isFalse();
        }

        @Test
        void shouldSelectAllElements_whenRectangleCoversAll() {
            controller.start(0, 0, state, false, false);

            controller.drag(600, 600, state);

            assertThat(state.getSelection()).containsExactlyInAnyOrder("A", "B", "C", "D");
        }

        @Test
        void shouldSelectNone_whenRectangleCoversNone() {
            controller.start(400, 0, state, false, false);

            controller.drag(450, 50, state);

            assertThat(state.getSelection()).isEmpty();
        }

        @Test
        void shouldHandleReversedRectangle() {
            // Drag from bottom-right to top-left
            controller.start(250, 250, state, false, false);

            controller.drag(50, 50, state);

            assertThat(state.isSelected("A")).isTrue();
            assertThat(state.isSelected("B")).isTrue();
        }

        @Test
        void shouldAddToInitialSelection_whenShiftDown() {
            state.select("D");

            controller.start(50, 50, state, true, false);
            controller.drag(150, 150, state);

            assertThat(state.isSelected("A")).isTrue();
            assertThat(state.isSelected("D")).isTrue();
        }

        @Test
        void shouldUpdateSelectionAsRectangleChanges() {
            controller.start(50, 50, state, false, false);

            // First drag selects A
            controller.drag(150, 150, state);
            assertThat(state.isSelected("A")).isTrue();
            assertThat(state.isSelected("B")).isFalse();

            // Expand to include B
            controller.drag(250, 250, state);
            assertThat(state.isSelected("A")).isTrue();
            assertThat(state.isSelected("B")).isTrue();
        }

        @Test
        void shouldReturnActiveRenderState() {
            controller.start(50, 50, state, false, false);
            controller.drag(250, 250, state);

            CanvasRenderer.MarqueeState renderState = controller.toRenderState();

            assertThat(renderState.active()).isTrue();
        }
    }

    @Nested
    @DisplayName("end")
    class End {

        @Test
        void shouldDeactivate() {
            controller.start(50, 50, state, false, false);
            controller.drag(250, 250, state);

            controller.end();

            assertThat(controller.isActive()).isFalse();
        }

        @Test
        void shouldPreserveSelection() {
            controller.start(50, 50, state, false, false);
            controller.drag(250, 250, state);

            controller.end();

            assertThat(state.isSelected("A")).isTrue();
            assertThat(state.isSelected("B")).isTrue();
        }

        @Test
        void shouldReturnIdleRenderState() {
            controller.start(50, 50, state, false, false);
            controller.drag(250, 250, state);

            controller.end();

            assertThat(controller.toRenderState().active()).isFalse();
        }
    }

    @Nested
    @DisplayName("cancel")
    class Cancel {

        @Test
        void shouldDeactivate() {
            controller.start(50, 50, state, false, false);
            controller.drag(250, 250, state);

            controller.cancel(state);

            assertThat(controller.isActive()).isFalse();
        }

        @Test
        void shouldRestoreOriginalSelection() {
            state.select("D");

            controller.start(50, 50, state, false, false);
            controller.drag(250, 250, state);
            // Now A and B are selected, D is not

            controller.cancel(state);

            assertThat(state.isSelected("D")).isTrue();
            assertThat(state.isSelected("A")).isFalse();
            assertThat(state.isSelected("B")).isFalse();
        }

        @Test
        void shouldClearSelection_whenOriginalWasEmpty() {
            controller.start(50, 50, state, false, false);
            controller.drag(250, 250, state);

            controller.cancel(state);

            assertThat(state.getSelection()).isEmpty();
        }

        @Test
        void shouldRestoreShiftSelection() {
            state.select("D");

            controller.start(50, 50, state, true, false);
            controller.drag(150, 150, state);
            // A and D are selected

            controller.cancel(state);

            // Should restore to just D (the initial selection)
            assertThat(state.isSelected("D")).isTrue();
            assertThat(state.isSelected("A")).isFalse();
        }
    }

    @Nested
    @DisplayName("hideVariables")
    class HideVariables {

        @Test
        void shouldSkipHiddenVariables() {
            // C and D are AUX elements; with hideAux=true they should not be selected
            controller.start(0, 0, state, false, true);

            controller.drag(600, 600, state);

            assertThat(state.isSelected("A")).isTrue();
            assertThat(state.isSelected("B")).isTrue();
            assertThat(state.isSelected("C")).isFalse();
            assertThat(state.isSelected("D")).isFalse();
        }

        @Test
        void shouldIncludeVariables_whenNotHidden() {
            controller.start(0, 0, state, false, false);

            controller.drag(600, 600, state);

            assertThat(state.getSelection()).containsExactlyInAnyOrder("A", "B", "C", "D");
        }
    }
}
