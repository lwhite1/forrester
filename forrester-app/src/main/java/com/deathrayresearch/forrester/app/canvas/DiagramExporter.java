package com.deathrayresearch.forrester.app.canvas;

import com.deathrayresearch.forrester.model.def.ConnectorRoute;
import com.deathrayresearch.forrester.model.def.FlowDef;
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
 * Exports the diagram to a PNG or JPEG image file.
 * Renders to an offscreen canvas at 2x scale without selection/hover overlays.
 */
public final class DiagramExporter {

    private static final double EXPORT_SCALE = 2.0;
    private static final double PADDING = 50;

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

        // Compute world-space bounding box
        double minX = Double.MAX_VALUE;
        double minY = Double.MAX_VALUE;
        double maxX = -Double.MAX_VALUE;
        double maxY = -Double.MAX_VALUE;

        for (String name : canvasState.getDrawOrder()) {
            double cx = canvasState.getX(name);
            double cy = canvasState.getY(name);
            double halfW = LayoutMetrics.effectiveWidth(canvasState, name) / 2;
            double halfH = LayoutMetrics.effectiveHeight(canvasState, name) / 2;

            minX = Math.min(minX, cx - halfW);
            minY = Math.min(minY, cy - halfH);
            maxX = Math.max(maxX, cx + halfW);
            maxY = Math.max(maxY, cy + halfH);
        }

        // Include cloud positions in bounding box
        for (FlowDef flow : editor.getFlows()) {
            double[] sourceCloud = FlowEndpointCalculator.cloudPosition(
                    FlowEndpointCalculator.FlowEnd.SOURCE, flow, canvasState);
            if (sourceCloud != null) {
                minX = Math.min(minX, sourceCloud[0] - LayoutMetrics.CLOUD_OFFSET / 4);
                minY = Math.min(minY, sourceCloud[1] - LayoutMetrics.CLOUD_OFFSET / 4);
                maxX = Math.max(maxX, sourceCloud[0] + LayoutMetrics.CLOUD_OFFSET / 4);
                maxY = Math.max(maxY, sourceCloud[1] + LayoutMetrics.CLOUD_OFFSET / 4);
            }

            double[] sinkCloud = FlowEndpointCalculator.cloudPosition(
                    FlowEndpointCalculator.FlowEnd.SINK, flow, canvasState);
            if (sinkCloud != null) {
                minX = Math.min(minX, sinkCloud[0] - LayoutMetrics.CLOUD_OFFSET / 4);
                minY = Math.min(minY, sinkCloud[1] - LayoutMetrics.CLOUD_OFFSET / 4);
                maxX = Math.max(maxX, sinkCloud[0] + LayoutMetrics.CLOUD_OFFSET / 4);
                maxY = Math.max(maxY, sinkCloud[1] + LayoutMetrics.CLOUD_OFFSET / 4);
            }
        }

        // Add padding
        minX -= PADDING;
        minY -= PADDING;
        maxX += PADDING;
        maxY += PADDING;

        double worldWidth = maxX - minX;
        double worldHeight = maxY - minY;

        // Show file chooser
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Export Diagram");
        chooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("PNG Image (*.png)", "*.png"),
                new FileChooser.ExtensionFilter("JPEG Image (*.jpg, *.jpeg)", "*.jpg", "*.jpeg"));
        chooser.setInitialFileName("diagram.png");

        File file = chooser.showSaveDialog(ownerWindow);
        if (file == null) {
            return;
        }

        // Save and clear selection so it doesn't render
        Set<String> savedSelection = Set.copyOf(canvasState.getSelection());
        canvasState.clearSelection();

        try {
            // Create offscreen canvas at 2x scale
            double canvasWidth = worldWidth * EXPORT_SCALE;
            double canvasHeight = worldHeight * EXPORT_SCALE;
            Canvas offscreen = new Canvas(canvasWidth, canvasHeight);
            GraphicsContext gc = offscreen.getGraphicsContext2D();

            // Set up export viewport: translate so bounding-box top-left maps to origin, scale = 2x
            Viewport exportViewport = new Viewport();
            exportViewport.restoreState(
                    -minX * EXPORT_SCALE,
                    -minY * EXPORT_SCALE,
                    EXPORT_SCALE);

            // Render with idle interactive states
            CanvasRenderer exportRenderer = new CanvasRenderer(canvasState, exportViewport);
            exportRenderer.render(gc, canvasWidth, canvasHeight,
                    editor, connectors,
                    FlowCreationController.State.IDLE,
                    CanvasRenderer.ReattachState.IDLE,
                    CanvasRenderer.MarqueeState.IDLE,
                    loopAnalysis,
                    null,   // hoveredElement
                    null,   // hoveredConnection
                    null);  // selectedConnection

            // Snapshot to image
            SnapshotParameters params = new SnapshotParameters();
            WritableImage image = offscreen.snapshot(params, null);

            // Determine format from file extension
            String fileName = file.getName().toLowerCase();
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
