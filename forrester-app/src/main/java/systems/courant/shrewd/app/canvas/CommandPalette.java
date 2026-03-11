package systems.courant.forrester.app.canvas;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Popup;
import javafx.stage.Window;

import java.util.Comparator;
import java.util.List;
import java.util.function.Supplier;

/**
 * A searchable command palette activated by Ctrl+K / Cmd+K for rapid model building.
 * Supports prefix matching, substring matching, and fuzzy subsequence matching
 * on command names and model element names.
 */
public class CommandPalette {

    /**
     * A named command that can be executed from the palette.
     *
     * @param name     display name shown in the result list
     * @param category short label shown to the right (e.g. "Build", "Simulate", "Stock")
     * @param action   action to run when the command is selected
     */
    public record Command(String name, String category, Runnable action) {}

    private final Popup popup = new Popup();
    private final TextField searchField = new TextField();
    private final ListView<Command> resultList = new ListView<>();
    private final Supplier<List<Command>> commandSupplier;
    private List<Command> allCommands = List.of();

    private static final int MAX_VISIBLE = 12;
    private static final double WIDTH = 480;

    public CommandPalette(Supplier<List<Command>> commandSupplier) {
        this.commandSupplier = commandSupplier;
        buildUI();
    }

    private void buildUI() {
        searchField.setPromptText("Type a command or element name\u2026");
        searchField.setStyle("-fx-font-size: 14px; -fx-padding: 8 12 8 12;");
        searchField.setPrefWidth(WIDTH);

        resultList.setPrefWidth(WIDTH);
        resultList.setMaxHeight(MAX_VISIBLE * 30);
        resultList.setFocusTraversable(false);
        resultList.setCellFactory(lv -> new CommandCell());

        VBox container = new VBox(searchField, resultList);
        container.setStyle(
                "-fx-background-color: white;"
                        + " -fx-border-color: #BDC3C7; -fx-border-width: 1;"
                        + " -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.25), 16, 0, 0, 4);"
                        + " -fx-background-radius: 6; -fx-border-radius: 6;");
        container.setPrefWidth(WIDTH);

        popup.getContent().add(container);
        popup.setAutoHide(true);
        popup.setHideOnEscape(true);

        searchField.textProperty().addListener((obs, old, text) -> updateResults(text));

        searchField.setOnKeyPressed(event -> {
            switch (event.getCode()) {
                case DOWN -> {
                    resultList.getSelectionModel().selectNext();
                    resultList.scrollTo(resultList.getSelectionModel().getSelectedIndex());
                    event.consume();
                }
                case UP -> {
                    resultList.getSelectionModel().selectPrevious();
                    resultList.scrollTo(resultList.getSelectionModel().getSelectedIndex());
                    event.consume();
                }
                case ENTER -> {
                    executeSelected();
                    event.consume();
                }
                default -> { }
            }
        });

        resultList.setOnMouseClicked(event -> executeSelected());
    }

    /**
     * Shows the palette centered near the top of the given owner window.
     * Refreshes the command list from the supplier each time it is shown.
     */
    public void show(Window owner) {
        allCommands = commandSupplier.get();
        searchField.clear();
        updateResults("");

        double x = owner.getX() + (owner.getWidth() - WIDTH) / 2;
        double y = owner.getY() + 100;
        popup.show(owner, x, y);
        searchField.requestFocus();
    }

    private void updateResults(String query) {
        List<Command> filtered;
        if (query == null || query.isBlank()) {
            filtered = allCommands;
        } else {
            String q = query.strip().toLowerCase();
            filtered = allCommands.stream()
                    .map(cmd -> new ScoredCommand(cmd, score(cmd.name().toLowerCase(), q)))
                    .filter(sc -> sc.score > 0)
                    .sorted(Comparator.comparingInt(ScoredCommand::score).reversed())
                    .map(ScoredCommand::command)
                    .toList();
        }
        resultList.getItems().setAll(filtered);
        if (!filtered.isEmpty()) {
            resultList.getSelectionModel().selectFirst();
        }
        resultList.setVisible(!filtered.isEmpty());
        resultList.setManaged(!filtered.isEmpty());
    }

    private void executeSelected() {
        Command selected = resultList.getSelectionModel().getSelectedItem();
        if (selected != null) {
            popup.hide();
            selected.action().run();
        }
    }

    /**
     * Scores how well {@code text} matches {@code query}.
     * Returns 0 for no match. Higher scores indicate better matches.
     * Priority: prefix &gt; substring &gt; fuzzy subsequence.
     */
    static int score(String text, String query) {
        if (text.startsWith(query)) {
            return 300 + (1000 - text.length());
        }
        int idx = text.indexOf(query);
        if (idx >= 0) {
            return 200 + (1000 - idx);
        }
        // Fuzzy subsequence match
        int ti = 0;
        int qi = 0;
        int matchScore = 100;
        boolean lastMatched = false;
        while (ti < text.length() && qi < query.length()) {
            if (text.charAt(ti) == query.charAt(qi)) {
                qi++;
                if (lastMatched) {
                    matchScore += 5;
                }
                if (ti == 0 || !Character.isLetterOrDigit(text.charAt(ti - 1))) {
                    matchScore += 10;
                }
                lastMatched = true;
            } else {
                lastMatched = false;
            }
            ti++;
        }
        return qi == query.length() ? matchScore : 0;
    }

    private record ScoredCommand(Command command, int score) {}

    private static class CommandCell extends ListCell<Command> {
        private final HBox hbox = new HBox(8);
        private final Label nameLabel = new Label();
        private final Label categoryLabel = new Label();

        CommandCell() {
            hbox.setAlignment(Pos.CENTER_LEFT);
            hbox.setPadding(new Insets(3, 10, 3, 10));
            HBox.setHgrow(nameLabel, Priority.ALWAYS);
            nameLabel.setMaxWidth(Double.MAX_VALUE);
            nameLabel.setStyle("-fx-font-size: 13px;");
            categoryLabel.setStyle(Styles.MUTED_TEXT + " -fx-font-size: 11px;");
            hbox.getChildren().addAll(nameLabel, categoryLabel);
        }

        @Override
        protected void updateItem(Command item, boolean empty) {
            super.updateItem(item, empty);
            if (empty || item == null) {
                setGraphic(null);
            } else {
                nameLabel.setText(item.name());
                categoryLabel.setText(item.category());
                setGraphic(hbox);
            }
        }
    }
}
