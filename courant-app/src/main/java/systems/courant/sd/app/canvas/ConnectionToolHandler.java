package systems.courant.sd.app.canvas;

import systems.courant.sd.app.canvas.controllers.CreationController;

import java.util.List;
import java.util.Set;
import java.util.function.BiConsumer;

/**
 * Bundles the behavior shared by connection-creation tools (flow, causal link,
 * info link). Each registration pairs a {@link CreationController} with a
 * click handler, so that new connection tools can be added by creating one
 * registration — without editing {@link InputDispatcher}.
 */
final class ConnectionToolHandler {

    /**
     * A registered connection tool: its creation controller, click action,
     * and optional rubber-band updater for info-link hovered-port tracking.
     */
    record Registration(
            CanvasToolBar.Tool tool,
            CreationController controller,
            BiConsumer<ModelCanvas, double[]> clickAction) {
    }

    private final List<Registration> registrations;

    ConnectionToolHandler(List<Registration> registrations) {
        this.registrations = List.copyOf(registrations);
    }

    /**
     * Returns true if the given tool is a registered connection tool.
     */
    boolean isConnectionTool(CanvasToolBar.Tool tool) {
        for (Registration reg : registrations) {
            if (reg.tool() == tool) {
                return true;
            }
        }
        return false;
    }

    /**
     * Returns true if any registered connection creation is in pending state.
     */
    boolean isAnyPending() {
        for (Registration reg : registrations) {
            if (reg.controller().isPending()) {
                return true;
            }
        }
        return false;
    }

    /**
     * Handles a click in connection tool mode. Returns true if the tool was handled.
     */
    boolean handleClick(CanvasToolBar.Tool activeTool, double worldX, double worldY,
                        ModelCanvas canvas) {
        for (Registration reg : registrations) {
            if (reg.tool() == activeTool) {
                reg.clickAction().accept(canvas, new double[]{worldX, worldY});
                return true;
            }
        }
        return false;
    }

    /**
     * Updates rubber-band endpoints for all pending connection creations
     * during mouse move.
     */
    void updateRubberBands(double worldX, double worldY, ModelCanvas canvas) {
        boolean needsRedraw = false;
        for (Registration reg : registrations) {
            if (reg.controller().isPending()) {
                reg.controller().updateRubberBand(worldX, worldY);
                needsRedraw = true;
            }
        }
        if (needsRedraw) {
            canvas.requestRedraw();
        }
    }

    /**
     * Performs a connection click at the selected element's center, enabling
     * fully keyboard-driven connection creation (Enter to confirm source/target).
     */
    void handleEnter(CanvasToolBar.Tool activeTool, ModelCanvas canvas) {
        CanvasState canvasState = canvas.canvasState();
        Set<String> selection = canvasState.getSelection();
        if (selection.isEmpty()) {
            return;
        }

        String selected = selection.iterator().next();
        double worldX = canvasState.getX(selected);
        double worldY = canvasState.getY(selected);
        if (Double.isNaN(worldX) || Double.isNaN(worldY)) {
            return;
        }

        handleClick(activeTool, worldX, worldY, canvas);
    }

    /**
     * Cycles element selection forward (Tab) or backward (Shift+Tab),
     * and updates all rubber-bands to point at the newly selected element.
     */
    void handleTab(ModelCanvas canvas, boolean reverse) {
        CanvasState canvasState = canvas.canvasState();
        List<String> drawOrder = canvasState.getDrawOrder();
        if (drawOrder.isEmpty()) {
            return;
        }

        Set<String> selection = canvasState.getSelection();
        String current = selection.isEmpty() ? null : selection.iterator().next();

        int currentIndex = current != null ? drawOrder.indexOf(current) : -1;
        int nextIndex;
        if (reverse) {
            nextIndex = currentIndex <= 0 ? drawOrder.size() - 1 : currentIndex - 1;
        } else {
            nextIndex = currentIndex < 0 || currentIndex >= drawOrder.size() - 1
                    ? 0 : currentIndex + 1;
        }

        String next = drawOrder.get(nextIndex);
        canvasState.select(next);

        // Update rubber-bands to point at the newly selected element
        double worldX = canvasState.getX(next);
        double worldY = canvasState.getY(next);
        if (!Double.isNaN(worldX) && !Double.isNaN(worldY)) {
            for (Registration reg : registrations) {
                reg.controller().updateRubberBand(worldX, worldY);
            }
        }

        canvas.requestRedraw();
        canvas.fireStatusChanged();
    }

    /**
     * Cancels whichever creation is currently pending. Returns true if any
     * cancellation occurred.
     */
    boolean cancelPending(ModelCanvas canvas) {
        boolean cancelled = false;
        for (Registration reg : registrations) {
            if (reg.controller().isPending()) {
                reg.controller().cancel();
                cancelled = true;
            }
        }
        if (cancelled) {
            canvas.requestRedraw();
        }
        return cancelled;
    }
}
