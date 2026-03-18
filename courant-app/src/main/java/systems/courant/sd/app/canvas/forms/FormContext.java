package systems.courant.sd.app.canvas.forms;

import javafx.scene.layout.GridPane;

import systems.courant.sd.app.canvas.ModelCanvas;
import systems.courant.sd.app.canvas.ModelEditor;

/**
 * Shared mutable context passed to {@link ElementForm} implementations.
 * Holds references to the canvas, editor, property grid, and the current element name.
 */
public class FormContext {

    private ModelCanvas canvas;
    private ModelEditor editor;
    private GridPane grid;
    private String elementName;
    private boolean updatingFields;
    private Runnable onFormRebuildRequested;
    private Runnable onOpenExpressionHelp;

    public ModelCanvas getCanvas() {
        return canvas;
    }

    public void setCanvas(ModelCanvas canvas) {
        this.canvas = canvas;
    }

    public ModelEditor getEditor() {
        return editor;
    }

    public void setEditor(ModelEditor editor) {
        this.editor = editor;
    }

    public GridPane getGrid() {
        return grid;
    }

    public void setGrid(GridPane grid) {
        this.grid = grid;
    }

    public String getElementName() {
        return elementName;
    }

    public void setElementName(String elementName) {
        this.elementName = elementName;
    }

    public boolean isUpdatingFields() {
        return updatingFields;
    }

    /**
     * Executes the given action with the reentrancy guard active.
     * Sets {@code updatingFields} to {@code true} before running the action and
     * guarantees it is reset to {@code false} afterwards, even if the action throws.
     */
    public void withUpdate(Runnable action) {
        updatingFields = true;
        try {
            action.run();
        } finally {
            updatingFields = false;
        }
    }

    public void setOnFormRebuildRequested(Runnable onFormRebuildRequested) {
        this.onFormRebuildRequested = onFormRebuildRequested;
    }

    public void setOnOpenExpressionHelp(Runnable onOpenExpressionHelp) {
        this.onOpenExpressionHelp = onOpenExpressionHelp;
    }

    public Runnable getOnOpenExpressionHelp() {
        return onOpenExpressionHelp;
    }

    public void requestFormRebuild() {
        if (onFormRebuildRequested != null) {
            onFormRebuildRequested.run();
        }
    }
}
