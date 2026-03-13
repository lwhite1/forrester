package systems.courant.sd.app.canvas;

import systems.courant.sd.model.def.ElementType;

import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.testfx.framework.junit5.ApplicationExtension;
import org.testfx.framework.junit5.Start;

import java.util.Collections;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThatCode;

@DisplayName("SparklineRenderer")
@ExtendWith(ApplicationExtension.class)
class SparklineRendererTest {

    private GraphicsContext gc;
    private CanvasState state;

    @Start
    void start(Stage stage) {
        Canvas canvas = new Canvas(800, 600);
        gc = canvas.getGraphicsContext2D();
        stage.setScene(new Scene(new StackPane(canvas), 800, 600));
        stage.show();

        state = new CanvasState();
        state.addElement("Stock1", ElementType.STOCK, 200, 200);
        state.addElement("Stock2", ElementType.STOCK, 400, 200);
        state.addElement("Aux1", ElementType.AUX, 300, 300);
        state.addElement("Flow1", ElementType.FLOW, 300, 200);
    }

    @Test
    @DisplayName("drawAll should not throw with valid sparkline data")
    void shouldDrawWithValidData() {
        Map<String, double[]> sparklines = Map.of(
                "Stock1", new double[]{0, 1, 2, 3, 4, 5},
                "Stock2", new double[]{10, 8, 6, 4, 2, 0}
        );

        assertThatCode(() -> SparklineRenderer.drawAll(gc, state, sparklines, false))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("drawAll should not throw with stale flag set")
    void shouldDrawWithStaleFlag() {
        Map<String, double[]> sparklines = Map.of(
                "Stock1", new double[]{1, 2, 3, 4, 5}
        );

        assertThatCode(() -> SparklineRenderer.drawAll(gc, state, sparklines, true))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("drawAll should handle null sparklines map gracefully")
    void shouldHandleNullMap() {
        assertThatCode(() -> SparklineRenderer.drawAll(gc, state, null, false))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("drawAll should handle empty sparklines map gracefully")
    void shouldHandleEmptyMap() {
        assertThatCode(() -> SparklineRenderer.drawAll(gc, state, Collections.emptyMap(), false))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("drawAll should skip elements not in canvas state")
    void shouldSkipUnknownElements() {
        Map<String, double[]> sparklines = Map.of(
                "NonExistent", new double[]{1, 2, 3}
        );

        assertThatCode(() -> SparklineRenderer.drawAll(gc, state, sparklines, false))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("drawAll should skip non-stock elements")
    void shouldSkipNonStockElements() {
        Map<String, double[]> sparklines = Map.of(
                "Aux1", new double[]{1, 2, 3, 4, 5},
                "Flow1", new double[]{1, 2, 3, 4, 5}
        );

        assertThatCode(() -> SparklineRenderer.drawAll(gc, state, sparklines, false))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("drawAll should skip arrays with fewer than two values")
    void shouldSkipSingleValueArrays() {
        Map<String, double[]> sparklines = Map.of(
                "Stock1", new double[]{42}
        );

        assertThatCode(() -> SparklineRenderer.drawAll(gc, state, sparklines, false))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("drawAll should handle flat line (all identical values)")
    void shouldHandleFlatLine() {
        Map<String, double[]> sparklines = Map.of(
                "Stock1", new double[]{5, 5, 5, 5, 5}
        );

        assertThatCode(() -> SparklineRenderer.drawAll(gc, state, sparklines, false))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("drawAll should handle values containing NaN")
    void shouldHandleNaNValues() {
        Map<String, double[]> sparklines = Map.of(
                "Stock1", new double[]{1, Double.NaN, 3, Double.NaN, 5}
        );

        assertThatCode(() -> SparklineRenderer.drawAll(gc, state, sparklines, false))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("drawAll should handle values containing Infinity")
    void shouldHandleInfinityValues() {
        Map<String, double[]> sparklines = Map.of(
                "Stock1", new double[]{1, Double.POSITIVE_INFINITY, 3, Double.NEGATIVE_INFINITY, 5}
        );

        assertThatCode(() -> SparklineRenderer.drawAll(gc, state, sparklines, false))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("drawAll should handle all NaN values")
    void shouldHandleAllNaNValues() {
        Map<String, double[]> sparklines = Map.of(
                "Stock1", new double[]{Double.NaN, Double.NaN, Double.NaN}
        );

        assertThatCode(() -> SparklineRenderer.drawAll(gc, state, sparklines, false))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("drawAll should handle negative values")
    void shouldHandleNegativeValues() {
        Map<String, double[]> sparklines = Map.of(
                "Stock1", new double[]{-10, -5, 0, 5, 10}
        );

        assertThatCode(() -> SparklineRenderer.drawAll(gc, state, sparklines, false))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("drawAll should handle large number of data points")
    void shouldHandleLargeDataset() {
        double[] values = new double[1000];
        for (int i = 0; i < values.length; i++) {
            values[i] = Math.sin(i * 0.1) * 100;
        }
        Map<String, double[]> sparklines = Map.of("Stock1", values);

        assertThatCode(() -> SparklineRenderer.drawAll(gc, state, sparklines, false))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("drawAll should handle exactly two data points")
    void shouldHandleTwoDataPoints() {
        Map<String, double[]> sparklines = Map.of(
                "Stock1", new double[]{0, 100}
        );

        assertThatCode(() -> SparklineRenderer.drawAll(gc, state, sparklines, false))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("drawAll should handle multiple stocks simultaneously")
    void shouldHandleMultipleStocks() {
        Map<String, double[]> sparklines = Map.of(
                "Stock1", new double[]{0, 1, 2, 3, 4},
                "Stock2", new double[]{10, 20, 30, 40, 50}
        );

        assertThatCode(() -> SparklineRenderer.drawAll(gc, state, sparklines, false))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("drawAll should handle stock with custom size")
    void shouldHandleCustomSizeStock() {
        state.setSize("Stock1", 200, 120);

        Map<String, double[]> sparklines = Map.of(
                "Stock1", new double[]{1, 2, 3, 4, 5}
        );

        assertThatCode(() -> SparklineRenderer.drawAll(gc, state, sparklines, false))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("drawAll should handle stock with very small custom size")
    void shouldHandleVerySmallStock() {
        state.setSize("Stock1", 10, 10);

        Map<String, double[]> sparklines = Map.of(
                "Stock1", new double[]{1, 2, 3, 4, 5}
        );

        assertThatCode(() -> SparklineRenderer.drawAll(gc, state, sparklines, false))
                .doesNotThrowAnyException();
    }
}
