package com.deathrayresearch.forrester.model.graph;

import com.deathrayresearch.forrester.model.def.ElementPlacement;
import com.deathrayresearch.forrester.model.def.ElementType;
import com.deathrayresearch.forrester.model.def.ModelDefinition;
import com.deathrayresearch.forrester.model.def.ModelDefinitionBuilder;
import com.deathrayresearch.forrester.model.def.ViewDef;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("AutoLayout")
class AutoLayoutTest {

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

        assertThat(placedNames.contains("S1")).as("S1 should be placed").isTrue();
        assertThat(placedNames.contains("S2")).as("S2 should be placed").isTrue();
        assertThat(placedNames.contains("F1")).as("F1 should be placed").isTrue();
        assertThat(placedNames.contains("A1")).as("A1 should be placed").isTrue();
        assertThat(placedNames.contains("C1")).as("C1 should be placed").isTrue();
        assertThat(view.elements().size()).isEqualTo(5);
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

        for (ElementPlacement p : view.elements()) {
            if (p.name().equals("S")) {
                assertThat(p.type()).isEqualTo(ElementType.STOCK);
            } else if (p.name().equals("F")) {
                assertThat(p.type()).isEqualTo(ElementType.FLOW);
            } else if (p.name().equals("A")) {
                assertThat(p.type()).isEqualTo(ElementType.AUX);
            } else if (p.name().equals("C")) {
                assertThat(p.type()).isEqualTo(ElementType.CONSTANT);
            }
        }
    }

    @Test
    void shouldNotOverlapElements() {
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

        List<ElementPlacement> elements = view.elements();
        // Check that no two elements share the same (x, y) position
        Set<String> positions = new HashSet<>();
        for (ElementPlacement p : elements) {
            String pos = p.x() + "," + p.y();
            assertThat(positions.add(pos))
                    .as("Elements should not overlap: duplicate position " + pos + " for " + p.name()).isTrue();
        }
    }

    @Test
    void shouldLayerElementsByType() {
        ModelDefinition def = new ModelDefinitionBuilder()
                .name("Layers")
                .stock("S", 100, "Thing")
                .flow("F", "S", "Day", "S", null)
                .aux("A", "S", "Thing")
                .constant("C", 1, "Thing")
                .build();

        ViewDef view = AutoLayout.layout(def);

        double stockY = -1, flowY = -1, auxY = -1, constantY = -1;
        for (ElementPlacement p : view.elements()) {
            if (p.name().equals("S")) {
                stockY = p.y();
            } else if (p.name().equals("F")) {
                flowY = p.y();
            } else if (p.name().equals("A")) {
                auxY = p.y();
            } else if (p.name().equals("C")) {
                constantY = p.y();
            }
        }

        // Auxiliaries should be above stocks, constants below
        assertThat(auxY < stockY).as("Auxiliaries should be above stocks (lower y)").isTrue();
        assertThat(constantY > stockY).as("Constants should be below stocks (higher y)").isTrue();
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

        assertThat(view.connectors().isEmpty()).as("Should generate connectors from dependencies").isFalse();
    }

    @Test
    void shouldHandleEmptyModel() {
        ModelDefinition def = new ModelDefinitionBuilder()
                .name("Empty")
                .build();

        ViewDef view = AutoLayout.layout(def);

        assertThat(view).isNotNull();
        assertThat(view.elements().isEmpty()).isTrue();
        assertThat(view.connectors().isEmpty()).isTrue();
    }

    @Nested
    @DisplayName("source/sink-aware flow positioning")
    class FlowPositioning {

        private Map<String, ElementPlacement> placementMap(ViewDef view) {
            return view.elements().stream()
                    .collect(Collectors.toMap(ElementPlacement::name, p -> p));
        }

        @Test
        void shouldPlaceInflowLeftOfStock() {
            // Births (inflow) → Population
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
            // Population → Deaths (outflow)
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
            // Births → Population → Deaths
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
            // S1 → Transfer → S2
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
            // Susceptible → Infection → Infectious → Recovery → Recovered
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
            // Prey_Births → Rabbits → Prey_Deaths, Pred_Births → Coyotes → Pred_Deaths
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

            // First chain
            assertThat(map.get("Prey_Births").x()).isLessThan(map.get("Rabbits").x());
            assertThat(map.get("Prey_Deaths").x()).isGreaterThan(map.get("Rabbits").x());

            // Second chain
            assertThat(map.get("Pred_Births").x()).isLessThan(map.get("Coyotes").x());
            assertThat(map.get("Pred_Deaths").x()).isGreaterThan(map.get("Coyotes").x());
        }
    }

    @Test
    void shouldPlaceLookupTables() {
        ModelDefinition def = new ModelDefinitionBuilder()
                .name("WithLookup")
                .stock("S", 100, "Thing")
                .lookupTable("Effect", new double[]{0, 50, 100}, new double[]{0, 0.5, 1}, "LINEAR")
                .build();

        ViewDef view = AutoLayout.layout(def);

        Set<String> placedNames = view.elements().stream()
                .map(ElementPlacement::name)
                .collect(Collectors.toSet());

        assertThat(placedNames.contains("Effect")).as("Lookup table should be placed").isTrue();

        ElementPlacement lookupPlacement = view.elements().stream()
                .filter(p -> p.name().equals("Effect"))
                .findFirst().orElseThrow();
        assertThat(lookupPlacement.type()).isEqualTo(ElementType.LOOKUP);
    }

    /**
     * Tests that verify exact (x, y) coordinates for all elements.
     * Layout constants: X_START=100, X_SPACING=150, Y_FLOW=200, Y_STOCK=200,
     * Y_AUX=50, Y_CONSTANT=350.
     */
    @Nested
    @DisplayName("exact coordinate verification")
    class ExactCoordinates {

        private Map<String, ElementPlacement> placementMap(ViewDef view) {
            return view.elements().stream()
                    .collect(Collectors.toMap(ElementPlacement::name, p -> p));
        }

        private void assertPosition(Map<String, ElementPlacement> map,
                                     String name, double expectedX, double expectedY) {
            ElementPlacement p = map.get(name);
            assertThat(p).as("Element '%s' should be placed", name).isNotNull();
            assertThat(p.x()).as("x of '%s'", name).isEqualTo(expectedX);
            assertThat(p.y()).as("y of '%s'", name).isEqualTo(expectedY);
        }

        @Test
        @DisplayName("Exponential Growth: Births(100) → Population(250) → Deaths(400)")
        void shouldLayoutExponentialGrowthWithExactPositions() {
            // Births → Population → Deaths, with Birth_Rate and Death_Rate
            ModelDefinition def = new ModelDefinitionBuilder()
                    .name("Exponential Growth")
                    .stock("Population", 100, "Person")
                    .flow("Births", "Population * Birth_Rate", "Day", null, "Population")
                    .flow("Deaths", "Population * Death_Rate", "Day", "Population", null)
                    .constant("Birth_Rate", 0.04, "Dimensionless unit")
                    .constant("Death_Rate", 0.03, "Dimensionless unit")
                    .build();

            ViewDef view = AutoLayout.layout(def);
            Map<String, ElementPlacement> map = placementMap(view);

            // Flows and stock at y=200
            assertPosition(map, "Births", 100, 200);
            assertPosition(map, "Population", 250, 200);
            assertPosition(map, "Deaths", 400, 200);

            // Constants at y=350, positioned below the flow they feed
            assertPosition(map, "Birth_Rate", 100, 350);
            assertPosition(map, "Death_Rate", 400, 350);
        }

        @Test
        @DisplayName("Bathtub: Inflow(100) → Water(250) → Outflow(400)")
        void shouldLayoutBathtubWithExactPositions() {
            ModelDefinition def = new ModelDefinitionBuilder()
                    .name("Bathtub")
                    .stock("Water_in_Tub", 50, "Gallon")
                    .flow("Inflow", "STEP(Inflow_Rate, 5)", "Minute", null, "Water_in_Tub")
                    .flow("Outflow", "MIN(Outflow_Rate, Water_in_Tub)", "Minute", "Water_in_Tub", null)
                    .constant("Outflow_Rate", 5, "Gallon per Minute")
                    .constant("Inflow_Rate", 5, "Gallon per Minute")
                    .build();

            ViewDef view = AutoLayout.layout(def);
            Map<String, ElementPlacement> map = placementMap(view);

            // Flows and stock at y=200
            assertPosition(map, "Inflow", 100, 200);
            assertPosition(map, "Water_in_Tub", 250, 200);
            assertPosition(map, "Outflow", 400, 200);

            // Constants below their associated flows
            assertPosition(map, "Outflow_Rate", 400, 350);
            assertPosition(map, "Inflow_Rate", 100, 350);
        }

        @Test
        @DisplayName("SIR chain: S(100) → Inf(250) → I(400) → Rec(550) → R(700)")
        void shouldLayoutSirWithExactPositions() {
            ModelDefinition def = new ModelDefinitionBuilder()
                    .name("SIR")
                    .stock("Susceptible", 990, "Person")
                    .stock("Infectious", 10, "Person")
                    .stock("Recovered", 0, "Person")
                    .flow("Infection", "Susceptible * Contact_Rate", "Day",
                            "Susceptible", "Infectious")
                    .flow("Recovery", "Infectious * Recovery_Rate", "Day",
                            "Infectious", "Recovered")
                    .constant("Contact_Rate", 0.3, "Dimensionless unit")
                    .constant("Recovery_Rate", 0.1, "Dimensionless unit")
                    .build();

            ViewDef view = AutoLayout.layout(def);
            Map<String, ElementPlacement> map = placementMap(view);

            // Chain: Susceptible → Infection → Infectious → Recovery → Recovered
            assertPosition(map, "Susceptible", 100, 200);
            assertPosition(map, "Infection", 250, 200);
            assertPosition(map, "Infectious", 400, 200);
            assertPosition(map, "Recovery", 550, 200);
            assertPosition(map, "Recovered", 700, 200);

            // Constants below their associated flows
            assertPosition(map, "Contact_Rate", 250, 350);
            assertPosition(map, "Recovery_Rate", 550, 350);
        }

        @Test
        @DisplayName("Goal Seeking: Production(100) → Inventory(250), constants below")
        void shouldLayoutGoalSeekingWithExactPositions() {
            ModelDefinition def = new ModelDefinitionBuilder()
                    .name("Goal Seeking")
                    .stock("Inventory", 100, "Unit")
                    .flow("Production", "(Goal - Inventory) / Adjustment_Time", "Day",
                            null, "Inventory")
                    .constant("Goal", 860, "Unit")
                    .constant("Adjustment_Time", 8, "Day")
                    .build();

            ViewDef view = AutoLayout.layout(def);
            Map<String, ElementPlacement> map = placementMap(view);

            assertPosition(map, "Production", 100, 200);
            assertPosition(map, "Inventory", 250, 200);

            // Both constants feed Production, so first gets x=100, second nudges to x=250
            assertPosition(map, "Goal", 100, 350);
            assertPosition(map, "Adjustment_Time", 250, 350);
        }

        @Test
        @DisplayName("All elements on same y-row share the same y coordinate")
        void shouldPlaceFlowsAndStocksAtSameY() {
            ModelDefinition def = new ModelDefinitionBuilder()
                    .name("Same Y")
                    .stock("S1", 100, "Thing")
                    .stock("S2", 50, "Thing")
                    .flow("F1", "S1 * 0.1", "Day", null, "S1")
                    .flow("F2", "S1 * 0.05", "Day", "S1", "S2")
                    .flow("F3", "S2 * 0.1", "Day", "S2", null)
                    .build();

            ViewDef view = AutoLayout.layout(def);
            Map<String, ElementPlacement> map = placementMap(view);

            // All flows and stocks at y=200
            for (ElementPlacement p : view.elements()) {
                if (p.type() == ElementType.FLOW || p.type() == ElementType.STOCK) {
                    assertThat(p.y()).as("y of '%s'", p.name()).isEqualTo(200);
                }
            }
        }

        @Test
        @DisplayName("X spacing between adjacent elements is exactly 150")
        void shouldUseConsistentSpacing() {
            ModelDefinition def = new ModelDefinitionBuilder()
                    .name("Spacing")
                    .stock("Population", 100, "Person")
                    .flow("Births", "Population * 0.04", "Day", null, "Population")
                    .flow("Deaths", "Population * 0.03", "Day", "Population", null)
                    .build();

            ViewDef view = AutoLayout.layout(def);
            Map<String, ElementPlacement> map = placementMap(view);

            double birthsX = map.get("Births").x();
            double popX = map.get("Population").x();
            double deathsX = map.get("Deaths").x();

            assertThat(popX - birthsX).as("Spacing Births→Population").isEqualTo(150);
            assertThat(deathsX - popX).as("Spacing Population→Deaths").isEqualTo(150);
        }

        @Test
        @DisplayName("Constants with no placed dependents fall back to sequential x")
        void shouldFallBackToSequentialForUnassociatedConstants() {
            ModelDefinition def = new ModelDefinitionBuilder()
                    .name("Unassociated")
                    .stock("S", 100, "Thing")
                    .flow("F", "S * 0.1", "Day", "S", null)
                    .constant("Orphan", 42, "Thing")
                    .build();

            ViewDef view = AutoLayout.layout(def);
            Map<String, ElementPlacement> map = placementMap(view);

            // Orphan constant has no dependents referencing it — falls back to x=100
            assertThat(map.get("Orphan").y()).isEqualTo(350);
            assertThat(map.get("Orphan").x()).isEqualTo(100);
        }
    }
}
