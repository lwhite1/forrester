package systems.courant.forrester.model.graph;

import systems.courant.forrester.model.def.ElementPlacement;
import systems.courant.forrester.model.def.ElementType;
import systems.courant.forrester.model.def.ModelDefinition;
import systems.courant.forrester.model.def.ModelDefinitionBuilder;
import systems.courant.forrester.model.def.ViewDef;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("AutoLayout")
class AutoLayoutTest {

    private static Map<String, ElementPlacement> placementMap(ViewDef view) {
        return view.elements().stream()
                .collect(Collectors.toMap(ElementPlacement::name, p -> p));
    }

    // ---------------------------------------------------------------
    // Basic placement and type assignment
    // ---------------------------------------------------------------

    @Test
    void shouldPlaceAllElements() {
        ModelDefinition def = new ModelDefinitionBuilder()
                .name("Full Model")
                .stock("S1", 100, "Thing")
                .stock("S2", 50, "Thing")
                .flow("F1", "S1 * 0.1", "Day", "S1", "S2")
                .aux("A1", "S1 + S2", "Thing")
                .constant("C1", 5, "Thing")
                .build();

        ViewDef view = AutoLayout.layout(def);

        assertThat(view).isNotNull();
        assertThat(view.name()).isEqualTo("Auto Layout");

        Set<String> placedNames = view.elements().stream()
                .map(ElementPlacement::name)
                .collect(Collectors.toSet());

        assertThat(placedNames).containsExactlyInAnyOrder("S1", "S2", "F1", "A1", "C1");
    }

    @Test
    void shouldAssignCorrectTypes() {
        ModelDefinition def = new ModelDefinitionBuilder()
                .name("Types")
                .stock("S", 100, "Thing")
                .flow("F", "S", "Day", "S", null)
                .aux("A", "S * 2", "Thing")
                .constant("C", 1, "Thing")
                .build();

        ViewDef view = AutoLayout.layout(def);
        Map<String, ElementPlacement> map = placementMap(view);

        assertThat(map.get("S").type()).isEqualTo(ElementType.STOCK);
        assertThat(map.get("F").type()).isEqualTo(ElementType.FLOW);
        assertThat(map.get("A").type()).isEqualTo(ElementType.AUX);
        assertThat(map.get("C").type()).isEqualTo(ElementType.CONSTANT);
    }

    @Test
    void shouldHandleEmptyModel() {
        ModelDefinition def = new ModelDefinitionBuilder()
                .name("Empty")
                .build();

        ViewDef view = AutoLayout.layout(def);

        assertThat(view).isNotNull();
        assertThat(view.elements()).isEmpty();
        assertThat(view.connectors()).isEmpty();
    }

    @Test
    void shouldGenerateConnectors() {
        ModelDefinition def = new ModelDefinitionBuilder()
                .name("WithConnectors")
                .stock("S", 100, "Thing")
                .constant("Rate", 0.1, "Dimensionless unit")
                .flow("F", "S * Rate", "Day", "S", null)
                .build();

        ViewDef view = AutoLayout.layout(def);

        assertThat(view.connectors()).isNotEmpty();
    }

    @Test
    void shouldPlaceLookupTables() {
        ModelDefinition def = new ModelDefinitionBuilder()
                .name("WithLookup")
                .stock("S", 100, "Thing")
                .lookupTable("Effect", new double[]{0, 50, 100}, new double[]{0, 0.5, 1}, "LINEAR")
                .build();

        ViewDef view = AutoLayout.layout(def);
        Map<String, ElementPlacement> map = placementMap(view);

        assertThat(map).containsKey("Effect");
        assertThat(map.get("Effect").type()).isEqualTo(ElementType.LOOKUP);
    }

    // ---------------------------------------------------------------
    // No overlapping bounding boxes
    // ---------------------------------------------------------------

    @Test
    void shouldNotOverlapBoundingBoxes() {
        ModelDefinition def = new ModelDefinitionBuilder()
                .name("Overlap Test")
                .stock("S1", 100, "Thing")
                .stock("S2", 50, "Thing")
                .stock("S3", 25, "Thing")
                .constant("C1", 1, "Thing")
                .constant("C2", 2, "Thing")
                .aux("A1", "S1", "Thing")
                .aux("A2", "S2", "Thing")
                .build();

        ViewDef view = AutoLayout.layout(def);
        assertNoOverlaps(view.elements());
    }

    // ---------------------------------------------------------------
    // Topology-driven layout: connected elements placed near each other
    // ---------------------------------------------------------------

    @Test
    void shouldPlaceConnectedElementsNearEachOther() {
        ModelDefinition def = new ModelDefinitionBuilder()
                .name("Topology")
                .stock("S", 100, "Thing")
                .flow("F", "S", "Day", "S", null)
                .aux("A", "S", "Thing")
                .constant("C", 1, "Thing")
                .build();

        ViewDef view = AutoLayout.layout(def);
        assertNoOverlaps(view.elements());
    }

    // ---------------------------------------------------------------
    // Flow positioning relative to stocks
    // ---------------------------------------------------------------

    @Nested
    @DisplayName("source/sink-aware flow positioning")
    class FlowPositioning {

        @Test
        void shouldPlaceInflowLeftOfStock() {
            ModelDefinition def = new ModelDefinitionBuilder()
                    .name("Inflow Test")
                    .stock("Population", 100, "Person")
                    .flow("Births", "Population * 0.04", "Day", null, "Population")
                    .build();

            ViewDef view = AutoLayout.layout(def);
            Map<String, ElementPlacement> map = placementMap(view);

            assertThat(map.get("Births").x())
                    .as("Inflow should be left of its sink stock")
                    .isLessThan(map.get("Population").x());
        }

        @Test
        void shouldPlaceOutflowRightOfStock() {
            ModelDefinition def = new ModelDefinitionBuilder()
                    .name("Outflow Test")
                    .stock("Population", 100, "Person")
                    .flow("Deaths", "Population * 0.03", "Day", "Population", null)
                    .build();

            ViewDef view = AutoLayout.layout(def);
            Map<String, ElementPlacement> map = placementMap(view);

            assertThat(map.get("Deaths").x())
                    .as("Outflow should be right of its source stock")
                    .isGreaterThan(map.get("Population").x());
        }

        @Test
        void shouldPlaceInflowLeftAndOutflowRight() {
            ModelDefinition def = new ModelDefinitionBuilder()
                    .name("Both Flows")
                    .stock("Population", 100, "Person")
                    .flow("Births", "Population * 0.04", "Day", null, "Population")
                    .flow("Deaths", "Population * 0.03", "Day", "Population", null)
                    .build();

            ViewDef view = AutoLayout.layout(def);
            Map<String, ElementPlacement> map = placementMap(view);

            assertThat(map.get("Births").x()).isLessThan(map.get("Population").x());
            assertThat(map.get("Deaths").x()).isGreaterThan(map.get("Population").x());
        }

        @Test
        void shouldPlaceTransferFlowBetweenStocks() {
            ModelDefinition def = new ModelDefinitionBuilder()
                    .name("Transfer Test")
                    .stock("S1", 100, "Thing")
                    .stock("S2", 50, "Thing")
                    .flow("Transfer", "S1 * 0.1", "Day", "S1", "S2")
                    .build();

            ViewDef view = AutoLayout.layout(def);
            Map<String, ElementPlacement> map = placementMap(view);

            assertThat(map.get("Transfer").x())
                    .as("Transfer flow should be between source and sink stocks")
                    .isGreaterThan(map.get("S1").x())
                    .isLessThan(map.get("S2").x());
        }

        @Test
        void shouldLayoutSirChainCorrectly() {
            ModelDefinition def = new ModelDefinitionBuilder()
                    .name("SIR")
                    .stock("Susceptible", 990, "Person")
                    .stock("Infectious", 10, "Person")
                    .stock("Recovered", 0, "Person")
                    .flow("Infection", "Susceptible * 0.3", "Day", "Susceptible", "Infectious")
                    .flow("Recovery", "Infectious * 0.1", "Day", "Infectious", "Recovered")
                    .build();

            ViewDef view = AutoLayout.layout(def);
            Map<String, ElementPlacement> map = placementMap(view);

            double sx = map.get("Susceptible").x();
            double ix = map.get("Infection").x();
            double infx = map.get("Infectious").x();
            double rx = map.get("Recovery").x();
            double recx = map.get("Recovered").x();

            assertThat(sx).isLessThan(ix);
            assertThat(ix).isLessThan(infx);
            assertThat(infx).isLessThan(rx);
            assertThat(rx).isLessThan(recx);
        }

        @Test
        void shouldLayoutParallelChainsCorrectly() {
            ModelDefinition def = new ModelDefinitionBuilder()
                    .name("Predator Prey")
                    .stock("Rabbits", 100, "Animal")
                    .stock("Coyotes", 10, "Animal")
                    .flow("Prey_Births", "Rabbits", "Day", null, "Rabbits")
                    .flow("Prey_Deaths", "Rabbits * Coyotes", "Day", "Rabbits", null)
                    .flow("Pred_Births", "Coyotes", "Day", null, "Coyotes")
                    .flow("Pred_Deaths", "Coyotes", "Day", "Coyotes", null)
                    .build();

            ViewDef view = AutoLayout.layout(def);
            Map<String, ElementPlacement> map = placementMap(view);

            assertThat(map.get("Prey_Births").x()).isLessThan(map.get("Rabbits").x());
            assertThat(map.get("Prey_Deaths").x()).isGreaterThan(map.get("Rabbits").x());
            assertThat(map.get("Pred_Births").x()).isLessThan(map.get("Coyotes").x());
            assertThat(map.get("Pred_Deaths").x()).isGreaterThan(map.get("Coyotes").x());
        }
    }

    // ---------------------------------------------------------------
    // Topology-driven placement for SCC and non-SCC elements
    // ---------------------------------------------------------------

    @Nested
    @DisplayName("topology-driven placement")
    class TopologyPlacement {

        @Test
        void sccMembersShouldBeCloseToEachOther() {
            ModelDefinition def = new ModelDefinitionBuilder()
                    .name("SCC Aux")
                    .stock("Population", 100, "Person")
                    .aux("Growth_Effect", "Population * 0.5", "Dimensionless unit")
                    .flow("Births", "Growth_Effect", "Day", null, "Population")
                    .build();

            ViewDef view = AutoLayout.layout(def);
            assertNoOverlaps(view.elements());

            Map<String, ElementPlacement> map = placementMap(view);
            assertThat(map).containsKeys("Population", "Growth_Effect", "Births");
        }

        @Test
        void feedbackLoopShouldNotCrash() {
            ModelDefinition def = new ModelDefinitionBuilder()
                    .name("Non-SCC Aux")
                    .stock("Population", 100, "Person")
                    .aux("Observer", "Population * 2", "Person")
                    .flow("Births", "10", "Day", null, "Population")
                    .build();

            ViewDef view = AutoLayout.layout(def);
            assertNoOverlaps(view.elements());

            Map<String, ElementPlacement> map = placementMap(view);
            assertThat(map).containsKeys("Population", "Observer", "Births");
        }

        @Test
        void constantShouldBePlaced() {
            ModelDefinition def = new ModelDefinitionBuilder()
                    .name("SCC Constant")
                    .stock("S", 100, "Thing")
                    .constant("Rate", 0.1, "Dimensionless unit")
                    .flow("Outflow", "S * Rate", "Day", "S", null)
                    .build();

            ViewDef view = AutoLayout.layout(def);
            assertNoOverlaps(view.elements());

            Map<String, ElementPlacement> map = placementMap(view);
            assertThat(map).containsKeys("S", "Rate", "Outflow");
        }
    }

    // ---------------------------------------------------------------
    // Cycle handling
    // ---------------------------------------------------------------

    @Test
    @DisplayName("SIR model with feedback loops does not crash")
    void shouldHandleCycles() {
        ModelDefinition def = new ModelDefinitionBuilder()
                .name("SIR with feedback")
                .stock("Susceptible", 1000, "Person")
                .stock("Infectious", 10, "Person")
                .stock("Recovered", 0, "Person")
                .flow("Infection",
                        "Contact_Rate * Infectious / (Susceptible + Infectious + Recovered) * Susceptible",
                        "Day", "Susceptible", "Infectious")
                .flow("Recovery", "Infectious * Recovery_Rate", "Day",
                        "Infectious", "Recovered")
                .constant("Contact_Rate", 8.0, "Dimensionless unit")
                .constant("Recovery_Rate", 0.20, "Dimensionless unit")
                .build();

        ViewDef view = AutoLayout.layout(def);

        assertThat(view.elements()).hasSize(7);
        assertNoOverlaps(view.elements());
    }

    @Test
    @DisplayName("SIR model chain should be ordered Susceptible → Infection → Infectious → Recovery → Recovered")
    void shouldOrderSirChainCorrectly() {
        ModelDefinition def = new ModelDefinitionBuilder()
                .name("SIR Chain Order")
                .stock("Susceptible", 1000, "Person")
                .stock("Infectious", 10, "Person")
                .stock("Recovered", 0, "Person")
                .flow("Infection",
                        "Contact_Rate * Infectious / (Susceptible + Infectious + Recovered) * Infectivity * Susceptible",
                        "Day", "Susceptible", "Infectious")
                .flow("Recovery", "Infectious * Recovery_Rate", "Day",
                        "Infectious", "Recovered")
                .constant("Contact_Rate", 8.0, "Dimensionless unit")
                .constant("Infectivity", 0.10, "Dimensionless unit")
                .constant("Recovery_Rate", 0.20, "Dimensionless unit")
                .build();

        ViewDef view = AutoLayout.layout(def);
        Map<String, ElementPlacement> map = placementMap(view);

        double sX = map.get("Susceptible").x();
        double infectionX = map.get("Infection").x();
        double iX = map.get("Infectious").x();
        double recoveryX = map.get("Recovery").x();
        double rX = map.get("Recovered").x();

        assertThat(sX).as("Susceptible left of Infection").isLessThan(infectionX);
        assertThat(infectionX).as("Infection left of Infectious").isLessThan(iX);
        assertThat(iX).as("Infectious left of Recovery").isLessThan(recoveryX);
        assertThat(recoveryX).as("Recovery left of Recovered").isLessThan(rX);
    }

    // ---------------------------------------------------------------
    // Determinism
    // ---------------------------------------------------------------

    @Test
    @DisplayName("Same input produces same output")
    void shouldBeDeterministic() {
        ModelDefinition def = new ModelDefinitionBuilder()
                .name("Determinism")
                .stock("Population", 100, "Person")
                .flow("Births", "Population * Birth_Rate", "Day", null, "Population")
                .flow("Deaths", "Population * Death_Rate", "Day", "Population", null)
                .constant("Birth_Rate", 0.04, "Dimensionless unit")
                .constant("Death_Rate", 0.03, "Dimensionless unit")
                .build();

        ViewDef view1 = AutoLayout.layout(def);
        ViewDef view2 = AutoLayout.layout(def);

        Map<String, ElementPlacement> map1 = placementMap(view1);
        Map<String, ElementPlacement> map2 = placementMap(view2);

        for (String name : map1.keySet()) {
            assertThat(map2.get(name).x()).as("x of '%s'", name).isEqualTo(map1.get(name).x());
            assertThat(map2.get(name).y()).as("y of '%s'", name).isEqualTo(map1.get(name).y());
        }
    }

    // ---------------------------------------------------------------
    // Complex model: inventory oscillation (9 elements)
    // ---------------------------------------------------------------

    @Test
    @DisplayName("Inventory oscillation model with 9+ elements lays out correctly")
    void shouldLayoutComplexModel() {
        ModelDefinition def = new ModelDefinitionBuilder()
                .name("Inventory Oscillation")
                .stock("Cars_on_Lot", 200, "Car")
                .stock("Perceived_Sales", 20, "Car")
                .aux("Customer_Demand", "IF(TIME > 25, Step_Demand, Base_Demand)", "Car")
                .aux("Desired_Inventory", "Perceived_Sales * Desired_Inventory_Multiplier", "Car")
                .aux("Inventory_Gap", "Desired_Inventory - Cars_on_Lot", "Car")
                .aux("Orders_to_Factory",
                        "MAX(Perceived_Sales + Inventory_Gap / Response_Delay, 0)", "Car")
                .flow("Sales", "MIN(Cars_on_Lot, Customer_Demand)", "Day",
                        "Cars_on_Lot", null)
                .flow("Perception_Adjustment",
                        "(Sales - Perceived_Sales) / Perception_Delay", "Day",
                        null, "Perceived_Sales")
                .flow("Deliveries", "DELAY3(Orders_to_Factory, Delivery_Delay)", "Day",
                        null, "Cars_on_Lot")
                .constant("Base_Demand", 20, "Car per Day")
                .constant("Step_Demand", 22, "Car per Day")
                .constant("Perception_Delay", 5, "Day")
                .constant("Response_Delay", 3, "Day")
                .constant("Delivery_Delay", 5, "Day")
                .constant("Desired_Inventory_Multiplier", 10, "Dimensionless unit")
                .build();

        ViewDef view = AutoLayout.layout(def);

        assertThat(view.elements()).hasSize(15);
        assertNoOverlaps(view.elements());
    }

    // ---------------------------------------------------------------
    // CLD variable and causal link layout
    // ---------------------------------------------------------------

    @Nested
    @DisplayName("CLD layout")
    class CldLayout {

        @Test
        void shouldPlaceCldVariables() {
            ModelDefinition def = new ModelDefinitionBuilder()
                    .name("CLD")
                    .cldVariable("Population")
                    .cldVariable("Birth Rate")
                    .causalLink("Population", "Birth Rate",
                            systems.courant.forrester.model.def.CausalLinkDef.Polarity.POSITIVE)
                    .causalLink("Birth Rate", "Population",
                            systems.courant.forrester.model.def.CausalLinkDef.Polarity.POSITIVE)
                    .build();

            ViewDef view = AutoLayout.layout(def);

            Set<String> placedNames = view.elements().stream()
                    .map(ElementPlacement::name)
                    .collect(Collectors.toSet());

            assertThat(placedNames).containsExactlyInAnyOrder("Population", "Birth Rate");
        }

        @Test
        void shouldAssignCldVariableType() {
            ModelDefinition def = new ModelDefinitionBuilder()
                    .name("CLD Type")
                    .cldVariable("X")
                    .cldVariable("Y")
                    .causalLink("X", "Y",
                            systems.courant.forrester.model.def.CausalLinkDef.Polarity.POSITIVE)
                    .build();

            ViewDef view = AutoLayout.layout(def);
            Map<String, ElementPlacement> map = placementMap(view);

            assertThat(map.get("X").type()).isEqualTo(ElementType.CLD_VARIABLE);
            assertThat(map.get("Y").type()).isEqualTo(ElementType.CLD_VARIABLE);
        }

        @Test
        void shouldNotOverlapCldVariables() {
            ModelDefinition def = new ModelDefinitionBuilder()
                    .name("CLD No Overlap")
                    .cldVariable("A")
                    .cldVariable("B")
                    .cldVariable("C")
                    .cldVariable("D")
                    .causalLink("A", "B",
                            systems.courant.forrester.model.def.CausalLinkDef.Polarity.POSITIVE)
                    .causalLink("B", "C",
                            systems.courant.forrester.model.def.CausalLinkDef.Polarity.NEGATIVE)
                    .causalLink("C", "D",
                            systems.courant.forrester.model.def.CausalLinkDef.Polarity.POSITIVE)
                    .causalLink("D", "A",
                            systems.courant.forrester.model.def.CausalLinkDef.Polarity.NEGATIVE)
                    .build();

            ViewDef view = AutoLayout.layout(def);
            assertNoOverlaps(view.elements());
        }

        @Test
        void shouldHandleMixedSfAndCldElements() {
            ModelDefinition def = new ModelDefinitionBuilder()
                    .name("Mixed")
                    .stock("S", 100, "Thing")
                    .flow("F", "S * 0.1", "Day", "S", null)
                    .cldVariable("Market Pressure")
                    .cldVariable("Demand")
                    .causalLink("Market Pressure", "Demand",
                            systems.courant.forrester.model.def.CausalLinkDef.Polarity.POSITIVE)
                    .build();

            ViewDef view = AutoLayout.layout(def);
            Map<String, ElementPlacement> map = placementMap(view);

            assertThat(map).containsKeys("S", "F", "Market Pressure", "Demand");
            assertNoOverlaps(view.elements());
        }

        @Test
        void shouldHandleCldOnlyModel() {
            ModelDefinition def = new ModelDefinitionBuilder()
                    .name("CLD Only")
                    .cldVariable("X")
                    .build();

            ViewDef view = AutoLayout.layout(def);

            assertThat(view.elements()).hasSize(1);
            assertThat(view.elements().getFirst().type()).isEqualTo(ElementType.CLD_VARIABLE);
        }
    }

    // ---------------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------------

    /**
     * Asserts that no two element bounding boxes overlap,
     * using actual element sizes from {@link ElementSizes}.
     */
    private static void assertNoOverlaps(List<ElementPlacement> elements) {
        for (int i = 0; i < elements.size(); i++) {
            ElementPlacement a = elements.get(i);
            ElementSizes sa = ElementSizes.forType(a.type());
            double aLeft = a.x() - sa.width() / 2.0;
            double aRight = a.x() + sa.width() / 2.0;
            double aTop = a.y() - sa.height() / 2.0;
            double aBottom = a.y() + sa.height() / 2.0;

            for (int j = i + 1; j < elements.size(); j++) {
                ElementPlacement b = elements.get(j);
                ElementSizes sb = ElementSizes.forType(b.type());
                double bLeft = b.x() - sb.width() / 2.0;
                double bRight = b.x() + sb.width() / 2.0;
                double bTop = b.y() - sb.height() / 2.0;
                double bBottom = b.y() + sb.height() / 2.0;

                boolean overlaps = aLeft < bRight && aRight > bLeft
                        && aTop < bBottom && aBottom > bTop;

                assertThat(overlaps)
                        .as("Elements '%s' and '%s' should not overlap", a.name(), b.name())
                        .isFalse();
            }
        }
    }
}
