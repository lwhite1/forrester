package com.deathrayresearch.forrester.model.graph;

import com.deathrayresearch.forrester.model.def.ConnectorRoute;
import com.deathrayresearch.forrester.model.def.ElementPlacement;
import com.deathrayresearch.forrester.model.def.ModelDefinition;
import com.deathrayresearch.forrester.model.def.ModelDefinitionBuilder;
import com.deathrayresearch.forrester.model.def.ViewDef;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("ViewValidator")
class ViewValidatorTest {

    @Test
    void shouldPassValidView() {
        ModelDefinition def = new ModelDefinitionBuilder()
                .name("Test")
                .stock("Tank", 100, "Thing")
                .constant("Rate", 0.1, "Dimensionless unit")
                .flow("Drain", "Tank * Rate", "Day", "Tank", null)
                .build();

        ViewDef view = new ViewDef("Main",
                List.of(
                        new ElementPlacement("Tank", "stock", 100, 200),
                        new ElementPlacement("Rate", "constant", 100, 350),
                        new ElementPlacement("Drain", "flow", 175, 200)
                ),
                List.of(
                        new ConnectorRoute("Tank", "Drain"),
                        new ConnectorRoute("Rate", "Drain")
                ),
                List.of());

        List<String> errors = ViewValidator.validate(view, def);
        assertTrue(errors.isEmpty(), "Valid view should have no errors: " + errors);
    }

    @Test
    void shouldDetectNonExistentElementPlacement() {
        ModelDefinition def = new ModelDefinitionBuilder()
                .name("Test")
                .stock("Tank", 100, "Thing")
                .build();

        ViewDef view = new ViewDef("Main",
                List.of(
                        new ElementPlacement("Tank", "stock", 100, 200),
                        new ElementPlacement("Ghost", "aux", 200, 200)
                ),
                List.of(),
                List.of());

        List<String> errors = ViewValidator.validate(view, def);
        assertEquals(1, errors.size());
        assertTrue(errors.get(0).contains("Ghost"), "Should mention the non-existent element");
    }

    @Test
    void shouldDetectNonExistentConnectorFrom() {
        ModelDefinition def = new ModelDefinitionBuilder()
                .name("Test")
                .stock("Tank", 100, "Thing")
                .flow("Drain", "Tank * 0.1", "Day", "Tank", null)
                .build();

        ViewDef view = new ViewDef("Main",
                List.of(
                        new ElementPlacement("Tank", "stock", 100, 200),
                        new ElementPlacement("Drain", "flow", 175, 200)
                ),
                List.of(new ConnectorRoute("NonExistent", "Drain")),
                List.of());

        List<String> errors = ViewValidator.validate(view, def);
        assertEquals(1, errors.size());
        assertTrue(errors.get(0).contains("NonExistent"));
    }

    @Test
    void shouldDetectNonExistentConnectorTo() {
        ModelDefinition def = new ModelDefinitionBuilder()
                .name("Test")
                .stock("Tank", 100, "Thing")
                .build();

        ViewDef view = new ViewDef("Main",
                List.of(new ElementPlacement("Tank", "stock", 100, 200)),
                List.of(new ConnectorRoute("Tank", "Missing")),
                List.of());

        List<String> errors = ViewValidator.validate(view, def);
        assertEquals(1, errors.size());
        assertTrue(errors.get(0).contains("Missing"));
    }

    @Test
    void shouldDetectMultipleErrors() {
        ModelDefinition def = new ModelDefinitionBuilder()
                .name("Test")
                .stock("S", 100, "Thing")
                .build();

        ViewDef view = new ViewDef("Main",
                List.of(
                        new ElementPlacement("S", "stock", 100, 200),
                        new ElementPlacement("A", "aux", 200, 100),
                        new ElementPlacement("B", "constant", 200, 300)
                ),
                List.of(new ConnectorRoute("X", "Y")),
                List.of());

        List<String> errors = ViewValidator.validate(view, def);
        // A, B placements + X, Y connector endpoints = 4 errors
        assertEquals(4, errors.size());
    }

    @Test
    void shouldPassEmptyView() {
        ModelDefinition def = new ModelDefinitionBuilder()
                .name("Test")
                .stock("S", 100, "Thing")
                .build();

        ViewDef view = new ViewDef("Empty View", List.of(), List.of(), List.of());

        List<String> errors = ViewValidator.validate(view, def);
        assertTrue(errors.isEmpty());
    }

    @Test
    void shouldValidateViewThroughDefinitionValidator() {
        // Test that DefinitionValidator delegates to ViewValidator
        ModelDefinition def = new ModelDefinitionBuilder()
                .name("Test")
                .stock("S", 100, "Thing")
                .view(new ViewDef("Bad View",
                        List.of(new ElementPlacement("Ghost", "stock", 100, 200)),
                        List.of(),
                        List.of()))
                .build();

        List<String> errors = com.deathrayresearch.forrester.model.def.DefinitionValidator.validate(def);
        assertFalse(errors.isEmpty(), "DefinitionValidator should report view errors");
        assertTrue(errors.stream().anyMatch(e -> e.contains("Ghost")));
    }
}
