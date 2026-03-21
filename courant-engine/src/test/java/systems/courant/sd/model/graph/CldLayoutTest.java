package systems.courant.sd.model.graph;

import systems.courant.sd.model.def.CausalLinkDef;
import systems.courant.sd.model.def.CldVariableDef;
import systems.courant.sd.model.def.ElementPlacement;
import systems.courant.sd.model.def.ElementType;
import systems.courant.sd.model.def.ModelDefinition;
import systems.courant.sd.model.def.ModelDefinitionBuilder;
import systems.courant.sd.model.def.ViewDef;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class CldLayoutTest {

    @Test
    void shouldPlaceAllCldVariables() {
        ModelDefinition def = new ModelDefinitionBuilder()
                .name("test")
                .cldVariable(new CldVariableDef("A"))
                .cldVariable(new CldVariableDef("B"))
                .cldVariable(new CldVariableDef("C"))
                .causalLink(new CausalLinkDef("A", "B"))
                .causalLink(new CausalLinkDef("B", "C"))
                .causalLink(new CausalLinkDef("C", "A"))
                .build();

        ViewDef view = CldLayout.layout(def);

        assertThat(view.elements()).hasSize(3);
        Set<String> names = new HashSet<>();
        for (ElementPlacement ep : view.elements()) {
            names.add(ep.name());
            assertThat(ep.type()).isEqualTo(ElementType.CLD_VARIABLE);
        }
        assertThat(names).containsExactlyInAnyOrder("A", "B", "C");
    }

    @Test
    void shouldCenterOnCanvas() {
        ModelDefinition def = new ModelDefinitionBuilder()
                .name("test")
                .cldVariable(new CldVariableDef("X"))
                .cldVariable(new CldVariableDef("Y"))
                .causalLink(new CausalLinkDef("X", "Y"))
                .build();

        ViewDef view = CldLayout.layout(def);

        double avgX = 0, avgY = 0;
        for (ElementPlacement ep : view.elements()) {
            avgX += ep.x();
            avgY += ep.y();
        }
        avgX /= view.elements().size();
        avgY /= view.elements().size();

        assertThat(avgX).isCloseTo(600, org.assertj.core.data.Offset.offset(1.0));
        assertThat(avgY).isCloseTo(400, org.assertj.core.data.Offset.offset(1.0));
    }

    @Test
    void shouldHandleSingleNode() {
        ModelDefinition def = new ModelDefinitionBuilder()
                .name("test")
                .cldVariable(new CldVariableDef("lone"))
                .build();

        ViewDef view = CldLayout.layout(def);

        assertThat(view.elements()).hasSize(1);
        assertThat(view.elements().getFirst().name()).isEqualTo("lone");
        assertThat(view.elements().getFirst().x()).isEqualTo(600);
        assertThat(view.elements().getFirst().y()).isEqualTo(400);
    }

    @Test
    void shouldHandleEmptyModel() {
        ModelDefinition def = new ModelDefinitionBuilder()
                .name("test")
                .build();

        ViewDef view = CldLayout.layout(def);

        assertThat(view.elements()).isEmpty();
    }

    @Test
    void shouldHandleDisconnectedNodes() {
        ModelDefinition def = new ModelDefinitionBuilder()
                .name("test")
                .cldVariable(new CldVariableDef("A"))
                .cldVariable(new CldVariableDef("B"))
                .cldVariable(new CldVariableDef("C"))
                .build();

        ViewDef view = CldLayout.layout(def);

        assertThat(view.elements()).hasSize(3);
    }

    @Test
    void shouldBeDeterministic() {
        ModelDefinition def = new ModelDefinitionBuilder()
                .name("test")
                .cldVariable(new CldVariableDef("A"))
                .cldVariable(new CldVariableDef("B"))
                .cldVariable(new CldVariableDef("C"))
                .causalLink(new CausalLinkDef("A", "B"))
                .causalLink(new CausalLinkDef("B", "C"))
                .causalLink(new CausalLinkDef("C", "A"))
                .build();

        ViewDef view1 = CldLayout.layout(def);
        ViewDef view2 = CldLayout.layout(def);

        for (int i = 0; i < view1.elements().size(); i++) {
            ElementPlacement p1 = view1.elements().get(i);
            ElementPlacement p2 = view2.elements().get(i);
            assertThat(p1.x()).isEqualTo(p2.x());
            assertThat(p1.y()).isEqualTo(p2.y());
        }
    }

    @Nested
    class MinimumDistance {
        @Test
        void shouldEnforceMinimumInterNodeDistance() {
            ModelDefinition def = new ModelDefinitionBuilder()
                    .name("test")
                    .cldVariable(new CldVariableDef("A"))
                    .cldVariable(new CldVariableDef("B"))
                    .cldVariable(new CldVariableDef("C"))
                    .cldVariable(new CldVariableDef("D"))
                    .causalLink(new CausalLinkDef("A", "B"))
                    .causalLink(new CausalLinkDef("B", "C"))
                    .causalLink(new CausalLinkDef("C", "D"))
                    .build();

            ViewDef view = CldLayout.layout(def);

            // Verify no two nodes are closer than some reasonable minimum
            List<ElementPlacement> elements = view.elements();
            for (int i = 0; i < elements.size(); i++) {
                for (int j = i + 1; j < elements.size(); j++) {
                    double dx = elements.get(i).x() - elements.get(j).x();
                    double dy = elements.get(i).y() - elements.get(j).y();
                    double dist = Math.sqrt(dx * dx + dy * dy);
                    assertThat(dist).isGreaterThan(40);
                }
            }
        }
    }

    @Nested
    class DagLayout {
        @Test
        void shouldLayOutDagLeftToRight() {
            // A → B → C (no cycles)
            Set<String> nodes = new LinkedHashSet<>(List.of("A", "B", "C"));
            Map<String, Set<String>> adj = new LinkedHashMap<>();
            adj.put("A", new LinkedHashSet<>(List.of("B")));
            adj.put("B", new LinkedHashSet<>(List.of("C")));
            adj.put("C", new LinkedHashSet<>());

            Map<String, double[]> positions = CldLayout.dagLayout(nodes, adj);

            // A should be leftmost, C should be rightmost
            assertThat(positions.get("A")[0]).isLessThan(positions.get("B")[0]);
            assertThat(positions.get("B")[0]).isLessThan(positions.get("C")[0]);
        }
    }

    @Nested
    class ForceDirectedLayout {
        @Test
        void shouldPlaceAllNodesForCyclicGraph() {
            // A → B → C → A
            Set<String> nodes = new LinkedHashSet<>(List.of("A", "B", "C"));
            Map<String, Set<String>> adj = new LinkedHashMap<>();
            adj.put("A", new LinkedHashSet<>(List.of("B")));
            adj.put("B", new LinkedHashSet<>(List.of("C")));
            adj.put("C", new LinkedHashSet<>(List.of("A")));

            Map<String, double[]> positions = CldLayout.forceDirectedLayout(nodes, adj);

            assertThat(positions).hasSize(3);
            assertThat(positions).containsKeys("A", "B", "C");
        }
    }

    @Nested
    class LoopAwareLayout {
        @Test
        void shouldPlaceNodesFromMultipleSCCsNearDifferentCentroids() {
            // Loop1: A → B → A, Loop2: C → D → C, cross-link: A → C
            ModelDefinition def = new ModelDefinitionBuilder()
                    .name("test")
                    .cldVariable(new CldVariableDef("A"))
                    .cldVariable(new CldVariableDef("B"))
                    .cldVariable(new CldVariableDef("C"))
                    .cldVariable(new CldVariableDef("D"))
                    .causalLink(new CausalLinkDef("A", "B"))
                    .causalLink(new CausalLinkDef("B", "A"))
                    .causalLink(new CausalLinkDef("C", "D"))
                    .causalLink(new CausalLinkDef("D", "C"))
                    .causalLink(new CausalLinkDef("A", "C"))
                    .build();

            ViewDef view = CldLayout.layout(def);

            assertThat(view.elements()).hasSize(4);

            // Nodes in loop1 (A, B) should be closer to each other than to loop2 (C, D)
            Map<String, double[]> positions = new LinkedHashMap<>();
            for (ElementPlacement ep : view.elements()) {
                positions.put(ep.name(), new double[]{ep.x(), ep.y()});
            }

            double distAB = dist(positions.get("A"), positions.get("B"));
            double distCD = dist(positions.get("C"), positions.get("D"));
            double distAC = dist(positions.get("A"), positions.get("C"));

            // Intra-loop distances should be less than cross-loop distance
            assertThat(distAB).isLessThan(distAC);
            assertThat(distCD).isLessThan(distAC);
        }

        @Test
        void shouldPlaceSharedNodeBetweenLoopCenters() {
            // Loop1: A → B → A, Loop2: B → C → B
            // B is shared between both loops
            ModelDefinition def = new ModelDefinitionBuilder()
                    .name("test")
                    .cldVariable(new CldVariableDef("A"))
                    .cldVariable(new CldVariableDef("B"))
                    .cldVariable(new CldVariableDef("C"))
                    .causalLink(new CausalLinkDef("A", "B"))
                    .causalLink(new CausalLinkDef("B", "A"))
                    .causalLink(new CausalLinkDef("B", "C"))
                    .causalLink(new CausalLinkDef("C", "B"))
                    .build();

            ViewDef view = CldLayout.layout(def);

            assertThat(view.elements()).hasSize(3);
        }

        @Test
        void shouldHandleSingletonNodesOutsideLoops() {
            // Loop: A → B → A, singleton: X (with link A → X)
            ModelDefinition def = new ModelDefinitionBuilder()
                    .name("test")
                    .cldVariable(new CldVariableDef("A"))
                    .cldVariable(new CldVariableDef("B"))
                    .cldVariable(new CldVariableDef("X"))
                    .causalLink(new CausalLinkDef("A", "B"))
                    .causalLink(new CausalLinkDef("B", "A"))
                    .causalLink(new CausalLinkDef("A", "X"))
                    .build();

            ViewDef view = CldLayout.layout(def);

            assertThat(view.elements()).hasSize(3);
            Set<String> names = new HashSet<>();
            for (ElementPlacement ep : view.elements()) {
                names.add(ep.name());
            }
            assertThat(names).containsExactlyInAnyOrder("A", "B", "X");
        }

        private double dist(double[] a, double[] b) {
            double dx = a[0] - b[0];
            double dy = a[1] - b[1];
            return Math.sqrt(dx * dx + dy * dy);
        }
    }
}
