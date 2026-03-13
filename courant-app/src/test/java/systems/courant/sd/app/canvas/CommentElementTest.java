package systems.courant.sd.app.canvas;

import systems.courant.sd.model.def.CommentDef;
import systems.courant.sd.model.def.ElementPlacement;
import systems.courant.sd.model.def.ElementType;
import systems.courant.sd.model.def.ModelDefinition;
import systems.courant.sd.model.def.ModelDefinitionBuilder;
import systems.courant.sd.model.def.ViewDef;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Comment Element")
class CommentElementTest {

    private ModelEditor editor;
    private CanvasState canvasState;
    private SelectionController selectionController;

    @BeforeEach
    void setUp() {
        editor = new ModelEditor();
        canvasState = new CanvasState();
        Clipboard clipboard = new Clipboard();
        CopyPasteController copyPaste = new CopyPasteController(clipboard);
        selectionController = new SelectionController(copyPaste);
    }

    @Nested
    @DisplayName("ModelEditor")
    class EditorTests {

        @Test
        @DisplayName("addComment creates a comment with auto-generated name")
        void shouldAddComment() {
            String name = editor.addComment();
            assertThat(name).isEqualTo("Comment 1");
            assertThat(editor.getComments()).hasSize(1);
            assertThat(editor.getCommentByName(name)).isNotNull();
            assertThat(editor.getCommentByName(name).text()).isEmpty();
        }

        @Test
        @DisplayName("addComment auto-increments IDs")
        void shouldAutoIncrementIds() {
            String name1 = editor.addComment();
            String name2 = editor.addComment();
            assertThat(name1).isEqualTo("Comment 1");
            assertThat(name2).isEqualTo("Comment 2");
        }

        @Test
        @DisplayName("setCommentText updates the text")
        void shouldSetCommentText() {
            String name = editor.addComment();
            boolean updated = editor.setCommentText(name, "This is a note");
            assertThat(updated).isTrue();
            assertThat(editor.getCommentByName(name).text()).isEqualTo("This is a note");
        }

        @Test
        @DisplayName("removeElement removes a comment")
        void shouldRemoveComment() {
            String name = editor.addComment();
            assertThat(editor.getComments()).hasSize(1);
            editor.removeElement(name);
            assertThat(editor.getComments()).isEmpty();
            assertThat(editor.getCommentByName(name)).isNull();
        }

        @Test
        @DisplayName("renameElement renames a comment")
        void shouldRenameComment() {
            String name = editor.addComment();
            editor.setCommentText(name, "My note");
            boolean renamed = editor.renameElement(name, "My Annotation");
            assertThat(renamed).isTrue();
            assertThat(editor.getCommentByName("My Annotation")).isNotNull();
            assertThat(editor.getCommentByName("My Annotation").text()).isEqualTo("My note");
            assertThat(editor.getCommentByName(name)).isNull();
        }

        @Test
        @DisplayName("hasElement returns true for comments")
        void shouldRecognizeCommentAsElement() {
            String name = editor.addComment();
            assertThat(editor.hasElement(name)).isTrue();
        }

        @Test
        @DisplayName("loadFrom loads comments from definition")
        void shouldLoadComments() {
            ModelDefinition def = new ModelDefinitionBuilder()
                    .name("Test")
                    .comment("Comment 1", "Note text")
                    .build();

            editor.loadFrom(def);

            assertThat(editor.getComments()).hasSize(1);
            assertThat(editor.getCommentByName("Comment 1")).isNotNull();
            assertThat(editor.getCommentByName("Comment 1").text()).isEqualTo("Note text");
        }

        @Test
        @DisplayName("toModelDefinition includes comments")
        void shouldIncludeCommentsInSnapshot() {
            editor.setModelName("Test");
            editor.addComment();
            editor.setCommentText("Comment 1", "A note");

            ModelDefinition def = editor.toModelDefinition();
            assertThat(def.comments()).hasSize(1);
            assertThat(def.comments().get(0).name()).isEqualTo("Comment 1");
            assertThat(def.comments().get(0).text()).isEqualTo("A note");
        }

        @Test
        @DisplayName("addCommentFrom copies text from template")
        void shouldCopyFromTemplate() {
            CommentDef template = new CommentDef("Original", "Template text");
            String name = editor.addCommentFrom(template);
            assertThat(editor.getCommentByName(name)).isNotNull();
            assertThat(editor.getCommentByName(name).text()).isEqualTo("Template text");
        }
    }

    @Nested
    @DisplayName("Element creation")
    class ElementCreation {

        @Test
        @DisplayName("createElementAt with PLACE_COMMENT creates a comment on canvas")
        void shouldCreateCommentOnCanvas() {
            String name = selectionController.createElementAt(
                    100, 200, CanvasToolBar.Tool.PLACE_COMMENT,
                    editor, canvasState, () -> {});
            assertThat(name).isNotNull();
            assertThat(name).startsWith("Comment");
            assertThat(canvasState.hasElement(name)).isTrue();
            assertThat(canvasState.getType(name)).contains(ElementType.COMMENT);
            assertThat(canvasState.getX(name)).isEqualTo(100);
            assertThat(canvasState.getY(name)).isEqualTo(200);
        }
    }

    @Nested
    @DisplayName("Canvas state")
    class CanvasStateTests {

        @Test
        @DisplayName("loadFrom handles COMMENT element placements")
        void shouldLoadCommentPlacements() {
            ViewDef view = new ViewDef("Main", List.of(
                    new ElementPlacement("Comment 1", ElementType.COMMENT, 50, 75, 200, 100)
            ), List.of(), List.of());

            canvasState.loadFrom(view);
            assertThat(canvasState.hasElement("Comment 1")).isTrue();
            assertThat(canvasState.getType("Comment 1")).contains(ElementType.COMMENT);
            assertThat(canvasState.getX("Comment 1")).isEqualTo(50);
            assertThat(canvasState.getY("Comment 1")).isEqualTo(75);
            assertThat(canvasState.hasCustomSize("Comment 1")).isTrue();
            assertThat(canvasState.getWidth("Comment 1")).isEqualTo(200);
            assertThat(canvasState.getHeight("Comment 1")).isEqualTo(100);
        }
    }

    @Nested
    @DisplayName("Clipboard")
    class ClipboardTests {

        @Test
        @DisplayName("Copy and paste a comment element")
        void shouldCopyAndPasteComment() {
            String name = editor.addComment();
            editor.setCommentText(name, "Sticky note");
            canvasState.addElement(name, ElementType.COMMENT, 100, 100);
            canvasState.select(name);

            Clipboard clipboard = new Clipboard();
            clipboard.capture(canvasState, editor, canvasState.getSelection());
            assertThat(clipboard.isEmpty()).isFalse();

            CopyPasteController copyPaste = new CopyPasteController(clipboard);
            CopyPasteController.PasteResult result = copyPaste.paste(canvasState, editor);
            assertThat(result.pastedNames()).hasSize(1);

            String pastedName = result.pastedNames().get(0);
            assertThat(editor.getCommentByName(pastedName)).isNotNull();
            assertThat(editor.getCommentByName(pastedName).text()).isEqualTo("Sticky note");
        }
    }
}
