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
    private CausalLinkGeometry.LoopContext loopCtx;

    @BeforeEach
    void setUp() {
        state = new CanvasState();
        state.addElement("A", ElementType.CLD_VARIABLE, 100, 200);
        state.addElement("B", ElementType.CLD_VARIABLE, 400, 200);
        controller = new CausalLinkDragController();
        links = List.of(new CausalLinkDef("A", "B"));
        connection = new ConnectionId("A", "B");
        loopCtx = CausalLinkGeometry.loopContext(state);
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
            double[] pos = CausalLinkDragController.handlePosition(
                    connection, state, links, loopCtx);

            assertThat(pos).isNotNull();
            assertThat(pos[0]).isCloseTo(250, within(50.0));
        }

        @Test
        void shouldReturnNullForSelfLoop() {
            ConnectionId selfLoop = new ConnectionId("A", "A");
            List<CausalLinkDef> selfLinks = List.of(new CausalLinkDef("A", "A"));

            double[] pos = CausalLinkDragController.handlePosition(
                    selfLoop, state, selfLinks, loopCtx);

            assertThat(pos).isNull();
        }

        @Test
        void shouldReturnNullForMissingElement() {
            ConnectionId missing = new ConnectionId("A", "Z");

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
            double[] pos = CausalLinkDragController.handlePosition(
                    connection, state, links, loopCtx);

            boolean hit = CausalLinkDragController.hitTestHandle(
                    pos[0], pos[1], connection, state, links, loopCtx);

            assertThat(hit).isTrue();
        }

        @Test
        void shouldMissHandleFarAway() {
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
            controller.start(connection, state, links, loopCtx);

            assertThat(controller.isActive()).isTrue();
            assertThat(controller.getFromName()).isEqualTo("A");
            assertThat(controller.getToName()).isEqualTo("B");
        }

        @Test
        void shouldComputePositiveStrengthDraggingAwayFromChord() {
            controller.start(connection, state, links, loopCtx);

            // Find the handle position to know which direction is "away"
            double[] pos = CausalLinkDragController.handlePosition(
                    connection, state, links, loopCtx);
            // Drag further away from chord midpoint (250,200) in the same
            // direction as the handle offset
            double dragX = pos[0] + (pos[0] - 250) * 2;
            double dragY = pos[1] + (pos[1] - 200) * 2;
            double strength = controller.drag(dragX, dragY);

            assertThat(strength).isGreaterThan(0);
        }

        @Test
        void shouldAllowNegativeStrengthForReverseCurvature() {
            controller.start(connection, state, links, loopCtx);

            // Drag in the opposite direction from the handle
            double[] pos = CausalLinkDragController.handlePosition(
                    connection, state, links, loopCtx);
            double dragX = 250 - (pos[0] - 250) * 2;
            double dragY = 200 - (pos[1] - 200) * 2;
            double strength = controller.drag(dragX, dragY);

            assertThat(strength).isLessThan(0);
        }

        @Test
        void shouldReturnZeroAtChordMidpoint() {
            controller.start(connection, state, links, loopCtx);

            // Drag to chord midpoint
            double strength = controller.drag(250, 200);

            assertThat(strength).isCloseTo(0, within(0.01));
        }

        @Test
        void shouldBeInactiveAfterCancel() {
            controller.start(connection, state, links, loopCtx);
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

            assertThat(cpCustom.y()).isNotCloseTo(cpAuto.y(), within(1.0));
        }

        @Test
        void shouldReverseCurveWithNegativeStrength() {
            CausalLinkGeometry.ControlPoint cpAuto = CausalLinkGeometry.controlPoint(
                    100, 200, 400, 200, "A", "B", links);

            List<CausalLinkDef> linksNeg = List.of(
                    new CausalLinkDef("A", "B", CausalLinkDef.Polarity.UNKNOWN,
                            null, -40.0));
            CausalLinkGeometry.ControlPoint cpNeg = CausalLinkGeometry.controlPoint(
                    100, 200, 400, 200, "A", "B", linksNeg);

            // Negative strength should curve in the opposite direction
            // The Y offset from midpoint (200) should be on opposite sides
            double autoOffset = cpAuto.y() - 200;
            double negOffset = cpNeg.y() - 200;
            assertThat(autoOffset * negOffset).isLessThan(0);
        }

        @Test
        void shouldUseAutoWhenStrengthIsNaN() {
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
