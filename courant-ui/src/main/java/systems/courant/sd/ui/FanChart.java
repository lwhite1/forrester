package systems.courant.sd.ui;

import systems.courant.sd.sweep.MonteCarloResult;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.stage.Stage;

import java.util.Locale;
import java.util.Map;

/**
 * Standalone JavaFX viewer that renders percentile bands (a "fan chart") from a
 * {@link MonteCarloResult}. Bands are drawn from outermost (lightest) to innermost
 * (darkest), with the median line stroked on top.
 *
 * <p>Usage:
 * <pre>{@code
 * FanChart.show(monteCarloResult, "Infectious");
 * }</pre>
 */
public class FanChart extends Application {

    private static final double WIDTH = 900;
    private static final double HEIGHT = 600;
    private static final double MARGIN_LEFT = 70;
    private static final double MARGIN_RIGHT = 30;
    private static final double MARGIN_TOP = 40;
    private static final double MARGIN_BOTTOM = 50;

    private static final Color BASE_COLOR = Color.STEELBLUE;

    /**
     * Band definitions: each band is defined by a lower percentile, an upper percentile,
     * and an opacity level.
     */
    private static final double[][] BANDS = {
            {2.5, 97.5, 0.15},   // 95% band — lightest
            {12.5, 87.5, 0.25},  // 75% band — medium
            {25.0, 75.0, 0.35},  // 50% band — darker
    };

    private final MonteCarloResult result;
    private final String variableName;

    /**
     * Creates a FanChart for the given Monte Carlo result and variable name.
     *
     * @param result       the Monte Carlo result to visualize
     * @param variableName the stock or variable name to plot
     */
    public FanChart(MonteCarloResult result, String variableName) {
        this.result = result;
        this.variableName = variableName;
    }

    /**
     * No-arg constructor required by JavaFX {@link Application#launch}.
     * Prefer {@link #show(MonteCarloResult, String)} for normal usage.
     */
    public FanChart() {
        this.result = null;
        this.variableName = null;
    }

    /**
     * Launches the fan chart viewer for the given Monte Carlo result and variable name.
     *
     * @param result       the Monte Carlo result to visualize
     * @param variableName the stock or variable name to plot
     */
    public static void show(MonteCarloResult result, String variableName) {
        ChartViewerApplication.ensureFxRunning();
        Platform.runLater(() -> {
            FanChart app = new FanChart(result, variableName);
            Stage stage = new Stage();
            app.start(stage);
        });
    }

    @Override
    public void start(Stage stage) {

        stage.setTitle("Fan Chart — " + variableName);

        Canvas canvas = new Canvas(WIDTH, HEIGHT);
        drawFanChart(canvas.getGraphicsContext2D(), result, variableName);

        StackPane root = new StackPane(canvas);
        Scene scene = new Scene(root, WIDTH, HEIGHT);
        stage.setScene(scene);
        stage.show();
    }

    private void drawFanChart(GraphicsContext gc, MonteCarloResult result, String variableName) {
        int stepCount = result.getStepCount();
        if (stepCount <= 1) {
            gc.setFill(Color.BLACK);
            gc.setFont(Font.font(14));
            gc.fillText("Not enough data points to draw chart (need at least 2 steps).",
                    MARGIN_LEFT, HEIGHT / 2);
            return;
        }

        // Compute all percentile series in a single pass
        Map<Double, double[]> pctMap = result.getPercentileSeries(variableName,
                2.5, 12.5, 25.0, 50.0, 75.0, 87.5, 97.5);
        double[] pct2 = pctMap.get(2.5);
        double[] pct97 = pctMap.get(97.5);
        double[] pct12 = pctMap.get(12.5);
        double[] pct87 = pctMap.get(87.5);
        double[] p25 = pctMap.get(25.0);
        double[] p75 = pctMap.get(75.0);
        double[] median = pctMap.get(50.0);

        double[][] lowerSeries = {pct2, pct12, p25};
        double[][] upperSeries = {pct97, pct87, p75};

        // Compute axis ranges
        double minVal = Double.MAX_VALUE;
        double maxVal = -Double.MAX_VALUE;
        for (int i = 0; i < stepCount; i++) {
            minVal = Math.min(minVal, pct2[i]);
            maxVal = Math.max(maxVal, pct97[i]);
        }

        // Add 5% padding
        double range = maxVal - minVal;
        if (range == 0) {
            range = 1;
        }
        minVal -= range * 0.05;
        maxVal += range * 0.05;

        double plotWidth = WIDTH - MARGIN_LEFT - MARGIN_RIGHT;
        double plotHeight = HEIGHT - MARGIN_TOP - MARGIN_BOTTOM;

        // Background
        gc.setFill(Color.WHITE);
        gc.fillRect(0, 0, WIDTH, HEIGHT);

        // Draw filled bands from outermost to innermost
        for (int b = 0; b < BANDS.length; b++) {
            double opacity = BANDS[b][2];
            gc.setFill(Color.color(BASE_COLOR.getRed(), BASE_COLOR.getGreen(),
                    BASE_COLOR.getBlue(), opacity));

            double[] xPoints = new double[stepCount * 2];
            double[] yPoints = new double[stepCount * 2];

            // Upper edge: left to right
            for (int i = 0; i < stepCount; i++) {
                xPoints[i] = MARGIN_LEFT + (i * plotWidth / (stepCount - 1));
                yPoints[i] = MARGIN_TOP + plotHeight - ((upperSeries[b][i] - minVal) / (maxVal - minVal) * plotHeight);
            }
            // Lower edge: right to left
            for (int i = 0; i < stepCount; i++) {
                xPoints[stepCount + i] = MARGIN_LEFT + ((stepCount - 1 - i) * plotWidth / (stepCount - 1));
                double lowerVal = lowerSeries[b][stepCount - 1 - i];
                yPoints[stepCount + i] = MARGIN_TOP + plotHeight
                        - ((lowerVal - minVal) / (maxVal - minVal) * plotHeight);
            }

            gc.fillPolygon(xPoints, yPoints, stepCount * 2);
        }

        // Draw median line
        gc.setStroke(Color.color(BASE_COLOR.getRed(), BASE_COLOR.getGreen(),
                BASE_COLOR.getBlue(), 0.9));
        gc.setLineWidth(2);
        gc.beginPath();
        for (int i = 0; i < stepCount; i++) {
            double x = MARGIN_LEFT + (i * plotWidth / (stepCount - 1));
            double y = MARGIN_TOP + plotHeight - ((median[i] - minVal) / (maxVal - minVal) * plotHeight);
            if (i == 0) {
                gc.moveTo(x, y);
            } else {
                gc.lineTo(x, y);
            }
        }
        gc.stroke();

        // Draw axes
        gc.setStroke(Color.BLACK);
        gc.setLineWidth(1);
        gc.strokeLine(MARGIN_LEFT, MARGIN_TOP, MARGIN_LEFT, MARGIN_TOP + plotHeight);
        gc.strokeLine(MARGIN_LEFT, MARGIN_TOP + plotHeight, MARGIN_LEFT + plotWidth, MARGIN_TOP + plotHeight);

        // Axis labels
        gc.setFill(Color.BLACK);
        gc.setFont(Font.font(12));

        // Y-axis ticks (5 ticks)
        int yTicks = 5;
        for (int i = 0; i <= yTicks; i++) {
            double val = minVal + (maxVal - minVal) * i / yTicks;
            double y = MARGIN_TOP + plotHeight - (plotHeight * i / yTicks);
            gc.strokeLine(MARGIN_LEFT - 5, y, MARGIN_LEFT, y);
            gc.fillText(String.format(Locale.US, "%.0f", val), 5, y + 4);
        }

        // X-axis ticks (up to 10 ticks)
        int xTicks = Math.min(10, stepCount - 1);
        for (int i = 0; i <= xTicks; i++) {
            int step = (int) Math.round((double) i * (stepCount - 1) / xTicks);
            double x = MARGIN_LEFT + (step * plotWidth / (stepCount - 1));
            gc.strokeLine(x, MARGIN_TOP + plotHeight, x, MARGIN_TOP + plotHeight + 5);
            gc.fillText(String.valueOf(step), x - 5, MARGIN_TOP + plotHeight + 20);
        }

        // Title
        gc.setFont(Font.font(14));
        gc.fillText(variableName + " — Monte Carlo Fan Chart (" + result.getRunCount() + " runs)",
                MARGIN_LEFT, MARGIN_TOP - 15);

        // X-axis label
        gc.setFont(Font.font(12));
        gc.fillText("Step", MARGIN_LEFT + plotWidth / 2 - 15, HEIGHT - 5);
    }
}
