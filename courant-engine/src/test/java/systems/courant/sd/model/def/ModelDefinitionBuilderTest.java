package systems.courant.sd.model.def;

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
        assertThat(sir.variables().size()).isEqualTo(3);
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
        assertThat(def.variables().isEmpty()).isTrue();
        assertThat(def.variables().isEmpty()).isTrue();
    }

    @Test
    void shouldBuildModelWithVariables() {
        ModelDefinition def = new ModelDefinitionBuilder()
                .name("Aux Test")
                .stock("Population", 1000, "Person")
                .variable("Birth Rate", "Population * Fertility", "Person")
                .constant("Fertility", 0.03, "Dimensionless")
                .build();
        assertThat(def.variables().size()).isEqualTo(2);
        assertThat(def.variables().get(0).name()).isEqualTo("Birth Rate");
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

    @Test
    void shouldClearLookupTables() {
        ModelDefinition def = new ModelDefinitionBuilder()
                .name("Clear Test")
                .lookupTable("T1", new double[]{0, 1}, new double[]{0, 1}, "LINEAR")
                .clearLookupTables()
                .build();
        assertThat(def.lookupTables()).isEmpty();
    }

    @Test
    void shouldClearModules() {
        ModelDefinition inner = new ModelDefinitionBuilder().name("Inner").build();
        ModelDefinition def = new ModelDefinitionBuilder()
                .name("Clear Modules Test")
                .module("m1", inner, java.util.Map.of(), java.util.Map.of())
                .clearModules()
                .build();
        assertThat(def.modules()).isEmpty();
    }

    @Test
    void shouldClearSubscripts() {
        ModelDefinition def = new ModelDefinitionBuilder()
                .name("Clear Subscripts")
                .subscript("Region", List.of("North", "South"))
                .clearSubscripts()
                .build();
        assertThat(def.subscripts()).isEmpty();
    }

    @Test
    void shouldClearCldVariables() {
        ModelDefinition def = new ModelDefinitionBuilder()
                .name("Clear CLD")
                .cldVariable("Population")
                .clearCldVariables()
                .build();
        assertThat(def.cldVariables()).isEmpty();
    }

    @Test
    void shouldClearCausalLinks() {
        ModelDefinition def = new ModelDefinitionBuilder()
                .name("Clear Links")
                .cldVariable("A")
                .cldVariable("B")
                .causalLink("A", "B", CausalLinkDef.Polarity.POSITIVE)
                .clearCausalLinks()
                .build();
        assertThat(def.causalLinks()).isEmpty();
    }

    @Test
    void shouldClearComments() {
        ModelDefinition def = new ModelDefinitionBuilder()
                .name("Clear Comments")
                .comment("note1", "Some text")
                .clearComments()
                .build();
        assertThat(def.comments()).isEmpty();
    }
}
