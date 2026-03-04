package com.deathrayresearch.forrester.app.canvas;

import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.util.Duration;

import java.util.List;
import java.util.function.Consumer;

/**
 * Shared mutable context passed to {@link ElementForm} implementations.
 * Holds references to the canvas, editor, property grid, and the current element name.
 * Also provides UI helper methods for building form rows.
 */
class FormContext {

    ModelCanvas canvas;
    ModelEditor editor;
    GridPane grid;
    String elementName;
    boolean updatingFields;
    Runnable onFormRebuildRequested;

    void requestFormRebuild() {
        if (onFormRebuildRequested != null) {
            onFormRebuildRequested.run();
        }
    }

    TextField createTextField(String text) {
        TextField field = new TextField(text);
        field.setMaxWidth(Double.MAX_VALUE);
        GridPane.setHgrow(field, Priority.ALWAYS);
        return field;
    }

    TextField createNameField() {
        TextField nameField = createTextField(elementName);
        nameField.setId("nameField");
        nameField.setOnAction(e -> commitRename(nameField));
        nameField.focusedProperty().addListener((obs, wasFocused, isFocused) -> {
            if (!isFocused && !updatingFields) {
                commitRename(nameField);
            }
        });
        return nameField;
    }

    void addFieldRow(int row, String labelText, Node field) {
        addFieldRow(row, labelText, field, null);
    }

    void addFieldRow(int row, String labelText, Node field, String helpText) {
        Label label = new Label(labelText);
        label.setStyle(Styles.FIELD_LABEL);
        grid.add(buildLabelNode(label, helpText), 0, row);
        grid.add(field, 1, row);
    }

    void addReadOnlyRow(int row, String labelText, String value) {
        addReadOnlyRow(row, labelText, value, null);
    }

    void addReadOnlyRow(int row, String labelText, String value, String helpText) {
        Label label = new Label(labelText);
        label.setStyle(Styles.FIELD_LABEL);
        Label valueLabel = new Label(value);
        valueLabel.setStyle(Styles.SMALL_TEXT);
        valueLabel.setWrapText(true);
        grid.add(buildLabelNode(label, helpText), 0, row);
        grid.add(valueLabel, 1, row);
    }

    void addReadOnlyRow(int row, String labelText, Label valueLabel) {
        addReadOnlyRow(row, labelText, valueLabel, null);
    }

    void addReadOnlyRow(int row, String labelText, Label valueLabel, String helpText) {
        Label label = new Label(labelText);
        label.setStyle(Styles.FIELD_LABEL);
        valueLabel.setStyle(Styles.SMALL_TEXT);
        valueLabel.setWrapText(true);
        grid.add(buildLabelNode(label, helpText), 0, row);
        grid.add(valueLabel, 1, row);
    }

    private Node buildLabelNode(Label label, String helpText) {
        if (helpText == null) {
            return label;
        }
        Label icon = new Label("\u24D8");
        icon.setStyle(Styles.HELP_ICON);
        Tooltip tip = new Tooltip(helpText);
        tip.setWrapText(true);
        tip.setMaxWidth(300);
        tip.setShowDelay(Duration.millis(300));
        Tooltip.install(icon, tip);
        HBox box = new HBox(4, label, icon);
        box.setAlignment(Pos.CENTER_LEFT);
        return box;
    }

    void addCommitHandlers(TextField field, Consumer<TextField> handler) {
        field.setOnAction(e -> handler.accept(field));
        field.focusedProperty().addListener((obs, wasFocused, isFocused) -> {
            if (!isFocused && !updatingFields) {
                handler.accept(field);
            }
        });
    }

    /** Common unit names for stock/aux/constant unit fields. */
    static final List<String> COMMON_UNITS = List.of(
            "Dimensionless",
            "Person", "Thing",
            "Day", "Week", "Month", "Year",
            "Hour", "Minute", "Second",
            "USD",
            "Kilogram", "Gram", "Pound",
            "Meter", "Kilometer", "Mile",
            "Liter", "Gallon US",
            "Celsius", "Fahrenheit"
    );

    /** Time unit names for flow time unit fields. */
    static final List<String> TIME_UNITS = List.of(
            "Day", "Week", "Month", "Year",
            "Hour", "Minute", "Second", "Millisecond"
    );

    ComboBox<String> createUnitComboBox(String currentValue) {
        ComboBox<String> box = new ComboBox<>();
        box.getItems().addAll(COMMON_UNITS);
        box.setEditable(true);
        box.setValue(currentValue != null ? currentValue : "");
        box.setMaxWidth(Double.MAX_VALUE);
        GridPane.setHgrow(box, Priority.ALWAYS);
        return box;
    }

    ComboBox<String> createTimeUnitComboBox(String currentValue) {
        ComboBox<String> box = new ComboBox<>();
        box.getItems().addAll(TIME_UNITS);
        box.setEditable(true);
        box.setValue(currentValue != null ? currentValue : "");
        box.setMaxWidth(Double.MAX_VALUE);
        GridPane.setHgrow(box, Priority.ALWAYS);
        return box;
    }

    void addComboCommitHandlers(ComboBox<String> box, Consumer<ComboBox<String>> handler) {
        box.setOnAction(e -> {
            if (!updatingFields) {
                handler.accept(box);
            }
        });
        box.focusedProperty().addListener((obs, wasFocused, isFocused) -> {
            if (!isFocused && !updatingFields) {
                handler.accept(box);
            }
        });
    }

    private void commitRename(TextField nameField) {
        String oldName = elementName;
        String newName = nameField.getText().trim();
        if (newName.isEmpty() || newName.equals(oldName) || !ModelEditor.isValidName(newName)) {
            nameField.setText(oldName);
            return;
        }
        if (editor.hasElement(newName)) {
            nameField.setText(oldName);
            return;
        }
        canvas.renameElement(oldName, newName);
        elementName = newName;
    }
}
