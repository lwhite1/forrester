package systems.courant.sd.app.canvas.controllers;

import systems.courant.sd.model.def.ElementType;
import systems.courant.sd.app.canvas.CanvasState;
import systems.courant.sd.app.canvas.HitTester;
import systems.courant.sd.app.canvas.ModelEditor;

/**
 * Two-click state machine for flow creation.
 * First click sets the source (stock or cloud), second click sets the sink and creates the flow.
 */
public class FlowCreationController implements CreationController {

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
        public static final State IDLE = new State(false, null, 0, 0, 0, 0);
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

    private final TwoClickState state = new TwoClickState();

    /**
     * Handles a click during flow creation.
     * First click: sets source (stock under cursor, or cloud if empty space). Returns pending result.
     * Second click: sets sink and creates the flow at the midpoint. Returns created or rejected result.
     */
    public FlowResult handleClick(double worldX, double worldY,
                                   CanvasState canvasState, ModelEditor editor) {
        if (!state.isPending()) {
            // First click: set source
            String hit = hitTestStockOnly(worldX, worldY, canvasState);
            double srcX;
            double srcY;
            if (hit != null) {
                srcX = canvasState.getX(hit);
                srcY = canvasState.getY(hit);
            } else {
                srcX = worldX;
                srcY = worldY;
            }
            state.begin(hit, srcX, srcY, worldX, worldY);
            return FlowResult.pending();
        } else {
            // Second click: set sink and create flow
            String sinkHit = hitTestStockOnly(worldX, worldY, canvasState);

            // Prevent self-loop: source and sink must not be the same stock
            if (sinkHit != null && sinkHit.equals(state.source())) {
                cancel();
                return FlowResult.rejected("Cannot create a self-loop: source and sink are the same stock");
            }

            // Prevent cloud-to-cloud: at least one end must be a stock
            if (state.source() == null && sinkHit == null) {
                cancel();
                return FlowResult.rejected("At least one end of a flow must connect to a stock");
            }

            double srcX = state.sourceX();
            double srcY = state.sourceY();
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

            String name = editor.addFlow(state.source(), sinkHit);
            canvasState.addElement(name, ElementType.FLOW, midX, midY);

            cancel();
            return FlowResult.created(name);
        }
    }

    /**
     * Updates the rubber-band endpoint during mouse movement.
     */
    public void updateRubberBand(double worldX, double worldY) {
        state.updateRubberBand(worldX, worldY);
    }

    /**
     * Cancels any pending flow creation, resetting all state.
     */
    public void cancel() {
        state.reset();
    }

    /**
     * Returns true if a flow creation is pending (first click done, awaiting second).
     */
    public boolean isPending() {
        return state.isPending();
    }

    /**
     * Returns an immutable snapshot of the current flow creation state.
     */
    public State getState() {
        if (!state.isPending()) {
            return State.IDLE;
        }
        return new State(true, state.source(), state.sourceX(), state.sourceY(),
                state.rubberBandEndX(), state.rubberBandEndY());
    }

    /**
     * Hit-tests for a stock element only. Returns the stock name, or null.
     */
    public static String hitTestStockOnly(double worldX, double worldY, CanvasState canvasState) {
        String hit = HitTester.hitTest(canvasState, worldX, worldY);
        if (hit != null && canvasState.getType(hit).orElse(null) == ElementType.STOCK) {
            return hit;
        }
        return null;
    }
}
