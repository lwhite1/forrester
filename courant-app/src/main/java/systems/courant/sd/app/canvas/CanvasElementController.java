package systems.courant.sd.app.canvas;

import systems.courant.sd.model.def.ElementType;

import java.util.Set;

import systems.courant.sd.app.canvas.controllers.CausalLinkCreationController;
import systems.courant.sd.app.canvas.controllers.FlowCreationController;
import systems.courant.sd.app.canvas.controllers.InfoLinkCreationController;

/**
 * Facade encapsulating element mutation operations: create, delete, rename,
 * copy/cut/paste, connection creation, and inline editing.
 * Extracted from {@link ModelCanvas} to isolate element CRUD concerns.
 */
public final class CanvasElementController {

    private final ModelCanvas canvas;

    CanvasElementController(ModelCanvas canvas) {
        this.canvas = canvas;
    }

    public void deleteSelectedElements() {
        if (canvas.editor == null) {
            return;
        }
        canvas.selectionController.deleteSelected(canvas.editor, canvas.canvasState(),
                () -> canvas.saveUndoState("Delete " + canvas.undo().describeSelection()));
        canvas.regenerateConnectors();
        canvas.requestRedraw();
        canvas.fireStatusChanged();
        canvas.inputDispatcher.updateCursor(canvas);
    }

    public void renameElement(String oldName, String newName) {
        applyRename(oldName, newName);
    }

    public void triggerBindingConfig(String moduleName) {
        canvas.navigation().openBindingsDialog(moduleName);
    }

    public void selectAll() {
        canvas.canvasState().selectAll();
        canvas.requestRedraw();
        canvas.fireStatusChanged();
    }

    public void selectElement(String name) {
        canvas.selectionController.selectAndCenter(name, canvas.canvasState(),
                canvas.viewport(), canvas.getWidth(), canvas.getHeight());
        canvas.fireStatusChanged();
        canvas.requestRedraw();
    }

    public void copySelection() {
        if (canvas.editor == null) {
            return;
        }
        canvas.selectionController.copy(canvas.editor, canvas.canvasState());
    }

    public void cutSelection() {
        if (canvas.editor == null) {
            return;
        }
        canvas.selectionController.cut(canvas.editor, canvas.canvasState(),
                () -> canvas.saveUndoState("Cut " + canvas.undo().describeSelection()));
        canvas.regenerateConnectors();
        canvas.requestRedraw();
        canvas.fireStatusChanged();
        canvas.inputDispatcher.updateCursor(canvas);
    }

    public Set<String> pasteClipboard() {
        if (canvas.editor == null) {
            return Set.of();
        }
        double centerWorldX = canvas.viewport().toWorldX(canvas.getWidth() / 2.0);
        double centerWorldY = canvas.viewport().toWorldY(canvas.getHeight() / 2.0);
        var viewportCenter = new CanvasState.Position(centerWorldX, centerWorldY);
        Set<String> replaced = canvas.selectionController.paste(
                canvas.editor, canvas.canvasState(),
                () -> canvas.saveUndoState("Paste elements"),
                viewportCenter);
        if (replaced == null) {
            return Set.of();
        }
        canvas.regenerateConnectors();
        canvas.requestRedraw();
        canvas.fireStatusChanged();
        if (!replaced.isEmpty() && canvas.onPasteWarning != null) {
            canvas.onPasteWarning.accept(replaced);
        }
        return replaced;
    }

    public void deleteSelectedOrConnection() {
        if (canvas.editor == null) {
            return;
        }
        if (canvas.selectedConnection != null && canvas.canvasState().getSelection().isEmpty()) {
            if (canvas.selectionController.deleteConnection(
                    canvas.selectedConnection, canvas.selectedIsCausalLink, canvas.editor,
                    () -> canvas.saveUndoState("Delete " + canvas.selectedConnection.from()
                            + " \u2192 " + canvas.selectedConnection.to() + " connection"))) {
                if (!canvas.selectedIsCausalLink) {
                    canvas.connectors = canvas.editor.generateConnectors();
                }
                canvas.clearSelectedConnection();
                canvas.invalidateAnalysis();
                canvas.requestRedraw();
                canvas.fireStatusChanged();
                canvas.inputDispatcher.updateCursor(canvas);
            }
        } else {
            deleteSelectedElements();
        }
    }

    public void handleFlowClick(double worldX, double worldY) {
        if (canvas.editor == null) {
            return;
        }
        var snapshot = canvas.undo().captureSnapshot();
        FlowCreationController.FlowResult result = canvas.flowCreation.handleClick(
                worldX, worldY, canvas.canvasState(), canvas.editor);
        if (result.isCreated()) {
            canvas.undo().pushUndoSnapshot(snapshot, "Add flow");
            canvas.regenerateConnectors();
            canvas.canvasState().clearSelection();
            canvas.canvasState().select(result.flowName());
        }
        canvas.requestRedraw();
        canvas.fireStatusChanged();
    }

    public void handleCausalLinkClick(double worldX, double worldY) {
        if (canvas.editor == null) {
            return;
        }
        var snapshot = canvas.undo().captureSnapshot();
        CausalLinkCreationController.LinkResult result = canvas.causalLinkCreation.handleClick(
                worldX, worldY, canvas.canvasState(), canvas.editor);
        if (result.isCreated()) {
            canvas.undo().pushUndoSnapshot(snapshot, "Add causal link");
            canvas.regenerateConnectors();
        }
        canvas.requestRedraw();
        canvas.fireStatusChanged();
    }

    public void handleInfoLinkClick(double worldX, double worldY) {
        if (canvas.editor == null) {
            return;
        }
        var snapshot = canvas.undo().captureSnapshot();
        InfoLinkCreationController.LinkResult result = canvas.infoLinkCreation.handleClick(
                worldX, worldY, canvas.canvasState(), canvas.editor);
        if (result.isCreated()) {
            canvas.undo().pushUndoSnapshot(snapshot, "Bind module port");
            canvas.regenerateConnectors();
        }
        canvas.requestRedraw();
        canvas.fireStatusChanged();
    }

    public void createElementAt(double worldX, double worldY) {
        if (canvas.editor == null) {
            return;
        }
        String name = canvas.selectionController.createElementAt(
                worldX, worldY, canvas.activeTool, canvas.editor, canvas.canvasState(),
                () -> canvas.saveUndoState("Add " + canvas.activeTool.label()));
        if (name != null) {
            canvas.regenerateConnectors();
            canvas.requestRedraw();
            canvas.fireStatusChanged();
            if (canvas.activeTool == CanvasToolBar.Tool.PLACE_COMMENT) {
                startInlineEdit(name);
            }
        }
    }

    public void startInlineEdit(String elementName) {
        if (canvas.editor == null) {
            return;
        }
        canvas.inlineEdit.startEdit(elementName, canvas.canvasState(),
                canvas.editor, canvas.viewport(), canvas.callbacks);
    }

    void applyRename(String oldName, String newName) {
        if (canvas.editor == null) {
            return;
        }
        if (canvas.selectionController.applyRename(oldName, newName, canvas.editor,
                canvas.canvasState(),
                () -> canvas.saveUndoState("Rename " + oldName + " \u2192 " + newName))) {
            canvas.regenerateAndRedraw();
        }
    }

    void classifyCldVariable(String name, ElementType targetType) {
        if (canvas.editor == null) {
            return;
        }
        if (canvas.selectionController.classifyCldVariable(name, targetType, canvas.editor,
                canvas.canvasState(),
                () -> canvas.saveUndoState(
                        "Classify " + name + " as " + targetType.name().toLowerCase()))) {
            canvas.regenerateAndRedraw();
            canvas.fireStatusChanged();
        }
    }

    // --- Context menu wrappers ---

    public void showElementContextMenu(String elementName, double screenX, double screenY) {
        canvas.contextMenuController.showElementContextMenu(
                canvas, elementName, canvas.canvasState(), screenX, screenY, canvas.callbacks);
    }

    public void showGeneralElementContextMenu(String elementName,
                                               double screenX, double screenY) {
        canvas.contextMenuController.showGeneralElementContextMenu(
                canvas, elementName, canvas.canvasState(), screenX, screenY, canvas.callbacks);
    }

    public void showCausalLinkContextMenu(ConnectionId link,
                                           double screenX, double screenY) {
        if (canvas.editor == null) {
            return;
        }
        canvas.contextMenuController.showCausalLinkContextMenu(
                canvas, link, canvas.editor, screenX, screenY, canvas.callbacks);
    }

    public void showInfoLinkContextMenu(ConnectionId link,
                                         double screenX, double screenY) {
        canvas.contextMenuController.showInfoLinkContextMenu(
                canvas, link, screenX, screenY, canvas.callbacks);
    }

    public void showCanvasContextMenu(double worldX, double worldY,
                                       double screenX, double screenY) {
        canvas.contextMenuController.showCanvasContextMenu(
                canvas, worldX, worldY, screenX, screenY, canvas.callbacks);
    }

    // --- Callback helpers ---

    String createElementAtForCallback(double wx, double wy, CanvasToolBar.Tool tool) {
        String name = canvas.selectionController.createElementAt(
                wx, wy, tool, canvas.editor, canvas.canvasState(),
                () -> canvas.saveUndoState("Add " + tool.label()));
        if (name != null) {
            canvas.regenerateConnectors();
            canvas.requestRedraw();
            canvas.fireStatusChanged();
        }
        return name;
    }

    boolean deleteConnectionForCallback(ConnectionId conn, boolean isCausal) {
        return canvas.selectionController.deleteConnection(conn, isCausal, canvas.editor,
                () -> canvas.saveUndoState(
                        "Delete " + conn.from() + " \u2192 " + conn.to() + " connection"));
    }

    boolean canPasteForCallback() {
        return canvas.selectionController.canPaste();
    }
}
