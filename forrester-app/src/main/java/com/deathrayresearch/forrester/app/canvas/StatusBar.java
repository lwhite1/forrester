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
    private final Label loopLabel = new Label();
    private final Label validationLabel = new Label();
    private final Label zoomLabel = new Label();

    public StatusBar() {
        setSpacing(0);
        setPadding(new Insets(3, 8, 3, 8));
        setStyle(Styles.STATUS_BAR_BACKGROUND);

        toolLabel.setStyle(Styles.STATUS_LABEL);
        selectionLabel.setStyle(Styles.STATUS_LABEL);
        elementsLabel.setStyle(Styles.STATUS_LABEL);
        loopLabel.setStyle(Styles.STATUS_LABEL);
        loopLabel.setVisible(false);
        loopLabel.setManaged(false);
        validationLabel.setStyle(Styles.STATUS_LABEL);
        validationLabel.setVisible(false);
        validationLabel.setManaged(false);
        zoomLabel.setStyle(Styles.STATUS_LABEL);

        Region spacer1 = new Region();
        HBox.setHgrow(spacer1, Priority.ALWAYS);
        Region spacer2 = new Region();
        HBox.setHgrow(spacer2, Priority.ALWAYS);

        Label sep1 = new Label("  |  ");
        sep1.setStyle(Styles.STATUS_LABEL);
        Label sep2 = new Label("  |  ");
        sep2.setStyle(Styles.STATUS_LABEL);
        Label loopSep = new Label("  |  ");
        loopSep.setStyle(Styles.STATUS_LABEL);
        loopSep.visibleProperty().bind(loopLabel.visibleProperty());
        loopSep.managedProperty().bind(loopLabel.managedProperty());

        Label validationSep = new Label("  |  ");
        validationSep.setStyle(Styles.STATUS_LABEL);
        validationSep.visibleProperty().bind(validationLabel.visibleProperty());
        validationSep.managedProperty().bind(validationLabel.managedProperty());

        getChildren().addAll(toolLabel, sep1, selectionLabel,
                loopSep, loopLabel, validationSep, validationLabel,
                spacer1, elementsLabel, spacer2, sep2, zoomLabel);

        updateTool(CanvasToolBar.Tool.SELECT);
        updateSelection(0);
        updateElements(0, 0, 0, 0, 0);
        updateZoom(1.0);
    }

    public void updateTool(CanvasToolBar.Tool tool) {
        String name = switch (tool) {
            case SELECT -> "Select";
            case PLACE_STOCK -> "Place Stock";
            case PLACE_FLOW -> "Place Flow";
            case PLACE_AUX -> "Place Auxiliary";
            case PLACE_CONSTANT -> "Place Constant";
            case PLACE_MODULE -> "Place Module";
            case PLACE_LOOKUP -> "Place Lookup";
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

    public void updateElements(int stocks, int flows, int auxiliaries, int constants, int modules) {
        int total = stocks + flows + auxiliaries + constants + modules;
        if (total == 0) {
            elementsLabel.setText("Empty model");
        } else {
            String text = total + " elements ("
                    + stocks + " stocks, " + flows + " flows, "
                    + auxiliaries + " aux, " + constants + " const";
            if (modules > 0) {
                text += ", " + modules + " mod";
            }
            text += ")";
            elementsLabel.setText(text);
        }
    }

    public void updateLoops(int loopCount) {
        loopLabel.setVisible(true);
        loopLabel.setManaged(true);
        if (loopCount == 0) {
            loopLabel.setText("No loops");
        } else if (loopCount == 1) {
            loopLabel.setText("1 loop");
        } else {
            loopLabel.setText(loopCount + " loops");
        }
    }

    public void clearLoops() {
        loopLabel.setVisible(false);
        loopLabel.setManaged(false);
    }

    public void updateValidation(int errorCount, int warningCount) {
        validationLabel.setVisible(true);
        validationLabel.setManaged(true);
        if (errorCount > 0) {
            validationLabel.setText(errorCount + " errors, " + warningCount + " warnings");
            validationLabel.setStyle(Styles.STATUS_LABEL + "-fx-text-fill: #d62728;");
        } else if (warningCount > 0) {
            validationLabel.setText(warningCount + " warnings");
            validationLabel.setStyle(Styles.STATUS_LABEL + "-fx-text-fill: #ff7f0e;");
        } else {
            validationLabel.setText("No issues");
            validationLabel.setStyle(Styles.STATUS_LABEL);
        }
    }

    public void clearValidation() {
        validationLabel.setVisible(false);
        validationLabel.setManaged(false);
    }

    public void updateZoom(double scale) {
        zoomLabel.setText(Math.round(scale * 100) + "%");
    }
}
