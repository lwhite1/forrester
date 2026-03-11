package systems.courant.forrester.model.graph;

import systems.courant.forrester.model.def.ModelDefinition;
import systems.courant.forrester.model.def.ModelDefinitionBuilder;
import systems.courant.forrester.model.graph.CausalTraceAnalysis.TraceDirection;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("CausalTraceAnalysis")
class CausalTraceAnalysisTest {

    // C → A1 → A2 → F → S (F drains into S)
    private static ModelDefinition buildChainModel() {
        return new ModelDefinitionBuilder()
                .name("Chain")
                .constant("C", 10, "Thing")
                .aux("A1", "C * 2", "Thing")
                .aux("A2", "A1 + 1", "Thing")
                .stock("S", 0, "Thing")
                .flow("F", "A2", "Day", null, "S")
                .build();
    }

    @Nested
    @DisplayName("UpstreamTrace")
    class UpstreamTrace {

        @Test
        void shouldTraceUpstreamFromFlow() {
            CausalTraceAnalysis trace = CausalTraceAnalysis.trace("F", TraceDirection.UPSTREAM, buildChainModel());

            assertThat(trace.origin()).isEqualTo("F");
            assertThat(trace.direction()).isEqualTo(TraceDirection.UPSTREAM);
            assertThat(trace.isTraced("F")).isTrue();
            assertThat(trace.isTraced("A2")).isTrue();
            assertThat(trace.isTraced("A1")).isTrue();
            assertThat(trace.isTraced("C")).isTrue();
            // S is downstream of F, should not be in upstream trace
            assertThat(trace.isTraced("S")).isFalse();
        }

        @Test
        void shouldAssignCorrectDepths() {
            CausalTraceAnalysis trace = CausalTraceAnalysis.trace("F", TraceDirection.UPSTREAM, buildChainModel());

            assertThat(trace.depthOf("F")).isEqualTo(0);
            assertThat(trace.depthOf("A2")).isEqualTo(1);
            assertThat(trace.depthOf("A1")).isEqualTo(2);
            assertThat(trace.depthOf("C")).isEqualTo(3);
            assertThat(trace.maxDepth()).isEqualTo(3);
        }

        @Test
        void shouldReturnNegativeOneForUntracedElement() {
            CausalTraceAnalysis trace = CausalTraceAnalysis.trace("F", TraceDirection.UPSTREAM, buildChainModel());

            assertThat(trace.depthOf("S")).isEqualTo(-1);
            assertThat(trace.depthOf("nonexistent")).isEqualTo(-1);
        }
    }

    @Nested
    @DisplayName("DownstreamTrace")
    class DownstreamTrace {

        @Test
        void shouldTraceDownstreamFromConstant() {
            CausalTraceAnalysis trace = CausalTraceAnalysis.trace("C", TraceDirection.DOWNSTREAM, buildChainModel());

            assertThat(trace.isTraced("C")).isTrue();
            assertThat(trace.isTraced("A1")).isTrue();
            assertThat(trace.isTraced("A2")).isTrue();
            assertThat(trace.isTraced("F")).isTrue();
            assertThat(trace.depthOf("C")).isEqualTo(0);
        }
    }

    @Nested
    @DisplayName("TraceEdges")
    class TraceEdges {

        @Test
        void shouldIncludeEdgesBetweenTracedElements() {
            CausalTraceAnalysis trace = CausalTraceAnalysis.trace("F", TraceDirection.UPSTREAM, buildChainModel());

            // C → A1 edge should be in trace
            assertThat(trace.isTraceEdge("C", "A1")).isTrue();
            // A1 → A2 edge should be in trace
            assertThat(trace.isTraceEdge("A1", "A2")).isTrue();
            // A2 → F edge should be in trace
            assertThat(trace.isTraceEdge("A2", "F")).isTrue();
        }

        @Test
        void shouldNotIncludeEdgesOutsideTrace() {
            CausalTraceAnalysis trace = CausalTraceAnalysis.trace("A1", TraceDirection.UPSTREAM, buildChainModel());

            // A1 → A2 edge is downstream, should not be in upstream trace edges
            // (A2 is not in the trace at all)
            assertThat(trace.isTraceEdge("A1", "A2")).isFalse();
        }
    }

    @Nested
    @DisplayName("Opacity")
    class Opacity {

        @Test
        void shouldReturnFullOpacityForOrigin() {
            CausalTraceAnalysis trace = CausalTraceAnalysis.trace("F", TraceDirection.UPSTREAM, buildChainModel());

            assertThat(trace.opacityForDepth(0)).isEqualTo(1.0);
        }

        @Test
        void shouldFadeWithDepth() {
            CausalTraceAnalysis trace = CausalTraceAnalysis.trace("F", TraceDirection.UPSTREAM, buildChainModel());

            double depth1 = trace.opacityForDepth(1);
            double depth2 = trace.opacityForDepth(2);
            double depth3 = trace.opacityForDepth(3);

            assertThat(depth1).isLessThan(1.0);
            assertThat(depth2).isLessThan(depth1);
            assertThat(depth3).isLessThanOrEqualTo(depth2);
            assertThat(depth3).isGreaterThanOrEqualTo(0.25);
        }

        @Test
        void shouldReturnFullOpacityWhenMaxDepthIsZero() {
            // Trace from a leaf — only the origin, maxDepth = 0
            CausalTraceAnalysis trace = CausalTraceAnalysis.trace("C", TraceDirection.UPSTREAM, buildChainModel());

            assertThat(trace.maxDepth()).isEqualTo(0);
            assertThat(trace.opacityForDepth(0)).isEqualTo(1.0);
        }
    }

    @Test
    void shouldHandleCyclicModel() {
        ModelDefinition def = new ModelDefinitionBuilder()
                .name("Cycle")
                .stock("Pop", 100, "Person")
                .flow("Births", "Pop * 0.04", "Day", null, "Pop")
                .build();

        CausalTraceAnalysis trace = CausalTraceAnalysis.trace("Pop", TraceDirection.UPSTREAM, def);

        // Both elements should be traced (they form a cycle)
        assertThat(trace.isTraced("Pop")).isTrue();
        assertThat(trace.isTraced("Births")).isTrue();
    }
}
