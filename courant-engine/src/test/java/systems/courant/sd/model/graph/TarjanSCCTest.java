package systems.courant.sd.model.graph;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("TarjanSCC")
class TarjanSCCTest {

    @Test
    void shouldFindSingleCycle() {
        Set<String> nodes = new LinkedHashSet<>(List.of("A", "B"));
        Map<String, Set<String>> graph = new LinkedHashMap<>();
        graph.put("A", Set.of("B"));
        graph.put("B", Set.of("A"));

        List<Set<String>> sccs = TarjanSCC.findNonTrivial(nodes, graph);
        assertThat(sccs).hasSize(1);
        assertThat(sccs.get(0)).containsExactlyInAnyOrder("A", "B");
    }

    @Test
    void shouldReturnEmptyForAcyclicGraph() {
        Set<String> nodes = new LinkedHashSet<>(List.of("A", "B", "C"));
        Map<String, Set<String>> graph = new LinkedHashMap<>();
        graph.put("A", Set.of("B"));
        graph.put("B", Set.of("C"));

        List<Set<String>> sccs = TarjanSCC.findNonTrivial(nodes, graph);
        assertThat(sccs).isEmpty();
    }

    @Test
    void shouldFindMultipleSCCs() {
        Set<String> nodes = new LinkedHashSet<>(List.of("A", "B", "C", "D"));
        Map<String, Set<String>> graph = new LinkedHashMap<>();
        graph.put("A", Set.of("B"));
        graph.put("B", Set.of("A"));
        graph.put("C", Set.of("D"));
        graph.put("D", Set.of("C"));

        List<Set<String>> sccs = TarjanSCC.findNonTrivial(nodes, graph);
        assertThat(sccs).hasSize(2);
    }

    @Test
    void shouldFindAllIncludingTrivial() {
        Set<String> nodes = new LinkedHashSet<>(List.of("A", "B", "C"));
        Map<String, Set<String>> graph = new LinkedHashMap<>();
        graph.put("A", Set.of("B"));
        graph.put("B", Set.of("A"));

        List<Set<String>> all = TarjanSCC.findAll(nodes, graph);
        // Two trivial-or-not SCCs: {A,B} and {C}
        assertThat(all).hasSize(2);
    }

    @Test
    void shouldHandleEmptyGraph() {
        List<Set<String>> sccs = TarjanSCC.findNonTrivial(Set.of(), Map.of());
        assertThat(sccs).isEmpty();
    }

    @Test
    void shouldHandleThreeNodeCycle() {
        Set<String> nodes = new LinkedHashSet<>(List.of("X", "Y", "Z"));
        Map<String, Set<String>> graph = new LinkedHashMap<>();
        graph.put("X", Set.of("Y"));
        graph.put("Y", Set.of("Z"));
        graph.put("Z", Set.of("X"));

        List<Set<String>> sccs = TarjanSCC.findNonTrivial(nodes, graph);
        assertThat(sccs).hasSize(1);
        assertThat(sccs.get(0)).containsExactlyInAnyOrder("X", "Y", "Z");
    }
}
