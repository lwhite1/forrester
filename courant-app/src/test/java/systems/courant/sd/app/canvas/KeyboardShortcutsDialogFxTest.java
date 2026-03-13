package systems.courant.sd.app.canvas;

import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.testfx.api.FxRobot;
import org.testfx.framework.junit5.ApplicationExtension;
import org.testfx.framework.junit5.Start;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("KeyboardShortcutsDialog (TestFX)")
@ExtendWith(ApplicationExtension.class)
class KeyboardShortcutsDialogFxTest {

    private KeyboardShortcutsDialog dialog;

    @Start
    void start(Stage stage) {
        dialog = new KeyboardShortcutsDialog();

        stage.setScene(new Scene(new StackPane(), 100, 100));
        stage.show();
    }

    @Test
    @DisplayName("dialog has correct window title")
    void dialogHasCorrectTitle(FxRobot robot) {
        assertThat(dialog.getTitle()).isEqualTo("Keyboard Shortcuts");
    }

    @Test
    @DisplayName("root is a ScrollPane wrapping a VBox")
    void rootIsScrollPaneWithVBox(FxRobot robot) {
        assertThat(dialog.getScene().getRoot()).isInstanceOf(ScrollPane.class);
        ScrollPane scroll = (ScrollPane) dialog.getScene().getRoot();
        assertThat(scroll.getContent()).isInstanceOf(VBox.class);
    }

    @Test
    @DisplayName("contains expected section headers")
    void containsExpectedSections(FxRobot robot) {
        ScrollPane scroll = (ScrollPane) dialog.getScene().getRoot();
        VBox content = (VBox) scroll.getContent();

        var sectionTitles = content.getChildren().stream()
                .filter(node -> node instanceof VBox)
                .map(node -> {
                    VBox section = (VBox) node;
                    Label header = (Label) section.getChildren().getFirst();
                    return header.getText();
                })
                .toList();

        assertThat(sectionTitles).containsExactly(
                "File", "Edit", "Tools", "Help", "View & Navigation",
                "Equation Editor", "Simulation", "Escape Priority Chain");
    }

    @Test
    @DisplayName("has 8 sections")
    void hasEightSections(FxRobot robot) {
        ScrollPane scroll = (ScrollPane) dialog.getScene().getRoot();
        VBox content = (VBox) scroll.getContent();
        assertThat(content.getChildren()).hasSize(8);
    }

    @Test
    @DisplayName("each section contains a GridPane with shortcuts")
    void eachSectionHasGrid(FxRobot robot) {
        ScrollPane scroll = (ScrollPane) dialog.getScene().getRoot();
        VBox content = (VBox) scroll.getContent();

        for (var child : content.getChildren()) {
            VBox section = (VBox) child;
            assertThat(section.getChildren()).hasSize(2);
            assertThat(section.getChildren().get(0)).isInstanceOf(Label.class);
            assertThat(section.getChildren().get(1)).isInstanceOf(GridPane.class);
        }
    }
}
