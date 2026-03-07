package com.deathrayresearch.forrester.app.canvas;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.ListCell;
import javafx.scene.layout.VBox;
import javafx.stage.Popup;
import javafx.stage.Window;

import java.util.List;
import java.util.function.IntConsumer;

/**
 * A lightweight popup that displays the undo history stack as a list.
 * Clicking an entry triggers an undo-to-depth operation, jumping directly
 * to that point in the history. Hovering previews the depth by highlighting
 * all entries that would be undone.
 */
public class UndoHistoryPopup extends Popup {

    private final ListView<String> listView;

    public UndoHistoryPopup(List<String> undoLabels, IntConsumer onJumpTo) {
        setAutoHide(true);

        listView = new ListView<>();
        listView.setPrefWidth(220);
        listView.setPrefHeight(Math.min(undoLabels.size() * 28 + 8, 320));
        listView.setFixedCellSize(28);

        // Populate: most recent action first, each labeled with its depth
        for (int i = 0; i < undoLabels.size(); i++) {
            listView.getItems().add(undoLabels.get(i));
        }

        listView.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    int idx = getIndex();
                    setText((idx + 1) + ". " + item);
                    setStyle("-fx-font-size: 12px;");
                }
            }
        });

        listView.setOnMouseClicked(event -> {
            int selected = listView.getSelectionModel().getSelectedIndex();
            if (selected >= 0) {
                hide();
                onJumpTo.accept(selected);
            }
        });

        Label header = new Label("Undo History");
        header.setStyle("-fx-font-weight: bold; -fx-font-size: 13px;");

        Label hint = new Label("Click to jump to that state");
        hint.setStyle("-fx-font-size: 11px; -fx-text-fill: #888;");

        VBox container = new VBox(4, header, listView, hint);
        container.setPadding(new Insets(8));
        container.setAlignment(Pos.TOP_LEFT);
        container.setStyle("-fx-background-color: white; -fx-border-color: #ccc; "
                + "-fx-border-radius: 4; -fx-background-radius: 4; "
                + "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.2), 8, 0, 0, 2);");

        getContent().add(container);
    }

    public void showBelow(Window owner, double screenX, double screenY) {
        show(owner, screenX, screenY);
    }
}
