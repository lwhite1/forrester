package com.deathrayresearch.forrester.app.canvas;

import com.deathrayresearch.forrester.app.LastDirectoryStore;
import com.deathrayresearch.forrester.model.def.ConnectorRoute;
import com.deathrayresearch.forrester.model.graph.FeedbackAnalysis;

import javafx.embed.swing.SwingFXUtils;
import javafx.scene.SnapshotParameters;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Alert;
import javafx.scene.image.WritableImage;
import javafx.stage.FileChooser;
import javafx.stage.Window;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Set;

import javax.imageio.ImageIO;

/**
 * Exports the diagram to a PNG, JPEG, or SVG file.
 * Raster formats are rendered to an offscreen canvas at 2x scale without selection/hover overlays.
 * SVG export delegates to {@link SvgExporter} for lossless vector output.
 */
public final class DiagramExporter {

    private static final double EXPORT_SCALE = 2.0;

    private DiagramExporter() {
    }

    /**
     * Exports the current diagram to an image file chosen by the user.
     *
     * @param canvasState  the canvas state containing element positions
     * @param editor       the model editor
     * @param connectors   the connector routes for info links
     * @param loopAnalysis optional feedback analysis (null if loop highlighting is off)
     * @param ownerWindow  the owner window for the file chooser dialog
     */
    public static void exportDiagram(CanvasState canvasState, ModelEditor editor,
                                     List<ConnectorRoute> connectors,
                                     FeedbackAnalysis loopAnalysis,
                                     Window ownerWindow) {
        if (canvasState.getDrawOrder().isEmpty()) {
            Alert info = new Alert(Alert.AlertType.INFORMATION);
            info.setTitle("Export Diagram");
            info.setHeaderText(null);
            info.setContentText("The diagram is empty. Add elements before exporting.");
            info.initOwner(ownerWindow);
            info.showAndWait();
            return;
        }

        ExportBounds.Bounds bounds = ExportBounds.compute(canvasState, editor);

        // Show file chooser
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Export Diagram");
        chooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("PNG Image (*.png)", "*.png"),
                new FileChooser.ExtensionFilter("JPEG Image (*.jpg, *.jpeg)", "*.jpg", "*.jpeg"),
                new FileChooser.ExtensionFilter("SVG Image (*.svg)", "*.svg"));
        chooser.setInitialFileName("diagram.png");
        LastDirectoryStore.applyExportDirectory(chooser);

        File file = chooser.showSaveDialog(ownerWindow);
        if (file == null) {
            return;
        }
        LastDirectoryStore.recordExportDirectory(file);

        // SVG export: delegate to SvgExporter (no selection clearing needed)
        String fileName = file.getName().toLowerCase();
        if (fileName.endsWith(".svg")) {
            try {
                SvgExporter.export(canvasState, editor, connectors, loopAnalysis, file);
            } catch (IOException e) {
                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setTitle("Export Error");
                alert.setHeaderText(null);
                alert.setContentText("Failed to export SVG: " + e.getMessage());
                alert.initOwner(ownerWindow);
                alert.showAndWait();
            }
            return;
        }

        // Save and clear selection so it doesn't render in raster export
        Set<String> savedSelection = Set.copyOf(canvasState.getSelection());
        canvasState.clearSelection();

        try {
            // Create offscreen canvas at 2x scale
            double canvasWidth = bounds.width() * EXPORT_SCALE;
            double canvasHeight = bounds.height() * EXPORT_SCALE;
            Canvas offscreen = new Canvas(canvasWidth, canvasHeight);
            GraphicsContext gc = offscreen.getGraphicsContext2D();

            // Set up export viewport: translate so bounding-box top-left maps to origin, scale = 2x
            Viewport exportViewport = new Viewport();
            exportViewport.restoreState(
                    -bounds.minX() * EXPORT_SCALE,
                    -bounds.minY() * EXPORT_SCALE,
                    EXPORT_SCALE);

            // Render with idle interactive states
            CanvasRenderer exportRenderer = new CanvasRenderer(canvasState, exportViewport);
            exportRenderer.render(gc, canvasWidth, canvasHeight,
                    editor, connectors,
                    FlowCreationController.State.IDLE,
                    CanvasRenderer.ReattachState.IDLE,
                    CanvasRenderer.RerouteState.IDLE,
                    CanvasRenderer.MarqueeState.IDLE,
                    loopAnalysis,
                    null,   // hoveredElement
                    null,   // hoveredConnection
                    null);  // selectedConnection

            // Snapshot to image
            SnapshotParameters params = new SnapshotParameters();
            WritableImage image = offscreen.snapshot(params, null);

            // Determine format from file extension
            String format;
            if (fileName.endsWith(".jpg") || fileName.endsWith(".jpeg")) {
                format = "jpg";
            } else {
                format = "png";
            }

            // Write image file
            ImageIO.write(SwingFXUtils.fromFXImage(image, null), format, file);
        } catch (IOException e) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Export Error");
            alert.setHeaderText(null);
            alert.setContentText("Failed to export diagram: " + e.getMessage());
            alert.initOwner(ownerWindow);
            alert.showAndWait();
        } finally {
            // Restore selection
            for (String name : savedSelection) {
                canvasState.addToSelection(name);
            }
        }
    }
}
