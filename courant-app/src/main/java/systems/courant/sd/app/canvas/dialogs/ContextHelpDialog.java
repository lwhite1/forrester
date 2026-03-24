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
import systems.courant.sd.app.canvas.GlossaryPane;
import systems.courant.sd.app.canvas.HelpContent;
import systems.courant.sd.app.canvas.HelpTopic;

/**
 * A two-pane help dialog with a {@link TreeView} sidebar for navigation
 * and a content area displaying help for the selected {@link HelpTopic}.
 * Includes an SD Terminology glossary section.
 *
 * <p>Intended to be used as a singleton window: call {@link #showTopic(HelpTopic)}
 * to navigate to a topic, bringing the window to front if already showing.
 */
public class ContextHelpDialog extends Stage {

    /** Sentinel value used for the glossary leaf node in the tree. */
    static final String GLOSSARY_CATEGORY = "Glossary";

    private final TreeView<HelpTopic> treeView;
    private final StackPane contentPane;
    private final Map<HelpTopic, TreeItem<HelpTopic>> itemsByTopic = new LinkedHashMap<>();
    private TreeItem<HelpTopic> glossaryItem;
    private GlossaryPane glossaryPane;

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
                    if (newItem != null && newItem.isLeaf()) {
                        if (newItem == glossaryItem) {
                            showGlossary(null);
                        } else if (newItem.getValue() != null) {
                            showContent(newItem.getValue());
                        }
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

    /**
     * Navigates to the glossary, optionally scrolling to a specific term.
     *
     * @param term the term to highlight, or null to show the full glossary
     */
    public void showGlossaryTerm(String term) {
        if (glossaryItem != null) {
            if (glossaryItem.getParent() != null) {
                glossaryItem.getParent().setExpanded(true);
            }
            treeView.getSelectionModel().select(glossaryItem);
        }
        showGlossary(term);
        toFront();
        requestFocus();
    }

    private void showContent(HelpTopic topic) {
        contentPane.getChildren().setAll(
                HelpContent.forTopic(topic, this::showGlossaryTerm));
    }

    private void showGlossary(String term) {
        if (glossaryPane == null) {
            glossaryPane = new GlossaryPane();
            glossaryPane.setOnTermNavigate(this::showGlossaryTerm);
        }
        contentPane.getChildren().setAll(glossaryPane);
        if (term != null) {
            glossaryPane.showTerm(term);
        }
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

        // Add glossary as a leaf under a "Glossary" category
        TreeItem<HelpTopic> glossaryCat = new TreeItem<>(null);
        glossaryCat.setExpanded(true);
        glossaryItem = new TreeItem<>(null);
        glossaryCat.getChildren().add(glossaryItem);
        root.getChildren().add(glossaryCat);

        TreeView<HelpTopic> tree = new TreeView<>(root);
        tree.setCellFactory(tv -> new TreeCell<>() {
            @Override
            protected void updateItem(HelpTopic item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || getTreeItem() == null) {
                    setText(null);
                    setStyle("");
                } else if (getTreeItem() == glossaryItem) {
                    setText("SD Terminology");
                    setStyle("");
                } else if (item == null) {
                    // Category node — find category name from first child
                    TreeItem<HelpTopic> treeItem = getTreeItem();
                    if (treeItem == glossaryItem.getParent()) {
                        setText(GLOSSARY_CATEGORY);
                        setStyle("-fx-font-weight: bold;");
                    } else if (!treeItem.getChildren().isEmpty()) {
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
