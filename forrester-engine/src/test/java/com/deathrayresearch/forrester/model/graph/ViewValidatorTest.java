package com.deathrayresearch.forrester.model.graph;

import com.deathrayresearch.forrester.model.def.ConnectorRoute;
import com.deathrayresearch.forrester.model.def.ElementPlacement;
import com.deathrayresearch.forrester.model.def.FlowRoute;
import com.deathrayresearch.forrester.model.def.ModelDefinition;
import com.deathrayresearch.forrester.model.def.ModelDefinitionBuilder;
import com.deathrayresearch.forrester.model.def.ViewDef;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

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
        assertThat(errors).as("Valid view should have no errors").isEmpty();
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
        assertThat(errors).hasSize(1);
        assertThat(errors.get(0)).contains("Ghost");
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
        assertThat(errors).hasSize(1);
        assertThat(errors.get(0)).contains("NonExistent");
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
        assertThat(errors).hasSize(1);
        assertThat(errors.get(0)).contains("Missing");
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
        assertThat(errors).hasSize(4);
    }

    @Test
    void shouldPassEmptyView() {
        ModelDefinition def = new ModelDefinitionBuilder()
                .name("Test")
                .stock("S", 100, "Thing")
                .build();

        ViewDef view = new ViewDef("Empty View", List.of(), List.of(), List.of());

        List<String> errors = ViewValidator.validate(view, def);
        assertThat(errors).isEmpty();
    }

    @Test
    void shouldDetectNonExistentFlowRoute() {
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
                List.of(),
                List.of(new FlowRoute("NonExistentFlow", List.of())));

        List<String> errors = ViewValidator.validate(view, def);
        assertThat(errors).hasSize(1);
        assertThat(errors.get(0)).contains("NonExistentFlow");
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
        assertThat(errors).as("DefinitionValidator should report view errors").isNotEmpty();
        assertThat(errors).anyMatch(e -> e.contains("Ghost"));
    }
}
