package com.deathrayresearch.forrester.app.canvas;

import com.deathrayresearch.forrester.model.def.AuxDef;
import com.deathrayresearch.forrester.model.def.ElementPlacement;
import com.deathrayresearch.forrester.model.def.ElementType;
import com.deathrayresearch.forrester.model.def.FlowDef;
import com.deathrayresearch.forrester.model.def.ModelDefinition;
import com.deathrayresearch.forrester.model.def.ModelDefinitionBuilder;
import com.deathrayresearch.forrester.model.def.ViewDef;

import java.util.List;

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

        @Test
        void shouldCleanEquationReferencesWhenConstantDeleted() {
            ModelDefinition def = new ModelDefinitionBuilder()
                    .name("Test")
                    .stock("S", 100, "u")
                    .constant("Rate", 0.5, "1/day")
                    .flow("Drain", "S * Rate", "day", "S", null)
                    .build();
            editor.loadFrom(def);

            editor.removeElement("Rate");

            FlowDef flow = editor.getFlows().get(0);
            assertThat(flow.equation()).isEqualTo("S * 0");
        }

        @Test
        void shouldCleanEquationReferencesWhenStockDeleted() {
            ModelDefinition def = new ModelDefinitionBuilder()
                    .name("Test")
                    .stock("Population", 1000, "people")
                    .constant("k", 0.1, "1/day")
                    .flow("Growth", "Population * k", "day", "Population", null)
                    .build();
            editor.loadFrom(def);

            editor.removeElement("Population");

            FlowDef flow = editor.getFlows().get(0);
            assertThat(flow.equation()).isEqualTo("0 * k");
        }
    }

    @Nested
    @DisplayName("addFlow with source/sink")
    class AddFlowWithConnections {

        @Test
        void shouldCreateFlowWithSourceAndSink() {
            ModelDefinition def = new ModelDefinitionBuilder()
                    .name("Test")
                    .stock("A", 100, "u")
                    .stock("B", 0, "u")
                    .build();
            editor.loadFrom(def);

            String name = editor.addFlow("A", "B");

            assertThat(editor.getFlows()).hasSize(1);
            FlowDef flow = editor.getFlows().get(0);
            assertThat(flow.name()).isEqualTo(name);
            assertThat(flow.source()).isEqualTo("A");
            assertThat(flow.sink()).isEqualTo("B");
        }

        @Test
        void shouldCreateFlowWithCloudSource() {
            String name = editor.addFlow(null, "Tank");

            FlowDef flow = editor.getFlows().get(0);
            assertThat(flow.source()).isNull();
            assertThat(flow.sink()).isEqualTo("Tank");
        }

        @Test
        void shouldCreateFlowWithCloudSink() {
            String name = editor.addFlow("Tank", null);

            FlowDef flow = editor.getFlows().get(0);
            assertThat(flow.source()).isEqualTo("Tank");
            assertThat(flow.sink()).isNull();
        }
    }

    @Nested
    @DisplayName("renameElement")
    class RenameElement {

        @Test
        void shouldRenameStock() {
            editor.addStock(); // "Stock 1"

            boolean result = editor.renameElement("Stock 1", "Population");

            assertThat(result).isTrue();
            assertThat(editor.getStocks().get(0).name()).isEqualTo("Population");
        }

        @Test
        void shouldRenameFlow() {
            editor.addFlow(); // "Flow 1"

            boolean result = editor.renameElement("Flow 1", "Birth Rate");

            assertThat(result).isTrue();
            assertThat(editor.getFlows().get(0).name()).isEqualTo("Birth Rate");
        }

        @Test
        void shouldRenameAux() {
            editor.addAux();

            boolean result = editor.renameElement(editor.getAuxiliaries().get(0).name(), "Rate");

            assertThat(result).isTrue();
            assertThat(editor.getAuxiliaries().get(0).name()).isEqualTo("Rate");
        }

        @Test
        void shouldRenameConstant() {
            editor.addConstant();

            boolean result = editor.renameElement(editor.getConstants().get(0).name(), "k");

            assertThat(result).isTrue();
            assertThat(editor.getConstants().get(0).name()).isEqualTo("k");
        }

        @Test
        void shouldReturnFalseForNonexistentElement() {
            assertThat(editor.renameElement("ghost", "new")).isFalse();
        }

        @Test
        void shouldReturnFalseForSameName() {
            editor.addStock();
            assertThat(editor.renameElement("Stock 1", "Stock 1")).isFalse();
        }

        @Test
        void shouldUpdateFlowSourceReference() {
            ModelDefinition def = new ModelDefinitionBuilder()
                    .name("Test")
                    .stock("Tank", 100, "u")
                    .flow("Drain", "Tank * 0.1", "day", "Tank", null)
                    .build();
            editor.loadFrom(def);

            editor.renameElement("Tank", "Reservoir");

            FlowDef flow = editor.getFlows().get(0);
            assertThat(flow.source()).isEqualTo("Reservoir");
        }

        @Test
        void shouldUpdateFlowSinkReference() {
            ModelDefinition def = new ModelDefinitionBuilder()
                    .name("Test")
                    .stock("Tank", 0, "u")
                    .flow("Fill", "10", "day", null, "Tank")
                    .build();
            editor.loadFrom(def);

            editor.renameElement("Tank", "Basin");

            FlowDef flow = editor.getFlows().get(0);
            assertThat(flow.sink()).isEqualTo("Basin");
        }

        @Test
        void shouldUpdateEquationReferences() {
            ModelDefinition def = new ModelDefinitionBuilder()
                    .name("Test")
                    .stock("S", 1000, "people")
                    .constant("Contact Rate", 8, "c/p/d")
                    .flow("Infection", "Contact_Rate * S", "day", "S", null)
                    .build();
            editor.loadFrom(def);

            editor.renameElement("Contact Rate", "Beta");

            FlowDef flow = editor.getFlows().get(0);
            assertThat(flow.equation()).isEqualTo("Beta * S");
        }

        @Test
        void shouldRejectRenameToExistingName() {
            editor.addStock();  // "Stock 1"
            editor.addStock();  // "Stock 2"

            boolean result = editor.renameElement("Stock 1", "Stock 2");

            assertThat(result).isFalse();
            assertThat(editor.getStocks().get(0).name()).isEqualTo("Stock 1");
            assertThat(editor.getStocks().get(1).name()).isEqualTo("Stock 2");
        }

        @Test
        void shouldRejectRenameToExistingNameAcrossTypes() {
            editor.addStock();     // "Stock 1"
            editor.addConstant();  // "Constant 2"

            boolean result = editor.renameElement("Stock 1", "Constant 2");

            assertThat(result).isFalse();
            assertThat(editor.getStocks().get(0).name()).isEqualTo("Stock 1");
        }

        @Test
        void shouldUpdateEquationTokensWithWordBoundaries() {
            ModelDefinition def = new ModelDefinitionBuilder()
                    .name("Test")
                    .stock("S", 1000, "people")
                    .constant("I", 10, "people")
                    .flow("F", "I * Infectivity", "day", "S", null)
                    .aux("A", "I + 1", "u")
                    .build();
            editor.loadFrom(def);

            editor.renameElement("I", "Infectious");

            // "I" in "Infectivity" should NOT be replaced
            assertThat(editor.getFlows().get(0).equation()).isEqualTo("Infectious * Infectivity");
            assertThat(editor.getAuxiliaries().get(0).equation()).isEqualTo("Infectious + 1");
        }
    }

    @Nested
    @DisplayName("setConstantValue")
    class SetConstantValue {

        @Test
        void shouldUpdateConstantValue() {
            editor.addConstant();
            String name = editor.getConstants().get(0).name();

            boolean result = editor.setConstantValue(name, 42.5);

            assertThat(result).isTrue();
            assertThat(editor.getConstants().get(0).value()).isEqualTo(42.5);
        }

        @Test
        void shouldReturnFalseForNonexistentConstant() {
            assertThat(editor.setConstantValue("ghost", 1.0)).isFalse();
        }

        @Test
        void shouldPreserveNameAndUnit() {
            ModelDefinition def = new ModelDefinitionBuilder()
                    .name("Test")
                    .constant("k", 0.5, "1/day")
                    .build();
            editor.loadFrom(def);

            editor.setConstantValue("k", 0.75);

            assertThat(editor.getConstants().get(0).name()).isEqualTo("k");
            assertThat(editor.getConstants().get(0).unit()).isEqualTo("1/day");
            assertThat(editor.getConstants().get(0).value()).isEqualTo(0.75);
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

        @Test
        void shouldIncludeViewDefWhenProvided() {
            editor.addStock();

            ViewDef view = new ViewDef("Main", List.of(
                    new ElementPlacement("Stock 1", ElementType.STOCK, 100, 200)
            ), List.of(), List.of());

            ModelDefinition def = editor.toModelDefinition(view);

            assertThat(def.views()).hasSize(1);
            assertThat(def.views().get(0).name()).isEqualTo("Main");
            assertThat(def.views().get(0).elements()).hasSize(1);
        }

        @Test
        void shouldOmitViewsWhenNull() {
            editor.addStock();

            ModelDefinition def = editor.toModelDefinition(null);

            assertThat(def.views()).isEmpty();
        }
    }

    @Nested
    @DisplayName("isValidName")
    class IsValidName {

        @Test
        void shouldAcceptSimpleName() {
            assertThat(ModelEditor.isValidName("Stock 1")).isTrue();
        }

        @Test
        void shouldAcceptUnderscores() {
            assertThat(ModelEditor.isValidName("Contact_Rate")).isTrue();
        }

        @Test
        void shouldRejectBlank() {
            assertThat(ModelEditor.isValidName("")).isFalse();
            assertThat(ModelEditor.isValidName("   ")).isFalse();
        }

        @Test
        void shouldRejectNull() {
            assertThat(ModelEditor.isValidName(null)).isFalse();
        }

        @Test
        void shouldRejectSpecialCharacters() {
            assertThat(ModelEditor.isValidName("Rate*2")).isFalse();
            assertThat(ModelEditor.isValidName("a+b")).isFalse();
            assertThat(ModelEditor.isValidName("x/y")).isFalse();
        }
    }

    @Nested
    @DisplayName("setFlowEquation")
    class SetFlowEquation {

        @Test
        void shouldUpdateFlowEquation() {
            editor.addFlow();
            String name = editor.getFlows().get(0).name();

            boolean result = editor.setFlowEquation(name, "Stock_1 * 0.5");

            assertThat(result).isTrue();
            assertThat(editor.getFlows().get(0).equation()).isEqualTo("Stock_1 * 0.5");
        }

        @Test
        void shouldReturnFalseForNonexistentFlow() {
            assertThat(editor.setFlowEquation("ghost", "1")).isFalse();
        }

        @Test
        void shouldRejectBlankEquation() {
            editor.addFlow();
            String name = editor.getFlows().get(0).name();

            assertThat(editor.setFlowEquation(name, "")).isFalse();
            assertThat(editor.setFlowEquation(name, "   ")).isFalse();
            assertThat(editor.getFlows().get(0).equation()).isEqualTo("0");
        }

        @Test
        void shouldRejectNullEquation() {
            editor.addFlow();
            String name = editor.getFlows().get(0).name();

            assertThat(editor.setFlowEquation(name, null)).isFalse();
            assertThat(editor.getFlows().get(0).equation()).isEqualTo("0");
        }

        @Test
        void shouldPreserveOtherFields() {
            ModelDefinition def = new ModelDefinitionBuilder()
                    .name("Test")
                    .stock("S", 100, "u")
                    .flow("Drain", "S * 0.1", "day", "S", null)
                    .build();
            editor.loadFrom(def);

            editor.setFlowEquation("Drain", "S * 0.2");

            FlowDef flow = editor.getFlows().get(0);
            assertThat(flow.name()).isEqualTo("Drain");
            assertThat(flow.equation()).isEqualTo("S * 0.2");
            assertThat(flow.timeUnit()).isEqualTo("day");
            assertThat(flow.source()).isEqualTo("S");
            assertThat(flow.sink()).isNull();
        }
    }

    @Nested
    @DisplayName("setAuxEquation")
    class SetAuxEquation {

        @Test
        void shouldUpdateAuxEquation() {
            editor.addAux();
            String name = editor.getAuxiliaries().get(0).name();

            boolean result = editor.setAuxEquation(name, "Stock_1 + Constant_2");

            assertThat(result).isTrue();
            assertThat(editor.getAuxiliaries().get(0).equation()).isEqualTo("Stock_1 + Constant_2");
        }

        @Test
        void shouldReturnFalseForNonexistentAux() {
            assertThat(editor.setAuxEquation("ghost", "1")).isFalse();
        }

        @Test
        void shouldRejectBlankEquation() {
            editor.addAux();
            String name = editor.getAuxiliaries().get(0).name();

            assertThat(editor.setAuxEquation(name, "")).isFalse();
            assertThat(editor.setAuxEquation(name, "   ")).isFalse();
            assertThat(editor.getAuxiliaries().get(0).equation()).isEqualTo("0");
        }

        @Test
        void shouldRejectNullEquation() {
            editor.addAux();
            String name = editor.getAuxiliaries().get(0).name();

            assertThat(editor.setAuxEquation(name, null)).isFalse();
            assertThat(editor.getAuxiliaries().get(0).equation()).isEqualTo("0");
        }

        @Test
        void shouldPreserveOtherFields() {
            ModelDefinition def = new ModelDefinitionBuilder()
                    .name("Test")
                    .aux("Rate", "Stock_1 * 0.1", "1/day")
                    .build();
            editor.loadFrom(def);

            editor.setAuxEquation("Rate", "Stock_1 * 0.2");

            AuxDef aux = editor.getAuxiliaries().get(0);
            assertThat(aux.name()).isEqualTo("Rate");
            assertThat(aux.equation()).isEqualTo("Stock_1 * 0.2");
            assertThat(aux.unit()).isEqualTo("1/day");
        }
    }

    @Nested
    @DisplayName("reconnectFlow")
    class ReconnectFlow {

        @Test
        void shouldReconnectCloudSourceToStock() {
            editor.addStock(); // Stock 1
            editor.addStock(); // Stock 2
            editor.addFlow(null, "Stock 2"); // Flow 3 with cloud source

            boolean result = editor.reconnectFlow("Flow 3",
                    FlowEndpointCalculator.FlowEnd.SOURCE, "Stock 1");

            assertThat(result).isTrue();
            FlowDef flow = editor.getFlows().get(0);
            assertThat(flow.source()).isEqualTo("Stock 1");
            assertThat(flow.sink()).isEqualTo("Stock 2");
        }

        @Test
        void shouldReconnectCloudSinkToStock() {
            editor.addStock(); // Stock 1
            editor.addStock(); // Stock 2
            editor.addFlow("Stock 1", null); // Flow 3 with cloud sink

            boolean result = editor.reconnectFlow("Flow 3",
                    FlowEndpointCalculator.FlowEnd.SINK, "Stock 2");

            assertThat(result).isTrue();
            FlowDef flow = editor.getFlows().get(0);
            assertThat(flow.source()).isEqualTo("Stock 1");
            assertThat(flow.sink()).isEqualTo("Stock 2");
        }

        @Test
        void shouldDisconnectSourceToCloud() {
            editor.addStock(); // Stock 1
            editor.addStock(); // Stock 2
            editor.addFlow("Stock 1", "Stock 2"); // Flow 3

            boolean result = editor.reconnectFlow("Flow 3",
                    FlowEndpointCalculator.FlowEnd.SOURCE, null);

            assertThat(result).isTrue();
            FlowDef flow = editor.getFlows().get(0);
            assertThat(flow.source()).isNull();
            assertThat(flow.sink()).isEqualTo("Stock 2");
        }

        @Test
        void shouldSwapConnectedEndpointToDifferentStock() {
            editor.addStock(); // Stock 1
            editor.addStock(); // Stock 2
            editor.addStock(); // Stock 3
            editor.addFlow("Stock 1", "Stock 2"); // Flow 4

            boolean result = editor.reconnectFlow("Flow 4",
                    FlowEndpointCalculator.FlowEnd.SOURCE, "Stock 3");

            assertThat(result).isTrue();
            FlowDef flow = editor.getFlows().get(0);
            assertThat(flow.source()).isEqualTo("Stock 3");
            assertThat(flow.sink()).isEqualTo("Stock 2");
        }

        @Test
        void shouldReturnFalseForNonexistentFlow() {
            boolean result = editor.reconnectFlow("Nonexistent",
                    FlowEndpointCalculator.FlowEnd.SOURCE, "Stock 1");

            assertThat(result).isFalse();
        }

        @Test
        void shouldRejectNonexistentStockName() {
            editor.addStock(); // Stock 1
            editor.addFlow(null, "Stock 1"); // Flow 2 with cloud source

            boolean result = editor.reconnectFlow("Flow 2",
                    FlowEndpointCalculator.FlowEnd.SOURCE, "Ghost");

            assertThat(result).isFalse();
            FlowDef flow = editor.getFlows().get(0);
            assertThat(flow.source()).isNull(); // unchanged
        }

        @Test
        void shouldRejectSelfLoop() {
            editor.addStock(); // Stock 1
            editor.addStock(); // Stock 2
            editor.addFlow("Stock 1", "Stock 2"); // Flow 3

            boolean result = editor.reconnectFlow("Flow 3",
                    FlowEndpointCalculator.FlowEnd.SOURCE, "Stock 2");

            assertThat(result).isFalse();
            FlowDef flow = editor.getFlows().get(0);
            assertThat(flow.source()).isEqualTo("Stock 1"); // unchanged
            assertThat(flow.sink()).isEqualTo("Stock 2");
        }
    }
}
