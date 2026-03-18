package systems.courant.sd.app.canvas.controllers;

import systems.courant.sd.model.def.ModelDefinition;
import systems.courant.sd.model.def.ModelDefinitionBuilder;
import systems.courant.sd.model.graph.CausalTraceAnalysis;
import systems.courant.sd.model.graph.CausalTraceAnalysis.TraceDirection;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("CausalTraceController")
class CausalTraceControllerTest {

    // A → B → F → S
    private static ModelDefinition chainModel() {
        return new ModelDefinitionBuilder()
                .name("Chain")
                .constant("A", 10, "Thing")
                .variable("B", "A * 2", "Thing")
                .stock("S", 0, "Thing")
                .flow("F", "B", "Day", null, "S")
                .build();
    }

    // A → F → S  (B removed compared to chainModel)
    private static ModelDefinition reducedModel() {
        return new ModelDefinitionBuilder()
                .name("Reduced")
                .constant("A", 10, "Thing")
                .stock("S", 0, "Thing")
                .flow("F", "A", "Day", null, "S")
                .build();
    }

    @Nested
    @DisplayName("startTrace / clearTrace lifecycle")
    class Lifecycle {

        @Test
        void shouldBeInactiveInitially() {
            CausalTraceController controller = new CausalTraceController();
            assertThat(controller.isActive()).isFalse();
            assertThat(controller.getAnalysis()).isNull();
        }

        @Test
        void shouldBeActiveAfterStartTrace() {
            CausalTraceController controller = new CausalTraceController();
            controller.startTrace("F", TraceDirection.UPSTREAM, chainModel());

            assertThat(controller.isActive()).isTrue();
            assertThat(controller.getAnalysis()).isNotNull();
            assertThat(controller.getAnalysis().origin()).isEqualTo("F");
        }

        @Test
        void shouldBeInactiveAfterClearTrace() {
            CausalTraceController controller = new CausalTraceController();
            controller.startTrace("F", TraceDirection.UPSTREAM, chainModel());
            controller.clearTrace();

            assertThat(controller.isActive()).isFalse();
            assertThat(controller.getAnalysis()).isNull();
        }
    }

    @Nested
    @DisplayName("invalidate recomputation")
    class Invalidate {

        @Test
        void shouldRecomputeTraceOnInvalidate() {
            CausalTraceController controller = new CausalTraceController();
            controller.startTrace("F", TraceDirection.UPSTREAM, chainModel());

            assertThat(controller.getAnalysis().isTraced("B")).isTrue();

            // After model mutation removes B, invalidate should recompute
            controller.invalidate(reducedModel());

            assertThat(controller.isActive()).isTrue();
            assertThat(controller.getAnalysis().isTraced("B")).isFalse();
            assertThat(controller.getAnalysis().isTraced("A")).isTrue();
        }

        @Test
        void shouldNotActivateWhenInvalidatedWhileInactive() {
            CausalTraceController controller = new CausalTraceController();
            controller.invalidate(chainModel());

            assertThat(controller.isActive()).isFalse();
        }

        @Test
        void shouldHandleNullModelGracefully() {
            CausalTraceController controller = new CausalTraceController();
            controller.startTrace("F", TraceDirection.UPSTREAM, chainModel());
            CausalTraceAnalysis before = controller.getAnalysis();

            controller.invalidate(null);

            assertThat(controller.isActive()).isTrue();
            assertThat(controller.getAnalysis()).isSameAs(before);
        }

        @Test
        void shouldClearTraceWhenTracedElementDeletedFromModel() {
            CausalTraceController controller = new CausalTraceController();
            controller.startTrace("B", TraceDirection.UPSTREAM, chainModel());
            assertThat(controller.isActive()).isTrue();

            // reducedModel() has no "B" — invalidate should clear the trace
            controller.invalidate(reducedModel());
            assertThat(controller.isActive()).isFalse();
            assertThat(controller.getAnalysis()).isNull();
        }

        @Test
        void shouldPreserveDirectionAcrossInvalidate() {
            CausalTraceController controller = new CausalTraceController();
            controller.startTrace("S", TraceDirection.DOWNSTREAM, chainModel());

            controller.invalidate(chainModel());

            assertThat(controller.getAnalysis().direction()).isEqualTo(TraceDirection.DOWNSTREAM);
            assertThat(controller.getAnalysis().origin()).isEqualTo("S");
        }
    }
}
