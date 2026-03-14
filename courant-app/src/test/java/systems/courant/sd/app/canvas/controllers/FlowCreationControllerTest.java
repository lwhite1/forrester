package systems.courant.sd.app.canvas.controllers;

import systems.courant.sd.model.def.ElementType;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import systems.courant.sd.app.canvas.CanvasState;
import systems.courant.sd.app.canvas.ModelEditor;

@DisplayName("FlowCreationController")
class FlowCreationControllerTest {

    private FlowCreationController controller;
    private CanvasState canvasState;
    private ModelEditor editor;

    @BeforeEach
    void setUp() {
        controller = new FlowCreationController();
        canvasState = new CanvasState();
        editor = new ModelEditor();
        editor.addStock(); // Stock 1
        editor.addStock(); // Stock 2
        canvasState.addElement("Stock 1", ElementType.STOCK, 100, 200);
        canvasState.addElement("Stock 2", ElementType.STOCK, 400, 200);
    }

    @Nested
    @DisplayName("first click")
    class FirstClick {

        @Test
        void shouldSetSourceWhenClickingStock() {
            FlowCreationController.FlowResult result =
                    controller.handleClick(100, 200, canvasState, editor);

            assertThat(result.isCreated()).isFalse();
            assertThat(result.isRejected()).isFalse();
            assertThat(controller.isPending()).isTrue();

            FlowCreationController.State state = controller.getState();
            assertThat(state.pending()).isTrue();
            assertThat(state.source()).isEqualTo("Stock 1");
            assertThat(state.sourceX()).isEqualTo(100);
            assertThat(state.sourceY()).isEqualTo(200);
        }

        @Test
        void shouldSetCloudSourceWhenClickingEmptySpace() {
            FlowCreationController.FlowResult result =
                    controller.handleClick(250, 100, canvasState, editor);

            assertThat(result.isCreated()).isFalse();
            assertThat(result.isRejected()).isFalse();
            assertThat(controller.isPending()).isTrue();

            FlowCreationController.State state = controller.getState();
            assertThat(state.source()).isNull();
            assertThat(state.sourceX()).isEqualTo(250);
            assertThat(state.sourceY()).isEqualTo(100);
        }
    }

    @Nested
    @DisplayName("second click")
    class SecondClick {

        @Test
        void shouldCreateFlowBetweenStocks() {
            controller.handleClick(100, 200, canvasState, editor);
            FlowCreationController.FlowResult result =
                    controller.handleClick(400, 200, canvasState, editor);

            assertThat(result.isCreated()).isTrue();
            String name = result.flowName();
            assertThat(controller.isPending()).isFalse();
            assertThat(canvasState.hasElement(name)).isTrue();
            assertThat(canvasState.getType(name)).hasValue(ElementType.FLOW);
            // Midpoint between (100,200) and (400,200)
            assertThat(canvasState.getX(name)).isEqualTo(250);
            assertThat(canvasState.getY(name)).isEqualTo(200);
        }

        @Test
        void shouldCreateFlowWithCloudSink() {
            controller.handleClick(100, 200, canvasState, editor);
            FlowCreationController.FlowResult result =
                    controller.handleClick(300, 300, canvasState, editor);

            assertThat(result.isCreated()).isTrue();
            String name = result.flowName();
            assertThat(controller.isPending()).isFalse();
            // Midpoint between (100,200) and (300,300)
            assertThat(canvasState.getX(name)).isEqualTo(200);
            assertThat(canvasState.getY(name)).isEqualTo(250);
        }

        @Test
        void shouldCreateFlowWithCloudSource() {
            controller.handleClick(250, 100, canvasState, editor); // cloud
            FlowCreationController.FlowResult result =
                    controller.handleClick(400, 200, canvasState, editor); // Stock 2

            assertThat(result.isCreated()).isTrue();
            String name = result.flowName();
            assertThat(controller.isPending()).isFalse();
            // Midpoint between (250,100) and (400,200)
            assertThat(canvasState.getX(name)).isEqualTo(325);
            assertThat(canvasState.getY(name)).isEqualTo(150);
        }
    }

    @Nested
    @DisplayName("validation")
    class Validation {

        @Test
        void shouldRejectSelfLoop() {
            controller.handleClick(100, 200, canvasState, editor); // Stock 1
            FlowCreationController.FlowResult result =
                    controller.handleClick(100, 200, canvasState, editor); // Stock 1 again

            assertThat(result.isRejected()).isTrue();
            assertThat(result.rejectionReason()).contains("self-loop");
            assertThat(controller.isPending()).isFalse();
        }

        @Test
        void shouldRejectCloudToCloudFlow() {
            controller.handleClick(250, 100, canvasState, editor); // cloud
            FlowCreationController.FlowResult result =
                    controller.handleClick(300, 300, canvasState, editor); // cloud

            assertThat(result.isRejected()).isTrue();
            assertThat(result.rejectionReason()).contains("stock");
            assertThat(controller.isPending()).isFalse();
        }

        @Test
        void shouldAllowCloudSourceWithStockSink() {
            controller.handleClick(250, 100, canvasState, editor); // cloud
            FlowCreationController.FlowResult result =
                    controller.handleClick(400, 200, canvasState, editor); // Stock 2

            assertThat(result.isCreated()).isTrue();
            assertThat(controller.isPending()).isFalse();
        }

        @Test
        void shouldAllowStockSourceWithCloudSink() {
            controller.handleClick(100, 200, canvasState, editor); // Stock 1
            FlowCreationController.FlowResult result =
                    controller.handleClick(300, 300, canvasState, editor); // cloud

            assertThat(result.isCreated()).isTrue();
            assertThat(controller.isPending()).isFalse();
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
            FlowCreationController.State state = controller.getState();
            assertThat(state.pending()).isFalse();
        }
    }

    @Nested
    @DisplayName("state record")
    class StateRecord {

        @Test
        void shouldReflectIdleState() {
            FlowCreationController.State state = controller.getState();
            assertThat(state.pending()).isFalse();
        }

        @Test
        void shouldReflectPendingState() {
            controller.handleClick(100, 200, canvasState, editor);
            controller.updateRubberBand(300, 250);

            FlowCreationController.State state = controller.getState();
            assertThat(state.pending()).isTrue();
            assertThat(state.rubberBandEndX()).isEqualTo(300);
            assertThat(state.rubberBandEndY()).isEqualTo(250);
        }
    }
}
