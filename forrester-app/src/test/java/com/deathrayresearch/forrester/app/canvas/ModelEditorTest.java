package com.deathrayresearch.forrester.app.canvas;

import com.deathrayresearch.forrester.model.def.AuxDef;
import com.deathrayresearch.forrester.model.def.ElementPlacement;
import com.deathrayresearch.forrester.model.def.ElementType;
import com.deathrayresearch.forrester.model.def.FlowDef;
import com.deathrayresearch.forrester.model.def.LookupTableDef;
import com.deathrayresearch.forrester.model.def.ModelDefinition;
import com.deathrayresearch.forrester.model.def.ModelDefinitionBuilder;
import com.deathrayresearch.forrester.model.def.ModuleInstanceDef;
import com.deathrayresearch.forrester.model.def.SimulationSettings;
import com.deathrayresearch.forrester.model.def.ViewDef;

import java.util.List;
import java.util.Map;

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

        @Test
        void shouldCleanAuxEquationWhenReferencedElementDeleted() {
            ModelDefinition def = new ModelDefinitionBuilder()
                    .name("Test")
                    .stock("S", 100, "u")
                    .constant("k", 0.5, "1/day")
                    .aux("Rate", "S * k", "u")
                    .build();
            editor.loadFrom(def);

            editor.removeElement("k");

            AuxDef aux = editor.getAuxiliaries().get(0);
            assertThat(aux.equation()).isEqualTo("S * 0");
        }

        @Test
        void shouldCleanFlowEquationWhenReferencedAuxDeleted() {
            ModelDefinition def = new ModelDefinitionBuilder()
                    .name("Test")
                    .stock("S", 100, "u")
                    .aux("Calc", "S * 2", "u")
                    .flow("F", "Calc + 1", "day", "S", null)
                    .build();
            editor.loadFrom(def);

            editor.removeElement("Calc");

            FlowDef flow = editor.getFlows().get(0);
            assertThat(flow.equation()).isEqualTo("0 + 1");
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
        void shouldUpdateAuxEquationWhenReferencedElementRenamed() {
            ModelDefinition def = new ModelDefinitionBuilder()
                    .name("Test")
                    .stock("S", 100, "people")
                    .aux("Rate", "S * 0.1", "1/day")
                    .flow("F", "Rate * S", "day", "S", null)
                    .build();
            editor.loadFrom(def);

            editor.renameElement("S", "Population");

            assertThat(editor.getAuxiliaries().get(0).equation()).isEqualTo("Population * 0.1");
            assertThat(editor.getFlows().get(0).equation()).isEqualTo("Rate * Population");
        }

        @Test
        void shouldUpdateFlowEquationWhenReferencedAuxRenamed() {
            ModelDefinition def = new ModelDefinitionBuilder()
                    .name("Test")
                    .stock("S", 100, "u")
                    .aux("Calc", "S * 2", "u")
                    .flow("F", "Calc + 1", "day", "S", null)
                    .build();
            editor.loadFrom(def);

            editor.renameElement("Calc", "Result");

            assertThat(editor.getFlows().get(0).equation()).isEqualTo("Result + 1");
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

        @Test
        void shouldRoundTripSimulationSettings() {
            ModelDefinition original = new ModelDefinitionBuilder()
                    .name("Test")
                    .stock("S", 100, "units")
                    .defaultSimulation("Day", 365, "Year")
                    .build();

            editor.loadFrom(original);

            assertThat(editor.getSimulationSettings()).isNotNull();
            assertThat(editor.getSimulationSettings().timeStep()).isEqualTo("Day");
            assertThat(editor.getSimulationSettings().duration()).isEqualTo(365);
            assertThat(editor.getSimulationSettings().durationUnit()).isEqualTo("Year");

            ModelDefinition rebuilt = editor.toModelDefinition();

            assertThat(rebuilt.defaultSimulation()).isNotNull();
            assertThat(rebuilt.defaultSimulation().timeStep()).isEqualTo("Day");
            assertThat(rebuilt.defaultSimulation().duration()).isEqualTo(365);
            assertThat(rebuilt.defaultSimulation().durationUnit()).isEqualTo("Year");
        }

        @Test
        void shouldRoundTripNullSimulationSettings() {
            ModelDefinition original = new ModelDefinitionBuilder()
                    .name("Test")
                    .stock("S", 100, "units")
                    .build();

            editor.loadFrom(original);

            assertThat(editor.getSimulationSettings()).isNull();

            ModelDefinition rebuilt = editor.toModelDefinition();

            assertThat(rebuilt.defaultSimulation()).isNull();
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

    @Nested
    @DisplayName("modules")
    class Modules {

        @Test
        void shouldAutoNameModules() {
            String name1 = editor.addModule();
            String name2 = editor.addModule();

            assertThat(name1).isEqualTo("Module 1");
            assertThat(name2).isEqualTo("Module 2");
            assertThat(editor.getModules()).hasSize(2);
        }

        @Test
        void shouldRemoveModule() {
            editor.addModule(); // Module 1

            editor.removeElement("Module 1");

            assertThat(editor.getModules()).isEmpty();
            assertThat(editor.hasElement("Module 1")).isFalse();
        }

        @Test
        void shouldRenameModule() {
            editor.addModule(); // Module 1

            boolean result = editor.renameElement("Module 1", "SIR Submodel");

            assertThat(result).isTrue();
            assertThat(editor.getModules().get(0).instanceName()).isEqualTo("SIR Submodel");
            assertThat(editor.hasElement("Module 1")).isFalse();
            assertThat(editor.hasElement("SIR Submodel")).isTrue();
        }

        @Test
        void shouldRejectRenameToExistingName() {
            editor.addModule(); // Module 1
            editor.addStock();  // Stock 2

            boolean result = editor.renameElement("Module 1", "Stock 2");

            assertThat(result).isFalse();
            assertThat(editor.getModules().get(0).instanceName()).isEqualTo("Module 1");
        }

        @Test
        void shouldLoadFromWithModules() {
            ModelDefinition innerDef = new ModelDefinitionBuilder()
                    .name("Inner")
                    .stock("S", 100, "u")
                    .build();
            ModuleInstanceDef moduleDef = new ModuleInstanceDef(
                    "MyModule", innerDef, Map.of(), Map.of());
            ModelDefinition def = new ModelDefinitionBuilder()
                    .name("Outer")
                    .module(moduleDef)
                    .build();

            editor.loadFrom(def);

            assertThat(editor.getModules()).hasSize(1);
            assertThat(editor.getModules().get(0).instanceName()).isEqualTo("MyModule");
            assertThat(editor.hasElement("MyModule")).isTrue();
        }

        @Test
        void shouldIncludeModulesInToModelDefinition() {
            editor.addModule();

            ModelDefinition def = editor.toModelDefinition();

            assertThat(def.modules()).hasSize(1);
            assertThat(def.modules().get(0).instanceName()).isEqualTo("Module 1");
        }

        @Test
        void shouldRoundTripModules() {
            ModelDefinition innerDef = new ModelDefinitionBuilder()
                    .name("Inner")
                    .stock("S", 0, "u")
                    .build();
            ModuleInstanceDef moduleDef = new ModuleInstanceDef(
                    "Sub", innerDef, Map.of(), Map.of());
            ModelDefinition original = new ModelDefinitionBuilder()
                    .name("Test")
                    .stock("S1", 100, "u")
                    .module(moduleDef)
                    .build();

            editor.loadFrom(original);
            ModelDefinition rebuilt = editor.toModelDefinition();

            assertThat(rebuilt.modules()).hasSize(1);
            assertThat(rebuilt.modules().get(0).instanceName()).isEqualTo("Sub");
            assertThat(rebuilt.stocks()).hasSize(1);
        }

        @Test
        void shouldClearModulesOnReload() {
            editor.addModule();

            ModelDefinition def = new ModelDefinitionBuilder()
                    .name("Empty")
                    .build();
            editor.loadFrom(def);

            assertThat(editor.getModules()).isEmpty();
        }

        @Test
        void shouldContinueNumberingAfterLoad() {
            ModelDefinition innerDef = new ModelDefinitionBuilder()
                    .name("Inner")
                    .build();
            ModuleInstanceDef moduleDef = new ModuleInstanceDef(
                    "Module 5", innerDef, Map.of(), Map.of());
            ModelDefinition def = new ModelDefinitionBuilder()
                    .name("Test")
                    .module(moduleDef)
                    .build();

            editor.loadFrom(def);
            String name = editor.addModule();

            assertThat(name).isEqualTo("Module 6");
        }
    }

    @Nested
    @DisplayName("updateModuleDefinition")
    class UpdateModuleDefinition {

        @Test
        void shouldReplaceDefinitionPreservingNameAndBindings() {
            ModelDefinition innerDef = new ModelDefinitionBuilder()
                    .name("Inner")
                    .stock("S", 100, "u")
                    .build();
            ModuleInstanceDef moduleDef = new ModuleInstanceDef(
                    "MyModule", innerDef, Map.of("in", "x"), Map.of("out", "y"));
            ModelDefinition def = new ModelDefinitionBuilder()
                    .name("Outer")
                    .module(moduleDef)
                    .build();
            editor.loadFrom(def);

            ModelDefinition newInner = new ModelDefinitionBuilder()
                    .name("Updated")
                    .stock("S", 200, "u")
                    .constant("k", 0.5, "1/d")
                    .build();
            editor.updateModuleDefinition(0, newInner);

            ModuleInstanceDef updated = editor.getModules().get(0);
            assertThat(updated.instanceName()).isEqualTo("MyModule");
            assertThat(updated.definition().stocks()).hasSize(1);
            assertThat(updated.definition().constants()).hasSize(1);
            assertThat(updated.inputBindings()).containsEntry("in", "x");
            assertThat(updated.outputBindings()).containsEntry("out", "y");
        }

        @Test
        void shouldDoNothingForInvalidIndex() {
            editor.addModule();

            editor.updateModuleDefinition(-1, new ModelDefinitionBuilder().name("X").build());
            editor.updateModuleDefinition(5, new ModelDefinitionBuilder().name("X").build());

            assertThat(editor.getModules()).hasSize(1);
        }

        @Test
        void shouldUpdateCorrectModuleByIndex() {
            editor.addModule(); // Module 1
            editor.addModule(); // Module 2

            ModelDefinition newDef = new ModelDefinitionBuilder()
                    .name("Replaced")
                    .stock("NewStock", 50, "u")
                    .build();
            editor.updateModuleDefinition(1, newDef);

            assertThat(editor.getModules().get(0).definition().stocks()).isEmpty();
            assertThat(editor.getModules().get(1).definition().stocks()).hasSize(1);
            assertThat(editor.getModules().get(1).instanceName()).isEqualTo("Module 2");
        }
    }

    @Nested
    @DisplayName("updateModuleBindings")
    class UpdateModuleBindings {

        @Test
        void shouldUpdateBindingsByName() {
            editor.addModule(); // Module 1

            boolean result = editor.updateModuleBindings("Module 1",
                    Map.of("input1", "Stock_1 * 0.5"),
                    Map.of("output1", "Result"));

            assertThat(result).isTrue();
            ModuleInstanceDef m = editor.getModules().get(0);
            assertThat(m.inputBindings()).containsEntry("input1", "Stock_1 * 0.5");
            assertThat(m.outputBindings()).containsEntry("output1", "Result");
        }

        @Test
        void shouldReturnFalseForNonexistentModule() {
            boolean result = editor.updateModuleBindings("Ghost",
                    Map.of(), Map.of());

            assertThat(result).isFalse();
        }

        @Test
        void shouldReplaceExistingBindings() {
            ModelDefinition innerDef = new ModelDefinitionBuilder()
                    .name("Inner")
                    .build();
            ModuleInstanceDef moduleDef = new ModuleInstanceDef(
                    "Mod", innerDef,
                    Map.of("old_in", "val1"),
                    Map.of("old_out", "val2"));
            ModelDefinition def = new ModelDefinitionBuilder()
                    .name("Outer")
                    .module(moduleDef)
                    .build();
            editor.loadFrom(def);

            editor.updateModuleBindings("Mod",
                    Map.of("new_in", "val3"),
                    Map.of("new_out", "val4"));

            ModuleInstanceDef m = editor.getModules().get(0);
            assertThat(m.inputBindings()).doesNotContainKey("old_in");
            assertThat(m.inputBindings()).containsEntry("new_in", "val3");
            assertThat(m.outputBindings()).doesNotContainKey("old_out");
            assertThat(m.outputBindings()).containsEntry("new_out", "val4");
        }

        @Test
        void shouldPreserveDefinitionWhenUpdatingBindings() {
            ModelDefinition innerDef = new ModelDefinitionBuilder()
                    .name("Inner")
                    .stock("S", 100, "u")
                    .build();
            ModuleInstanceDef moduleDef = new ModuleInstanceDef(
                    "Mod", innerDef, Map.of(), Map.of());
            ModelDefinition def = new ModelDefinitionBuilder()
                    .name("Outer")
                    .module(moduleDef)
                    .build();
            editor.loadFrom(def);

            editor.updateModuleBindings("Mod",
                    Map.of("in", "expr"), Map.of());

            ModuleInstanceDef m = editor.getModules().get(0);
            assertThat(m.definition().stocks()).hasSize(1);
            assertThat(m.definition().stocks().get(0).name()).isEqualTo("S");
        }
    }

    @Nested
    @DisplayName("getModuleByName")
    class GetModuleByName {

        @Test
        void shouldReturnModuleWhenFound() {
            editor.addModule(); // Module 1

            ModuleInstanceDef result = editor.getModuleByName("Module 1");

            assertThat(result).isNotNull();
            assertThat(result.instanceName()).isEqualTo("Module 1");
        }

        @Test
        void shouldReturnNullWhenNotFound() {
            assertThat(editor.getModuleByName("Ghost")).isNull();
        }
    }

    @Nested
    @DisplayName("getModuleIndex")
    class GetModuleIndex {

        @Test
        void shouldReturnIndexWhenFound() {
            editor.addModule(); // Module 1
            editor.addModule(); // Module 2

            assertThat(editor.getModuleIndex("Module 1")).isZero();
            assertThat(editor.getModuleIndex("Module 2")).isEqualTo(1);
        }

        @Test
        void shouldReturnNegativeOneWhenNotFound() {
            assertThat(editor.getModuleIndex("Ghost")).isEqualTo(-1);
        }
    }

    @Nested
    @DisplayName("removeConnectionReference")
    class RemoveConnectionReference {

        @Test
        void shouldRemoveReferenceFromAuxEquation() {
            ModelDefinition def = new ModelDefinitionBuilder()
                    .name("Test")
                    .constant("Rate", 0.5, null)
                    .aux("Calc", "Rate * 10", null)
                    .build();
            editor.loadFrom(def);

            boolean removed = editor.removeConnectionReference("Rate", "Calc");

            assertThat(removed).isTrue();
            AuxDef aux = editor.getAuxByName("Calc");
            assertThat(aux.equation()).doesNotContain("Rate");
            assertThat(aux.equation()).contains("0");
        }

        @Test
        void shouldRemoveReferenceFromFlowEquation() {
            ModelDefinition def = new ModelDefinitionBuilder()
                    .name("Test")
                    .stock("Population", 100, null)
                    .constant("Growth Rate", 0.1, null)
                    .flow("Growth", "Population * Growth_Rate", "day", "Population", null)
                    .build();
            editor.loadFrom(def);

            boolean removed = editor.removeConnectionReference("Growth Rate", "Growth");

            assertThat(removed).isTrue();
            FlowDef flow = editor.getFlowByName("Growth");
            assertThat(flow.equation()).doesNotContain("Growth_Rate");
        }

        @Test
        void shouldReturnFalseWhenReferenceNotFound() {
            ModelDefinition def = new ModelDefinitionBuilder()
                    .name("Test")
                    .aux("Calc", "10 + 5", null)
                    .build();
            editor.loadFrom(def);

            boolean removed = editor.removeConnectionReference("Ghost", "Calc");

            assertThat(removed).isFalse();
        }

        @Test
        void shouldReturnFalseWhenTargetNotFound() {
            ModelDefinition def = new ModelDefinitionBuilder()
                    .name("Test")
                    .constant("Rate", 0.5, null)
                    .build();
            editor.loadFrom(def);

            boolean removed = editor.removeConnectionReference("Rate", "Ghost");

            assertThat(removed).isFalse();
        }
    }

    @Nested
    @DisplayName("addXxxFrom (template copy)")
    class AddFromTemplate {

        @Test
        void shouldCopyStockProperties() {
            ModelDefinition def = new ModelDefinitionBuilder()
                    .name("Test")
                    .stock("Original", 42, "widgets")
                    .build();
            editor.loadFrom(def);

            String newName = editor.addStockFrom(editor.getStocks().get(0));

            assertThat(newName).isNotEqualTo("Original");
            assertThat(editor.getStockByName(newName)).isNotNull();
            assertThat(editor.getStockByName(newName).initialValue()).isEqualTo(42);
            assertThat(editor.getStockByName(newName).unit()).isEqualTo("widgets");
        }

        @Test
        void shouldCopyFlowWithNewEndpoints() {
            ModelDefinition def = new ModelDefinitionBuilder()
                    .name("Test")
                    .stock("S1", 100, null)
                    .stock("S2", 0, null)
                    .flow("Transfer", "S1 * 0.1", "day", "S1", "S2")
                    .build();
            editor.loadFrom(def);

            String newName = editor.addFlowFrom(
                    editor.getFlows().get(0), null, null);

            assertThat(newName).isNotEqualTo("Transfer");
            FlowDef newFlow = editor.getFlowByName(newName);
            assertThat(newFlow).isNotNull();
            assertThat(newFlow.source()).isNull();
            assertThat(newFlow.sink()).isNull();
            assertThat(newFlow.equation()).isEqualTo("S1 * 0.1");
        }

        @Test
        void shouldCopyAuxWithCustomEquation() {
            ModelDefinition def = new ModelDefinitionBuilder()
                    .name("Test")
                    .aux("Ratio", "A / B", "fraction")
                    .build();
            editor.loadFrom(def);

            String newName = editor.addAuxFrom(
                    editor.getAuxiliaries().get(0), "X / Y");

            assertThat(newName).isNotEqualTo("Ratio");
            AuxDef newAux = editor.getAuxByName(newName);
            assertThat(newAux).isNotNull();
            assertThat(newAux.equation()).isEqualTo("X / Y");
            assertThat(newAux.unit()).isEqualTo("fraction");
        }

        @Test
        void shouldCopyConstantProperties() {
            ModelDefinition def = new ModelDefinitionBuilder()
                    .name("Test")
                    .constant("Pi", 3.14159, "ratio")
                    .build();
            editor.loadFrom(def);

            String newName = editor.addConstantFrom(editor.getConstants().get(0));

            assertThat(newName).isNotEqualTo("Pi");
            assertThat(editor.getConstantByName(newName)).isNotNull();
            assertThat(editor.getConstantByName(newName).value()).isEqualTo(3.14159);
            assertThat(editor.getConstantByName(newName).unit()).isEqualTo("ratio");
        }

        @Test
        void shouldGenerateUniqueNamesOnMultipleCopies() {
            editor.addStock();
            String name1 = editor.addStockFrom(editor.getStocks().get(0));
            String name2 = editor.addStockFrom(editor.getStocks().get(0));

            assertThat(name1).isNotEqualTo(name2);
        }
    }

    @Nested
    @DisplayName("lookup tables")
    class LookupTables {

        @Test
        void shouldAutoNameLookupWithDefaultValues() {
            String name = editor.addLookup();

            assertThat(name).startsWith("Lookup ");
            assertThat(editor.getLookupTables()).hasSize(1);
            LookupTableDef lt = editor.getLookupTableByName(name);
            assertThat(lt).isNotNull();
            assertThat(lt.xValues()).containsExactly(0.0, 1.0);
            assertThat(lt.yValues()).containsExactly(0.0, 1.0);
            assertThat(lt.interpolation()).isEqualTo("LINEAR");
        }

        @Test
        void shouldCopyLookupFromTemplate() {
            ModelDefinition def = new ModelDefinitionBuilder()
                    .name("Test")
                    .lookupTable("Table", new double[]{0, 5, 10},
                            new double[]{1, 3, 9}, "SPLINE")
                    .build();
            editor.loadFrom(def);

            String newName = editor.addLookupFrom(editor.getLookupTables().get(0));

            assertThat(newName).isNotEqualTo("Table");
            LookupTableDef copy = editor.getLookupTableByName(newName);
            assertThat(copy).isNotNull();
            assertThat(copy.xValues()).containsExactly(0, 5, 10);
            assertThat(copy.yValues()).containsExactly(1, 3, 9);
            assertThat(copy.interpolation()).isEqualTo("SPLINE");
        }

        @Test
        void shouldReturnLookupByName() {
            editor.addLookup();
            String name = editor.getLookupTables().get(0).name();

            LookupTableDef lt = editor.getLookupTableByName(name);

            assertThat(lt).isNotNull();
            assertThat(lt.name()).isEqualTo(name);
        }

        @Test
        void shouldReturnNullForMissingLookup() {
            assertThat(editor.getLookupTableByName("Ghost")).isNull();
        }

        @Test
        void shouldSetLookupTableData() {
            String name = editor.addLookup();
            LookupTableDef updated = new LookupTableDef(name,
                    new double[]{0, 10, 20}, new double[]{5, 15, 25}, "SPLINE");

            boolean result = editor.setLookupTable(name, updated);

            assertThat(result).isTrue();
            LookupTableDef lt = editor.getLookupTableByName(name);
            assertThat(lt.xValues()).containsExactly(0, 10, 20);
            assertThat(lt.yValues()).containsExactly(5, 15, 25);
            assertThat(lt.interpolation()).isEqualTo("SPLINE");
        }

        @Test
        void shouldRemoveLookupAndNameIndex() {
            String name = editor.addLookup();
            assertThat(editor.hasElement(name)).isTrue();

            editor.removeElement(name);

            assertThat(editor.getLookupTables()).isEmpty();
            assertThat(editor.hasElement(name)).isFalse();
        }

        @Test
        void shouldRenameLookupPreservingData() {
            ModelDefinition def = new ModelDefinitionBuilder()
                    .name("Test")
                    .lookupTable("OldTable", new double[]{0, 1, 2},
                            new double[]{10, 20, 30}, "LINEAR")
                    .build();
            editor.loadFrom(def);

            boolean renamed = editor.renameElement("OldTable", "NewTable");

            assertThat(renamed).isTrue();
            assertThat(editor.getLookupTableByName("OldTable")).isNull();
            LookupTableDef lt = editor.getLookupTableByName("NewTable");
            assertThat(lt).isNotNull();
            assertThat(lt.xValues()).containsExactly(0, 1, 2);
            assertThat(lt.yValues()).containsExactly(10, 20, 30);
        }

        @Test
        void shouldIncludeLookupsInNameIndexOnLoad() {
            ModelDefinition def = new ModelDefinitionBuilder()
                    .name("Test")
                    .lookupTable("MyTable", new double[]{0, 1},
                            new double[]{0, 1}, "LINEAR")
                    .build();

            editor.loadFrom(def);

            assertThat(editor.hasElement("MyTable")).isTrue();
        }

        @Test
        void shouldContinueNumberingAfterLoadingLookups() {
            ModelDefinition def = new ModelDefinitionBuilder()
                    .name("Test")
                    .lookupTable("Lookup 5", new double[]{0, 1},
                            new double[]{0, 1}, "LINEAR")
                    .build();
            editor.loadFrom(def);

            String name = editor.addLookup();

            assertThat(name).isEqualTo("Lookup 6");
        }

        @Test
        void shouldIncludeLookupsInToModelDefinition() {
            String name = editor.addLookup();

            ModelDefinition def = editor.toModelDefinition();

            assertThat(def.lookupTables()).hasSize(1);
            assertThat(def.lookupTables().get(0).name()).isEqualTo(name);
        }
    }

    @Nested
    @DisplayName("replaceToken")
    class ReplaceToken {

        @Test
        void shouldReplaceWholeTokenOnly() {
            String result = ModelEditor.replaceToken("A + AB + A_B", "A", "X");
            assertThat(result).isEqualTo("X + AB + A_B");
        }

        @Test
        void shouldReplaceMultipleOccurrences() {
            String result = ModelEditor.replaceToken("A + A * A", "A", "B");
            assertThat(result).isEqualTo("B + B * B");
        }

        @Test
        void shouldHandleUnderscoreTokens() {
            String result = ModelEditor.replaceToken(
                    "Contact_Rate * Pop", "Contact_Rate", "New_Rate");
            assertThat(result).isEqualTo("New_Rate * Pop");
        }

        @Test
        void shouldNotReplacePartialMatch() {
            String result = ModelEditor.replaceToken("Population", "Pop", "People");
            assertThat(result).isEqualTo("Population");
        }

        @Test
        void shouldHandleEmptyEquation() {
            String result = ModelEditor.replaceToken("", "A", "B");
            assertThat(result).isEmpty();
        }
    }
}
