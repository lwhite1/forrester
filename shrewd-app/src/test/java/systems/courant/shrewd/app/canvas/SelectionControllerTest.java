package systems.courant.shrewd.app.canvas;

import systems.courant.shrewd.model.def.ElementType;
import systems.courant.shrewd.model.def.ModelDefinition;
import systems.courant.shrewd.model.def.ModelDefinitionBuilder;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

@DisplayName("SelectionController")
class SelectionControllerTest {

    private SelectionController controller;
    private CopyPasteController copyPaste;
    private Clipboard clipboard;
    private ModelEditor editor;
    private CanvasState canvasState;
    private AtomicInteger undoCount;

    @BeforeEach
    void setUp() {
        clipboard = new Clipboard();
        copyPaste = new CopyPasteController(clipboard);
        controller = new SelectionController(copyPaste);
        editor = new ModelEditor();
        canvasState = new CanvasState();
        undoCount = new AtomicInteger(0);
    }

    @Nested
    @DisplayName("createElementAt")
    class CreateElementAt {

        @Test
        void shouldCreateStock() {
            String name = controller.createElementAt(100, 200,
                    CanvasToolBar.Tool.PLACE_STOCK, editor, canvasState,
                    undoCount::incrementAndGet);

            assertThat(name).isNotNull();
            assertThat(canvasState.hasElement(name)).isTrue();
            assertThat(canvasState.getType(name)).hasValue(ElementType.STOCK);
            assertThat(canvasState.getX(name)).isCloseTo(100, within(0.001));
            assertThat(canvasState.getY(name)).isCloseTo(200, within(0.001));
        }

        @Test
        void shouldCreateAux() {
            String name = controller.createElementAt(150, 250,
                    CanvasToolBar.Tool.PLACE_AUX, editor, canvasState,
                    undoCount::incrementAndGet);

            assertThat(name).isNotNull();
            assertThat(canvasState.getType(name)).hasValue(ElementType.AUX);
        }

        @Test
        void shouldCreateModule() {
            String name = controller.createElementAt(150, 250,
                    CanvasToolBar.Tool.PLACE_MODULE, editor, canvasState,
                    undoCount::incrementAndGet);

            assertThat(name).isNotNull();
            assertThat(canvasState.getType(name)).hasValue(ElementType.MODULE);
        }

        @Test
        void shouldCreateLookup() {
            String name = controller.createElementAt(150, 250,
                    CanvasToolBar.Tool.PLACE_LOOKUP, editor, canvasState,
                    undoCount::incrementAndGet);

            assertThat(name).isNotNull();
            assertThat(canvasState.getType(name)).hasValue(ElementType.LOOKUP);
        }

        @Test
        void shouldCreateComment() {
            String name = controller.createElementAt(150, 250,
                    CanvasToolBar.Tool.PLACE_COMMENT, editor, canvasState,
                    undoCount::incrementAndGet);

            assertThat(name).isNotNull();
            assertThat(canvasState.getType(name)).hasValue(ElementType.COMMENT);
        }

        @Test
        void shouldReturnNull_whenToolIsSelect() {
            String name = controller.createElementAt(100, 200,
                    CanvasToolBar.Tool.SELECT, editor, canvasState,
                    undoCount::incrementAndGet);

            assertThat(name).isNull();
        }

        @Test
        void shouldReturnNull_whenEditorIsNull() {
            String name = controller.createElementAt(100, 200,
                    CanvasToolBar.Tool.PLACE_STOCK, null, canvasState,
                    undoCount::incrementAndGet);

            assertThat(name).isNull();
        }

        @Test
        void shouldSaveUndo() {
            controller.createElementAt(100, 200,
                    CanvasToolBar.Tool.PLACE_STOCK, editor, canvasState,
                    undoCount::incrementAndGet);

            assertThat(undoCount.get()).isEqualTo(1);
        }

        @Test
        void shouldSelectCreatedElement() {
            String name = controller.createElementAt(100, 200,
                    CanvasToolBar.Tool.PLACE_STOCK, editor, canvasState,
                    undoCount::incrementAndGet);

            assertThat(canvasState.isSelected(name)).isTrue();
            assertThat(canvasState.getSelection()).containsExactly(name);
        }

        @Test
        void shouldClearPreviousSelectionOnCreate() {
            canvasState.addElement("existing", ElementType.STOCK, 0, 0);
            canvasState.select("existing");

            String name = controller.createElementAt(100, 200,
                    CanvasToolBar.Tool.PLACE_STOCK, editor, canvasState,
                    undoCount::incrementAndGet);

            assertThat(canvasState.isSelected("existing")).isFalse();
            assertThat(canvasState.isSelected(name)).isTrue();
        }
    }

    @Nested
    @DisplayName("deleteSelected")
    class DeleteSelected {

        @BeforeEach
        void loadModel() {
            ModelDefinition def = new ModelDefinitionBuilder()
                    .name("Test")
                    .stock("Population", 100, "Person")
                    .aux("rate", "0.1", "units")
                    .build();
            editor.loadFrom(def);
            canvasState.addElement("Population", ElementType.STOCK, 100, 200);
            canvasState.addElement("rate", ElementType.AUX, 200, 200);
        }

        @Test
        void shouldDeleteSelectedElements() {
            canvasState.select("Population");

            controller.deleteSelected(editor, canvasState, undoCount::incrementAndGet);

            assertThat(canvasState.hasElement("Population")).isFalse();
            assertThat(editor.hasElement("Population")).isFalse();
        }

        @Test
        void shouldDeleteMultipleSelectedElements() {
            canvasState.select("Population");
            canvasState.addToSelection("rate");

            controller.deleteSelected(editor, canvasState, undoCount::incrementAndGet);

            assertThat(canvasState.hasElement("Population")).isFalse();
            assertThat(canvasState.hasElement("rate")).isFalse();
        }

        @Test
        void shouldDoNothing_whenNothingSelected() {
            controller.deleteSelected(editor, canvasState, undoCount::incrementAndGet);

            assertThat(canvasState.hasElement("Population")).isTrue();
            assertThat(undoCount.get()).isEqualTo(0);
        }

        @Test
        void shouldDoNothing_whenEditorIsNull() {
            canvasState.select("Population");

            controller.deleteSelected(null, canvasState, undoCount::incrementAndGet);

            assertThat(canvasState.hasElement("Population")).isTrue();
        }

        @Test
        void shouldSaveUndo() {
            canvasState.select("Population");

            controller.deleteSelected(editor, canvasState, undoCount::incrementAndGet);

            assertThat(undoCount.get()).isEqualTo(1);
        }

        @Test
        void shouldNotAffectUnselectedElements() {
            canvasState.select("Population");

            controller.deleteSelected(editor, canvasState, undoCount::incrementAndGet);

            assertThat(canvasState.hasElement("rate")).isTrue();
            assertThat(editor.hasElement("rate")).isTrue();
        }
    }

    @Nested
    @DisplayName("deleteConnection")
    class DeleteConnection {

        @Test
        void shouldReturnFalse_whenEditorIsNull() {
            ConnectionId conn = new ConnectionId("A", "B");

            boolean result = controller.deleteConnection(conn, false, null,
                    undoCount::incrementAndGet);

            assertThat(result).isFalse();
        }

        @Test
        void shouldReturnFalse_whenConnectionIsNull() {
            boolean result = controller.deleteConnection(null, false, editor,
                    undoCount::incrementAndGet);

            assertThat(result).isFalse();
        }

        @Test
        void shouldSaveUndo_whenDeleting() {
            ConnectionId conn = new ConnectionId("A", "B");

            controller.deleteConnection(conn, false, editor,
                    undoCount::incrementAndGet);

            assertThat(undoCount.get()).isEqualTo(1);
        }
    }

    @Nested
    @DisplayName("applyRename")
    class ApplyRename {

        @BeforeEach
        void loadModel() {
            ModelDefinition def = new ModelDefinitionBuilder()
                    .name("Test")
                    .stock("Population", 100, "Person")
                    .build();
            editor.loadFrom(def);
            canvasState.addElement("Population", ElementType.STOCK, 100, 200);
        }

        @Test
        void shouldRenameInBothEditorAndCanvas() {
            boolean result = controller.applyRename("Population", "People",
                    editor, canvasState, undoCount::incrementAndGet);

            assertThat(result).isTrue();
            assertThat(editor.hasElement("People")).isTrue();
            assertThat(editor.hasElement("Population")).isFalse();
            assertThat(canvasState.hasElement("People")).isTrue();
            assertThat(canvasState.hasElement("Population")).isFalse();
        }

        @Test
        void shouldReturnFalse_whenSameName() {
            boolean result = controller.applyRename("Population", "Population",
                    editor, canvasState, undoCount::incrementAndGet);

            assertThat(result).isFalse();
        }

        @Test
        void shouldReturnFalse_whenNewNameAlreadyExists() {
            editor.addStock();
            String existingName = "Stock 1"; // auto-generated

            boolean result = controller.applyRename("Population", existingName,
                    editor, canvasState, undoCount::incrementAndGet);

            assertThat(result).isFalse();
        }

        @Test
        void shouldSaveUndo_whenRenaming() {
            controller.applyRename("Population", "People",
                    editor, canvasState, undoCount::incrementAndGet);

            assertThat(undoCount.get()).isEqualTo(1);
        }
    }

    @Nested
    @DisplayName("canPaste")
    class CanPaste {

        @Test
        void shouldReturnFalse_whenClipboardEmpty() {
            assertThat(controller.canPaste()).isFalse();
        }
    }

    @Nested
    @DisplayName("copy")
    class Copy {

        @Test
        void shouldDoNothing_whenEditorIsNull() {
            // Should not throw
            controller.copy(null, canvasState);
        }
    }

    @Nested
    @DisplayName("cut")
    class Cut {

        @Test
        void shouldDoNothing_whenEditorIsNull() {
            controller.cut(null, canvasState, undoCount::incrementAndGet);

            assertThat(undoCount.get()).isEqualTo(0);
        }

        @Test
        void shouldDoNothing_whenNothingSelected() {
            controller.cut(editor, canvasState, undoCount::incrementAndGet);

            assertThat(undoCount.get()).isEqualTo(0);
        }
    }

    @Nested
    @DisplayName("paste")
    class Paste {

        @Test
        void shouldReturnEmptySet_whenEditorIsNull() {
            assertThat(controller.paste(null, canvasState, undoCount::incrementAndGet))
                    .isEmpty();
        }

        @Test
        void shouldReturnEmptySet_whenClipboardEmpty() {
            assertThat(controller.paste(editor, canvasState, undoCount::incrementAndGet))
                    .isEmpty();
        }
    }

    @Nested
    @DisplayName("selectAndCenter")
    class SelectAndCenter {

        @Test
        void shouldSelectElement() {
            canvasState.addElement("A", ElementType.STOCK, 100, 200);
            Viewport viewport = new Viewport();

            controller.selectAndCenter("A", canvasState, viewport, 800, 600);

            assertThat(canvasState.isSelected("A")).isTrue();
            assertThat(canvasState.getSelection()).containsExactly("A");
        }

        @Test
        void shouldClearPreviousSelection() {
            canvasState.addElement("A", ElementType.STOCK, 100, 200);
            canvasState.addElement("B", ElementType.STOCK, 300, 400);
            canvasState.select("B");
            Viewport viewport = new Viewport();

            controller.selectAndCenter("A", canvasState, viewport, 800, 600);

            assertThat(canvasState.isSelected("A")).isTrue();
            assertThat(canvasState.isSelected("B")).isFalse();
        }

        @Test
        void shouldPanViewportToCenterElement() {
            canvasState.addElement("A", ElementType.STOCK, 100, 200);
            Viewport viewport = new Viewport();

            controller.selectAndCenter("A", canvasState, viewport, 800, 600);

            // Expected: translateX = 400 - 100*1.0 = 300
            //           translateY = 300 - 200*1.0 = 100
            assertThat(viewport.getTranslateX()).isCloseTo(300, within(0.001));
            assertThat(viewport.getTranslateY()).isCloseTo(100, within(0.001));
        }
    }

    @Nested
    @DisplayName("classifyCldVariable")
    class ClassifyCldVariable {

        @Test
        void shouldReturnFalse_whenEditorIsNull() {
            boolean result = controller.classifyCldVariable("var", ElementType.STOCK,
                    null, canvasState, undoCount::incrementAndGet);

            assertThat(result).isFalse();
        }
    }
}
