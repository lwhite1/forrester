package systems.courant.sd.app.canvas;

import systems.courant.sd.model.def.ElementType;
import systems.courant.sd.model.def.FlowDef;
import systems.courant.sd.model.def.ModelDefinition;
import systems.courant.sd.model.def.ModelDefinitionBuilder;
import systems.courant.sd.model.def.ModuleInstanceDef;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("CopyPasteController")
class CopyPasteControllerTest {

    private CopyPasteController controller;
    private ModelEditor editor;
    private CanvasState canvasState;

    @BeforeEach
    void setUp() {
        controller = new CopyPasteController(new Clipboard());
        editor = new ModelEditor();
        canvasState = new CanvasState();
    }

    @Nested
    @DisplayName("copy and paste")
    class CopyAndPaste {

        @Test
        void shouldCopyAndPasteStock() {
            ModelDefinition def = new ModelDefinitionBuilder()
                    .name("Test")
                    .stock("Population", 100, "people")
                    .build();
            editor.loadFrom(def);
            canvasState.addElement("Population", ElementType.STOCK, 200, 300);
            canvasState.select("Population");

            controller.copy(canvasState, editor);
            assertThat(controller.hasContent()).isTrue();

            CopyPasteController.PasteResult pasteResult = controller.paste(canvasState, editor);
            List<String> pasted = pasteResult.pastedNames();

            assertThat(pasted).hasSize(1);
            String newName = pasted.get(0);
            assertThat(newName).isNotEqualTo("Population");
            assertThat(editor.getStockByName(newName)).isPresent();
            assertThat(editor.getStockByName(newName).orElseThrow().initialValue()).isEqualTo(100);
        }

        @Test
        void shouldCopyAndPasteConstant() {
            ModelDefinition def = new ModelDefinitionBuilder()
                    .name("Test")
                    .constant("Rate", 0.5, "1/day")
                    .build();
            editor.loadFrom(def);
            canvasState.addElement("Rate", ElementType.AUX, 100, 100);
            canvasState.select("Rate");

            controller.copy(canvasState, editor);
            CopyPasteController.PasteResult pasteResult = controller.paste(canvasState, editor);
            List<String> pasted = pasteResult.pastedNames();

            assertThat(pasted).hasSize(1);
            assertThat(editor.getVariableByName(pasted.get(0)).orElseThrow().literalValue()).isEqualTo(0.5);
        }

        @Test
        void shouldPasteMultipleElements() {
            ModelDefinition def = new ModelDefinitionBuilder()
                    .name("Test")
                    .stock("S1", 100, null)
                    .constant("C1", 5, null)
                    .build();
            editor.loadFrom(def);
            canvasState.addElement("S1", ElementType.STOCK, 100, 100);
            canvasState.addElement("C1", ElementType.AUX, 200, 200);
            canvasState.select("S1");
            canvasState.addToSelection("C1");

            controller.copy(canvasState, editor);
            CopyPasteController.PasteResult pasteResult = controller.paste(canvasState, editor);
            List<String> pasted = pasteResult.pastedNames();

            assertThat(pasted).hasSize(2);
        }

        @Test
        void shouldReconnectFlowEndpointsWhenBothEndsCopied() {
            ModelDefinition def = new ModelDefinitionBuilder()
                    .name("Test")
                    .stock("Source", 100, null)
                    .stock("Sink", 0, null)
                    .flow("Transfer", "Source * 0.1", "day", "Source", "Sink")
                    .build();
            editor.loadFrom(def);
            canvasState.addElement("Source", ElementType.STOCK, 100, 100);
            canvasState.addElement("Sink", ElementType.STOCK, 300, 100);
            canvasState.addElement("Transfer", ElementType.FLOW, 200, 100);
            canvasState.select("Source");
            canvasState.addToSelection("Sink");
            canvasState.addToSelection("Transfer");

            controller.copy(canvasState, editor);
            CopyPasteController.PasteResult pasteResult = controller.paste(canvasState, editor);
            List<String> pasted = pasteResult.pastedNames();

            assertThat(pasted).hasSize(3);

            // Find the pasted flow
            String pastedFlowName = pasted.stream()
                    .filter(n -> editor.getFlowByName(n).isPresent())
                    .findFirst()
                    .orElse(null);
            assertThat(pastedFlowName).isNotNull();

            FlowDef pastedFlow = editor.getFlowByName(pastedFlowName).orElseThrow();
            // Source and sink should be reconnected to the pasted stocks
            assertThat(pastedFlow.source()).isNotNull();
            assertThat(pastedFlow.sink()).isNotNull();
            assertThat(pastedFlow.source()).isNotEqualTo("Source");
            assertThat(pastedFlow.sink()).isNotEqualTo("Sink");
        }

        @Test
        void shouldNotPasteWhenClipboardEmpty() {
            CopyPasteController.PasteResult pasteResult = controller.paste(canvasState, editor);
            List<String> pasted = pasteResult.pastedNames();
            assertThat(pasted).isEmpty();
        }

        @Test
        void shouldNotCopyWhenSelectionEmpty() {
            controller.copy(canvasState, editor);
            assertThat(controller.hasContent()).isFalse();
        }

        @Test
        void shouldPasteMultipleTimesFromSameClipboard() {
            editor.addStock();
            canvasState.addElement("Stock 1", ElementType.STOCK, 100, 100);
            canvasState.select("Stock 1");

            controller.copy(canvasState, editor);

            List<String> paste1 = controller.paste(canvasState, editor).pastedNames();
            List<String> paste2 = controller.paste(canvasState, editor).pastedNames();

            assertThat(paste1).hasSize(1);
            assertThat(paste2).hasSize(1);
            assertThat(paste1.get(0)).isNotEqualTo(paste2.get(0));
        }
    }

    @Nested
    @DisplayName("clearDanglingReferences")
    class ClearDanglingReferences {

        @Test
        void shouldReplaceMissingElementWithZero() {
            // editor has no elements — all references are dangling
            String result = CopyPasteController.clearDanglingReferences(
                    "Growth_Rate * Population", editor).equation();
            assertThat(result).isEqualTo("0 * 0");
        }

        @Test
        void shouldPreserveExistingElements() {
            ModelDefinition def = new ModelDefinitionBuilder()
                    .name("Test")
                    .stock("Population", 100, null)
                    .build();
            editor.loadFrom(def);

            String result = CopyPasteController.clearDanglingReferences(
                    "Population * Growth_Rate", editor).equation();
            assertThat(result).isEqualTo("Population * 0");
        }

        @Test
        void shouldPreserveNumericLiterals() {
            String result = CopyPasteController.clearDanglingReferences(
                    "100 + 3.14 + 1e10", editor).equation();
            assertThat(result).isEqualTo("100 + 3.14 + 1e10");
        }

        @Test
        void shouldPreserveKeywords() {
            String result = CopyPasteController.clearDanglingReferences(
                    "TIME + DT", editor).equation();
            assertThat(result).isEqualTo("TIME + DT");
        }

        @Test
        void shouldPreserveBuiltInConstantPI() {
            String result = CopyPasteController.clearDanglingReferences(
                    "2 * PI * radius", editor).equation();
            assertThat(result).isEqualTo("2 * PI * 0");
        }

        @Test
        void shouldPreserveBuiltInConstantE() {
            String result = CopyPasteController.clearDanglingReferences(
                    "E + missing", editor).equation();
            assertThat(result).isEqualTo("E + 0");
        }

        @Test
        void shouldPreservePIAndEInComplexExpression() {
            String result = CopyPasteController.clearDanglingReferences(
                    "PI * E + TIME", editor).equation();
            assertThat(result).isEqualTo("PI * E + TIME");
        }

        @Test
        void shouldPreserveFunctionCalls() {
            String result = CopyPasteController.clearDanglingReferences(
                    "MAX(Missing, 0)", editor).equation();
            assertThat(result).isEqualTo("MAX(0, 0)");
        }

        @Test
        void shouldPreserveIfKeyword() {
            ModelDefinition def = new ModelDefinitionBuilder()
                    .name("Test")
                    .stock("Population", 100, null)
                    .build();
            editor.loadFrom(def);

            String result = CopyPasteController.clearDanglingReferences(
                    "IF(Population > 0, Population, Missing)", editor).equation();
            assertThat(result).isEqualTo("IF(Population > 0, Population, 0)");
        }

        @Test
        void shouldPreserveReferencesToElementsWithUnderscores() {
            ModelDefinition def = new ModelDefinitionBuilder()
                    .name("Test")
                    .constant("Outflow_Rate", 5.0, null)
                    .build();
            editor.loadFrom(def);

            String result = CopyPasteController.clearDanglingReferences(
                    "Outflow_Rate", editor).equation();
            assertThat(result).isEqualTo("Outflow_Rate");
        }

        @Test
        void shouldHandleElementNamesWithSpaces() {
            ModelDefinition def = new ModelDefinitionBuilder()
                    .name("Test")
                    .constant("Birth Rate", 0.5, null)
                    .build();
            editor.loadFrom(def);

            String result = CopyPasteController.clearDanglingReferences(
                    "Birth_Rate * Missing_Stock", editor).equation();
            assertThat(result).isEqualTo("Birth_Rate * 0");
        }

        @Test
        void shouldReportReplacedReferences() {
            var cr = CopyPasteController.clearDanglingReferences(
                    "Growth_Rate * Contact_Rate", editor);
            assertThat(cr.equation()).isEqualTo("0 * 0");
            assertThat(cr.replaced()).containsExactlyInAnyOrder("Growth Rate", "Contact Rate");
        }

        @Test
        void shouldHandleBacktickQuotedIdentifiers() {
            ModelDefinition def = new ModelDefinitionBuilder()
                    .name("Test")
                    .stock("Population", 100, null)
                    .build();
            editor.loadFrom(def);

            var cr = CopyPasteController.clearDanglingReferences(
                    "`Population` * `Growth Rate`", editor);
            assertThat(cr.equation()).isEqualTo("`Population` * 0");
            assertThat(cr.replaced()).containsExactly("Growth Rate");
        }
    }

    @Nested
    @DisplayName("paste with dangling references")
    class PasteWithDanglingReferences {

        @Test
        void shouldReplaceDanglingReferencesInFlowEquation() {
            // Build model with stock, constant, and a flow referencing both
            ModelDefinition def = new ModelDefinitionBuilder()
                    .name("Test")
                    .stock("Population", 100, null)
                    .constant("Growth Rate", 0.05, null)
                    .flow("Births", "Population * Growth_Rate", "day", "Population", null)
                    .build();
            editor.loadFrom(def);
            canvasState.addElement("Population", ElementType.STOCK, 100, 100);
            canvasState.addElement("Growth Rate", ElementType.AUX, 200, 100);
            canvasState.addElement("Births", ElementType.FLOW, 150, 100);

            // Copy only the flow (not the stock or constant it references)
            canvasState.select("Births");
            controller.copy(canvasState, editor);

            // Paste into a fresh editor with no pre-existing elements
            ModelEditor targetEditor = new ModelEditor();
            CanvasState targetCanvas = new CanvasState();
            List<String> pasted = controller.paste(targetCanvas, targetEditor).pastedNames();

            assertThat(pasted).hasSize(1);
            String pastedFlowName = pasted.get(0);
            FlowDef pastedFlow = targetEditor.getFlowByName(pastedFlowName).orElseThrow();
            // Both Population and Growth_Rate don't exist in target, replaced with 0
            assertThat(pastedFlow.equation()).isEqualTo("0 * 0");
        }

        @Test
        void shouldPreserveCopiedElementReferencesAndReplaceMissing() {
            // Build model with stock, constant, and a flow referencing both
            ModelDefinition def = new ModelDefinitionBuilder()
                    .name("Test")
                    .stock("Population", 100, null)
                    .constant("Growth Rate", 0.05, null)
                    .flow("Births", "Population * Growth_Rate", "day", "Population", null)
                    .build();
            editor.loadFrom(def);
            canvasState.addElement("Population", ElementType.STOCK, 100, 100);
            canvasState.addElement("Growth Rate", ElementType.AUX, 200, 100);
            canvasState.addElement("Births", ElementType.FLOW, 150, 100);

            // Copy the stock and the flow, but NOT the constant
            canvasState.select("Population");
            canvasState.addToSelection("Births");
            controller.copy(canvasState, editor);

            // Paste into a fresh editor
            ModelEditor targetEditor = new ModelEditor();
            CanvasState targetCanvas = new CanvasState();
            List<String> pasted = controller.paste(targetCanvas, targetEditor).pastedNames();

            assertThat(pasted).hasSize(2);
            String pastedFlowName = pasted.stream()
                    .filter(n -> targetEditor.getFlowByName(n).isPresent())
                    .findFirst()
                    .orElse(null);
            assertThat(pastedFlowName).isNotNull();

            FlowDef pastedFlow = targetEditor.getFlowByName(pastedFlowName).orElseThrow();
            // The pasted stock reference should be remapped; Growth_Rate replaced with 0
            assertThat(pastedFlow.equation()).doesNotContain("Growth_Rate");
            assertThat(pastedFlow.equation()).contains("0");
            // The pasted stock's new name should appear in the equation
            String pastedStockName = pasted.stream()
                    .filter(n -> targetEditor.getStockByName(n).isPresent())
                    .findFirst()
                    .orElse(null);
            assertThat(pastedFlow.equation())
                    .contains(pastedStockName.replace(' ', '_'));
        }
    }

    @Nested
    @DisplayName("paste module bindings")
    class PasteModuleBindings {

        @Test
        void shouldRemapModuleInputBindingsForCopiedElements() {
            // Inner module definition (minimal)
            ModelDefinition innerDef = new ModelDefinitionBuilder()
                    .name("Inner")
                    .stock("X", 0, null)
                    .build();

            // Source model with a constant and a module whose input binds to it
            ModelDefinition def = new ModelDefinitionBuilder()
                    .name("Test")
                    .constant("Rate", 0.5, null)
                    .module("Mod1", innerDef,
                            Map.of("input_port", "Rate"),
                            Map.of("output_port", "Rate"))
                    .build();
            editor.loadFrom(def);
            canvasState.addElement("Rate", ElementType.AUX, 100, 100);
            canvasState.addElement("Mod1", ElementType.MODULE, 200, 200);

            // Copy both the constant and module
            canvasState.select("Rate");
            canvasState.addToSelection("Mod1");
            controller.copy(canvasState, editor);

            // Paste into the same editor
            CopyPasteController.PasteResult pasteResult = controller.paste(canvasState, editor);
            List<String> pasted = pasteResult.pastedNames();
            assertThat(pasted).hasSize(2);

            String pastedModuleName = pasted.stream()
                    .filter(n -> editor.getModuleByName(n).isPresent())
                    .findFirst()
                    .orElse(null);
            assertThat(pastedModuleName).isNotNull();

            String pastedConstName = pasted.stream()
                    .filter(n -> editor.getVariableByName(n).isPresent()
                            && !n.equals("Rate"))
                    .findFirst()
                    .orElse(null);
            assertThat(pastedConstName).isNotNull();

            ModuleInstanceDef pastedModule = editor.getModuleByName(pastedModuleName).orElseThrow();
            // Input binding should reference the pasted constant, not original "Rate"
            assertThat(pastedModule.inputBindings().get("input_port"))
                    .isEqualTo(pastedConstName.replace(' ', '_'));
            // Output binding should also be remapped
            assertThat(pastedModule.outputBindings().get("output_port"))
                    .isEqualTo(pastedConstName.replace(' ', '_'));
        }

        @Test
        void shouldReplaceDanglingModuleBindingsWithZero() {
            ModelDefinition innerDef = new ModelDefinitionBuilder()
                    .name("Inner")
                    .stock("X", 0, null)
                    .build();

            ModelDefinition def = new ModelDefinitionBuilder()
                    .name("Test")
                    .constant("Rate", 0.5, null)
                    .module("Mod1", innerDef,
                            Map.of("input_port", "Rate"),
                            Map.of())
                    .build();
            editor.loadFrom(def);
            canvasState.addElement("Rate", ElementType.AUX, 100, 100);
            canvasState.addElement("Mod1", ElementType.MODULE, 200, 200);

            // Copy only the module (not the constant it references)
            canvasState.select("Mod1");
            controller.copy(canvasState, editor);

            // Paste into a fresh editor
            ModelEditor targetEditor = new ModelEditor();
            CanvasState targetCanvas = new CanvasState();
            List<String> pasted = controller.paste(targetCanvas, targetEditor).pastedNames();

            assertThat(pasted).hasSize(1);
            ModuleInstanceDef pastedModule = targetEditor.getModuleByName(pasted.get(0)).orElseThrow();
            // "Rate" doesn't exist in target — input should be replaced with 0
            assertThat(pastedModule.inputBindings().get("input_port")).isEqualTo("0");
        }

        @Test
        void shouldRemoveDanglingOutputBindingsInsteadOfReplacingWithZero() {
            ModelDefinition innerDef = new ModelDefinitionBuilder()
                    .name("Inner")
                    .stock("X", 0, null)
                    .build();

            ModelDefinition def = new ModelDefinitionBuilder()
                    .name("Test")
                    .constant("Rate", 0.5, null)
                    .module("Mod1", innerDef,
                            Map.of("input_port", "Rate"),
                            Map.of("output_port", "Rate"))
                    .build();
            editor.loadFrom(def);
            canvasState.addElement("Rate", ElementType.AUX, 100, 100);
            canvasState.addElement("Mod1", ElementType.MODULE, 200, 200);

            // Copy only the module (not the constant it references)
            canvasState.select("Mod1");
            controller.copy(canvasState, editor);

            ModelEditor targetEditor = new ModelEditor();
            CanvasState targetCanvas = new CanvasState();
            List<String> pasted = controller.paste(targetCanvas, targetEditor).pastedNames();

            ModuleInstanceDef pastedModule = targetEditor.getModuleByName(pasted.get(0)).orElseThrow();
            // Output binding should be removed, not "0"
            assertThat(pastedModule.outputBindings()).doesNotContainKey("output_port");
            // Input binding still gets "0" (valid as expression)
            assertThat(pastedModule.inputBindings().get("input_port")).isEqualTo("0");
        }
    }

    @Nested
    @DisplayName("paste result reports replaced references")
    class PasteResultReporting {

        @Test
        void shouldReportReplacedReferencesInPasteResult() {
            ModelDefinition def = new ModelDefinitionBuilder()
                    .name("Test")
                    .stock("Population", 100, null)
                    .constant("Growth Rate", 0.05, null)
                    .flow("Births", "Population * Growth_Rate", "day", "Population", null)
                    .build();
            editor.loadFrom(def);
            canvasState.addElement("Population", ElementType.STOCK, 100, 100);
            canvasState.addElement("Growth Rate", ElementType.AUX, 200, 100);
            canvasState.addElement("Births", ElementType.FLOW, 150, 100);

            // Copy only the flow
            canvasState.select("Births");
            controller.copy(canvasState, editor);

            ModelEditor targetEditor = new ModelEditor();
            CanvasState targetCanvas = new CanvasState();
            CopyPasteController.PasteResult result = controller.paste(targetCanvas, targetEditor);

            assertThat(result.pastedNames()).hasSize(1);
            assertThat(result.replacedReferences()).containsExactlyInAnyOrder(
                    "Population", "Growth Rate");
        }

        @Test
        void shouldReturnEmptyReplacedWhenAllReferencesExist() {
            ModelDefinition def = new ModelDefinitionBuilder()
                    .name("Test")
                    .stock("Population", 100, null)
                    .constant("Rate", 0.05, null)
                    .flow("Growth", "Population * Rate", "day", "Population", null)
                    .build();
            editor.loadFrom(def);
            canvasState.addElement("Population", ElementType.STOCK, 100, 100);
            canvasState.addElement("Rate", ElementType.AUX, 200, 100);
            canvasState.addElement("Growth", ElementType.FLOW, 150, 100);

            canvasState.select("Population");
            canvasState.addToSelection("Rate");
            canvasState.addToSelection("Growth");
            controller.copy(canvasState, editor);

            ModelEditor targetEditor = new ModelEditor();
            CanvasState targetCanvas = new CanvasState();
            CopyPasteController.PasteResult result = controller.paste(targetCanvas, targetEditor);

            assertThat(result.replacedReferences()).isEmpty();
        }
    }

    @Nested
    @DisplayName("cross-model paste preserves names and relationships")
    class CrossModelPaste {

        @Test
        void shouldPreserveOriginalNamesWhenPastingIntoEmptyModel() {
            ModelDefinition def = new ModelDefinitionBuilder()
                    .name("Source Model")
                    .stock("Population", 100, "people")
                    .constant("Growth Rate", 0.05, "1/year")
                    .build();
            editor.loadFrom(def);
            canvasState.addElement("Population", ElementType.STOCK, 100, 100);
            canvasState.addElement("Growth Rate", ElementType.AUX, 200, 200);
            canvasState.select("Population");
            canvasState.addToSelection("Growth Rate");

            controller.copy(canvasState, editor);

            ModelEditor targetEditor = new ModelEditor();
            CanvasState targetCanvas = new CanvasState();
            List<String> pasted = controller.paste(targetCanvas, targetEditor).pastedNames();

            assertThat(pasted).containsExactlyInAnyOrder("Population", "Growth Rate");
            assertThat(targetEditor.getStockByName("Population").orElseThrow().initialValue()).isEqualTo(100);
            assertThat(targetEditor.getVariableByName("Growth Rate").orElseThrow().literalValue()).isEqualTo(0.05);
        }

        @Test
        void shouldPreserveFlowConnectionsWhenPastingIntoEmptyModel() {
            ModelDefinition def = new ModelDefinitionBuilder()
                    .name("Source Model")
                    .stock("Source", 100, null)
                    .stock("Sink", 0, null)
                    .flow("Transfer", "Source * 0.1", "day", "Source", "Sink")
                    .build();
            editor.loadFrom(def);
            canvasState.addElement("Source", ElementType.STOCK, 100, 100);
            canvasState.addElement("Sink", ElementType.STOCK, 300, 100);
            canvasState.addElement("Transfer", ElementType.FLOW, 200, 100);
            canvasState.select("Source");
            canvasState.addToSelection("Sink");
            canvasState.addToSelection("Transfer");

            controller.copy(canvasState, editor);

            ModelEditor targetEditor = new ModelEditor();
            CanvasState targetCanvas = new CanvasState();
            List<String> pasted = controller.paste(targetCanvas, targetEditor).pastedNames();

            assertThat(pasted).containsExactlyInAnyOrder("Source", "Sink", "Transfer");

            FlowDef pastedFlow = targetEditor.getFlowByName("Transfer").orElseThrow();
            assertThat(pastedFlow.source()).isEqualTo("Source");
            assertThat(pastedFlow.sink()).isEqualTo("Sink");
            assertThat(pastedFlow.equation()).isEqualTo("Source * 0.1");
        }

        @Test
        void shouldPreserveBathtubModelConnectionsAcrossModels() {
            // Reproduces the bathtub model scenario: constants with underscored names
            ModelDefinition def = new ModelDefinitionBuilder()
                    .name("Bathtub")
                    .stock("Water_in_Tub", 50, "Gallon")
                    .flow("Outflow", "MIN(Outflow_Rate, Water_in_Tub)", "Minute",
                            "Water_in_Tub", null)
                    .flow("Inflow", "STEP(Inflow_Rate, 5)", "Minute",
                            null, "Water_in_Tub")
                    .constant("Outflow_Rate", 5.0, "Gallon per Minute")
                    .constant("Inflow_Rate", 5.0, "Gallon per Minute")
                    .build();
            editor.loadFrom(def);
            canvasState.addElement("Water_in_Tub", ElementType.STOCK, 200, 200);
            canvasState.addElement("Outflow", ElementType.FLOW, 350, 200);
            canvasState.addElement("Inflow", ElementType.FLOW, 50, 200);
            canvasState.addElement("Outflow_Rate", ElementType.AUX, 350, 350);
            canvasState.addElement("Inflow_Rate", ElementType.AUX, 50, 350);
            canvasState.select("Water_in_Tub");
            canvasState.addToSelection("Outflow");
            canvasState.addToSelection("Inflow");
            canvasState.addToSelection("Outflow_Rate");
            canvasState.addToSelection("Inflow_Rate");

            controller.copy(canvasState, editor);

            ModelEditor targetEditor = new ModelEditor();
            CanvasState targetCanvas = new CanvasState();
            List<String> pasted = controller.paste(targetCanvas, targetEditor).pastedNames();

            assertThat(pasted).containsExactlyInAnyOrder(
                    "Water_in_Tub", "Outflow", "Inflow", "Outflow_Rate", "Inflow_Rate");

            // Verify equations preserved — constant references not replaced with 0
            FlowDef outflow = targetEditor.getFlowByName("Outflow").orElseThrow();
            assertThat(outflow.equation()).isEqualTo("MIN(Outflow_Rate, Water_in_Tub)");
            assertThat(outflow.source()).isEqualTo("Water_in_Tub");

            FlowDef inflow = targetEditor.getFlowByName("Inflow").orElseThrow();
            assertThat(inflow.equation()).isEqualTo("STEP(Inflow_Rate, 5)");
            assertThat(inflow.sink()).isEqualTo("Water_in_Tub");
        }

        @Test
        void shouldPreserveConstantToFlowEquationReference() {
            ModelDefinition def = new ModelDefinitionBuilder()
                    .name("Source Model")
                    .stock("Population", 100, null)
                    .constant("Rate", 0.05, null)
                    .flow("Growth", "Population * Rate", "day", "Population", null)
                    .build();
            editor.loadFrom(def);
            canvasState.addElement("Population", ElementType.STOCK, 100, 100);
            canvasState.addElement("Rate", ElementType.AUX, 200, 200);
            canvasState.addElement("Growth", ElementType.FLOW, 150, 150);
            canvasState.select("Population");
            canvasState.addToSelection("Rate");
            canvasState.addToSelection("Growth");

            controller.copy(canvasState, editor);

            ModelEditor targetEditor = new ModelEditor();
            CanvasState targetCanvas = new CanvasState();
            List<String> pasted = controller.paste(targetCanvas, targetEditor).pastedNames();

            assertThat(pasted).containsExactlyInAnyOrder("Population", "Rate", "Growth");

            FlowDef pastedFlow = targetEditor.getFlowByName("Growth").orElseThrow();
            assertThat(pastedFlow.equation()).isEqualTo("Population * Rate");
            assertThat(pastedFlow.source()).isEqualTo("Population");

            // Verify the constant exists so connectors would be generated
            assertThat(targetEditor.getVariableByName("Rate")).isPresent();
            assertThat(targetEditor.hasElement("Rate")).isTrue();
        }

        @Test
        void shouldFallBackToGeneratedNameOnConflict() {
            ModelDefinition def = new ModelDefinitionBuilder()
                    .name("Test")
                    .stock("Population", 100, null)
                    .build();
            editor.loadFrom(def);
            canvasState.addElement("Population", ElementType.STOCK, 100, 100);
            canvasState.select("Population");

            controller.copy(canvasState, editor);

            // Target already has "Population"
            ModelEditor targetEditor = new ModelEditor();
            ModelDefinition targetDef = new ModelDefinitionBuilder()
                    .name("Target")
                    .stock("Population", 50, null)
                    .build();
            targetEditor.loadFrom(targetDef);
            CanvasState targetCanvas = new CanvasState();
            targetCanvas.addElement("Population", ElementType.STOCK, 100, 100);
            List<String> pasted = controller.paste(targetCanvas, targetEditor).pastedNames();

            assertThat(pasted).hasSize(1);
            assertThat(pasted.get(0)).isNotEqualTo("Population");
            assertThat(targetEditor.getStockByName(pasted.get(0)).orElseThrow().initialValue()).isEqualTo(100);
        }
    }

    @Nested
    @DisplayName("remapEquationTokens")
    class RemapEquationTokens {

        @Test
        void shouldRemapSingleToken() {
            Map<String, String> mapping = Map.of("Old Name", "New Name");
            String result = CopyPasteController.remapEquationTokens(
                    "Old_Name * 10", mapping);
            assertThat(result).isEqualTo("New_Name * 10");
        }

        @Test
        void shouldRemapMultipleTokens() {
            Map<String, String> mapping = Map.of(
                    "A", "X",
                    "B", "Y");
            String result = CopyPasteController.remapEquationTokens("A + B", mapping);
            assertThat(result).isEqualTo("X + Y");
        }

        @Test
        void shouldNotRemapUnmappedTokens() {
            Map<String, String> mapping = Map.of("A", "X");
            String result = CopyPasteController.remapEquationTokens(
                    "A + B + C", mapping);
            assertThat(result).isEqualTo("X + B + C");
        }

        @Test
        void shouldRemapBacktickQuotedIdentifiers() {
            Map<String, String> mapping = Map.of("Tasks Remaining", "Open Items");
            String result = CopyPasteController.remapEquationTokens(
                    "`Tasks Remaining` * Rate", mapping);
            assertThat(result).isEqualTo("`Open Items` * Rate");
        }

        @Test
        void shouldLeaveUnmappedBacktickTokensUntouched() {
            Map<String, String> mapping = Map.of("A", "X");
            String result = CopyPasteController.remapEquationTokens(
                    "`Unknown Name` + A", mapping);
            assertThat(result).isEqualTo("`Unknown Name` + X");
        }
    }

    @Nested
    @DisplayName("paste with viewport center")
    class PasteWithViewportCenter {

        @Test
        void shouldPlaceElementsAtViewportCenterWhenNoSelection() {
            ModelDefinition def = new ModelDefinitionBuilder()
                    .name("Test")
                    .stock("Population", 100, "people")
                    .build();
            editor.loadFrom(def);
            canvasState.addElement("Population", ElementType.STOCK, 200, 300);
            canvasState.select("Population");

            controller.copy(canvasState, editor);

            // Paste into a fresh canvas with no selection, providing a viewport center
            // far from the origin to simulate a panned view
            ModelEditor targetEditor = new ModelEditor();
            CanvasState targetCanvas = new CanvasState();
            var viewportCenter = new CanvasState.Position(5000, 3000);

            CopyPasteController.PasteResult result =
                    controller.paste(targetCanvas, targetEditor, viewportCenter);
            List<String> pasted = result.pastedNames();

            assertThat(pasted).hasSize(1);
            String pastedName = pasted.get(0);
            // The element should be placed near the viewport center, not at (30, 30)
            double pastedX = targetCanvas.getX(pastedName);
            double pastedY = targetCanvas.getY(pastedName);
            assertThat(pastedX).isEqualTo(5000.0);
            assertThat(pastedY).isEqualTo(3000.0);
        }

        @Test
        void shouldPreserveRelativePositionsWhenPastingAtViewportCenter() {
            ModelDefinition def = new ModelDefinitionBuilder()
                    .name("Test")
                    .stock("S1", 100, null)
                    .constant("C1", 5, null)
                    .build();
            editor.loadFrom(def);
            canvasState.addElement("S1", ElementType.STOCK, 100, 100);
            canvasState.addElement("C1", ElementType.AUX, 200, 300);
            canvasState.select("S1");
            canvasState.addToSelection("C1");

            controller.copy(canvasState, editor);

            ModelEditor targetEditor = new ModelEditor();
            CanvasState targetCanvas = new CanvasState();
            var viewportCenter = new CanvasState.Position(4000, 2000);

            CopyPasteController.PasteResult result =
                    controller.paste(targetCanvas, targetEditor, viewportCenter);
            List<String> pasted = result.pastedNames();

            assertThat(pasted).hasSize(2);
            double x0 = targetCanvas.getX(pasted.get(0));
            double y0 = targetCanvas.getY(pasted.get(0));
            double x1 = targetCanvas.getX(pasted.get(1));
            double y1 = targetCanvas.getY(pasted.get(1));
            // The relative offset between the two elements should be preserved:
            // original offset was (200-100, 300-100) = (100, 200)
            assertThat(Math.abs((x1 - x0) - 100)).isLessThan(0.001);
            assertThat(Math.abs((y1 - y0) - 200)).isLessThan(0.001);
        }

        @Test
        void shouldUseSelectionAnchorWhenSelectionExistsEvenWithViewportCenter() {
            ModelDefinition def = new ModelDefinitionBuilder()
                    .name("Test")
                    .stock("Population", 100, "people")
                    .build();
            editor.loadFrom(def);
            canvasState.addElement("Population", ElementType.STOCK, 200, 300);
            canvasState.select("Population");

            controller.copy(canvasState, editor);

            // Selection is still active, so viewport center should be ignored
            var viewportCenter = new CanvasState.Position(9999, 9999);
            CopyPasteController.PasteResult result =
                    controller.paste(canvasState, editor, viewportCenter);
            List<String> pasted = result.pastedNames();

            assertThat(pasted).hasSize(1);
            String pastedName = pasted.get(0);
            double pastedX = canvasState.getX(pastedName);
            double pastedY = canvasState.getY(pastedName);
            // Should be anchored to selection center (200,300) + offset (30,30) = (230, 330)
            assertThat(pastedX).isEqualTo(230.0);
            assertThat(pastedY).isEqualTo(330.0);
        }

        @Test
        void shouldFallBackToFixedOffsetWhenNoViewportCenterProvided() {
            ModelDefinition def = new ModelDefinitionBuilder()
                    .name("Test")
                    .stock("Population", 100, "people")
                    .build();
            editor.loadFrom(def);
            canvasState.addElement("Population", ElementType.STOCK, 200, 300);
            canvasState.select("Population");

            controller.copy(canvasState, editor);

            // Paste with no selection and null viewport center (backwards compatibility)
            ModelEditor targetEditor = new ModelEditor();
            CanvasState targetCanvas = new CanvasState();

            CopyPasteController.PasteResult result =
                    controller.paste(targetCanvas, targetEditor, null);
            List<String> pasted = result.pastedNames();

            assertThat(pasted).hasSize(1);
            String pastedName = pasted.get(0);
            double pastedX = targetCanvas.getX(pastedName);
            double pastedY = targetCanvas.getY(pastedName);
            // Should fall back to (30, 30)
            assertThat(pastedX).isEqualTo(30.0);
            assertThat(pastedY).isEqualTo(30.0);
        }
    }
}
