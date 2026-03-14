package systems.courant.sd.app.canvas.dialogs;

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

@DisplayName("SdConceptsDialog (TestFX)")
@ExtendWith(ApplicationExtension.class)
class SdConceptsDialogFxTest {

    private SdConceptsDialog dialog;

    @Start
    void start(Stage stage) {
        dialog = new SdConceptsDialog();

        stage.setScene(new Scene(new StackPane(), 100, 100));
        stage.show();
    }

    @Test
    @DisplayName("dialog has correct window title")
    void dialogHasCorrectTitle(FxRobot robot) {
        assertThat(dialog.getTitle()).isEqualTo("SD Concepts");
    }

    @Test
    @DisplayName("has 7 tabs")
    void hasSevenTabs(FxRobot robot) {
        TabPane tabs = (TabPane) dialog.getScene().getRoot();
        assertThat(tabs.getTabs()).hasSize(7);
    }

    @Test
    @DisplayName("tab titles match expected names")
    void tabTitlesMatchExpected(FxRobot robot) {
        TabPane tabs = (TabPane) dialog.getScene().getRoot();
        assertThat(tabs.getTabs().stream().map(Tab::getText).toList())
                .containsExactly(
                        "Overview", "Stocks", "Flows", "Variables",
                        "Feedback Loops", "Causal Loops", "Simulation");
    }

    @Test
    @DisplayName("tabs are not closeable")
    void tabsNotCloseable(FxRobot robot) {
        TabPane tabs = (TabPane) dialog.getScene().getRoot();
        assertThat(tabs.getTabClosingPolicy())
                .isEqualTo(TabPane.TabClosingPolicy.UNAVAILABLE);
    }
}
