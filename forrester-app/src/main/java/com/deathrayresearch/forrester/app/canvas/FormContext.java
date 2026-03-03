package com.deathrayresearch.forrester.app.canvas;

import javafx.scene.Node;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;

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
        Label label = new Label(labelText);
        label.setStyle(Styles.FIELD_LABEL);
        grid.add(label, 0, row);
        grid.add(field, 1, row);
    }

    void addReadOnlyRow(int row, String labelText, String value) {
        Label label = new Label(labelText);
        label.setStyle(Styles.FIELD_LABEL);
        Label valueLabel = new Label(value);
        valueLabel.setStyle(Styles.SMALL_TEXT);
        valueLabel.setWrapText(true);
        grid.add(label, 0, row);
        grid.add(valueLabel, 1, row);
    }

    void addReadOnlyRow(int row, String labelText, Label valueLabel) {
        Label label = new Label(labelText);
        label.setStyle(Styles.FIELD_LABEL);
        valueLabel.setStyle(Styles.SMALL_TEXT);
        valueLabel.setWrapText(true);
        grid.add(label, 0, row);
        grid.add(valueLabel, 1, row);
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
