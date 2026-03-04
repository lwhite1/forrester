package com.deathrayresearch.forrester.app.canvas;

import com.deathrayresearch.forrester.model.def.LookupTableDef;

import org.apache.commons.math3.analysis.interpolation.SplineInterpolator;

import javafx.geometry.Insets;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;

import java.util.Objects;

/**
 * Property form for lookup table elements. Builds editable fields for name,
 * interpolation mode, and an inline data point table with add/remove row buttons.
 */
class LookupForm implements ElementForm {

    private final FormContext ctx;
    private LineChart<Number, Number> chart;
    private int chartRow;

    LookupForm(FormContext ctx) {
        this.ctx = ctx;
    }

    @Override
    public int build(int startRow) {
        LookupTableDef lookup = ctx.editor.getLookupTableByName(ctx.elementName);
        if (lookup == null) {
            ctx.addReadOnlyRow(startRow++, "Name", ctx.elementName);
            return startRow;
        }

        int row = startRow;
        TextField nameField = ctx.createNameField();
        ctx.addFieldRow(row++, "Name", nameField,
                "The name used to reference this lookup table in equations.\n"
                + "Use LOOKUP(table_name, input_value) in equations.");

        // Interpolation dropdown
        ComboBox<String> interpBox = new ComboBox<>();
        interpBox.getItems().addAll("LINEAR", "SPLINE");
        interpBox.setValue(lookup.interpolation());
        interpBox.setMaxWidth(Double.MAX_VALUE);
        interpBox.setOnAction(e -> {
            if (!ctx.updatingFields) {
                String newInterp = interpBox.getValue();
                LookupTableDef lt = ctx.editor.getLookupTableByName(ctx.elementName);
                if (lt != null && !Objects.equals(newInterp, lt.interpolation())) {
                    ctx.canvas.applyLookupTable(ctx.elementName, new LookupTableDef(
                            ctx.elementName, lt.comment(),
                            lt.xValues(), lt.yValues(), newInterp));
                    replaceChart(lt.xValues(), lt.yValues(), newInterp);
                }
            }
        });
        ctx.addFieldRow(row++, "Interpolation", interpBox,
                "How values between data points are estimated.\n"
                + "LINEAR: straight lines between points.\n"
                + "SPLINE: smooth curves through points.");

        // Data points summary
        double[] xs = lookup.xValues();
        double[] ys = lookup.yValues();
        ctx.addReadOnlyRow(row++, "Data Points", xs.length + " points",
                "The x/y pairs defining the lookup function");

        // Editable table of x/y pairs
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
            TextField xField = new TextField(ElementRenderer.formatValue(xs[i]));
            TextField yField = new TextField(ElementRenderer.formatValue(ys[i]));
            xField.setPrefWidth(70);
            yField.setPrefWidth(70);

            final int index = i;
            Runnable commitRow = () -> commitDataPoint(xField, yField, index);
            xField.setOnAction(e -> commitRow.run());
            xField.focusedProperty().addListener((obs, wasFocused, isFocused) -> {
                if (!isFocused && !ctx.updatingFields) {
                    commitRow.run();
                }
            });
            yField.setOnAction(e -> commitRow.run());
            yField.focusedProperty().addListener((obs, wasFocused, isFocused) -> {
                if (!isFocused && !ctx.updatingFields) {
                    commitRow.run();
                }
            });

            tableGrid.add(xField, 0, i + 1);
            tableGrid.add(yField, 1, i + 1);
        }

        ctx.grid.add(tableGrid, 0, row, 2, 1);
        row++;

        // Inline chart preview
        chartRow = row;
        chart = buildChart(xs, ys, lookup.interpolation());
        ctx.grid.add(chart, 0, row, 2, 1);
        row++;

        // Add/remove row buttons
        HBox rowButtons = new HBox(4);
        Button addRowBtn = new Button("+ Row");
        addRowBtn.setStyle(Styles.SMALL_TEXT);
        addRowBtn.setOnAction(e -> addRow());

        Button removeRowBtn = new Button("- Row");
        removeRowBtn.setStyle(Styles.SMALL_TEXT);
        removeRowBtn.setOnAction(e -> removeRow());

        rowButtons.getChildren().addAll(addRowBtn, removeRowBtn);
        ctx.grid.add(rowButtons, 0, row, 2, 1);
        row++;

        TextArea commentArea = new TextArea(lookup.comment() != null ? lookup.comment() : "");
        commentArea.setId("propComment");
        commentArea.setPrefRowCount(2);
        commentArea.setWrapText(true);
        commentArea.setMaxWidth(Double.MAX_VALUE);
        GridPane.setHgrow(commentArea, javafx.scene.layout.Priority.ALWAYS);
        ctx.addTextAreaCommitHandlers(commentArea, this::commitComment);
        ctx.addFieldRow(row++, "Comment", commentArea,
                "Optional documentation for this element");

        return row;
    }

    @Override
    public void updateValues() {
        // Lookup forms are not cached — always rebuilt
    }

    private void replaceChart(double[] xs, double[] ys, String interpolation) {
        ctx.grid.getChildren().remove(chart);
        chart = buildChart(xs, ys, interpolation);
        ctx.grid.add(chart, 0, chartRow, 2, 1);
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
        newChart.setPrefHeight(160);
        newChart.setMinHeight(120);
        newChart.setPadding(Insets.EMPTY);
        newChart.setStyle("-fx-padding: 0;");

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

        return newChart;
    }

    private void commitComment(TextArea area) {
        String text = area.getText().trim();
        String comment = text.isEmpty() ? null : text;
        LookupTableDef lt = ctx.editor.getLookupTableByName(ctx.elementName);
        if (lt == null || Objects.equals(comment, lt.comment())) {
            return;
        }
        ctx.canvas.applyLookupComment(ctx.elementName, comment);
    }

    private void commitDataPoint(TextField xField, TextField yField, int index) {
        LookupTableDef lt = ctx.editor.getLookupTableByName(ctx.elementName);
        if (lt == null) {
            return;
        }
        try {
            double newX = Double.parseDouble(xField.getText().trim());
            double newY = Double.parseDouble(yField.getText().trim());
            double[] xs = lt.xValues();
            double[] ys = lt.yValues();
            if (index >= xs.length) {
                return;
            }
            if (xs[index] == newX && ys[index] == newY) {
                return;
            }
            xs[index] = newX;
            ys[index] = newY;
            // Validate: x values must be strictly increasing
            for (int i = 1; i < xs.length; i++) {
                if (xs[i] <= xs[i - 1]) {
                    xField.setText(ElementRenderer.formatValue(lt.xValues()[index]));
                    yField.setText(ElementRenderer.formatValue(lt.yValues()[index]));
                    return;
                }
            }
            ctx.canvas.applyLookupTable(ctx.elementName, new LookupTableDef(
                    ctx.elementName, lt.comment(), xs, ys, lt.interpolation()));
        } catch (NumberFormatException ignored) {
            xField.setText(ElementRenderer.formatValue(lt.xValues()[index]));
            yField.setText(ElementRenderer.formatValue(lt.yValues()[index]));
        }
    }

    private void addRow() {
        LookupTableDef lt = ctx.editor.getLookupTableByName(ctx.elementName);
        if (lt == null) {
            return;
        }
        double[] oldX = lt.xValues();
        double[] oldY = lt.yValues();
        double[] newX = new double[oldX.length + 1];
        double[] newY = new double[oldY.length + 1];
        System.arraycopy(oldX, 0, newX, 0, oldX.length);
        System.arraycopy(oldY, 0, newY, 0, oldY.length);
        newX[oldX.length] = oldX[oldX.length - 1] + 1;
        newY[oldY.length] = oldY[oldY.length - 1];
        ctx.canvas.applyLookupTable(ctx.elementName, new LookupTableDef(
                ctx.elementName, lt.comment(), newX, newY, lt.interpolation()));
    }

    private void removeRow() {
        LookupTableDef lt = ctx.editor.getLookupTableByName(ctx.elementName);
        if (lt == null || lt.xValues().length <= 2) {
            return;
        }
        double[] oldX = lt.xValues();
        double[] oldY = lt.yValues();
        double[] newX = new double[oldX.length - 1];
        double[] newY = new double[oldY.length - 1];
        System.arraycopy(oldX, 0, newX, 0, newX.length);
        System.arraycopy(oldY, 0, newY, 0, newY.length);
        ctx.canvas.applyLookupTable(ctx.elementName, new LookupTableDef(
                ctx.elementName, lt.comment(), newX, newY, lt.interpolation()));
    }
}
