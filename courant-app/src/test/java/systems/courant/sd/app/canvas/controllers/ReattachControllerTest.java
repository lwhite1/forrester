package systems.courant.sd.app.canvas.controllers;

import systems.courant.sd.app.canvas.CanvasState;
import systems.courant.sd.app.canvas.FlowEndpointCalculator;
import systems.courant.sd.app.canvas.ModelEditor;
import systems.courant.sd.model.def.ElementType;
import systems.courant.sd.model.def.ModelDefinition;
import systems.courant.sd.model.def.ModelDefinitionBuilder;

import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("ReattachController")
class ReattachControllerTest {

    private ReattachController controller;
    private CanvasState canvasState;
    private ModelEditor editor;

    @BeforeEach
    void setUp() {
        controller = new ReattachController();
        canvasState = new CanvasState();
        editor = new ModelEditor();

        ModelDefinition def = new ModelDefinitionBuilder()
                .name("Test")
                .stock("Source", 100, "u")
                .stock("Sink", 0, "u")
                .flow("Flow", "Source * 0.1", "day", "Source", "Sink")
                .build();
        editor.loadFrom(def);

        canvasState.addElement("Source", ElementType.STOCK, 100, 200);
        canvasState.addElement("Sink", ElementType.STOCK, 300, 200);
        canvasState.addElement("Flow", ElementType.FLOW, 200, 200);

        FlowEndpointCalculator.CloudHit hit = new FlowEndpointCalculator.CloudHit(
                "Flow", FlowEndpointCalculator.FlowEnd.SOURCE, 100, 200);
        controller.start(hit, canvasState);
    }

    @Nested
    @DisplayName("complete")
    class Complete {

        @Test
        void shouldReturnTrueWhenReconnectSucceeds() {
            AtomicBoolean undoSaved = new AtomicBoolean(false);

            boolean result = controller.complete(
                    500, 500, canvasState, editor,
                    () -> undoSaved.set(true), () -> {});

            assertThat(result).isTrue();
            assertThat(undoSaved).isTrue();
        }

        @Test
        void shouldReturnFalseWhenSelfLoopRejected() {
            AtomicBoolean undoSaved = new AtomicBoolean(false);

            boolean result = controller.complete(
                    300, 200, canvasState, editor,
                    () -> undoSaved.set(true), () -> {});

            assertThat(result).isFalse();
            assertThat(undoSaved).isTrue();
        }

        @Test
        void shouldCallSaveUndoBeforeReconnect() {
            AtomicBoolean undoSaved = new AtomicBoolean(false);

            controller.complete(500, 500, canvasState, editor,
                    () -> undoSaved.set(true), () -> {});

            assertThat(undoSaved).isTrue();
        }

        @Test
        void shouldDeactivateAfterComplete() {
            controller.complete(500, 500, canvasState, editor, () -> {}, () -> {});

            assertThat(controller.isActive()).isFalse();
        }

        @Test
        void shouldDiscardUndoAndDeactivateOnException() {
            AtomicBoolean undoSaved = new AtomicBoolean(false);
            AtomicBoolean undoDiscarded = new AtomicBoolean(false);

            ModelEditor throwingEditor = new ModelEditor() {
                @Override
                public boolean reconnectFlow(String flowName,
                        FlowEndpointCalculator.FlowEnd end, String targetStock) {
                    throw new IllegalStateException("reconnect failed");
                }
            };

            assertThatThrownBy(() -> controller.complete(
                    500, 500, canvasState, throwingEditor,
                    () -> undoSaved.set(true),
                    () -> undoDiscarded.set(true)))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessage("reconnect failed");

            assertThat(undoSaved).isTrue();
            assertThat(undoDiscarded).isTrue();
            assertThat(controller.isActive()).isFalse();
        }

        @Test
        void shouldNotDiscardUndoOnSuccess() {
            AtomicBoolean undoDiscarded = new AtomicBoolean(false);

            controller.complete(500, 500, canvasState, editor,
                    () -> {}, () -> undoDiscarded.set(true));

            assertThat(undoDiscarded).isFalse();
        }

        @Test
        void shouldNotDiscardUndoOnRejection() {
            AtomicBoolean undoDiscarded = new AtomicBoolean(false);

            controller.complete(300, 200, canvasState, editor,
                    () -> {}, () -> undoDiscarded.set(true));

            assertThat(undoDiscarded).isFalse();
        }
    }
}
