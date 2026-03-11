package systems.courant.forrester.model.def;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("ModelDefinitionBuilder")
class ModelDefinitionBuilderTest {

    @Test
    void shouldBuildSIRModel() {
        ModelDefinition sir = new ModelDefinitionBuilder()
                .name("SIR Model")
                .comment("Classic SIR epidemiological model")
                .stock("Susceptible", 1000, "Person")
                .stock("Infectious", 10, "Person")
                .stock("Recovered", 0, "Person")
                .flow("Infection",
                        "Contact_Rate * Infectious / (Susceptible + Infectious + Recovered) * Infectivity * Susceptible",
                        "Day", "Susceptible", "Infectious")
                .flow("Recovery", "Infectious * Recovery_Rate", "Day",
                        "Infectious", "Recovered")
                .constant("Contact_Rate", 8.0, "Dimensionless")
                .constant("Infectivity", 0.10, "Dimensionless")
                .constant("Recovery_Rate", 0.20, "Dimensionless")
                .defaultSimulation("Day", 56, "Day")
                .build();

        assertThat(sir.name()).isEqualTo("SIR Model");
        assertThat(sir.stocks().size()).isEqualTo(3);
        assertThat(sir.flows().size()).isEqualTo(2);
        assertThat(sir.auxiliaries().size()).isEqualTo(3);
        assertThat(sir.defaultSimulation()).isNotNull();
        assertThat(sir.defaultSimulation().duration()).isEqualTo(56);
    }

    @Test
    void shouldBuildEmptyModel() {
        ModelDefinition def = new ModelDefinitionBuilder()
                .name("Empty")
                .build();
        assertThat(def.name()).isEqualTo("Empty");
        assertThat(def.stocks().isEmpty()).isTrue();
        assertThat(def.flows().isEmpty()).isTrue();
        assertThat(def.auxiliaries().isEmpty()).isTrue();
        assertThat(def.auxiliaries().isEmpty()).isTrue();
    }

    @Test
    void shouldBuildModelWithAuxiliaries() {
        ModelDefinition def = new ModelDefinitionBuilder()
                .name("Aux Test")
                .stock("Population", 1000, "Person")
                .aux("Birth Rate", "Population * Fertility", "Person")
                .constant("Fertility", 0.03, "Dimensionless")
                .build();
        assertThat(def.auxiliaries().size()).isEqualTo(2);
        assertThat(def.auxiliaries().get(0).name()).isEqualTo("Birth Rate");
    }

    @Test
    void shouldBuildModelWithLookupTable() {
        ModelDefinition def = new ModelDefinitionBuilder()
                .name("Lookup Test")
                .lookupTable("Effect of Crowding",
                        new double[]{0, 0.5, 1.0, 1.5, 2.0},
                        new double[]{1.0, 0.9, 0.7, 0.4, 0.1},
                        "LINEAR")
                .build();
        assertThat(def.lookupTables().size()).isEqualTo(1);
        assertThat(def.lookupTables().get(0).xValues().length).isEqualTo(5);
    }

    @Test
    void shouldBuildModelWithSimulationSettings() {
        ModelDefinition def = new ModelDefinitionBuilder()
                .name("Sim Test")
                .defaultSimulation("Week", 52, "Week")
                .build();
        assertThat(def.defaultSimulation().timeStep()).isEqualTo("Week");
        assertThat(def.defaultSimulation().duration()).isEqualTo(52);
    }

    @Test
    void shouldValidateSuccessfully() {
        ModelDefinition sir = new ModelDefinitionBuilder()
                .name("SIR Model")
                .stock("Susceptible", 1000, "Person")
                .stock("Infectious", 10, "Person")
                .stock("Recovered", 0, "Person")
                .flow("Infection",
                        "Contact_Rate * Infectious / (Susceptible + Infectious + Recovered) * Infectivity * Susceptible",
                        "Day", "Susceptible", "Infectious")
                .flow("Recovery", "Infectious * Recovery_Rate", "Day",
                        "Infectious", "Recovered")
                .constant("Contact_Rate", 8.0, "Dimensionless")
                .constant("Infectivity", 0.10, "Dimensionless")
                .constant("Recovery_Rate", 0.20, "Dimensionless")
                .build();

        List<String> errors = DefinitionValidator.validate(sir);
        assertThat(errors.isEmpty()).as("SIR model should validate: " + errors).isTrue();
    }
}
