package systems.courant.forrester.model.graph;

import systems.courant.forrester.model.def.ConnectorRoute;
import systems.courant.forrester.model.def.ModelDefinition;
import systems.courant.forrester.model.def.ModelDefinitionBuilder;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("ConnectorGenerator")
class ConnectorGeneratorTest {

    @Test
    void shouldGenerateConnectorsFromFormulaDependencies() {
        ModelDefinition def = new ModelDefinitionBuilder()
                .name("Simple")
                .stock("Tank", 100, "Thing")
                .constant("Rate", 0.1, "Dimensionless unit")
                .flow("Drain", "Tank * Rate", "Day", "Tank", null)
                .build();

        List<ConnectorRoute> connectors = ConnectorGenerator.generate(def);

        assertThat(connectors.isEmpty()).isFalse();
        // Should have connectors for: Tank → Drain, Rate → Drain, Drain → Tank
        Set<String> keys = connectors.stream()
                .map(c -> c.from() + " -> " + c.to())
                .collect(Collectors.toSet());
        assertThat(keys.contains("Tank -> Drain")).as("Tank influences Drain").isTrue();
        assertThat(keys.contains("Rate -> Drain")).as("Rate influences Drain").isTrue();
        assertThat(keys.contains("Drain -> Tank")).as("Drain connects to Tank (source)").isTrue();
    }

    @Test
    void shouldEliminateDuplicateConnectors() {
        // A flow that references the same stock in its formula AND as source/sink
        ModelDefinition def = new ModelDefinitionBuilder()
                .name("DupTest")
                .stock("S", 100, "Thing")
                .flow("F", "S * 0.1", "Day", "S", null)
                .build();

        List<ConnectorRoute> connectors = ConnectorGenerator.generate(def);

        // Count connectors from S → F (formula dep) — should appear only once
        long sToFCount = connectors.stream()
                .filter(c -> c.from().equals("S") && c.to().equals("F"))
                .count();
        assertThat(sToFCount).as("S → F should appear only once").isEqualTo(1);
    }

    @Test
    void shouldGenerateConnectorsForSIRModel() {
        ModelDefinition sir = new ModelDefinitionBuilder()
                .name("SIR")
                .stock("Susceptible", 1000, "Person")
                .stock("Infectious", 10, "Person")
                .stock("Recovered", 0, "Person")
                .constant("Contact Rate", 8, "Dimensionless unit")
                .constant("Infectivity", 0.5, "Dimensionless unit")
                .constant("Duration", 5, "Day")
                .flow("Infection",
                        "Contact_Rate * Infectious / (Susceptible + Infectious + Recovered) * Infectivity * Susceptible",
                        "Day", "Susceptible", "Infectious")
                .flow("Recovery", "Infectious / Duration", "Day", "Infectious", "Recovered")
                .build();

        List<ConnectorRoute> connectors = ConnectorGenerator.generate(sir);

        Set<String> keys = connectors.stream()
                .map(c -> c.from() + " -> " + c.to())
                .collect(Collectors.toSet());

        // Infection formula refs
        assertThat(keys.contains("Contact Rate -> Infection")).isTrue();
        assertThat(keys.contains("Infectivity -> Infection")).isTrue();
        assertThat(keys.contains("Susceptible -> Infection")).isTrue();
        assertThat(keys.contains("Infectious -> Infection")).isTrue();
        assertThat(keys.contains("Recovered -> Infection")).isTrue();

        // Infection flow connections
        assertThat(keys.contains("Infection -> Susceptible")).isTrue(); // source
        assertThat(keys.contains("Infection -> Infectious")).isTrue();  // sink

        // Recovery formula refs
        assertThat(keys.contains("Infectious -> Recovery")).isTrue();
        assertThat(keys.contains("Duration -> Recovery")).isTrue();

        // Recovery flow connections
        assertThat(keys.contains("Recovery -> Infectious")).isTrue(); // source
        assertThat(keys.contains("Recovery -> Recovered")).isTrue();  // sink
    }

    @Test
    void shouldReturnEmptyForModelWithNoEquations() {
        ModelDefinition def = new ModelDefinitionBuilder()
                .name("ConstantsOnly")
                .constant("A", 1, "Thing")
                .constant("B", 2, "Thing")
                .build();

        List<ConnectorRoute> connectors = ConnectorGenerator.generate(def);
        assertThat(connectors.isEmpty()).isTrue();
    }

    @Test
    void shouldHandleAuxiliaryDependencies() {
        ModelDefinition def = new ModelDefinitionBuilder()
                .name("WithAux")
                .stock("S", 100, "Thing")
                .constant("C", 2, "Thing")
                .aux("Total", "S + C", "Thing")
                .build();

        List<ConnectorRoute> connectors = ConnectorGenerator.generate(def);

        Set<String> keys = connectors.stream()
                .map(c -> c.from() + " -> " + c.to())
                .collect(Collectors.toSet());

        assertThat(keys.contains("S -> Total")).as("S influences Total").isTrue();
        assertThat(keys.contains("C -> Total")).as("C influences Total").isTrue();
    }
}
