package systems.courant.shrewd.app.canvas;

import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.testfx.api.FxRobot;
import org.testfx.framework.junit5.ApplicationExtension;
import org.testfx.framework.junit5.Start;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("UndoHistoryPopup (TestFX)")
@ExtendWith(ApplicationExtension.class)
class UndoHistoryPopupFxTest {

    private UndoHistoryPopup popup;
    private final List<String> labels = List.of("Move stock", "Add flow", "Rename variable");

    @Start
    void start(Stage stage) {
        popup = new UndoHistoryPopup(labels, index -> {});

        stage.setScene(new Scene(new StackPane(), 100, 100));
        stage.show();
    }

    @Test
    @DisplayName("popup constructs without error")
    void popupConstructs(FxRobot robot) {
        assertThat(popup).isNotNull();
    }

    @Test
    @DisplayName("popup content is a VBox with header, list, and hint")
    void popupContentIsVBox(FxRobot robot) {
        assertThat(popup.getContent()).hasSize(1);
        assertThat(popup.getContent().getFirst()).isInstanceOf(VBox.class);

        VBox container = (VBox) popup.getContent().getFirst();
        assertThat(container.getChildren()).hasSize(3);
        assertThat(container.getChildren().get(0)).isInstanceOf(Label.class);
        assertThat(container.getChildren().get(1)).isInstanceOf(ListView.class);
        assertThat(container.getChildren().get(2)).isInstanceOf(Label.class);
    }

    @Test
    @DisplayName("header label shows 'Undo History'")
    void headerLabelText(FxRobot robot) {
        VBox container = (VBox) popup.getContent().getFirst();
        Label header = (Label) container.getChildren().get(0);
        assertThat(header.getText()).isEqualTo("Undo History");
    }

    @Test
    @DisplayName("ListView is populated with all undo labels")
    void listViewIsPopulated(FxRobot robot) {
        VBox container = (VBox) popup.getContent().getFirst();
        @SuppressWarnings("unchecked")
        ListView<String> listView = (ListView<String>) container.getChildren().get(1);
        assertThat(listView.getItems()).hasSize(3);
        assertThat(listView.getItems()).containsExactly("Move stock", "Add flow", "Rename variable");
    }

    @Test
    @DisplayName("popup auto-hides")
    void popupAutoHides(FxRobot robot) {
        assertThat(popup.isAutoHide()).isTrue();
    }

    @Test
    @DisplayName("hint label contains usage instruction")
    void hintLabelText(FxRobot robot) {
        VBox container = (VBox) popup.getContent().getFirst();
        Label hint = (Label) container.getChildren().get(2);
        assertThat(hint.getText()).contains("Click to jump");
    }

    @Test
    @DisplayName("construction with empty list produces empty ListView")
    void emptyListProducesEmptyListView(FxRobot robot) {
        UndoHistoryPopup emptyPopup = new UndoHistoryPopup(List.of(), index -> {});
        VBox container = (VBox) emptyPopup.getContent().getFirst();
        @SuppressWarnings("unchecked")
        ListView<String> listView = (ListView<String>) container.getChildren().get(1);
        assertThat(listView.getItems()).isEmpty();
    }
}
