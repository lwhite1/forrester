package systems.courant.sd.app.canvas;

import javafx.geometry.Insets;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;

import java.util.List;
import java.util.function.Consumer;

/**
 * A pane displaying the SD terminology glossary with search filtering.
 * Each entry shows the term, definition, relevance, and cross-reference links.
 */
public final class GlossaryPane extends VBox {

    private final TextField searchField;
    private final VBox entriesBox;
    private final ScrollPane scrollPane;
    private Consumer<String> onTermNavigate;
    private String highlightedTerm;

    public GlossaryPane() {
        setSpacing(0);
        setPadding(Insets.EMPTY);

        searchField = new TextField();
        searchField.setPromptText("Search glossary\u2026");
        searchField.setId("glossarySearch");
        VBox.setMargin(searchField, new Insets(8, 12, 8, 12));

        entriesBox = new VBox(0);
        entriesBox.setPadding(new Insets(0, 12, 12, 12));

        scrollPane = new ScrollPane(entriesBox);
        scrollPane.setFitToWidth(true);
        scrollPane.setId("glossaryScroll");

        getChildren().addAll(searchField, scrollPane);

        searchField.textProperty().addListener((obs, oldVal, newVal) -> refresh(newVal));
        refresh("");
    }

    /**
     * Sets a callback invoked when the user clicks a cross-reference link.
     */
    public void setOnTermNavigate(Consumer<String> callback) {
        this.onTermNavigate = callback;
    }

    /**
     * Scrolls to and highlights the entry for the given term.
     */
    public void showTerm(String term) {
        highlightedTerm = term;
        searchField.clear();
        refresh("");

        // Find the entry node and scroll to it
        for (var node : entriesBox.getChildren()) {
            if (node instanceof VBox entryBox && term.equals(entryBox.getUserData())) {
                scrollPane.layout();
                double y = entryBox.getBoundsInParent().getMinY();
                double contentHeight = entriesBox.getBoundsInLocal().getHeight();
                double viewportHeight = scrollPane.getViewportBounds().getHeight();
                if (contentHeight > viewportHeight) {
                    scrollPane.setVvalue(y / (contentHeight - viewportHeight));
                }
                break;
            }
        }
    }

    private void refresh(String query) {
        entriesBox.getChildren().clear();
        Glossary glossary = Glossary.instance();
        List<Glossary.Entry> results = glossary.search(query);

        for (Glossary.Entry entry : results) {
            entriesBox.getChildren().add(buildEntryNode(entry));
        }
    }

    private VBox buildEntryNode(Glossary.Entry entry) {
        VBox box = new VBox(4);
        box.setPadding(new Insets(10, 0, 10, 0));
        box.setUserData(entry.term());
        box.setStyle("-fx-border-color: transparent transparent #e0e0e0 transparent; "
                + "-fx-border-width: 0 0 1 0;");

        if (entry.term().equals(highlightedTerm)) {
            box.setStyle(box.getStyle()
                    + " -fx-background-color: #fff3cd; -fx-background-radius: 4;");
        }

        // Term heading
        Text termText = new Text(entry.term());
        termText.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");
        TextFlow heading = new TextFlow(termText);

        // Courant term
        if (entry.courantTerm() != null && !entry.courantTerm().isEmpty()) {
            Text courantLabel = new Text("  In Courant: ");
            courantLabel.setStyle("-fx-fill: #0066cc;");
            Text courantText = new Text(entry.courantTerm());
            courantText.setStyle("-fx-fill: #0066cc; -fx-font-weight: bold;");
            heading.getChildren().addAll(courantLabel, courantText);
        }

        // Aliases
        if (!entry.aliases().isEmpty()) {
            Text aliasLabel = new Text("  Also: ");
            aliasLabel.setStyle("-fx-fill: #666;");
            Text aliasText = new Text(String.join(", ", entry.aliases()));
            aliasText.setStyle("-fx-fill: #666; -fx-font-style: italic;");
            heading.getChildren().addAll(aliasLabel, aliasText);
        }

        // Definition
        Text defText = new Text(entry.definition());
        TextFlow defFlow = new TextFlow(defText);
        defFlow.setMaxWidth(480);

        // Relevance
        Text relLabel = new Text("Relevance: ");
        relLabel.setStyle("-fx-font-weight: bold; -fx-fill: #444;");
        Text relText = new Text(entry.relevance());
        relText.setStyle("-fx-fill: #444;");
        TextFlow relFlow = new TextFlow(relLabel, relText);
        relFlow.setMaxWidth(480);

        box.getChildren().addAll(heading, defFlow, relFlow);

        // Related terms as hyperlinks
        if (!entry.related().isEmpty()) {
            TextFlow relatedFlow = new TextFlow();
            relatedFlow.setMaxWidth(480);
            Text seeAlso = new Text("See also: ");
            seeAlso.setStyle("-fx-font-weight: bold; -fx-fill: #444;");
            relatedFlow.getChildren().add(seeAlso);

            for (int i = 0; i < entry.related().size(); i++) {
                String related = entry.related().get(i);
                Hyperlink link = new Hyperlink(related);
                link.setOnAction(e -> {
                    if (onTermNavigate != null) {
                        onTermNavigate.accept(related);
                    } else {
                        showTerm(related);
                    }
                });
                relatedFlow.getChildren().add(link);
                if (i < entry.related().size() - 1) {
                    relatedFlow.getChildren().add(new Text(", "));
                }
            }
            box.getChildren().add(relatedFlow);
        }

        return box;
    }
}
