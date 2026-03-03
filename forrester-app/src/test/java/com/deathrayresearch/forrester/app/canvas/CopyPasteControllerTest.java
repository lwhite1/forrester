package com.deathrayresearch.forrester.app.canvas;

import com.deathrayresearch.forrester.model.def.ElementType;
import com.deathrayresearch.forrester.model.def.FlowDef;
import com.deathrayresearch.forrester.model.def.ModelDefinition;
import com.deathrayresearch.forrester.model.def.ModelDefinitionBuilder;

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
        controller = new CopyPasteController();
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
