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

@DisplayName("Tutorial Dialogs (TestFX)")
@ExtendWith(ApplicationExtension.class)
class TutorialDialogFxTest {

    private QuickstartDialog quickstartDialog;
    private SirTutorialDialog sirDialog;
    private SupplyChainTutorialDialog supplyChainDialog;
    private CldTutorialDialog cldDialog;

    @Start
    void start(Stage stage) {
        quickstartDialog = new QuickstartDialog();
        sirDialog = new SirTutorialDialog();
        supplyChainDialog = new SupplyChainTutorialDialog();
        cldDialog = new CldTutorialDialog();

        // Show a dummy stage so TestFX has something to anchor to
        stage.setScene(new Scene(new StackPane(), 100, 100));
        stage.show();
    }

    @Test
    @DisplayName("SIR tutorial has 7 tabs")
    void sirDialogHasSevenTabs(FxRobot robot) {
        TabPane tabs = (TabPane) sirDialog.getScene().getRoot();
        assertThat(tabs.getTabs()).hasSize(7);
    }

    @Test
    @DisplayName("SIR tutorial tab titles follow numbered sequence")
    void sirDialogTabTitles(FxRobot robot) {
        TabPane tabs = (TabPane) sirDialog.getScene().getRoot();
        assertThat(tabs.getTabs().stream().map(Tab::getText).toList())
                .containsExactly(
                        "1. The Idea",
                        "2. Stocks",
                        "3. Flows",
                        "4. Parameters",
                        "5. Simulate",
                        "6. Experiment",
                        "7. Key Takeaways");
    }

    @Test
    @DisplayName("SIR tutorial has correct window title")
    void sirDialogTitle(FxRobot robot) {
        assertThat(sirDialog.getTitle()).contains("SIR Epidemic");
    }

    @Test
    @DisplayName("Supply chain tutorial has 7 tabs")
    void supplyChainDialogHasSevenTabs(FxRobot robot) {
        TabPane tabs = (TabPane) supplyChainDialog.getScene().getRoot();
        assertThat(tabs.getTabs()).hasSize(7);
    }

    @Test
    @DisplayName("Supply chain tutorial tab titles follow numbered sequence")
    void supplyChainDialogTabTitles(FxRobot robot) {
        TabPane tabs = (TabPane) supplyChainDialog.getScene().getRoot();
        assertThat(tabs.getTabs().stream().map(Tab::getText).toList())
                .containsExactly(
                        "1. The Idea",
                        "2. Stocks",
                        "3. Flows",
                        "4. Parameters",
                        "5. Simulate",
                        "6. Experiment",
                        "7. Key Takeaways");
    }

    @Test
    @DisplayName("Supply chain tutorial has correct window title")
    void supplyChainDialogTitle(FxRobot robot) {
        assertThat(supplyChainDialog.getTitle()).contains("Supply Chain");
    }

    @Test
    @DisplayName("CLD tutorial has 7 tabs")
    void cldDialogHasSevenTabs(FxRobot robot) {
        TabPane tabs = (TabPane) cldDialog.getScene().getRoot();
        assertThat(tabs.getTabs()).hasSize(7);
    }

    @Test
    @DisplayName("CLD tutorial tab titles follow numbered sequence")
    void cldDialogTabTitles(FxRobot robot) {
        TabPane tabs = (TabPane) cldDialog.getScene().getRoot();
        assertThat(tabs.getTabs().stream().map(Tab::getText).toList())
                .containsExactly(
                        "1. The Idea",
                        "2. Variables",
                        "3. Causal Links",
                        "4. Polarity",
                        "5. Feedback Loops",
                        "6. Explore",
                        "7. Key Takeaways");
    }

    @Test
    @DisplayName("CLD tutorial has correct window title")
    void cldDialogTitle(FxRobot robot) {
        assertThat(cldDialog.getTitle()).contains("Causal Loop Diagrams");
    }

    @Test
    @DisplayName("Quickstart tutorial has 6 tabs")
    void quickstartDialogHasSixTabs(FxRobot robot) {
        TabPane tabs = (TabPane) quickstartDialog.getScene().getRoot();
        assertThat(tabs.getTabs()).hasSize(6);
    }

    @Test
    @DisplayName("Quickstart tutorial has correct window title")
    void quickstartDialogTitle(FxRobot robot) {
        assertThat(quickstartDialog.getTitle()).contains("Getting Started");
    }

    @Test
    @DisplayName("Tabs are not closeable")
    void tabsNotCloseable(FxRobot robot) {
        TabPane qsTabs = (TabPane) quickstartDialog.getScene().getRoot();
        assertThat(qsTabs.getTabClosingPolicy())
                .isEqualTo(TabPane.TabClosingPolicy.UNAVAILABLE);

        TabPane sirTabs = (TabPane) sirDialog.getScene().getRoot();
        assertThat(sirTabs.getTabClosingPolicy())
                .isEqualTo(TabPane.TabClosingPolicy.UNAVAILABLE);

        TabPane scTabs = (TabPane) supplyChainDialog.getScene().getRoot();
        assertThat(scTabs.getTabClosingPolicy())
                .isEqualTo(TabPane.TabClosingPolicy.UNAVAILABLE);

        TabPane cldTabs = (TabPane) cldDialog.getScene().getRoot();
        assertThat(cldTabs.getTabClosingPolicy())
                .isEqualTo(TabPane.TabClosingPolicy.UNAVAILABLE);
    }

    @Test
    @DisplayName("All tutorial dialogs provide a tutorial ID for progress tracking")
    void allDialogsProvideTutorialId(FxRobot robot) {
        assertThat(quickstartDialog.getTutorialId()).isEqualTo("first-model");
        assertThat(sirDialog.getTutorialId()).isEqualTo("sir-epidemic");
        assertThat(supplyChainDialog.getTutorialId()).isEqualTo("supply-chain");
        assertThat(cldDialog.getTutorialId()).isEqualTo("cld-basics");
    }
}
