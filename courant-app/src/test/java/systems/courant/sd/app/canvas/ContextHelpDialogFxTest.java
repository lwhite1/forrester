package systems.courant.sd.app.canvas;

import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.control.TreeCell;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.testfx.api.FxRobot;
import org.testfx.framework.junit5.ApplicationExtension;
import org.testfx.framework.junit5.Start;
import org.testfx.util.WaitForAsyncUtils;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("ContextHelpDialog (TestFX)")
@ExtendWith(ApplicationExtension.class)
class ContextHelpDialogFxTest {

    private ContextHelpDialog dialog;

    @Start
    void start(Stage stage) {
        dialog = new ContextHelpDialog();

        stage.setScene(new Scene(new StackPane(), 100, 100));
        stage.show();
    }

    @Test
    @DisplayName("dialog has correct window title")
    void dialogHasCorrectTitle(FxRobot robot) {
        assertThat(dialog.getTitle()).isEqualTo("Help");
    }

    @Test
    @DisplayName("root is a BorderPane with TreeView on the left")
    void rootIsBorderPaneWithTreeView(FxRobot robot) {
        assertThat(dialog.getScene().getRoot()).isInstanceOf(BorderPane.class);
        BorderPane root = (BorderPane) dialog.getScene().getRoot();
        assertThat(root.getLeft()).isInstanceOf(TreeView.class);
    }

    @Test
    @DisplayName("TreeView has items for every HelpTopic")
    void treeViewHasAllTopics(FxRobot robot) {
        BorderPane root = (BorderPane) dialog.getScene().getRoot();
        @SuppressWarnings("unchecked")
        TreeView<HelpTopic> tree = (TreeView<HelpTopic>) root.getLeft();

        // Count all leaf items (topic items, not category nodes)
        int leafCount = countLeaves(tree.getRoot());
        assertThat(leafCount).isEqualTo(HelpTopic.values().length);
    }

    @Test
    @DisplayName("content pane is populated after construction")
    void contentPaneIsPopulated(FxRobot robot) {
        BorderPane root = (BorderPane) dialog.getScene().getRoot();
        StackPane contentPane = (StackPane) root.getCenter();
        assertThat(contentPane.getChildren()).isNotEmpty();
    }

    @Test
    @DisplayName("showTopic navigates to the given topic")
    void showTopicNavigatesToTopic(FxRobot robot) {
        Platform.runLater(() -> dialog.showTopic(HelpTopic.FLOW));
        WaitForAsyncUtils.waitForFxEvents();

        BorderPane root = (BorderPane) dialog.getScene().getRoot();
        @SuppressWarnings("unchecked")
        TreeView<HelpTopic> tree = (TreeView<HelpTopic>) root.getLeft();
        TreeItem<HelpTopic> selected = tree.getSelectionModel().getSelectedItem();
        assertThat(selected).isNotNull();
        assertThat(selected.getValue()).isEqualTo(HelpTopic.FLOW);
    }

    @Test
    @DisplayName("showTopic updates content pane")
    void showTopicUpdatesContent(FxRobot robot) {
        BorderPane root = (BorderPane) dialog.getScene().getRoot();
        StackPane contentPane = (StackPane) root.getCenter();

        Platform.runLater(() -> dialog.showTopic(HelpTopic.MONTE_CARLO));
        WaitForAsyncUtils.waitForFxEvents();

        // Content pane should have exactly one child after navigation
        assertThat(contentPane.getChildren()).hasSize(1);
    }

    @Test
    @DisplayName("tree root is hidden")
    void treeRootIsHidden(FxRobot robot) {
        BorderPane root = (BorderPane) dialog.getScene().getRoot();
        @SuppressWarnings("unchecked")
        TreeView<HelpTopic> tree = (TreeView<HelpTopic>) root.getLeft();
        assertThat(tree.isShowRoot()).isFalse();
    }

    @Test
    @DisplayName("empty tree cell clears bold style from category header (#469)")
    void emptyCellClearsStyle(FxRobot robot) {
        Platform.runLater(() -> dialog.show());
        WaitForAsyncUtils.waitForFxEvents();

        // Find a tree cell that is a category header (bold), then check that
        // empty cells do NOT retain bold style
        var cells = robot.lookup(".tree-cell").queryAllAs(TreeCell.class);
        for (TreeCell<?> cell : cells) {
            if (cell.getText() == null && cell.isEmpty()) {
                // Empty cells must not have bold style
                String style = cell.getStyle();
                assertThat(style == null || !style.contains("-fx-font-weight: bold"))
                        .as("Empty cell should not retain bold style")
                        .isTrue();
            }
        }
    }

    private int countLeaves(TreeItem<HelpTopic> item) {
        if (item.isLeaf()) {
            return 1;
        }
        int count = 0;
        for (TreeItem<HelpTopic> child : item.getChildren()) {
            count += countLeaves(child);
        }
        return count;
    }
}
