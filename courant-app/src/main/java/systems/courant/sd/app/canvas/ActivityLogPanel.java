package systems.courant.sd.app.canvas;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.layout.VBox;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * A collapsible panel that displays a chronological log of user actions.
 * Entries auto-scroll to the bottom as new items are added.
 */
public class ActivityLogPanel extends VBox {

    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm:ss");

    private final ObservableList<ActivityLogEntry> entries = FXCollections.observableArrayList();
    private final ListView<ActivityLogEntry> listView;

    public ActivityLogPanel() {
        setPrefWidth(250);
        setMinWidth(180);
        setStyle(Styles.ACTIVITY_LOG_PANEL);

        Label title = new Label("Activity Log");
        title.setStyle(Styles.ACTIVITY_LOG_TITLE);

        listView = new ListView<>(entries);
        listView.setCellFactory(lv -> new LogCell());
        listView.setStyle(Styles.TRANSPARENT_BACKGROUND);
        VBox.setVgrow(listView, javafx.scene.layout.Priority.ALWAYS);

        getChildren().addAll(title, listView);
    }

    /**
     * Logs a new activity entry and scrolls to show it.
     */
    public void log(String type, String message) {
        ActivityLogEntry entry = new ActivityLogEntry(LocalDateTime.now(), type, message);
        if (Platform.isFxApplicationThread()) {
            addEntry(entry);
        } else {
            Platform.runLater(() -> addEntry(entry));
        }
    }

    private static final int MAX_ENTRIES = 1000;

    private void addEntry(ActivityLogEntry entry) {
        if (entries.size() >= MAX_ENTRIES) {
            entries.remove(0);
        }
        entries.add(entry);
        listView.scrollTo(entries.size() - 1);
    }

    /**
     * Creates a {@link ModelEditListener} that logs events to this panel.
     */
    public ModelEditListener createListener() {
        return new ModelEditListener() {
            @Override
            public void onElementAdded(String name, String typeName) {
                log("edit", "Added " + typeName + " \"" + name + "\"");
            }

            @Override
            public void onElementRemoved(String name) {
                log("edit", "Removed \"" + name + "\"");
            }

            @Override
            public void onElementRenamed(String oldName, String newName) {
                log("edit", "Renamed \"" + oldName + "\" to \"" + newName + "\"");
            }

            @Override
            public void onEquationChanged(String elementName) {
                log("edit", "Equation changed: \"" + elementName + "\"");
            }

            @Override
            public void onSimulationRun() {
                log("simulation", "Simulation completed");
            }

            @Override
            public void onValidation(int errors, int warnings) {
                log("validation", "Validation: " + errors + " errors, " + warnings + " warnings");
            }

            @Override
            public void onAnalysisRun(String type, String details) {
                log("analysis", type + ": " + details);
            }

            @Override
            public void onModelSaved(String filename) {
                log("file", "Saved: " + filename);
            }

            @Override
            public void onModelOpened(String filename) {
                log("file", "Opened: " + filename);
            }
        };
    }

    private static class LogCell extends ListCell<ActivityLogEntry> {

        private final VBox container = new VBox(1);
        private final Label timeLabel = new Label();
        private final Label messageLabel = new Label();

        LogCell() {
            timeLabel.setStyle(Styles.LOG_TIME_LABEL);
            messageLabel.setStyle(Styles.LOG_MESSAGE_LABEL);
            messageLabel.setWrapText(true);
            container.setPadding(new Insets(2, 6, 2, 6));
            container.getChildren().addAll(timeLabel, messageLabel);
        }

        @Override
        protected void updateItem(ActivityLogEntry item, boolean empty) {
            super.updateItem(item, empty);
            if (empty || item == null) {
                setGraphic(null);
            } else {
                timeLabel.setText(item.timestamp().format(TIME_FORMAT));
                messageLabel.setText(item.message());
                setGraphic(container);
            }
        }
    }
}
