package systems.courant.forrester.app.canvas;

import systems.courant.forrester.model.def.AuxDef;
import systems.courant.forrester.model.def.CausalLinkDef;
import systems.courant.forrester.model.def.CldVariableDef;
import systems.courant.forrester.model.def.ConstantDef;
import systems.courant.forrester.model.def.ElementPlacement;
import systems.courant.forrester.model.def.ElementType;
import systems.courant.forrester.model.def.FlowDef;
import systems.courant.forrester.model.def.LookupTableDef;
import systems.courant.forrester.model.def.ModelDefinition;
import systems.courant.forrester.model.def.ModelDefinitionBuilder;
import systems.courant.forrester.model.def.ModuleInstanceDef;
import systems.courant.forrester.model.def.SimulationSettings;
import systems.courant.forrester.model.def.StockDef;
import systems.courant.forrester.model.def.ViewDef;

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
        void shouldNumberEachTypeIndependently() {
            editor.addStock();  // "Stock 1"
            String flowName = editor.addFlow();  // "Flow 1"
            String auxName = editor.addAux();  // "Aux 1"

            assertThat(flowName).isEqualTo("Flow 1");
            assertThat(auxName).isEqualTo("Aux 1");
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
            editor.addConstant();  // "Constant 1"

            boolean result = editor.renameElement("Stock 1", "Constant 1");

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

        @Test
        void shouldRejectReservedWords() {
            assertThat(ModelEditor.isValidName("TIME")).isFalse();
            assertThat(ModelEditor.isValidName("time")).isFalse();
            assertThat(ModelEditor.isValidName("DT")).isFalse();
            assertThat(ModelEditor.isValidName("dt")).isFalse();
            assertThat(ModelEditor.isValidName("Pi")).isFalse();
            assertThat(ModelEditor.isValidName("E")).isFalse();
            assertThat(ModelEditor.isValidName("IF")).isFalse();
            assertThat(ModelEditor.isValidName("AND")).isFalse();
            assertThat(ModelEditor.isValidName("or")).isFalse();
            assertThat(ModelEditor.isValidName("Not")).isFalse();
            assertThat(ModelEditor.isValidName("THEN")).isFalse();
            assertThat(ModelEditor.isValidName("ELSE")).isFalse();
        }

        @Test
        void shouldAcceptNamesContainingReservedWords() {
            assertThat(ModelEditor.isValidName("Time Delay")).isTrue();
            assertThat(ModelEditor.isValidName("IF Rate")).isTrue();
            assertThat(ModelEditor.isValidName("Elapsed Time")).isTrue();
        }

        @Test
        void shouldRejectNameExceedingMaxLength() {
            String longName = "A".repeat(ModelEditor.MAX_NAME_LENGTH + 1);
            assertThat(ModelEditor.isValidName(longName)).isFalse();
        }

        @Test
        void shouldAcceptNameAtMaxLength() {
            String maxName = "A".repeat(ModelEditor.MAX_NAME_LENGTH);
            assertThat(ModelEditor.isValidName(maxName)).isTrue();
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
            editor.addFlow(null, "Stock 2"); // Flow 1 with cloud source

            boolean result = editor.reconnectFlow("Flow 1",
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
            editor.addFlow("Stock 1", null); // Flow 1 with cloud sink

            boolean result = editor.reconnectFlow("Flow 1",
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
            editor.addFlow("Stock 1", "Stock 2"); // Flow 1

            boolean result = editor.reconnectFlow("Flow 1",
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
            editor.addFlow("Stock 1", "Stock 2"); // Flow 1

            boolean result = editor.reconnectFlow("Flow 1",
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
            editor.addFlow(null, "Stock 1"); // Flow 1 with cloud source

            boolean result = editor.reconnectFlow("Flow 1",
                    FlowEndpointCalculator.FlowEnd.SOURCE, "Ghost");

            assertThat(result).isFalse();
            FlowDef flow = editor.getFlows().get(0);
            assertThat(flow.source()).isNull(); // unchanged
        }

        @Test
        void shouldRejectSelfLoop() {
            editor.addStock(); // Stock 1
            editor.addStock(); // Stock 2
            editor.addFlow("Stock 1", "Stock 2"); // Flow 1

            boolean result = editor.reconnectFlow("Flow 1",
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
            editor.addStock();  // Stock 1

            boolean result = editor.renameElement("Module 1", "Stock 1");

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

            ModuleInstanceDef result = editor.getModuleByName("Module 1").orElseThrow();

            assertThat(result.instanceName()).isEqualTo("Module 1");
        }

        @Test
        void shouldReturnEmptyWhenNotFound() {
            assertThat(editor.getModuleByName("Ghost")).isEmpty();
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
            AuxDef aux = editor.getAuxByName("Calc").orElseThrow();
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
            FlowDef flow = editor.getFlowByName("Growth").orElseThrow();
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
            assertThat(editor.getStockByName(newName)).isPresent();
            assertThat(editor.getStockByName(newName).orElseThrow().initialValue()).isEqualTo(42);
            assertThat(editor.getStockByName(newName).orElseThrow().unit()).isEqualTo("widgets");
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
            FlowDef newFlow = editor.getFlowByName(newName).orElseThrow();
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
            AuxDef newAux = editor.getAuxByName(newName).orElseThrow();
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
            assertThat(editor.getConstantByName(newName)).isPresent();
            assertThat(editor.getConstantByName(newName).orElseThrow().value()).isEqualTo(3.14159);
            assertThat(editor.getConstantByName(newName).orElseThrow().unit()).isEqualTo("ratio");
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
            LookupTableDef lt = editor.getLookupTableByName(name).orElseThrow();
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
            LookupTableDef copy = editor.getLookupTableByName(newName).orElseThrow();
            assertThat(copy.xValues()).containsExactly(0, 5, 10);
            assertThat(copy.yValues()).containsExactly(1, 3, 9);
            assertThat(copy.interpolation()).isEqualTo("SPLINE");
        }

        @Test
        void shouldReturnLookupByName() {
            editor.addLookup();
            String name = editor.getLookupTables().get(0).name();

            LookupTableDef lt = editor.getLookupTableByName(name).orElseThrow();

            assertThat(lt.name()).isEqualTo(name);
        }

        @Test
        void shouldReturnEmptyForMissingLookup() {
            assertThat(editor.getLookupTableByName("Ghost")).isEmpty();
        }

        @Test
        void shouldSetLookupTableData() {
            String name = editor.addLookup();
            LookupTableDef updated = new LookupTableDef(name,
                    new double[]{0, 10, 20}, new double[]{5, 15, 25}, "SPLINE");

            boolean result = editor.setLookupTable(name, updated);

            assertThat(result).isTrue();
            LookupTableDef lt = editor.getLookupTableByName(name).orElseThrow();
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
            assertThat(editor.getLookupTableByName("OldTable")).isEmpty();
            LookupTableDef lt = editor.getLookupTableByName("NewTable").orElseThrow();
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

    @Nested
    @DisplayName("SIR model flow source/sink")
    class SirFlowSourceSink {

        @Test
        void shouldPreserveFlowSourceAndSinkFromBuilder() {
            ModelDefinition def = new ModelDefinitionBuilder()
                    .name("SIR")
                    .stock("Susceptible", 1000, "Person")
                    .stock("Infectious", 10, "Person")
                    .stock("Recovered", 0, "Person")
                    .flow("Infection", "Susceptible * 0.1", "Day",
                            "Susceptible", "Infectious")
                    .flow("Recovery", "Infectious * 0.2", "Day",
                            "Infectious", "Recovered")
                    .build();

            editor.loadFrom(def);

            FlowDef infection = editor.getFlowByName("Infection").orElseThrow();
            assertThat(infection.source()).isEqualTo("Susceptible");
            assertThat(infection.sink()).isEqualTo("Infectious");

            FlowDef recovery = editor.getFlowByName("Recovery").orElseThrow();
            assertThat(recovery.source()).isEqualTo("Infectious");
            assertThat(recovery.sink()).isEqualTo("Recovered");
        }

        @Test
        void shouldPreserveFlowSourceAndSinkAfterUndoRoundTrip() {
            ModelDefinition def = new ModelDefinitionBuilder()
                    .name("SIR")
                    .stock("Susceptible", 1000, "Person")
                    .stock("Infectious", 10, "Person")
                    .stock("Recovered", 0, "Person")
                    .flow("Infection", "Susceptible * 0.1", "Day",
                            "Susceptible", "Infectious")
                    .flow("Recovery", "Infectious * 0.2", "Day",
                            "Infectious", "Recovered")
                    .build();

            editor.loadFrom(def);

            // Simulate an undo snapshot round-trip
            ModelDefinition snapshot = editor.toModelDefinition();
            ModelEditor restored = new ModelEditor();
            restored.loadFrom(snapshot);

            FlowDef infection = restored.getFlowByName("Infection").orElseThrow();
            assertThat(infection.source()).isEqualTo("Susceptible");
            assertThat(infection.sink()).isEqualTo("Infectious");

            FlowDef recovery = restored.getFlowByName("Recovery").orElseThrow();
            assertThat(recovery.source()).isEqualTo("Infectious");
            assertThat(recovery.sink()).isEqualTo("Recovered");
        }

        @Test
        void shouldPreserveFlowSourceAndSinkFromJson() {
            var serializer = new systems.courant.forrester.io.json.ModelDefinitionSerializer();
            ModelDefinition def = new ModelDefinitionBuilder()
                    .name("SIR")
                    .stock("Susceptible", 1000, "Person")
                    .stock("Infectious", 10, "Person")
                    .stock("Recovered", 0, "Person")
                    .flow("Infection", "Susceptible * 0.1", "Day",
                            "Susceptible", "Infectious")
                    .flow("Recovery", "Infectious * 0.2", "Day",
                            "Infectious", "Recovered")
                    .build();

            String json = serializer.toJson(def);
            ModelDefinition deserialized = serializer.fromJson(json);
            editor.loadFrom(deserialized);

            FlowDef infection = editor.getFlowByName("Infection").orElseThrow();
            assertThat(infection.source()).isEqualTo("Susceptible");
            assertThat(infection.sink()).isEqualTo("Infectious");

            FlowDef recovery = editor.getFlowByName("Recovery").orElseThrow();
            assertThat(recovery.source()).isEqualTo("Infectious");
            assertThat(recovery.sink()).isEqualTo("Recovered");
        }
    }

    @Nested
    @DisplayName("CLD Variables")
    class CldVariables {

        @Test
        void shouldAddCldVariable() {
            String name = editor.addCldVariable();
            assertThat(name).isEqualTo("Variable 1");
            assertThat(editor.getCldVariables()).hasSize(1);
            assertThat(editor.hasElement("Variable 1")).isTrue();
        }

        @Test
        void shouldAutoIncrementCldVariableNames() {
            editor.addCldVariable();
            String second = editor.addCldVariable();
            assertThat(second).isEqualTo("Variable 2");
        }

        @Test
        void shouldRemoveCldVariable() {
            editor.addCldVariable();
            editor.removeElement("Variable 1");
            assertThat(editor.getCldVariables()).isEmpty();
            assertThat(editor.hasElement("Variable 1")).isFalse();
        }

        @Test
        void shouldRenameCldVariable() {
            editor.addCldVariable();
            boolean result = editor.renameElement("Variable 1", "Workload");
            assertThat(result).isTrue();
            assertThat(editor.getCldVariables().get(0).name()).isEqualTo("Workload");
            assertThat(editor.hasElement("Workload")).isTrue();
            assertThat(editor.hasElement("Variable 1")).isFalse();
        }

        @Test
        void shouldSetCldVariableComment() {
            editor.addCldVariable();
            boolean result = editor.setCldVariableComment("Variable 1", "A description");
            assertThat(result).isTrue();
            assertThat(editor.getCldVariables().get(0).comment()).isEqualTo("A description");
        }

        @Test
        void shouldLoadCldVariablesFromDefinition() {
            ModelDefinition def = new ModelDefinitionBuilder()
                    .name("CLD")
                    .cldVariable("Stress", "Work stress")
                    .cldVariable("Performance")
                    .causalLink("Stress", "Performance", CausalLinkDef.Polarity.NEGATIVE)
                    .build();

            editor.loadFrom(def);

            assertThat(editor.getCldVariables()).hasSize(2);
            assertThat(editor.getCausalLinks()).hasSize(1);
            assertThat(editor.hasElement("Stress")).isTrue();
            assertThat(editor.hasElement("Performance")).isTrue();
        }

        @Test
        void shouldIncludeCldDataInSnapshot() {
            editor.addCldVariable(); // Variable 1
            editor.addCldVariable(); // Variable 2
            editor.addCausalLink("Variable 1", "Variable 2", CausalLinkDef.Polarity.POSITIVE);

            ModelDefinition snapshot = editor.toModelDefinition();
            assertThat(snapshot.cldVariables()).hasSize(2);
            assertThat(snapshot.causalLinks()).hasSize(1);
            assertThat(snapshot.causalLinks().get(0).polarity())
                    .isEqualTo(CausalLinkDef.Polarity.POSITIVE);
        }

        @Test
        void shouldClearCldDataOnReload() {
            editor.addCldVariable();
            editor.addCausalLink("Variable 1", "Variable 1", CausalLinkDef.Polarity.POSITIVE);

            editor.loadFrom(new ModelDefinitionBuilder().name("Empty").build());
            assertThat(editor.getCldVariables()).isEmpty();
            assertThat(editor.getCausalLinks()).isEmpty();
        }

        @Test
        void shouldContinueCounterAfterLoad() {
            ModelDefinition def = new ModelDefinitionBuilder()
                    .name("CLD")
                    .cldVariable("Variable 5")
                    .build();
            editor.loadFrom(def);

            String next = editor.addCldVariable();
            assertThat(next).isEqualTo("Variable 6");
        }
    }

    @Nested
    @DisplayName("Causal Links")
    class CausalLinks {

        @Test
        void shouldAddCausalLink() {
            editor.addCldVariable(); // Variable 1
            editor.addCldVariable(); // Variable 2

            boolean result = editor.addCausalLink("Variable 1", "Variable 2",
                    CausalLinkDef.Polarity.POSITIVE);

            assertThat(result).isTrue();
            assertThat(editor.getCausalLinks()).hasSize(1);
        }

        @Test
        void shouldRejectCausalLinkWithMissingEndpoint() {
            editor.addCldVariable(); // Variable 1

            boolean result = editor.addCausalLink("Variable 1", "Nonexistent",
                    CausalLinkDef.Polarity.POSITIVE);

            assertThat(result).isFalse();
            assertThat(editor.getCausalLinks()).isEmpty();
        }

        @Test
        void shouldRemoveCausalLink() {
            editor.addCldVariable();
            editor.addCldVariable();
            editor.addCausalLink("Variable 1", "Variable 2", CausalLinkDef.Polarity.POSITIVE);

            boolean result = editor.removeCausalLink("Variable 1", "Variable 2");

            assertThat(result).isTrue();
            assertThat(editor.getCausalLinks()).isEmpty();
        }

        @Test
        void shouldSetCausalLinkPolarity() {
            editor.addCldVariable();
            editor.addCldVariable();
            editor.addCausalLink("Variable 1", "Variable 2", CausalLinkDef.Polarity.UNKNOWN);

            boolean result = editor.setCausalLinkPolarity("Variable 1", "Variable 2",
                    CausalLinkDef.Polarity.NEGATIVE);

            assertThat(result).isTrue();
            assertThat(editor.getCausalLinks().get(0).polarity())
                    .isEqualTo(CausalLinkDef.Polarity.NEGATIVE);
        }

        @Test
        void shouldRemoveCausalLinksWhenVariableDeleted() {
            editor.addCldVariable(); // Variable 1
            editor.addCldVariable(); // Variable 2
            editor.addCldVariable(); // Variable 3
            editor.addCausalLink("Variable 1", "Variable 2", CausalLinkDef.Polarity.POSITIVE);
            editor.addCausalLink("Variable 2", "Variable 3", CausalLinkDef.Polarity.NEGATIVE);
            editor.addCausalLink("Variable 3", "Variable 1", CausalLinkDef.Polarity.POSITIVE);

            editor.removeElement("Variable 2");

            // Only the link from 3->1 should survive
            assertThat(editor.getCausalLinks()).hasSize(1);
            assertThat(editor.getCausalLinks().get(0).from()).isEqualTo("Variable 3");
            assertThat(editor.getCausalLinks().get(0).to()).isEqualTo("Variable 1");
        }

        @Test
        void shouldUpdateCausalLinksWhenVariableRenamed() {
            editor.addCldVariable(); // Variable 1
            editor.addCldVariable(); // Variable 2
            editor.addCausalLink("Variable 1", "Variable 2", CausalLinkDef.Polarity.POSITIVE);

            editor.renameElement("Variable 1", "Workload");

            CausalLinkDef link = editor.getCausalLinks().get(0);
            assertThat(link.from()).isEqualTo("Workload");
            assertThat(link.to()).isEqualTo("Variable 2");
        }

        @Test
        void shouldAllowCausalLinkBetweenCldAndSfElements() {
            editor.addCldVariable(); // Variable 1
            editor.addStock();       // Stock 1

            boolean result = editor.addCausalLink("Variable 1", "Stock 1",
                    CausalLinkDef.Polarity.POSITIVE);

            assertThat(result).isTrue();
            assertThat(editor.getCausalLinks()).hasSize(1);
        }

        @Test
        void shouldRejectDuplicateCausalLink() {
            editor.addCldVariable(); // Variable 1
            editor.addCldVariable(); // Variable 2

            boolean first = editor.addCausalLink("Variable 1", "Variable 2",
                    CausalLinkDef.Polarity.POSITIVE);
            boolean second = editor.addCausalLink("Variable 1", "Variable 2",
                    CausalLinkDef.Polarity.NEGATIVE);

            assertThat(first).isTrue();
            assertThat(second).isFalse();
            assertThat(editor.getCausalLinks()).hasSize(1);
            assertThat(editor.getCausalLinks().getFirst().polarity())
                    .isEqualTo(CausalLinkDef.Polarity.POSITIVE);
        }

        @Test
        void shouldAllowReverseLinkBetweenSameVariables() {
            editor.addCldVariable(); // Variable 1
            editor.addCldVariable(); // Variable 2

            boolean forward = editor.addCausalLink("Variable 1", "Variable 2",
                    CausalLinkDef.Polarity.POSITIVE);
            boolean reverse = editor.addCausalLink("Variable 2", "Variable 1",
                    CausalLinkDef.Polarity.NEGATIVE);

            assertThat(forward).isTrue();
            assertThat(reverse).isTrue();
            assertThat(editor.getCausalLinks()).hasSize(2);
        }
    }

    @Nested
    @DisplayName("classifyCldVariable")
    class ClassifyCldVariable {

        @Test
        void shouldClassifyAsStock() {
            editor.addCldVariable(); // Variable 1

            boolean result = editor.classifyCldVariable("Variable 1", ElementType.STOCK);

            assertThat(result).isTrue();
            assertThat(editor.getCldVariables()).isEmpty();
            assertThat(editor.getStocks()).hasSize(1);
            StockDef stock = editor.getStocks().getFirst();
            assertThat(stock.name()).isEqualTo("Variable 1");
            assertThat(stock.initialValue()).isEqualTo(0);
        }

        @Test
        void shouldClassifyAsFlow() {
            editor.addCldVariable(); // Variable 1

            boolean result = editor.classifyCldVariable("Variable 1", ElementType.FLOW);

            assertThat(result).isTrue();
            assertThat(editor.getCldVariables()).isEmpty();
            assertThat(editor.getFlows()).hasSize(1);
            FlowDef flow = editor.getFlows().getFirst();
            assertThat(flow.name()).isEqualTo("Variable 1");
        }

        @Test
        void shouldClassifyAsAux() {
            editor.addCldVariable(); // Variable 1

            boolean result = editor.classifyCldVariable("Variable 1", ElementType.AUX);

            assertThat(result).isTrue();
            assertThat(editor.getCldVariables()).isEmpty();
            assertThat(editor.getAuxiliaries()).hasSize(1);
            AuxDef aux = editor.getAuxiliaries().getFirst();
            assertThat(aux.name()).isEqualTo("Variable 1");
        }

        @Test
        void shouldClassifyAsConstant() {
            editor.addCldVariable(); // Variable 1

            boolean result = editor.classifyCldVariable("Variable 1", ElementType.CONSTANT);

            assertThat(result).isTrue();
            assertThat(editor.getCldVariables()).isEmpty();
            assertThat(editor.getConstants()).hasSize(1);
            ConstantDef constant = editor.getConstants().getFirst();
            assertThat(constant.name()).isEqualTo("Variable 1");
        }

        @Test
        void shouldPreserveComment() {
            editor.addCldVariable(); // Variable 1
            editor.setCldVariableComment("Variable 1", "Important variable");

            editor.classifyCldVariable("Variable 1", ElementType.STOCK);

            StockDef stock = editor.getStocks().getFirst();
            assertThat(stock.comment()).isEqualTo("Important variable");
        }

        @Test
        void shouldRemoveCausalLinksInvolvingClassifiedVariable() {
            editor.addCldVariable(); // Variable 1
            editor.addCldVariable(); // Variable 2
            editor.addCldVariable(); // Variable 3
            editor.addCausalLink("Variable 1", "Variable 2",
                    CausalLinkDef.Polarity.POSITIVE);
            editor.addCausalLink("Variable 3", "Variable 1",
                    CausalLinkDef.Polarity.NEGATIVE);
            editor.addCausalLink("Variable 2", "Variable 3",
                    CausalLinkDef.Polarity.POSITIVE);

            editor.classifyCldVariable("Variable 1", ElementType.STOCK);

            // Links involving Variable 1 should be removed
            assertThat(editor.getCausalLinks()).hasSize(1);
            assertThat(editor.getCausalLinks().getFirst().from()).isEqualTo("Variable 2");
            assertThat(editor.getCausalLinks().getFirst().to()).isEqualTo("Variable 3");
        }

        @Test
        void shouldReturnFalseForNonExistentVariable() {
            boolean result = editor.classifyCldVariable("NoSuchVar", ElementType.STOCK);

            assertThat(result).isFalse();
        }

        @Test
        void shouldReturnFalseForUnsupportedTargetType() {
            editor.addCldVariable(); // Variable 1

            boolean result = editor.classifyCldVariable("Variable 1", ElementType.MODULE);

            assertThat(result).isFalse();
            // Variable should still be a CLD variable
            assertThat(editor.getCldVariables()).hasSize(1);
        }

        @Test
        void shouldPreserveNameInNameIndex() {
            editor.addCldVariable(); // Variable 1

            editor.classifyCldVariable("Variable 1", ElementType.STOCK);

            // Name should still be registered (as a stock now)
            assertThat(editor.hasElement("Variable 1")).isTrue();
        }

        @Test
        void shouldPreserveOtherCldVariables() {
            editor.addCldVariable(); // Variable 1
            editor.addCldVariable(); // Variable 2

            editor.classifyCldVariable("Variable 1", ElementType.AUX);

            assertThat(editor.getCldVariables()).hasSize(1);
            assertThat(editor.getCldVariables().getFirst().name()).isEqualTo("Variable 2");
        }
    }
}
