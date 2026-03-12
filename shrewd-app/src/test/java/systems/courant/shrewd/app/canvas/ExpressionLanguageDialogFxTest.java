package systems.courant.shrewd.app.canvas;

import javafx.scene.Scene;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.testfx.api.FxRobot;
import org.testfx.framework.junit5.ApplicationExtension;
import org.testfx.framework.junit5.Start;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("ExpressionLanguageDialog (TestFX)")
@ExtendWith(ApplicationExtension.class)
class ExpressionLanguageDialogFxTest {

    private ExpressionLanguageDialog dialog;

    @Start
    void start(Stage stage) {
        dialog = new ExpressionLanguageDialog();

        stage.setScene(new Scene(new StackPane(), 100, 100));
        stage.show();
    }

    @Test
    @DisplayName("dialog has correct window title")
    void dialogHasCorrectTitle(FxRobot robot) {
        assertThat(dialog.getTitle()).isEqualTo("Expression Language Reference");
    }

    @Test
    @DisplayName("has 6 tabs")
    void hasSixTabs(FxRobot robot) {
        TabPane tabs = (TabPane) dialog.getScene().getRoot();
        assertThat(tabs.getTabs()).hasSize(6);
    }

    @Test
    @DisplayName("tab titles match expected names")
    void tabTitlesMatchExpected(FxRobot robot) {
        TabPane tabs = (TabPane) dialog.getScene().getRoot();
        assertThat(tabs.getTabs().stream().map(Tab::getText).toList())
                .containsExactly(
                        "Basics", "Operators", "Math Functions",
                        "SD Functions", "Patterns", "Grammar");
    }

    @Test
    @DisplayName("tabs are not closeable")
    void tabsNotCloseable(FxRobot robot) {
        TabPane tabs = (TabPane) dialog.getScene().getRoot();
        assertThat(tabs.getTabClosingPolicy())
                .isEqualTo(TabPane.TabClosingPolicy.UNAVAILABLE);
    }

    @Test
    @DisplayName("focusSdFunctions selects SD Functions tab")
    void focusSdFunctionsSelectsCorrectTab(FxRobot robot) {
        dialog.focusSdFunctions();
        TabPane tabs = (TabPane) dialog.getScene().getRoot();
        Tab selected = tabs.getSelectionModel().getSelectedItem();
        assertThat(selected.getText()).isEqualTo("SD Functions");
    }
}
