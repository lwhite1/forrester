package systems.courant.sd.app.canvas;

import systems.courant.sd.app.LastDirectoryStore;

import javafx.embed.swing.SwingFXUtils;
import javafx.scene.Node;
import javafx.scene.SnapshotParameters;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Alert;
import javafx.scene.image.WritableImage;
import javafx.stage.FileChooser;
import javafx.stage.Window;

import javax.imageio.ImageIO;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Locale;

/**
 * Shared chart utilities: color palette, series coloring, and number formatting.
 */
final class ChartUtils {

    private static final String[] SERIES_COLORS_ARRAY = {
        "#1f77b4", "#ff7f0e", "#2ca02c", "#d62728", "#9467bd",
        "#8c564b", "#e377c2", "#7f7f7f", "#bcbd22", "#17becf"
    };

    static final java.util.List<String> SERIES_COLORS = java.util.List.of(SERIES_COLORS_ARRAY);

    /** Muted colors for ghost run overlays, one per retained run (max 5). */
    static final java.util.List<String> GHOST_COLORS = java.util.List.of(
            "#4A90D9", "#2E7D32", "#E65100", "#7B1FA2", "#C62828"
    );

    /** Opacity applied to ghost run chart series. */
    static final double GHOST_OPACITY = 0.30;

    private ChartUtils() {
    }

    static void applySeriesColors(List<XYChart.Series<Number, Number>> allSeries) {
        for (int i = 0; i < allSeries.size(); i++) {
            String color = SERIES_COLORS.get(i % SERIES_COLORS.size());
            XYChart.Series<Number, Number> series = allSeries.get(i);
            series.nodeProperty().addListener((obs, oldNode, newNode) -> {
                if (newNode != null) {
                    newNode.setStyle("-fx-stroke: " + color + ";");
                }
            });
            if (series.getNode() != null) {
                series.getNode().setStyle("-fx-stroke: " + color + ";");
            }
        }
    }

    /**
     * Saves a snapshot of the given JavaFX node as a PNG image, prompting the user
     * with a file chooser dialog.
     *
     * @param node            the node to snapshot
     * @param defaultFilename default filename for the save dialog
     * @param owner           the owner window for the dialog (may be null)
     */
    static void saveNodeAsPng(Node node, String defaultFilename, Window owner) {
        if (node == null) {
            return;
        }
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Save Chart as PNG");
        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("PNG Image", "*.png"));
        fileChooser.setInitialFileName(defaultFilename);
        LastDirectoryStore.applyExportDirectory(fileChooser);

        File file = fileChooser.showSaveDialog(owner);
        if (file != null) {
            LastDirectoryStore.recordExportDirectory(file);
            WritableImage image = node.snapshot(new SnapshotParameters(), null);
            try {
                ImageIO.write(SwingFXUtils.fromFXImage(image, null), "png", file);
            } catch (IOException e) {
                new Alert(Alert.AlertType.ERROR,
                        "Failed to save image: " + e.getMessage()).showAndWait();
            }
        }
    }

    /**
     * Shows a CSV file save dialog and invokes the writer callback if a file is selected.
     * Handles {@link IOException} and {@link java.io.UncheckedIOException} with an error alert.
     *
     * @param title           dialog title
     * @param defaultFilename default filename
     * @param owner           the owner window (may be null)
     * @param writer          callback that receives the selected file path
     */
    static void showCsvSaveDialog(String title, String defaultFilename,
                                  Window owner, CsvFileWriter writer) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle(title);
        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("CSV Files", "*.csv"));
        fileChooser.setInitialFileName(defaultFilename);
        LastDirectoryStore.applyExportDirectory(fileChooser);

        File file = fileChooser.showSaveDialog(owner);
        if (file != null) {
            LastDirectoryStore.recordExportDirectory(file);
            try {
                writer.write(file);
            } catch (IOException | java.io.UncheckedIOException e) {
                new Alert(Alert.AlertType.ERROR,
                        "Failed to export CSV: " + e.getMessage()).showAndWait();
            }
        }
    }

    @FunctionalInterface
    interface CsvFileWriter {
        void write(File file) throws IOException;
    }

    static String formatNumber(double value) {
        if (value == Math.floor(value) && Double.isFinite(value)
                && Math.abs(value) <= Long.MAX_VALUE) {
            return String.valueOf((long) value);
        }
        return String.format(Locale.US, "%.4f", value);
    }
}
