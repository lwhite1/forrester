package systems.courant.sd.model.graph;

import systems.courant.sd.model.def.CausalLinkDef;
import systems.courant.sd.model.def.CldVariableDef;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class CldLoopInfoTest {

    @Test
    void shouldReturnEmptyForNoVariables() {
        CldLoopInfo info = CldLoopInfo.compute(List.of(), List.of());
        assertThat(info.isEmpty()).isTrue();
        assertThat(info).isSameAs(CldLoopInfo.EMPTY);
    }

    @Test
    void shouldReturnEmptyForDag() {
        // A → B → C (no cycles)
        List<CldVariableDef> vars = List.of(
                new CldVariableDef("A"),
                new CldVariableDef("B"),
                new CldVariableDef("C"));
        List<CausalLinkDef> links = List.of(
                new CausalLinkDef("A", "B"),
                new CausalLinkDef("B", "C"));

        CldLoopInfo info = CldLoopInfo.compute(vars, links);
        assertThat(info.isEmpty()).isTrue();
    }

    @Test
    void shouldDetectSingleLoop() {
        // A → B → C → A
        List<CldVariableDef> vars = List.of(
                new CldVariableDef("A"),
                new CldVariableDef("B"),
                new CldVariableDef("C"));
        List<CausalLinkDef> links = List.of(
                new CausalLinkDef("A", "B"),
                new CausalLinkDef("B", "C"),
                new CausalLinkDef("C", "A"));

        CldLoopInfo info = CldLoopInfo.compute(vars, links);
        assertThat(info.isEmpty()).isFalse();
        assertThat(info.loops()).hasSize(1);
        assertThat(info.loops().getFirst()).containsExactlyInAnyOrder("A", "B", "C");
    }

    @Nested
    class InSameLoop {
        @Test
        void shouldReturnTrueForNodesInSameLoop() {
            List<CldVariableDef> vars = List.of(
                    new CldVariableDef("A"),
                    new CldVariableDef("B"),
                    new CldVariableDef("C"));
            List<CausalLinkDef> links = List.of(
                    new CausalLinkDef("A", "B"),
                    new CausalLinkDef("B", "C"),
                    new CausalLinkDef("C", "A"));

            CldLoopInfo info = CldLoopInfo.compute(vars, links);
            assertThat(info.inSameLoop("A", "B")).isTrue();
            assertThat(info.inSameLoop("B", "C")).isTrue();
            assertThat(info.inSameLoop("A", "C")).isTrue();
        }

        @Test
        void shouldReturnFalseForNodesInDifferentLoops() {
            // Loop1: A → B → A, Loop2: C → D → C
            List<CldVariableDef> vars = List.of(
                    new CldVariableDef("A"),
                    new CldVariableDef("B"),
                    new CldVariableDef("C"),
                    new CldVariableDef("D"));
            List<CausalLinkDef> links = List.of(
                    new CausalLinkDef("A", "B"),
                    new CausalLinkDef("B", "A"),
                    new CausalLinkDef("C", "D"),
                    new CausalLinkDef("D", "C"));

            CldLoopInfo info = CldLoopInfo.compute(vars, links);
            assertThat(info.inSameLoop("A", "C")).isFalse();
            assertThat(info.inSameLoop("B", "D")).isFalse();
        }

        @Test
        void shouldReturnFalseForNodeNotInAnyLoop() {
            List<CldVariableDef> vars = List.of(
                    new CldVariableDef("A"),
                    new CldVariableDef("B"),
                    new CldVariableDef("X"));
            List<CausalLinkDef> links = List.of(
                    new CausalLinkDef("A", "B"),
                    new CausalLinkDef("B", "A"),
                    new CausalLinkDef("A", "X"));

            CldLoopInfo info = CldLoopInfo.compute(vars, links);
            assertThat(info.inSameLoop("A", "X")).isFalse();
        }
    }

    @Nested
    class SharedNode {
        @Test
        void shouldNotBeSharedWhenAllInOneSCC() {
            // A → B → A and B → C → B merges into one SCC {A, B, C}
            List<CldVariableDef> vars = List.of(
                    new CldVariableDef("A"),
                    new CldVariableDef("B"),
                    new CldVariableDef("C"));
            List<CausalLinkDef> links = List.of(
                    new CausalLinkDef("A", "B"),
                    new CausalLinkDef("B", "A"),
                    new CausalLinkDef("B", "C"),
                    new CausalLinkDef("C", "B"));

            CldLoopInfo info = CldLoopInfo.compute(vars, links);
            // SCCs are disjoint — all three form one SCC
            assertThat(info.loops()).hasSize(1);
            assertThat(info.isSharedNode("A")).isFalse();
            assertThat(info.isSharedNode("B")).isFalse();
            assertThat(info.isSharedNode("C")).isFalse();
        }

        @Test
        void shouldReturnFalseForNodeNotInAnyLoop() {
            CldLoopInfo info = CldLoopInfo.EMPTY;
            assertThat(info.isSharedNode("X")).isFalse();
        }
    }

    @Nested
    class LoopsOf {
        @Test
        void shouldReturnEmptyForNonLoopNode() {
            CldLoopInfo info = CldLoopInfo.EMPTY;
            assertThat(info.loopsOf("X")).isEmpty();
        }

        @Test
        void shouldReturnSingleLoopIndex() {
            List<CldVariableDef> vars = List.of(
                    new CldVariableDef("A"),
                    new CldVariableDef("B"));
            List<CausalLinkDef> links = List.of(
                    new CausalLinkDef("A", "B"),
                    new CausalLinkDef("B", "A"));

            CldLoopInfo info = CldLoopInfo.compute(vars, links);
            assertThat(info.loopsOf("A")).hasSize(1);
        }

        @Test
        void shouldReturnOneLoopForNodeInMergedSCC() {
            // A→B→A and B→C→B merge into one SCC
            List<CldVariableDef> vars = List.of(
                    new CldVariableDef("A"),
                    new CldVariableDef("B"),
                    new CldVariableDef("C"));
            List<CausalLinkDef> links = List.of(
                    new CausalLinkDef("A", "B"),
                    new CausalLinkDef("B", "A"),
                    new CausalLinkDef("B", "C"),
                    new CausalLinkDef("C", "B"));

            CldLoopInfo info = CldLoopInfo.compute(vars, links);
            // All three are in one SCC
            assertThat(info.loopsOf("B")).hasSize(1);
            assertThat(info.loopsOf("A")).hasSize(1);
        }
    }
}
