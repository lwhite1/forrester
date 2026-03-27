package systems.courant.sd.app.canvas.dialogs;

import systems.courant.sd.model.def.ReferenceDataset;

import javafx.geometry.Insets;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.layout.GridPane;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Dialog for mapping CSV column names to model variable names. Each CSV column
 * is shown with a dropdown to select the corresponding model variable or
 * "(skip)" to exclude it from the imported dataset.
 */
public class ColumnMappingDialog extends ValidatingDialog<ReferenceDataset> {

    private static final String SKIP = "(skip)";

    private final ReferenceDataset dataset;
    private final Map<String, ComboBox<String>> mappings;

    /**
     * Creates a column mapping dialog.
     *
     * @param dataset       the raw dataset parsed from CSV
     * @param modelVariables the known model variable names available for mapping
     */
    public ColumnMappingDialog(ReferenceDataset dataset, List<String> modelVariables) {
        super("Map Reference Data Columns", "Map CSV columns to model variables");
        this.dataset = dataset;

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(8);
        grid.setPadding(new Insets(20, 20, 10, 10));

        grid.add(new Label("CSV Column"), 0, 0);
        grid.add(new Label("Model Variable"), 1, 0);

        List<String> csvColumns = dataset.variableNames();
        mappings = new LinkedHashMap<>();

        for (int i = 0; i < csvColumns.size(); i++) {
            String csvCol = csvColumns.get(i);
            Label csvLabel = new Label(csvCol);
            csvLabel.setId("csvCol_" + i);

            ComboBox<String> combo = new ComboBox<>();
            combo.setId("mapping_" + i);
            combo.getItems().add(SKIP);
            combo.getItems().addAll(modelVariables);

            // Auto-select if CSV column name matches a model variable (case-insensitive)
            String match = modelVariables.stream()
                    .filter(v -> v.equalsIgnoreCase(csvCol)
                            || v.replace(" ", "_").equalsIgnoreCase(csvCol)
                            || v.equalsIgnoreCase(csvCol.replace("_", " ")))
                    .findFirst()
                    .orElse(csvCol);
            if (modelVariables.contains(match)) {
                combo.setValue(match);
            } else {
                // Keep original CSV name if no match — user can still use it
                combo.getItems().add(csvCol);
                combo.setValue(csvCol);
            }

            grid.add(csvLabel, 0, i + 1);
            grid.add(combo, 1, i + 1);
            mappings.put(csvCol, combo);
        }

        setStandardContent(grid);
    }

    @Override
    protected ReferenceDataset buildResult() {
        return buildMappedDataset(dataset, mappings);
    }

    private static ReferenceDataset buildMappedDataset(ReferenceDataset original,
                                                       Map<String, ComboBox<String>> mappings) {
        Map<String, double[]> mappedColumns = new LinkedHashMap<>();
        for (Map.Entry<String, ComboBox<String>> entry : mappings.entrySet()) {
            String csvCol = entry.getKey();
            String targetName = entry.getValue().getValue();
            if (targetName == null || SKIP.equals(targetName)) {
                continue;
            }
            double[] data = original.columns().get(csvCol);
            if (data == null) {
                continue;
            }
            mappedColumns.put(targetName, data);
        }
        if (mappedColumns.isEmpty()) {
            return null;
        }
        return new ReferenceDataset(original.name(), original.timeValues(), mappedColumns);
    }
}
