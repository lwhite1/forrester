package systems.courant.forrester.app.canvas;

import systems.courant.forrester.model.def.ConnectorRoute;
import systems.courant.forrester.model.def.ElementPlacement;
import systems.courant.forrester.model.def.ElementType;
import systems.courant.forrester.model.def.ViewDef;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

@DisplayName("Connection hit testing")
class ConnectionHitTestTest {

    @Nested
    @DisplayName("pointToSegmentDistance")
    class PointToSegmentDistanceTest {

        @Test
        void shouldReturnZeroForPointOnSegment() {
            // Midpoint of segment from (0,0) to (10,0)
            double dist = HitTester.pointToSegmentDistance(5, 0, 0, 0, 10, 0);
            assertThat(dist).isCloseTo(0.0, within(0.001));
        }

        @Test
        void shouldReturnPerpendicularDistanceToMidpoint() {
            // Point 5 units above midpoint of horizontal segment
            double dist = HitTester.pointToSegmentDistance(5, 5, 0, 0, 10, 0);
            assertThat(dist).isCloseTo(5.0, within(0.001));
        }

        @Test
        void shouldReturnDistanceToNearestEndpointBeyondSegment() {
            // Point beyond the end of segment (0,0)-(10,0), at (15,0)
            double dist = HitTester.pointToSegmentDistance(15, 0, 0, 0, 10, 0);
            assertThat(dist).isCloseTo(5.0, within(0.001));
        }

        @Test
        void shouldReturnDistanceToStartEndpointBefore() {
            // Point before the start of segment (0,0)-(10,0), at (-3,0)
            double dist = HitTester.pointToSegmentDistance(-3, 0, 0, 0, 10, 0);
            assertThat(dist).isCloseTo(3.0, within(0.001));
        }

        @Test
        void shouldHandleDegenerateSegment() {
            // Zero-length segment at (5,5), point at (5,8)
            double dist = HitTester.pointToSegmentDistance(5, 8, 5, 5, 5, 5);
            assertThat(dist).isCloseTo(3.0, within(0.001));
        }

        @Test
        void shouldHandleDiagonalSegment() {
            // Segment from (0,0) to (10,10), point at (0,10) — distance is 10/sqrt(2)
            double dist = HitTester.pointToSegmentDistance(0, 10, 0, 0, 10, 10);
            assertThat(dist).isCloseTo(Math.sqrt(50), within(0.001));
        }

        @Test
        void shouldHandlePointOnEndpoint() {
            double dist = HitTester.pointToSegmentDistance(10, 0, 0, 0, 10, 0);
            assertThat(dist).isCloseTo(0.0, within(0.001));
        }
    }

    @Nested
    @DisplayName("hitTestInfoLink")
    class HitTestInfoLinkTest {

        private CanvasState state;

        @BeforeEach
        void setUp() {
            state = new CanvasState();
            // Two elements: A at (100,100) and B at (300,100) both AUX (100x55)
            ViewDef view = new ViewDef("test", List.of(
                    new ElementPlacement("A", ElementType.AUX, 100, 100),
                    new ElementPlacement("B", ElementType.AUX, 300, 100)
            ), List.of(), List.of());
            state.loadFrom(view);
        }

        @Test
        void shouldHitConnectionNearMidpoint() {
            List<ConnectorRoute> connectors = List.of(
                    new ConnectorRoute("A", "B", List.of()));

            // Midpoint of clipped connection is approximately (200, 100)
            // Point 3 units above the line — within tolerance of 6
            ConnectionId result = HitTester.hitTestInfoLink(state, connectors, 200, 103);
            assertThat(result).isNotNull();
            assertThat(result.from()).isEqualTo("A");
            assertThat(result.to()).isEqualTo("B");
        }

        @Test
        void shouldMissConnectionFarFromLine() {
            List<ConnectorRoute> connectors = List.of(
                    new ConnectorRoute("A", "B", List.of()));

            // Point 20 units above the line — outside tolerance
            ConnectionId result = HitTester.hitTestInfoLink(state, connectors, 200, 120);
            assertThat(result).isNull();
        }

        @Test
        void shouldReturnNullForEmptyConnectors() {
            ConnectionId result = HitTester.hitTestInfoLink(state, List.of(), 200, 100);
            assertThat(result).isNull();
        }

        @Test
        void shouldSkipConnectorsWithMissingEndpoints() {
            List<ConnectorRoute> connectors = List.of(
                    new ConnectorRoute("A", "missing", List.of()));

            ConnectionId result = HitTester.hitTestInfoLink(state, connectors, 200, 100);
            assertThat(result).isNull();
        }

        @Test
        void shouldReturnLastDrawnConnectionFirst() {
            // Two connections overlapping — second connector should be hit first
            ViewDef view = new ViewDef("test", List.of(
                    new ElementPlacement("A", ElementType.AUX, 100, 100),
                    new ElementPlacement("B", ElementType.AUX, 300, 100),
                    new ElementPlacement("C", ElementType.AUX, 300, 100)
            ), List.of(), List.of());
            state.loadFrom(view);

            List<ConnectorRoute> connectors = List.of(
                    new ConnectorRoute("A", "B", List.of()),
                    new ConnectorRoute("A", "C", List.of()));

            ConnectionId result = HitTester.hitTestInfoLink(state, connectors, 200, 100);
            assertThat(result).isNotNull();
            assertThat(result.to()).isEqualTo("C");
        }
    }
}
