package systems.courant.sd.app.canvas.controllers;

/**
 * Common interface for two-click creation controllers (flow, causal link,
 * info link). Enables uniform handling of rubber-band updates, pending
 * state checks, and cancellation across all connection creation tools.
 */
public interface CreationController {

    /** Returns true if a first click has been registered and the creation is awaiting a second click. */
    boolean isPending();

    /** Updates the rubber-band endpoint shown while the user moves the mouse. */
    void updateRubberBand(double worldX, double worldY);

    /** Cancels the pending creation, returning to idle state. */
    void cancel();
}
