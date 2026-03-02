package com.deathrayresearch.forrester.app.canvas;

import com.deathrayresearch.forrester.model.def.FlowDef;
import com.deathrayresearch.forrester.model.def.ModelDefinition;
import com.deathrayresearch.forrester.model.def.ModelDefinitionBuilder;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("ModelEditor")
class ModelEditorTest {

    private ModelEditor editor;

    @BeforeEach
    void setUp() {
        editor = new ModelEditor();
    }

    @Nested
    @DisplayName("loadFrom")
    class LoadFrom {

        @Test
        void shouldLoadStocksAndFlowsFromDefinition() {
            ModelDefinition def = new ModelDefinitionBuilder()
                    .name("Test")
                    .stock("S", 100, "units")
                    .flow("F", "S * 0.1", "day", "S", null)
                    .build();

            editor.loadFrom(def);

            assertThat(editor.getStocks()).hasSize(1);
            assertThat(editor.getStocks().get(0).name()).isEqualTo("S");
            assertThat(editor.getFlows()).hasSize(1);
            assertThat(editor.getFlows().get(0).name()).isEqualTo("F");
            assertThat(editor.getModelName()).isEqualTo("Test");
        }

        @Test
        void shouldLoadAllElementTypes() {
            ModelDefinition def = new ModelDefinitionBuilder()
                    .name("Full")
                    .stock("S1", 0, "u")
                    .flow("F1", "0", "day", null, null)
                    .aux("A1", "S1", "u")
                    .constant("C1", 5, "u")
                    .build();

            editor.loadFrom(def);

            assertThat(editor.getStocks()).hasSize(1);
            assertThat(editor.getFlows()).hasSize(1);
            assertThat(editor.getAuxiliaries()).hasSize(1);
            assertThat(editor.getConstants()).hasSize(1);
        }

        @Test
        void shouldClearPreviousStateOnReload() {
            ModelDefinition def1 = new ModelDefinitionBuilder()
                    .name("First")
                    .stock("A", 0, "u")
                    .build();
            editor.loadFrom(def1);

            ModelDefinition def2 = new ModelDefinitionBuilder()
                    .name("Second")
                    .constant("B", 1, "u")
                    .build();
            editor.loadFrom(def2);

            assertThat(editor.getStocks()).isEmpty();
            assertThat(editor.getConstants()).hasSize(1);
            assertThat(editor.getModelName()).isEqualTo("Second");
        }
    }

    @Nested
    @DisplayName("add elements")
    class AddElements {

        @Test
        void shouldAutoNameStocks() {
            String name1 = editor.addStock();
            String name2 = editor.addStock();

            assertThat(name1).isEqualTo("Stock 1");
            assertThat(name2).isEqualTo("Stock 2");
            assertThat(editor.getStocks()).hasSize(2);
        }

        @Test
        void shouldAutoNameFlows() {
            String name = editor.addFlow();

            assertThat(name).startsWith("Flow ");
            assertThat(editor.getFlows()).hasSize(1);
            assertThat(editor.getFlows().get(0).source()).isNull();
            assertThat(editor.getFlows().get(0).sink()).isNull();
        }

        @Test
        void shouldAutoNameAuxiliaries() {
            String name = editor.addAux();

            assertThat(name).startsWith("Aux ");
            assertThat(editor.getAuxiliaries()).hasSize(1);
        }

        @Test
        void shouldAutoNameConstants() {
            String name = editor.addConstant();

            assertThat(name).startsWith("Constant ");
            assertThat(editor.getConstants()).hasSize(1);
        }

        @Test
        void shouldContinueNumberingAfterLoadFrom() {
            ModelDefinition def = new ModelDefinitionBuilder()
                    .name("Test")
                    .stock("Stock 5", 0, "u")
                    .build();
            editor.loadFrom(def);

            String name = editor.addStock();

            assertThat(name).isEqualTo("Stock 6");
        }

        @Test
        void shouldUseUniqueIdsAcrossTypes() {
            editor.addStock();  // "Stock 1"
            String flowName = editor.addFlow();  // "Flow 2"
            String auxName = editor.addAux();  // "Aux 3"

            assertThat(flowName).isEqualTo("Flow 2");
            assertThat(auxName).isEqualTo("Aux 3");
        }
    }

    @Nested
    @DisplayName("removeElement")
    class RemoveElement {

        @Test
        void shouldRemoveStock() {
            editor.addStock();  // Stock 1
            assertThat(editor.getStocks()).hasSize(1);

            editor.removeElement("Stock 1");

            assertThat(editor.getStocks()).isEmpty();
        }

        @Test
        void shouldRemoveFlow() {
            editor.addFlow();
            String name = editor.getFlows().get(0).name();

            editor.removeElement(name);

            assertThat(editor.getFlows()).isEmpty();
        }

        @Test
        void shouldRemoveAux() {
            editor.addAux();
            String name = editor.getAuxiliaries().get(0).name();

            editor.removeElement(name);

            assertThat(editor.getAuxiliaries()).isEmpty();
        }

        @Test
        void shouldRemoveConstant() {
            editor.addConstant();
            String name = editor.getConstants().get(0).name();

            editor.removeElement(name);

            assertThat(editor.getConstants()).isEmpty();
        }

        @Test
        void shouldNullifyFlowSourceWhenStockDeleted() {
            ModelDefinition def = new ModelDefinitionBuilder()
                    .name("Test")
                    .stock("Tank", 100, "liters")
                    .flow("Drain", "Tank * 0.1", "day", "Tank", null)
                    .build();
            editor.loadFrom(def);

            editor.removeElement("Tank");

            assertThat(editor.getStocks()).isEmpty();
            assertThat(editor.getFlows()).hasSize(1);
            FlowDef flow = editor.getFlows().get(0);
            assertThat(flow.source()).isNull();
            assertThat(flow.sink()).isNull();
        }

        @Test
        void shouldNullifyFlowSinkWhenStockDeleted() {
            ModelDefinition def = new ModelDefinitionBuilder()
                    .name("Test")
                    .stock("Tank", 0, "liters")
                    .flow("Fill", "10", "day", null, "Tank")
                    .build();
            editor.loadFrom(def);

            editor.removeElement("Tank");

            FlowDef flow = editor.getFlows().get(0);
            assertThat(flow.source()).isNull();
            assertThat(flow.sink()).isNull();
        }

        @Test
        void shouldNullifyBothSourceAndSinkWhenStockDeleted() {
            ModelDefinition def = new ModelDefinitionBuilder()
                    .name("Test")
                    .stock("A", 100, "u")
                    .stock("B", 0, "u")
                    .flow("Transfer", "A * 0.5", "day", "A", "B")
                    .build();
            editor.loadFrom(def);

            editor.removeElement("A");

            FlowDef flow = editor.getFlows().get(0);
            assertThat(flow.source()).isNull();
            assertThat(flow.sink()).isEqualTo("B");
        }

        @Test
        void shouldDoNothingForNonexistentElement() {
            editor.addStock();
            editor.removeElement("nonexistent");

            assertThat(editor.getStocks()).hasSize(1);
        }
    }

    @Nested
    @DisplayName("toModelDefinition")
    class ToModelDefinition {

        @Test
        void shouldRoundTripFromDefinition() {
            ModelDefinition original = new ModelDefinitionBuilder()
                    .name("SIR")
                    .stock("S", 1000, "people")
                    .stock("I", 10, "people")
                    .constant("k", 0.5, "1/day")
                    .flow("Infection", "S * k", "day", "S", "I")
                    .build();

            editor.loadFrom(original);
            ModelDefinition rebuilt = editor.toModelDefinition();

            assertThat(rebuilt.name()).isEqualTo("SIR");
            assertThat(rebuilt.stocks()).hasSize(2);
            assertThat(rebuilt.flows()).hasSize(1);
            assertThat(rebuilt.constants()).hasSize(1);
        }

        @Test
        void shouldReflectMutations() {
            editor.addStock();
            editor.addConstant();

            ModelDefinition def = editor.toModelDefinition();

            assertThat(def.stocks()).hasSize(1);
            assertThat(def.constants()).hasSize(1);
        }
    }
}
