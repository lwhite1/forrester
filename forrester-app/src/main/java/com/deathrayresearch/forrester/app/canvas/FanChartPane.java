package com.deathrayresearch.forrester.app.canvas;

import com.deathrayresearch.forrester.sweep.MonteCarloResult;

import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;

/**
 * Embeddable pane that renders a fan chart (percentile bands) from a
 * {@link MonteCarloResult}. Adapted from the standalone {@code FanChart}
 * viewer in forrester-ui.
 */
public class FanChartPane extends Pane {

    private static final double MARGIN_LEFT = 70;
    private static final double MARGIN_RIGHT = 30;
    private static final double MARGIN_TOP = 40;
    private static final double MARGIN_BOTTOM = 50;

    private static final Color BASE_COLOR = Color.STEELBLUE;

    private static final double[][] BANDS = {
            {2.5, 97.5, 0.15},
            {12.5, 87.5, 0.25},
            {25.0, 75.0, 0.35},
    };

    private final Canvas canvas;

    public FanChartPane(MonteCarloResult result, String variableName) {
        canvas = new Canvas(800, 500);
        getChildren().add(canvas);

        // Redraw when resized
        widthProperty().addListener((obs, old, val) -> {
            canvas.setWidth(val.doubleValue());
            drawFanChart(canvas.getGraphicsContext2D(), result, variableName);
        });
        heightProperty().addListener((obs, old, val) -> {
            canvas.setHeight(val.doubleValue());
            drawFanChart(canvas.getGraphicsContext2D(), result, variableName);
        });

        drawFanChart(canvas.getGraphicsContext2D(), result, variableName);
    }

    /**
     * Redraws the fan chart for a different variable.
     */
    public void redraw(MonteCarloResult result, String variableName) {
        drawFanChart(canvas.getGraphicsContext2D(), result, variableName);
    }

    private void drawFanChart(GraphicsContext gc, MonteCarloResult result, String variableName) {
        double w = canvas.getWidth();
        double h = canvas.getHeight();

        gc.clearRect(0, 0, w, h);
        gc.setFill(Color.WHITE);
        gc.fillRect(0, 0, w, h);

        int stepCount = result.getStepCount();
        if (stepCount <= 1) {
            gc.setFill(Color.BLACK);
            gc.setFont(Font.font(14));
            gc.fillText("Not enough data points.", MARGIN_LEFT, h / 2);
            return;
        }

        double[] p2_5 = result.getPercentileSeries(variableName, 2.5);
        double[] p97_5 = result.getPercentileSeries(variableName, 97.5);
        double[] p12_5 = result.getPercentileSeries(variableName, 12.5);
        double[] p87_5 = result.getPercentileSeries(variableName, 87.5);
        double[] p25 = result.getPercentileSeries(variableName, 25);
        double[] p75 = result.getPercentileSeries(variableName, 75);
        double[] median = result.getPercentileSeries(variableName, 50);

        double[][] lowerSeries = {p2_5, p12_5, p25};
        double[][] upperSeries = {p97_5, p87_5, p75};

        double minVal = Double.MAX_VALUE;
        double maxVal = -Double.MAX_VALUE;
        for (int i = 0; i < stepCount; i++) {
            minVal = Math.min(minVal, p2_5[i]);
            maxVal = Math.max(maxVal, p97_5[i]);
        }
        double range = maxVal - minVal;
        if (range == 0) {
            range = 1;
        }
        minVal -= range * 0.05;
        maxVal += range * 0.05;

        double plotWidth = w - MARGIN_LEFT - MARGIN_RIGHT;
        double plotHeight = h - MARGIN_TOP - MARGIN_BOTTOM;

        // Draw bands
        for (int b = 0; b < BANDS.length; b++) {
            double opacity = BANDS[b][2];
            gc.setFill(Color.color(BASE_COLOR.getRed(), BASE_COLOR.getGreen(),
                    BASE_COLOR.getBlue(), opacity));

            double[] xPoints = new double[stepCount * 2];
            double[] yPoints = new double[stepCount * 2];

            for (int i = 0; i < stepCount; i++) {
                xPoints[i] = MARGIN_LEFT + (i * plotWidth / (stepCount - 1));
                yPoints[i] = MARGIN_TOP + plotHeight
                        - ((upperSeries[b][i] - minVal) / (maxVal - minVal) * plotHeight);
            }
            for (int i = 0; i < stepCount; i++) {
                xPoints[stepCount + i] = MARGIN_LEFT
                        + ((stepCount - 1 - i) * plotWidth / (stepCount - 1));
                yPoints[stepCount + i] = MARGIN_TOP + plotHeight
                        - ((lowerSeries[b][stepCount - 1 - i] - minVal)
                        / (maxVal - minVal) * plotHeight);
            }

            gc.fillPolygon(xPoints, yPoints, stepCount * 2);
        }

        // Median line
        gc.setStroke(Color.color(BASE_COLOR.getRed(), BASE_COLOR.getGreen(),
                BASE_COLOR.getBlue(), 0.9));
        gc.setLineWidth(2);
        gc.beginPath();
        for (int i = 0; i < stepCount; i++) {
            double x = MARGIN_LEFT + (i * plotWidth / (stepCount - 1));
            double y = MARGIN_TOP + plotHeight
                    - ((median[i] - minVal) / (maxVal - minVal) * plotHeight);
            if (i == 0) {
                gc.moveTo(x, y);
            } else {
                gc.lineTo(x, y);
            }
        }
        gc.stroke();

        // Axes
        gc.setStroke(Color.BLACK);
        gc.setLineWidth(1);
        gc.strokeLine(MARGIN_LEFT, MARGIN_TOP, MARGIN_LEFT, MARGIN_TOP + plotHeight);
        gc.strokeLine(MARGIN_LEFT, MARGIN_TOP + plotHeight,
                MARGIN_LEFT + plotWidth, MARGIN_TOP + plotHeight);

        // Axis labels
        gc.setFill(Color.BLACK);
        gc.setFont(Font.font(12));

        int yTicks = 5;
        for (int i = 0; i <= yTicks; i++) {
            double val = minVal + (maxVal - minVal) * i / yTicks;
            double y = MARGIN_TOP + plotHeight - (plotHeight * i / yTicks);
            gc.strokeLine(MARGIN_LEFT - 5, y, MARGIN_LEFT, y);
            gc.fillText(String.format("%.0f", val), 5, y + 4);
        }

        int xTicks = Math.min(10, stepCount - 1);
        for (int i = 0; i <= xTicks; i++) {
            int step = (int) Math.round((double) i * (stepCount - 1) / xTicks);
            double x = MARGIN_LEFT + (step * plotWidth / (stepCount - 1));
            gc.strokeLine(x, MARGIN_TOP + plotHeight, x, MARGIN_TOP + plotHeight + 5);
            gc.fillText(String.valueOf(step), x - 5, MARGIN_TOP + plotHeight + 20);
        }

        // Title
        gc.setFont(Font.font(14));
        gc.fillText(variableName + " — Fan Chart (" + result.getRunCount() + " runs)",
                MARGIN_LEFT, MARGIN_TOP - 15);

        gc.setFont(Font.font(12));
        gc.fillText("Step", MARGIN_LEFT + plotWidth / 2 - 15, h - 5);
    }
}
