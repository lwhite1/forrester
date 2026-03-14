package systems.courant.sd.app.canvas.controllers;

import systems.courant.sd.model.def.CausalLinkDef;
import systems.courant.sd.model.def.ElementType;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import systems.courant.sd.app.canvas.CanvasState;
import systems.courant.sd.app.canvas.ModelEditor;

@DisplayName("CausalLinkCreationController")
class CausalLinkCreationControllerTest {

    private CausalLinkCreationController controller;
    private CanvasState canvasState;
    private ModelEditor editor;

    @BeforeEach
    void setUp() {
        controller = new CausalLinkCreationController();
        canvasState = new CanvasState();
        editor = new ModelEditor();
        editor.addCldVariable();  // Variable 1
        editor.addCldVariable();  // Variable 2
        canvasState.addElement("Variable 1", ElementType.CLD_VARIABLE, 100, 200);
        canvasState.addElement("Variable 2", ElementType.CLD_VARIABLE, 400, 200);
    }

    @Nested
    @DisplayName("first click")
    class FirstClick {

        @Test
        void shouldSetSourceWhenClickingVariable() {
            CausalLinkCreationController.LinkResult result =
                    controller.handleClick(100, 200, canvasState, editor);

            assertThat(result.isCreated()).isFalse();
            assertThat(result.isRejected()).isFalse();
            assertThat(controller.isPending()).isTrue();

            CausalLinkCreationController.State state = controller.getState();
            assertThat(state.pending()).isTrue();
            assertThat(state.source()).isEqualTo("Variable 1");
            assertThat(state.sourceX()).isEqualTo(100);
            assertThat(state.sourceY()).isEqualTo(200);
        }

        @Test
        void shouldRejectClickOnEmptySpace() {
            CausalLinkCreationController.LinkResult result =
                    controller.handleClick(250, 100, canvasState, editor);

            assertThat(result.isRejected()).isTrue();
            assertThat(result.rejectionReason()).contains("variable");
            assertThat(controller.isPending()).isFalse();
        }
    }

    @Nested
    @DisplayName("second click")
    class SecondClick {

        @Test
        void shouldCreateLinkBetweenVariables() {
            controller.handleClick(100, 200, canvasState, editor);
            CausalLinkCreationController.LinkResult result =
                    controller.handleClick(400, 200, canvasState, editor);

            assertThat(result.isCreated()).isTrue();
            assertThat(controller.isPending()).isFalse();
            assertThat(editor.getCausalLinks()).hasSize(1);
            assertThat(editor.getCausalLinks().getFirst().from()).isEqualTo("Variable 1");
            assertThat(editor.getCausalLinks().getFirst().to()).isEqualTo("Variable 2");
            assertThat(editor.getCausalLinks().getFirst().polarity())
                    .isEqualTo(CausalLinkDef.Polarity.UNKNOWN);
        }

        @Test
        void shouldRejectClickOnEmptySpace() {
            controller.handleClick(100, 200, canvasState, editor);
            CausalLinkCreationController.LinkResult result =
                    controller.handleClick(300, 300, canvasState, editor);

            assertThat(result.isRejected()).isTrue();
            assertThat(result.rejectionReason()).contains("variable");
            assertThat(controller.isPending()).isFalse();
        }
    }

    @Nested
    @DisplayName("validation")
    class Validation {

        @Test
        void shouldAllowSelfLoop() {
            controller.handleClick(100, 200, canvasState, editor);
            CausalLinkCreationController.LinkResult result =
                    controller.handleClick(100, 200, canvasState, editor);

            assertThat(result.isCreated()).isTrue();
            assertThat(controller.isPending()).isFalse();
            assertThat(editor.getCausalLinks()).hasSize(1);
            assertThat(editor.getCausalLinks().getFirst().from()).isEqualTo("Variable 1");
            assertThat(editor.getCausalLinks().getFirst().to()).isEqualTo("Variable 1");
        }

        @Test
        void shouldRejectDuplicateLink() {
            // Create first link
            controller.handleClick(100, 200, canvasState, editor);
            controller.handleClick(400, 200, canvasState, editor);

            // Try to create same link again
            controller.handleClick(100, 200, canvasState, editor);
            CausalLinkCreationController.LinkResult result =
                    controller.handleClick(400, 200, canvasState, editor);

            assertThat(result.isRejected()).isTrue();
            assertThat(result.rejectionReason()).contains("already exists");
            assertThat(editor.getCausalLinks()).hasSize(1);
        }
    }

    @Nested
    @DisplayName("cancel")
    class Cancel {

        @Test
        void shouldResetAllState() {
            controller.handleClick(100, 200, canvasState, editor);
            assertThat(controller.isPending()).isTrue();

            controller.cancel();

            assertThat(controller.isPending()).isFalse();
            CausalLinkCreationController.State state = controller.getState();
            assertThat(state.pending()).isFalse();
        }
    }

    @Nested
    @DisplayName("rubber band")
    class RubberBand {

        @Test
        void shouldUpdateRubberBandEndpoint() {
            controller.handleClick(100, 200, canvasState, editor);
            controller.updateRubberBand(300, 250);

            CausalLinkCreationController.State state = controller.getState();
            assertThat(state.rubberBandEndX()).isEqualTo(300);
            assertThat(state.rubberBandEndY()).isEqualTo(250);
        }

        @Test
        void shouldReturnIdleStateWhenNotPending() {
            CausalLinkCreationController.State state = controller.getState();
            assertThat(state.pending()).isFalse();
        }
    }

    @Nested
    @DisplayName("cross-type links")
    class CrossTypeLinks {

        @Test
        void shouldCreateLinkFromStockToCldVariable() {
            editor.addStock();
            canvasState.addElement("Stock 1", ElementType.STOCK, 600, 200);

            controller.handleClick(600, 200, canvasState, editor);
            CausalLinkCreationController.LinkResult result =
                    controller.handleClick(100, 200, canvasState, editor);

            assertThat(result.isCreated()).isTrue();
            assertThat(editor.getCausalLinks()).hasSize(1);
            assertThat(editor.getCausalLinks().getFirst().from()).isEqualTo("Stock 1");
            assertThat(editor.getCausalLinks().getFirst().to()).isEqualTo("Variable 1");
        }
    }
}
