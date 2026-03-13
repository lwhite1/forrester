package systems.courant.sd.app.canvas;

import systems.courant.sd.model.def.ElementType;
import systems.courant.sd.model.graph.CausalTraceAnalysis;
import systems.courant.sd.model.graph.CausalTraceAnalysis.TraceDirection;

import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.stage.Stage;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.testfx.framework.junit5.ApplicationExtension;
import org.testfx.framework.junit5.Start;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

@DisplayName("CausalTraceRenderer")
@ExtendWith(ApplicationExtension.class)
class CausalTraceRendererTest {

    private GraphicsContext gc;
    private CanvasState state;

    @Start
    void start(Stage stage) {
        Canvas canvas = new Canvas(800, 600);
        gc = canvas.getGraphicsContext2D();
        stage.setScene(new Scene(new StackPane(canvas), 800, 600));
        stage.show();

        state = new CanvasState();
        state.addElement("Origin", ElementType.STOCK, 200, 200);
        state.addElement("Upstream1", ElementType.STOCK, 100, 100);
        state.addElement("Downstream1", ElementType.AUX, 400, 300);
        state.addElement("FlowElem", ElementType.FLOW, 300, 200);
        state.addElement("CldVar", ElementType.CLD_VARIABLE, 500, 100);
    }

    private CausalTraceAnalysis buildTrace(String origin, TraceDirection direction,
                                            Map<String, Integer> depthMap, int maxDepth) {
        return new CausalTraceAnalysis(origin, direction, depthMap,
                Collections.emptySet(), maxDepth);
    }

    // --- traceColor ---

    @Test
    @DisplayName("traceColor should return upstream color for upstream direction")
    void shouldReturnUpstreamColor() {
        Color color = CausalTraceRenderer.traceColor(TraceDirection.UPSTREAM);

        assertThat(color).isEqualTo(ColorPalette.TRACE_UPSTREAM);
    }

    @Test
    @DisplayName("traceColor should return downstream color for downstream direction")
    void shouldReturnDownstreamColor() {
        Color color = CausalTraceRenderer.traceColor(TraceDirection.DOWNSTREAM);

        assertThat(color).isEqualTo(ColorPalette.TRACE_DOWNSTREAM);
    }

    // --- drawTraceHighlight ---

    @Test
    @DisplayName("drawTraceHighlight should draw on origin element")
    void shouldDrawOriginHighlight() {
        Map<String, Integer> depthMap = new LinkedHashMap<>();
        depthMap.put("Origin", 0);
        CausalTraceAnalysis trace = buildTrace("Origin", TraceDirection.DOWNSTREAM, depthMap, 0);

        assertThatCode(() -> CausalTraceRenderer.drawTraceHighlight(gc, state, "Origin", trace))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("drawTraceHighlight should draw on upstream non-origin element")
    void shouldDrawUpstreamHighlight() {
        Map<String, Integer> depthMap = new LinkedHashMap<>();
        depthMap.put("Origin", 0);
        depthMap.put("Upstream1", 1);
        CausalTraceAnalysis trace = buildTrace("Origin", TraceDirection.UPSTREAM, depthMap, 1);

        assertThatCode(() -> CausalTraceRenderer.drawTraceHighlight(gc, state, "Upstream1", trace))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("drawTraceHighlight should draw on downstream non-origin element")
    void shouldDrawDownstreamHighlight() {
        Map<String, Integer> depthMap = new LinkedHashMap<>();
        depthMap.put("Origin", 0);
        depthMap.put("Downstream1", 1);
        CausalTraceAnalysis trace = buildTrace("Origin", TraceDirection.DOWNSTREAM, depthMap, 1);

        assertThatCode(() -> CausalTraceRenderer.drawTraceHighlight(gc, state, "Downstream1", trace))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("drawTraceHighlight should draw on flow element (diamond shape)")
    void shouldDrawFlowHighlight() {
        Map<String, Integer> depthMap = new LinkedHashMap<>();
        depthMap.put("Origin", 0);
        depthMap.put("FlowElem", 1);
        CausalTraceAnalysis trace = buildTrace("Origin", TraceDirection.DOWNSTREAM, depthMap, 1);

        assertThatCode(() -> CausalTraceRenderer.drawTraceHighlight(gc, state, "FlowElem", trace))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("drawTraceHighlight should draw on CLD variable")
    void shouldDrawCldVarHighlight() {
        Map<String, Integer> depthMap = new LinkedHashMap<>();
        depthMap.put("Origin", 0);
        depthMap.put("CldVar", 2);
        CausalTraceAnalysis trace = buildTrace("Origin", TraceDirection.UPSTREAM, depthMap, 2);

        assertThatCode(() -> CausalTraceRenderer.drawTraceHighlight(gc, state, "CldVar", trace))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("drawTraceHighlight should handle nonexistent element gracefully")
    void shouldHandleNonexistentElement() {
        Map<String, Integer> depthMap = new LinkedHashMap<>();
        depthMap.put("NonExistent", 1);
        CausalTraceAnalysis trace = buildTrace("Origin", TraceDirection.DOWNSTREAM, depthMap, 1);

        assertThatCode(() -> CausalTraceRenderer.drawTraceHighlight(gc, state, "NonExistent", trace))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("drawTraceHighlight should handle deep trace with faded opacity")
    void shouldHandleDeepTrace() {
        Map<String, Integer> depthMap = new LinkedHashMap<>();
        depthMap.put("Origin", 0);
        depthMap.put("Upstream1", 5);
        CausalTraceAnalysis trace = buildTrace("Origin", TraceDirection.UPSTREAM, depthMap, 10);

        assertThatCode(() -> CausalTraceRenderer.drawTraceHighlight(gc, state, "Upstream1", trace))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("drawTraceHighlight should handle element with custom size")
    void shouldHandleCustomSize() {
        state.setSize("Origin", 200, 120);
        Map<String, Integer> depthMap = new LinkedHashMap<>();
        depthMap.put("Origin", 0);
        CausalTraceAnalysis trace = buildTrace("Origin", TraceDirection.DOWNSTREAM, depthMap, 0);

        assertThatCode(() -> CausalTraceRenderer.drawTraceHighlight(gc, state, "Origin", trace))
                .doesNotThrowAnyException();
    }

    // --- drawTraceEdge ---

    @Test
    @DisplayName("drawTraceEdge should draw upstream edge")
    void shouldDrawUpstreamEdge() {
        assertThatCode(() -> CausalTraceRenderer.drawTraceEdge(
                gc, 100, 100, 400, 300, 0.8, TraceDirection.UPSTREAM))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("drawTraceEdge should draw downstream edge")
    void shouldDrawDownstreamEdge() {
        assertThatCode(() -> CausalTraceRenderer.drawTraceEdge(
                gc, 100, 100, 400, 300, 0.8, TraceDirection.DOWNSTREAM))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("drawTraceEdge should handle zero opacity")
    void shouldHandleZeroOpacity() {
        assertThatCode(() -> CausalTraceRenderer.drawTraceEdge(
                gc, 100, 100, 400, 300, 0.0, TraceDirection.UPSTREAM))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("drawTraceEdge should handle full opacity")
    void shouldHandleFullOpacity() {
        assertThatCode(() -> CausalTraceRenderer.drawTraceEdge(
                gc, 100, 100, 400, 300, 1.0, TraceDirection.DOWNSTREAM))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("drawTraceEdge should handle zero-length edge")
    void shouldHandleZeroLengthEdge() {
        assertThatCode(() -> CausalTraceRenderer.drawTraceEdge(
                gc, 200, 200, 200, 200, 0.5, TraceDirection.UPSTREAM))
                .doesNotThrowAnyException();
    }

    // --- drawTraceEdgeCurved ---

    @Test
    @DisplayName("drawTraceEdgeCurved should draw curved upstream edge")
    void shouldDrawCurvedUpstreamEdge() {
        assertThatCode(() -> CausalTraceRenderer.drawTraceEdgeCurved(
                gc, 100, 200, 250, 100, 400, 200, 0.8, TraceDirection.UPSTREAM))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("drawTraceEdgeCurved should draw curved downstream edge")
    void shouldDrawCurvedDownstreamEdge() {
        assertThatCode(() -> CausalTraceRenderer.drawTraceEdgeCurved(
                gc, 100, 200, 250, 100, 400, 200, 0.6, TraceDirection.DOWNSTREAM))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("drawTraceEdgeCurved should handle collinear control point")
    void shouldHandleCollinearControlPoint() {
        assertThatCode(() -> CausalTraceRenderer.drawTraceEdgeCurved(
                gc, 100, 200, 250, 200, 400, 200, 0.5, TraceDirection.UPSTREAM))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("drawTraceEdgeCurved should handle very low opacity")
    void shouldHandleVeryLowOpacity() {
        assertThatCode(() -> CausalTraceRenderer.drawTraceEdgeCurved(
                gc, 100, 200, 250, 100, 400, 200, 0.1, TraceDirection.DOWNSTREAM))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("drawTraceEdgeCurved should handle degenerate curve (all same point)")
    void shouldHandleDegenerateCurve() {
        assertThatCode(() -> CausalTraceRenderer.drawTraceEdgeCurved(
                gc, 200, 200, 200, 200, 200, 200, 0.5, TraceDirection.UPSTREAM))
                .doesNotThrowAnyException();
    }
}
