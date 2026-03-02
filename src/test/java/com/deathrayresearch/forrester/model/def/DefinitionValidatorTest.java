package com.deathrayresearch.forrester.model.def;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("DefinitionValidator")
class DefinitionValidatorTest {

    @Test
    void shouldPassForValidSIRModel() {
        ModelDefinition sir = buildSIR();
        List<String> errors = DefinitionValidator.validate(sir);
        assertTrue(errors.isEmpty(), "Expected no errors but got: " + errors);
    }

    @Test
    void shouldDetectDuplicateNames() {
        ModelDefinition def = new ModelDefinitionBuilder()
                .name("Dup")
                .stock("S", 100, "Person")
                .constant("S", 5.0, "Person") // same name as stock
                .build();
        List<String> errors = DefinitionValidator.validate(def);
        assertTrue(errors.stream().anyMatch(e -> e.contains("Duplicate")));
    }

    @Test
    void shouldDetectDanglingFlowSource() {
        ModelDefinition def = new ModelDefinitionBuilder()
                .name("Bad")
                .stock("A", 100, "Person")
                .flow("F", "A * 0.1", "Day", "NonExistent", "A")
                .build();
        List<String> errors = DefinitionValidator.validate(def);
        assertTrue(errors.stream().anyMatch(e -> e.contains("non-existent source")));
    }

    @Test
    void shouldDetectDanglingFlowSink() {
        ModelDefinition def = new ModelDefinitionBuilder()
                .name("Bad")
                .stock("A", 100, "Person")
                .flow("F", "A * 0.1", "Day", "A", "NonExistent")
                .build();
        List<String> errors = DefinitionValidator.validate(def);
        assertTrue(errors.stream().anyMatch(e -> e.contains("non-existent sink")));
    }

    @Test
    void shouldDetectInvalidFormula() {
        ModelDefinition def = new ModelDefinitionBuilder()
                .name("Bad")
                .stock("A", 100, "Person")
                .flow("F", "A +", "Day", "A", null) // invalid expression
                .build();
        List<String> errors = DefinitionValidator.validate(def);
        assertTrue(errors.stream().anyMatch(e -> e.contains("invalid equation")));
    }

    @Test
    void shouldDetectCircularModuleReference() {
        // Model A contains module instance of itself
        ModelDefinition inner = new ModelDefinitionBuilder()
                .name("A")
                .stock("S", 0, "Thing")
                .build();
        ModelDefinition outer = new ModelDefinitionBuilder()
                .name("A") // same name as inner → circular
                .stock("S", 0, "Thing")
                .module("sub", inner, Map.of(), Map.of())
                .build();
        List<String> errors = DefinitionValidator.validate(outer);
        assertTrue(errors.stream().anyMatch(e -> e.contains("Circular")));
    }

    @Test
    void shouldAllowNullSourceAndSinkForFlows() {
        ModelDefinition def = new ModelDefinitionBuilder()
                .name("Open")
                .flow("Inflow", "10", "Day", null, null)
                .build();
        List<String> errors = DefinitionValidator.validate(def);
        assertTrue(errors.isEmpty(), "Null source/sink should be allowed: " + errors);
    }

    @Test
    void shouldDetectNonRootCircularModuleReference() {
        // B contains C, C contains B — cycle that doesn't involve the root "Outer"
        ModelDefinition defC = new ModelDefinitionBuilder()
                .name("C")
                .stock("S", 0, "Thing")
                .build();
        // B contains C — but we need C to contain B to form a cycle.
        // Since ModelDefinition is immutable, we build them with matching names:
        // B_inner contains a module named "C" whose definition is named "B" (same as B_inner).
        ModelDefinition cycleBack = new ModelDefinitionBuilder()
                .name("B")
                .stock("S2", 0, "Thing")
                .build();
        ModelDefinition defCWithB = new ModelDefinitionBuilder()
                .name("C")
                .stock("S", 0, "Thing")
                .module("back", cycleBack, Map.of(), Map.of())
                .build();
        ModelDefinition defB = new ModelDefinitionBuilder()
                .name("B")
                .stock("S", 0, "Thing")
                .module("goToC", defCWithB, Map.of(), Map.of())
                .build();
        ModelDefinition outer = new ModelDefinitionBuilder()
                .name("Outer")
                .module("b", defB, Map.of(), Map.of())
                .build();

        List<String> errors = DefinitionValidator.validate(outer);
        assertTrue(errors.stream().anyMatch(e -> e.contains("Circular")),
                "Should detect B→C→B cycle even though root is 'Outer': " + errors);
    }

    @Test
    void shouldDetectBadModuleInputBinding() {
        ModelDefinition moduleDef = new ModelDefinitionBuilder()
                .name("Module")
                .moduleInterface(new ModuleInterface(
                        List.of(new PortDef("input1", "Person")),
                        List.of()))
                .stock("S", 0, "Person")
                .build();
        ModelDefinition outer = new ModelDefinitionBuilder()
                .name("Outer")
                .module("m1", moduleDef,
                        Map.of("nonExistentPort", "10"), // bad port name
                        Map.of())
                .build();
        List<String> errors = DefinitionValidator.validate(outer);
        assertTrue(errors.stream().anyMatch(e -> e.contains("non-existent input port")));
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
                .flow("Recovery", "Infectious * Recovery_Rate", "Day", "Infectious", "Recovered")
                .constant("Contact_Rate", 8.0, "Dimensionless")
                .constant("Infectivity", 0.10, "Dimensionless")
                .constant("Recovery_Rate", 0.20, "Dimensionless")
                .defaultSimulation("Day", 56, "Day")
                .build();
    }
}
