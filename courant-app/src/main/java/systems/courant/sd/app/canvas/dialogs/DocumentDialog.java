package systems.courant.sd.app.canvas.dialogs;

import systems.courant.sd.app.canvas.ModelEditor;
import systems.courant.sd.app.canvas.Styles;
import systems.courant.sd.model.def.FlowDef;
import systems.courant.sd.model.def.LookupTableDef;
import systems.courant.sd.model.def.ModuleInstanceDef;
import systems.courant.sd.model.def.StockDef;
import systems.courant.sd.model.def.VariableDef;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.function.Consumer;

/**
 * A non-modal dialog that displays all model elements in a filterable, sortable table.
 * Each row shows the element's name, type, and equation/value. Clicking a row selects
 * the corresponding element on the canvas.
 *
 * <p>Modeled after Vensim's "Document" tool — provides a structured, non-graphical
 * way to inspect model elements, review equations, and navigate to specific variables.
 *
 * <p>Only one document dialog may be open per window. Use {@link #showOrUpdate} to
 * create a new dialog or refresh and bring an existing one to the front.
 */
public class DocumentDialog extends Dialog<Void> {

    /** One dialog instance per owner stage. */
    private static final Map<Stage, DocumentDialog> OPEN_INSTANCES = new WeakHashMap<>();

    private static final String ALL_TYPES = "All Types";

    private final TableView<ElementRow> table;
    private final ObservableList<ElementRow> allRows;
    private final FilteredList<ElementRow> filteredRows;
    private final TextField searchField;
    private final ComboBox<String> typeFilter;
    private final Label summaryLabel;
    private Consumer<String> onSelectElement;

    /**
     * A single row in the document table.
     *
     * @param name     the element name
     * @param type     display label for the element type
     * @param equation the equation, value, or description
     */
    public record ElementRow(String name, String type, String equation) {}

    /**
     * Shows a document dialog for the given editor. If one is already open for the
     * given owner, refreshes its contents and brings it to the front.
     */
    public static void showOrUpdate(ModelEditor editor, Consumer<String> onSelectElement,
                                    Stage owner) {
        DocumentDialog existing = OPEN_INSTANCES.get(owner);
        if (existing != null && existing.isShowing()) {
            existing.refresh(editor);
            existing.onSelectElement = onSelectElement;
            Stage window = (Stage) existing.getDialogPane().getScene().getWindow();
            window.toFront();
            window.requestFocus();
            return;
        }
        DocumentDialog dialog = new DocumentDialog(editor, onSelectElement, owner);
        OPEN_INSTANCES.put(owner, dialog);
        dialog.show();
    }

    /**
     * Returns the currently open document dialog for the given owner,
     * or {@code null} if none is showing. Visible for testing.
     */
    public static DocumentDialog getOpenInstance(Stage owner) {
        DocumentDialog instance = OPEN_INSTANCES.get(owner);
        if (instance != null && !instance.isShowing()) {
            OPEN_INSTANCES.remove(owner);
            return null;
        }
        return instance;
    }

    public DocumentDialog(ModelEditor editor, Consumer<String> onSelectElement,
                          Stage owner) {
        initModality(Modality.NONE);
        setTitle("Document");
        this.onSelectElement = onSelectElement;

        // --- Search field ---
        searchField = new TextField();
        searchField.setId("documentSearch");
        searchField.setPromptText("Search by name\u2026");
        HBox.setHgrow(searchField, Priority.ALWAYS);

        // --- Type filter ---
        typeFilter = new ComboBox<>();
        typeFilter.setId("documentTypeFilter");
        typeFilter.getItems().addAll(ALL_TYPES, "Stock", "Flow", "Variable",
                "Constant", "Lookup Table", "Module");
        typeFilter.setValue(ALL_TYPES);

        HBox filterBar = new HBox(8, searchField, typeFilter);
        filterBar.setAlignment(Pos.CENTER_LEFT);
        filterBar.setPadding(new Insets(8, 8, 4, 8));

        // --- Table ---
        allRows = FXCollections.observableArrayList();
        filteredRows = new FilteredList<>(allRows, row -> true);
        SortedList<ElementRow> sortedRows = new SortedList<>(filteredRows);

        table = new TableView<>();
        table.setId("documentTable");
        table.setPlaceholder(new Label("No model elements."));

        TableColumn<ElementRow, String> nameCol = new TableColumn<>("Name");
        nameCol.setCellValueFactory(data ->
                new SimpleStringProperty(data.getValue().name()));
        nameCol.setPrefWidth(180);

        TableColumn<ElementRow, String> typeCol = new TableColumn<>("Type");
        typeCol.setCellValueFactory(data ->
                new SimpleStringProperty(data.getValue().type()));
        typeCol.setPrefWidth(100);

        TableColumn<ElementRow, String> equationCol = new TableColumn<>("Equation / Value");
        equationCol.setCellValueFactory(data ->
                new SimpleStringProperty(data.getValue().equation()));
        equationCol.setPrefWidth(360);

        table.getColumns().add(nameCol);
        table.getColumns().add(typeCol);
        table.getColumns().add(equationCol);

        sortedRows.comparatorProperty().bind(table.comparatorProperty());
        table.setItems(sortedRows);

        // Row click → select element on canvas
        table.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null && this.onSelectElement != null) {
                this.onSelectElement.accept(newVal.name());
            }
        });

        // --- Filter logic ---
        searchField.textProperty().addListener((obs, oldVal, newVal) -> applyFilter());
        typeFilter.valueProperty().addListener((obs, oldVal, newVal) -> applyFilter());

        // --- Summary ---
        summaryLabel = new Label();
        summaryLabel.setId("documentSummary");
        summaryLabel.setPadding(new Insets(6, 8, 6, 8));

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox bottomBar = new HBox(summaryLabel, spacer);
        bottomBar.setAlignment(Pos.CENTER_LEFT);
        bottomBar.setPadding(new Insets(4, 8, 4, 0));

        // --- Layout ---
        BorderPane root = new BorderPane();
        root.setTop(filterBar);
        root.setCenter(table);
        root.setBottom(bottomBar);

        getDialogPane().setContent(root);
        getDialogPane().setPrefWidth(Styles.screenAwareWidth(700));
        getDialogPane().setPrefHeight(Styles.screenAwareHeight(500));
        getDialogPane().getButtonTypes().add(ButtonType.CLOSE);

        setResultConverter(button -> null);
        setOnHidden(e -> OPEN_INSTANCES.values().remove(this));

        // Populate
        refresh(editor);
    }

    /**
     * Refreshes the table contents from the given editor.
     */
    public void refresh(ModelEditor editor) {
        allRows.setAll(buildRows(editor));
        updateSummary();
    }

    /**
     * Returns a snapshot of the rows currently visible (after filtering).
     * Visible for testing.
     */
    public List<ElementRow> getVisibleRows() {
        return List.copyOf(filteredRows);
    }

    /**
     * Returns the search text field. Visible for testing.
     */
    public TextField getSearchField() {
        return searchField;
    }

    /**
     * Returns the type filter combo box. Visible for testing.
     */
    public ComboBox<String> getTypeFilter() {
        return typeFilter;
    }

    /**
     * Returns the table view. Visible for testing.
     */
    public TableView<ElementRow> getTable() {
        return table;
    }

    private void applyFilter() {
        String search = searchField.getText();
        String searchLower = (search == null || search.isBlank())
                ? null : search.toLowerCase(Locale.ROOT);
        String selectedType = typeFilter.getValue();
        boolean filterByType = selectedType != null && !ALL_TYPES.equals(selectedType);

        filteredRows.setPredicate(row -> {
            if (searchLower != null
                    && !row.name().toLowerCase(Locale.ROOT).contains(searchLower)) {
                return false;
            }
            if (filterByType && !row.type().equals(selectedType)) {
                return false;
            }
            return true;
        });
        updateSummary();
    }

    private void updateSummary() {
        int showing = filteredRows.size();
        int total = allRows.size();
        if (showing == total) {
            summaryLabel.setText(total + " elements");
        } else {
            summaryLabel.setText(showing + " of " + total + " elements");
        }
    }

    static List<ElementRow> buildRows(ModelEditor editor) {
        List<ElementRow> rows = new ArrayList<>();

        for (StockDef s : editor.getStocks()) {
            String equation = s.initialExpression() != null
                    ? s.initialExpression()
                    : String.valueOf(s.initialValue());
            rows.add(new ElementRow(s.name(), "Stock", equation));
        }

        for (FlowDef f : editor.getFlows()) {
            rows.add(new ElementRow(f.name(), "Flow", f.equation()));
        }

        for (VariableDef v : editor.getVariables()) {
            String type = v.isLiteral() ? "Constant" : "Variable";
            rows.add(new ElementRow(v.name(), type, v.equation()));
        }

        for (LookupTableDef lt : editor.getLookupTables()) {
            String desc = "(" + lt.xValues().length + " points, " + lt.interpolation() + ")";
            rows.add(new ElementRow(lt.name(), "Lookup Table", desc));
        }

        for (ModuleInstanceDef m : editor.getModules()) {
            String desc = m.definition().name() != null ? m.definition().name() : "";
            rows.add(new ElementRow(m.instanceName(), "Module", desc));
        }

        rows.sort((a, b) -> a.name().compareToIgnoreCase(b.name()));
        return rows;
    }
}
