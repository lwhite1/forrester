package systems.courant.forrester.ui;

import systems.courant.forrester.Simulation;
import systems.courant.forrester.measure.units.time.Times;

import javafx.application.Application;
import javafx.embed.swing.SwingFXUtils;
import javafx.event.ActionEvent;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.chart.XYChart.Series;
import javafx.scene.control.Alert;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.image.WritableImage;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * JavaFX Application that displays simulation results as a line chart.
 *
 * <p>Because {@link Application#launch} creates the instance internally, simulation data
 * must be passed via static state. The static methods are synchronized to allow safe
 * accumulation of data from the simulation thread before the FX thread reads it.
 */
public class ChartViewerApplication extends Application {

    private static final Object LOCK = new Object();

    private static List<Series<String, Number>> series = new ArrayList<>();
    private static double width = 800;
    private static double height = 600;
    private static DateTimeFormatter formatter = DateTimeFormatter.BASIC_ISO_DATE;
    private static String title = "";
    private static String xAxisLabel = "";

    private Scene scene;
    private LineChart<String, Number> lineChart;

    public static void setSimulation(Simulation simulation) {
        synchronized (LOCK) {
            title = simulation.getModel().getName();
            xAxisLabel = simulation.getTimeStep().getName();

            if (simulation.getDuration().isGreaterThan(Times.days(1))) {
                formatter = DateTimeFormatter.BASIC_ISO_DATE;
            } else {
                formatter = DateTimeFormatter.ISO_LOCAL_TIME;
            }
        }
    }

    @Override
    public void start(Stage stage) {
        List<Series<String, Number>> localSeries;
        String localTitle;
        String localXAxisLabel;
        synchronized (LOCK) {
            localSeries = new ArrayList<>(series);
            localTitle = title;
            localXAxisLabel = xAxisLabel;
        }

        stage.setTitle(localTitle);

        BorderPane root = new BorderPane();
        scene = new Scene(root, width, height);

        lineChart = createLineChart("", localXAxisLabel);
        lineChart.getData().addAll(localSeries);
        lineChart.setAnimated(false);
        addLineChartContextMenu(stage);

        ArrayList<CheckBox> checkBoxes = new ArrayList<>();
        for (Series<String, Number> s : localSeries) {
            CheckBox cb = new CheckBox(s.getName());
            cb.setOnAction(e -> refreshChart(e, localSeries));
            cb.setSelected(true);
            checkBoxes.add(cb);
        }

        Label dataLabel = new Label("Data:");
        VBox vbox = new VBox(8);
        vbox.getChildren().add(dataLabel);
        vbox.getChildren().addAll(checkBoxes);
        vbox.setPadding(new Insets(20, 10, 20, 0));

        root.setCenter(lineChart);
        root.setRight(vbox);

        stage.setScene(scene);
        stage.show();
    }

    /**
     * Initializes chart series from stock and variable names.
     */
    public static void addSeries(List<String> modelEntityNames, List<String> modelVariableNames) {
        synchronized (LOCK) {
            series = new ArrayList<>();
            List<String> allNames = new ArrayList<>(modelEntityNames);
            allNames.addAll(modelVariableNames);
            for (String name : allNames) {
                Series<String, Number> s = new Series<>();
                s.setName(name);
                series.add(s);
            }
        }
    }

    /**
     * Initializes chart series from flow names.
     */
    public static void addFlowSeries(List<String> flowNames) {
        synchronized (LOCK) {
            series = new ArrayList<>();
            for (String name : flowNames) {
                Series<String, Number> s = new Series<>();
                s.setName(name);
                series.add(s);
            }
        }
    }

    /**
     * Adds a data point to each series using a formatted timestamp as the x-axis label.
     */
    public static void addValues(List<Double> modelEntityValues,
                                 List<Double> variableValues,
                                 LocalDateTime currentTime) {
        synchronized (LOCK) {
            List<Double> allValues = new ArrayList<>(modelEntityValues);
            allValues.addAll(variableValues);

            for (int i = 0; i < allValues.size() && i < series.size(); i++) {
                double value = allValues.get(i);
                series.get(i).getData()
                        .add(new XYChart.Data<>(currentTime.format(formatter), value));
            }
        }
    }

    /**
     * Adds a data point to each series using the step number as the x-axis label.
     */
    public static void addValues(List<Double> modelEntityValues,
                                 List<Double> variableValues,
                                 int step) {
        synchronized (LOCK) {
            List<Double> allValues = new ArrayList<>(modelEntityValues);
            allValues.addAll(variableValues);

            for (int i = 0; i < allValues.size() && i < series.size(); i++) {
                double value = allValues.get(i);
                series.get(i).getData()
                        .add(new XYChart.Data<>(String.valueOf(step), value));
            }
        }
    }

    /**
     * Sets the scene dimensions for the chart window.
     */
    public static void setSize(double width, double height) {
        synchronized (LOCK) {
            ChartViewerApplication.width = width;
            ChartViewerApplication.height = height;
        }
    }

    private void refreshChart(ActionEvent event, List<Series<String, Number>> allSeries) {
        CheckBox cb = (CheckBox) event.getSource();
        String text = cb.getText();
        for (Series<String, Number> s : allSeries) {
            if (text.equals(s.getName())) {
                if (cb.isSelected()) {
                    lineChart.getData().add(s);
                } else {
                    lineChart.getData().remove(s);
                }
            }
        }
    }

    private LineChart<String, Number> createLineChart(String chartTitle, String xLabel) {
        CategoryAxis xAxis = new CategoryAxis();
        NumberAxis yAxis = new NumberAxis();
        xAxis.setLabel(xLabel);
        yAxis.setLabel("Value");
        LineChart<String, Number> chart = new LineChart<>(xAxis, yAxis);
        chart.setCreateSymbols(false);
        chart.setTitle(chartTitle);
        return chart;
    }

    private void addLineChartContextMenu(Stage stage) {
        final MenuItem saveAsFile = new MenuItem("Save as file");
        saveAsFile.setOnAction(event -> saveToFile(stage));

        final ContextMenu menu = new ContextMenu(saveAsFile);

        lineChart.setOnMouseClicked(event -> {
            if (MouseButton.SECONDARY.equals(event.getButton())) {
                menu.show(lineChart, event.getScreenX(), event.getScreenY());
            }
        });
    }

    private void saveToFile(Stage stage) {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Save Chart Image");
        chooser.setInitialFileName("chart.png");
        chooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("PNG Image", "*.png"));
        File outputFile = chooser.showSaveDialog(stage);
        if (outputFile == null) {
            return;
        }
        WritableImage image = scene.snapshot(null);
        BufferedImage bImage = SwingFXUtils.fromFXImage(image, null);
        try {
            ImageIO.write(bImage, "png", outputFile);
        } catch (IOException e) {
            Alert alert = new Alert(Alert.AlertType.ERROR,
                    "Failed to save chart image: " + e.getMessage());
            alert.setHeaderText("Save Error");
            alert.showAndWait();
        }
    }
}
