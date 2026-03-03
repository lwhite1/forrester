package com.deathrayresearch.forrester.app.canvas;

import com.deathrayresearch.forrester.model.def.LookupTableDef;

import javafx.geometry.Insets;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
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
        ctx.addFieldRow(row++, "Name", nameField);

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
                }
            }
        });
        ctx.addFieldRow(row++, "Interpolation", interpBox);

        // Data points summary
        double[] xs = lookup.xValues();
        double[] ys = lookup.yValues();
        ctx.addReadOnlyRow(row++, "Data Points", xs.length + " points");

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
        ctx.grid.add(buildChart(xs, ys), 0, row, 2, 1);
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

        return row;
    }

    @Override
    public void updateValues() {
        // Lookup forms are not cached — always rebuilt
    }

    private LineChart<Number, Number> buildChart(double[] xs, double[] ys) {
        NumberAxis xAxis = new NumberAxis();
        xAxis.setLabel("X");
        xAxis.setTickLabelFont(javafx.scene.text.Font.font(9));

        NumberAxis yAxis = new NumberAxis();
        yAxis.setLabel("Y");
        yAxis.setTickLabelFont(javafx.scene.text.Font.font(9));

        LineChart<Number, Number> chart = new LineChart<>(xAxis, yAxis);
        chart.setCreateSymbols(true);
        chart.setAnimated(false);
        chart.setLegendVisible(false);
        chart.setPrefHeight(160);
        chart.setMinHeight(120);
        chart.setPadding(Insets.EMPTY);

        XYChart.Series<Number, Number> series = new XYChart.Series<>();
        for (int i = 0; i < xs.length; i++) {
            series.getData().add(new XYChart.Data<>(xs[i], ys[i]));
        }
        chart.getData().add(series);

        chart.setStyle("-fx-padding: 0;");
        series.nodeProperty().addListener((obs, oldNode, newNode) -> {
            if (newNode != null) {
                newNode.setStyle("-fx-stroke: #1f77b4; -fx-stroke-width: 2px;");
            }
        });

        return chart;
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
