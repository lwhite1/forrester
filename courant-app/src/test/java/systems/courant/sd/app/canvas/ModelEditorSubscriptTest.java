package systems.courant.sd.app.canvas;

import systems.courant.sd.model.def.FlowDef;
import systems.courant.sd.model.def.ModelDefinition;
import systems.courant.sd.model.def.ModelDefinitionBuilder;
import systems.courant.sd.model.def.StockDef;
import systems.courant.sd.model.def.SubscriptDef;
import systems.courant.sd.model.def.VariableDef;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for subscript-related features of {@link ModelEditor}:
 * subscript preservation on stock setters, subscript assignment,
 * subscript definition management, copy-paste with subscripts,
 * and element subscript queries.
 */
@DisplayName("ModelEditor — Subscripts")
class ModelEditorSubscriptTest {

    private ModelEditor editor;

    @BeforeEach
    void setUp() {
        editor = new ModelEditor();
    }

    @Nested
    @DisplayName("subscript preservation on stock setters")
    class StockSubscriptPreservation {

        @Test
        void shouldPreserveSubscriptsOnSetInitialValue() {
            ModelDefinition def = new ModelDefinitionBuilder()
                    .name("Test")
                    .subscript("Region", List.of("North", "South"))
                    .stock("Pop", 100, "Person", List.of("Region"))
                    .build();
            editor.loadFrom(def);
            editor.setStockInitialValue("Pop", 200);
            StockDef updated = editor.getStockByName("Pop").orElseThrow();
            assertThat(updated.initialValue()).isEqualTo(200);
            assertThat(updated.subscripts()).containsExactly("Region");
        }

        @Test
        void shouldPreserveSubscriptsOnSetUnit() {
            ModelDefinition def = new ModelDefinitionBuilder()
                    .name("Test")
                    .subscript("Region", List.of("North", "South"))
                    .stock("Pop", 100, "Person", List.of("Region"))
                    .build();
            editor.loadFrom(def);
            editor.setStockUnit("Pop", "Animal");
            StockDef updated = editor.getStockByName("Pop").orElseThrow();
            assertThat(updated.unit()).isEqualTo("Animal");
            assertThat(updated.subscripts()).containsExactly("Region");
        }

        @Test
        void shouldPreserveSubscriptsOnSetNegativeValuePolicy() {
            ModelDefinition def = new ModelDefinitionBuilder()
                    .name("Test")
                    .subscript("Region", List.of("North", "South"))
                    .stock("Pop", 100, "Person", List.of("Region"))
                    .build();
            editor.loadFrom(def);
            editor.setStockNegativeValuePolicy("Pop", "ALLOW");
            StockDef updated = editor.getStockByName("Pop").orElseThrow();
            assertThat(updated.negativeValuePolicy()).isEqualTo("ALLOW");
            assertThat(updated.subscripts()).containsExactly("Region");
        }

        @Test
        void shouldPreserveSubscriptsOnSetComment() {
            ModelDefinition def = new ModelDefinitionBuilder()
                    .name("Test")
                    .subscript("Region", List.of("North", "South"))
                    .stock("Pop", 100, "Person", List.of("Region"))
                    .build();
            editor.loadFrom(def);
            editor.setStockComment("Pop", "A comment");
            StockDef updated = editor.getStockByName("Pop").orElseThrow();
            assertThat(updated.comment()).isEqualTo("A comment");
            assertThat(updated.subscripts()).containsExactly("Region");
        }
    }

    @Nested
    @DisplayName("subscript assignment setters")
    class SubscriptAssignment {

        @Test
        void shouldSetStockSubscripts() {
            editor.loadFrom(new ModelDefinitionBuilder().name("Test")
                    .stock("S", 0, "units").build());
            boolean updated = editor.setStockSubscripts("S", List.of("Region"));
            assertThat(updated).isTrue();
            assertThat(editor.getStockByName("S").orElseThrow().subscripts())
                    .containsExactly("Region");
        }

        @Test
        void shouldSetFlowSubscripts() {
            editor.loadFrom(new ModelDefinitionBuilder().name("Test")
                    .flow("F", "0", "Day", null, null).build());
            boolean updated = editor.setFlowSubscripts("F", List.of("Age"));
            assertThat(updated).isTrue();
            assertThat(editor.getFlowByName("F").orElseThrow().subscripts())
                    .containsExactly("Age");
        }

        @Test
        void shouldSetVariableSubscripts() {
            editor.loadFrom(new ModelDefinitionBuilder().name("Test")
                    .variable("V", "0", "units").build());
            boolean updated = editor.setVariableSubscripts("V", List.of("Region", "Age"));
            assertThat(updated).isTrue();
            assertThat(editor.getVariableByName("V").orElseThrow().subscripts())
                    .containsExactly("Region", "Age");
        }

        @Test
        void shouldReturnFalseForNonexistentElement() {
            editor.loadFrom(new ModelDefinitionBuilder().name("Test").build());
            assertThat(editor.setStockSubscripts("Missing", List.of("X"))).isFalse();
            assertThat(editor.setFlowSubscripts("Missing", List.of("X"))).isFalse();
            assertThat(editor.setVariableSubscripts("Missing", List.of("X"))).isFalse();
        }
    }

    @Nested
    @DisplayName("subscript definition management")
    class SubscriptDefinitionManagement {

        @Test
        void shouldAddSubscriptDef() {
            editor.loadFrom(new ModelDefinitionBuilder().name("Test").build());
            editor.addSubscript(new SubscriptDef("Region", List.of("N", "S")));
            assertThat(editor.getSubscripts()).hasSize(1);
            assertThat(editor.getSubscripts().get(0).name()).isEqualTo("Region");
            assertThat(editor.getSubscripts().get(0).labels()).containsExactly("N", "S");
        }

        @Test
        void shouldUpdateSubscriptDef() {
            editor.loadFrom(new ModelDefinitionBuilder().name("Test")
                    .subscript("Region", List.of("N", "S")).build());
            boolean updated = editor.updateSubscript("Region",
                    new SubscriptDef("Region", List.of("N", "S", "E", "W")));
            assertThat(updated).isTrue();
            assertThat(editor.getSubscripts().get(0).labels())
                    .containsExactly("N", "S", "E", "W");
        }

        @Test
        void shouldRemoveSubscriptDef() {
            editor.loadFrom(new ModelDefinitionBuilder().name("Test")
                    .subscript("Region", List.of("N", "S")).build());
            boolean removed = editor.removeSubscript("Region");
            assertThat(removed).isTrue();
            assertThat(editor.getSubscripts()).isEmpty();
        }

        @Test
        void shouldReturnFalseWhenRemovingNonexistentSubscript() {
            editor.loadFrom(new ModelDefinitionBuilder().name("Test").build());
            assertThat(editor.removeSubscript("Missing")).isFalse();
        }

        @Test
        void shouldReturnFalseWhenUpdatingNonexistentSubscript() {
            editor.loadFrom(new ModelDefinitionBuilder().name("Test").build());
            assertThat(editor.updateSubscript("Missing",
                    new SubscriptDef("Missing", List.of("A")))).isFalse();
        }
    }

    @Nested
    @DisplayName("copy-paste preserves subscripts")
    class CopyPasteSubscripts {

        @Test
        void shouldPreserveSubscriptsOnAddStockFrom() {
            editor.loadFrom(new ModelDefinitionBuilder().name("Test").build());
            StockDef template = new StockDef("Pop", null, 100, "Person",
                    null, List.of("Region"));
            String name = editor.addStockFrom(template);
            StockDef copy = editor.getStockByName(name).orElseThrow();
            assertThat(copy.subscripts()).containsExactly("Region");
        }

        @Test
        void shouldPreserveSubscriptsOnAddFlowFrom() {
            editor.loadFrom(new ModelDefinitionBuilder().name("Test").build());
            FlowDef template = new FlowDef("Migration", null, "0.1",
                    "Year", "Person", null, null, List.of("Region"));
            String name = editor.addFlowFrom(template, null, null);
            FlowDef copy = editor.getFlowByName(name).orElseThrow();
            assertThat(copy.subscripts()).containsExactly("Region");
        }

        @Test
        void shouldPreserveSubscriptsOnAddVariableFrom() {
            editor.loadFrom(new ModelDefinitionBuilder().name("Test").build());
            VariableDef template = new VariableDef("Rate", null, "0.5",
                    "1/Year", List.of("Region"));
            String name = editor.addVariableFrom(template, template.equation());
            VariableDef copy = editor.getVariableByName(name).orElseThrow();
            assertThat(copy.subscripts()).containsExactly("Region");
        }
    }

    @Nested
    @DisplayName("getElementSubscripts")
    class GetElementSubscripts {

        @Test
        void shouldReturnSubscriptsForSubscriptedStock() {
            editor.loadFrom(new ModelDefinitionBuilder().name("Test")
                    .subscript("Region", List.of("N", "S"))
                    .stock("Pop", 100, "Person", List.of("Region"))
                    .build());
            assertThat(editor.getElementSubscripts("Pop")).containsExactly("Region");
        }

        @Test
        void shouldReturnSubscriptsForSubscriptedFlow() {
            editor.loadFrom(new ModelDefinitionBuilder().name("Test")
                    .subscript("Region", List.of("N", "S"))
                    .flow("Migration", "0", "Year", null, null, List.of("Region"))
                    .build());
            assertThat(editor.getElementSubscripts("Migration")).containsExactly("Region");
        }

        @Test
        void shouldReturnEmptyListForScalarElement() {
            editor.loadFrom(new ModelDefinitionBuilder().name("Test")
                    .stock("S", 0, "units").build());
            assertThat(editor.getElementSubscripts("S")).isEmpty();
        }

        @Test
        void shouldReturnEmptyListForUnknownElement() {
            editor.loadFrom(new ModelDefinitionBuilder().name("Test").build());
            assertThat(editor.getElementSubscripts("Missing")).isEmpty();
        }
    }
}
