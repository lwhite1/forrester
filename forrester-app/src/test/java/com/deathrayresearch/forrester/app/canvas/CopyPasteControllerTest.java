package com.deathrayresearch.forrester.app.canvas;

import com.deathrayresearch.forrester.model.def.ElementType;
import com.deathrayresearch.forrester.model.def.FlowDef;
import com.deathrayresearch.forrester.model.def.ModelDefinition;
import com.deathrayresearch.forrester.model.def.ModelDefinitionBuilder;
import com.deathrayresearch.forrester.model.def.ModuleInstanceDef;

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

            List<String> pasted = controller.paste(canvasState, editor);

            assertThat(pasted).hasSize(1);
            String newName = pasted.get(0);
            assertThat(newName).isNotEqualTo("Population");
            assertThat(editor.getStockByName(newName)).isNotNull();
            assertThat(editor.getStockByName(newName).initialValue()).isEqualTo(100);
        }

        @Test
        void shouldCopyAndPasteConstant() {
            ModelDefinition def = new ModelDefinitionBuilder()
                    .name("Test")
                    .constant("Rate", 0.5, "1/day")
                    .build();
            editor.loadFrom(def);
            canvasState.addElement("Rate", ElementType.CONSTANT, 100, 100);
            canvasState.select("Rate");

            controller.copy(canvasState, editor);
            List<String> pasted = controller.paste(canvasState, editor);

            assertThat(pasted).hasSize(1);
            assertThat(editor.getConstantByName(pasted.get(0)).value()).isEqualTo(0.5);
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
            canvasState.addElement("C1", ElementType.CONSTANT, 200, 200);
            canvasState.select("S1");
            canvasState.addToSelection("C1");

            controller.copy(canvasState, editor);
            List<String> pasted = controller.paste(canvasState, editor);

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
            List<String> pasted = controller.paste(canvasState, editor);

            assertThat(pasted).hasSize(3);

            // Find the pasted flow
            String pastedFlowName = pasted.stream()
                    .filter(n -> editor.getFlowByName(n) != null)
                    .findFirst()
                    .orElse(null);
            assertThat(pastedFlowName).isNotNull();

            FlowDef pastedFlow = editor.getFlowByName(pastedFlowName);
            // Source and sink should be reconnected to the pasted stocks
            assertThat(pastedFlow.source()).isNotNull();
            assertThat(pastedFlow.sink()).isNotNull();
            assertThat(pastedFlow.source()).isNotEqualTo("Source");
            assertThat(pastedFlow.sink()).isNotEqualTo("Sink");
        }

        @Test
        void shouldNotPasteWhenClipboardEmpty() {
            List<String> pasted = controller.paste(canvasState, editor);
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

            List<String> paste1 = controller.paste(canvasState, editor);
            List<String> paste2 = controller.paste(canvasState, editor);

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
                    "Growth_Rate * Population", editor);
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
                    "Population * Growth_Rate", editor);
            assertThat(result).isEqualTo("Population * 0");
        }

        @Test
        void shouldPreserveNumericLiterals() {
            String result = CopyPasteController.clearDanglingReferences(
                    "100 + 3.14 + 1e10", editor);
            assertThat(result).isEqualTo("100 + 3.14 + 1e10");
        }

        @Test
        void shouldPreserveKeywords() {
            String result = CopyPasteController.clearDanglingReferences(
                    "TIME + DT", editor);
            assertThat(result).isEqualTo("TIME + DT");
        }

        @Test
        void shouldPreserveFunctionCalls() {
            String result = CopyPasteController.clearDanglingReferences(
                    "MAX(Missing, 0)", editor);
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
                    "IF(Population > 0, Population, Missing)", editor);
            assertThat(result).isEqualTo("IF(Population > 0, Population, 0)");
        }

        @Test
        void shouldHandleElementNamesWithSpaces() {
            ModelDefinition def = new ModelDefinitionBuilder()
                    .name("Test")
                    .constant("Birth Rate", 0.5, null)
                    .build();
            editor.loadFrom(def);

            String result = CopyPasteController.clearDanglingReferences(
                    "Birth_Rate * Missing_Stock", editor);
            assertThat(result).isEqualTo("Birth_Rate * 0");
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
            canvasState.addElement("Growth Rate", ElementType.CONSTANT, 200, 100);
            canvasState.addElement("Births", ElementType.FLOW, 150, 100);

            // Copy only the flow (not the stock or constant it references)
            canvasState.select("Births");
            controller.copy(canvasState, editor);

            // Paste into a fresh editor with no pre-existing elements
            ModelEditor targetEditor = new ModelEditor();
            CanvasState targetCanvas = new CanvasState();
            List<String> pasted = controller.paste(targetCanvas, targetEditor);

            assertThat(pasted).hasSize(1);
            String pastedFlowName = pasted.get(0);
            FlowDef pastedFlow = targetEditor.getFlowByName(pastedFlowName);
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
            canvasState.addElement("Growth Rate", ElementType.CONSTANT, 200, 100);
            canvasState.addElement("Births", ElementType.FLOW, 150, 100);

            // Copy the stock and the flow, but NOT the constant
            canvasState.select("Population");
            canvasState.addToSelection("Births");
            controller.copy(canvasState, editor);

            // Paste into a fresh editor
            ModelEditor targetEditor = new ModelEditor();
            CanvasState targetCanvas = new CanvasState();
            List<String> pasted = controller.paste(targetCanvas, targetEditor);

            assertThat(pasted).hasSize(2);
            String pastedFlowName = pasted.stream()
                    .filter(n -> targetEditor.getFlowByName(n) != null)
                    .findFirst()
                    .orElse(null);
            assertThat(pastedFlowName).isNotNull();

            FlowDef pastedFlow = targetEditor.getFlowByName(pastedFlowName);
            // The pasted stock reference should be remapped; Growth_Rate replaced with 0
            assertThat(pastedFlow.equation()).doesNotContain("Growth_Rate");
            assertThat(pastedFlow.equation()).contains("0");
            // The pasted stock's new name should appear in the equation
            String pastedStockName = pasted.stream()
                    .filter(n -> targetEditor.getStockByName(n) != null)
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
            canvasState.addElement("Rate", ElementType.CONSTANT, 100, 100);
            canvasState.addElement("Mod1", ElementType.MODULE, 200, 200);

            // Copy both the constant and module
            canvasState.select("Rate");
            canvasState.addToSelection("Mod1");
            controller.copy(canvasState, editor);

            // Paste into the same editor
            List<String> pasted = controller.paste(canvasState, editor);
            assertThat(pasted).hasSize(2);

            String pastedModuleName = pasted.stream()
                    .filter(n -> editor.getModuleByName(n) != null)
                    .findFirst()
                    .orElse(null);
            assertThat(pastedModuleName).isNotNull();

            String pastedConstName = pasted.stream()
                    .filter(n -> editor.getConstantByName(n) != null
                            && !n.equals("Rate"))
                    .findFirst()
                    .orElse(null);
            assertThat(pastedConstName).isNotNull();

            ModuleInstanceDef pastedModule = editor.getModuleByName(pastedModuleName);
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
            canvasState.addElement("Rate", ElementType.CONSTANT, 100, 100);
            canvasState.addElement("Mod1", ElementType.MODULE, 200, 200);

            // Copy only the module (not the constant it references)
            canvasState.select("Mod1");
            controller.copy(canvasState, editor);

            // Paste into a fresh editor
            ModelEditor targetEditor = new ModelEditor();
            CanvasState targetCanvas = new CanvasState();
            List<String> pasted = controller.paste(targetCanvas, targetEditor);

            assertThat(pasted).hasSize(1);
            ModuleInstanceDef pastedModule = targetEditor.getModuleByName(pasted.get(0));
            // "Rate" doesn't exist in target — should be replaced with 0
            assertThat(pastedModule.inputBindings().get("input_port")).isEqualTo("0");
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
    }
}
