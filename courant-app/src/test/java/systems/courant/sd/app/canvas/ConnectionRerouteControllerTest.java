package systems.courant.sd.app.canvas;

import systems.courant.sd.app.canvas.ConnectionRerouteController.RerouteEnd;
import systems.courant.sd.app.canvas.ConnectionRerouteController.RerouteHit;
import systems.courant.sd.model.def.ConnectorRoute;
import systems.courant.sd.model.def.ElementType;
import systems.courant.sd.model.def.ModelDefinition;
import systems.courant.sd.model.def.ModelDefinitionBuilder;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("ConnectionRerouteController")
class ConnectionRerouteControllerTest {

    private ConnectionRerouteController controller;
    private CanvasState canvasState;
    private ModelEditor editor;
    private List<ConnectorRoute> connectors;

    @BeforeEach
    void setUp() {
        controller = new ConnectionRerouteController();
        canvasState = new CanvasState();
        editor = new ModelEditor();

        // Set up model: Source constant, Target aux with equation referencing Source,
        // and NewSource constant for reroute destination.
        ModelDefinition def = new ModelDefinitionBuilder()
                .name("Test")
                .constant("Source", 1, "units")
                .variable("Target", "Source", "units")
                .constant("NewSource", 2, "units")
                .build();
        editor.loadFrom(def);

        // Place elements far apart so endpoint hit testing is unambiguous.
        // AUX default size is 100x55, CONSTANT is 100x55.
        canvasState.addElement("Source", ElementType.AUX, 100, 100);
        canvasState.addElement("Target", ElementType.AUX, 500, 100);
        canvasState.addElement("NewSource", ElementType.AUX, 500, 300);

        connectors = List.of(new ConnectorRoute("Source", "Target", List.of()));
    }

    @Nested
    @DisplayName("hitTestEndpoint")
    class HitTestEndpoint {

        @Test
        void shouldReturnFromWhenClickNearSourceEndpoint() {
            ConnectionId connection = new ConnectionId("Source", "Target");

            // The from-end clipped endpoint is near x=150 (right edge of Source)
            RerouteHit hit = ConnectionRerouteController.hitTestEndpoint(
                    connection, canvasState, connectors, 152, 100);

            assertThat(hit).isNotNull();
            assertThat(hit.end()).isEqualTo(RerouteEnd.FROM);
            assertThat(hit.from()).isEqualTo("Source");
            assertThat(hit.to()).isEqualTo("Target");
        }

        @Test
        void shouldReturnToWhenClickNearTargetEndpoint() {
            ConnectionId connection = new ConnectionId("Source", "Target");

            // The to-end clipped endpoint is near x=450 (left edge of Target)
            RerouteHit hit = ConnectionRerouteController.hitTestEndpoint(
                    connection, canvasState, connectors, 448, 100);

            assertThat(hit).isNotNull();
            assertThat(hit.end()).isEqualTo(RerouteEnd.TO);
            assertThat(hit.from()).isEqualTo("Source");
            assertThat(hit.to()).isEqualTo("Target");
        }

        @Test
        void shouldReturnNullWhenClickOutsideTolerance() {
            ConnectionId connection = new ConnectionId("Source", "Target");

            // Click at midpoint (300,100) — far from both endpoints
            RerouteHit hit = ConnectionRerouteController.hitTestEndpoint(
                    connection, canvasState, connectors, 300, 100);

            assertThat(hit).isNull();
        }

        @Test
        void shouldReturnNullForNullConnection() {
            RerouteHit hit = ConnectionRerouteController.hitTestEndpoint(
                    null, canvasState, connectors, 150, 100);

            assertThat(hit).isNull();
        }

        @Test
        void shouldReturnNullWhenElementMissing() {
            ConnectionId connection = new ConnectionId("Source", "Ghost");

            RerouteHit hit = ConnectionRerouteController.hitTestEndpoint(
                    connection, canvasState, connectors, 150, 100);

            assertThat(hit).isNull();
        }
    }

    @Nested
    @DisplayName("prepare and drag")
    class PrepareAndDrag {

        @Test
        void shouldSetActiveStateOnPrepare() {
            RerouteHit hit = new RerouteHit("Source", "Target",
                    RerouteEnd.FROM, 500, 100);

            controller.prepare(hit);

            assertThat(controller.isActive()).isTrue();
            assertThat(controller.isDragStarted()).isFalse();
            assertThat(controller.getEnd()).isEqualTo(RerouteEnd.FROM);
            assertThat(controller.getAnchorX()).isEqualTo(500);
            assertThat(controller.getAnchorY()).isEqualTo(100);
        }

        @Test
        void shouldUpdateRubberBandOnDrag() {
            RerouteHit hit = new RerouteHit("Source", "Target",
                    RerouteEnd.FROM, 500, 100);
            controller.prepare(hit);

            controller.drag(250, 150);

            assertThat(controller.isDragStarted()).isTrue();
            assertThat(controller.getRubberBandX()).isEqualTo(250);
            assertThat(controller.getRubberBandY()).isEqualTo(150);
        }
    }

    @Nested
    @DisplayName("complete")
    class Complete {

        @Test
        void shouldRerouteSourceToValidTarget() {
            // Prepare rerouting the FROM end of Source→Target connection
            RerouteHit hit = new RerouteHit("Source", "Target",
                    RerouteEnd.FROM, 500, 100);
            controller.prepare(hit);
            controller.drag(500, 300);

            boolean[] undoSaved = {false};
            boolean result = controller.complete(500, 300, canvasState,
                    editor, () -> undoSaved[0] = true);

            assertThat(result).isTrue();
            assertThat(undoSaved[0]).isTrue();
            // Target's equation should now reference NewSource instead of Source
            assertThat(editor.getVariableByName("Target").orElseThrow().equation()).isEqualTo("NewSource");
            assertThat(controller.isActive()).isFalse();
        }

        @Test
        void shouldRejectRerouteToSameElement() {
            // Rerouting FROM end, but dropping on fromName itself (Source)
            RerouteHit hit = new RerouteHit("Source", "Target",
                    RerouteEnd.FROM, 500, 100);
            controller.prepare(hit);
            controller.drag(100, 100);

            boolean result = controller.complete(100, 100, canvasState,
                    editor, () -> {});

            assertThat(result).isFalse();
            assertThat(controller.isActive()).isFalse();
        }

        @Test
        void shouldReturnFalseWithoutDrag() {
            RerouteHit hit = new RerouteHit("Source", "Target",
                    RerouteEnd.FROM, 500, 100);
            controller.prepare(hit);
            // No drag — complete immediately

            boolean result = controller.complete(500, 300, canvasState,
                    editor, () -> {});

            assertThat(result).isFalse();
            assertThat(controller.isActive()).isFalse();
        }
    }

    @Nested
    @DisplayName("cancel")
    class Cancel {

        @Test
        void shouldResetAllState() {
            RerouteHit hit = new RerouteHit("Source", "Target",
                    RerouteEnd.FROM, 500, 100);
            controller.prepare(hit);
            controller.drag(250, 150);
            assertThat(controller.isActive()).isTrue();
            assertThat(controller.isDragStarted()).isTrue();

            controller.cancel();

            assertThat(controller.isActive()).isFalse();
            assertThat(controller.isDragStarted()).isFalse();
            assertThat(controller.getEnd()).isNull();
        }
    }
}
