package com.deathrayresearch.forrester.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static com.deathrayresearch.forrester.measure.Units.THING;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Module constants and sub-modules")
class ModuleConstantTest {

    @Test
    void shouldStoreAndRetrieveConstants() {
        Module module = new Module("TestModule");
        Constant c = new Constant("Rate", THING, 0.05);
        module.addConstant(c);

        assertThat(module.getConstant("Rate")).isPresent();
        assertThat(module.getConstant("Rate").orElseThrow().getValue()).isEqualTo(0.05);
        assertThat(module.getConstants()).hasSize(1);
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
    void shouldReturnNullForMissingConstant() {
        Module module = new Module("TestModule");
        assertThat(module.getConstant("NonExistent")).isEmpty();
    }

    @Test
    void shouldReturnNullForMissingSubModule() {
        Module module = new Module("TestModule");
        assertThat(module.getSubModule("NonExistent")).isEmpty();
    }

    @Test
    void shouldReturnUnmodifiableConstants() {
        Module module = new Module("TestModule");
        assertThatThrownBy(() ->
                module.getConstants().put("x", new Constant("x", THING, 1)))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void shouldReturnUnmodifiableSubModules() {
        Module module = new Module("TestModule");
        assertThatThrownBy(() ->
                module.getSubModules().put("x", new Module("x")))
                .isInstanceOf(UnsupportedOperationException.class);
    }
}
