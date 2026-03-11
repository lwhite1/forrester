package systems.courant.shrewd.model.graph;

import systems.courant.shrewd.model.def.CausalLinkDef;
import systems.courant.shrewd.model.def.ModelDefinition;
import systems.courant.shrewd.model.def.ModelDefinitionBuilder;
import systems.courant.shrewd.model.graph.FeedbackAnalysis.CausalLoop;
import systems.courant.shrewd.model.graph.FeedbackAnalysis.LoopInfo;
import systems.courant.shrewd.model.graph.FeedbackAnalysis.LoopType;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("FeedbackAnalysis")
class FeedbackAnalysisTest {

    @Nested
    @DisplayName("no loops")
    class NoLoops {

        @Test
        void shouldReturnEmptyForAcyclicModel() {
            ModelDefinition def = new ModelDefinitionBuilder()
                    .name("Acyclic")
                    .constant("Rate", 5, "Thing")
                    .stock("S", 0, "Thing")
                    .flow("F", "Rate", "Day", null, "S")
                    .build();

            FeedbackAnalysis analysis = FeedbackAnalysis.analyze(def);

            assertThat(analysis.loopParticipants()).isEmpty();
            assertThat(analysis.loopEdges()).isEmpty();
            assertThat(analysis.loopCount()).isZero();
        }

        @Test
        void shouldHandleEmptyModel() {
            ModelDefinition def = new ModelDefinitionBuilder()
                    .name("Empty")
                    .build();

            FeedbackAnalysis analysis = FeedbackAnalysis.analyze(def);

            assertThat(analysis.loopParticipants()).isEmpty();
            assertThat(analysis.loopCount()).isZero();
        }

        @Test
        void shouldNotFlagLinearChain() {
            // A → B → C with each flow referencing only its source stock
            // This is the user's SIR-like model — no feedback
            ModelDefinition def = new ModelDefinitionBuilder()
                    .name("Linear SIR")
                    .stock("A", 100, "Person")
                    .stock("B", 10, "Person")
                    .stock("C", 0, "Person")
                    .flow("AB", "A * 0.1", "Day", "A", "B")
                    .flow("BC", "B * 0.05", "Day", "B", "C")
                    .build();

            FeedbackAnalysis analysis = FeedbackAnalysis.analyze(def);

            assertThat(analysis.loopParticipants()).isEmpty();
            assertThat(analysis.loopCount()).isZero();
        }
    }

    @Nested
    @DisplayName("single-stock loops (not detected)")
    class SingleStockLoops {

        @Test
        void shouldNotFlagPopulationGrowthLoop() {
            // Population → Births → Population is a single-stock self-influence.
            // Not a multi-stock feedback loop.
            ModelDefinition def = new ModelDefinitionBuilder()
                    .name("Population Growth")
                    .stock("Population", 100, "Person")
                    .flow("Births", "Population * 0.04", "Day", null, "Population")
                    .build();

            FeedbackAnalysis analysis = FeedbackAnalysis.analyze(def);

            assertThat(analysis.loopParticipants()).isEmpty();
            assertThat(analysis.loopCount()).isZero();
        }

        @Test
        void shouldNotFlagDrainLoop() {
            ModelDefinition def = new ModelDefinitionBuilder()
                    .name("Drain")
                    .stock("Tank", 100, "Litre")
                    .constant("Rate", 0.1, "Dimensionless unit")
                    .flow("Drain", "Tank * Rate", "Day", "Tank", null)
                    .build();

            FeedbackAnalysis analysis = FeedbackAnalysis.analyze(def);

            assertThat(analysis.isInLoop("Tank")).isFalse();
            assertThat(analysis.isInLoop("Drain")).isFalse();
            assertThat(analysis.isInLoop("Rate")).isFalse();
            assertThat(analysis.loopCount()).isZero();
        }

        @Test
        void shouldNotFlagSharedStockWithTwoFlows() {
            // Population with Births and Deaths — both reference Population
            // but it's still a single-stock structure, not a multi-stock loop
            ModelDefinition def = new ModelDefinitionBuilder()
                    .name("Shared Node")
                    .stock("Population", 100, "Person")
                    .flow("Births", "Population * 0.04", "Day", null, "Population")
                    .flow("Deaths", "Population * 0.02", "Day", "Population", null)
                    .build();

            FeedbackAnalysis analysis = FeedbackAnalysis.analyze(def);

            assertThat(analysis.loopParticipants()).isEmpty();
            assertThat(analysis.loopCount()).isZero();
        }
    }

    @Nested
    @DisplayName("multi-stock feedback loops")
    class MultiStockLoops {

        @Test
        void shouldDetectPredatorPreyLoop() {
            // Predation flow references both Prey and Predators, creating
            // a Prey ↔ Predators feedback loop
            ModelDefinition def = new ModelDefinitionBuilder()
                    .name("Predator-Prey")
                    .stock("Prey", 100, "Animal")
                    .stock("Predators", 20, "Animal")
                    .flow("Predation", "Prey * Predators * 0.01", "Day",
                            "Prey", "Predators")
                    .build();

            FeedbackAnalysis analysis = FeedbackAnalysis.analyze(def);

            assertThat(analysis.isInLoop("Prey")).isTrue();
            assertThat(analysis.isInLoop("Predators")).isTrue();
            assertThat(analysis.isInLoop("Predation")).isTrue();
            assertThat(analysis.loopCount()).isEqualTo(1);
        }

        @Test
        void shouldDetectLoopsInSirModel() {
            // In the full SIR model, Infection depends on Susceptible, Infectious,
            // and Recovered — creating a multi-stock feedback structure
            ModelDefinition sir = new ModelDefinitionBuilder()
                    .name("SIR")
                    .stock("Susceptible", 1000, "Person")
                    .stock("Infectious", 10, "Person")
                    .stock("Recovered", 0, "Person")
                    .constant("Contact Rate", 8, "Dimensionless unit")
                    .constant("Infectivity", 0.5, "Dimensionless unit")
                    .constant("Duration", 5, "Day")
                    .flow("Infection",
                            "Contact_Rate * Infectious / (Susceptible + Infectious + Recovered) * Infectivity * Susceptible",
                            "Day", "Susceptible", "Infectious")
                    .flow("Recovery", "Infectious / Duration", "Day",
                            "Infectious", "Recovered")
                    .build();

            FeedbackAnalysis analysis = FeedbackAnalysis.analyze(sir);

            // All three stocks are in the loop (Recovered appears in Infection's
            // denominator, so it causally influences other stocks via the flow)
            assertThat(analysis.isInLoop("Susceptible")).isTrue();
            assertThat(analysis.isInLoop("Infectious")).isTrue();
            assertThat(analysis.isInLoop("Recovered")).isTrue();

            // Both flows participate
            assertThat(analysis.isInLoop("Infection")).isTrue();
            assertThat(analysis.isInLoop("Recovery")).isTrue();

            // Constants do NOT participate
            assertThat(analysis.isInLoop("Contact Rate")).isFalse();
            assertThat(analysis.isInLoop("Infectivity")).isFalse();
            assertThat(analysis.isInLoop("Duration")).isFalse();

            // One feedback group containing all 3 stocks in the SCC
            assertThat(analysis.loopCount()).isEqualTo(1);
            assertThat(analysis.causalLoops().getFirst().path())
                    .containsExactlyInAnyOrder("Susceptible", "Infectious", "Recovered");
        }

        @Test
        void shouldDetectTwoIndependentLoops() {
            // Two separate predator-prey-like loops
            ModelDefinition def = new ModelDefinitionBuilder()
                    .name("Two Loops")
                    .stock("A", 100, "Thing")
                    .stock("B", 50, "Thing")
                    .stock("C", 200, "Thing")
                    .stock("D", 30, "Thing")
                    .flow("F1", "A * B * 0.01", "Day", "A", "B")
                    .flow("F2", "C * D * 0.02", "Day", "C", "D")
                    .build();

            FeedbackAnalysis analysis = FeedbackAnalysis.analyze(def);

            assertThat(analysis.isInLoop("A")).isTrue();
            assertThat(analysis.isInLoop("B")).isTrue();
            assertThat(analysis.isInLoop("C")).isTrue();
            assertThat(analysis.isInLoop("D")).isTrue();
            assertThat(analysis.loopCount()).isEqualTo(2);
        }

        @Test
        void shouldDetectLoopThroughAuxiliary() {
            // A → G → B and B → (via aux X) → F → A
            // Tests transitive dependency resolution through auxiliaries
            ModelDefinition def = new ModelDefinitionBuilder()
                    .name("Aux Loop")
                    .stock("A", 100, "Thing")
                    .stock("B", 50, "Thing")
                    .aux("X", "B * 2", "Thing")
                    .flow("F", "X", "Day", "B", "A")
                    .flow("G", "A * 0.1", "Day", "A", "B")
                    .build();

            FeedbackAnalysis analysis = FeedbackAnalysis.analyze(def);

            assertThat(analysis.isInLoop("A")).isTrue();
            assertThat(analysis.isInLoop("B")).isTrue();
            assertThat(analysis.isInLoop("F")).isTrue();
            assertThat(analysis.isInLoop("G")).isTrue();
            assertThat(analysis.loopCount()).isEqualTo(1);
        }

        @Test
        void shouldReportLoopEdges() {
            ModelDefinition def = new ModelDefinitionBuilder()
                    .name("Predator-Prey")
                    .stock("Prey", 100, "Animal")
                    .stock("Predators", 20, "Animal")
                    .flow("Predation", "Prey * Predators * 0.01", "Day",
                            "Prey", "Predators")
                    .build();

            FeedbackAnalysis analysis = FeedbackAnalysis.analyze(def);

            // Formula dependency edges: stocks → flow
            assertThat(analysis.isLoopEdge("Prey", "Predation")).isTrue();
            assertThat(analysis.isLoopEdge("Predators", "Predation")).isTrue();

            // Material flow edges: flow → stocks
            assertThat(analysis.isLoopEdge("Predation", "Prey")).isTrue();
            assertThat(analysis.isLoopEdge("Predation", "Predators")).isTrue();
        }
    }

    @Nested
    @DisplayName("loop info and filtering")
    class LoopInfoAndFiltering {

        @Test
        void shouldReturnLoopInfoForSfCycle() {
            ModelDefinition def = new ModelDefinitionBuilder()
                    .name("Predator-Prey")
                    .stock("Prey", 100, "Animal")
                    .stock("Predators", 20, "Animal")
                    .flow("Predation", "Prey * Predators * 0.01", "Day",
                            "Prey", "Predators")
                    .build();

            FeedbackAnalysis analysis = FeedbackAnalysis.analyze(def);

            assertThat(analysis.loopCount()).isEqualTo(1);

            Optional<LoopInfo> info = analysis.loopInfo(0);
            assertThat(info).isPresent();
            assertThat(info.get().label()).startsWith("Feedback Group");
            assertThat(info.get().type()).isEqualTo(LoopType.INDETERMINATE);
            assertThat(info.get().narrative()).isNotEmpty();
        }

        @Test
        void shouldReturnEmptyForOutOfRangeIndex() {
            ModelDefinition def = new ModelDefinitionBuilder()
                    .name("Empty")
                    .build();

            FeedbackAnalysis analysis = FeedbackAnalysis.analyze(def);

            assertThat(analysis.loopInfo(0)).isEmpty();
            assertThat(analysis.loopInfo(-1)).isEmpty();
        }

        @Test
        void shouldFilterSfCycleToSingleLoop() {
            ModelDefinition def = new ModelDefinitionBuilder()
                    .name("Two Loops")
                    .stock("A", 100, "Thing")
                    .stock("B", 50, "Thing")
                    .stock("C", 200, "Thing")
                    .stock("D", 30, "Thing")
                    .flow("F1", "A * B * 0.01", "Day", "A", "B")
                    .flow("F2", "C * D * 0.02", "Day", "C", "D")
                    .build();

            FeedbackAnalysis analysis = FeedbackAnalysis.analyze(def);
            assertThat(analysis.loopCount()).isEqualTo(2);

            FeedbackAnalysis filtered = analysis.filterToLoop(0);
            assertThat(filtered.loopCount()).isEqualTo(1);
            assertThat(filtered.causalLoops()).hasSize(1);
            // Filtered analysis should contain only the first cycle's elements
            assertThat(filtered.loopParticipants()).isSubsetOf(analysis.loopParticipants());
        }

        @Test
        void shouldFilterCldLoopToSingleLoop() {
            ModelDefinition def = new ModelDefinitionBuilder()
                    .name("CLD")
                    .cldVariable("X")
                    .cldVariable("Y")
                    .cldVariable("Z")
                    .causalLink("X", "Y", CausalLinkDef.Polarity.POSITIVE)
                    .causalLink("Y", "Z", CausalLinkDef.Polarity.POSITIVE)
                    .causalLink("Z", "X", CausalLinkDef.Polarity.NEGATIVE)
                    .build();

            FeedbackAnalysis analysis = FeedbackAnalysis.analyze(def);
            assertThat(analysis.causalLoops()).isNotEmpty();

            int totalLoops = analysis.loopCount();
            FeedbackAnalysis filtered = analysis.filterToLoop(totalLoops - 1);
            assertThat(filtered.loopCount()).isEqualTo(1);
            assertThat(filtered.causalLoops()).hasSize(1);
        }

        @Test
        void shouldReturnFullAnalysisForInvalidIndex() {
            ModelDefinition def = new ModelDefinitionBuilder()
                    .name("Predator-Prey")
                    .stock("Prey", 100, "Animal")
                    .stock("Predators", 20, "Animal")
                    .flow("Predation", "Prey * Predators * 0.01", "Day",
                            "Prey", "Predators")
                    .build();

            FeedbackAnalysis analysis = FeedbackAnalysis.analyze(def);
            FeedbackAnalysis filtered = analysis.filterToLoop(-1);
            assertThat(filtered).isSameAs(analysis);

            FeedbackAnalysis filtered2 = analysis.filterToLoop(999);
            assertThat(filtered2).isSameAs(analysis);
        }

        @Test
        void shouldReturnCldLoopInfoWithType() {
            ModelDefinition def = new ModelDefinitionBuilder()
                    .name("CLD")
                    .cldVariable("X")
                    .cldVariable("Y")
                    .causalLink("X", "Y", CausalLinkDef.Polarity.POSITIVE)
                    .causalLink("Y", "X", CausalLinkDef.Polarity.POSITIVE)
                    .build();

            FeedbackAnalysis analysis = FeedbackAnalysis.analyze(def);
            assertThat(analysis.causalLoops()).isNotEmpty();

            // CLD loops come after SF groups (0 SF groups here)
            Optional<LoopInfo> info = analysis.loopInfo(0);
            assertThat(info).isPresent();
            assertThat(info.get().type()).isEqualTo(FeedbackAnalysis.LoopType.REINFORCING);
            assertThat(info.get().label()).startsWith("R");
            assertThat(info.get().narrative()).contains("X");
            assertThat(info.get().narrative()).contains("Y");
        }
    }

    @Nested
    @DisplayName("type filtering")
    class TypeFiltering {

        @Test
        void shouldFilterIndicesByType() {
            // Create a model with both R and B loops
            ModelDefinition def = new ModelDefinitionBuilder()
                    .name("Mixed CLD")
                    .cldVariable("A")
                    .cldVariable("B")
                    .cldVariable("C")
                    .causalLink("A", "B", CausalLinkDef.Polarity.POSITIVE)
                    .causalLink("B", "A", CausalLinkDef.Polarity.POSITIVE)
                    .causalLink("B", "C", CausalLinkDef.Polarity.POSITIVE)
                    .causalLink("C", "B", CausalLinkDef.Polarity.NEGATIVE)
                    .build();

            FeedbackAnalysis analysis = FeedbackAnalysis.analyze(def);
            assertThat(analysis.causalLoops()).hasSizeGreaterThanOrEqualTo(2);

            // Should have at least one R and one B
            List<Integer> rIndices = analysis.filteredIndices(LoopType.REINFORCING);
            List<Integer> bIndices = analysis.filteredIndices(LoopType.BALANCING);
            assertThat(rIndices).isNotEmpty();
            assertThat(bIndices).isNotEmpty();

            // Null filter returns all
            List<Integer> allIndices = analysis.filteredIndices(null);
            assertThat(allIndices).hasSize(analysis.loopCount());
        }

        @Test
        void shouldFilterByTypeReturningOnlyMatchingLoops() {
            ModelDefinition def = new ModelDefinitionBuilder()
                    .name("Mixed CLD")
                    .cldVariable("A")
                    .cldVariable("B")
                    .cldVariable("C")
                    .causalLink("A", "B", CausalLinkDef.Polarity.POSITIVE)
                    .causalLink("B", "A", CausalLinkDef.Polarity.POSITIVE)
                    .causalLink("B", "C", CausalLinkDef.Polarity.POSITIVE)
                    .causalLink("C", "B", CausalLinkDef.Polarity.NEGATIVE)
                    .build();

            FeedbackAnalysis analysis = FeedbackAnalysis.analyze(def);

            FeedbackAnalysis rOnly = analysis.filterByType(LoopType.REINFORCING);
            assertThat(rOnly.causalLoops()).allSatisfy(
                    loop -> assertThat(loop.type()).isEqualTo(LoopType.REINFORCING));
            assertThat(rOnly.loopGroups()).isEmpty();

            FeedbackAnalysis bOnly = analysis.filterByType(LoopType.BALANCING);
            assertThat(bOnly.causalLoops()).allSatisfy(
                    loop -> assertThat(loop.type()).isEqualTo(LoopType.BALANCING));
        }

        @Test
        void shouldReturnFullAnalysisForNullFilter() {
            ModelDefinition def = new ModelDefinitionBuilder()
                    .name("CLD")
                    .cldVariable("X")
                    .cldVariable("Y")
                    .causalLink("X", "Y", CausalLinkDef.Polarity.POSITIVE)
                    .causalLink("Y", "X", CausalLinkDef.Polarity.POSITIVE)
                    .build();

            FeedbackAnalysis analysis = FeedbackAnalysis.analyze(def);
            assertThat(analysis.filterByType(null)).isSameAs(analysis);
        }

        @Test
        void shouldReturnIndeterminateLoopTypeForSfCycles() {
            ModelDefinition def = new ModelDefinitionBuilder()
                    .name("Predator-Prey")
                    .stock("Prey", 100, "Animal")
                    .stock("Predators", 20, "Animal")
                    .flow("Predation", "Prey * Predators * 0.01", "Day",
                            "Prey", "Predators")
                    .build();

            FeedbackAnalysis analysis = FeedbackAnalysis.analyze(def);
            assertThat(analysis.loopType(0)).isEqualTo(LoopType.INDETERMINATE);
        }
    }

    @Nested
    @DisplayName("S&F feedback groups")
    class SfFeedbackGroups {

        @Test
        void shouldReportSccAsOneGroup() {
            // 3-stock SCC should produce one feedback group, not individual cycles
            ModelDefinition def = new ModelDefinitionBuilder()
                    .name("SIR")
                    .stock("S", 1000, "Person")
                    .stock("I", 10, "Person")
                    .stock("R", 0, "Person")
                    .flow("Infection",
                            "S * I / (S + I + R) * 0.5", "Day", "S", "I")
                    .flow("Recovery", "I * 0.1", "Day", "I", "R")
                    .build();

            FeedbackAnalysis analysis = FeedbackAnalysis.analyze(def);

            assertThat(analysis.loopGroups()).isEmpty();
            // One feedback group for the SCC
            assertThat(analysis.loopCount()).isEqualTo(1);
            FeedbackAnalysis.CausalLoop group = analysis.causalLoops().getFirst();
            assertThat(group.type()).isEqualTo(LoopType.INDETERMINATE);
            assertThat(group.label()).startsWith("Feedback Group");
            assertThat(group.path()).containsExactlyInAnyOrder("I", "R", "S");
        }

        @Test
        void shouldFilterSfGroupToSingleLoop() {
            ModelDefinition def = new ModelDefinitionBuilder()
                    .name("PP")
                    .stock("Prey", 100, "Animal")
                    .stock("Predators", 20, "Animal")
                    .flow("Predation", "Prey * Predators * 0.01", "Day",
                            "Prey", "Predators")
                    .build();

            FeedbackAnalysis analysis = FeedbackAnalysis.analyze(def);
            assertThat(analysis.loopCount()).isEqualTo(1);

            FeedbackAnalysis filtered = analysis.filterToLoop(0);
            assertThat(filtered.loopCount()).isEqualTo(1);
            // Should include flows as participants for highlighting
            assertThat(filtered.loopParticipants()).contains("Predation");
        }

        @Test
        void shouldReportTwoIndependentGroups() {
            ModelDefinition def = new ModelDefinitionBuilder()
                    .name("Two Groups")
                    .stock("A", 100, "Thing")
                    .stock("B", 50, "Thing")
                    .stock("C", 200, "Thing")
                    .stock("D", 30, "Thing")
                    .flow("F1", "A * B * 0.01", "Day", "A", "B")
                    .flow("F2", "C * D * 0.02", "Day", "C", "D")
                    .build();

            FeedbackAnalysis analysis = FeedbackAnalysis.analyze(def);
            assertThat(analysis.loopCount()).isEqualTo(2);
            assertThat(analysis.causalLoops().get(0).label()).contains("1");
            assertThat(analysis.causalLoops().get(1).label()).contains("2");
        }
    }

    @Nested
    @DisplayName("behavioral narratives")
    class BehavioralNarratives {

        @Test
        void shouldGenerateReinforcingNarrative() {
            // X →(+) Y →(+) X — both positive, reinforcing
            CausalLoop loop = new CausalLoop(
                    List.of("Population", "Births"),
                    List.of(CausalLinkDef.Polarity.POSITIVE, CausalLinkDef.Polarity.POSITIVE),
                    LoopType.REINFORCING, "R1");

            String narrative = FeedbackAnalysis.buildBehavioralNarrative(loop);

            assertThat(narrative).startsWith("As Population rises");
            assertThat(narrative).contains("Births increases");
            assertThat(narrative).contains("further raising Population");
            assertThat(narrative).contains("reinforcing");
        }

        @Test
        void shouldGenerateBalancingNarrative() {
            // X →(+) Y →(-) X — one negative, balancing
            CausalLoop loop = new CausalLoop(
                    List.of("Temperature", "Cooling"),
                    List.of(CausalLinkDef.Polarity.POSITIVE, CausalLinkDef.Polarity.NEGATIVE),
                    LoopType.BALANCING, "B1");

            String narrative = FeedbackAnalysis.buildBehavioralNarrative(loop);

            assertThat(narrative).startsWith("As Temperature rises");
            assertThat(narrative).contains("Cooling increases");
            assertThat(narrative).contains("further lowering Temperature");
            assertThat(narrative).contains("balancing");
        }

        @Test
        void shouldGenerateThreeVariableNarrative() {
            // A →(+) B →(-) C →(-) A — two negatives, reinforcing
            CausalLoop loop = new CausalLoop(
                    List.of("A", "B", "C"),
                    List.of(CausalLinkDef.Polarity.POSITIVE,
                            CausalLinkDef.Polarity.NEGATIVE,
                            CausalLinkDef.Polarity.NEGATIVE),
                    LoopType.REINFORCING, "R1");

            String narrative = FeedbackAnalysis.buildBehavioralNarrative(loop);

            assertThat(narrative).startsWith("As A rises");
            assertThat(narrative).contains("B increases");
            assertThat(narrative).contains("C decreases");
            assertThat(narrative).contains("further raising A");
            assertThat(narrative).contains("reinforcing");
        }

        @Test
        void shouldFallBackToChainForIndeterminate() {
            CausalLoop loop = new CausalLoop(
                    List.of("X", "Y"),
                    List.of(CausalLinkDef.Polarity.UNKNOWN, CausalLinkDef.Polarity.POSITIVE),
                    LoopType.INDETERMINATE, "?1");

            String narrative = FeedbackAnalysis.buildBehavioralNarrative(loop);

            // Should use simple chain format
            assertThat(narrative).contains("X");
            assertThat(narrative).contains("Y");
            assertThat(narrative).doesNotContain("rises");
        }

        @Test
        void shouldStorePolaritiesInCausalLoop() {
            ModelDefinition def = new ModelDefinitionBuilder()
                    .name("CLD")
                    .cldVariable("A")
                    .cldVariable("B")
                    .cldVariable("C")
                    .causalLink("A", "B", CausalLinkDef.Polarity.POSITIVE)
                    .causalLink("B", "C", CausalLinkDef.Polarity.NEGATIVE)
                    .causalLink("C", "A", CausalLinkDef.Polarity.POSITIVE)
                    .build();

            FeedbackAnalysis analysis = FeedbackAnalysis.analyze(def);
            assertThat(analysis.causalLoops()).isNotEmpty();

            CausalLoop loop = analysis.causalLoops().getFirst();
            assertThat(loop.polarities()).hasSize(loop.path().size());
            assertThat(loop.polarities()).doesNotContain(CausalLinkDef.Polarity.UNKNOWN);
        }

        @Test
        void shouldUseBehavioralNarrativeInLoopInfo() {
            ModelDefinition def = new ModelDefinitionBuilder()
                    .name("CLD")
                    .cldVariable("Population")
                    .cldVariable("Births")
                    .causalLink("Population", "Births", CausalLinkDef.Polarity.POSITIVE)
                    .causalLink("Births", "Population", CausalLinkDef.Polarity.POSITIVE)
                    .build();

            FeedbackAnalysis analysis = FeedbackAnalysis.analyze(def);
            Optional<LoopInfo> info = analysis.loopInfo(0);

            assertThat(info).isPresent();
            assertThat(info.get().narrative()).contains("rises");
            assertThat(info.get().narrative()).contains("reinforcing");
        }
    }
}
