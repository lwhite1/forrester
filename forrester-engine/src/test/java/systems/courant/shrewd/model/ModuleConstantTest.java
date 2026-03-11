package systems.courant.forrester.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static systems.courant.forrester.measure.Units.THING;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Module variables and sub-modules")
class ModuleConstantTest {

    @Test
    void shouldStoreAndRetrieveVariables() {
        Module module = new Module("TestModule");
        Variable v = new Variable("Rate", THING, () -> 0.05);
        module.addVariable(v);

        assertThat(module.getVariable("Rate")).isPresent();
        assertThat(module.getVariable("Rate").orElseThrow().getValue()).isEqualTo(0.05);
        assertThat(module.getVariables()).hasSize(1);
    }

    @Test
    void shouldStoreAndRetrieveSubModules() {
        Module parent = new Module("Parent");
        Module child = new Module("Child");
        parent.addSubModule(child);

        assertThat(parent.getSubModule("Child")).isPresent().containsSame(child);
        assertThat(parent.getSubModules()).hasSize(1);
    }

    @Test
    void shouldReturnEmptyForMissingVariable() {
        Module module = new Module("TestModule");
        assertThat(module.getVariable("NonExistent")).isEmpty();
    }

    @Test
    void shouldReturnEmptyForMissingSubModule() {
        Module module = new Module("TestModule");
        assertThat(module.getSubModule("NonExistent")).isEmpty();
    }

    @Test
    void shouldReturnUnmodifiableSubModules() {
        Module module = new Module("TestModule");
        assertThatThrownBy(() ->
                module.getSubModules().put("x", new Module("x")))
                .isInstanceOf(UnsupportedOperationException.class);
    }
}
