package com.deathrayresearch.forrester.app.canvas;

import com.deathrayresearch.forrester.model.def.ModelDefinition;
import com.deathrayresearch.forrester.model.def.ViewDef;

import java.util.ArrayDeque;
import java.util.Deque;

/**
 * Snapshot-based undo/redo manager. Stores immutable state pairs (model + view)
 * on two stacks. Pushing a new undo state clears the redo stack. The undo stack
 * is capped at {@value MAX_UNDO} entries.
 */
public class UndoManager {

    static final int MAX_UNDO = 100;

    /**
     * Immutable snapshot of both model data and canvas layout.
     */
    public record Snapshot(ModelDefinition model, ViewDef view) {}

    private final Deque<Snapshot> undoStack = new ArrayDeque<>();
    private final Deque<Snapshot> redoStack = new ArrayDeque<>();

    /**
     * Saves the current state before a mutation. Clears the redo stack and
     * enforces the maximum undo depth.
     *
     * @param current the state snapshot captured before the mutation
     */
    public void pushUndo(Snapshot current) {
        undoStack.push(current);
        redoStack.clear();
        if (undoStack.size() > MAX_UNDO) {
            undoStack.removeLast();
        }
    }

    /**
     * Undoes one step: pops the previous state from the undo stack and pushes
     * the current state onto the redo stack.
     *
     * @param current the current state to preserve for redo
     * @return the previous snapshot to restore, or null if nothing to undo
     */
    public Snapshot undo(Snapshot current) {
        if (undoStack.isEmpty()) {
            return null;
        }
        redoStack.push(current);
        return undoStack.pop();
    }

    /**
     * Redoes one step: pops the next state from the redo stack and pushes
     * the current state onto the undo stack.
     *
     * @param current the current state to preserve for undo
     * @return the next snapshot to restore, or null if nothing to redo
     */
    public Snapshot redo(Snapshot current) {
        if (redoStack.isEmpty()) {
            return null;
        }
        undoStack.push(current);
        return redoStack.pop();
    }

    /**
     * Returns true if there is at least one state on the undo stack.
     */
    public boolean canUndo() {
        return !undoStack.isEmpty();
    }

    /**
     * Returns true if there is at least one state on the redo stack.
     */
    public boolean canRedo() {
        return !redoStack.isEmpty();
    }

    /**
     * Clears both undo and redo stacks.
     */
    public void clear() {
        undoStack.clear();
        redoStack.clear();
    }
}
