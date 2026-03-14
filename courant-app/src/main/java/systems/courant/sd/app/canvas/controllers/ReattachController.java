package systems.courant.sd.app.canvas.controllers;

import systems.courant.sd.app.canvas.CanvasState;
import systems.courant.sd.app.canvas.FlowEndpointCalculator;
import systems.courant.sd.app.canvas.ModelEditor;
import systems.courant.sd.app.canvas.renderers.CanvasRenderer;
/**
 * Manages flow endpoint reattachment drag interactions.
 * When a user drags a cloud or connected endpoint, this controller
 * tracks the rubber-band state and completes the reconnection.
 */
public final class ReattachController {

    private boolean active;
    private String flowName;
    private FlowEndpointCalculator.FlowEnd end;
    private double diamondX;
    private double diamondY;
    private double rubberBandX;
    private double rubberBandY;

    public boolean isActive() {
        return active;
    }

    public String flowName() {
        return flowName;
    }

    /**
     * Returns the renderer state for the current reattachment.
     */
    public CanvasRenderer.ReattachState toRenderState() {
        if (!active) {
            return CanvasRenderer.ReattachState.IDLE;
        }
        return new CanvasRenderer.ReattachState(true,
                diamondX, diamondY, rubberBandX, rubberBandY);
    }

    /**
     * Begins a reattachment drag from a cloud or connected endpoint hit.
     */
    public void start(FlowEndpointCalculator.CloudHit hit, CanvasState state) {
        active = true;
        flowName = hit.flowName();
        end = hit.end();
        diamondX = state.getX(hit.flowName());
        diamondY = state.getY(hit.flowName());
        rubberBandX = hit.cloudX();
        rubberBandY = hit.cloudY();
    }

    /**
     * Updates the rubber-band endpoint during a drag.
     */
    public void drag(double worldX, double worldY) {
        rubberBandX = worldX;
        rubberBandY = worldY;
    }

    /**
     * Completes the reattachment: if released on a stock, reconnects the
     * flow endpoint; if released on empty space, disconnects to cloud.
     */
    public void complete(double worldX, double worldY, CanvasState state,
                  ModelEditor editor, Runnable saveUndo) {
        saveUndo.run();
        String stockHit = FlowCreationController.hitTestStockOnly(worldX, worldY, state);
        editor.reconnectFlow(flowName, end, stockHit);
        cancel();
    }

    /**
     * Cancels the reattachment drag without making changes.
     */
    public void cancel() {
        active = false;
        flowName = null;
        end = null;
    }
}
