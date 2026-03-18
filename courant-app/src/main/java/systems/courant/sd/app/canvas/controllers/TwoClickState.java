package systems.courant.sd.app.canvas.controllers;

/**
 * Common two-click state machine for controllers that create links between elements.
 * Manages the pending/source/rubber-band state shared by {@link FlowCreationController},
 * {@link CausalLinkCreationController}, and {@link InfoLinkCreationController}.
 */
public class TwoClickState {

    private boolean pending;
    private String source;
    private double sourceX;
    private double sourceY;
    private double rubberBandEndX;
    private double rubberBandEndY;

    /**
     * Transitions to the pending state with the given source and coordinates.
     *
     * @param source the source element name, or {@code null} for cloud/port sources
     * @param srcX   the source X coordinate
     * @param srcY   the source Y coordinate
     * @param mouseX the initial rubber-band end X
     * @param mouseY the initial rubber-band end Y
     */
    public void begin(String source, double srcX, double srcY, double mouseX, double mouseY) {
        this.pending = true;
        this.source = source;
        this.sourceX = srcX;
        this.sourceY = srcY;
        this.rubberBandEndX = mouseX;
        this.rubberBandEndY = mouseY;
    }

    /**
     * Updates the rubber-band endpoint during mouse movement.
     * Ignored when not in the pending state.
     */
    public void updateRubberBand(double worldX, double worldY) {
        if (pending) {
            rubberBandEndX = worldX;
            rubberBandEndY = worldY;
        }
    }

    /**
     * Resets all state back to idle.
     */
    public void reset() {
        pending = false;
        source = null;
        sourceX = 0;
        sourceY = 0;
        rubberBandEndX = 0;
        rubberBandEndY = 0;
    }

    public boolean isPending() {
        return pending;
    }

    public String source() {
        return source;
    }

    public double sourceX() {
        return sourceX;
    }

    public double sourceY() {
        return sourceY;
    }

    public double rubberBandEndX() {
        return rubberBandEndX;
    }

    public double rubberBandEndY() {
        return rubberBandEndY;
    }
}
