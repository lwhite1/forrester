package com.deathrayresearch.forrester.model.compile;

import com.deathrayresearch.forrester.Simulation;
import com.deathrayresearch.forrester.model.Module;
import com.deathrayresearch.forrester.model.Stock;
import com.deathrayresearch.forrester.model.def.ModelDefinition;
import com.deathrayresearch.forrester.model.def.ModelDefinitionBuilder;
import com.deathrayresearch.forrester.model.def.ModuleInterface;
import com.deathrayresearch.forrester.model.def.PortDef;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

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
        assertFalse(stocks.isEmpty());
        Stock tank = stocks.stream()
                .filter(s -> s.getName().equals("Tank"))
                .findFirst().orElseThrow();
        assertTrue(tank.getValue() < 100, "Tank should have drained");
        assertTrue(tank.getValue() > 30, "Tank should not be fully drained");
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
                .aux("output", "Value * 2", "Thing")
                .build();

        ModelDefinition outer = new ModelDefinitionBuilder()
                .name("Consumer")
                .module("producer", innerModule,
                        Map.of(),
                        Map.of("output", "ProducerOutput"))
                .defaultSimulation("Day", 1, "Day")
                .build();

        CompiledModel compiled = compiler.compile(outer);
        assertNotNull(compiled.getModel().getVariable("ProducerOutput"),
                "Output binding should create variable in parent model");
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
        assertEquals(2, stocks.size());
    }

    @Test
    void shouldSupportQualifiedNameParsing() {
        QualifiedName qn = QualifiedName.parse("Workforce.Total Workforce");
        assertEquals(2, qn.parts().size());
        assertEquals("Workforce", qn.parts().get(0));
        assertEquals("Total Workforce", qn.leaf());
        assertTrue(qn.isQualified());
    }

    @Test
    void shouldSupportSimpleQualifiedName() {
        QualifiedName qn = QualifiedName.parse("Population");
        assertEquals(1, qn.parts().size());
        assertEquals("Population", qn.leaf());
        assertFalse(qn.isQualified());
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
        assertFalse(modules.isEmpty());
        Module m = modules.stream()
                .filter(mod -> mod.getName().equals("m1"))
                .findFirst().orElseThrow();
        assertNotNull(m.getStock("S"));
        assertEquals(42, m.getStock("S").getValue());
    }
}
