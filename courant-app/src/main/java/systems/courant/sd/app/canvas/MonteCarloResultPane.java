package systems.courant.sd.app.canvas;

import systems.courant.sd.sweep.MonteCarloResult;

import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import java.util.ArrayList;
import java.util.List;

/**
 * Displays Monte Carlo results as a fan chart with a ComboBox for selecting
 * which stock or variable to visualize.
 */
public class MonteCarloResultPane extends BorderPane {

    private final MonteCarloResult result;
    private FanChartPane fanChartPane;
    private String currentVariable;

    public MonteCarloResultPane(MonteCarloResult result) {
        this.result = result;

        List<String> allNames = new ArrayList<>();
        allNames.addAll(result.getStockNames());
        allNames.addAll(result.getVariableNames());

        ComboBox<String> varCombo = new ComboBox<>(FXCollections.observableArrayList(allNames));

        HBox topBar = new HBox(8, new Label("Variable:"), varCombo);
        topBar.setPadding(new Insets(8));
        setTop(topBar);

        varCombo.valueProperty().addListener((obs, old, val) -> {
            if (val != null) {
                currentVariable = val;
                showVariable(val);
            }
        });

        if (!allNames.isEmpty()) {
            varCombo.setValue(allNames.getFirst());
        }

        ContextMenu contextMenu = new ContextMenu();
        MenuItem saveItem = new MenuItem("Save as PNG...");
        saveItem.setOnAction(e -> saveChartAsPng());
        MenuItem exportCsv = new MenuItem("Export CSV (Percentiles)...");
        exportCsv.setOnAction(e -> exportPercentileCsv());
        MenuItem copyItem = new MenuItem("Copy to Clipboard (Percentiles)");
        copyItem.setOnAction(e -> ClipboardExporter.copyMonteCarloPercentiles(result, currentVariable));
        contextMenu.getItems().addAll(saveItem, exportCsv, copyItem);
        setOnContextMenuRequested(e ->
                contextMenu.show(this, e.getScreenX(), e.getScreenY()));
    }

    private void showVariable(String variableName) {
        if (fanChartPane == null) {
            fanChartPane = new FanChartPane(result, variableName);
            setCenter(fanChartPane);
        } else {
            fanChartPane.redraw(result, variableName);
        }
    }

    private void saveChartAsPng() {
        ChartUtils.saveNodeAsPng(fanChartPane, "montecarlo_chart.png",
                getScene() != null ? getScene().getWindow() : null);
    }

    private void exportPercentileCsv() {
        if (currentVariable == null) {
            return;
        }
        ChartUtils.showCsvSaveDialog("Export Percentile CSV", "montecarlo_percentiles.csv",
                getScene() != null ? getScene().getWindow() : null,
                file -> result.writePercentileCsv(file.getAbsolutePath(), currentVariable,
                        2.5, 25, 50, 75, 97.5));
    }
}
