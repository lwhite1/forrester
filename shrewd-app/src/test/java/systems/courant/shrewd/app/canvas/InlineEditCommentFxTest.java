package systems.courant.shrewd.app.canvas;

import systems.courant.shrewd.model.def.ElementType;

import javafx.scene.Scene;
import javafx.scene.control.TextArea;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.Pane;
import javafx.stage.Stage;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.testfx.api.FxRobot;
import org.testfx.framework.junit5.ApplicationExtension;
import org.testfx.framework.junit5.Start;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Inline editing of comment elements (TestFX)")
@ExtendWith(ApplicationExtension.class)
class InlineEditCommentFxTest {

    private InlineEditController controller;
    private CanvasState canvasState;
    private ModelEditor editor;
    private Viewport viewport;
    private Pane overlayPane;

    private String lastRenamedFrom;
    private String lastRenamedTo;
    private String lastCommentName;
    private String lastCommentText;
    private boolean postEditCalled;

    private final InlineEditController.Callbacks callbacks = new InlineEditController.Callbacks() {
        @Override
        public void applyRename(String oldName, String newName) {
            lastRenamedFrom = oldName;
            lastRenamedTo = newName;
        }

        @Override
        public void saveAndSetFlowEquation(String name, String equation) { }

        @Override
        public void saveAndSetAuxEquation(String name, String equation) { }

        @Override
        public void saveAndSetCommentText(String name, String text) {
            lastCommentName = name;
            lastCommentText = text;
        }

        @Override
        public void postEdit() {
            postEditCalled = true;
        }
    };

    @Start
    void start(Stage stage) {
        controller = new InlineEditController();
        canvasState = new CanvasState();
        editor = new ModelEditor();
        viewport = new Viewport();

        overlayPane = new Pane();
        overlayPane.setPrefSize(800, 600);
        controller.setOverlayPane(overlayPane);

        stage.setScene(new Scene(overlayPane, 800, 600));
        stage.show();
    }

    private void startEditOnFxThread(String name, FxRobot robot) {
        robot.interact(() -> controller.startEdit(name, canvasState, editor, viewport, callbacks));
    }

    @Test
    @DisplayName("Comment inline edit opens a TextArea, not a TextField")
    void shouldOpenTextAreaForComment(FxRobot robot) {
        String name = editor.addComment();
        canvasState.addElement(name, ElementType.COMMENT, 400, 300);

        startEditOnFxThread(name, robot);

        assertThat(controller.isActive()).isTrue();
        assertThat(overlayPane.getChildren()).hasSize(1);
        assertThat(overlayPane.getChildren().get(0)).isInstanceOf(TextArea.class);
    }

    @Test
    @DisplayName("Comment inline edit shows current text")
    void shouldShowCurrentText(FxRobot robot) {
        String name = editor.addComment();
        editor.setCommentText(name, "Existing note");
        canvasState.addElement(name, ElementType.COMMENT, 400, 300);

        startEditOnFxThread(name, robot);

        TextArea area = (TextArea) overlayPane.getChildren().get(0);
        assertThat(area.getText()).isEqualTo("Existing note");
    }

    @Test
    @DisplayName("Focus loss commits comment text via saveAndSetCommentText callback")
    void shouldCommitOnFocusLoss(FxRobot robot) {
        String name = editor.addComment();
        canvasState.addElement(name, ElementType.COMMENT, 400, 300);

        startEditOnFxThread(name, robot);

        TextArea area = (TextArea) overlayPane.getChildren().get(0);
        robot.clickOn(area).write("Hello world");

        // Click elsewhere to lose focus — use coordinates outside the TextArea
        robot.interact(() -> overlayPane.requestFocus());

        assertThat(lastCommentName).isEqualTo(name);
        assertThat(lastCommentText).isEqualTo("Hello world");
        assertThat(lastRenamedFrom).isNull();
        assertThat(lastRenamedTo).isNull();
        assertThat(postEditCalled).isTrue();
    }

    @Test
    @DisplayName("Escape commits comment text")
    void shouldCommitOnEscape(FxRobot robot) {
        String name = editor.addComment();
        canvasState.addElement(name, ElementType.COMMENT, 400, 300);

        startEditOnFxThread(name, robot);

        TextArea area = (TextArea) overlayPane.getChildren().get(0);
        robot.clickOn(area).write("A note");
        robot.push(KeyCode.ESCAPE);

        assertThat(lastCommentName).isEqualTo(name);
        assertThat(lastCommentText).isEqualTo("A note");
        assertThat(controller.isActive()).isFalse();
    }

    @Test
    @DisplayName("Comment inline edit does not trigger rename")
    void shouldNotRename(FxRobot robot) {
        String name = editor.addComment();
        canvasState.addElement(name, ElementType.COMMENT, 400, 300);

        startEditOnFxThread(name, robot);

        TextArea area = (TextArea) overlayPane.getChildren().get(0);
        robot.clickOn(area).write("Some text");
        robot.interact(() -> overlayPane.requestFocus());

        assertThat(lastRenamedFrom).isNull();
        assertThat(lastRenamedTo).isNull();
    }
}
