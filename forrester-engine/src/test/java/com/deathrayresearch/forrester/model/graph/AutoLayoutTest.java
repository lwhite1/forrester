package com.deathrayresearch.forrester.model.graph;

import com.deathrayresearch.forrester.model.def.ElementPlacement;
import com.deathrayresearch.forrester.model.def.ElementType;
import com.deathrayresearch.forrester.model.def.ModelDefinition;
import com.deathrayresearch.forrester.model.def.ModelDefinitionBuilder;
import com.deathrayresearch.forrester.model.def.ViewDef;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

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

        assertThat(view).isNotNull();
        assertThat(view.name()).isEqualTo("Auto Layout");

        Set<String> placedNames = view.elements().stream()
                .map(ElementPlacement::name)
                .collect(Collectors.toSet());

        assertThat(placedNames.contains("S1")).as("S1 should be placed").isTrue();
        assertThat(placedNames.contains("S2")).as("S2 should be placed").isTrue();
        assertThat(placedNames.contains("F1")).as("F1 should be placed").isTrue();
        assertThat(placedNames.contains("A1")).as("A1 should be placed").isTrue();
        assertThat(placedNames.contains("C1")).as("C1 should be placed").isTrue();
        assertThat(view.elements().size()).isEqualTo(5);
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
                assertThat(p.type()).isEqualTo(ElementType.STOCK);
            } else if (p.name().equals("F")) {
                assertThat(p.type()).isEqualTo(ElementType.FLOW);
            } else if (p.name().equals("A")) {
                assertThat(p.type()).isEqualTo(ElementType.AUX);
            } else if (p.name().equals("C")) {
                assertThat(p.type()).isEqualTo(ElementType.CONSTANT);
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
            assertThat(positions.add(pos))
                    .as("Elements should not overlap: duplicate position " + pos + " for " + p.name()).isTrue();
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
        assertThat(auxY < stockY).as("Auxiliaries should be above stocks (lower y)").isTrue();
        assertThat(constantY > stockY).as("Constants should be below stocks (higher y)").isTrue();
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

        assertThat(view.connectors().isEmpty()).as("Should generate connectors from dependencies").isFalse();
    }

    @Test
    void shouldHandleEmptyModel() {
        ModelDefinition def = new ModelDefinitionBuilder()
                .name("Empty")
                .build();

        ViewDef view = AutoLayout.layout(def);

        assertThat(view).isNotNull();
        assertThat(view.elements().isEmpty()).isTrue();
        assertThat(view.connectors().isEmpty()).isTrue();
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

        assertThat(placedNames.contains("Effect")).as("Lookup table should be placed").isTrue();

        ElementPlacement lookupPlacement = view.elements().stream()
                .filter(p -> p.name().equals("Effect"))
                .findFirst().orElseThrow();
        assertThat(lookupPlacement.type()).isEqualTo(ElementType.LOOKUP);
    }
}
