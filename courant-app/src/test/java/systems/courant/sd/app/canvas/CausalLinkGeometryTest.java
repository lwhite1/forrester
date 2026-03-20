package systems.courant.sd.app.canvas;

import systems.courant.sd.model.def.CausalLinkDef;
import systems.courant.sd.model.def.CldVariableDef;
import systems.courant.sd.model.def.ElementPlacement;
import systems.courant.sd.model.def.ElementType;
import systems.courant.sd.model.def.ViewDef;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

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
