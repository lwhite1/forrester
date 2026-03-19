package systems.courant.sd.app.canvas;

import systems.courant.sd.app.LastDirectoryStore;
import systems.courant.sd.model.def.ConnectorRoute;
import systems.courant.sd.model.def.ModelDefinition;
import systems.courant.sd.model.graph.FeedbackAnalysis;

import javafx.scene.control.Alert;
import javafx.stage.FileChooser;
import javafx.stage.Window;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;

/**
 * Handles the UI flow for exporting model reports as HTML or PDF:
 * shows a file chooser, generates the report, and writes it to disk.
 */
public final class ReportExporter {

    private ReportExporter() {
    }

    /**
     * Exports a model report to an HTML file chosen by the user.
     *
     * @param canvasState  the canvas state for diagram rendering
     * @param editor       the model editor with current model data
     * @param connectors   connector routes for info links
     * @param loopAnalysis optional feedback analysis (null if unavailable)
     * @param ownerWindow  the owner window for the file chooser
     * @param modelName    the model name (used for default filename)
     */
    public static void exportReport(CanvasState canvasState, ModelEditor editor,
                                    List<ConnectorRoute> connectors,
                                    FeedbackAnalysis loopAnalysis,
                                    Window ownerWindow,
                                    String modelName) {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Export Report");
        chooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("HTML Report (*.html)", "*.html"),
                new FileChooser.ExtensionFilter("PDF Report (*.pdf)", "*.pdf"));
        String baseName = (modelName != null && !modelName.isBlank()
                && !"Untitled".equals(modelName))
                ? modelName.replaceAll("[^a-zA-Z0-9_\\-]", "_")
                : "report";
        chooser.setInitialFileName(baseName + "_report.html");
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

            String html = ReportGenerator.generate(definition, svgDiagram);

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
}
