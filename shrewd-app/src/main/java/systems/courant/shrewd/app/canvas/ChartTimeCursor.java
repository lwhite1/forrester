package systems.courant.shrewd.app.canvas;

import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.geometry.Bounds;
import javafx.geometry.Point2D;
import javafx.scene.Node;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Label;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Line;

/**
 * Adds a vertical time cursor overlay to an {@link XYChart}. The cursor tracks
 * mouse position and exposes the current time step as a {@link DoubleProperty},
 * allowing multiple charts to share a synchronized cursor.
 *
 * <p>Usage: wrap the chart with {@link #install(XYChart)} which returns a
 * {@code StackPane} containing the chart and the cursor overlay. Bind or
 * listen to {@link #cursorTimeStepProperty()} to synchronize across charts.
 */
final class ChartTimeCursor {

    static final String CURSOR_LINE_ID = "timeCursorLine";
    static final String CURSOR_LABEL_ID = "timeCursorLabel";
    static final Color CURSOR_COLOR = Color.web("#555555");
    static final double CURSOR_WIDTH = 1.0;

    private final DoubleProperty cursorTimeStep = new SimpleDoubleProperty(Double.NaN);
    private final XYChart<Number, Number> chart;
    private final Line cursorLine;
    private final Label cursorLabel;
    private final Pane overlay;

    /**
     * Installs a time cursor on the given chart. Returns a {@code StackPane}
     * containing the chart and the transparent cursor overlay.
     */
    static StackPane install(XYChart<Number, Number> chart, ChartTimeCursor[] out) {
        ChartTimeCursor cursor = new ChartTimeCursor(chart);
        out[0] = cursor;
        StackPane wrapper = new StackPane(chart, cursor.overlay);
        return wrapper;
    }

    private ChartTimeCursor(XYChart<Number, Number> chart) {
        this.chart = chart;

        cursorLine = new Line();
        cursorLine.setId(CURSOR_LINE_ID);
        cursorLine.setStroke(CURSOR_COLOR);
        cursorLine.setStrokeWidth(CURSOR_WIDTH);
        cursorLine.getStrokeDashArray().addAll(4.0, 4.0);
        cursorLine.setVisible(false);
        cursorLine.setMouseTransparent(true);

        cursorLabel = new Label();
        cursorLabel.setId(CURSOR_LABEL_ID);
        cursorLabel.setStyle("-fx-font-size: 10px; -fx-text-fill: #555; "
                + "-fx-background-color: rgba(255,255,255,0.85); -fx-padding: 1 4 1 4;");
        cursorLabel.setVisible(false);
        cursorLabel.setMouseTransparent(true);

        overlay = new Pane(cursorLine, cursorLabel);
        overlay.setMouseTransparent(false);
        overlay.setPickOnBounds(false);
        overlay.setStyle("-fx-background-color: transparent;");

        overlay.setOnMouseMoved(event -> {
            Node plotArea = chart.lookup(".chart-plot-background");
            if (plotArea == null) {
                return;
            }
            Point2D plotLocal = plotArea.sceneToLocal(event.getSceneX(), event.getSceneY());
            Bounds plotBounds = plotArea.getBoundsInLocal();
            if (plotLocal.getX() >= 0 && plotLocal.getX() <= plotBounds.getWidth()
                    && plotLocal.getY() >= 0 && plotLocal.getY() <= plotBounds.getHeight()) {
                NumberAxis xAxis = (NumberAxis) chart.getXAxis();
                double value = xAxis.getValueForDisplay(plotLocal.getX()).doubleValue();
                cursorTimeStep.set(value);
            }
        });

        overlay.setOnMouseExited(event -> cursorTimeStep.set(Double.NaN));

        cursorTimeStep.addListener((obs, oldVal, newVal) -> updateCursorDisplay(newVal.doubleValue()));
    }

    /**
     * The cursor time step property. {@code Double.NaN} means no cursor is active.
     */
    DoubleProperty cursorTimeStepProperty() {
        return cursorTimeStep;
    }

    private void updateCursorDisplay(double timeStep) {
        if (Double.isNaN(timeStep)) {
            cursorLine.setVisible(false);
            cursorLabel.setVisible(false);
            return;
        }

        Node plotArea = chart.lookup(".chart-plot-background");
        if (plotArea == null) {
            return;
        }

        NumberAxis xAxis = (NumberAxis) chart.getXAxis();
        double displayX = xAxis.getDisplayPosition(timeStep);
        Bounds plotBounds = plotArea.getBoundsInLocal();

        // Convert plot-area local position to overlay coordinates
        Point2D topInScene = plotArea.localToScene(displayX, 0);
        Point2D bottomInScene = plotArea.localToScene(displayX, plotBounds.getHeight());
        Point2D topInOverlay = overlay.sceneToLocal(topInScene);
        Point2D bottomInOverlay = overlay.sceneToLocal(bottomInScene);

        if (topInOverlay == null || bottomInOverlay == null) {
            return;
        }

        cursorLine.setStartX(topInOverlay.getX());
        cursorLine.setStartY(topInOverlay.getY());
        cursorLine.setEndX(bottomInOverlay.getX());
        cursorLine.setEndY(bottomInOverlay.getY());
        cursorLine.setVisible(true);

        cursorLabel.setText("t=" + ChartUtils.formatNumber(timeStep));
        cursorLabel.setLayoutX(topInOverlay.getX() + 4);
        cursorLabel.setLayoutY(topInOverlay.getY());
        cursorLabel.setVisible(true);
    }
}
