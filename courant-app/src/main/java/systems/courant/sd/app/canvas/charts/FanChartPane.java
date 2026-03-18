package systems.courant.sd.app.canvas.charts;

import systems.courant.sd.sweep.MonteCarloResult;

import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;

import java.util.Locale;
import java.util.Map;

/**
 * Embeddable pane that renders a fan chart (percentile bands) from a
 * {@link MonteCarloResult}. Adapted from the standalone {@code FanChart}
 * viewer in courant-ui.
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
    private MonteCarloResult currentResult;
    private String currentVariable;

    public FanChartPane(MonteCarloResult result, String variableName) {
        this.currentResult = result;
        this.currentVariable = variableName;
        canvas = new Canvas(0, 0);
        getChildren().add(canvas);
        canvas.setManaged(false);
    }

    @Override
    protected void layoutChildren() {
        double w = getWidth();
        double h = getHeight();
        if (w != canvas.getWidth() || h != canvas.getHeight()) {
            canvas.setWidth(w);
            canvas.setHeight(h);
            drawFanChart(canvas.getGraphicsContext2D(), currentResult, currentVariable);
        }
    }

    /**
     * Redraws the fan chart for a different variable.
     */
    public void redraw(MonteCarloResult result, String variableName) {
        this.currentResult = result;
        this.currentVariable = variableName;
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

        Map<Double, double[]> pctMap;
        try {
            pctMap = result.getPercentileSeries(variableName,
                    2.5, 12.5, 25.0, 50.0, 75.0, 87.5, 97.5);
        } catch (IllegalArgumentException e) {
            gc.setFill(Color.BLACK);
            gc.setFont(Font.font(14));
            gc.fillText("Variable not found: " + variableName, MARGIN_LEFT, h / 2);
            return;
        }

        double[][] lowerSeries = {pctMap.get(2.5), pctMap.get(12.5), pctMap.get(25.0)};
        double[][] upperSeries = {pctMap.get(97.5), pctMap.get(87.5), pctMap.get(75.0)};
        double[] median = pctMap.get(50.0);

        double minVal = Double.MAX_VALUE;
        double maxVal = -Double.MAX_VALUE;
        for (int i = 0; i < stepCount; i++) {
            minVal = Math.min(minVal, lowerSeries[0][i]);
            maxVal = Math.max(maxVal, upperSeries[0][i]);
        }
        double range = maxVal - minVal;
        if (range == 0) {
            range = 1;
        }
        minVal -= range * 0.05;
        maxVal += range * 0.05;

        double plotWidth = w - MARGIN_LEFT - MARGIN_RIGHT;
        double plotHeight = h - MARGIN_TOP - MARGIN_BOTTOM;

        if (plotWidth <= 0 || plotHeight <= 0) {
            return;
        }

        drawBands(gc, stepCount, lowerSeries, upperSeries, minVal, maxVal, plotWidth, plotHeight);
        drawMedianLine(gc, stepCount, median, minVal, maxVal, plotWidth, plotHeight);
        drawAxes(gc, plotWidth, plotHeight);
        drawAxisLabels(gc, stepCount, minVal, maxVal, plotWidth, plotHeight);
        drawTitle(gc, variableName + " — Fan Chart (" + result.getRunCount() + " runs)",
                plotWidth, h);
    }

    private void drawBands(GraphicsContext gc, int stepCount,
                           double[][] lowerSeries, double[][] upperSeries,
                           double minVal, double maxVal,
                           double plotWidth, double plotHeight) {
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
    }

    private void drawMedianLine(GraphicsContext gc, int stepCount, double[] median,
                                double minVal, double maxVal,
                                double plotWidth, double plotHeight) {
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
    }

    private void drawAxes(GraphicsContext gc, double plotWidth, double plotHeight) {
        gc.setStroke(Color.BLACK);
        gc.setLineWidth(1);
        gc.strokeLine(MARGIN_LEFT, MARGIN_TOP, MARGIN_LEFT, MARGIN_TOP + plotHeight);
        gc.strokeLine(MARGIN_LEFT, MARGIN_TOP + plotHeight,
                MARGIN_LEFT + plotWidth, MARGIN_TOP + plotHeight);
    }

    private void drawAxisLabels(GraphicsContext gc, int stepCount,
                                double minVal, double maxVal,
                                double plotWidth, double plotHeight) {
        gc.setFill(Color.BLACK);
        gc.setFont(Font.font(12));

        int yTicks = 5;
        for (int i = 0; i <= yTicks; i++) {
            double val = minVal + (maxVal - minVal) * i / yTicks;
            double y = MARGIN_TOP + plotHeight - (plotHeight * i / yTicks);
            gc.strokeLine(MARGIN_LEFT - 5, y, MARGIN_LEFT, y);
            gc.fillText(String.format(Locale.US, "%.0f", val), 5, y + 4);
        }

        int xTicks = Math.min(10, stepCount - 1);
        for (int i = 0; i <= xTicks; i++) {
            int step = (int) Math.round((double) i * (stepCount - 1) / xTicks);
            double x = MARGIN_LEFT + (step * plotWidth / (stepCount - 1));
            gc.strokeLine(x, MARGIN_TOP + plotHeight, x, MARGIN_TOP + plotHeight + 5);
            gc.fillText(String.valueOf(step), x - 5, MARGIN_TOP + plotHeight + 20);
        }
    }

    private void drawTitle(GraphicsContext gc, String title,
                           double plotWidth, double canvasHeight) {
        gc.setFont(Font.font(14));
        gc.fillText(title, MARGIN_LEFT, MARGIN_TOP - 15);

        gc.setFont(Font.font(12));
        gc.fillText("Step", MARGIN_LEFT + plotWidth / 2 - 15, canvasHeight - 5);
    }
}
