package com.deathrayresearch.forrester.model.graph;

import com.deathrayresearch.forrester.model.def.ElementPlacement;
import com.deathrayresearch.forrester.model.def.ModelDefinition;
import com.deathrayresearch.forrester.model.def.ModelDefinitionBuilder;
import com.deathrayresearch.forrester.model.def.ViewDef;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("AutoLayout")
class AutoLayoutTest {

    @Test
    void shouldPlaceAllElements() {
        ModelDefinition def = new ModelDefinitionBuilder()
                .name("Full Model")
                .stock("S1", 100, "Thing")
                .stock("S2", 50, "Thing")
                .flow("F1", "S1 * 0.1", "Day", "S1", "S2")
                .aux("A1", "S1 + S2", "Thing")
                .constant("C1", 5, "Thing")
                .build();

        ViewDef view = AutoLayout.layout(def);

        assertNotNull(view);
        assertEquals("Auto Layout", view.name());

        Set<String> placedNames = view.elements().stream()
                .map(ElementPlacement::name)
                .collect(Collectors.toSet());

        assertTrue(placedNames.contains("S1"), "S1 should be placed");
        assertTrue(placedNames.contains("S2"), "S2 should be placed");
        assertTrue(placedNames.contains("F1"), "F1 should be placed");
        assertTrue(placedNames.contains("A1"), "A1 should be placed");
        assertTrue(placedNames.contains("C1"), "C1 should be placed");
        assertEquals(5, view.elements().size());
    }

    @Test
    void shouldAssignCorrectTypes() {
        ModelDefinition def = new ModelDefinitionBuilder()
                .name("Types")
                .stock("S", 100, "Thing")
                .flow("F", "S", "Day", "S", null)
                .aux("A", "S * 2", "Thing")
                .constant("C", 1, "Thing")
                .build();

        ViewDef view = AutoLayout.layout(def);

        for (ElementPlacement p : view.elements()) {
            if (p.name().equals("S")) {
                assertEquals("stock", p.type());
            } else if (p.name().equals("F")) {
                assertEquals("flow", p.type());
            } else if (p.name().equals("A")) {
                assertEquals("aux", p.type());
            } else if (p.name().equals("C")) {
                assertEquals("constant", p.type());
            }
        }
    }

    @Test
    void shouldNotOverlapElements() {
        ModelDefinition def = new ModelDefinitionBuilder()
                .name("Overlap Test")
                .stock("S1", 100, "Thing")
                .stock("S2", 50, "Thing")
                .stock("S3", 25, "Thing")
                .constant("C1", 1, "Thing")
                .constant("C2", 2, "Thing")
                .aux("A1", "S1", "Thing")
                .aux("A2", "S2", "Thing")
                .build();

        ViewDef view = AutoLayout.layout(def);

        List<ElementPlacement> elements = view.elements();
        // Check that no two elements share the same (x, y) position
        Set<String> positions = new HashSet<>();
        for (ElementPlacement p : elements) {
            String pos = p.x() + "," + p.y();
            assertTrue(positions.add(pos),
                    "Elements should not overlap: duplicate position " + pos + " for " + p.name());
        }
    }

    @Test
    void shouldLayerElementsByType() {
        ModelDefinition def = new ModelDefinitionBuilder()
                .name("Layers")
                .stock("S", 100, "Thing")
                .flow("F", "S", "Day", "S", null)
                .aux("A", "S", "Thing")
                .constant("C", 1, "Thing")
                .build();

        ViewDef view = AutoLayout.layout(def);

        double stockY = -1, flowY = -1, auxY = -1, constantY = -1;
        for (ElementPlacement p : view.elements()) {
            if (p.name().equals("S")) {
                stockY = p.y();
            } else if (p.name().equals("F")) {
                flowY = p.y();
            } else if (p.name().equals("A")) {
                auxY = p.y();
            } else if (p.name().equals("C")) {
                constantY = p.y();
            }
        }

        // Auxiliaries should be above stocks, constants below
        assertTrue(auxY < stockY, "Auxiliaries should be above stocks (lower y)");
        assertTrue(constantY > stockY, "Constants should be below stocks (higher y)");
    }

    @Test
    void shouldGenerateConnectors() {
        ModelDefinition def = new ModelDefinitionBuilder()
                .name("WithConnectors")
                .stock("S", 100, "Thing")
                .constant("Rate", 0.1, "Dimensionless unit")
                .flow("F", "S * Rate", "Day", "S", null)
                .build();

        ViewDef view = AutoLayout.layout(def);

        assertFalse(view.connectors().isEmpty(), "Should generate connectors from dependencies");
    }

    @Test
    void shouldHandleEmptyModel() {
        ModelDefinition def = new ModelDefinitionBuilder()
                .name("Empty")
                .build();

        ViewDef view = AutoLayout.layout(def);

        assertNotNull(view);
        assertTrue(view.elements().isEmpty());
        assertTrue(view.connectors().isEmpty());
    }

    @Test
    void shouldPlaceLookupTables() {
        ModelDefinition def = new ModelDefinitionBuilder()
                .name("WithLookup")
                .stock("S", 100, "Thing")
                .lookupTable("Effect", new double[]{0, 50, 100}, new double[]{0, 0.5, 1}, "LINEAR")
                .build();

        ViewDef view = AutoLayout.layout(def);

        Set<String> placedNames = view.elements().stream()
                .map(ElementPlacement::name)
                .collect(Collectors.toSet());

        assertTrue(placedNames.contains("Effect"), "Lookup table should be placed");

        ElementPlacement lookupPlacement = view.elements().stream()
                .filter(p -> p.name().equals("Effect"))
                .findFirst().orElseThrow();
        assertEquals("lookup", lookupPlacement.type());
    }
}
