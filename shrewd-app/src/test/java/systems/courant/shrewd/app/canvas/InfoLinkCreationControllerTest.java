package systems.courant.shrewd.app.canvas;

import systems.courant.shrewd.model.def.ElementType;
import systems.courant.shrewd.model.def.ModelDefinition;
import systems.courant.shrewd.model.def.ModuleInstanceDef;
import systems.courant.shrewd.model.def.ModuleInterface;
import systems.courant.shrewd.model.def.PortDef;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("InfoLinkCreationController")
class InfoLinkCreationControllerTest {

    private InfoLinkCreationController controller;
    private CanvasState canvasState;
    private ModelEditor editor;

    @BeforeEach
    void setUp() {
        controller = new InfoLinkCreationController();
        canvasState = new CanvasState();
        editor = new ModelEditor();

        // Add a regular variable
        editor.addVariable();  // Aux 1
        canvasState.addElement("Variable 1", ElementType.AUX, 100, 200);

        // Add a second variable
        editor.addVariable();  // Aux 2
        canvasState.addElement("Variable 2", ElementType.AUX, 500, 200);

        // Add a module with input and output ports
        ModuleInterface iface = new ModuleInterface(
                List.of(new PortDef("inPort", "units")),
                List.of(new PortDef("outPort", "units"))
        );
        ModelDefinition moduleDef = new ModelDefinition(
                "Module 1", null, iface,
                List.of(), List.of(), List.of(),
                List.of(), List.of(), List.of(), List.of(), null);
        editor.addModuleFrom(new ModuleInstanceDef("Module 1", moduleDef, Map.of(), Map.of()));
        canvasState.addElement("Module 1", ElementType.MODULE, 300, 200);
    }

    /** Computes the Y coordinate of port 0 out of 1 port for the module at (300, 200). */
    private double portY() {
        double halfH = LayoutMetrics.MODULE_HEIGHT / 2;
        return PortGeometry.portY(200 - halfH, LayoutMetrics.MODULE_HEIGHT, 0, 1);
    }

    /** Computes the input port X for the module at center X=300. */
    private double inputPortX() {
        return PortGeometry.inputPortX(300, LayoutMetrics.MODULE_WIDTH / 2);
    }

    /** Computes the output port X for the module at center X=300. */
    private double outputPortX() {
        return PortGeometry.outputPortX(300, LayoutMetrics.MODULE_WIDTH / 2);
    }

    @Nested
    @DisplayName("element to input port")
    class ElementToInputPort {

        @Test
        void shouldCreateInputBinding() {
            // First click: element
            InfoLinkCreationController.LinkResult r1 =
                    controller.handleClick(100, 200, canvasState, editor);
            assertThat(r1.isCreated()).isFalse();
            assertThat(r1.isRejected()).isFalse();
            assertThat(controller.isPending()).isTrue();

            // Second click: input port
            InfoLinkCreationController.LinkResult r2 =
                    controller.handleClick(inputPortX(), portY(), canvasState, editor);
            assertThat(r2.isCreated()).isTrue();
            assertThat(controller.isPending()).isFalse();

            // Verify binding was created
            ModuleInstanceDef module = editor.getModuleByName("Module 1").orElseThrow();
            assertThat(module.inputBindings()).containsEntry("inPort", "Variable_1");
        }
    }

    @Nested
    @DisplayName("output port to element")
    class OutputPortToElement {

        @Test
        void shouldCreateOutputBinding() {
            // First click: output port
            InfoLinkCreationController.LinkResult r1 =
                    controller.handleClick(outputPortX(), portY(), canvasState, editor);
            assertThat(r1.isCreated()).isFalse();
            assertThat(r1.isRejected()).isFalse();
            assertThat(controller.isPending()).isTrue();

            // Second click: element
            InfoLinkCreationController.LinkResult r2 =
                    controller.handleClick(500, 200, canvasState, editor);
            assertThat(r2.isCreated()).isTrue();
            assertThat(controller.isPending()).isFalse();

            // Verify binding was created
            ModuleInstanceDef module = editor.getModuleByName("Module 1").orElseThrow();
            assertThat(module.outputBindings()).containsEntry("outPort", "Variable 2");
        }
    }

    @Nested
    @DisplayName("rejection cases")
    class RejectionCases {

        @Test
        void shouldRejectClickOnEmptySpace() {
            InfoLinkCreationController.LinkResult result =
                    controller.handleClick(800, 800, canvasState, editor);
            assertThat(result.isRejected()).isTrue();
            assertThat(result.rejectionReason()).contains("element");
            assertThat(controller.isPending()).isFalse();
        }

        @Test
        void shouldRejectElementToOutputPort() {
            // First click: element
            controller.handleClick(100, 200, canvasState, editor);

            // Second click: output port — wrong direction
            InfoLinkCreationController.LinkResult result =
                    controller.handleClick(outputPortX(), portY(), canvasState, editor);
            assertThat(result.isRejected()).isTrue();
            assertThat(controller.isPending()).isFalse();
        }

        @Test
        void shouldRejectElementToElement() {
            // First click: element
            controller.handleClick(100, 200, canvasState, editor);

            // Second click: another element (no port)
            InfoLinkCreationController.LinkResult result =
                    controller.handleClick(500, 200, canvasState, editor);
            assertThat(result.isRejected()).isTrue();
            assertThat(controller.isPending()).isFalse();
        }

        @Test
        void shouldRejectDuplicateInputBinding() {
            // Create binding first time
            controller.handleClick(100, 200, canvasState, editor);
            controller.handleClick(inputPortX(), portY(), canvasState, editor);

            // Try to create same binding again
            controller.handleClick(100, 200, canvasState, editor);
            InfoLinkCreationController.LinkResult result =
                    controller.handleClick(inputPortX(), portY(), canvasState, editor);
            assertThat(result.isRejected()).isTrue();
            assertThat(result.rejectionReason()).contains("already exists");
        }

        @Test
        void shouldRejectDuplicateOutputBinding() {
            // Create binding first time
            controller.handleClick(outputPortX(), portY(), canvasState, editor);
            controller.handleClick(500, 200, canvasState, editor);

            // Try to create same binding again
            controller.handleClick(outputPortX(), portY(), canvasState, editor);
            InfoLinkCreationController.LinkResult result =
                    controller.handleClick(500, 200, canvasState, editor);
            assertThat(result.isRejected()).isTrue();
            assertThat(result.rejectionReason()).contains("already exists");
        }
    }

    @Nested
    @DisplayName("cancel")
    class Cancel {

        @Test
        void shouldResetPendingState() {
            controller.handleClick(100, 200, canvasState, editor);
            assertThat(controller.isPending()).isTrue();

            controller.cancel();

            assertThat(controller.isPending()).isFalse();
            InfoLinkCreationController.State state = controller.getState();
            assertThat(state.pending()).isFalse();
        }
    }

    @Nested
    @DisplayName("rubber band")
    class RubberBand {

        @Test
        void shouldUpdateRubberBandEndpoint() {
            controller.handleClick(100, 200, canvasState, editor);
            controller.updateRubberBand(350, 250);

            InfoLinkCreationController.State state = controller.getState();
            assertThat(state.rubberBandEndX()).isEqualTo(350);
            assertThat(state.rubberBandEndY()).isEqualTo(250);
        }

        @Test
        void shouldReturnIdleStateWhenNotPending() {
            InfoLinkCreationController.State state = controller.getState();
            assertThat(state.pending()).isFalse();
        }

        @Test
        void shouldIncludeSourceInfoInState() {
            controller.handleClick(100, 200, canvasState, editor);
            InfoLinkCreationController.State state = controller.getState();
            assertThat(state.pending()).isTrue();
            assertThat(state.sourceName()).isEqualTo("Variable 1");
            assertThat(state.sourcePort()).isNull();
        }

        @Test
        void shouldIncludeSourcePortInState() {
            controller.handleClick(outputPortX(), portY(), canvasState, editor);
            InfoLinkCreationController.State state = controller.getState();
            assertThat(state.pending()).isTrue();
            assertThat(state.sourceName()).isNull();
            assertThat(state.sourcePort()).isNotNull();
            assertThat(state.sourcePort().portName()).isEqualTo("outPort");
        }
    }
}
