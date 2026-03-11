package systems.courant.shrewd.app.canvas;

import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

/**
 * A reference card dialog showing all keyboard shortcuts.
 */
public class KeyboardShortcutsDialog extends Stage {

    public KeyboardShortcutsDialog() {
        setTitle("Keyboard Shortcuts");

        VBox content = new VBox(16);
        content.setPadding(new Insets(20));

        content.getChildren().addAll(
                section("File",
                        "Ctrl+N", "New model",
                        "Ctrl+Shift+N", "New window",
                        "Ctrl+O", "Open model",
                        "Ctrl+S", "Save",
                        "Ctrl+Shift+S", "Save as",
                        "Ctrl+E", "Export diagram",
                        "Ctrl+W", "Close window"),
                section("Edit",
                        "Ctrl+Z", "Undo",
                        "Ctrl+Shift+Z", "Redo",
                        "Ctrl+C", "Copy selection",
                        "Ctrl+X", "Cut selection",
                        "Ctrl+V", "Paste",
                        "Ctrl+A", "Select all",
                        "Delete / Backspace", "Delete selected element or connection"),
                section("Tools",
                        "1", "Select tool",
                        "2", "Place stock",
                        "3", "Place flow",
                        "4", "Place variable",
                        "5", "Place module",
                        "6", "Place lookup table",
                        "7", "Place CLD variable",
                        "8", "Draw causal link"),
                section("View & Navigation",
                        "Ctrl+K", "Command palette",
                        "Ctrl+Shift+F", "Zoom to fit",
                        "Ctrl+ +", "Zoom in",
                        "Ctrl+ -", "Zoom out",
                        "Ctrl+0", "Reset zoom",
                        "Scroll wheel", "Zoom in/out",
                        "Space + drag", "Pan canvas",
                        "[ / ]", "Step through feedback loops",
                        "Ctrl+L", "Toggle activity log",
                        "Ctrl+Shift+D", "Pop out / dock dashboard"),
                section("Equation Editor",
                        "Enter", "Commit equation",
                        "Shift+Enter", "New line",
                        "Tab", "Insert autocomplete suggestion",
                        "Up / Down", "Navigate autocomplete suggestions",
                        "Escape", "Dismiss autocomplete"),
                section("Simulation",
                        "Ctrl+R", "Run simulation",
                        "Ctrl+B", "Validate model"),
                section("Escape Priority Chain",
                        "1st", "Cancel resize / marquee / reattach / reroute",
                        "2nd", "Cancel pending flow or causal link creation",
                        "3rd", "Reset tool to Select",
                        "4th", "Deselect connection",
                        "5th", "Clear element selection",
                        "6th", "Navigate back from module")
        );

        ScrollPane scroll = new ScrollPane(content);
        scroll.setFitToWidth(true);

        Scene scene = new Scene(scroll, 480, 700);
        setScene(scene);
    }

    private VBox section(String title, String... pairs) {
        Label header = new Label(title);
        header.setStyle("-fx-font-size: 14px; -fx-font-weight: bold;");

        GridPane grid = new GridPane();
        grid.setHgap(16);
        grid.setVgap(4);
        grid.setPadding(new Insets(4, 0, 0, 0));

        ColumnConstraints keyCol = new ColumnConstraints();
        keyCol.setMinWidth(160);
        ColumnConstraints descCol = new ColumnConstraints();
        descCol.setMinWidth(200);
        grid.getColumnConstraints().addAll(keyCol, descCol);

        for (int i = 0; i < pairs.length; i += 2) {
            int row = i / 2;
            Label key = new Label(pairs[i]);
            key.setStyle("-fx-font-family: monospace; -fx-font-weight: bold;");
            Label desc = new Label(pairs[i + 1]);
            grid.add(key, 0, row);
            grid.add(desc, 1, row);
        }

        VBox box = new VBox(4, header, grid);
        return box;
    }
}
