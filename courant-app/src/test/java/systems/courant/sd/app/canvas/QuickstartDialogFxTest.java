package systems.courant.sd.app.canvas;

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

@DisplayName("QuickstartDialog (TestFX)")
@ExtendWith(ApplicationExtension.class)
class QuickstartDialogFxTest {

    private QuickstartDialog dialog;

    @Start
    void start(Stage stage) {
        dialog = new QuickstartDialog();

        stage.setScene(new Scene(new StackPane(), 100, 100));
        stage.show();
    }

    @Test
    @DisplayName("dialog has correct window title")
    void dialogHasCorrectTitle(FxRobot robot) {
        assertThat(dialog.getTitle()).isEqualTo("Getting Started — Build Your First Model");
    }

    @Test
    @DisplayName("has 6 tabs")
    void hasSixTabs(FxRobot robot) {
        TabPane tabs = (TabPane) dialog.getScene().getRoot();
        assertThat(tabs.getTabs()).hasSize(6);
    }

    @Test
    @DisplayName("tab titles follow numbered sequence")
    void tabTitlesFollowSequence(FxRobot robot) {
        TabPane tabs = (TabPane) dialog.getScene().getRoot();
        assertThat(tabs.getTabs().stream().map(Tab::getText).toList())
                .containsExactly(
                        "1. The Idea",
                        "2. Place Elements",
                        "3. Connect & Equate",
                        "4. Simulate",
                        "5. Experiment",
                        "6. Next Steps");
    }

    @Test
    @DisplayName("tabs are not closeable")
    void tabsNotCloseable(FxRobot robot) {
        TabPane tabs = (TabPane) dialog.getScene().getRoot();
        assertThat(tabs.getTabClosingPolicy())
                .isEqualTo(TabPane.TabClosingPolicy.UNAVAILABLE);
    }
}
