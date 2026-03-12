package systems.courant.shrewd.app.canvas;

import com.opencsv.CSVWriter;

import systems.courant.shrewd.model.graph.FeedbackAnalysis;
import systems.courant.shrewd.model.graph.LoopDominanceAnalysis;

import javafx.embed.swing.SwingFXUtils;
import javafx.geometry.Insets;
import javafx.scene.SnapshotParameters;
import javafx.scene.chart.AreaChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Alert;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TitledPane;
import javafx.scene.image.WritableImage;
import javafx.beans.property.DoubleProperty;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.layout.Priority;
import javafx.stage.FileChooser;
import systems.courant.shrewd.app.LastDirectoryStore;

import javax.imageio.ImageIO;

import java.io.File;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;

/**
 * Displays a stacked area chart showing loop dominance over simulation time.
 * Each loop is a colored area; the tallest area at any time step represents
 * the dominant loop.
 */
final class LoopDominancePane extends VBox {

    private final LoopDominanceAnalysis dominance;
    private AreaChart<Number, Number> chart;
    private ChartTimeCursor timeCursor;

    LoopDominancePane(LoopDominanceAnalysis dominance) {
        this.dominance = dominance;
        NumberAxis xAxis = new NumberAxis();
        xAxis.setLabel("Step");
        xAxis.setAutoRanging(true);

        NumberAxis yAxis = new NumberAxis();
        yAxis.setLabel("Loop Activity");
        yAxis.setAutoRanging(true);

        chart = new AreaChart<>(xAxis, yAxis);
        chart.setTitle("Loop Dominance");
        chart.setAnimated(false);
        chart.setCreateSymbols(false);

        List<String> labels = dominance.loopLabels();
        for (int i = 0; i < dominance.loopCount(); i++) {
            XYChart.Series<Number, Number> series = new XYChart.Series<>();
            String type = dominance.loopTypes().get(i) != null
                    ? " (" + LoopNavigatorBar.formatType(dominance.loopTypes().get(i)) + ")"
                    : "";
            series.setName(labels.get(i) + type);

            // Sample every N steps if there are too many data points
            int totalSteps = dominance.stepCount();
            int sampleInterval = Math.max(1, totalSteps / 500);

            for (int step = 0; step < totalSteps; step += sampleInterval) {
                series.getData().add(new XYChart.Data<>(step, dominance.score(i, step)));
            }
            chart.getData().add(series);
        }

        // Apply loop colors
        applyLoopColors(chart, dominance);

        ChartTimeCursor[] cursorHolder = new ChartTimeCursor[1];
        StackPane chartWithCursor = ChartTimeCursor.install(chart, cursorHolder);
        timeCursor = cursorHolder[0];

        VBox.setVgrow(chartWithCursor, Priority.ALWAYS);

        ContextMenu contextMenu = new ContextMenu();
        MenuItem saveItem = new MenuItem("Save as PNG...");
        saveItem.setOnAction(e -> saveChartAsPng());
        MenuItem exportCsv = new MenuItem("Export CSV...");
        exportCsv.setOnAction(e -> exportCsv());
        contextMenu.getItems().addAll(saveItem, exportCsv);
        chart.setOnContextMenuRequested(e ->
                contextMenu.show(chart, e.getScreenX(), e.getScreenY()));

        Label helpText = new Label(
                "This chart shows which feedback loop is driving the most change at each "
                + "time step. The tallest area at any point indicates the dominant loop. "
                + "Activity is measured as the sum of absolute stock-value changes for "
                + "each loop between consecutive steps. A shift in dominance often signals "
                + "a transition in system behavior \u2014 e.g., from exponential growth "
                + "(reinforcing) to goal-seeking (balancing).");
        helpText.setWrapText(true);
        helpText.setStyle("-fx-font-size: 11px; -fx-text-fill: #555; -fx-padding: 4 8 4 8;");

        TitledPane helpPane = new TitledPane("How to read this chart", helpText);
        helpPane.setExpanded(false);
        helpPane.setAnimated(false);
        helpPane.setPadding(new Insets(4, 0, 0, 0));

        getChildren().addAll(chartWithCursor, helpPane);
    }

    /**
     * Returns the time cursor property for this chart, allowing synchronization
     * with other charts. Value is {@code Double.NaN} when no cursor is active.
     */
    DoubleProperty cursorTimeStepProperty() {
        return timeCursor != null ? timeCursor.cursorTimeStepProperty() : null;
    }

    private void saveChartAsPng() {
        if (chart == null) {
            return;
        }
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Save Chart as PNG");
        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("PNG Image", "*.png"));
        fileChooser.setInitialFileName("loop_dominance_chart.png");
        LastDirectoryStore.applyExportDirectory(fileChooser);

        File file = fileChooser.showSaveDialog(getScene() != null ? getScene().getWindow() : null);
        if (file != null) {
            LastDirectoryStore.recordExportDirectory(file);
            WritableImage image = chart.snapshot(new SnapshotParameters(), null);
            try {
                ImageIO.write(SwingFXUtils.fromFXImage(image, null), "png", file);
            } catch (IOException e) {
                new Alert(Alert.AlertType.ERROR,
                        "Failed to save image: " + e.getMessage()).showAndWait();
            }
        }
    }

    private void exportCsv() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Export Loop Dominance CSV");
        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("CSV Files", "*.csv"));
        fileChooser.setInitialFileName("loop_dominance.csv");
        LastDirectoryStore.applyExportDirectory(fileChooser);

        File file = fileChooser.showSaveDialog(getScene() != null ? getScene().getWindow() : null);
        if (file != null) {
            LastDirectoryStore.recordExportDirectory(file);
            try (CSVWriter writer = new CSVWriter(new OutputStreamWriter(
                    Files.newOutputStream(file.toPath()), StandardCharsets.UTF_8))) {
                List<String> header = new java.util.ArrayList<>();
                header.add("Step");
                header.addAll(dominance.loopLabels());
                writer.writeNext(header.toArray(new String[0]));

                for (int step = 0; step < dominance.stepCount(); step++) {
                    String[] row = new String[dominance.loopCount() + 1];
                    row[0] = String.valueOf(step);
                    for (int loop = 0; loop < dominance.loopCount(); loop++) {
                        row[loop + 1] = String.valueOf(dominance.score(loop, step));
                    }
                    writer.writeNext(row);
                }
            } catch (IOException e) {
                new Alert(Alert.AlertType.ERROR,
                        "Failed to export CSV: " + e.getMessage()).showAndWait();
            }
        }
    }

    private void applyLoopColors(AreaChart<Number, Number> chart,
                                 LoopDominanceAnalysis dominance) {
        String[] rColors = {"#27ae60", "#2ecc71", "#1abc9c", "#16a085"};
        String[] bColors = {"#2980b9", "#3498db", "#2c3e50", "#34495e"};
        String[] neutralColors = {"#7f8c8d", "#95a5a6", "#bdc3c7"};

        for (int i = 0; i < dominance.loopCount(); i++) {
            FeedbackAnalysis.LoopType type = dominance.loopTypes().get(i);
            String color;
            if (type == FeedbackAnalysis.LoopType.REINFORCING) {
                color = rColors[i % rColors.length];
            } else if (type == FeedbackAnalysis.LoopType.BALANCING) {
                color = bColors[i % bColors.length];
            } else {
                color = neutralColors[i % neutralColors.length];
            }

            if (i < chart.getData().size()) {
                XYChart.Series<Number, Number> series = chart.getData().get(i);
                String fillStyle = "-fx-fill: " + color + "40;";
                if (series.getNode() != null) {
                    series.getNode().setStyle(fillStyle);
                }
                series.nodeProperty().addListener((obs, oldNode, newNode) -> {
                    if (newNode != null) {
                        newNode.setStyle(fillStyle);
                    }
                });
                for (XYChart.Data<Number, Number> d : series.getData()) {
                    if (d.getNode() != null) {
                        d.getNode().setVisible(false);
                    }
                    d.nodeProperty().addListener((obs, oldNode, newNode) -> {
                        if (newNode != null) {
                            newNode.setVisible(false);
                        }
                    });
                }
            }
        }
    }
}
