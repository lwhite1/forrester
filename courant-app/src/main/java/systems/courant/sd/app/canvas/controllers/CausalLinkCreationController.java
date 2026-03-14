package systems.courant.sd.app.canvas.controllers;

import systems.courant.sd.model.def.CausalLinkDef;
import systems.courant.sd.app.canvas.CanvasState;
import systems.courant.sd.app.canvas.HitTester;
import systems.courant.sd.app.canvas.ModelEditor;

/**
 * Two-click state machine for causal link creation.
 * First click sets the source variable, second click sets the target and creates the link.
 */
public class CausalLinkCreationController {

    /**
     * Immutable snapshot of the causal link creation state, used by the renderer for rubber-band drawing.
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
     * Result of a causal link creation click attempt.
     */
    public record LinkResult(boolean success, String rejectionReason) {

        static LinkResult pending() {
            return new LinkResult(false, null);
        }

        static LinkResult created() {
            return new LinkResult(true, null);
        }

        static LinkResult rejected(String reason) {
            return new LinkResult(false, reason);
        }

        public boolean isCreated() {
            return success;
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
     * Handles a click during causal link creation.
     * First click: sets source (any element under cursor). Returns pending result.
     * Second click: sets target and creates the link. Returns created or rejected result.
     */
    public LinkResult handleClick(double worldX, double worldY,
                                  CanvasState canvasState, ModelEditor editor) {
        if (!pending) {
            String hit = HitTester.hitTest(canvasState, worldX, worldY);
            if (hit == null) {
                return LinkResult.rejected("Click on a variable to start drawing a causal link");
            }
            pending = true;
            pendingSource = hit;
            sourceX = canvasState.getX(hit);
            sourceY = canvasState.getY(hit);
            rubberBandEndX = worldX;
            rubberBandEndY = worldY;
            return LinkResult.pending();
        } else {
            String targetHit = HitTester.hitTest(canvasState, worldX, worldY);

            if (targetHit == null) {
                cancel();
                return LinkResult.rejected("Click on a variable to complete the causal link");
            }

            // Check for duplicate link
            for (CausalLinkDef existing : editor.getCausalLinks()) {
                if (existing.from().equals(pendingSource) && existing.to().equals(targetHit)) {
                    cancel();
                    return LinkResult.rejected("A causal link already exists between these variables");
                }
            }

            editor.addCausalLink(pendingSource, targetHit, CausalLinkDef.Polarity.UNKNOWN);
            cancel();
            return LinkResult.created();
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
     * Cancels any pending causal link creation, resetting all state.
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
     * Returns true if a causal link creation is pending (first click done, awaiting second).
     */
    public boolean isPending() {
        return pending;
    }

    /**
     * Returns an immutable snapshot of the current causal link creation state.
     */
    public State getState() {
        if (!pending) {
            return State.IDLE;
        }
        return new State(true, pendingSource, sourceX, sourceY, rubberBandEndX, rubberBandEndY);
    }
}
