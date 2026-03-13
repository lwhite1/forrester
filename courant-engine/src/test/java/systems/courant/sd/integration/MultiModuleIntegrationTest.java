package systems.courant.sd.integration;

import systems.courant.sd.Simulation;
import systems.courant.sd.io.json.ModelDefinitionSerializer;
import systems.courant.sd.model.Module;
import systems.courant.sd.model.Stock;
import systems.courant.sd.model.compile.CompiledModel;
import systems.courant.sd.model.compile.ModelCompiler;
import systems.courant.sd.model.def.FlowDef;
import systems.courant.sd.model.def.ModelDefinition;
import systems.courant.sd.model.def.ModelDefinitionBuilder;
import systems.courant.sd.model.def.ModuleInterface;
import systems.courant.sd.model.def.PortDef;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

/**
 * Integration test: multi-module model compilation and simulation.
 *
 * <p>Tests that parent models with sub-modules compile correctly, simulate with proper
 * inter-module bindings, and survive JSON serialization round-trips.
 */
@DisplayName("Multi-module integration")
class MultiModuleIntegrationTest {

    private final ModelCompiler compiler = new ModelCompiler();
    private final ModelDefinitionSerializer serializer = new ModelDefinitionSerializer();

    @Test
    @DisplayName("should simulate parent with module using input bindings")
    void shouldSimulateWithInputBindings() {
        ModelDefinition growthModule = new ModelDefinitionBuilder()
                .name("GrowthModule")
                .moduleInterface(new ModuleInterface(
                        List.of(new PortDef("growth_rate", "1/Day")),
                        List.of()))
                .stock("Population", 100, "Person")
                .flow(new FlowDef("births", "Population * growth_rate", "Day",
                        null, "Population"))
                .build();

        ModelDefinition host = new ModelDefinitionBuilder()
                .name("Host")
                .defaultSimulation("Day", 20, "Day")
                .constant("Rate", 0.05, "1/Day")
                .module("region", growthModule,
                        Map.of("growth_rate", "Rate"),
                        Map.of())
                .build();

        CompiledModel compiled = compiler.compile(host);
        Simulation sim = compiled.createSimulation();
        sim.execute();

        Stock pop = findStock(compiled, "Population");
        // 100 * (1 + 0.05)^20 ≈ 265, Euler approximation will be close
        assertThat(pop.getValue())
                .as("Population should grow with bound rate")
                .isGreaterThan(200)
                .isLessThan(300);
    }

    @Test
    @DisplayName("should simulate two independent module instances")
    void shouldSimulateTwoInstances() {
        ModelDefinition counterModule = new ModelDefinitionBuilder()
                .name("Counter")
                .moduleInterface(new ModuleInterface(
                        List.of(new PortDef("step", "Thing/Day")),
                        List.of()))
                .stock("Total", 0, "Thing")
                .flow(new FlowDef("accumulate", "step", "Day", null, "Total"))
                .build();

        ModelDefinition host = new ModelDefinitionBuilder()
                .name("DualCounter")
                .defaultSimulation("Day", 10, "Day")
                .constant("SmallStep", 1, "Thing/Day")
                .constant("BigStep", 5, "Thing/Day")
                .module("slow", counterModule,
                        Map.of("step", "SmallStep"),
                        Map.of())
                .module("fast", counterModule,
                        Map.of("step", "BigStep"),
                        Map.of())
                .build();

        CompiledModel compiled = compiler.compile(host);
        Simulation sim = compiled.createSimulation();
        sim.execute();

        List<Stock> stocks = compiled.getModel().getStocks();
        assertThat(stocks).hasSize(2);

        // With dt=1 and 10 steps (0..10 inclusive = 11 updates):
        // slow = 1*11 = 11, fast = 5*11 = 55, total = 66
        double sum = stocks.stream().mapToDouble(Stock::getValue).sum();
        assertThat(sum).isCloseTo(66, within(0.1));
    }

    @Test
    @DisplayName("should access module stocks through module API")
    void shouldAccessModuleStocks() {
        ModelDefinition inner = new ModelDefinitionBuilder()
                .name("Tank")
                .stock("Level", 75, "Liter")
                .flow(new FlowDef("drain", "Level * 0.1", "Day", "Level", null))
                .build();

        ModelDefinition outer = new ModelDefinitionBuilder()
                .name("Plant")
                .defaultSimulation("Day", 5, "Day")
                .module("tank1", inner, Map.of(), Map.of())
                .build();

        CompiledModel compiled = compiler.compile(outer);
        Simulation sim = compiled.createSimulation();
        sim.execute();

        List<Module> modules = compiled.getModel().getModules();
        assertThat(modules).hasSize(1);

        Module tank = modules.getFirst();
        assertThat(tank.getName()).isEqualTo("tank1");
        assertThat(tank.getStock("Level")).isPresent();
        assertThat(tank.getStock("Level").orElseThrow().getValue())
                .as("Tank should have partially drained")
                .isLessThan(75)
                .isGreaterThan(0);
    }

    @Test
    @DisplayName("should round-trip modular model through JSON")
    void shouldRoundTripModularModel() {
        ModelDefinition inner = new ModelDefinitionBuilder()
                .name("Heater")
                .moduleInterface(new ModuleInterface(
                        List.of(new PortDef("target_temp", "Degree")),
                        List.of()))
                .stock("Temp", 20, "Degree")
                .flow(new FlowDef("heating", "(target_temp - Temp) * 0.1", "Minute",
                        null, "Temp"))
                .build();

        ModelDefinition original = new ModelDefinitionBuilder()
                .name("Building")
                .defaultSimulation("Minute", 60, "Minute")
                .constant("Setpoint", 22, "Degree")
                .module("heater", inner,
                        Map.of("target_temp", "Setpoint"),
                        Map.of())
                .build();

        // Simulate original
        CompiledModel compiled1 = compiler.compile(original);
        Simulation sim1 = compiled1.createSimulation();
        sim1.execute();
        double originalTemp = findStock(compiled1, "Temp").getValue();

        // JSON round-trip
        String json = serializer.toJson(original);
        ModelDefinition loaded = serializer.fromJson(json);

        // Simulate loaded
        CompiledModel compiled2 = compiler.compile(loaded);
        Simulation sim2 = compiled2.createSimulation();
        sim2.execute();
        double loadedTemp = findStock(compiled2, "Temp").getValue();

        assertThat(loadedTemp).isCloseTo(originalTemp, within(0.001));
        assertThat(loaded.modules()).hasSize(1);
        assertThat(loaded.modules().getFirst().instanceName()).isEqualTo("heater");
    }

    private Stock findStock(CompiledModel compiled, String name) {
        return compiled.getModel().getStocks().stream()
                .filter(s -> s.getName().equals(name))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Stock not found: " + name));
    }
}
