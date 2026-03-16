package systems.courant.sd.app.canvas;

import systems.courant.sd.model.def.CausalLinkDef;
import systems.courant.sd.model.def.ElementType;
import systems.courant.sd.model.def.ModuleInstanceDef;
import systems.courant.sd.model.def.SimulationSettings;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Separator;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.FlowPane;
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
import systems.courant.sd.app.canvas.forms.CldVariableForm;
import systems.courant.sd.app.canvas.forms.CommentForm;
import systems.courant.sd.app.canvas.forms.ElementForm;
import systems.courant.sd.app.canvas.forms.FlowForm;
import systems.courant.sd.app.canvas.forms.FormContext;
import systems.courant.sd.app.canvas.forms.LookupForm;
import systems.courant.sd.app.canvas.forms.StockForm;
import systems.courant.sd.app.canvas.forms.VariableForm;

/**
 * Right-side panel that displays properties of the currently selected canvas element.
 * Updates whenever the canvas selection changes. Contains a context toolbar with
 * action buttons and a form section with editable property fields.
 *
 * <p>Per-type form building is delegated to {@link ElementForm} implementations:
 * {@link StockForm}, {@link FlowForm}, {@link VariableForm},
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

        ctx.setGrid(propertyGrid);
        ctx.setOnFormRebuildRequested(() -> updateSelection(ctx.getCanvas(), ctx.getEditor()));

        showPlaceholder();
    }

    /**
     * Sets a callback that opens the Expression Language help dialog.
     * Called by ModelWindow after construction.
     */
    public void setOnOpenExpressionHelp(Runnable callback) {
        ctx.setOnOpenExpressionHelp(callback);
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
     * Called by CourantApp whenever the canvas status changes.
     * Wrapped in updatingFields guard to prevent spurious focus-loss commits.
     */
    public void updateSelection(ModelCanvas canvas, ModelEditor editor) {
        ctx.setCanvas(canvas);
        ctx.setEditor(editor);
        ctx.withUpdate(() -> {
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
                ctx.setElementName(name);
                if (type == cachedFormType && currentForm != null && isCacheableType(type)) {
                    currentForm.updateValues();
                } else {
                    showSingleElement(name, type);
                }
            } else {
                showMultiSelection(selection.size());
            }
        });
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
            if (!isFocused && !ctx.isUpdatingFields()) {
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
            if (!isFocused && !ctx.isUpdatingFields()) {
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
        int auxes = editor.getVariables().size();
        int lookups = editor.getLookupTables().size();
        int modules = editor.getModules().size();
        int commentCount = editor.getComments().size();

        if (stocks > 0) {
            parts.add(stocks + (stocks == 1 ? " stock" : " stocks"));
        }
        if (flows > 0) {
            parts.add(flows + (flows == 1 ? " flow" : " flows"));
        }
        if (auxes > 0) {
            parts.add(auxes + (auxes == 1 ? " variable" : " variables"));
        }
        if (lookups > 0) {
            parts.add(lookups + (lookups == 1 ? " lookup table" : " lookup tables"));
        }
        if (modules > 0) {
            parts.add(modules + (modules == 1 ? " module" : " modules"));
        }
        if (commentCount > 0) {
            parts.add(commentCount + (commentCount == 1 ? " comment" : " comments"));
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
            if (ctx.getCanvas() != null) {
                ctx.getCanvas().deleteSelectedElements();
                ctx.getCanvas().requestFocus();
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

        boolean isCausalLink = ctx.getCanvas() != null && ctx.getCanvas().isSelectedConnectionCausalLink();

        ctx.addReadOnlyRow(row++, "Type", isCausalLink ? "Causal Link" : "Info Link");
        ctx.addReadOnlyRow(row++, "From", connection.from());
        ctx.addReadOnlyRow(row++, "To", connection.to());

        if (isCausalLink && ctx.getEditor() != null) {
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
                    if (!ctx.isUpdatingFields()) {
                        CausalLinkDef.Polarity newPolarity = polarityFromDisplay(polarityBox.getValue());
                        ctx.getCanvas().applyMutation(() ->
                                ctx.getEditor().setCausalLinkPolarity(
                                        connection.from(), connection.to(), newPolarity));
                        explanation.setText(buildExplanation(connection, newPolarity));
                    }
                });
            }
        }

        getChildren().addAll(contextToolbar, separator, scrollPane);
    }

    private CausalLinkDef findCausalLink(ConnectionId connection) {
        for (CausalLinkDef link : ctx.getEditor().getCausalLinks()) {
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
        ctx.setElementName(name);
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
            if (ctx.getCanvas() != null) {
                ctx.getCanvas().deleteSelectedElements();
                ctx.getCanvas().requestFocus();
            }
        });

        contextToolbar.getChildren().addAll(renameBtn, deleteBtn);

        if (type == ElementType.MODULE) {
            Button drillBtn = createToolbarButton("Drill Into");
            drillBtn.setId("propertiesDrill");
            drillBtn.setOnAction(e -> {
                if (ctx.getCanvas() != null) {
                    ctx.getCanvas().drillInto(ctx.getElementName());
                    ctx.getCanvas().requestFocus();
                }
            });

            Button bindingsBtn = createToolbarButton("Bindings");
            bindingsBtn.setId("propertiesBindings");
            bindingsBtn.setOnAction(e -> {
                if (ctx.getCanvas() != null) {
                    ctx.getCanvas().triggerBindingConfig(ctx.getElementName());
                    ctx.getCanvas().requestFocus();
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

        if (type != ElementType.COMMENT && ctx.getCanvas() != null) {
            propertyGrid.add(new Separator(), 0, row, 2, 1);
            row++;
            row = buildDependencySection(row, name);
        }

        getChildren().addAll(contextToolbar, separator, scrollPane);
    }

    private ElementForm createForm(ElementType type) {
        return switch (type) {
            case STOCK -> new StockForm(ctx);
            case FLOW -> new FlowForm(ctx);
            case AUX -> new VariableForm(ctx);
            case LOOKUP -> new LookupForm(ctx);
            case CLD_VARIABLE -> new CldVariableForm(ctx);
            case COMMENT -> new CommentForm(ctx);
            case MODULE -> null;
        };
    }

    private int buildModuleForm(int row) {
        Optional<ModuleInstanceDef> moduleOpt = ctx.getEditor().getModuleByName(ctx.getElementName());
        if (moduleOpt.isEmpty()) {
            ctx.addReadOnlyRow(row++, "Name", ctx.getElementName());
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

    private int buildDependencySection(int row, String elementName) {
        Set<String> usedBy = ctx.getCanvas().whereUsed(elementName);
        Set<String> uses = ctx.getCanvas().uses(elementName);

        if (!usedBy.isEmpty()) {
            Label label = new Label("Used by");
            label.setStyle(Styles.FIELD_LABEL);
            FlowPane links = buildElementLinks(usedBy);
            propertyGrid.add(label, 0, row);
            propertyGrid.add(links, 1, row);
            row++;
        } else {
            ctx.addReadOnlyRow(row++, "Used by", "(none)");
        }

        if (!uses.isEmpty()) {
            Label label = new Label("Uses");
            label.setStyle(Styles.FIELD_LABEL);
            FlowPane links = buildElementLinks(uses);
            propertyGrid.add(label, 0, row);
            propertyGrid.add(links, 1, row);
            row++;
        } else {
            ctx.addReadOnlyRow(row++, "Uses", "(none)");
        }

        return row;
    }

    private FlowPane buildElementLinks(Set<String> elementNames) {
        FlowPane pane = new FlowPane(4, 2);
        pane.setPrefWrapLength(PREFERRED_WIDTH - 90);
        for (String name : elementNames) {
            Hyperlink link = new Hyperlink(name);
            link.setStyle(Styles.SMALL_TEXT + " -fx-text-fill: #0066cc;");
            link.setPadding(new Insets(0, 2, 0, 0));
            link.setOnAction(e -> {
                if (ctx.getCanvas() != null) {
                    ctx.getCanvas().selectElement(name);
                }
            });
            pane.getChildren().add(link);
        }
        return pane;
    }

    private void disposeCurrentForm() {
        if (currentForm != null) {
            currentForm.dispose();
            currentForm = null;
        }
        cachedFormType = null;
        ctx.setElementName(null);
    }

    private static boolean isCacheableType(ElementType type) {
        return type == ElementType.STOCK || type == ElementType.FLOW
                || type == ElementType.AUX
                || type == ElementType.CLD_VARIABLE
                || type == ElementType.COMMENT;
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
            case AUX -> "Variable";
            case MODULE -> "Module";
            case LOOKUP -> "Lookup Table";
            case CLD_VARIABLE -> "CLD Variable";
            case COMMENT -> "Comment";
        };
    }
}
