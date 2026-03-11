package systems.courant.shrewd.app.canvas;

import systems.courant.shrewd.model.def.ElementType;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Handles element selection, creation, deletion, copy/cut/paste operations.
 * Delegates clipboard work to {@link CopyPasteController}.
 */
final class SelectionController {

    private final CopyPasteController copyPaste;

    SelectionController(CopyPasteController copyPaste) {
        this.copyPaste = copyPaste;
    }

    /**
     * Creates a new element at the given world coordinates based on the active tool.
     * Returns the created element name, or null if nothing was created.
     */
    String createElementAt(double worldX, double worldY,
                           CanvasToolBar.Tool activeTool,
                           ModelEditor editor, CanvasState canvasState,
                           Runnable saveUndo) {
        if (editor == null) {
            return null;
        }

        saveUndo.run();

        String name;
        ElementType type;

        switch (activeTool) {
            case PLACE_STOCK -> {
                name = editor.addStock();
                type = ElementType.STOCK;
            }
            case PLACE_AUX -> {
                name = editor.addAux();
                type = ElementType.AUX;
            }
            case PLACE_MODULE -> {
                name = editor.addModule();
                type = ElementType.MODULE;
            }
            case PLACE_LOOKUP -> {
                name = editor.addLookup();
                type = ElementType.LOOKUP;
            }
            case PLACE_CLD_VARIABLE -> {
                name = editor.addCldVariable();
                type = ElementType.CLD_VARIABLE;
            }
            case PLACE_COMMENT -> {
                name = editor.addComment();
                type = ElementType.COMMENT;
            }
            default -> {
                return null;
            }
        }

        canvasState.addElement(name, type, worldX, worldY);
        if (type == ElementType.CLD_VARIABLE) {
            double w = LayoutMetrics.cldVarWidthForName(name);
            canvasState.setSize(name, w, LayoutMetrics.CLD_VAR_HEIGHT);
        }
        canvasState.clearSelection();
        canvasState.select(name);
        return name;
    }

    /**
     * Deletes all currently selected elements from the model and canvas.
     */
    void deleteSelected(ModelEditor editor, CanvasState canvasState, Runnable saveUndo) {
        if (editor == null || canvasState.getSelection().isEmpty()) {
            return;
        }

        saveUndo.run();

        List<String> toDelete = new ArrayList<>(canvasState.getSelection());
        for (String name : toDelete) {
            editor.removeElement(name);
            canvasState.removeElement(name);
        }
    }

    boolean canPaste() {
        return copyPaste.hasContent();
    }

    /**
     * Copies the current selection to the clipboard.
     */
    void copy(ModelEditor editor, CanvasState canvasState) {
        if (editor == null) {
            return;
        }
        copyPaste.copy(canvasState, editor);
    }

    /**
     * Cuts the current selection: copies to clipboard, then deletes.
     */
    void cut(ModelEditor editor, CanvasState canvasState, Runnable saveUndo) {
        if (editor == null || canvasState.getSelection().isEmpty()) {
            return;
        }
        copyPaste.copy(canvasState, editor);
        deleteSelected(editor, canvasState, saveUndo);
    }

    /**
     * Pastes clipboard contents. Returns the set of reference names that were
     * replaced with 0 (empty if none).
     */
    Set<String> paste(ModelEditor editor, CanvasState canvasState, Runnable saveUndo) {
        if (editor == null || !copyPaste.hasContent()) {
            return Set.of();
        }

        saveUndo.run();

        CopyPasteController.PasteResult result = copyPaste.paste(canvasState, editor);
        if (result.pastedNames().isEmpty()) {
            return Set.of();
        }

        canvasState.clearSelection();
        for (String name : result.pastedNames()) {
            canvasState.addToSelection(name);
        }

        return result.replacedReferences();
    }

    /**
     * Deletes the currently selected connection (info link or causal link).
     *
     * @return true if a connection was deleted
     */
    boolean deleteConnection(ConnectionId connection, boolean isCausalLink,
                             ModelEditor editor, Runnable saveUndo) {
        if (editor == null || connection == null) {
            return false;
        }

        saveUndo.run();
        if (isCausalLink) {
            editor.removeCausalLink(connection.from(), connection.to());
        } else {
            editor.removeConnectionReference(connection.from(), connection.to());
        }
        return true;
    }

    /**
     * Applies a rename to both the model editor and canvas state.
     *
     * @return true if the rename was applied
     */
    boolean applyRename(String oldName, String newName,
                        ModelEditor editor, CanvasState canvasState, Runnable saveUndo) {
        if (oldName.equals(newName) || editor.hasElement(newName)) {
            return false;
        }
        saveUndo.run();
        if (!editor.renameElement(oldName, newName)) {
            return false;
        }
        canvasState.renameElement(oldName, newName);
        if (canvasState.getType(newName).orElse(null) == ElementType.CLD_VARIABLE) {
            double w = LayoutMetrics.cldVarWidthForName(newName);
            canvasState.setSize(newName, w, LayoutMetrics.CLD_VAR_HEIGHT);
        }
        return true;
    }

    /**
     * Classifies a CLD variable as a concrete element type.
     *
     * @return true if the classification was applied
     */
    boolean classifyCldVariable(String name, ElementType targetType,
                                ModelEditor editor, CanvasState canvasState,
                                Runnable saveUndo) {
        if (editor == null) {
            return false;
        }
        saveUndo.run();
        if (editor.classifyCldVariable(name, targetType)) {
            canvasState.setType(name, targetType);
            return true;
        }
        return false;
    }

    /**
     * Selects a single element by name, clearing the current selection,
     * and pans the viewport to center the element on screen.
     */
    void selectAndCenter(String name, CanvasState canvasState, Viewport viewport,
                         double canvasWidth, double canvasHeight) {
        canvasState.clearSelection();
        canvasState.select(name);

        double worldX = canvasState.getX(name);
        double worldY = canvasState.getY(name);
        if (!Double.isNaN(worldX) && !Double.isNaN(worldY)) {
            double canvasCenterX = canvasWidth / 2.0;
            double canvasCenterY = canvasHeight / 2.0;
            double scale = viewport.getScale();
            viewport.restoreState(
                    canvasCenterX - worldX * scale,
                    canvasCenterY - worldY * scale,
                    scale);
        }
    }
}
