package systems.courant.sd.app.canvas.dialogs;

import javafx.scene.Scene;
import javafx.scene.control.TreeCell;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

import java.util.LinkedHashMap;
import java.util.Map;
import systems.courant.sd.app.canvas.HelpContent;
import systems.courant.sd.app.canvas.HelpTopic;

/**
 * A two-pane help dialog with a {@link TreeView} sidebar for navigation
 * and a content area displaying help for the selected {@link HelpTopic}.
 *
 * <p>Intended to be used as a singleton window: call {@link #showTopic(HelpTopic)}
 * to navigate to a topic, bringing the window to front if already showing.
 */
public class ContextHelpDialog extends Stage {

    private final TreeView<HelpTopic> treeView;
    private final StackPane contentPane;
    private final Map<HelpTopic, TreeItem<HelpTopic>> itemsByTopic = new LinkedHashMap<>();

    public ContextHelpDialog() {
        setTitle("Help");

        treeView = buildTreeView();
        treeView.setPrefWidth(180);
        treeView.setMinWidth(140);
        treeView.setShowRoot(false);

        contentPane = new StackPane();

        BorderPane root = new BorderPane();
        root.setLeft(treeView);
        root.setCenter(contentPane);

        Scene scene = new Scene(root, 740, 580);
        setScene(scene);

        // Show content when tree selection changes
        treeView.getSelectionModel().selectedItemProperty().addListener(
                (obs, oldItem, newItem) -> {
                    if (newItem != null && newItem.getValue() != null && newItem.isLeaf()) {
                        showContent(newItem.getValue());
                    }
                });

        // Default to overview
        showTopic(HelpTopic.OVERVIEW);
    }

    /**
     * Navigates to the given topic, selecting it in the sidebar and displaying its content.
     * Brings the window to front if already showing.
     */
    public void showTopic(HelpTopic topic) {
        TreeItem<HelpTopic> item = itemsByTopic.get(topic);
        if (item != null) {
            if (item.getParent() != null) {
                item.getParent().setExpanded(true);
            }
            treeView.getSelectionModel().select(item);
        }
        showContent(topic);
        toFront();
        requestFocus();
    }

    private void showContent(HelpTopic topic) {
        contentPane.getChildren().setAll(HelpContent.forTopic(topic));
    }

    private TreeView<HelpTopic> buildTreeView() {
        TreeItem<HelpTopic> root = new TreeItem<>();

        // Group topics by category, preserving enum order
        Map<String, TreeItem<HelpTopic>> categories = new LinkedHashMap<>();
        for (HelpTopic topic : HelpTopic.values()) {
            TreeItem<HelpTopic> categoryItem = categories.computeIfAbsent(
                    topic.category(), cat -> {
                        TreeItem<HelpTopic> catNode = new TreeItem<>(null);
                        catNode.setExpanded(true);
                        return catNode;
                    });
            TreeItem<HelpTopic> topicItem = new TreeItem<>(topic);
            categoryItem.getChildren().add(topicItem);
            itemsByTopic.put(topic, topicItem);
        }

        // Add categories to root
        for (Map.Entry<String, TreeItem<HelpTopic>> entry : categories.entrySet()) {
            root.getChildren().add(entry.getValue());
        }

        TreeView<HelpTopic> tree = new TreeView<>(root);
        tree.setCellFactory(tv -> new TreeCell<>() {
            @Override
            protected void updateItem(HelpTopic item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || getTreeItem() == null) {
                    setText(null);
                    setStyle("");
                } else if (item == null) {
                    // Category node — find category name from first child
                    TreeItem<HelpTopic> treeItem = getTreeItem();
                    if (!treeItem.getChildren().isEmpty()) {
                        HelpTopic firstChild = treeItem.getChildren().getFirst().getValue();
                        setText(firstChild != null ? firstChild.category() : "");
                        setStyle("-fx-font-weight: bold;");
                    } else {
                        setText("");
                    }
                } else {
                    setText(item.displayName());
                    setStyle("");
                }
            }
        });

        return tree;
    }
}
