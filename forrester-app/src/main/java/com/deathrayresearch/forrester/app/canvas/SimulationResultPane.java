package com.deathrayresearch.forrester.app.canvas;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.embed.swing.SwingFXUtils;
import javafx.geometry.Insets;
import javafx.scene.SnapshotParameters;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Alert;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.imageio.ImageIO;

/**
 * Embeddable pane displaying simulation results as a chart and table.
 */
public class SimulationResultPane extends BorderPane {

    private LineChart<Number, Number> chart;

    public SimulationResultPane(SimulationRunner.SimulationResult result) {
        TabPane tabPane = new TabPane();
        tabPane.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);

        Tab tableTab = new Tab("Table", buildTable(result));
        Tab chartTab = new Tab("Chart", buildChartPane(result));
        tabPane.getTabs().addAll(tableTab, chartTab);

        setCenter(tabPane);
    }

    private TableView<double[]> buildTable(SimulationRunner.SimulationResult result) {
        TableView<double[]> table = new TableView<>();
        List<String> columns = result.columnNames();

        for (int c = 0; c < columns.size(); c++) {
            final int colIndex = c;
            TableColumn<double[], String> col = new TableColumn<>(columns.get(c));
            col.setCellValueFactory(data -> {
                double[] row = data.getValue();
                if (colIndex < row.length) {
                    double val = row[colIndex];
                    if (colIndex == 0) {
                        return new SimpleStringProperty(String.valueOf((int) val));
                    }
                    return new SimpleStringProperty(ChartUtils.formatNumber(val));
                }
                return new SimpleStringProperty("");
            });
            col.setPrefWidth(colIndex == 0 ? 60 : 120);
            table.getColumns().add(col);
        }

        table.setItems(FXCollections.observableArrayList(result.rows()));
        return table;
    }

    private BorderPane buildChartPane(SimulationRunner.SimulationResult result) {
        List<String> columns = result.columnNames();
        List<double[]> rows = result.rows();

        NumberAxis xAxis = new NumberAxis();
        xAxis.setLabel(columns.isEmpty() ? "Step" : columns.get(0));

        NumberAxis yAxis = new NumberAxis();
        yAxis.setLabel("Value");

        chart = new LineChart<>(xAxis, yAxis);
        chart.setCreateSymbols(false);
        chart.setAnimated(false);
        chart.setLegendVisible(false);

        List<XYChart.Series<Number, Number>> allSeries = new ArrayList<>();
        for (int c = 1; c < columns.size(); c++) {
            XYChart.Series<Number, Number> series = new XYChart.Series<>();
            series.setName(columns.get(c));
            for (double[] row : rows) {
                if (c < row.length) {
                    series.getData().add(new XYChart.Data<>(row[0], row[c]));
                }
            }
            allSeries.add(series);
        }

        chart.getData().addAll(allSeries);
        ChartUtils.applySeriesColors(allSeries);

        VBox sidebar = new VBox(6);
        sidebar.setPadding(new Insets(10));

        for (int i = 0; i < allSeries.size(); i++) {
            XYChart.Series<Number, Number> series = allSeries.get(i);
            String color = ChartUtils.SERIES_COLORS[i % ChartUtils.SERIES_COLORS.length];

            CheckBox cb = new CheckBox(series.getName());
            cb.setSelected(true);
            cb.setStyle("-fx-text-fill: " + color + ";");
            cb.selectedProperty().addListener((obs, wasSelected, isSelected) -> {
                if (series.getNode() != null) {
                    series.getNode().setVisible(isSelected);
                }
                series.getData().forEach(d -> {
                    if (d.getNode() != null) {
                        d.getNode().setVisible(isSelected);
                    }
                });
            });
            sidebar.getChildren().add(cb);
        }

        ScrollPane sidebarScroll = new ScrollPane(sidebar);
        sidebarScroll.setFitToWidth(true);
        sidebarScroll.setPrefWidth(180);

        ContextMenu contextMenu = new ContextMenu();
        MenuItem saveItem = new MenuItem("Save as PNG...");
        saveItem.setOnAction(e -> saveChartAsPng());
        contextMenu.getItems().add(saveItem);
        chart.setOnContextMenuRequested(e ->
                contextMenu.show(chart, e.getScreenX(), e.getScreenY()));

        BorderPane pane = new BorderPane();
        pane.setCenter(chart);
        pane.setRight(sidebarScroll);
        return pane;
    }

    private void saveChartAsPng() {
        if (chart == null) {
            return;
        }
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Save Chart as PNG");
        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("PNG Image", "*.png"));
        fileChooser.setInitialFileName("simulation_chart.png");

        File file = fileChooser.showSaveDialog(getScene() != null ? getScene().getWindow() : null);
        if (file != null) {
            WritableImage image = chart.snapshot(new SnapshotParameters(), null);
            try {
                ImageIO.write(SwingFXUtils.fromFXImage(image, null), "png", file);
            } catch (IOException e) {
                Alert alert = new Alert(Alert.AlertType.ERROR,
                        "Failed to save image: " + e.getMessage());
                alert.showAndWait();
            }
        }
    }

}
