package systems.courant.sd.model.compile;

import systems.courant.sd.Simulation;
import systems.courant.sd.model.Module;
import systems.courant.sd.model.Stock;
import systems.courant.sd.model.def.ModelDefinition;
import systems.courant.sd.model.def.ModelDefinitionBuilder;
import systems.courant.sd.model.def.ModuleInterface;
import systems.courant.sd.model.def.PortDef;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Nested Module Compilation")
class NestedModuleTest {

    private final ModelCompiler compiler = new ModelCompiler();

    @Test
    void shouldCompileModelWithModule() {
        // Inner module: a simple drain
        ModelDefinition drainModule = new ModelDefinitionBuilder()
                .name("Drain Module")
                .moduleInterface(new ModuleInterface(
                        List.of(new PortDef("drain_rate", "Dimensionless unit")),
                        List.of()))
                .stock("Tank", 100, "Thing")
                .flow("Drain", "Tank * drain_rate", "Day", "Tank", null)
                .build();

        // Outer model: uses the drain module
        ModelDefinition outer = new ModelDefinitionBuilder()
                .name("Outer Model")
                .constant("MyRate", 0.1, "Dimensionless unit")
                .module("drain1", drainModule,
                        Map.of("drain_rate", "MyRate"),
                        Map.of())
                .defaultSimulation("Day", 10, "Day")
                .build();

        CompiledModel compiled = compiler.compile(outer);
        Simulation sim = compiled.createSimulation();
        sim.execute();

        // The tank should have drained
        List<Stock> stocks = compiled.getModel().getStocks();
        assertThat(stocks).isNotEmpty();
        Stock tank = stocks.stream()
                .filter(s -> s.getName().equals("Tank"))
                .findFirst().orElseThrow();
        assertThat(tank.getValue()).as("Tank should have drained").isLessThan(100);
        assertThat(tank.getValue()).as("Tank should not be fully drained").isGreaterThan(30);
    }

    @Test
    void shouldCompileModelWithOutputBindings() {
        // Inner module exports a variable
        ModelDefinition innerModule = new ModelDefinitionBuilder()
                .name("Producer")
                .moduleInterface(new ModuleInterface(
                        List.of(),
                        List.of(new PortDef("output", "Thing"))))
                .stock("Value", 50, "Thing")
                .variable("output", "Value * 2", "Thing")
                .build();

        ModelDefinition outer = new ModelDefinitionBuilder()
                .name("Consumer")
                .module("producer", innerModule,
                        Map.of(),
                        Map.of("output", "ProducerOutput"))
                .defaultSimulation("Day", 1, "Day")
                .build();

        CompiledModel compiled = compiler.compile(outer);
        assertThat(compiled.getModel().getVariable("ProducerOutput"))
                .as("Output binding should create variable in parent model").isPresent();
    }

    @Test
    void shouldSupportTwoModuleInstances() {
        ModelDefinition module = new ModelDefinitionBuilder()
                .name("Counter")
                .stock("Count", 0, "Thing")
                .flow("Increment", "1", "Day", null, "Count")
                .build();

        ModelDefinition outer = new ModelDefinitionBuilder()
                .name("Dual Counter")
                .module("counter1", module, Map.of(), Map.of())
                .module("counter2", module, Map.of(), Map.of())
                .defaultSimulation("Day", 5, "Day")
                .build();

        CompiledModel compiled = compiler.compile(outer);
        Simulation sim = compiled.createSimulation();
        sim.execute();

        // Both counters should have incremented independently
        List<Stock> stocks = compiled.getModel().getStocks();
        assertThat(stocks).hasSize(2);
    }

    @Test
    void shouldSupportQualifiedNameParsing() {
        QualifiedName qn = QualifiedName.parse("Workforce.Total Workforce");
        assertThat(qn.parts()).hasSize(2);
        assertThat(qn.parts().get(0)).isEqualTo("Workforce");
        assertThat(qn.leaf()).isEqualTo("Total Workforce");
        assertThat(qn.isQualified()).isTrue();
    }

    @Test
    void shouldSupportSimpleQualifiedName() {
        QualifiedName qn = QualifiedName.parse("Population");
        assertThat(qn.parts()).hasSize(1);
        assertThat(qn.leaf()).isEqualTo("Population");
        assertThat(qn.isQualified()).isFalse();
    }

    @Test
    void shouldAccessModuleElements() {
        ModelDefinition inner = new ModelDefinitionBuilder()
                .name("Inner")
                .stock("S", 42, "Thing")
                .build();

        ModelDefinition outer = new ModelDefinitionBuilder()
                .name("Outer")
                .module("m1", inner, Map.of(), Map.of())
                .defaultSimulation("Day", 1, "Day")
                .build();

        CompiledModel compiled = compiler.compile(outer);
        List<Module> modules = compiled.getModel().getModules();
        assertThat(modules).isNotEmpty();
        Module m = modules.stream()
                .filter(mod -> mod.getName().equals("m1"))
                .findFirst().orElseThrow();
        assertThat(m.getStock("S")).isPresent();
        assertThat(m.getStock("S").orElseThrow().getValue()).isEqualTo(42);
    }
}
