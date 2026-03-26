package systems.courant.sd.app.canvas;

import systems.courant.sd.model.def.CausalLinkDef;
import systems.courant.sd.model.def.CldVariableDef;
import systems.courant.sd.model.def.ElementPlacement;
import systems.courant.sd.model.def.ElementType;
import systems.courant.sd.model.def.ViewDef;
import systems.courant.sd.model.graph.CldLoopInfo;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class CausalLinkGeometryTest {

    @Nested
    class CentroidAwareControlPoint {

        @Test
        void shouldFallBackWhenCentroidIsNaN() {
            List<CausalLinkDef> links = List.of(new CausalLinkDef("A", "B"));

            CausalLinkGeometry.ControlPoint cp1 = CausalLinkGeometry.controlPoint(
                    0, 0, 100, 0, "A", "B", links);
            CausalLinkGeometry.ControlPoint cp2 = CausalLinkGeometry.controlPoint(
                    0, 0, 100, 0, "A", "B", links, Double.NaN, Double.NaN);

            assertThat(cp2.x()).isEqualTo(cp1.x());
            assertThat(cp2.y()).isEqualTo(cp1.y());
        }

        @Test
        void shouldCurveAwayFromCentroid() {
            List<CausalLinkDef> links = List.of(new CausalLinkDef("A", "B"));

            // Centroid below the link — curve should go up (negative Y)
            CausalLinkGeometry.ControlPoint cp = CausalLinkGeometry.controlPoint(
                    0, 0, 200, 0, "A", "B", links, 100, 200);

            // The control point should be above the chord (negative Y from midpoint)
            assertThat(cp.y()).isLessThan(0);
        }

        @Test
        void shouldFallBackToPerpendicularWhenMidpointEqualscentroid() {
            List<CausalLinkDef> links = List.of(new CausalLinkDef("A", "B"));

            // Centroid is exactly at the midpoint
            CausalLinkGeometry.ControlPoint cp = CausalLinkGeometry.controlPoint(
                    0, 0, 200, 0, "A", "B", links, 100, 0);

            // Should still produce a valid control point (not at midpoint)
            double dx = cp.x() - 100;
            double dy = cp.y() - 0;
            assertThat(Math.sqrt(dx * dx + dy * dy)).isGreaterThan(1);
        }

        @Test
        void shouldHandleReciprocalLinks() {
            List<CausalLinkDef> links = List.of(
                    new CausalLinkDef("A", "B"),
                    new CausalLinkDef("B", "A"));

            CausalLinkGeometry.ControlPoint cpAB = CausalLinkGeometry.controlPoint(
                    0, 0, 200, 0, "A", "B", links, 100, 100);
            CausalLinkGeometry.ControlPoint cpBA = CausalLinkGeometry.controlPoint(
                    200, 0, 0, 0, "B", "A", links, 100, 100);

            // A→B and B→A should curve in different directions
            // They should not have the same control point
            assertThat(cpAB.y()).isNotCloseTo(cpBA.y(), org.assertj.core.data.Offset.offset(1.0));
        }
    }

    @Nested
    class CentroidAwareSelfLoop {

        @Test
        void shouldFallBackWhenCentroidIsNaN() {
            double[] lp1 = CausalLinkGeometry.selfLoopPoints(100, 100, 50, 25);
            double[] lp2 = CausalLinkGeometry.selfLoopPoints(100, 100, 50, 25,
                    Double.NaN, Double.NaN);

            for (int i = 0; i < 8; i++) {
                assertThat(lp2[i]).isEqualTo(lp1[i]);
            }
        }

        @Test
        void shouldLoopOutwardFromCentroid() {
            // Centroid at (100, 200), node at (100, 100) — centroid is below
            // Loop should go upward (away from centroid)
            double[] lp = CausalLinkGeometry.selfLoopPoints(100, 100, 50, 25,
                    100, 200);

            // Control points should be above the node (lower Y values)
            double cp1Y = lp[3]; // cp1Y
            double cp2Y = lp[5]; // cp2Y
            assertThat(cp1Y).isLessThan(100);
            assertThat(cp2Y).isLessThan(100);
        }
    }

    @Nested
    class LoopAwareControlPoint {

        @Test
        void shouldUseLargerBulgeForSameLoopEdge() {
            // A → B → A (same loop), k=0.45
            List<CausalLinkDef> links = List.of(
                    new CausalLinkDef("A", "B"),
                    new CausalLinkDef("B", "A"));
            CldLoopInfo loopInfo = CldLoopInfo.compute(
                    List.of(new CldVariableDef("A"), new CldVariableDef("B")), links);

            Map<Integer, double[]> loopCentroids = Map.of(0, new double[]{100, 0});
            CausalLinkGeometry.LoopContext loopCtx = new CausalLinkGeometry.LoopContext(
                    loopInfo, loopCentroids, 100, 0);

            CausalLinkGeometry.ControlPoint cpLoop = CausalLinkGeometry.controlPoint(
                    0, 0, 200, 0, "A", "B", links, loopCtx);

            // Compare with old centroid-aware method (k=0.35)
            CausalLinkGeometry.ControlPoint cpOld = CausalLinkGeometry.controlPoint(
                    0, 0, 200, 0, "A", "B", links, 100, 0);

            // Same-loop curve (k=0.45) should have larger offset than default (k=0.35)
            double loopOffset = Math.abs(cpLoop.y());
            double oldOffset = Math.abs(cpOld.y());
            assertThat(loopOffset).isGreaterThan(oldOffset);
        }

        @Test
        void shouldUseSmallerBulgeForCrossLoopEdge() {
            // Loop1: A → B → A, Loop2: C → D → C, cross-edge: A → C
            List<CausalLinkDef> links = List.of(
                    new CausalLinkDef("A", "B"),
                    new CausalLinkDef("B", "A"),
                    new CausalLinkDef("C", "D"),
                    new CausalLinkDef("D", "C"),
                    new CausalLinkDef("A", "C"));
            List<CldVariableDef> vars = List.of(
                    new CldVariableDef("A"),
                    new CldVariableDef("B"),
                    new CldVariableDef("C"),
                    new CldVariableDef("D"));
            CldLoopInfo loopInfo = CldLoopInfo.compute(vars, links);
            assertThat(loopInfo.inSameLoop("A", "C")).isFalse();

            CausalLinkGeometry.LoopContext loopCtx = new CausalLinkGeometry.LoopContext(
                    loopInfo, Map.of(), 100, 100);

            // Cross-loop edges should use k=0.25 (gentle curve)
            assertThat(loopCtx.bulgeFactorFor("A", "C")).isEqualTo(0.25);
        }

        @Test
        void shouldUseSameLoopBulgeWhenAllInOneSCC() {
            // A→B→A and B→C→B merge into one SCC {A,B,C}
            // All intra-SCC edges get k=0.45
            List<CausalLinkDef> links = List.of(
                    new CausalLinkDef("A", "B"),
                    new CausalLinkDef("B", "A"),
                    new CausalLinkDef("B", "C"),
                    new CausalLinkDef("C", "B"));
            List<CldVariableDef> vars = List.of(
                    new CldVariableDef("A"),
                    new CldVariableDef("B"),
                    new CldVariableDef("C"));
            CldLoopInfo loopInfo = CldLoopInfo.compute(vars, links);

            Map<Integer, double[]> loopCentroids = Map.of(0, new double[]{100, 100});
            CausalLinkGeometry.LoopContext loopCtx = new CausalLinkGeometry.LoopContext(
                    loopInfo, loopCentroids, 100, 100);

            // All in same SCC → k=0.45
            assertThat(loopCtx.bulgeFactorFor("A", "B")).isEqualTo(0.45);
            assertThat(loopCtx.bulgeFactorFor("B", "C")).isEqualTo(0.45);
        }

        @Test
        void shouldFallBackWhenLoopContextIsNull() {
            List<CausalLinkDef> links = List.of(new CausalLinkDef("A", "B"));

            CausalLinkGeometry.ControlPoint cp1 = CausalLinkGeometry.controlPoint(
                    0, 0, 100, 0, "A", "B", links);
            CausalLinkGeometry.ControlPoint cp2 = CausalLinkGeometry.controlPoint(
                    0, 0, 100, 0, "A", "B", links, (CausalLinkGeometry.LoopContext) null);

            assertThat(cp2.x()).isEqualTo(cp1.x());
            assertThat(cp2.y()).isEqualTo(cp1.y());
        }
    }

    @Nested
    class LoopContextComputation {

        @Test
        void shouldComputeLoopContextFromCanvasState() {
            CanvasState state = new CanvasState();
            state.loadFrom(new ViewDef("test", List.of(
                    new ElementPlacement("A", ElementType.CLD_VARIABLE, 0, 0),
                    new ElementPlacement("B", ElementType.CLD_VARIABLE, 200, 0),
                    new ElementPlacement("C", ElementType.CLD_VARIABLE, 100, 200)
            ), List.of(), List.of()));
            state.setCldLoopInfo(CldLoopInfo.compute(
                    List.of(new CldVariableDef("A"), new CldVariableDef("B"),
                            new CldVariableDef("C")),
                    List.of(new CausalLinkDef("A", "B"),
                            new CausalLinkDef("B", "C"),
                            new CausalLinkDef("C", "A"))));

            CausalLinkGeometry.LoopContext ctx = CausalLinkGeometry.loopContext(state);

            assertThat(ctx.globalCentroidX()).isCloseTo(100, org.assertj.core.data.Offset.offset(1.0));
            assertThat(ctx.loopCentroids()).isNotEmpty();
        }
    }

    @Nested
    class LinkStability {

        @Test
        void shouldNotChangeLinksWhenUnconnectedVariableAdded() {
            // Set up A→B link with two variables
            List<CausalLinkDef> links = List.of(new CausalLinkDef("A", "B"));

            CanvasState state1 = new CanvasState();
            state1.loadFrom(new ViewDef("test", List.of(
                    new ElementPlacement("A", ElementType.CLD_VARIABLE, 0, 0),
                    new ElementPlacement("B", ElementType.CLD_VARIABLE, 200, 0)
            ), List.of(), List.of()));

            CausalLinkGeometry.LoopContext ctx1 = CausalLinkGeometry.loopContext(state1);
            CausalLinkGeometry.ControlPoint cp1 = CausalLinkGeometry.controlPoint(
                    0, 0, 200, 0, "A", "B", links, ctx1);

            // Add an unconnected variable C near the link
            CanvasState state2 = new CanvasState();
            state2.loadFrom(new ViewDef("test", List.of(
                    new ElementPlacement("A", ElementType.CLD_VARIABLE, 0, 0),
                    new ElementPlacement("B", ElementType.CLD_VARIABLE, 200, 0),
                    new ElementPlacement("C", ElementType.CLD_VARIABLE, 100, 50)
            ), List.of(), List.of()));

            CausalLinkGeometry.LoopContext ctx2 = CausalLinkGeometry.loopContext(state2);
            CausalLinkGeometry.ControlPoint cp2 = CausalLinkGeometry.controlPoint(
                    0, 0, 200, 0, "A", "B", links, ctx2);

            // Link position must not change
            assertThat(cp2.x()).isEqualTo(cp1.x());
            assertThat(cp2.y()).isEqualTo(cp1.y());
        }

        @Test
        void shouldNotChangeLinksWhenDistantVariableMoves() {
            // A→B link; variable C exists but is not connected to A or B
            List<CausalLinkDef> links = List.of(new CausalLinkDef("A", "B"));

            CanvasState state1 = new CanvasState();
            state1.loadFrom(new ViewDef("test", List.of(
                    new ElementPlacement("A", ElementType.CLD_VARIABLE, 0, 0),
                    new ElementPlacement("B", ElementType.CLD_VARIABLE, 200, 0),
                    new ElementPlacement("C", ElementType.CLD_VARIABLE, 300, 300)
            ), List.of(), List.of()));

            CausalLinkGeometry.LoopContext ctx1 = CausalLinkGeometry.loopContext(state1);
            CausalLinkGeometry.ControlPoint cp1 = CausalLinkGeometry.controlPoint(
                    0, 0, 200, 0, "A", "B", links, ctx1);

            // Move C to a very different position
            CanvasState state2 = new CanvasState();
            state2.loadFrom(new ViewDef("test", List.of(
                    new ElementPlacement("A", ElementType.CLD_VARIABLE, 0, 0),
                    new ElementPlacement("B", ElementType.CLD_VARIABLE, 200, 0),
                    new ElementPlacement("C", ElementType.CLD_VARIABLE, -500, -500)
            ), List.of(), List.of()));

            CausalLinkGeometry.LoopContext ctx2 = CausalLinkGeometry.loopContext(state2);
            CausalLinkGeometry.ControlPoint cp2 = CausalLinkGeometry.controlPoint(
                    0, 0, 200, 0, "A", "B", links, ctx2);

            // Link position must not change — C is not an endpoint
            assertThat(cp2.x()).isEqualTo(cp1.x());
            assertThat(cp2.y()).isEqualTo(cp1.y());
        }

        @Test
        void shouldPreserveUserStrengthOverride() {
            // Link with user-defined strength should be completely stable
            List<CausalLinkDef> links = List.of(new CausalLinkDef("A", "B").withStrength(60));

            CausalLinkGeometry.LoopContext ctx1 = new CausalLinkGeometry.LoopContext(
                    null, Map.of(), 100, 100);
            CausalLinkGeometry.ControlPoint cp1 = CausalLinkGeometry.controlPoint(
                    0, 0, 200, 0, "A", "B", links, ctx1);

            // Different centroid — should not matter
            CausalLinkGeometry.LoopContext ctx2 = new CausalLinkGeometry.LoopContext(
                    null, Map.of(), 500, -300);
            CausalLinkGeometry.ControlPoint cp2 = CausalLinkGeometry.controlPoint(
                    0, 0, 200, 0, "A", "B", links, ctx2);

            assertThat(cp2.x()).isEqualTo(cp1.x());
            assertThat(cp2.y()).isEqualTo(cp1.y());
        }
    }

    @Nested
    class BiasControlPoint {

        @Test
        void shouldShiftControlPointAlongChordWithBias() {
            // Horizontal chord: A at (0,0), B at (200,0)
            List<CausalLinkDef> linksNoBias = List.of(new CausalLinkDef("A", "B"));
            List<CausalLinkDef> linksWithBias = List.of(
                    new CausalLinkDef("A", "B", CausalLinkDef.Polarity.UNKNOWN,
                            null, Double.NaN, 30.0));

            CausalLinkGeometry.ControlPoint cpAuto = CausalLinkGeometry.controlPoint(
                    0, 0, 200, 0, "A", "B", linksNoBias);
            CausalLinkGeometry.ControlPoint cpBias = CausalLinkGeometry.controlPoint(
                    0, 0, 200, 0, "A", "B", linksWithBias);

            // Bias=30 on a horizontal chord should shift X by 30
            assertThat(cpBias.x()).isCloseTo(cpAuto.x() + 30,
                    org.assertj.core.data.Offset.offset(0.01));
            // Y (perpendicular) should be unchanged
            assertThat(cpBias.y()).isCloseTo(cpAuto.y(),
                    org.assertj.core.data.Offset.offset(0.01));
        }

        @Test
        void shouldWorkWithLoopContextOverload() {
            CanvasState state = new CanvasState();
            state.loadFrom(new ViewDef("test", List.of(
                    new ElementPlacement("A", ElementType.CLD_VARIABLE, 0, 0),
                    new ElementPlacement("B", ElementType.CLD_VARIABLE, 200, 0)
            ), List.of(), List.of()));

            List<CausalLinkDef> linksNoBias = List.of(new CausalLinkDef("A", "B"));
            List<CausalLinkDef> linksWithBias = List.of(
                    new CausalLinkDef("A", "B", CausalLinkDef.Polarity.UNKNOWN,
                            null, Double.NaN, 40.0));

            CausalLinkGeometry.LoopContext ctx = CausalLinkGeometry.loopContext(state);

            CausalLinkGeometry.ControlPoint cpAuto = CausalLinkGeometry.controlPoint(
                    0, 0, 200, 0, "A", "B", linksNoBias, ctx);
            CausalLinkGeometry.ControlPoint cpBias = CausalLinkGeometry.controlPoint(
                    0, 0, 200, 0, "A", "B", linksWithBias, ctx);

            assertThat(cpBias.x()).isCloseTo(cpAuto.x() + 40,
                    org.assertj.core.data.Offset.offset(0.01));
        }

        @Test
        void shouldDefaultToZeroBias() {
            // Links without explicit bias should have bias=0 (no shift)
            List<CausalLinkDef> links = List.of(new CausalLinkDef("A", "B"));

            CausalLinkGeometry.ControlPoint cp = CausalLinkGeometry.controlPoint(
                    0, 0, 200, 0, "A", "B", links);

            // Midpoint X is 100 — control point X should be at 100 (no bias shift)
            assertThat(cp.x()).isCloseTo(100,
                    org.assertj.core.data.Offset.offset(0.01));
        }
    }

    @Nested
    class GraphCentroidComputation {

        @Test
        void shouldReturnNullForEmptyList() {
            CanvasState state = new CanvasState();
            double[] result = CausalLinkGeometry.graphCentroid(state, List.of());
            assertThat(result).isNull();
        }

        @Test
        void shouldReturnNullForNullList() {
            CanvasState state = new CanvasState();
            double[] result = CausalLinkGeometry.graphCentroid(state, null);
            assertThat(result).isNull();
        }

        @Test
        void shouldComputeMeanPosition() {
            CanvasState state = new CanvasState();
            state.loadFrom(new ViewDef("test", List.of(
                    new ElementPlacement("A", ElementType.CLD_VARIABLE, 100, 200),
                    new ElementPlacement("B", ElementType.CLD_VARIABLE, 300, 400)
            ), List.of(), List.of()));

            List<CldVariableDef> vars = List.of(
                    new CldVariableDef("A"),
                    new CldVariableDef("B"));

            double[] centroid = CausalLinkGeometry.graphCentroid(state, vars);

            assertThat(centroid).isNotNull();
            assertThat(centroid[0]).isEqualTo(200); // mean X
            assertThat(centroid[1]).isEqualTo(300); // mean Y
        }

        @Test
        void shouldSkipNodesNotOnCanvas() {
            CanvasState state = new CanvasState();
            state.loadFrom(new ViewDef("test", List.of(
                    new ElementPlacement("A", ElementType.CLD_VARIABLE, 100, 200)
            ), List.of(), List.of()));

            List<CldVariableDef> vars = List.of(
                    new CldVariableDef("A"),
                    new CldVariableDef("B")); // B not on canvas

            double[] centroid = CausalLinkGeometry.graphCentroid(state, vars);

            assertThat(centroid).isNotNull();
            assertThat(centroid[0]).isEqualTo(100);
            assertThat(centroid[1]).isEqualTo(200);
        }
    }
}
