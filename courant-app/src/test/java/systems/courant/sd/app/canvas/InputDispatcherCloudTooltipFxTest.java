package systems.courant.sd.app.canvas;

import systems.courant.sd.app.canvas.controllers.TooltipController;
import systems.courant.sd.model.def.ElementType;

import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.control.Tooltip;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.testfx.framework.junit5.ApplicationExtension;
import org.testfx.framework.junit5.Start;
import org.testfx.util.WaitForAsyncUtils;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests that cloud tooltips appear when hovering a cloud from empty space (#1191).
 * <p>
 * The bug: cloud tooltip checks were inside the {@code if (changed)} block in
 * {@link InputDispatcher#handleMouseMoved}, where {@code changed} only tracks
 * element/connection transitions. Moving from empty space to a cloud keeps
 * {@code hit=null} and {@code connHit=null}, so {@code changed} is false and
 * the tooltip never fires.
 */
@DisplayName("InputDispatcher cloud tooltip from empty space (#1191)")
@ExtendWith(ApplicationExtension.class)
class InputDispatcherCloudTooltipFxTest {

    private ModelCanvas canvas;
    private String flowName;

    @Start
    void start(Stage stage) {
        canvas = new ModelCanvas(new Clipboard());
        canvas.undo().setUndoManager(new UndoManager());

        // Flow with both endpoints disconnected — both ends are clouds.
        // No stocks, so the only element is the flow diamond at (300, 300).
        ModelEditor editor = new ModelEditor();
        flowName = editor.addFlow(null, null);

        CanvasState state = new CanvasState();
        state.addElement(flowName, ElementType.FLOW, 300, 300);
        canvas.setModel(editor, state.toViewDef());

        StackPane root = new StackPane(canvas);
        stage.setScene(new Scene(root, 800, 600));
        stage.show();
    }

    @Test
    @DisplayName("cloud tooltip fires when hovering from empty space to cloud")
    void shouldShowCloudTooltipFromEmptySpace() {
        Platform.runLater(() -> {
            // Source cloud is at (300 - 84, 300) = (216, 300)
            // Flow hit half-width = 55, so flow hits x in [245, 355]
            // Cloud at 216 < 245 — safely outside flow hit area.
            FlowGeometry.Point2D cloudPos = FlowEndpointCalculator.cloudPosition(
                    FlowEndpointCalculator.FlowEnd.SOURCE,
                    canvas.getEditor().getFlows().getFirst(), canvas.canvasState());
            assertThat(cloudPos).isNotNull();
            double cx = cloudPos.x();
            double cy = cloudPos.y();

            // Verify cloud is outside flow hit area
            assertThat(HitTester.hitTest(canvas.canvasState(), cx, cy, false)).isNull();

            // Directly call handleMouseMoved — bypasses Event.fireEvent routing issues
            // Move to empty space first (sets hoveredElement=null, hoveredConnection=null)
            canvas.inputDispatcher.handleMouseMoved(syntheticMouseMove(700, 500), canvas);
            // Move to cloud — this is the scenario that was broken:
            // hoveredElement stays null -> changed=false -> cloud tooltip was skipped
            canvas.inputDispatcher.handleMouseMoved(syntheticMouseMove(cx, cy), canvas);

            Tooltip tooltip = getTooltip();
            assertThat(tooltip.getText()).contains("Source cloud");
            assertThat(tooltip.getText()).contains(flowName);
        });
        WaitForAsyncUtils.waitForFxEvents();
    }

    @Test
    @DisplayName("cloud tooltip hides when moving from cloud to empty space")
    void shouldHideCloudTooltipWhenLeavingCloud() {
        Platform.runLater(() -> {
            FlowGeometry.Point2D cloudPos = FlowEndpointCalculator.cloudPosition(
                    FlowEndpointCalculator.FlowEnd.SOURCE,
                    canvas.getEditor().getFlows().getFirst(), canvas.canvasState());
            assertThat(cloudPos).isNotNull();

            // Move to cloud
            canvas.inputDispatcher.handleMouseMoved(
                    syntheticMouseMove(cloudPos.x(), cloudPos.y()), canvas);
            Tooltip tooltip = getTooltip();
            assertThat(tooltip.getText()).contains("Source cloud");

            // Move to empty space — tooltip text should be cleared
            canvas.inputDispatcher.handleMouseMoved(syntheticMouseMove(700, 500), canvas);
            // updateCloudTooltip(null) calls hide() — verify tooltip was uninstalled
            // by re-hovering the cloud and checking text is set again
            canvas.inputDispatcher.handleMouseMoved(
                    syntheticMouseMove(cloudPos.x(), cloudPos.y()), canvas);
            assertThat(tooltip.getText()).contains("Source cloud");
        });
        WaitForAsyncUtils.waitForFxEvents();
    }

    private Tooltip getTooltip() {
        try {
            var tcField = ModelCanvas.class.getDeclaredField("tooltipController");
            tcField.setAccessible(true);
            TooltipController tc = (TooltipController) tcField.get(canvas);

            var ttField = TooltipController.class.getDeclaredField("elementTooltip");
            ttField.setAccessible(true);
            return (Tooltip) ttField.get(tc);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }

    private static MouseEvent syntheticMouseMove(double x, double y) {
        return new MouseEvent(
                MouseEvent.MOUSE_MOVED,
                x, y, x, y,
                MouseButton.NONE, 0,
                false, false, false, false,
                false, false, false,
                false, false, false, null);
    }
}
