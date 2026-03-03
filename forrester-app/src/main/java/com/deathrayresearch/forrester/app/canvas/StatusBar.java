package com.deathrayresearch.forrester.app.canvas;

import javafx.geometry.Insets;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;

/**
 * Bottom status bar showing the active tool, selection count, element counts, and zoom level.
 */
public class StatusBar extends HBox {

    private final Label toolLabel = new Label();
    private final Label selectionLabel = new Label();
    private final Label elementsLabel = new Label();
    private final Label zoomLabel = new Label();

    public StatusBar() {
        setSpacing(0);
        setPadding(new Insets(3, 8, 3, 8));
        setStyle("-fx-background-color: #E8EAED; -fx-border-color: #BDC3C7; -fx-border-width: 1 0 0 0;");

        String labelStyle = "-fx-font-size: 11px; -fx-text-fill: #555;";
        toolLabel.setStyle(labelStyle);
        selectionLabel.setStyle(labelStyle);
        elementsLabel.setStyle(labelStyle);
        zoomLabel.setStyle(labelStyle);

        Region spacer1 = new Region();
        HBox.setHgrow(spacer1, Priority.ALWAYS);
        Region spacer2 = new Region();
        HBox.setHgrow(spacer2, Priority.ALWAYS);

        Label sep1 = new Label("  |  ");
        sep1.setStyle(labelStyle);
        Label sep2 = new Label("  |  ");
        sep2.setStyle(labelStyle);

        getChildren().addAll(toolLabel, sep1, selectionLabel, spacer1, elementsLabel, spacer2, sep2, zoomLabel);

        updateTool(CanvasToolBar.Tool.SELECT);
        updateSelection(0);
        updateElements(0, 0, 0, 0);
        updateZoom(1.0);
    }

    public void updateTool(CanvasToolBar.Tool tool) {
        String name = switch (tool) {
            case SELECT -> "Select";
            case PLACE_STOCK -> "Place Stock";
            case PLACE_FLOW -> "Place Flow";
            case PLACE_AUX -> "Place Auxiliary";
            case PLACE_CONSTANT -> "Place Constant";
        };
        toolLabel.setText(name);
    }

    public void updateSelection(int count) {
        if (count == 0) {
            selectionLabel.setText("No selection");
        } else if (count == 1) {
            selectionLabel.setText("1 selected");
        } else {
            selectionLabel.setText(count + " selected");
        }
    }

    public void updateElements(int stocks, int flows, int auxiliaries, int constants) {
        int total = stocks + flows + auxiliaries + constants;
        if (total == 0) {
            elementsLabel.setText("Empty model");
        } else {
            elementsLabel.setText(total + " elements ("
                    + stocks + " stocks, " + flows + " flows, "
                    + auxiliaries + " aux, " + constants + " const)");
        }
    }

    public void updateZoom(double scale) {
        zoomLabel.setText(Math.round(scale * 100) + "%");
    }
}
