package systems.courant.shrewd.integration;

import systems.courant.shrewd.Simulation;
import systems.courant.shrewd.io.json.ModelDefinitionSerializer;
import systems.courant.shrewd.model.Stock;
import systems.courant.shrewd.model.compile.CompiledModel;
import systems.courant.shrewd.model.compile.ModelCompiler;
import systems.courant.shrewd.model.def.ConnectorRoute;
import systems.courant.shrewd.model.def.ElementPlacement;
import systems.courant.shrewd.model.def.ElementType;
import systems.courant.shrewd.model.def.FlowDef;
import systems.courant.shrewd.model.def.ModelDefinition;
import systems.courant.shrewd.model.def.ModelDefinitionBuilder;
import systems.courant.shrewd.model.def.ViewDef;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

/**
 * Integration test: build model with view data → serialize to JSON → deserialize
 * → verify structural fidelity and simulation equivalence.
 *
 * <p>Extends unit-level round-trip coverage by including view element placements
 * and connector routes, which are the most fragile part of the serialization pipeline.
 */
@DisplayName("File round-trip (JSON save/load)")
class FileRoundTripTest {

    private final ModelDefinitionSerializer serializer = new ModelDefinitionSerializer();
    private final ModelCompiler compiler = new ModelCompiler();

    @Nested
    @DisplayName("Structural fidelity")
    class StructuralFidelity {

        @Test
        @DisplayName("should preserve view element placements through JSON round-trip")
        void shouldPreserveViewPlacements() {
            List<ElementPlacement> elements = List.of(
                    new ElementPlacement("Population", ElementType.STOCK, 200, 300),
                    new ElementPlacement("births", ElementType.FLOW, 100, 300),
                    new ElementPlacement("rate", ElementType.AUX, 100, 200));
            List<ConnectorRoute> connectors = List.of(
                    new ConnectorRoute("rate", "births", List.of()));
            ViewDef view = new ViewDef("Main View", elements, connectors, List.of());

            ModelDefinition original = new ModelDefinitionBuilder()
                    .name("ViewTest")
                    .defaultSimulation("Day", 10, "Day")
                    .stock("Population", 100, "People")
                    .flow(new FlowDef("births", "Population * rate", "Day", null, "Population"))
                    .constant("rate", 0.02, "1/Day")
                    .view(view)
                    .build();

            String json = serializer.toJson(original);
            ModelDefinition loaded = serializer.fromJson(json);

            assertThat(loaded.views()).hasSize(1);
            ViewDef loadedView = loaded.views().getFirst();
            assertThat(loadedView.name()).isEqualTo("Main View");
            assertThat(loadedView.elements()).hasSize(3);

            ElementPlacement pop = loadedView.elements().stream()
                    .filter(e -> e.name().equals("Population")).findFirst().orElseThrow();
            assertThat(pop.type()).isEqualTo(ElementType.STOCK);
            assertThat(pop.x()).isEqualTo(200);
            assertThat(pop.y()).isEqualTo(300);

            assertThat(loadedView.connectors()).hasSize(1);
            ConnectorRoute conn = loadedView.connectors().getFirst();
            assertThat(conn.from()).isEqualTo("rate");
            assertThat(conn.to()).isEqualTo("births");
        }

        @Test
        @DisplayName("should preserve all definition fields through JSON round-trip")
        void shouldPreserveAllFields() {
            ModelDefinition original = new ModelDefinitionBuilder()
                    .name("FullModel")
                    .defaultSimulation("Month", 120, "Month")
                    .stock("Inventory", 500, "Widget")
                    .flow(new FlowDef("production", "desired_production", "Month",
                            null, "Inventory"))
                    .flow(new FlowDef("shipments", "Inventory * ship_rate", "Month",
                            "Inventory", null))
                    .constant("desired_production", 50, "Widget/Month")
                    .constant("ship_rate", 0.1, "1/Month")
                    .aux("net_flow", "desired_production - Inventory * ship_rate", "Widget/Month")
                    .lookupTable("effect_curve",
                            new double[]{0, 0.5, 1, 1.5, 2},
                            new double[]{0, 0.3, 1, 1.5, 1.8},
                            "LINEAR")
                    .build();

            String json = serializer.toJson(original);
            ModelDefinition loaded = serializer.fromJson(json);

            assertThat(loaded.name()).isEqualTo("FullModel");
            assertThat(loaded.stocks()).hasSize(1);
            assertThat(loaded.flows()).hasSize(2);
            assertThat(loaded.parameters()).hasSize(2);
            assertThat(loaded.auxiliaries()).hasSize(3); // 2 constants + 1 formula aux
            assertThat(loaded.lookupTables()).hasSize(1);
            assertThat(loaded.defaultSimulation().timeStep()).isEqualTo("Month");
            assertThat(loaded.defaultSimulation().duration()).isEqualTo(120.0);
        }
    }

    @Nested
    @DisplayName("Simulation equivalence")
    class SimulationEquivalence {

        @Test
        @DisplayName("should produce identical simulation results after JSON round-trip")
        void shouldMatchSimulationResults() {
            ModelDefinition original = new ModelDefinitionBuilder()
                    .name("Logistic Growth")
                    .defaultSimulation("Day", 100, "Day")
                    .stock("Population", 10, "Animal")
                    .flow(new FlowDef("growth", "Population * rate * (1 - Population / capacity)",
                            "Day", null, "Population"))
                    .constant("rate", 0.1, "1/Day")
                    .constant("capacity", 1000, "Animal")
                    .build();

            CompiledModel compiled1 = compiler.compile(original);
            Simulation sim1 = compiled1.createSimulation();
            sim1.execute();
            double originalPop = findStock(compiled1, "Population").getValue();

            String json = serializer.toJson(original);
            ModelDefinition loaded = serializer.fromJson(json);

            CompiledModel compiled2 = compiler.compile(loaded);
            Simulation sim2 = compiled2.createSimulation();
            sim2.execute();
            double loadedPop = findStock(compiled2, "Population").getValue();

            assertThat(loadedPop).isCloseTo(originalPop, within(0.001));
        }

        @Test
        @DisplayName("should survive double round-trip without drift")
        void shouldSurviveDoubleRoundTrip() {
            ModelDefinition original = new ModelDefinitionBuilder()
                    .name("Double RT")
                    .defaultSimulation("Day", 30, "Day")
                    .stock("Level", 100, "Unit")
                    .flow(new FlowDef("inflow", "20", "Day", null, "Level"))
                    .flow(new FlowDef("outflow", "Level * 0.1", "Day", "Level", null))
                    .build();

            String json1 = serializer.toJson(original);
            ModelDefinition pass1 = serializer.fromJson(json1);
            String json2 = serializer.toJson(pass1);
            ModelDefinition pass2 = serializer.fromJson(json2);

            assertThat(json2).isEqualTo(json1);

            CompiledModel compiled = compiler.compile(pass2);
            Simulation sim = compiled.createSimulation();
            sim.execute();
            assertThat(findStock(compiled, "Level").getValue()).isFinite();
        }
    }

    private Stock findStock(CompiledModel compiled, String name) {
        return compiled.getModel().getStocks().stream()
                .filter(s -> s.getName().equals(name))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Stock not found: " + name));
    }
}
