package com.deathrayresearch.forrester.app.canvas;

import com.deathrayresearch.forrester.model.def.ElementType;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

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
            String result = controller.handleClick(100, 200, canvasState, editor);

            assertThat(result).isNull();
            assertThat(controller.isPending()).isTrue();

            FlowCreationController.State state = controller.getState();
            assertThat(state.pending()).isTrue();
            assertThat(state.source()).isEqualTo("Stock 1");
            assertThat(state.sourceX()).isEqualTo(100);
            assertThat(state.sourceY()).isEqualTo(200);
        }

        @Test
        void shouldSetCloudSourceWhenClickingEmptySpace() {
            String result = controller.handleClick(250, 100, canvasState, editor);

            assertThat(result).isNull();
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
            String name = controller.handleClick(400, 200, canvasState, editor);

            assertThat(name).isNotNull();
            assertThat(controller.isPending()).isFalse();
            assertThat(canvasState.hasElement(name)).isTrue();
            assertThat(canvasState.getType(name)).isEqualTo(ElementType.FLOW);
            // Midpoint between (100,200) and (400,200)
            assertThat(canvasState.getX(name)).isEqualTo(250);
            assertThat(canvasState.getY(name)).isEqualTo(200);
        }

        @Test
        void shouldCreateFlowWithCloudSink() {
            controller.handleClick(100, 200, canvasState, editor);
            String name = controller.handleClick(300, 300, canvasState, editor);

            assertThat(name).isNotNull();
            assertThat(controller.isPending()).isFalse();
            // Midpoint between (100,200) and (300,300)
            assertThat(canvasState.getX(name)).isEqualTo(200);
            assertThat(canvasState.getY(name)).isEqualTo(250);
        }

        @Test
        void shouldCreateFlowWithCloudSource() {
            controller.handleClick(250, 100, canvasState, editor); // cloud
            String name = controller.handleClick(400, 200, canvasState, editor); // Stock 2

            assertThat(name).isNotNull();
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
            String name = controller.handleClick(100, 200, canvasState, editor); // Stock 1 again

            assertThat(name).isNull();
            assertThat(controller.isPending()).isTrue(); // stays pending
        }

        @Test
        void shouldRejectCloudToCloudFlow() {
            controller.handleClick(250, 100, canvasState, editor); // cloud
            String name = controller.handleClick(300, 300, canvasState, editor); // cloud

            assertThat(name).isNull();
            assertThat(controller.isPending()).isTrue(); // stays pending
        }

        @Test
        void shouldAllowCloudSourceWithStockSink() {
            controller.handleClick(250, 100, canvasState, editor); // cloud
            String name = controller.handleClick(400, 200, canvasState, editor); // Stock 2

            assertThat(name).isNotNull();
            assertThat(controller.isPending()).isFalse();
        }

        @Test
        void shouldAllowStockSourceWithCloudSink() {
            controller.handleClick(100, 200, canvasState, editor); // Stock 1
            String name = controller.handleClick(300, 300, canvasState, editor); // cloud

            assertThat(name).isNotNull();
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
