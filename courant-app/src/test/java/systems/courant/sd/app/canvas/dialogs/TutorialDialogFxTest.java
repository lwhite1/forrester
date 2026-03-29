package systems.courant.sd.app.canvas.dialogs;

import javafx.scene.Cursor;
import javafx.scene.Scene;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.layout.StackPane;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
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

    private ContentTutorialDialog quickstartDialog;
    private ContentTutorialDialog sirDialog;
    private ContentTutorialDialog delaysDialog;
    private ContentTutorialDialog cldDialog;

    @Start
    void start(Stage stage) {
        quickstartDialog = new ContentTutorialDialog(
                TutorialContentLoader.load("modeling/first-model.json"));
        sirDialog = new ContentTutorialDialog(
                TutorialContentLoader.load("modeling/feedback-loops.json"));
        delaysDialog = new ContentTutorialDialog(
                TutorialContentLoader.load("modeling/delays.json"));
        cldDialog = new ContentTutorialDialog(
                TutorialContentLoader.load("modeling/cld-basics.json"));

        // Show a dummy stage so TestFX has something to anchor to
        stage.setScene(new Scene(new StackPane(), 100, 100));
        stage.show();
    }

    @Test
    @DisplayName("First-model tutorial has 6 tabs")
    void quickstartDialogHasSixTabs(FxRobot robot) {
        TabPane tabs = (TabPane) quickstartDialog.getScene().getRoot();
        assertThat(tabs.getTabs()).hasSize(6);
    }

    @Test
    @DisplayName("First-model tutorial has correct window title")
    void quickstartDialogTitle(FxRobot robot) {
        assertThat(quickstartDialog.getTitle()).contains("Your First Model");
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
    void delaysDialogHasSevenTabs(FxRobot robot) {
        TabPane tabs = (TabPane) delaysDialog.getScene().getRoot();
        assertThat(tabs.getTabs()).hasSize(7);
    }

    @Test
    @DisplayName("Supply chain tutorial tab titles follow numbered sequence")
    void delaysDialogTabTitles(FxRobot robot) {
        TabPane tabs = (TabPane) delaysDialog.getScene().getRoot();
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
    @DisplayName("Delays tutorial has correct window title")
    void delaysDialogTitle(FxRobot robot) {
        assertThat(delaysDialog.getTitle()).contains("Delays");
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
    @DisplayName("Tabs are not closeable")
    void tabsNotCloseable(FxRobot robot) {
        TabPane qsTabs = (TabPane) quickstartDialog.getScene().getRoot();
        assertThat(qsTabs.getTabClosingPolicy())
                .isEqualTo(TabPane.TabClosingPolicy.UNAVAILABLE);

        TabPane sirTabs = (TabPane) sirDialog.getScene().getRoot();
        assertThat(sirTabs.getTabClosingPolicy())
                .isEqualTo(TabPane.TabClosingPolicy.UNAVAILABLE);

        TabPane scTabs = (TabPane) delaysDialog.getScene().getRoot();
        assertThat(scTabs.getTabClosingPolicy())
                .isEqualTo(TabPane.TabClosingPolicy.UNAVAILABLE);

        TabPane cldTabs = (TabPane) cldDialog.getScene().getRoot();
        assertThat(cldTabs.getTabClosingPolicy())
                .isEqualTo(TabPane.TabClosingPolicy.UNAVAILABLE);
    }

    @Test
    @DisplayName("All tutorial dialogs provide a tutorial ID for progress tracking")
    void allDialogsProvideTutorialId(FxRobot robot) {
        assertThat(quickstartDialog.resolvedTutorialId()).isEqualTo("first-model");
        assertThat(sirDialog.resolvedTutorialId()).isEqualTo("feedback-loops");
        assertThat(delaysDialog.resolvedTutorialId()).isEqualTo("delays");
        assertThat(cldDialog.resolvedTutorialId()).isEqualTo("cld-basics");
    }

    @Test
    @DisplayName("Tutorial text flows are focusable for copy support")
    void textFlowsAreFocusable(FxRobot robot) {
        TextFlow flow = findFirstTextFlow(quickstartDialog);
        assertThat(flow).isNotNull();
        assertThat(flow.isFocusTraversable()).isTrue();
    }

    @Test
    @DisplayName("Tutorial text flows have text cursor")
    void textFlowsHaveTextCursor(FxRobot robot) {
        TextFlow flow = findFirstTextFlow(quickstartDialog);
        assertThat(flow).isNotNull();
        assertThat(flow.getCursor()).isEqualTo(Cursor.TEXT);
    }

    @Test
    @DisplayName("Tutorial text flows have context menu handler for copy")
    void textFlowsHaveContextMenuHandler(FxRobot robot) {
        TextFlow flow = findFirstTextFlow(quickstartDialog);
        assertThat(flow).isNotNull();
        assertThat(flow.getOnContextMenuRequested()).isNotNull();
    }

    @Test
    @DisplayName("Tutorial text flows have key handler for Ctrl+C")
    void textFlowsHaveKeyHandler(FxRobot robot) {
        TextFlow flow = findFirstTextFlow(quickstartDialog);
        assertThat(flow).isNotNull();
        assertThat(flow.getOnKeyPressed()).isNotNull();
    }

    private static TextFlow findFirstTextFlow(ContentTutorialDialog dialog) {
        TabPane tabs = (TabPane) dialog.getScene().getRoot();
        Tab firstTab = tabs.getTabs().getFirst();
        ScrollPane scroll = (ScrollPane) firstTab.getContent();
        if (scroll.getContent() instanceof TextFlow tf) {
            return tf;
        }
        // Model tab wraps TextFlow in a VBox
        if (scroll.getContent() instanceof javafx.scene.layout.VBox vbox) {
            return vbox.getChildren().stream()
                    .filter(n -> n instanceof TextFlow)
                    .map(n -> (TextFlow) n)
                    .findFirst().orElse(null);
        }
        return null;
    }
}
