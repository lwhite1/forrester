package systems.courant.sd.app.canvas.controllers;

import systems.courant.sd.app.canvas.CanvasState;
import systems.courant.sd.app.canvas.CausalLinkGeometry;
import systems.courant.sd.app.canvas.ConnectionId;
import systems.courant.sd.model.def.CausalLinkDef;
import systems.courant.sd.model.def.ElementType;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

@DisplayName("CausalLinkDragController")
class CausalLinkDragControllerTest {

    private CanvasState state;
    private CausalLinkDragController controller;
    private List<CausalLinkDef> links;
    private ConnectionId connection;

    @BeforeEach
    void setUp() {
        state = new CanvasState();
        state.addElement("A", ElementType.CLD_VARIABLE, 100, 200);
        state.addElement("B", ElementType.CLD_VARIABLE, 400, 200);
        controller = new CausalLinkDragController();
        links = List.of(new CausalLinkDef("A", "B"));
        connection = new ConnectionId("A", "B");
    }

    @Nested
    @DisplayName("initial state")
    class InitialState {

        @Test
        void shouldNotBeActive() {
            assertThat(controller.isActive()).isFalse();
        }
    }

    @Nested
    @DisplayName("handle position")
    class HandlePosition {

        @Test
        void shouldReturnMidpointOfCurve() {
            CausalLinkGeometry.LoopContext loopCtx = CausalLinkGeometry.loopContext(state);
            double[] pos = CausalLinkDragController.handlePosition(
                    connection, state, links, loopCtx);

            assertThat(pos).isNotNull();
            // Handle at t=0.5 on the Bézier curve should be near the midpoint
            // but offset perpendicular due to curvature
            assertThat(pos[0]).isCloseTo(250, within(50.0));
        }

        @Test
        void shouldReturnNullForSelfLoop() {
            ConnectionId selfLoop = new ConnectionId("A", "A");
            List<CausalLinkDef> selfLinks = List.of(new CausalLinkDef("A", "A"));
            CausalLinkGeometry.LoopContext loopCtx = CausalLinkGeometry.loopContext(state);

            double[] pos = CausalLinkDragController.handlePosition(
                    selfLoop, state, selfLinks, loopCtx);

            assertThat(pos).isNull();
        }

        @Test
        void shouldReturnNullForMissingElement() {
            ConnectionId missing = new ConnectionId("A", "Z");
            CausalLinkGeometry.LoopContext loopCtx = CausalLinkGeometry.loopContext(state);

            double[] pos = CausalLinkDragController.handlePosition(
                    missing, state, links, loopCtx);

            assertThat(pos).isNull();
        }
    }

    @Nested
    @DisplayName("hit test")
    class HitTest {

        @Test
        void shouldDetectHitOnHandle() {
            CausalLinkGeometry.LoopContext loopCtx = CausalLinkGeometry.loopContext(state);
            double[] pos = CausalLinkDragController.handlePosition(
                    connection, state, links, loopCtx);

            boolean hit = CausalLinkDragController.hitTestHandle(
                    pos[0], pos[1], connection, state, links, loopCtx);

            assertThat(hit).isTrue();
        }

        @Test
        void shouldMissHandleFarAway() {
            CausalLinkGeometry.LoopContext loopCtx = CausalLinkGeometry.loopContext(state);

            boolean hit = CausalLinkDragController.hitTestHandle(
                    0, 0, connection, state, links, loopCtx);

            assertThat(hit).isFalse();
        }
    }

    @Nested
    @DisplayName("drag interaction")
    class DragInteraction {

        @Test
        void shouldBeActiveAfterStart() {
            controller.start(connection, state, links);

            assertThat(controller.isActive()).isTrue();
            assertThat(controller.getFromName()).isEqualTo("A");
            assertThat(controller.getToName()).isEqualTo("B");
        }

        @Test
        void shouldComputePositiveStrengthForPerpendicularDrag() {
            controller.start(connection, state, links);

            // Chord is horizontal (A at 100,200, B at 400,200)
            // Canonical perpendicular for A<B points downward (+Y)
            // Direction=+1 (no reciprocal), so dragging below midpoint is positive
            double strength = controller.drag(250, 260);

            assertThat(strength).isGreaterThan(0);
        }

        @Test
        void shouldReturnZeroStrengthForReverseDrag() {
            controller.start(connection, state, links);

            // Drag above midpoint (opposite to perpendicular*direction)
            double strength = controller.drag(250, 140);

            assertThat(strength).isEqualTo(0.0);
        }

        @Test
        void shouldBeInactiveAfterCancel() {
            controller.start(connection, state, links);
            controller.cancel();

            assertThat(controller.isActive()).isFalse();
        }
    }

    @Nested
    @DisplayName("strength application")
    class StrengthApplication {

        @Test
        void shouldPreserveStrengthInControlPoint() {
            List<CausalLinkDef> linksWithStrength = List.of(
                    new CausalLinkDef("A", "B", CausalLinkDef.Polarity.POSITIVE,
                            null, 80.0));

            CausalLinkGeometry.ControlPoint cpCustom = CausalLinkGeometry.controlPoint(
                    100, 200, 400, 200, "A", "B", linksWithStrength);
            CausalLinkGeometry.ControlPoint cpAuto = CausalLinkGeometry.controlPoint(
                    100, 200, 400, 200, "A", "B", links);

            // Custom strength=80 should produce a different control point than auto
            assertThat(cpCustom.y()).isNotCloseTo(cpAuto.y(), within(1.0));
        }

        @Test
        void shouldUseAutoWhenStrengthIsNaN() {
            // Default strength is NaN
            CausalLinkGeometry.ControlPoint cp1 = CausalLinkGeometry.controlPoint(
                    100, 200, 400, 200, "A", "B", links);

            List<CausalLinkDef> linksExplicitNaN = List.of(
                    new CausalLinkDef("A", "B", CausalLinkDef.Polarity.UNKNOWN,
                            null, Double.NaN));
            CausalLinkGeometry.ControlPoint cp2 = CausalLinkGeometry.controlPoint(
                    100, 200, 400, 200, "A", "B", linksExplicitNaN);

            assertThat(cp1.x()).isCloseTo(cp2.x(), within(0.01));
            assertThat(cp1.y()).isCloseTo(cp2.y(), within(0.01));
        }
    }
}
