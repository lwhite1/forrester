package systems.courant.sd.app.canvas;

import systems.courant.sd.model.def.CausalLinkDef;
import systems.courant.sd.model.def.ModelDefinition;
import systems.courant.sd.model.def.ModelDefinitionBuilder;
import systems.courant.sd.model.graph.FeedbackAnalysis;
import systems.courant.sd.model.graph.FeedbackAnalysis.LoopType;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("LoopHighlightController")
class LoopHighlightControllerTest {

    private LoopHighlightController controller;

    /** Model with both R and B loops. */
    private ModelDefinition mixedModel() {
        return new ModelDefinitionBuilder()
                .name("Mixed CLD")
                .cldVariable("A")
                .cldVariable("B")
                .cldVariable("C")
                .causalLink("A", "B", CausalLinkDef.Polarity.POSITIVE)
                .causalLink("B", "A", CausalLinkDef.Polarity.POSITIVE)   // R loop
                .causalLink("B", "C", CausalLinkDef.Polarity.POSITIVE)
                .causalLink("C", "B", CausalLinkDef.Polarity.NEGATIVE)   // B loop
                .build();
    }

    @BeforeEach
    void setUp() {
        controller = new LoopHighlightController();
    }

    @Nested
    @DisplayName("type filtering")
    class TypeFilterTests {

        @Test
        void shouldDefaultToNullFilter() {
            assertThat(controller.getTypeFilter()).isNull();
        }

        @Test
        void shouldSetTypeFilter() {
            controller.setActive(true, LoopHighlightControllerTest.this::mixedModel);
            assertThat(controller.setTypeFilter(LoopType.REINFORCING)).isTrue();
            assertThat(controller.getTypeFilter()).isEqualTo(LoopType.REINFORCING);
        }

        @Test
        void shouldResetActiveIndexOnFilterChange() {
            controller.setActive(true, LoopHighlightControllerTest.this::mixedModel);
            controller.stepForward(); // set activeIndex to some loop
            assertThat(controller.getActiveIndex()).isGreaterThanOrEqualTo(0);

            controller.setTypeFilter(LoopType.BALANCING);
            assertThat(controller.getActiveIndex()).isEqualTo(-1);
        }

        @Test
        void shouldReturnFalseWhenFilterUnchanged() {
            controller.setActive(true, LoopHighlightControllerTest.this::mixedModel);
            controller.setTypeFilter(LoopType.REINFORCING);
            assertThat(controller.setTypeFilter(LoopType.REINFORCING)).isFalse();
        }

        @Test
        void shouldResetFilterOnDeactivate() {
            controller.setActive(true, LoopHighlightControllerTest.this::mixedModel);
            controller.setTypeFilter(LoopType.BALANCING);

            controller.setActive(false, LoopHighlightControllerTest.this::mixedModel);
            assertThat(controller.getTypeFilter()).isNull();
        }

        @Test
        void shouldCountFilteredLoops() {
            controller.setActive(true, LoopHighlightControllerTest.this::mixedModel);
            int totalCount = controller.filteredLoopCount();

            controller.setTypeFilter(LoopType.REINFORCING);
            int rCount = controller.filteredLoopCount();

            controller.setTypeFilter(LoopType.BALANCING);
            int bCount = controller.filteredLoopCount();

            assertThat(rCount).isGreaterThan(0);
            assertThat(bCount).isGreaterThan(0);
            assertThat(rCount + bCount).isLessThanOrEqualTo(totalCount);
        }
    }

    @Nested
    @DisplayName("filtered stepping")
    class FilteredSteppingTests {

        @Test
        void shouldStepOnlyThroughFilteredLoops() {
            controller.setActive(true, LoopHighlightControllerTest.this::mixedModel);
            controller.setTypeFilter(LoopType.REINFORCING);

            controller.stepForward();
            int idx = controller.getActiveIndex();
            assertThat(idx).isGreaterThanOrEqualTo(0);

            // Verify the loop at this index is actually reinforcing
            FeedbackAnalysis analysis = controller.getAnalysis();
            assertThat(analysis.loopType(idx)).isEqualTo(LoopType.REINFORCING);
        }

        @Test
        void shouldWrapAroundWithinFilter() {
            controller.setActive(true, LoopHighlightControllerTest.this::mixedModel);
            controller.setTypeFilter(LoopType.BALANCING);
            int bCount = controller.filteredLoopCount();

            // Step forward through all B loops and one more (should wrap)
            for (int i = 0; i <= bCount; i++) {
                controller.stepForward();
            }
            int idx = controller.getActiveIndex();
            FeedbackAnalysis analysis = controller.getAnalysis();
            assertThat(analysis.loopType(idx)).isEqualTo(LoopType.BALANCING);
        }

        @Test
        void shouldStepBackWithinFilter() {
            controller.setActive(true, LoopHighlightControllerTest.this::mixedModel);
            controller.setTypeFilter(LoopType.REINFORCING);

            controller.stepBack(); // from "all", goes to last R loop
            int idx = controller.getActiveIndex();
            FeedbackAnalysis analysis = controller.getAnalysis();
            assertThat(analysis.loopType(idx)).isEqualTo(LoopType.REINFORCING);
        }

        @Test
        void shouldNotStepWhenNoMatchingLoops() {
            // Model with only R loops
            ModelDefinition rOnly = new ModelDefinitionBuilder()
                    .name("R Only")
                    .cldVariable("X")
                    .cldVariable("Y")
                    .causalLink("X", "Y", CausalLinkDef.Polarity.POSITIVE)
                    .causalLink("Y", "X", CausalLinkDef.Polarity.POSITIVE)
                    .build();

            controller.setActive(true, () -> rOnly);
            controller.setTypeFilter(LoopType.BALANCING);

            assertThat(controller.filteredLoopCount()).isZero();
            assertThat(controller.stepForward()).isFalse();
            assertThat(controller.stepBack()).isFalse();
        }
    }

    @Nested
    @DisplayName("filtered active analysis")
    class FilteredActiveAnalysis {

        @Test
        void shouldFilterAnalysisByTypeWhenShowingAll() {
            controller.setActive(true, LoopHighlightControllerTest.this::mixedModel);
            controller.setTypeFilter(LoopType.REINFORCING);

            // activeIndex is -1 (show all), but with R filter
            FeedbackAnalysis active = controller.getActiveAnalysis();
            assertThat(active).isNotNull();
            assertThat(active.causalLoops()).allSatisfy(
                    loop -> assertThat(loop.type()).isEqualTo(LoopType.REINFORCING));
        }

        @Test
        void shouldReturnSingleLoopWhenSteppedWithFilter() {
            controller.setActive(true, LoopHighlightControllerTest.this::mixedModel);
            controller.setTypeFilter(LoopType.BALANCING);
            controller.stepForward();

            FeedbackAnalysis active = controller.getActiveAnalysis();
            assertThat(active).isNotNull();
            assertThat(active.loopCount()).isEqualTo(1);
        }
    }
}
