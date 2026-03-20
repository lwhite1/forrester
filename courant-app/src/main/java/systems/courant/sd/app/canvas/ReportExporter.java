package systems.courant.sd.app.canvas;

import systems.courant.sd.app.LastDirectoryStore;
import systems.courant.sd.model.def.ConnectorRoute;
import systems.courant.sd.model.def.ModelDefinition;
import systems.courant.sd.model.graph.FeedbackAnalysis;
import systems.courant.sd.sweep.MonteCarloResult;
import systems.courant.sd.sweep.OptimizationResult;
import systems.courant.sd.sweep.RunResult;
import systems.courant.sd.sweep.SensitivitySummary.ParameterImpact;
import systems.courant.sd.sweep.SweepResult;
import systems.courant.sd.model.graph.LoopDominanceAnalysis;

import javafx.scene.control.Alert;
import javafx.stage.FileChooser;
import javafx.stage.Window;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Handles the UI flow for exporting model reports as HTML or PDF:
 * shows a file chooser, generates the report, and writes it to disk.
 * When dashboard results are available, the exported report includes
 * simulation results, Monte Carlo fan charts, sensitivity analysis, etc.
 */
public final class ReportExporter {

    private ReportExporter() {
    }

    /**
     * Exports a model report to an HTML file chosen by the user.
     * Includes simulation results from the dashboard if available.
     *
     * @param canvasState  the canvas state for diagram rendering
     * @param editor       the model editor with current model data
     * @param connectors   connector routes for info links
     * @param loopAnalysis optional feedback analysis (null if unavailable)
     * @param ownerWindow  the owner window for the file chooser
     * @param modelName    the model name (used for default filename)
     * @param dashboard    the dashboard panel with stored results (null if unavailable)
     */
    public static void exportReport(CanvasState canvasState, ModelEditor editor,
                                    List<ConnectorRoute> connectors,
                                    FeedbackAnalysis loopAnalysis,
                                    Window ownerWindow,
                                    String modelName,
                                    DashboardPanel dashboard) {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Export Report");
        chooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("HTML Report (*.html)", "*.html"),
                new FileChooser.ExtensionFilter("PDF Report (*.pdf)", "*.pdf"));
        String baseName = (modelName != null && !modelName.isBlank()
                && !"Untitled".equals(modelName))
                ? modelName.replaceAll("[^a-zA-Z0-9_\\-]", "_")
                : "report";
        chooser.setInitialFileName(baseName + "_report");
        LastDirectoryStore.applyExportDirectory(chooser);

        File file = chooser.showSaveDialog(ownerWindow);
        if (file == null) {
            return;
        }
        LastDirectoryStore.recordExportDirectory(file);

        try {
            ModelDefinition definition = editor.toModelDefinition();

            // Generate SVG diagram content (may be null if diagram is empty)
            String svgDiagram = SvgExporter.toSvgString(
                    canvasState, editor, connectors, loopAnalysis);

            // Build result sections from dashboard if results are available
            String extraCss = null;
            String extraSections = null;

            if (dashboard != null && dashboard.hasResults()) {
                RunResult singleRun = convertSimResult(dashboard.getLastSimResult());
                SweepResult sweep = dashboard.getLastSweepResult();
                MonteCarloResult monteCarlo = dashboard.getLastMonteCarloResult();
                List<ParameterImpact> sensitivity = dashboard.getLastSensitivityImpacts();
                OptimizationResult optimization = dashboard.getLastOptimizationResult();
                LoopDominanceAnalysis dominance = dashboard.getLastDominanceResult();

                String sections = ResultReportGenerator.generateSections(
                        singleRun, sweep, monteCarlo, sensitivity,
                        optimization, dominance);
                if (!sections.isBlank()) {
                    extraCss = ResultReportGenerator.getResultsCSS();
                    extraSections = sections;
                }
            }

            String html = ReportGenerator.generate(definition, svgDiagram,
                    extraCss, extraSections);

            if (file.getName().toLowerCase().endsWith(".pdf")) {
                PdfReportExporter.exportToPdf(html, file.toPath());
            } else {
                Files.writeString(file.toPath(), html, StandardCharsets.UTF_8);
            }
        } catch (IOException e) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Export Error");
            alert.setHeaderText(null);
            alert.setContentText("Failed to export report: " + e.getMessage());
            alert.initOwner(ownerWindow);
            alert.showAndWait();
        }
    }

    /**
     * Converts a UI-level {@link SimulationRunner.SimulationResult} to a
     * {@link RunResult} for report generation.
     *
     * @param simResult the simulation result (may be null)
     * @return a RunResult, or null if input is null
     */
    static RunResult convertSimResult(SimulationRunner.SimulationResult simResult) {
        if (simResult == null) {
            return null;
        }

        List<String> columns = simResult.columnNames();
        Set<String> stockNameSet = simResult.stockNames();

        // Separate stock and variable names (first column is "Step")
        List<String> stockNames = new ArrayList<>();
        List<String> variableNames = new ArrayList<>();
        List<Integer> stockIndices = new ArrayList<>();
        List<Integer> variableIndices = new ArrayList<>();

        for (int i = 1; i < columns.size(); i++) {
            String name = columns.get(i);
            if (stockNameSet.contains(name)) {
                stockNames.add(name);
                stockIndices.add(i);
            } else {
                variableNames.add(name);
                variableIndices.add(i);
            }
        }

        // Extract step values and snapshot arrays
        List<double[]> rows = simResult.rows();
        long[] steps = new long[rows.size()];
        List<double[]> stockSnapshots = new ArrayList<>(rows.size());
        List<double[]> variableSnapshots = new ArrayList<>(rows.size());

        for (int r = 0; r < rows.size(); r++) {
            double[] row = rows.get(r);
            steps[r] = (long) row[0];

            double[] stockVals = new double[stockIndices.size()];
            for (int s = 0; s < stockIndices.size(); s++) {
                stockVals[s] = row[stockIndices.get(s)];
            }
            stockSnapshots.add(stockVals);

            double[] varVals = new double[variableIndices.size()];
            for (int v = 0; v < variableIndices.size(); v++) {
                varVals[v] = row[variableIndices.get(v)];
            }
            variableSnapshots.add(varVals);
        }

        return RunResult.fromTimeSeries(stockNames, variableNames, steps,
                stockSnapshots, variableSnapshots);
    }
}
