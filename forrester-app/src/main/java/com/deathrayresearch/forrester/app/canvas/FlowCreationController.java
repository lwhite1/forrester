package com.deathrayresearch.forrester.app.canvas;

import com.deathrayresearch.forrester.model.def.ElementType;

/**
 * Two-click state machine for flow creation.
 * First click sets the source (stock or cloud), second click sets the sink and creates the flow.
 */
public class FlowCreationController {

    /**
     * Immutable snapshot of the flow creation state, used by the renderer for rubber-band drawing.
     */
    public record State(
            boolean pending,
            String source,
            double sourceX,
            double sourceY,
            double rubberBandEndX,
            double rubberBandEndY
    ) {
        static final State IDLE = new State(false, null, 0, 0, 0, 0);
    }

    /**
     * Result of a flow creation click attempt.
     */
    public record FlowResult(String flowName, String rejectionReason) {

        static FlowResult pending() {
            return new FlowResult(null, null);
        }

        static FlowResult created(String name) {
            return new FlowResult(name, null);
        }

        static FlowResult rejected(String reason) {
            return new FlowResult(null, reason);
        }

        public boolean isCreated() {
            return flowName != null;
        }

        public boolean isRejected() {
            return rejectionReason != null;
        }
    }

    private boolean pending;
    private String pendingSource;
    private double sourceX;
    private double sourceY;
    private double rubberBandEndX;
    private double rubberBandEndY;

    /**
     * Handles a click during flow creation.
     * First click: sets source (stock under cursor, or cloud if empty space). Returns pending result.
     * Second click: sets sink and creates the flow at the midpoint. Returns created or rejected result.
     */
    public FlowResult handleClick(double worldX, double worldY,
                                   CanvasState canvasState, ModelEditor editor) {
        if (!pending) {
            // First click: set source
            String hit = hitTestStockOnly(worldX, worldY, canvasState);
            pending = true;
            pendingSource = hit;

            if (hit != null) {
                sourceX = canvasState.getX(hit);
                sourceY = canvasState.getY(hit);
            } else {
                sourceX = worldX;
                sourceY = worldY;
            }
            rubberBandEndX = worldX;
            rubberBandEndY = worldY;
            return FlowResult.pending();
        } else {
            // Second click: set sink and create flow
            String sinkHit = hitTestStockOnly(worldX, worldY, canvasState);

            // Prevent self-loop: source and sink must not be the same stock
            if (sinkHit != null && sinkHit.equals(pendingSource)) {
                cancel();
                return FlowResult.rejected("Cannot create a self-loop: source and sink are the same stock");
            }

            // Prevent cloud-to-cloud: at least one end must be a stock
            if (pendingSource == null && sinkHit == null) {
                cancel();
                return FlowResult.rejected("At least one end of a flow must connect to a stock");
            }

            double srcX = sourceX;
            double srcY = sourceY;
            double dstX;
            double dstY;

            if (sinkHit != null) {
                dstX = canvasState.getX(sinkHit);
                dstY = canvasState.getY(sinkHit);
            } else {
                dstX = worldX;
                dstY = worldY;
            }

            double midX = (srcX + dstX) / 2;
            double midY = (srcY + dstY) / 2;

            String name = editor.addFlow(pendingSource, sinkHit);
            canvasState.addElement(name, ElementType.FLOW, midX, midY);

            cancel();
            return FlowResult.created(name);
        }
    }

    /**
     * Updates the rubber-band endpoint during mouse movement.
     */
    public void updateRubberBand(double worldX, double worldY) {
        if (pending) {
            rubberBandEndX = worldX;
            rubberBandEndY = worldY;
        }
    }

    /**
     * Cancels any pending flow creation, resetting all state.
     */
    public void cancel() {
        pending = false;
        pendingSource = null;
        sourceX = 0;
        sourceY = 0;
        rubberBandEndX = 0;
        rubberBandEndY = 0;
    }

    /**
     * Returns true if a flow creation is pending (first click done, awaiting second).
     */
    public boolean isPending() {
        return pending;
    }

    /**
     * Returns an immutable snapshot of the current flow creation state.
     */
    public State getState() {
        if (!pending) {
            return State.IDLE;
        }
        return new State(true, pendingSource, sourceX, sourceY, rubberBandEndX, rubberBandEndY);
    }

    /**
     * Hit-tests for a stock element only. Returns the stock name, or null.
     */
    static String hitTestStockOnly(double worldX, double worldY, CanvasState canvasState) {
        String hit = HitTester.hitTest(canvasState, worldX, worldY);
        if (hit != null && canvasState.getType(hit).orElse(null) == ElementType.STOCK) {
            return hit;
        }
        return null;
    }
}
