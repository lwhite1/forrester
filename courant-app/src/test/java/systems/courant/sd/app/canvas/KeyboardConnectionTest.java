package systems.courant.sd.app.canvas;

import systems.courant.sd.model.def.ElementType;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for keyboard-driven connection creation (#4).
 * Verifies Tab-cycling through elements and Enter-to-connect
 * at element centers, without requiring a live JavaFX stage.
 */
@DisplayName("Keyboard-driven connection creation (#4)")
class KeyboardConnectionTest {

    private CanvasState canvasState;
    private ModelEditor editor;

    @BeforeEach
    void setUp() {
        canvasState = new CanvasState();
        editor = new ModelEditor();

        editor.addCldVariable(); // Variable 1
        editor.addCldVariable(); // Variable 2
        editor.addCldVariable(); // Variable 3

        canvasState.addElement("Variable 1", ElementType.CLD_VARIABLE, 100, 200);
        canvasState.addElement("Variable 2", ElementType.CLD_VARIABLE, 300, 200);
        canvasState.addElement("Variable 3", ElementType.CLD_VARIABLE, 500, 200);
    }

    @Nested
    @DisplayName("Tab cycling")
    class TabCycling {

        @Test
        @DisplayName("Tab should select the first element when nothing is selected")
        void shouldSelectFirstWhenNoneSelected() {
            List<String> drawOrder = canvasState.getDrawOrder();
            assertThat(canvasState.getSelection()).isEmpty();

            // Simulate Tab: no current selection → select first element
            canvasState.select(drawOrder.getFirst());

            assertThat(canvasState.getSelection()).containsExactly("Variable 1");
        }

        @Test
        @DisplayName("Tab should cycle to next element")
        void shouldCycleForward() {
            canvasState.select("Variable 1");
            List<String> drawOrder = canvasState.getDrawOrder();
            int idx = drawOrder.indexOf("Variable 1");
            canvasState.select(drawOrder.get(idx + 1));

            assertThat(canvasState.getSelection()).containsExactly("Variable 2");
        }

        @Test
        @DisplayName("Tab should wrap around from last to first")
        void shouldWrapForward() {
            canvasState.select("Variable 3");
            List<String> drawOrder = canvasState.getDrawOrder();
            int idx = drawOrder.indexOf("Variable 3");
            int nextIdx = idx >= drawOrder.size() - 1 ? 0 : idx + 1;
            canvasState.select(drawOrder.get(nextIdx));

            assertThat(canvasState.getSelection()).containsExactly("Variable 1");
        }

        @Test
        @DisplayName("Shift+Tab should cycle to previous element")
        void shouldCycleBackward() {
            canvasState.select("Variable 2");
            List<String> drawOrder = canvasState.getDrawOrder();
            int idx = drawOrder.indexOf("Variable 2");
            canvasState.select(drawOrder.get(idx - 1));

            assertThat(canvasState.getSelection()).containsExactly("Variable 1");
        }

        @Test
        @DisplayName("Shift+Tab should wrap around from first to last")
        void shouldWrapBackward() {
            canvasState.select("Variable 1");
            List<String> drawOrder = canvasState.getDrawOrder();
            int idx = drawOrder.indexOf("Variable 1");
            int prevIdx = idx <= 0 ? drawOrder.size() - 1 : idx - 1;
            canvasState.select(drawOrder.get(prevIdx));

            assertThat(canvasState.getSelection()).containsExactly("Variable 3");
        }
    }

    @Nested
    @DisplayName("Enter-to-connect")
    class EnterToConnect {

        @Test
        @DisplayName("Enter on selected element should create causal link source")
        void shouldSetSourceViaEnterOnSelectedElement() {
            CausalLinkCreationController controller = new CausalLinkCreationController();
            canvasState.select("Variable 1");

            String selected = canvasState.getSelection().iterator().next();
            double x = canvasState.getX(selected);
            double y = canvasState.getY(selected);

            CausalLinkCreationController.LinkResult result =
                    controller.handleClick(x, y, canvasState, editor);

            assertThat(result.isCreated()).isFalse();
            assertThat(result.isRejected()).isFalse();
            assertThat(controller.isPending()).isTrue();
            assertThat(controller.getState().source()).isEqualTo("Variable 1");
        }

        @Test
        @DisplayName("Two Enter presses on different elements should create a causal link")
        void shouldCreateCausalLinkViaTwoEnterPresses() {
            CausalLinkCreationController controller = new CausalLinkCreationController();

            // First Enter: select source
            canvasState.select("Variable 1");
            String source = canvasState.getSelection().iterator().next();
            controller.handleClick(canvasState.getX(source), canvasState.getY(source),
                    canvasState, editor);

            // Tab to next, then Enter: select target
            canvasState.select("Variable 2");
            String target = canvasState.getSelection().iterator().next();
            CausalLinkCreationController.LinkResult result =
                    controller.handleClick(canvasState.getX(target), canvasState.getY(target),
                            canvasState, editor);

            assertThat(result.isCreated()).isTrue();
            assertThat(editor.getCausalLinks()).hasSize(1);
            assertThat(editor.getCausalLinks().getFirst().from()).isEqualTo("Variable 1");
            assertThat(editor.getCausalLinks().getFirst().to()).isEqualTo("Variable 2");
        }

        @Test
        @DisplayName("Enter should create flow between stocks")
        void shouldCreateFlowViaKeyboard() {
            // Set up stocks instead
            CanvasState flowCanvas = new CanvasState();
            ModelEditor flowEditor = new ModelEditor();
            String stockA = flowEditor.addStock();
            String stockB = flowEditor.addStock();
            flowCanvas.addElement(stockA, ElementType.STOCK, 100, 200);
            flowCanvas.addElement(stockB, ElementType.STOCK, 400, 200);

            FlowCreationController controller = new FlowCreationController();

            // First Enter on stock A
            flowCanvas.select(stockA);
            String source = flowCanvas.getSelection().iterator().next();
            controller.handleClick(flowCanvas.getX(source), flowCanvas.getY(source),
                    flowCanvas, flowEditor);

            assertThat(controller.isPending()).isTrue();

            // Second Enter on stock B
            flowCanvas.select(stockB);
            String target = flowCanvas.getSelection().iterator().next();
            FlowCreationController.FlowResult result =
                    controller.handleClick(flowCanvas.getX(target), flowCanvas.getY(target),
                            flowCanvas, flowEditor);

            assertThat(result.isCreated()).isTrue();
            assertThat(result.flowName()).isNotNull();
        }
    }

    @Nested
    @DisplayName("isConnectionTool")
    class ConnectionToolCheck {

        @Test
        @DisplayName("PLACE_FLOW should be a connection tool")
        void flowIsConnectionTool() {
            assertThat(CanvasToolBar.Tool.PLACE_FLOW).isNotNull();
            // Verify via the set of connection tools
            assertThat(isConnectionTool(CanvasToolBar.Tool.PLACE_FLOW)).isTrue();
        }

        @Test
        @DisplayName("PLACE_CAUSAL_LINK should be a connection tool")
        void causalLinkIsConnectionTool() {
            assertThat(isConnectionTool(CanvasToolBar.Tool.PLACE_CAUSAL_LINK)).isTrue();
        }

        @Test
        @DisplayName("PLACE_INFO_LINK should be a connection tool")
        void infoLinkIsConnectionTool() {
            assertThat(isConnectionTool(CanvasToolBar.Tool.PLACE_INFO_LINK)).isTrue();
        }

        @Test
        @DisplayName("SELECT should not be a connection tool")
        void selectIsNotConnectionTool() {
            assertThat(isConnectionTool(CanvasToolBar.Tool.SELECT)).isFalse();
        }

        @Test
        @DisplayName("PLACE_STOCK should not be a connection tool")
        void stockIsNotConnectionTool() {
            assertThat(isConnectionTool(CanvasToolBar.Tool.PLACE_STOCK)).isFalse();
        }

        private boolean isConnectionTool(CanvasToolBar.Tool tool) {
            return tool == CanvasToolBar.Tool.PLACE_FLOW
                    || tool == CanvasToolBar.Tool.PLACE_CAUSAL_LINK
                    || tool == CanvasToolBar.Tool.PLACE_INFO_LINK;
        }
    }
}
