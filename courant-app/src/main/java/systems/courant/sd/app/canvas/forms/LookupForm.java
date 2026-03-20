package systems.courant.sd.app.canvas.forms;

import systems.courant.sd.model.def.LookupTableDef;

import org.apache.commons.math3.analysis.interpolation.SplineInterpolator;

import javafx.geometry.Insets;
import javafx.geometry.Point2D;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;

import java.util.Arrays;
import java.util.Objects;
import java.util.Optional;
import systems.courant.sd.app.canvas.ChartUtils;
import systems.courant.sd.app.canvas.Styles;
import systems.courant.sd.app.canvas.renderers.ElementRenderer;

/**
 * Property form for lookup table elements. Builds editable fields for name,
 * interpolation mode, and an inline data point table with add/remove row buttons.
 *
 * <p>The inline chart is interactive: data points can be dragged to new positions,
 * clicking the chart adds a new point at that location, and right-clicking a
 * point deletes it (minimum 2 points enforced).
 */
public class LookupForm implements ElementForm {

    public static final int MIN_POINTS = 2;
    public static final String POINT_STYLE = "-fx-background-color: #1f77b4; "
            + "-fx-background-radius: 5; -fx-padding: 5;";
    public static final String POINT_DRAG_STYLE = "-fx-background-color: #ff7f0e; "
            + "-fx-background-radius: 5; -fx-padding: 5;";

    private final FormContext ctx;
    private final FormFieldBuilder fields;
    private LineChart<Number, Number> chart;
    private int chartRow;

    public LookupForm(FormContext ctx, FormFieldBuilder fields) {
        this.ctx = ctx;
        this.fields = fields;
    }

    @Override
    public int build(int startRow) {
        Optional<LookupTableDef> lookupOpt = ctx.getEditor().getLookupTableByName(ctx.getElementName());
        if (lookupOpt.isEmpty()) {
            fields.addReadOnlyRow(startRow++, "Name", ctx.getElementName());
            return startRow;
        }
        LookupTableDef lookup = lookupOpt.get();

        int row = startRow;
        row = buildHeaderFields(row, lookup);
        row = buildInterpolationDropdown(row, lookup);
        row = buildDataPointsTable(row, lookup);
        row = buildChartAndButtons(row, lookup);

        return row;
    }

    private int buildHeaderFields(int row, LookupTableDef lookup) {
        TextField nameField = fields.createNameField();
        fields.addFieldRow(row++, "Name", nameField,
                "The name used to reference this lookup table in equations.\n"
                + "Use LOOKUP(table_name, input_value) in equations.");

        fields.addCommentArea(row++, lookup.comment(), this::commitComment);

        ComboBox<String> unitBox = fields.createUnitComboBox(lookup.unit());
        fields.addComboCommitHandlers(unitBox, this::commitUnit);
        fields.addFieldRow(row++, "Unit", unitBox,
                "The unit of measurement for the lookup output");

        return row;
    }

    private int buildInterpolationDropdown(int row, LookupTableDef lookup) {
        ComboBox<String> interpBox = new ComboBox<>();
        interpBox.getItems().addAll("LINEAR", "SPLINE");
        interpBox.setValue(lookup.interpolation());
        interpBox.setMaxWidth(Double.MAX_VALUE);
        interpBox.setOnAction(e -> {
            if (!ctx.isUpdatingFields()) {
                String newInterp = interpBox.getValue();
                ctx.getEditor().getLookupTableByName(ctx.getElementName()).ifPresent(lt -> {
                    if (!Objects.equals(newInterp, lt.interpolation())) {
                        LookupTableDef updated = new LookupTableDef(
                                ctx.getElementName(), lt.comment(),
                                lt.xValues(), lt.yValues(), newInterp, lt.unit());
                        ctx.getCanvas().applyMutation(() -> ctx.getEditor().setLookupTable(ctx.getElementName(), updated));
                        replaceChart(lt.xValues(), lt.yValues(), newInterp);
                    }
                });
            }
        });
        fields.addFieldRow(row++, "Interpolation", interpBox,
                "How values between data points are estimated.\n"
                + "LINEAR: straight lines between points.\n"
                + "SPLINE: smooth curves through points.");
        return row;
    }

    private int buildDataPointsTable(int row, LookupTableDef lookup) {
        double[] xs = lookup.xValues();
        double[] ys = lookup.yValues();
        fields.addReadOnlyRow(row++, "Data Points", xs.length + " points",
                "The x/y pairs defining the lookup function");

        GridPane tableGrid = new GridPane();
        tableGrid.setHgap(4);
        tableGrid.setVgap(2);

        Label xHeader = new Label("X");
        xHeader.setStyle(Styles.FIELD_LABEL);
        Label yHeader = new Label("Y");
        yHeader.setStyle(Styles.FIELD_LABEL);
        tableGrid.add(xHeader, 0, 0);
        tableGrid.add(yHeader, 1, 0);

        for (int i = 0; i < xs.length; i++) {
            addDataPointRow(tableGrid, xs[i], ys[i], i);
        }

        ctx.getGrid().add(tableGrid, 0, row, 2, 1);
        row++;
        return row;
    }

    private void addDataPointRow(GridPane tableGrid, double xVal, double yVal, int index) {
        TextField xField = new TextField(ElementRenderer.formatValue(xVal));
        TextField yField = new TextField(ElementRenderer.formatValue(yVal));
        xField.setPrefWidth(70);
        yField.setPrefWidth(70);

        Runnable commitRow = () -> commitDataPoint(xField, yField, index);
        xField.setOnAction(e -> commitRow.run());
        xField.focusedProperty().addListener((obs, wasFocused, isFocused) -> {
            if (!isFocused && !ctx.isUpdatingFields()) {
                commitRow.run();
            }
        });
        yField.setOnAction(e -> commitRow.run());
        yField.focusedProperty().addListener((obs, wasFocused, isFocused) -> {
            if (!isFocused && !ctx.isUpdatingFields()) {
                commitRow.run();
            }
        });

        tableGrid.add(xField, 0, index + 1);
        tableGrid.add(yField, 1, index + 1);
    }

    private int buildChartAndButtons(int row, LookupTableDef lookup) {
        double[] xs = lookup.xValues();
        double[] ys = lookup.yValues();

        chartRow = row;
        chart = buildChart(xs, ys, lookup.interpolation());
        ctx.getGrid().add(chart, 0, row, 2, 1);
        row++;

        HBox rowButtons = new HBox(4);
        Button addRowBtn = new Button("+ Row");
        addRowBtn.setStyle(Styles.SMALL_TEXT);
        addRowBtn.setOnAction(e -> addRow());

        Button removeRowBtn = new Button("- Row");
        removeRowBtn.setStyle(Styles.SMALL_TEXT);
        removeRowBtn.setOnAction(e -> removeRow());

        rowButtons.getChildren().addAll(addRowBtn, removeRowBtn);
        ctx.getGrid().add(rowButtons, 0, row, 2, 1);
        row++;

        return row;
    }

    @Override
    public void updateValues() {
        // Lookup forms are not cached — always rebuilt
    }

    private void replaceChart(double[] xs, double[] ys, String interpolation) {
        ctx.getGrid().getChildren().remove(chart);
        chart = buildChart(xs, ys, interpolation);
        ctx.getGrid().add(chart, 0, chartRow, 2, 1);
    }

    private static final int SPLINE_INTERPOLATION_POINTS = 100;

    private LineChart<Number, Number> buildChart(double[] xs, double[] ys,
                                                  String interpolation) {
        NumberAxis xAxis = new NumberAxis();
        xAxis.setLabel("X");
        xAxis.setTickLabelFont(javafx.scene.text.Font.font(9));

        NumberAxis yAxis = new NumberAxis();
        yAxis.setLabel("Y");
        yAxis.setTickLabelFont(javafx.scene.text.Font.font(9));

        LineChart<Number, Number> newChart = new LineChart<>(xAxis, yAxis);
        newChart.setAnimated(false);
        newChart.setLegendVisible(false);
        newChart.setCreateSymbols(false);
        newChart.setPrefHeight(200);
        newChart.setMinHeight(160);
        newChart.setPadding(Insets.EMPTY);
        newChart.setStyle("-fx-padding: 0;");

        // Line/curve series (no symbols)
        XYChart.Series<Number, Number> lineSeries = new XYChart.Series<>();
        if ("SPLINE".equals(interpolation) && xs.length >= 3) {
            var function = new SplineInterpolator().interpolate(xs, ys);
            double xMin = xs[0];
            double xMax = xs[xs.length - 1];
            for (int i = 0; i <= SPLINE_INTERPOLATION_POINTS; i++) {
                double x = xMin + (xMax - xMin) * i / SPLINE_INTERPOLATION_POINTS;
                lineSeries.getData().add(new XYChart.Data<>(x, function.value(x)));
            }
        } else {
            for (int i = 0; i < xs.length; i++) {
                lineSeries.getData().add(new XYChart.Data<>(xs[i], ys[i]));
            }
        }
        newChart.getData().add(lineSeries);

        // Draggable data points series (symbols only, no line)
        XYChart.Series<Number, Number> pointsSeries = new XYChart.Series<>();
        for (int i = 0; i < xs.length; i++) {
            pointsSeries.getData().add(new XYChart.Data<>(xs[i], ys[i]));
        }
        newChart.getData().add(pointsSeries);

        // Style: hide line for points series, show symbols
        pointsSeries.nodeProperty().addListener((obs, oldNode, newNode) -> {
            if (newNode != null) {
                newNode.setStyle("-fx-stroke: transparent;");
            }
        });
        if (pointsSeries.getNode() != null) {
            pointsSeries.getNode().setStyle("-fx-stroke: transparent;");
        }

        // Make each data point draggable
        for (int i = 0; i < pointsSeries.getData().size(); i++) {
            XYChart.Data<Number, Number> dataPoint = pointsSeries.getData().get(i);
            final int pointIndex = i;
            dataPoint.nodeProperty().addListener((obs, oldNode, newNode) -> {
                if (newNode != null) {
                    setupDraggablePoint(newNode, dataPoint, pointIndex, newChart,
                            lineSeries, pointsSeries, interpolation);
                }
            });
            if (dataPoint.getNode() != null) {
                setupDraggablePoint(dataPoint.getNode(), dataPoint, pointIndex, newChart,
                        lineSeries, pointsSeries, interpolation);
            }
        }

        // Click on chart background to add a point
        installClickToAdd(newChart);

        return newChart;
    }

    private void setupDraggablePoint(Node node, XYChart.Data<Number, Number> dataPoint,
                                      int pointIndex, LineChart<Number, Number> chart,
                                      XYChart.Series<Number, Number> lineSeries,
                                      XYChart.Series<Number, Number> pointsSeries,
                                      String interpolation) {
        node.setStyle(POINT_STYLE);
        node.setCursor(Cursor.HAND);

        Tooltip tooltip = new Tooltip(formatPointTooltip(
                dataPoint.getXValue().doubleValue(), dataPoint.getYValue().doubleValue()));
        Tooltip.install(node, tooltip);

        node.setOnMousePressed(event -> {
            if (event.getButton() == MouseButton.PRIMARY) {
                node.setStyle(POINT_DRAG_STYLE);
                event.consume();
            }
        });

        node.setOnMouseDragged(event -> {
            if (event.getButton() != MouseButton.PRIMARY) {
                return;
            }
            Node plotArea = chart.lookup(".chart-plot-background");
            if (plotArea == null) {
                return;
            }
            Point2D plotLocal = plotArea.sceneToLocal(event.getSceneX(), event.getSceneY());
            NumberAxis xAxis = (NumberAxis) chart.getXAxis();
            NumberAxis yAxis = (NumberAxis) chart.getYAxis();
            double newX = xAxis.getValueForDisplay(plotLocal.getX()).doubleValue();
            double newY = yAxis.getValueForDisplay(plotLocal.getY()).doubleValue();

            // Constrain X between neighbors to maintain sorted order
            double[] bounds = getNeighborBounds(pointIndex);
            newX = Math.max(bounds[0], Math.min(bounds[1], newX));

            dataPoint.setXValue(newX);
            dataPoint.setYValue(newY);
            tooltip.setText(formatPointTooltip(newX, newY));

            // Update the line series in real time
            updateLineSeries(lineSeries, pointsSeries, interpolation);
            event.consume();
        });

        node.setOnMouseReleased(event -> {
            if (event.getButton() != MouseButton.PRIMARY) {
                return;
            }
            node.setStyle(POINT_STYLE);
            double finalX = dataPoint.getXValue().doubleValue();
            double finalY = dataPoint.getYValue().doubleValue();
            commitChartDrag(pointIndex, finalX, finalY);
            event.consume();
        });

        // Right-click to delete
        ContextMenu deleteMenu = new ContextMenu();
        MenuItem deleteItem = new MenuItem("Delete Point");
        deleteItem.setOnAction(e -> deletePointByIndex(pointIndex));
        deleteMenu.getItems().add(deleteItem);
        node.setOnContextMenuRequested(event -> {
            Optional<LookupTableDef> ltOpt = ctx.getEditor().getLookupTableByName(ctx.getElementName());
            if (ltOpt.isPresent() && ltOpt.get().xValues().length > MIN_POINTS) {
                deleteMenu.show(node, event.getScreenX(), event.getScreenY());
            }
            event.consume();
        });
    }

    private double[] getNeighborBounds(int pointIndex) {
        Optional<LookupTableDef> ltOpt = ctx.getEditor().getLookupTableByName(ctx.getElementName());
        if (ltOpt.isEmpty()) {
            return new double[]{Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY};
        }
        double[] xs = ltOpt.get().xValues();
        double lower = pointIndex > 0 ? xs[pointIndex - 1] + 1e-6 : Double.NEGATIVE_INFINITY;
        double upper = pointIndex < xs.length - 1 ? xs[pointIndex + 1] - 1e-6 : Double.POSITIVE_INFINITY;
        return new double[]{lower, upper};
    }

    private void updateLineSeries(XYChart.Series<Number, Number> lineSeries,
                                   XYChart.Series<Number, Number> pointsSeries,
                                   String interpolation) {
        int n = pointsSeries.getData().size();
        double[] xs = new double[n];
        double[] ys = new double[n];
        for (int i = 0; i < n; i++) {
            xs[i] = pointsSeries.getData().get(i).getXValue().doubleValue();
            ys[i] = pointsSeries.getData().get(i).getYValue().doubleValue();
        }

        lineSeries.getData().clear();
        if ("SPLINE".equals(interpolation) && n >= 3) {
            try {
                var function = new SplineInterpolator().interpolate(xs, ys);
                double xMin = xs[0];
                double xMax = xs[n - 1];
                for (int i = 0; i <= SPLINE_INTERPOLATION_POINTS; i++) {
                    double x = xMin + (xMax - xMin) * i / SPLINE_INTERPOLATION_POINTS;
                    lineSeries.getData().add(new XYChart.Data<>(x, function.value(x)));
                }
                return;
            } catch (IllegalArgumentException ignored) {
                // Fall back to linear if spline fails (e.g. near-duplicate x values)
                lineSeries.getData().clear();
            }
        }
        for (int i = 0; i < n; i++) {
            lineSeries.getData().add(new XYChart.Data<>(xs[i], ys[i]));
        }
    }

    private void installClickToAdd(LineChart<Number, Number> chart) {
        chart.setOnMouseClicked(event -> {
            if (event.getButton() != MouseButton.PRIMARY || event.getClickCount() != 2) {
                return;
            }
            Node plotArea = chart.lookup(".chart-plot-background");
            if (plotArea == null) {
                return;
            }
            Point2D plotLocal = plotArea.sceneToLocal(event.getSceneX(), event.getSceneY());
            if (plotLocal.getX() < 0 || plotLocal.getY() < 0) {
                return;
            }
            NumberAxis xAxis = (NumberAxis) chart.getXAxis();
            NumberAxis yAxis = (NumberAxis) chart.getYAxis();
            double newX = xAxis.getValueForDisplay(plotLocal.getX()).doubleValue();
            double newY = yAxis.getValueForDisplay(plotLocal.getY()).doubleValue();
            addPointAtPosition(newX, newY);
            event.consume();
        });
    }

    public void addPointAtPosition(double x, double y) {
        Optional<LookupTableDef> ltOpt = ctx.getEditor().getLookupTableByName(ctx.getElementName());
        if (ltOpt.isEmpty()) {
            return;
        }
        LookupTableDef lt = ltOpt.get();
        double[] oldX = lt.xValues();
        double[] oldY = lt.yValues();

        // Find insertion index to maintain sorted order
        int insertAt = Arrays.binarySearch(oldX, x);
        if (insertAt >= 0) {
            // Exact X already exists — don't add duplicate
            return;
        }
        insertAt = -(insertAt + 1);

        double[] newX = new double[oldX.length + 1];
        double[] newY = new double[oldY.length + 1];
        System.arraycopy(oldX, 0, newX, 0, insertAt);
        newX[insertAt] = x;
        System.arraycopy(oldX, insertAt, newX, insertAt + 1, oldX.length - insertAt);
        System.arraycopy(oldY, 0, newY, 0, insertAt);
        newY[insertAt] = y;
        System.arraycopy(oldY, insertAt, newY, insertAt + 1, oldY.length - insertAt);

        LookupTableDef updated = new LookupTableDef(
                ctx.getElementName(), lt.comment(), newX, newY, lt.interpolation(), lt.unit());
        ctx.getCanvas().applyMutation(() -> ctx.getEditor().setLookupTable(ctx.getElementName(), updated));
    }

    public void deletePointByIndex(int index) {
        Optional<LookupTableDef> ltOpt = ctx.getEditor().getLookupTableByName(ctx.getElementName());
        if (ltOpt.isEmpty() || ltOpt.get().xValues().length <= MIN_POINTS) {
            return;
        }
        LookupTableDef lt = ltOpt.get();
        double[] oldX = lt.xValues();
        double[] oldY = lt.yValues();
        if (index < 0 || index >= oldX.length) {
            return;
        }
        double[] newX = new double[oldX.length - 1];
        double[] newY = new double[oldY.length - 1];
        System.arraycopy(oldX, 0, newX, 0, index);
        System.arraycopy(oldX, index + 1, newX, index, oldX.length - index - 1);
        System.arraycopy(oldY, 0, newY, 0, index);
        System.arraycopy(oldY, index + 1, newY, index, oldY.length - index - 1);
        LookupTableDef updated = new LookupTableDef(
                ctx.getElementName(), lt.comment(), newX, newY, lt.interpolation(), lt.unit());
        ctx.getCanvas().applyMutation(() -> ctx.getEditor().setLookupTable(ctx.getElementName(), updated));
    }

    private void commitChartDrag(int index, double newX, double newY) {
        Optional<LookupTableDef> ltOpt = ctx.getEditor().getLookupTableByName(ctx.getElementName());
        if (ltOpt.isEmpty()) {
            return;
        }
        LookupTableDef lt = ltOpt.get();
        double[] xs = lt.xValues();
        double[] ys = lt.yValues();
        if (index >= xs.length) {
            return;
        }
        if (xs[index] == newX && ys[index] == newY) {
            return;
        }
        double[] newXs = xs.clone();
        double[] newYs = ys.clone();
        newXs[index] = newX;
        newYs[index] = newY;
        // Validate sorted order
        for (int i = 1; i < newXs.length; i++) {
            if (newXs[i] <= newXs[i - 1]) {
                return;
            }
        }
        LookupTableDef updated = new LookupTableDef(
                ctx.getElementName(), lt.comment(), newXs, newYs, lt.interpolation(), lt.unit());
        ctx.getCanvas().applyMutation(() -> ctx.getEditor().setLookupTable(ctx.getElementName(), updated));
    }

    public static String formatPointTooltip(double x, double y) {
        return "(" + ChartUtils.formatNumber(x) + ", " + ChartUtils.formatNumber(y) + ")";
    }

    private void commitComment(TextArea area) {
        String text = area.getText().trim();
        String comment = text.isEmpty() ? null : text;
        Optional<LookupTableDef> ltOpt = ctx.getEditor().getLookupTableByName(ctx.getElementName());
        if (ltOpt.isEmpty() || Objects.equals(comment, ltOpt.get().comment())) {
            return;
        }
        ctx.getCanvas().applyMutation(() -> ctx.getEditor().setLookupComment(ctx.getElementName(), comment));
    }

    private void commitDataPoint(TextField xField, TextField yField, int index) {
        Optional<LookupTableDef> ltOpt = ctx.getEditor().getLookupTableByName(ctx.getElementName());
        if (ltOpt.isEmpty()) {
            return;
        }
        LookupTableDef lt = ltOpt.get();
        double[] xs = lt.xValues();
        double[] ys = lt.yValues();
        if (index >= xs.length) {
            return;
        }
        double newX;
        boolean xValid = true;
        try {
            newX = Double.parseDouble(xField.getText().trim());
        } catch (NumberFormatException e) {
            newX = xs[index];
            xValid = false;
        }
        double newY;
        boolean yValid = true;
        try {
            newY = Double.parseDouble(yField.getText().trim());
        } catch (NumberFormatException e) {
            newY = ys[index];
            yValid = false;
        }
        if (!xValid || !yValid) {
            if (!xValid) {
                xField.setText(ElementRenderer.formatValue(xs[index]));
                fields.flashInvalidInput(xField);
            }
            if (!yValid) {
                yField.setText(ElementRenderer.formatValue(ys[index]));
                fields.flashInvalidInput(yField);
            }
            return;
        }
        if (xs[index] == newX && ys[index] == newY) {
            return;
        }
        // Work on copies so original arrays stay intact if validation fails
        double[] newXs = xs.clone();
        double[] newYs = ys.clone();
        newXs[index] = newX;
        newYs[index] = newY;
        // Validate: x values must be strictly increasing
        for (int i = 1; i < newXs.length; i++) {
            if (newXs[i] <= newXs[i - 1]) {
                xField.setText(ElementRenderer.formatValue(xs[index]));
                fields.flashInvalidInput(xField);
                return;
            }
        }
        LookupTableDef updated = new LookupTableDef(
                ctx.getElementName(), lt.comment(), newXs, newYs, lt.interpolation(), lt.unit());
        ctx.getCanvas().applyMutation(() -> ctx.getEditor().setLookupTable(ctx.getElementName(), updated));
    }

    private void addRow() {
        Optional<LookupTableDef> ltOpt = ctx.getEditor().getLookupTableByName(ctx.getElementName());
        if (ltOpt.isEmpty()) {
            return;
        }
        LookupTableDef lt = ltOpt.get();
        double[] oldX = lt.xValues();
        double[] oldY = lt.yValues();
        if (oldX.length == 0) {
            return;
        }
        double[] newX = new double[oldX.length + 1];
        double[] newY = new double[oldY.length + 1];
        System.arraycopy(oldX, 0, newX, 0, oldX.length);
        System.arraycopy(oldY, 0, newY, 0, oldY.length);
        newX[oldX.length] = oldX[oldX.length - 1] + 1;
        newY[oldY.length] = oldY[oldY.length - 1];
        LookupTableDef updated = new LookupTableDef(
                ctx.getElementName(), lt.comment(), newX, newY, lt.interpolation(), lt.unit());
        ctx.getCanvas().applyMutation(() -> ctx.getEditor().setLookupTable(ctx.getElementName(), updated));
    }

    private void removeRow() {
        Optional<LookupTableDef> ltOpt = ctx.getEditor().getLookupTableByName(ctx.getElementName());
        if (ltOpt.isEmpty() || ltOpt.get().xValues().length <= MIN_POINTS) {
            return;
        }
        LookupTableDef lt = ltOpt.get();
        double[] oldX = lt.xValues();
        double[] oldY = lt.yValues();
        double[] newX = new double[oldX.length - 1];
        double[] newY = new double[oldY.length - 1];
        System.arraycopy(oldX, 0, newX, 0, newX.length);
        System.arraycopy(oldY, 0, newY, 0, newY.length);
        LookupTableDef updated = new LookupTableDef(
                ctx.getElementName(), lt.comment(), newX, newY, lt.interpolation(), lt.unit());
        ctx.getCanvas().applyMutation(() -> ctx.getEditor().setLookupTable(ctx.getElementName(), updated));
    }

    private void commitUnit(ComboBox<String> box) {
        String unit = box.getValue() != null ? box.getValue().trim() : "";
        String normalizedUnit = unit.isEmpty() ? null : unit;
        Optional<LookupTableDef> ltOpt = ctx.getEditor().getLookupTableByName(ctx.getElementName());
        if (ltOpt.isPresent() && Objects.equals(normalizedUnit, ltOpt.get().unit())) {
            return;
        }
        ctx.getCanvas().applyMutation(() -> ctx.getEditor().setLookupUnit(ctx.getElementName(), normalizedUnit));
    }
}
