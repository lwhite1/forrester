package systems.courant.forrester.app.canvas;

import systems.courant.forrester.model.def.CausalLinkDef;
import systems.courant.forrester.model.def.ElementType;
import systems.courant.forrester.model.def.ModuleInstanceDef;
import systems.courant.forrester.model.def.SimulationSettings;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Separator;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * Right-side panel that displays properties of the currently selected canvas element.
 * Updates whenever the canvas selection changes. Contains a context toolbar with
 * action buttons and a form section with editable property fields.
 *
 * <p>Per-type form building is delegated to {@link ElementForm} implementations:
 * {@link StockForm}, {@link FlowForm}, {@link AuxForm}, {@link ConstantForm},
 * and {@link LookupForm}. Module forms are built inline (no commit handlers).</p>
 */
public class PropertiesPanel extends VBox {

    private static final double PREFERRED_WIDTH = 250;

    private final HBox contextToolbar = new HBox(4);
    private final Separator separator = new Separator();
    private final ScrollPane scrollPane = new ScrollPane();
    private final GridPane propertyGrid = new GridPane();
    private final Label placeholderLabel = new Label("No selection");
    private final FormContext ctx = new FormContext();

    private ElementForm currentForm;
    private ElementType cachedFormType;
    private Runnable onRunSimulation;
    private Runnable onValidateModel;
    private Runnable onOpenSettings;

    public PropertiesPanel() {
        setId("propertiesPanel");
        setPrefWidth(PREFERRED_WIDTH);
        setMinWidth(180);
        setPadding(new Insets(4));
        setSpacing(4);

        contextToolbar.setId("propertiesToolbar");
        contextToolbar.setPadding(new Insets(2));
        contextToolbar.setAlignment(Pos.CENTER_LEFT);

        propertyGrid.setHgap(8);
        propertyGrid.setVgap(6);
        propertyGrid.setPadding(new Insets(8));

        ColumnConstraints labelCol = new ColumnConstraints();
        labelCol.setMinWidth(70);
        labelCol.setHgrow(Priority.NEVER);
        ColumnConstraints fieldCol = new ColumnConstraints();
        fieldCol.setHgrow(Priority.ALWAYS);
        propertyGrid.getColumnConstraints().addAll(labelCol, fieldCol);

        scrollPane.setContent(propertyGrid);
        scrollPane.setFitToWidth(true);
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        VBox.setVgrow(scrollPane, Priority.ALWAYS);

        placeholderLabel.setId("propertiesPlaceholder");
        placeholderLabel.setStyle(Styles.PLACEHOLDER_TEXT);
        placeholderLabel.setPadding(new Insets(20));
        placeholderLabel.setAlignment(Pos.CENTER);
        placeholderLabel.setMaxWidth(Double.MAX_VALUE);

        ctx.grid = propertyGrid;
        ctx.onFormRebuildRequested = () -> updateSelection(ctx.canvas, ctx.editor);

        showPlaceholder();
    }

    /**
     * Sets a callback that opens the Expression Language help dialog.
     * Called by ModelWindow after construction.
     */
    public void setOnOpenExpressionHelp(Runnable callback) {
        ctx.onOpenExpressionHelp = callback;
    }

    /**
     * Sets callbacks for the quick-action buttons shown in the model summary.
     */
    public void setModelSummaryActions(Runnable runSimulation, Runnable validateModel,
                                       Runnable openSettings) {
        this.onRunSimulation = runSimulation;
        this.onValidateModel = validateModel;
        this.onOpenSettings = openSettings;
    }

    /**
     * Updates the panel to reflect the current selection on the canvas.
     * Called by ForresterApp whenever the canvas status changes.
     * Wrapped in updatingFields guard to prevent spurious focus-loss commits.
     */
    public void updateSelection(ModelCanvas canvas, ModelEditor editor) {
        ctx.canvas = canvas;
        ctx.editor = editor;
        ctx.updatingFields = true;

        try {
            if (canvas == null || editor == null) {
                showPlaceholder();
                return;
            }

            Set<String> selection = canvas.getSelectedElementNames();

            if (selection.isEmpty()) {
                ConnectionId conn = canvas.getSelectedConnection();
                if (conn != null) {
                    showConnectionProperties(conn);
                } else {
                    showModelSummary(editor);
                }
            } else if (selection.size() == 1) {
                String name = selection.iterator().next();
                ElementType type = canvas.getSelectedElementType(name);
                ctx.elementName = name;
                if (type == cachedFormType && currentForm != null && isCacheableType(type)) {
                    currentForm.updateValues();
                } else {
                    showSingleElement(name, type);
                }
            } else {
                showMultiSelection(selection.size());
            }
        } finally {
            ctx.updatingFields = false;
        }
    }

    private void showPlaceholder() {
        disposeCurrentForm();
        getChildren().clear();
        getChildren().add(placeholderLabel);
    }

    private void showModelSummary(ModelEditor editor) {
        disposeCurrentForm();
        getChildren().clear();

        propertyGrid.getChildren().clear();
        contextToolbar.getChildren().clear();
        int row = 0;

        // Model name (editable)
        TextField nameField = ctx.createTextField(
                editor.getModelName() != null ? editor.getModelName() : "Untitled");
        nameField.setId("modelNameField");
        ctx.addFieldRow(row++, "Model", nameField);
        nameField.setOnAction(e -> commitModelName(nameField, editor));
        nameField.focusedProperty().addListener((obs, wasFocused, isFocused) -> {
            if (!isFocused && !ctx.updatingFields) {
                commitModelName(nameField, editor);
            }
        });

        // Description (editable)
        TextArea descArea = new TextArea(
                editor.getModelComment() != null ? editor.getModelComment() : "");
        descArea.setId("modelDescField");
        descArea.setPrefRowCount(2);
        descArea.setWrapText(true);
        descArea.setMaxWidth(Double.MAX_VALUE);
        descArea.setPromptText("Model description...");
        GridPane.setHgrow(descArea, Priority.ALWAYS);
        ctx.addFieldRow(row++, "Description", descArea);
        descArea.focusedProperty().addListener((obs, wasFocused, isFocused) -> {
            if (!isFocused && !ctx.updatingFields) {
                commitModelComment(descArea, editor);
            }
        });

        // Separator before element counts
        propertyGrid.add(new Separator(), 0, row, 2, 1);
        row++;

        // Element counts
        String counts = buildElementCounts(editor);
        Label countsLabel = new Label(counts);
        countsLabel.setId("modelElementCounts");
        countsLabel.setWrapText(true);
        countsLabel.setStyle(Styles.SMALL_TEXT);
        propertyGrid.add(countsLabel, 0, row, 2, 1);
        row++;

        // Simulation settings summary
        SimulationSettings settings = editor.getSimulationSettings();
        if (settings != null) {
            String settingsText = String.format("Duration: %.0f %s, dt = 1 %s",
                    settings.duration(), settings.durationUnit(), settings.timeStep());
            ctx.addReadOnlyRow(row++, "Simulation", settingsText);
        }

        // Separator before quick actions
        propertyGrid.add(new Separator(), 0, row, 2, 1);
        row++;

        // Quick action buttons
        HBox actions = new HBox(6);
        actions.setAlignment(Pos.CENTER_LEFT);

        if (onRunSimulation != null) {
            Button runBtn = new Button("Run");
            runBtn.setId("summaryRunBtn");
            runBtn.setStyle(Styles.SMALL_TEXT);
            runBtn.setOnAction(e -> onRunSimulation.run());
            actions.getChildren().add(runBtn);
        }
        if (onValidateModel != null) {
            Button validateBtn = new Button("Validate");
            validateBtn.setId("summaryValidateBtn");
            validateBtn.setStyle(Styles.SMALL_TEXT);
            validateBtn.setOnAction(e -> onValidateModel.run());
            actions.getChildren().add(validateBtn);
        }
        if (onOpenSettings != null) {
            Button settingsBtn = new Button("Settings");
            settingsBtn.setId("summarySettingsBtn");
            settingsBtn.setStyle(Styles.SMALL_TEXT);
            settingsBtn.setOnAction(e -> onOpenSettings.run());
            actions.getChildren().add(settingsBtn);
        }

        if (!actions.getChildren().isEmpty()) {
            propertyGrid.add(actions, 0, row, 2, 1);
        }

        getChildren().addAll(scrollPane);
    }

    private void commitModelName(TextField nameField, ModelEditor editor) {
        String newName = nameField.getText().trim();
        if (!newName.isEmpty() && !newName.equals(editor.getModelName())) {
            editor.setModelName(newName);
        }
    }

    private void commitModelComment(TextArea descArea, ModelEditor editor) {
        String text = descArea.getText().trim();
        String comment = text.isEmpty() ? null : text;
        if (!Objects.equals(comment, editor.getModelComment())) {
            editor.setModelComment(comment != null ? comment : "");
        }
    }

    private static String buildElementCounts(ModelEditor editor) {
        List<String> parts = new ArrayList<>();
        int stocks = editor.getStocks().size();
        int flows = editor.getFlows().size();
        int auxes = editor.getAuxiliaries().size();
        int constants = editor.getConstants().size();
        int lookups = editor.getLookupTables().size();
        int modules = editor.getModules().size();

        if (stocks > 0) {
            parts.add(stocks + (stocks == 1 ? " stock" : " stocks"));
        }
        if (flows > 0) {
            parts.add(flows + (flows == 1 ? " flow" : " flows"));
        }
        if (auxes > 0) {
            parts.add(auxes + (auxes == 1 ? " auxiliary" : " auxiliaries"));
        }
        if (constants > 0) {
            parts.add(constants + (constants == 1 ? " constant" : " constants"));
        }
        if (lookups > 0) {
            parts.add(lookups + (lookups == 1 ? " lookup table" : " lookup tables"));
        }
        if (modules > 0) {
            parts.add(modules + (modules == 1 ? " module" : " modules"));
        }

        if (parts.isEmpty()) {
            return "No elements yet";
        }
        return String.join(", ", parts);
    }

    private void showMultiSelection(int count) {
        disposeCurrentForm();
        getChildren().clear();

        contextToolbar.getChildren().clear();
        Button deleteBtn = createToolbarButton("Delete");
        deleteBtn.setOnAction(e -> {
            if (ctx.canvas != null) {
                ctx.canvas.deleteSelectedElements();
                ctx.canvas.requestFocus();
            }
        });
        contextToolbar.getChildren().add(deleteBtn);

        propertyGrid.getChildren().clear();
        ctx.addReadOnlyRow(0, "Selection", count + " elements selected");

        getChildren().addAll(contextToolbar, separator, scrollPane);
    }

    private void showConnectionProperties(ConnectionId connection) {
        disposeCurrentForm();
        getChildren().clear();

        contextToolbar.getChildren().clear();

        propertyGrid.getChildren().clear();
        int row = 0;

        boolean isCausalLink = ctx.canvas != null && ctx.canvas.isSelectedConnectionCausalLink();

        ctx.addReadOnlyRow(row++, "Type", isCausalLink ? "Causal Link" : "Info Link");
        ctx.addReadOnlyRow(row++, "From", connection.from());
        ctx.addReadOnlyRow(row++, "To", connection.to());

        if (isCausalLink && ctx.editor != null) {
            CausalLinkDef link = findCausalLink(connection);
            if (link != null) {
                // Polarity combo box
                ComboBox<String> polarityBox = new ComboBox<>();
                polarityBox.setId("propPolarity");
                polarityBox.getItems().addAll("+ (positive)", "- (negative)", "? (unknown)");
                polarityBox.setValue(polarityDisplayText(link.polarity()));
                polarityBox.setMaxWidth(Double.MAX_VALUE);
                GridPane.setHgrow(polarityBox, Priority.ALWAYS);
                ctx.addFieldRow(row++, "Polarity", polarityBox,
                        "The direction of causal influence");

                // English explanation label
                Label explanation = new Label(buildExplanation(connection, link.polarity()));
                explanation.setId("propExplanation");
                explanation.setWrapText(true);
                explanation.setStyle(Styles.SMALL_TEXT + " -fx-text-fill: #555;");
                explanation.setMaxWidth(Double.MAX_VALUE);
                propertyGrid.add(explanation, 0, row, 2, 1);

                polarityBox.setOnAction(e -> {
                    if (!ctx.updatingFields) {
                        CausalLinkDef.Polarity newPolarity = polarityFromDisplay(polarityBox.getValue());
                        ctx.canvas.applyMutation(() ->
                                ctx.editor.setCausalLinkPolarity(
                                        connection.from(), connection.to(), newPolarity));
                        explanation.setText(buildExplanation(connection, newPolarity));
                    }
                });
            }
        }

        getChildren().addAll(contextToolbar, separator, scrollPane);
    }

    private CausalLinkDef findCausalLink(ConnectionId connection) {
        for (CausalLinkDef link : ctx.editor.getCausalLinks()) {
            if (link.from().equals(connection.from()) && link.to().equals(connection.to())) {
                return link;
            }
        }
        return null;
    }

    private static String polarityDisplayText(CausalLinkDef.Polarity polarity) {
        return switch (polarity) {
            case POSITIVE -> "+ (positive)";
            case NEGATIVE -> "- (negative)";
            case UNKNOWN -> "? (unknown)";
        };
    }

    private static CausalLinkDef.Polarity polarityFromDisplay(String display) {
        if (display == null) {
            return CausalLinkDef.Polarity.UNKNOWN;
        }
        if (display.startsWith("+")) {
            return CausalLinkDef.Polarity.POSITIVE;
        }
        if (display.startsWith("-")) {
            return CausalLinkDef.Polarity.NEGATIVE;
        }
        return CausalLinkDef.Polarity.UNKNOWN;
    }

    private static String buildExplanation(ConnectionId connection, CausalLinkDef.Polarity polarity) {
        String from = connection.from();
        String to = connection.to();
        return switch (polarity) {
            case POSITIVE -> String.format(
                    "When the value of %s increases, the value of %s tends to increase. " +
                    "When the value of %s decreases, the value of %s tends to decrease.",
                    from, to, from, to);
            case NEGATIVE -> String.format(
                    "When the value of %s increases, the value of %s tends to decrease. " +
                    "When the value of %s decreases, the value of %s tends to increase.",
                    from, to, from, to);
            case UNKNOWN -> String.format(
                    "The causal relationship between %s and %s has not been specified.",
                    from, to);
        };
    }

    private void showSingleElement(String name, ElementType type) {
        disposeCurrentForm();
        ctx.elementName = name;
        getChildren().clear();

        // Build context toolbar
        contextToolbar.getChildren().clear();

        Button renameBtn = createToolbarButton("Rename");
        renameBtn.setId("propertiesRename");
        renameBtn.setOnAction(e -> {
            for (javafx.scene.Node node : propertyGrid.getChildren()) {
                if (node instanceof TextField tf && "nameField".equals(tf.getId())) {
                    tf.requestFocus();
                    tf.selectAll();
                    return;
                }
            }
        });

        Button deleteBtn = createToolbarButton("Delete");
        deleteBtn.setId("propertiesDelete");
        deleteBtn.setOnAction(e -> {
            if (ctx.canvas != null) {
                ctx.canvas.deleteSelectedElements();
                ctx.canvas.requestFocus();
            }
        });

        contextToolbar.getChildren().addAll(renameBtn, deleteBtn);

        if (type == ElementType.MODULE) {
            Button drillBtn = createToolbarButton("Drill Into");
            drillBtn.setId("propertiesDrill");
            drillBtn.setOnAction(e -> {
                if (ctx.canvas != null) {
                    ctx.canvas.drillInto(ctx.elementName);
                    ctx.canvas.requestFocus();
                }
            });

            Button bindingsBtn = createToolbarButton("Bindings");
            bindingsBtn.setId("propertiesBindings");
            bindingsBtn.setOnAction(e -> {
                if (ctx.canvas != null) {
                    ctx.canvas.triggerBindingConfig(ctx.elementName);
                    ctx.canvas.requestFocus();
                }
            });

            contextToolbar.getChildren().addAll(drillBtn, bindingsBtn);
        }

        // Build property form
        propertyGrid.getChildren().clear();
        int row = 0;

        ctx.addReadOnlyRow(row++, "Type", formatType(type));

        if (type == null) {
            getChildren().addAll(contextToolbar, separator, scrollPane);
            return;
        }

        currentForm = createForm(type);
        if (currentForm != null) {
            row = currentForm.build(row);
            if (isCacheableType(type)) {
                cachedFormType = type;
            }
        } else if (type == ElementType.MODULE) {
            row = buildModuleForm(row);
        } else {
            ctx.addReadOnlyRow(row++, "Name", name);
        }

        getChildren().addAll(contextToolbar, separator, scrollPane);
    }

    private ElementForm createForm(ElementType type) {
        return switch (type) {
            case STOCK -> new StockForm(ctx);
            case FLOW -> new FlowForm(ctx);
            case AUX -> new AuxForm(ctx);
            case CONSTANT -> new ConstantForm(ctx);
            case LOOKUP -> new LookupForm(ctx);
            case CLD_VARIABLE -> new CldVariableForm(ctx);
            case MODULE -> null;
        };
    }

    private int buildModuleForm(int row) {
        Optional<ModuleInstanceDef> moduleOpt = ctx.editor.getModuleByName(ctx.elementName);
        if (moduleOpt.isEmpty()) {
            ctx.addReadOnlyRow(row++, "Name", ctx.elementName);
            return row;
        }
        ModuleInstanceDef module = moduleOpt.get();

        TextField nameField = ctx.createNameField();
        ctx.addFieldRow(row++, "Instance Name", nameField);

        Map<String, String> inputs = module.inputBindings();
        if (inputs.isEmpty()) {
            ctx.addReadOnlyRow(row++, "Inputs", "(none)");
        } else {
            boolean first = true;
            for (Map.Entry<String, String> entry : inputs.entrySet()) {
                ctx.addReadOnlyRow(row++, first ? "Inputs" : "",
                        entry.getKey() + " = " + entry.getValue());
                first = false;
            }
        }

        Map<String, String> outputs = module.outputBindings();
        if (outputs.isEmpty()) {
            ctx.addReadOnlyRow(row++, "Outputs", "(none)");
        } else {
            boolean first = true;
            for (Map.Entry<String, String> entry : outputs.entrySet()) {
                ctx.addReadOnlyRow(row++, first ? "Outputs" : "",
                        entry.getKey() + " -> " + entry.getValue());
                first = false;
            }
        }

        return row;
    }

    private void disposeCurrentForm() {
        if (currentForm != null) {
            currentForm.dispose();
            currentForm = null;
        }
        cachedFormType = null;
        ctx.elementName = null;
    }

    private static boolean isCacheableType(ElementType type) {
        return type == ElementType.STOCK || type == ElementType.FLOW
                || type == ElementType.AUX || type == ElementType.CONSTANT
                || type == ElementType.CLD_VARIABLE;
    }

    private static Button createToolbarButton(String text) {
        Button button = new Button(text);
        button.setStyle(Styles.SMALL_TEXT);
        return button;
    }

    private static String formatType(ElementType type) {
        if (type == null) {
            return "Unknown";
        }
        return switch (type) {
            case STOCK -> "Stock";
            case FLOW -> "Flow";
            case AUX -> "Auxiliary";
            case CONSTANT -> "Constant";
            case MODULE -> "Module";
            case LOOKUP -> "Lookup Table";
            case CLD_VARIABLE -> "CLD Variable";
        };
    }
}
