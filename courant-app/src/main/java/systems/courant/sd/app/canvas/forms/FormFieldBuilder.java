package systems.courant.sd.app.canvas.forms;

import javafx.animation.PauseTransition;
import javafx.geometry.Pos;
import javafx.geometry.Side;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

import javafx.collections.FXCollections;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import systems.courant.sd.app.canvas.EquationField;
import systems.courant.sd.app.canvas.EquationTemplates;
import systems.courant.sd.app.canvas.ModelEditor;
import systems.courant.sd.app.canvas.Styles;
import systems.courant.sd.model.def.SubscriptDef;

/**
 * UI builder utility that creates and arranges form fields in the property grid.
 * Delegates to {@link FormContext} for state access (canvas, editor, element name).
 */
public class FormFieldBuilder {

    private final FormContext ctx;

    /** Common unit names for stock/aux/constant unit fields. */
    public static final List<String> COMMON_UNITS = List.of(
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
    public static final List<String> TIME_UNITS = List.of(
            "Day", "Week", "Month", "Year",
            "Hour", "Minute", "Second", "Millisecond"
    );

    public FormFieldBuilder(FormContext ctx) {
        this.ctx = ctx;
    }

    public TextField createTextField(String text) {
        TextField field = new TextField(text);
        field.setMaxWidth(Double.MAX_VALUE);
        GridPane.setHgrow(field, Priority.ALWAYS);
        return field;
    }

    /**
     * Creates a multi-line equation editor with syntax highlighting.
     */
    public EquationField createEquationField(String text) {
        CodeAreaEquationField field = new CodeAreaEquationField(text);
        Node node = field.node();
        node.setStyle(node.getStyle() + "; -fx-max-width: infinity;");
        GridPane.setHgrow(node, Priority.ALWAYS);
        return field;
    }

    public TextField createNameField() {
        TextField nameField = createTextField(ctx.getElementName());
        nameField.setId("nameField");
        nameField.setOnAction(e -> commitRename(nameField));
        nameField.focusedProperty().addListener((obs, wasFocused, isFocused) -> {
            if (!isFocused && !ctx.isUpdatingFields()) {
                commitRename(nameField);
            }
        });
        return nameField;
    }

    /**
     * Wraps an equation field in an HBox with a "?" help button that opens
     * the Expression Language dialog and a template button.
     */
    public Node wrapWithHelpButton(EquationField equationField) {
        Runnable onOpenExpressionHelp = ctx.getOnOpenExpressionHelp();
        if (onOpenExpressionHelp == null) {
            return equationField.node();
        }
        Button helpBtn = new Button("?");
        helpBtn.setId("equationHelpButton");
        helpBtn.setMinWidth(24);
        helpBtn.setMaxWidth(24);
        helpBtn.setMinHeight(24);
        helpBtn.setMaxHeight(24);
        helpBtn.setFocusTraversable(false);
        helpBtn.setStyle("-fx-font-size: 11; -fx-padding: 0;");
        Tooltip.install(helpBtn, new Tooltip("Open function reference"));
        helpBtn.setOnAction(e -> onOpenExpressionHelp.run());

        Button templateBtn = new Button("\u2261");
        templateBtn.setId("equationTemplateButton");
        templateBtn.setMinWidth(24);
        templateBtn.setMaxWidth(24);
        templateBtn.setMinHeight(24);
        templateBtn.setMaxHeight(24);
        templateBtn.setFocusTraversable(false);
        templateBtn.setStyle("-fx-font-size: 11; -fx-padding: 0;");
        Tooltip.install(templateBtn, new Tooltip("Insert equation template"));
        ContextMenu templateMenu = EquationTemplates.createMenu(equationField);
        templateBtn.setOnAction(e ->
                templateMenu.show(templateBtn, Side.BOTTOM, 0, 0));

        Node fieldNode = equationField.node();
        HBox box = new HBox(4, fieldNode, templateBtn, helpBtn);
        box.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(fieldNode, Priority.ALWAYS);
        GridPane.setHgrow(box, Priority.ALWAYS);
        return box;
    }

    public void addFieldRow(int row, String labelText, Node field) {
        addFieldRow(row, labelText, field, null);
    }

    public void addFieldRow(int row, String labelText, Node field, String helpText) {
        Label label = new Label(labelText);
        label.setStyle(Styles.FIELD_LABEL);
        ctx.getGrid().add(buildLabelNode(label, helpText), 0, row);
        ctx.getGrid().add(field, 1, row);
    }

    public void addReadOnlyRow(int row, String labelText, String value) {
        addReadOnlyRow(row, labelText, value, null);
    }

    public void addReadOnlyRow(int row, String labelText, String value, String helpText) {
        Label label = new Label(labelText);
        label.setStyle(Styles.FIELD_LABEL);
        Label valueLabel = new Label(value);
        valueLabel.setStyle(Styles.SMALL_TEXT);
        valueLabel.setWrapText(true);
        ctx.getGrid().add(buildLabelNode(label, helpText), 0, row);
        ctx.getGrid().add(valueLabel, 1, row);
    }

    public void addReadOnlyRow(int row, String labelText, Label valueLabel) {
        addReadOnlyRow(row, labelText, valueLabel, null);
    }

    public void addReadOnlyRow(int row, String labelText, Label valueLabel, String helpText) {
        Label label = new Label(labelText);
        label.setStyle(Styles.FIELD_LABEL);
        valueLabel.setStyle(Styles.SMALL_TEXT);
        valueLabel.setWrapText(true);
        ctx.getGrid().add(buildLabelNode(label, helpText), 0, row);
        ctx.getGrid().add(valueLabel, 1, row);
    }

    private Node buildLabelNode(Label label, String helpText) {
        Tooltip labelTip = new Tooltip(label.getText());
        labelTip.setShowDelay(Duration.millis(400));
        label.setTooltip(labelTip);

        if (helpText == null) {
            return label;
        }
        Label icon = new Label("\u24D8");
        icon.setStyle(Styles.HELP_ICON);
        Tooltip tip = new Tooltip(helpText);
        tip.setWrapText(true);
        tip.setMaxWidth(360);
        tip.setStyle("-fx-font-size: 13px;");
        tip.setShowDelay(Duration.millis(300));
        Tooltip.install(icon, tip);
        HBox box = new HBox(4, label, icon);
        box.setAlignment(Pos.CENTER_LEFT);
        return box;
    }

    public void addCommitHandlers(TextField field, Consumer<TextField> handler) {
        field.setOnAction(e -> handler.accept(field));
        field.focusedProperty().addListener((obs, wasFocused, isFocused) -> {
            if (!isFocused && !ctx.isUpdatingFields()) {
                handler.accept(field);
            }
        });
    }

    /**
     * Adds commit handlers to an {@link EquationField}. Enter commits; focus loss commits.
     */
    public void addEquationCommitHandlers(EquationField field, Consumer<EquationField> handler) {
        field.setOnAction(e -> handler.accept(field));
        field.focusedProperty().addListener((obs, wasFocused, isFocused) -> {
            if (!isFocused && !ctx.isUpdatingFields()) {
                handler.accept(field);
            }
        });
    }

    public void addTextAreaCommitHandlers(TextArea area, Consumer<TextArea> handler) {
        area.focusedProperty().addListener((obs, wasFocused, isFocused) -> {
            if (!isFocused && !ctx.isUpdatingFields()) {
                handler.accept(area);
            }
        });
    }

    /**
     * Creates a standard comment/description TextArea, registers commit handlers,
     * and adds it as a field row. Returns the TextArea for forms that need a reference.
     *
     * @param row            the grid row index
     * @param currentComment the element's current comment (may be {@code null})
     * @param commitHandler  handler invoked on focus-loss to persist changes
     * @return the configured TextArea
     */
    public TextArea addCommentArea(int row, String currentComment, Consumer<TextArea> commitHandler) {
        TextArea area = new TextArea(currentComment != null ? currentComment : "");
        area.setId("propComment");
        area.setPrefRowCount(2);
        area.setWrapText(true);
        area.setMaxWidth(Double.MAX_VALUE);
        GridPane.setHgrow(area, Priority.ALWAYS);
        addTextAreaCommitHandlers(area, commitHandler);
        VBox resizable = wrapResizable(area);
        addFieldRow(row, "Description", resizable, "Documentation for this element");
        return area;
    }

    /**
     * Wraps a TextArea in a VBox with a drag handle at the bottom that allows
     * the user to resize the TextArea vertically.
     */
    public VBox wrapResizable(TextArea area) {
        Region handle = new Region();
        handle.setPrefHeight(6);
        handle.setMinHeight(6);
        handle.setMaxHeight(6);
        handle.setCursor(Cursor.S_RESIZE);
        handle.setStyle("-fx-background-color: #cccccc; -fx-background-radius: 2;");
        handle.hoverProperty().addListener((obs, wasHover, isHover) ->
                handle.setStyle(isHover
                        ? "-fx-background-color: #999999; -fx-background-radius: 2;"
                        : "-fx-background-color: #cccccc; -fx-background-radius: 2;"));

        double[] dragState = new double[2]; // [startY, startHeight]
        handle.setOnMousePressed(e -> {
            dragState[0] = e.getScreenY();
            dragState[1] = area.getHeight();
            e.consume();
        });
        handle.setOnMouseDragged(e -> {
            double delta = e.getScreenY() - dragState[0];
            double newHeight = Math.max(40, dragState[1] + delta);
            area.setPrefHeight(newHeight);
            e.consume();
        });

        VBox box = new VBox(area, handle);
        box.setMaxWidth(Double.MAX_VALUE);
        GridPane.setHgrow(box, Priority.ALWAYS);
        return box;
    }

    /**
     * Adds a subscript assignment row with one checkbox per defined dimension.
     * If no subscript dimensions are defined in the model, nothing is added and
     * {@code startRow} is returned unchanged — keeping the form free of subscript
     * UI for users who don't use this feature.
     *
     * @return the next available row index
     */
    public int addSubscriptRow(int startRow, List<SubscriptDef> modelSubscripts,
                               List<String> currentSubscripts,
                               Consumer<List<String>> onChanged) {
        if (modelSubscripts == null || modelSubscripts.isEmpty()) {
            return startRow;
        }
        javafx.scene.layout.FlowPane checkboxPane = new javafx.scene.layout.FlowPane(8, 4);
        for (SubscriptDef dim : modelSubscripts) {
            javafx.scene.control.CheckBox cb = new javafx.scene.control.CheckBox(dim.name());
            cb.setSelected(currentSubscripts != null && currentSubscripts.contains(dim.name()));
            cb.setStyle("-fx-font-size: 11px;");
            cb.selectedProperty().addListener((obs, wasSelected, isSelected) -> {
                if (!ctx.isUpdatingFields()) {
                    List<String> checked = new ArrayList<>();
                    for (var node : checkboxPane.getChildren()) {
                        if (node instanceof javafx.scene.control.CheckBox box
                                && box.isSelected()) {
                            checked.add(box.getText());
                        }
                    }
                    onChanged.accept(checked);
                }
            });
            checkboxPane.getChildren().add(cb);
        }
        checkboxPane.setMaxWidth(Double.MAX_VALUE);
        GridPane.setHgrow(checkboxPane, Priority.ALWAYS);
        addFieldRow(startRow, "Subscripts", checkboxPane,
                "Dimensions this element is arrayed over");
        return startRow + 1;
    }

    public void addComboCommitHandlers(ComboBox<String> box, Consumer<ComboBox<String>> handler) {
        box.setOnAction(e -> {
            if (!ctx.isUpdatingFields()) {
                handler.accept(box);
            }
        });
        box.focusedProperty().addListener((obs, wasFocused, isFocused) -> {
            if (!isFocused && !ctx.isUpdatingFields()) {
                handler.accept(box);
            }
        });
    }

    public ComboBox<String> createUnitComboBox(String currentValue) {
        return createFilterableComboBox(COMMON_UNITS, currentValue);
    }

    public ComboBox<String> createTimeUnitComboBox(String currentValue) {
        return createFilterableComboBox(TIME_UNITS, currentValue);
    }

    private ComboBox<String> createFilterableComboBox(List<String> allItems, String currentValue) {
        ComboBox<String> box = new ComboBox<>(FXCollections.observableArrayList(allItems));
        box.setEditable(true);
        box.setValue(currentValue != null ? currentValue : "");
        box.setMaxWidth(Double.MAX_VALUE);
        GridPane.setHgrow(box, Priority.ALWAYS);

        box.getEditor().textProperty().addListener((obs, oldText, newText) -> {
            if (newText == null || newText.isEmpty()) {
                box.setItems(FXCollections.observableArrayList(allItems));
                return;
            }
            String lower = newText.toLowerCase();
            List<String> filtered = allItems.stream()
                    .filter(item -> item.toLowerCase().startsWith(lower))
                    .toList();
            box.setItems(FXCollections.observableArrayList(
                    filtered.isEmpty() ? allItems : filtered));
            if (!box.isShowing() && box.isFocused() && !filtered.isEmpty()) {
                box.show();
            }
        });

        return box;
    }

    /**
     * Briefly highlights a text field with a red border and tooltip to indicate
     * that the entered value was invalid and has been reverted. The border fades
     * after 2 seconds.
     */
    public void flashInvalidInput(TextField field) {
        String original = field.getStyle();
        field.setStyle(Styles.EQUATION_ERROR_BORDER);
        Tooltip tip = new Tooltip("Invalid number \u2014 reverted to previous value");
        tip.setShowDelay(Duration.ZERO);
        Tooltip.install(field, tip);
        PauseTransition fade = new PauseTransition(Duration.seconds(2));
        fade.setOnFinished(e -> {
            field.setStyle(original);
            Tooltip.uninstall(field, tip);
        });
        fade.play();
    }

    private void commitRename(TextField nameField) {
        String oldName = ctx.getElementName();
        String newName = nameField.getText().trim();
        if (newName.isEmpty() || newName.equals(oldName) || !ModelEditor.isValidName(newName)) {
            nameField.setText(oldName);
            return;
        }
        if (ctx.getEditor().hasElement(newName)) {
            nameField.setText(oldName);
            return;
        }
        ctx.getCanvas().elements().renameElement(oldName, newName);
        ctx.setElementName(newName);
    }
}
