package systems.courant.sd.app.canvas;

import systems.courant.sd.model.def.ModelDefinition;
import systems.courant.sd.model.def.ViewDef;

import java.util.Set;

/**
 * Facade encapsulating undo/redo state management: snapshot capture,
 * undo/redo operations, and undo manager lifecycle.
 * Extracted from {@link ModelCanvas} to isolate undo concerns.
 */
public final class CanvasUndoFacade {

    private final ModelCanvas canvas;

    CanvasUndoFacade(ModelCanvas canvas) {
        this.canvas = canvas;
    }

    public void setUndoManager(UndoManager undoManager) {
        canvas.undoManager = undoManager;
    }

    public UndoManager getUndoManager() {
        return canvas.undoManager;
    }

    UndoManager.Snapshot captureSnapshot() {
        return new UndoManager.Snapshot(
                canvas.editor.toModelDefinition(canvas.canvasState().toViewDef()),
                canvas.canvasState().toViewDef());
    }

    public void saveUndoState(String label) {
        if (canvas.undoManager != null && canvas.editor != null) {
            canvas.undoManager.pushUndo(captureSnapshot(), label);
        }
    }

    public void saveUndoStateTentative(String label) {
        if (canvas.undoManager != null && canvas.editor != null) {
            canvas.undoManager.pushUndoTentative(captureSnapshot(), label);
        }
    }

    void pushUndoSnapshot(UndoManager.Snapshot snapshot, String label) {
        if (canvas.undoManager != null) {
            canvas.undoManager.pushUndo(snapshot, label);
        }
    }

    String describeSelection() {
        Set<String> sel = canvas.canvasState().getSelection();
        if (sel.isEmpty()) {
            return "elements";
        }
        if (sel.size() == 1) {
            return sel.iterator().next();
        }
        return sel.size() + " elements";
    }

    private void restoreSnapshot(UndoManager.Snapshot snapshot) {
        canvas.editor.loadFrom(snapshot.model());
        canvas.canvasState().loadFrom(snapshot.view());
        canvas.connectors = canvas.editor.generateConnectors();
        canvas.invalidateAnalysis();
        canvas.requestRedraw();
        canvas.fireStatusChanged();
    }

    public void performUndo() {
        if (canvas.undoManager == null || canvas.editor == null) {
            return;
        }
        canvas.undoManager.undo(captureSnapshot()).ifPresent(this::restoreSnapshot);
    }

    public void performRedo() {
        if (canvas.undoManager == null || canvas.editor == null) {
            return;
        }
        canvas.undoManager.redo(captureSnapshot()).ifPresent(this::restoreSnapshot);
    }

    public void performUndoTo(int depth) {
        if (canvas.undoManager == null || canvas.editor == null) {
            return;
        }
        canvas.undoManager.undoTo(captureSnapshot(), depth).ifPresent(this::restoreSnapshot);
    }
}
