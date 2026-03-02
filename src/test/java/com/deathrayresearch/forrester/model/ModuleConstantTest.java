package com.deathrayresearch.forrester.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static com.deathrayresearch.forrester.measure.Units.THING;
import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Module constants and sub-modules")
class ModuleConstantTest {

    @Test
    void shouldStoreAndRetrieveConstants() {
        Module module = new Module("TestModule");
        Constant c = new Constant("Rate", THING, 0.05);
        module.addConstant(c);

        assertEquals(0.05, module.getConstant("Rate").getValue());
        assertEquals(1, module.getConstants().size());
    }

    @Test
    void shouldStoreAndRetrieveSubModules() {
        Module parent = new Module("Parent");
        Module child = new Module("Child");
        parent.addSubModule(child);

        assertSame(child, parent.getSubModule("Child"));
        assertEquals(1, parent.getSubModules().size());
    }

    @Test
    void shouldReturnNullForMissingConstant() {
        Module module = new Module("TestModule");
        assertNull(module.getConstant("NonExistent"));
    }

    @Test
    void shouldReturnNullForMissingSubModule() {
        Module module = new Module("TestModule");
        assertNull(module.getSubModule("NonExistent"));
    }

    @Test
    void shouldReturnUnmodifiableConstants() {
        Module module = new Module("TestModule");
        assertThrows(UnsupportedOperationException.class, () ->
                module.getConstants().put("x", new Constant("x", THING, 1)));
    }

    @Test
    void shouldReturnUnmodifiableSubModules() {
        Module module = new Module("TestModule");
        assertThrows(UnsupportedOperationException.class, () ->
                module.getSubModules().put("x", new Module("x")));
    }
}
