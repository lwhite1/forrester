package systems.courant.sd.app.canvas.charts;

import systems.courant.sd.sweep.MonteCarloResult;

import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.stage.Window;

import java.util.ArrayList;
import java.util.List;

import systems.courant.sd.app.canvas.ChartUtils;
import systems.courant.sd.app.canvas.ClipboardExporter;

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
        allNames.addAll(ChartUtils.filterSimulationSettings(result.getStockNames()));
        allNames.addAll(ChartUtils.filterSimulationSettings(result.getVariableNames()));

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

        MenuItem saveItem = ChartUtils.createPngMenuItem(this, "montecarlo_chart.png",
                this::getOwnerWindow);
        MenuItem exportCsv = ChartUtils.createCsvMenuItem("Export CSV (Percentiles)...",
                "montecarlo_percentiles.csv", this::getOwnerWindow,
                file -> {
                    if (currentVariable == null) {
                        return;
                    }
                    result.writePercentileCsv(file.getAbsolutePath(), currentVariable,
                            2.5, 25, 50, 75, 97.5);
                });
        MenuItem copyItem = new MenuItem("Copy to Clipboard (Percentiles)");
        copyItem.setOnAction(e -> ClipboardExporter.copyMonteCarloPercentiles(result, currentVariable));
        ChartUtils.attachContextMenu(this, saveItem, exportCsv, copyItem);
    }

    private void showVariable(String variableName) {
        if (fanChartPane == null) {
            fanChartPane = new FanChartPane(result, variableName);
            setCenter(fanChartPane);
        } else {
            fanChartPane.redraw(result, variableName);
        }
    }

    private Window getOwnerWindow() {
        return getScene() != null ? getScene().getWindow() : null;
    }
}
