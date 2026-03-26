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
            CausalLinkDragController.DragResult result = controller.drag(dragX, dragY);

            assertThat(result.strength()).isGreaterThan(0);
        }

        @Test
        void shouldAllowNegativeStrengthForReverseCurvature() {
            controller.start(connection, state, links, loopCtx);

            // Drag in the opposite direction from the handle
            double[] pos = CausalLinkDragController.handlePosition(
                    connection, state, links, loopCtx);
            double dragX = 250 - (pos[0] - 250) * 2;
            double dragY = 200 - (pos[1] - 200) * 2;
            CausalLinkDragController.DragResult result = controller.drag(dragX, dragY);

            assertThat(result.strength()).isLessThan(0);
        }

        @Test
        void shouldReturnZeroBothAtChordMidpoint() {
            controller.start(connection, state, links, loopCtx);

            // Drag to chord midpoint — both strength and bias should be near zero
            CausalLinkDragController.DragResult result = controller.drag(250, 200);

            assertThat(result.strength()).isCloseTo(0, within(0.01));
            assertThat(result.bias()).isCloseTo(0, within(0.01));
        }

        @Test
        void shouldComputePositiveBiasDraggingTowardTarget() {
            controller.start(connection, state, links, loopCtx);

            // Drag to the right of midpoint (toward B at x=400)
            // Chord midpoint is (250, 200), chord goes left-to-right
            CausalLinkDragController.DragResult result = controller.drag(350, 200);

            assertThat(result.bias()).isGreaterThan(0);
        }

        @Test
        void shouldComputeNegativeBiasDraggingTowardSource() {
            controller.start(connection, state, links, loopCtx);

            // Drag to the left of midpoint (toward A at x=100)
            CausalLinkDragController.DragResult result = controller.drag(150, 200);

            assertThat(result.bias()).isLessThan(0);
        }

        @Test
        void shouldDecomposeDiagonalDragIntoBothComponents() {
            controller.start(connection, state, links, loopCtx);

            // Drag diagonally — should produce both non-zero strength and bias
            // For a horizontal chord, dragging up-right should give positive
            // bias (rightward) and non-zero strength (upward component)
            CausalLinkDragController.DragResult result = controller.drag(350, 100);

            assertThat(result.strength()).isNotCloseTo(0, within(0.01));
            assertThat(result.bias()).isNotCloseTo(0, within(0.01));
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

    @Nested
    @DisplayName("bias application")
    class BiasApplication {

        @Test
        void shouldShiftControlPointAlongChordWithPositiveBias() {
            // A at (100,200), B at (400,200) — horizontal chord, midpoint at (250,200)
            List<CausalLinkDef> linksWithBias = List.of(
                    new CausalLinkDef("A", "B", CausalLinkDef.Polarity.UNKNOWN,
                            null, Double.NaN, 50.0));

            CausalLinkGeometry.ControlPoint cpBias = CausalLinkGeometry.controlPoint(
                    100, 200, 400, 200, "A", "B", linksWithBias);
            CausalLinkGeometry.ControlPoint cpAuto = CausalLinkGeometry.controlPoint(
                    100, 200, 400, 200, "A", "B", links);

            // Positive bias should shift CP to the right (toward B)
            assertThat(cpBias.x()).isGreaterThan(cpAuto.x());
            // Y offset from perpendicular should be the same
            assertThat(cpBias.y()).isCloseTo(cpAuto.y(), within(0.01));
        }

        @Test
        void shouldShiftControlPointOppositeWithNegativeBias() {
            List<CausalLinkDef> linksWithBias = List.of(
                    new CausalLinkDef("A", "B", CausalLinkDef.Polarity.UNKNOWN,
                            null, Double.NaN, -50.0));

            CausalLinkGeometry.ControlPoint cpBias = CausalLinkGeometry.controlPoint(
                    100, 200, 400, 200, "A", "B", linksWithBias);
            CausalLinkGeometry.ControlPoint cpAuto = CausalLinkGeometry.controlPoint(
                    100, 200, 400, 200, "A", "B", links);

            // Negative bias should shift CP to the left (toward A)
            assertThat(cpBias.x()).isLessThan(cpAuto.x());
        }

        @Test
        void shouldCombineStrengthAndBias() {
            List<CausalLinkDef> linksWithBoth = List.of(
                    new CausalLinkDef("A", "B", CausalLinkDef.Polarity.UNKNOWN,
                            null, 60.0, 40.0));

            CausalLinkGeometry.ControlPoint cp = CausalLinkGeometry.controlPoint(
                    100, 200, 400, 200, "A", "B", linksWithBoth);

            // For a horizontal chord: bias shifts X, strength shifts Y
            // Midpoint is (250, 200), bias=40 shifts right, strength=60 shifts perpendicular
            assertThat(cp.x()).isCloseTo(250 + 40, within(1.0));
        }
    }
}
