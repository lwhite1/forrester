package systems.courant.sd.app.canvas.renderers;

import systems.courant.sd.model.def.ElementType;
import systems.courant.sd.model.graph.FeedbackAnalysis.LoopType;

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

import static org.assertj.core.api.Assertions.assertThatCode;
import systems.courant.sd.app.canvas.CanvasState;
import systems.courant.sd.app.canvas.CausalLinkGeometry;

@DisplayName("FeedbackLoopRenderer")
@ExtendWith(ApplicationExtension.class)
class FeedbackLoopRendererTest {

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
        state.addElement("Flow1", ElementType.FLOW, 300, 200);
        state.addElement("Aux1", ElementType.AUX, 300, 300);
        state.addElement("CldVar1", ElementType.CLD_VARIABLE, 500, 300);
    }

    // --- drawLoopHighlight ---

    @Test
    @DisplayName("drawLoopHighlight should draw on stock element")
    void shouldDrawHighlightOnStock() {
        assertThatCode(() -> FeedbackLoopRenderer.drawLoopHighlight(gc, state, "Stock1"))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("drawLoopHighlight should draw on flow element (diamond shape)")
    void shouldDrawHighlightOnFlow() {
        assertThatCode(() -> FeedbackLoopRenderer.drawLoopHighlight(gc, state, "Flow1"))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("drawLoopHighlight should draw on aux element")
    void shouldDrawHighlightOnAux() {
        assertThatCode(() -> FeedbackLoopRenderer.drawLoopHighlight(gc, state, "Aux1"))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("drawLoopHighlight should draw on CLD variable")
    void shouldDrawHighlightOnCldVariable() {
        assertThatCode(() -> FeedbackLoopRenderer.drawLoopHighlight(gc, state, "CldVar1"))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("drawLoopHighlight should handle nonexistent element gracefully")
    void shouldHandleNonexistentElement() {
        assertThatCode(() -> FeedbackLoopRenderer.drawLoopHighlight(gc, state, "NonExistent"))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("drawLoopHighlight should handle element with custom size")
    void shouldHandleCustomSize() {
        state.setSize("Stock1", 200, 120);

        assertThatCode(() -> FeedbackLoopRenderer.drawLoopHighlight(gc, state, "Stock1"))
                .doesNotThrowAnyException();
    }

    // --- drawLoopLabel ---

    @Test
    @DisplayName("drawLoopLabel should draw reinforcing label")
    void shouldDrawReinforcingLabel() {
        assertThatCode(() -> FeedbackLoopRenderer.drawLoopLabel(
                gc, "R1", LoopType.REINFORCING, 300, 150))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("drawLoopLabel should draw balancing label")
    void shouldDrawBalancingLabel() {
        assertThatCode(() -> FeedbackLoopRenderer.drawLoopLabel(
                gc, "B1", LoopType.BALANCING, 300, 150))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("drawLoopLabel should draw indeterminate label")
    void shouldDrawIndeterminateLabel() {
        assertThatCode(() -> FeedbackLoopRenderer.drawLoopLabel(
                gc, "?1", LoopType.INDETERMINATE, 300, 150))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("drawLoopLabel should handle long label text")
    void shouldHandleLongLabel() {
        assertThatCode(() -> FeedbackLoopRenderer.drawLoopLabel(
                gc, "Feedback Group 1", LoopType.INDETERMINATE, 300, 150))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("drawLoopLabel should handle empty label")
    void shouldHandleEmptyLabel() {
        assertThatCode(() -> FeedbackLoopRenderer.drawLoopLabel(
                gc, "", LoopType.REINFORCING, 300, 150))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("drawLoopLabel should handle label at canvas edge")
    void shouldHandleLabelAtEdge() {
        assertThatCode(() -> FeedbackLoopRenderer.drawLoopLabel(
                gc, "R1", LoopType.REINFORCING, 0, 0))
                .doesNotThrowAnyException();
    }

    // --- drawLoopEdge ---

    @Test
    @DisplayName("drawLoopEdge should draw straight edge")
    void shouldDrawStraightEdge() {
        assertThatCode(() -> FeedbackLoopRenderer.drawLoopEdge(
                gc, 100, 100, 400, 200))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("drawLoopEdge should draw zero-length edge")
    void shouldDrawZeroLengthEdge() {
        assertThatCode(() -> FeedbackLoopRenderer.drawLoopEdge(
                gc, 200, 200, 200, 200))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("drawLoopEdge should draw edge with negative coordinates")
    void shouldDrawEdgeWithNegativeCoords() {
        assertThatCode(() -> FeedbackLoopRenderer.drawLoopEdge(
                gc, -100, -50, 400, 300))
                .doesNotThrowAnyException();
    }

    // --- drawLoopEdgeCurved ---

    @Test
    @DisplayName("drawLoopEdgeCurved should draw curved edge with control point")
    void shouldDrawCurvedEdge() {
        assertThatCode(() -> FeedbackLoopRenderer.drawLoopEdgeCurved(
                gc, 100, 200, 250, 100, 400, 200))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("drawLoopEdgeCurved should handle collinear control point")
    void shouldHandleCollinearControlPoint() {
        assertThatCode(() -> FeedbackLoopRenderer.drawLoopEdgeCurved(
                gc, 100, 200, 250, 200, 400, 200))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("drawLoopEdgeCurved should handle control point at start")
    void shouldHandleControlPointAtStart() {
        assertThatCode(() -> FeedbackLoopRenderer.drawLoopEdgeCurved(
                gc, 100, 200, 100, 200, 400, 200))
                .doesNotThrowAnyException();
    }

    // --- drawLoopEdgeCubic ---

    @Test
    @DisplayName("drawLoopEdgeCubic should draw from self-loop points")
    void shouldDrawCubicEdge() {
        double[] loopPts = CausalLinkGeometry.selfLoopPoints(300, 200, 55, 15);

        assertThatCode(() -> FeedbackLoopRenderer.drawLoopEdgeCubic(gc, loopPts))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("drawLoopEdgeCubic should draw with arbitrary control points")
    void shouldDrawCubicWithArbitraryPoints() {
        double[] pts = {100, 200, 150, 100, 250, 100, 300, 200};

        assertThatCode(() -> FeedbackLoopRenderer.drawLoopEdgeCubic(gc, pts))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("drawLoopEdgeCubic should draw degenerate cubic (all same point)")
    void shouldDrawDegenerateCubic() {
        double[] pts = {200, 200, 200, 200, 200, 200, 200, 200};

        assertThatCode(() -> FeedbackLoopRenderer.drawLoopEdgeCubic(gc, pts))
                .doesNotThrowAnyException();
    }
}
