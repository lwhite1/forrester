package systems.courant.sd.app.canvas;

import systems.courant.sd.model.def.ElementType;

import systems.courant.sd.app.canvas.controllers.CanvasContextMenuController;
import systems.courant.sd.app.canvas.controllers.InlineEditController;

/**
 * Named implementation of the inline-edit and context-menu callback interfaces
 * for {@link ModelCanvas}. Extracted to reduce anonymous class bulk in ModelCanvas.
 */
final class CanvasCallbacks implements InlineEditController.Callbacks,
        CanvasContextMenuController.Callbacks {

    private final ModelCanvas canvas;

    CanvasCallbacks(ModelCanvas canvas) {
        this.canvas = canvas;
    }

    // --- InlineEditController.Callbacks ---

    @Override
    public void applyRename(String oldName, String newName) {
        canvas.elements().applyRename(oldName, newName);
    }

    @Override
    public void saveAndSetFlowEquation(String name, String equation) {
        canvas.saveUndoState("Edit " + name + " equation");
        canvas.getEditor().setFlowEquation(name, equation);
        canvas.regenerateAndRedraw();
    }

    @Override
    public void saveAndSetAuxEquation(String name, String equation) {
        canvas.saveUndoState("Edit " + name + " equation");
        canvas.getEditor().setVariableEquation(name, equation);
        canvas.regenerateAndRedraw();
    }

    @Override
    public void saveAndSetCommentText(String name, String text) {
        canvas.saveUndoState("Edit " + name + " text");
        canvas.getEditor().setCommentText(name, text);
        canvas.canvasState().clearSize(name);
        canvas.regenerateAndRedraw();
    }

    @Override
    public void postEdit() {
        canvas.requestFocus();
        canvas.updateCursorViaDispatcher();
    }

    // --- CanvasContextMenuController.Callbacks ---

    @Override
    public void startInlineEdit(String name) {
        canvas.elements().startInlineEdit(name);
    }

    @Override
    public void deleteSelectedElements() {
        canvas.elements().deleteSelectedElements();
    }

    @Override
    public void cutSelection() {
        canvas.elements().cutSelection();
    }

    @Override
    public void copySelection() {
        canvas.elements().copySelection();
    }

    @Override
    public void pasteClipboard() {
        canvas.elements().pasteClipboard();
    }

    @Override
    public void selectAll() {
        canvas.elements().selectAll();
    }

    @Override
    public void switchTool(CanvasToolBar.Tool tool) {
        canvas.switchTool(tool);
    }

    @Override
    public void saveUndoState(String label) {
        canvas.saveUndoState(label);
    }

    @Override
    public void regenerateConnectors() {
        canvas.scheduleRegenerateConnectors();
    }

    @Override
    public void redraw() {
        canvas.requestRedraw();
    }

    @Override
    public void fireStatusChanged() {
        canvas.fireStatusChanged();
    }

    @Override
    public void clearSelectedConnection() {
        canvas.clearSelectedConnection();
    }

    @Override
    public void updateCursor() {
        canvas.updateCursorViaDispatcher();
    }

    @Override
    public String createElementAt(double wx, double wy, CanvasToolBar.Tool tool) {
        return canvas.elements().createElementAtForCallback(wx, wy, tool);
    }

    @Override
    public boolean deleteConnection(ConnectionId conn, boolean isCausal) {
        return canvas.elements().deleteConnectionForCallback(conn, isCausal);
    }

    @Override
    public boolean canPaste() {
        return canvas.elements().canPasteForCallback();
    }

    @Override
    public void classifyCldVariable(String name, ElementType type) {
        canvas.elements().classifyCldVariable(name, type);
    }

    @Override
    public void convertVariableToComment(String variableName) {
        canvas.elements().convertVariableToComment(variableName);
    }

    @Override
    public void drillInto(String moduleName) {
        canvas.navigation().drillInto(moduleName);
    }

    @Override
    public void openDefinePortsDialog(String moduleName) {
        canvas.navigation().openDefinePortsDialog(moduleName);
    }

    @Override
    public void openBindingsDialog(String moduleName) {
        canvas.navigation().openBindingsDialog(moduleName);
    }

    @Override
    public void traceUpstream(String name) {
        canvas.analysis().traceUpstream(name);
    }

    @Override
    public void traceDownstream(String name) {
        canvas.analysis().traceDownstream(name);
    }

    @Override
    public void showWhereUsed(String name) {
        canvas.analysis().showWhereUsed(name);
    }

    @Override
    public void showUses(String name) {
        canvas.analysis().showUses(name);
    }
}
