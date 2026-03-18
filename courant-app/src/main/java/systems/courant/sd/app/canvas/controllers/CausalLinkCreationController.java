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

    private final TwoClickState state = new TwoClickState();

    /**
     * Handles a click during causal link creation.
     * First click: sets source (any element under cursor). Returns pending result.
     * Second click: sets target and creates the link. Returns created or rejected result.
     */
    public LinkResult handleClick(double worldX, double worldY,
                                  CanvasState canvasState, ModelEditor editor) {
        if (!state.isPending()) {
            String hit = HitTester.hitTest(canvasState, worldX, worldY);
            if (hit == null) {
                return LinkResult.rejected("Click on a variable to start drawing a causal link");
            }
            state.begin(hit, canvasState.getX(hit), canvasState.getY(hit), worldX, worldY);
            return LinkResult.pending();
        } else {
            String targetHit = HitTester.hitTest(canvasState, worldX, worldY);

            if (targetHit == null) {
                cancel();
                return LinkResult.rejected("Click on a variable to complete the causal link");
            }

            // Check for duplicate link
            for (CausalLinkDef existing : editor.getCausalLinks()) {
                if (existing.from().equals(state.source()) && existing.to().equals(targetHit)) {
                    cancel();
                    return LinkResult.rejected("A causal link already exists between these variables");
                }
            }

            editor.addCausalLink(state.source(), targetHit, CausalLinkDef.Polarity.UNKNOWN);
            cancel();
            return LinkResult.created();
        }
    }

    /**
     * Updates the rubber-band endpoint during mouse movement.
     */
    public void updateRubberBand(double worldX, double worldY) {
        state.updateRubberBand(worldX, worldY);
    }

    /**
     * Cancels any pending causal link creation, resetting all state.
     */
    public void cancel() {
        state.reset();
    }

    /**
     * Returns true if a causal link creation is pending (first click done, awaiting second).
     */
    public boolean isPending() {
        return state.isPending();
    }

    /**
     * Returns an immutable snapshot of the current causal link creation state.
     */
    public State getState() {
        if (!state.isPending()) {
            return State.IDLE;
        }
        return new State(true, state.source(), state.sourceX(), state.sourceY(),
                state.rubberBandEndX(), state.rubberBandEndY());
    }
}
