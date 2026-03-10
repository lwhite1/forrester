package systems.courant.forrester.io.json;

import systems.courant.forrester.Simulation;
import systems.courant.forrester.model.Stock;
import systems.courant.forrester.model.compile.CompiledModel;
import systems.courant.forrester.model.compile.ModelCompiler;
import systems.courant.forrester.model.def.ModelDefinition;
import systems.courant.forrester.model.def.ModelDefinitionBuilder;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

@DisplayName("JSON round-trip: build → compile → simulate → serialize → deserialize → compile → simulate")
class JsonRoundTripTest {

    private final ModelDefinitionSerializer serializer = new ModelDefinitionSerializer();
    private final ModelCompiler compiler = new ModelCompiler();

    private Stock findStock(List<Stock> stocks, String name) {
        return stocks.stream()
                .filter(s -> s.getName().equals(name))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Stock not found: " + name));
    }

    @Test
    void shouldProduceIdenticalSIRResults() {
        // Build and simulate the original
        ModelDefinition original = buildSIR();
        CompiledModel compiled1 = compiler.compile(original);
        Simulation sim1 = compiled1.createSimulation();
        sim1.execute();

        // Serialize and deserialize
        String json = serializer.toJson(original);
        ModelDefinition deserialized = serializer.fromJson(json);

        // Compile and simulate the deserialized version
        CompiledModel compiled2 = compiler.compile(deserialized);
        Simulation sim2 = compiled2.createSimulation();
        sim2.execute();

        // Compare stock values by name
        List<Stock> stocks1 = compiled1.getModel().getStocks();
        List<Stock> stocks2 = compiled2.getModel().getStocks();
        assertThat(stocks2).hasSameSizeAs(stocks1);
        for (Stock s1 : stocks1) {
            Stock s2 = findStock(stocks2, s1.getName());
            assertThat(s2.getValue())
                    .as("Stock '%s' values should match", s1.getName())
                    .isCloseTo(s1.getValue(), within(0.001));
        }
    }

    @Test
    void shouldProduceIdenticalExponentialGrowthResults() {
        ModelDefinition original = new ModelDefinitionBuilder()
                .name("Exponential Growth")
                .stock("Population", 100, "Person")
                .flow("Births", "Population * Growth_Rate", "Day", null, "Population")
                .constant("Growth_Rate", 0.02, "Dimensionless unit")
                .defaultSimulation("Day", 50, "Day")
                .build();

        CompiledModel compiled1 = compiler.compile(original);
        Simulation sim1 = compiled1.createSimulation();
        sim1.execute();
        double originalPop = findStock(compiled1.getModel().getStocks(), "Population").getValue();

        String json = serializer.toJson(original);
        ModelDefinition deserialized = serializer.fromJson(json);
        CompiledModel compiled2 = compiler.compile(deserialized);
        Simulation sim2 = compiled2.createSimulation();
        sim2.execute();
        double roundTrippedPop = findStock(compiled2.getModel().getStocks(), "Population").getValue();

        assertThat(roundTrippedPop).isCloseTo(originalPop, within(0.001));
    }

    @Test
    void shouldProduceIdenticalDrainResults() {
        ModelDefinition original = new ModelDefinitionBuilder()
                .name("Drain")
                .stock("Tank", 1000, "Thing")
                .flow("Drain", "Tank * 0.05", "Day", "Tank", null)
                .defaultSimulation("Day", 30, "Day")
                .build();

        CompiledModel compiled1 = compiler.compile(original);
        Simulation sim1 = compiled1.createSimulation();
        sim1.execute();

        String json = serializer.toJson(original);
        ModelDefinition deserialized = serializer.fromJson(json);
        CompiledModel compiled2 = compiler.compile(deserialized);
        Simulation sim2 = compiled2.createSimulation();
        sim2.execute();

        List<Stock> stocks1 = compiled1.getModel().getStocks();
        List<Stock> stocks2 = compiled2.getModel().getStocks();
        for (Stock s1 : stocks1) {
            Stock s2 = findStock(stocks2, s1.getName());
            assertThat(s2.getValue())
                    .as("Stock '%s' values should match", s1.getName())
                    .isCloseTo(s1.getValue(), within(0.001));
        }
    }

    private ModelDefinition buildSIR() {
        return new ModelDefinitionBuilder()
                .name("SIR Model")
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
                .defaultSimulation("Day", 56, "Day")
                .build();
    }
}
