package systems.courant.sd.model.graph;

import systems.courant.sd.model.def.CausalLinkDef;
import systems.courant.sd.model.def.ModelDefinition;
import systems.courant.sd.model.def.ModelDefinitionBuilder;
import systems.courant.sd.model.graph.FeedbackAnalysis.CausalLoop;
import systems.courant.sd.model.graph.FeedbackAnalysis.LoopType;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("CLD Loop Detection")
class CldLoopDetectionTest {

    @Nested
    @DisplayName("no CLD loops")
    class NoLoops {

        @Test
        void shouldReturnEmptyForModelWithoutCausalLinks() {
            ModelDefinition def = new ModelDefinitionBuilder()
                    .name("No links")
                    .cldVariable("A")
                    .cldVariable("B")
                    .build();

            FeedbackAnalysis analysis = FeedbackAnalysis.analyze(def);

            assertThat(analysis.causalLoops()).isEmpty();
            assertThat(analysis.loopCount()).isZero();
        }

        @Test
        void shouldReturnEmptyForAcyclicCld() {
            ModelDefinition def = new ModelDefinitionBuilder()
                    .name("Acyclic")
                    .cldVariable("A")
                    .cldVariable("B")
                    .cldVariable("C")
                    .causalLink("A", "B", CausalLinkDef.Polarity.POSITIVE)
                    .causalLink("B", "C", CausalLinkDef.Polarity.NEGATIVE)
                    .build();

            FeedbackAnalysis analysis = FeedbackAnalysis.analyze(def);

            assertThat(analysis.causalLoops()).isEmpty();
            assertThat(analysis.loopCount()).isZero();
        }

        @Test
        void shouldReturnEmptyForEmptyModel() {
            ModelDefinition def = new ModelDefinitionBuilder()
                    .name("Empty")
                    .build();

            FeedbackAnalysis analysis = FeedbackAnalysis.analyze(def);

            assertThat(analysis.causalLoops()).isEmpty();
            assertThat(analysis.loopCount()).isZero();
        }
    }

    @Nested
    @DisplayName("reinforcing loops")
    class ReinforcingLoops {

        @Test
        void shouldDetectSimpleReinforcingLoop() {
            // A →(+) B →(+) A: zero negatives = reinforcing
            ModelDefinition def = new ModelDefinitionBuilder()
                    .name("R loop")
                    .cldVariable("A")
                    .cldVariable("B")
                    .causalLink("A", "B", CausalLinkDef.Polarity.POSITIVE)
                    .causalLink("B", "A", CausalLinkDef.Polarity.POSITIVE)
                    .build();

            FeedbackAnalysis analysis = FeedbackAnalysis.analyze(def);

            assertThat(analysis.causalLoops()).hasSize(1);
            CausalLoop loop = analysis.causalLoops().getFirst();
            assertThat(loop.type()).isEqualTo(LoopType.REINFORCING);
            assertThat(loop.label()).isEqualTo("R1");
            assertThat(loop.path()).containsExactly("A", "B");
        }

        @Test
        void shouldClassifyDoubleNegativeAsReinforcing() {
            // A →(-) B →(-) A: two negatives = reinforcing
            ModelDefinition def = new ModelDefinitionBuilder()
                    .name("Double negative")
                    .cldVariable("A")
                    .cldVariable("B")
                    .causalLink("A", "B", CausalLinkDef.Polarity.NEGATIVE)
                    .causalLink("B", "A", CausalLinkDef.Polarity.NEGATIVE)
                    .build();

            FeedbackAnalysis analysis = FeedbackAnalysis.analyze(def);

            assertThat(analysis.causalLoops()).hasSize(1);
            assertThat(analysis.causalLoops().getFirst().type())
                    .isEqualTo(LoopType.REINFORCING);
        }

        @Test
        void shouldDetectThreeNodeReinforcingLoop() {
            // A →(+) B →(+) C →(+) A: zero negatives
            ModelDefinition def = new ModelDefinitionBuilder()
                    .name("Three-node R")
                    .cldVariable("A")
                    .cldVariable("B")
                    .cldVariable("C")
                    .causalLink("A", "B", CausalLinkDef.Polarity.POSITIVE)
                    .causalLink("B", "C", CausalLinkDef.Polarity.POSITIVE)
                    .causalLink("C", "A", CausalLinkDef.Polarity.POSITIVE)
                    .build();

            FeedbackAnalysis analysis = FeedbackAnalysis.analyze(def);

            assertThat(analysis.causalLoops()).hasSize(1);
            CausalLoop loop = analysis.causalLoops().getFirst();
            assertThat(loop.type()).isEqualTo(LoopType.REINFORCING);
            assertThat(loop.path()).hasSize(3);
        }
    }

    @Nested
    @DisplayName("balancing loops")
    class BalancingLoops {

        @Test
        void shouldDetectSimpleBalancingLoop() {
            // A →(+) B →(-) A: one negative = balancing
            ModelDefinition def = new ModelDefinitionBuilder()
                    .name("B loop")
                    .cldVariable("A")
                    .cldVariable("B")
                    .causalLink("A", "B", CausalLinkDef.Polarity.POSITIVE)
                    .causalLink("B", "A", CausalLinkDef.Polarity.NEGATIVE)
                    .build();

            FeedbackAnalysis analysis = FeedbackAnalysis.analyze(def);

            assertThat(analysis.causalLoops()).hasSize(1);
            CausalLoop loop = analysis.causalLoops().getFirst();
            assertThat(loop.type()).isEqualTo(LoopType.BALANCING);
            assertThat(loop.label()).isEqualTo("B1");
        }

        @Test
        void shouldClassifyThreeNegativesAsBalancing() {
            // A →(-) B →(-) C →(-) A: three negatives (odd) = balancing
            ModelDefinition def = new ModelDefinitionBuilder()
                    .name("Triple negative")
                    .cldVariable("A")
                    .cldVariable("B")
                    .cldVariable("C")
                    .causalLink("A", "B", CausalLinkDef.Polarity.NEGATIVE)
                    .causalLink("B", "C", CausalLinkDef.Polarity.NEGATIVE)
                    .causalLink("C", "A", CausalLinkDef.Polarity.NEGATIVE)
                    .build();

            FeedbackAnalysis analysis = FeedbackAnalysis.analyze(def);

            assertThat(analysis.causalLoops()).hasSize(1);
            assertThat(analysis.causalLoops().getFirst().type())
                    .isEqualTo(LoopType.BALANCING);
        }
    }

    @Nested
    @DisplayName("indeterminate loops")
    class IndeterminateLoops {

        @Test
        void shouldClassifyLoopWithUnknownPolarityAsIndeterminate() {
            ModelDefinition def = new ModelDefinitionBuilder()
                    .name("Unknown polarity")
                    .cldVariable("A")
                    .cldVariable("B")
                    .causalLink("A", "B", CausalLinkDef.Polarity.POSITIVE)
                    .causalLink("B", "A", CausalLinkDef.Polarity.UNKNOWN)
                    .build();

            FeedbackAnalysis analysis = FeedbackAnalysis.analyze(def);

            assertThat(analysis.causalLoops()).hasSize(1);
            CausalLoop loop = analysis.causalLoops().getFirst();
            assertThat(loop.type()).isEqualTo(LoopType.INDETERMINATE);
            assertThat(loop.label()).isEqualTo("?1");
        }
    }

    @Nested
    @DisplayName("multiple loops")
    class MultipleLoops {

        @Test
        void shouldDetectTwoIndependentLoops() {
            ModelDefinition def = new ModelDefinitionBuilder()
                    .name("Two loops")
                    .cldVariable("A")
                    .cldVariable("B")
                    .cldVariable("C")
                    .cldVariable("D")
                    .causalLink("A", "B", CausalLinkDef.Polarity.POSITIVE)
                    .causalLink("B", "A", CausalLinkDef.Polarity.POSITIVE)
                    .causalLink("C", "D", CausalLinkDef.Polarity.POSITIVE)
                    .causalLink("D", "C", CausalLinkDef.Polarity.NEGATIVE)
                    .build();

            FeedbackAnalysis analysis = FeedbackAnalysis.analyze(def);

            assertThat(analysis.causalLoops()).hasSize(2);
            assertThat(analysis.loopCount()).isEqualTo(2);

            long reinforcing = analysis.causalLoops().stream()
                    .filter(l -> l.type() == LoopType.REINFORCING).count();
            long balancing = analysis.causalLoops().stream()
                    .filter(l -> l.type() == LoopType.BALANCING).count();
            assertThat(reinforcing).isEqualTo(1);
            assertThat(balancing).isEqualTo(1);
        }

        @Test
        void shouldDetectMultipleCyclesInSameComponent() {
            // A ↔ B ↔ C with A→C and C→A creates multiple cycles:
            // A→B→A, B→C→B, A→B→C→A
            ModelDefinition def = new ModelDefinitionBuilder()
                    .name("Overlapping")
                    .cldVariable("A")
                    .cldVariable("B")
                    .cldVariable("C")
                    .causalLink("A", "B", CausalLinkDef.Polarity.POSITIVE)
                    .causalLink("B", "A", CausalLinkDef.Polarity.POSITIVE)
                    .causalLink("B", "C", CausalLinkDef.Polarity.POSITIVE)
                    .causalLink("C", "B", CausalLinkDef.Polarity.NEGATIVE)
                    .causalLink("A", "C", CausalLinkDef.Polarity.POSITIVE)
                    .causalLink("C", "A", CausalLinkDef.Polarity.NEGATIVE)
                    .build();

            FeedbackAnalysis analysis = FeedbackAnalysis.analyze(def);

            // Independent cycle basis: 3 unique cycles (A↔B, B↔C, A↔C)
            // Two non-tree edges (B→C, C→B) produce the same normalized cycle
            assertThat(analysis.causalLoops().size()).isEqualTo(3);
        }
    }

    @Nested
    @DisplayName("loop participants and edges")
    class ParticipantsAndEdges {

        @Test
        void shouldIncludeCldVariablesInLoopParticipants() {
            ModelDefinition def = new ModelDefinitionBuilder()
                    .name("Participants")
                    .cldVariable("X")
                    .cldVariable("Y")
                    .cldVariable("Z")
                    .causalLink("X", "Y", CausalLinkDef.Polarity.POSITIVE)
                    .causalLink("Y", "X", CausalLinkDef.Polarity.POSITIVE)
                    .build();

            FeedbackAnalysis analysis = FeedbackAnalysis.analyze(def);

            assertThat(analysis.isInLoop("X")).isTrue();
            assertThat(analysis.isInLoop("Y")).isTrue();
            assertThat(analysis.isInLoop("Z")).isFalse();
        }

        @Test
        void shouldIncludeCausalLinkEdgesInLoopEdges() {
            ModelDefinition def = new ModelDefinitionBuilder()
                    .name("Edges")
                    .cldVariable("A")
                    .cldVariable("B")
                    .causalLink("A", "B", CausalLinkDef.Polarity.POSITIVE)
                    .causalLink("B", "A", CausalLinkDef.Polarity.NEGATIVE)
                    .build();

            FeedbackAnalysis analysis = FeedbackAnalysis.analyze(def);

            assertThat(analysis.isLoopEdge("A", "B")).isTrue();
            assertThat(analysis.isLoopEdge("B", "A")).isTrue();
        }

        @Test
        void shouldNotIncludeNonLoopEdges() {
            ModelDefinition def = new ModelDefinitionBuilder()
                    .name("Non-loop edge")
                    .cldVariable("A")
                    .cldVariable("B")
                    .cldVariable("C")
                    .causalLink("A", "B", CausalLinkDef.Polarity.POSITIVE)
                    .causalLink("B", "A", CausalLinkDef.Polarity.POSITIVE)
                    .causalLink("B", "C", CausalLinkDef.Polarity.POSITIVE)
                    .build();

            FeedbackAnalysis analysis = FeedbackAnalysis.analyze(def);

            assertThat(analysis.isLoopEdge("A", "B")).isTrue();
            assertThat(analysis.isLoopEdge("B", "A")).isTrue();
            assertThat(analysis.isLoopEdge("B", "C")).isFalse();
        }
    }

    @Nested
    @DisplayName("mixed SF and CLD models")
    class MixedModels {

        @Test
        void shouldDetectBothSfAndCldLoops() {
            ModelDefinition def = new ModelDefinitionBuilder()
                    .name("Mixed")
                    // SF feedback loop
                    .stock("Prey", 100, "Animal")
                    .stock("Predators", 20, "Animal")
                    .flow("Predation", "Prey * Predators * 0.01", "Day",
                            "Prey", "Predators")
                    // CLD feedback loop
                    .cldVariable("Stress")
                    .cldVariable("Burnout")
                    .causalLink("Stress", "Burnout", CausalLinkDef.Polarity.POSITIVE)
                    .causalLink("Burnout", "Stress", CausalLinkDef.Polarity.POSITIVE)
                    .build();

            FeedbackAnalysis analysis = FeedbackAnalysis.analyze(def);

            // SF loop
            assertThat(analysis.isInLoop("Prey")).isTrue();
            assertThat(analysis.isInLoop("Predators")).isTrue();

            // CLD loop
            assertThat(analysis.isInLoop("Stress")).isTrue();
            assertThat(analysis.isInLoop("Burnout")).isTrue();

            // Total: 1 S&F cycle (INDETERMINATE) + 1 CLD cycle (REINFORCING)
            assertThat(analysis.loopCount()).isEqualTo(2);
            assertThat(analysis.causalLoops()).hasSize(2);
            assertThat(analysis.causalLoops().stream()
                    .filter(l -> l.type() == LoopType.REINFORCING).count()).isEqualTo(1);
            assertThat(analysis.causalLoops().stream()
                    .filter(l -> l.type() == LoopType.INDETERMINATE).count()).isEqualTo(1);
        }
    }

    @Nested
    @DisplayName("CompetitionFaculty model (#1208)")
    class CompetitionFacultyModel {

        @Test
        void shouldDetectLoopsInCompetitionFacultyGraph() {
            // Reproduces the CompetitionFaculty CLD model graph structure
            ModelDefinition def = new ModelDefinitionBuilder()
                    .name("CompetitionFaculty")
                    .cldVariable("scientific papers section A")
                    .cldVariable("scientific papers section B")
                    .cldVariable("fraction of total budget to section A")
                    .cldVariable("budget section A")
                    .cldVariable("budget section B")
                    .cldVariable("scientific personnel section A")
                    .cldVariable("scientific personnel section B")
                    .cldVariable("total TPM budget")
                    .causalLink("scientific papers section A", "fraction of total budget to section A", CausalLinkDef.Polarity.POSITIVE)
                    .causalLink("scientific papers section B", "fraction of total budget to section A", CausalLinkDef.Polarity.NEGATIVE)
                    .causalLink("scientific personnel section A", "scientific papers section A", CausalLinkDef.Polarity.POSITIVE)
                    .causalLink("scientific personnel section B", "scientific papers section B", CausalLinkDef.Polarity.POSITIVE)
                    .causalLink("fraction of total budget to section A", "budget section B", CausalLinkDef.Polarity.NEGATIVE)
                    .causalLink("fraction of total budget to section A", "budget section A", CausalLinkDef.Polarity.POSITIVE)
                    .causalLink("budget section A", "scientific personnel section A", CausalLinkDef.Polarity.POSITIVE)
                    .causalLink("total TPM budget", "budget section A", CausalLinkDef.Polarity.POSITIVE)
                    .causalLink("total TPM budget", "budget section B", CausalLinkDef.Polarity.POSITIVE)
                    .causalLink("budget section B", "scientific personnel section B", CausalLinkDef.Polarity.POSITIVE)
                    .build();

            FeedbackAnalysis analysis = FeedbackAnalysis.analyze(def);

            assertThat(analysis.loopCount()).as("should detect feedback loops").isGreaterThan(0);
            assertThat(analysis.causalLoops()).isNotEmpty();
            // Both loops should be reinforcing (even number of negative links)
            assertThat(analysis.causalLoops()).allSatisfy(loop ->
                    assertThat(loop.type()).isEqualTo(LoopType.REINFORCING));
        }
    }

    @Nested
    @DisplayName("Middle East model (#1301)")
    class MiddleEastModel {

        @Test
        void shouldProduceReasonableLoopCount() {
            // Reproduces the Middle East CLD model (13 variables, 22 links after polarity fix)
            ModelDefinition def = new ModelDefinitionBuilder()
                    .name("ConflictInMiddleEast")
                    .cldVariable("successful suicide attacks")
                    .cldVariable("autonomy Palestinians")
                    .cldVariable("preparedness to dialogue")
                    .cldVariable("willingness suicide attacks")
                    .cldVariable("security by shielding wall")
                    .cldVariable("degree of equality in negotiations")
                    .cldVariable("living conditions Palestinians")
                    .cldVariable("restrictions on Palestinians")
                    .cldVariable("reprisal Isi on Paln")
                    .cldVariable("trust Isi in Paln")
                    .cldVariable("trust Paln in Isi")
                    .cldVariable("progress peace process")
                    .cldVariable("chance of success of peace process")
                    .causalLink("trust Isi in Paln", "restrictions on Palestinians", CausalLinkDef.Polarity.NEGATIVE)
                    .causalLink("restrictions on Palestinians", "living conditions Palestinians", CausalLinkDef.Polarity.NEGATIVE)
                    .causalLink("successful suicide attacks", "trust Isi in Paln", CausalLinkDef.Polarity.NEGATIVE)
                    .causalLink("successful suicide attacks", "reprisal Isi on Paln", CausalLinkDef.Polarity.POSITIVE)
                    .causalLink("reprisal Isi on Paln", "living conditions Palestinians", CausalLinkDef.Polarity.NEGATIVE)
                    .causalLink("trust Isi in Paln", "security by shielding wall", CausalLinkDef.Polarity.NEGATIVE)
                    .causalLink("security by shielding wall", "successful suicide attacks", CausalLinkDef.Polarity.NEGATIVE)
                    .causalLink("security by shielding wall", "living conditions Palestinians", CausalLinkDef.Polarity.NEGATIVE)
                    .causalLink("living conditions Palestinians", "willingness suicide attacks", CausalLinkDef.Polarity.NEGATIVE)
                    .causalLink("willingness suicide attacks", "successful suicide attacks", CausalLinkDef.Polarity.POSITIVE)
                    .causalLink("living conditions Palestinians", "preparedness to dialogue", CausalLinkDef.Polarity.POSITIVE)
                    .causalLink("trust Paln in Isi", "preparedness to dialogue", CausalLinkDef.Polarity.POSITIVE)
                    .causalLink("trust Isi in Paln", "preparedness to dialogue", CausalLinkDef.Polarity.POSITIVE)
                    .causalLink("preparedness to dialogue", "progress peace process", CausalLinkDef.Polarity.POSITIVE)
                    .causalLink("reprisal Isi on Paln", "trust Paln in Isi", CausalLinkDef.Polarity.NEGATIVE)
                    .causalLink("living conditions Palestinians", "degree of equality in negotiations", CausalLinkDef.Polarity.POSITIVE)
                    .causalLink("degree of equality in negotiations", "chance of success of peace process", CausalLinkDef.Polarity.POSITIVE)
                    .causalLink("chance of success of peace process", "progress peace process", CausalLinkDef.Polarity.POSITIVE)
                    .causalLink("progress peace process", "autonomy Palestinians", CausalLinkDef.Polarity.POSITIVE)
                    .causalLink("autonomy Palestinians", "living conditions Palestinians", CausalLinkDef.Polarity.POSITIVE)
                    .causalLink("autonomy Palestinians", "successful suicide attacks", CausalLinkDef.Polarity.POSITIVE)
                    .causalLink("progress peace process", "trust Paln in Isi", CausalLinkDef.Polarity.POSITIVE)
                    .causalLink("progress peace process", "trust Isi in Paln", CausalLinkDef.Polarity.POSITIVE)
                    .build();

            FeedbackAnalysis analysis = FeedbackAnalysis.analyze(def);

            // Independent cycle basis: exactly 8 loops, matching original hand-labeled model
            assertThat(analysis.loopCount()).isEqualTo(8);

            // No duplicate loops (different labels, same path)
            List<List<String>> paths = analysis.causalLoops().stream()
                    .map(CausalLoop::path).toList();
            assertThat(paths).doesNotHaveDuplicates();

            // Shortest loops should come first
            for (int i = 1; i < paths.size(); i++) {
                assertThat(paths.get(i).size())
                        .as("loops should be sorted by length")
                        .isGreaterThanOrEqualTo(paths.get(i - 1).size());
            }
        }
    }

    @Nested
    @DisplayName("filterToLoop isolation")
    class FilterToLoopIsolation {

        @Test
        void shouldHighlightDifferentElementsForOverlappingLoops() {
            // Two loops sharing 2 elements but differing on the 3rd:
            // R1: A → B → C → A
            // R2: A → B → D → A
            ModelDefinition def = new ModelDefinitionBuilder()
                    .name("Overlapping")
                    .cldVariable("A")
                    .cldVariable("B")
                    .cldVariable("C")
                    .cldVariable("D")
                    .causalLink("A", "B", CausalLinkDef.Polarity.POSITIVE)
                    .causalLink("B", "C", CausalLinkDef.Polarity.POSITIVE)
                    .causalLink("C", "A", CausalLinkDef.Polarity.POSITIVE)
                    .causalLink("B", "D", CausalLinkDef.Polarity.POSITIVE)
                    .causalLink("D", "A", CausalLinkDef.Polarity.POSITIVE)
                    .build();

            FeedbackAnalysis analysis = FeedbackAnalysis.analyze(def);
            assertThat(analysis.loopCount()).isGreaterThanOrEqualTo(2);

            FeedbackAnalysis loop0 = analysis.filterToLoop(0);
            FeedbackAnalysis loop1 = analysis.filterToLoop(1);

            // Each filtered analysis must contain ONLY its own path elements
            assertThat(loop0.loopParticipants())
                    .as("loop 0 participants must match its path exactly")
                    .containsExactlyInAnyOrderElementsOf(loop0.causalLoops().getFirst().path());
            assertThat(loop1.loopParticipants())
                    .as("loop 1 participants must match its path exactly")
                    .containsExactlyInAnyOrderElementsOf(loop1.causalLoops().getFirst().path());

            // The two loops must NOT highlight identical element sets
            assertThat(loop0.loopParticipants())
                    .as("overlapping loops must highlight different elements")
                    .isNotEqualTo(loop1.loopParticipants());
        }
    }

    @Nested
    @DisplayName("loop labels")
    class LoopLabels {

        @Test
        void shouldNumberLoopsByType() {
            // Create two reinforcing and one balancing loop
            ModelDefinition def = new ModelDefinitionBuilder()
                    .name("Labels")
                    .cldVariable("A")
                    .cldVariable("B")
                    .cldVariable("C")
                    .cldVariable("D")
                    .cldVariable("E")
                    .cldVariable("F")
                    .causalLink("A", "B", CausalLinkDef.Polarity.POSITIVE)
                    .causalLink("B", "A", CausalLinkDef.Polarity.POSITIVE)
                    .causalLink("C", "D", CausalLinkDef.Polarity.POSITIVE)
                    .causalLink("D", "C", CausalLinkDef.Polarity.POSITIVE)
                    .causalLink("E", "F", CausalLinkDef.Polarity.POSITIVE)
                    .causalLink("F", "E", CausalLinkDef.Polarity.NEGATIVE)
                    .build();

            FeedbackAnalysis analysis = FeedbackAnalysis.analyze(def);

            List<String> labels = analysis.causalLoops().stream()
                    .map(CausalLoop::label).toList();
            assertThat(labels).contains("R1", "R2", "B1");
        }
    }
}
