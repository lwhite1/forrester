package com.deathrayresearch.forrester.app.canvas;

import com.deathrayresearch.forrester.io.json.ModelDefinitionSerializer;
import com.deathrayresearch.forrester.model.def.ModelDefinition;
import com.deathrayresearch.forrester.model.def.ViewDef;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Optional;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

/**
 * Snapshot-based undo/redo manager. Stores GZIP-compressed JSON snapshots
 * on two stacks to reduce memory usage for large models. Each entry carries
 * an action label for display in the undo history panel.
 *
 * <p>Pushing a new undo state clears the redo stack. The undo stack
 * is capped at {@value MAX_UNDO} entries.
 */
public class UndoManager {

    static final int MAX_UNDO = 100;

    /**
     * Immutable snapshot of both model data and canvas layout.
     */
    public record Snapshot(ModelDefinition model, ViewDef view) {}

    /**
     * A compressed undo entry: GZIP-compressed JSON bytes plus a human-readable label.
     */
    record CompressedEntry(byte[] data, String label) {}

    private static final ModelDefinitionSerializer SERIALIZER = new ModelDefinitionSerializer();

    private final Deque<CompressedEntry> undoStack = new ArrayDeque<>();
    private final Deque<CompressedEntry> redoStack = new ArrayDeque<>();

    /**
     * Saves the current state before a mutation. Clears the redo stack and
     * enforces the maximum undo depth.
     *
     * @param current the state snapshot captured before the mutation
     * @param label   a human-readable description of the action about to be performed
     */
    public void pushUndo(Snapshot current, String label) {
        undoStack.push(compress(current, label));
        redoStack.clear();
        if (undoStack.size() > MAX_UNDO) {
            undoStack.removeLast();
        }
    }

    /**
     * Saves the current state before a mutation with a default label.
     *
     * @param current the state snapshot captured before the mutation
     */
    public void pushUndo(Snapshot current) {
        pushUndo(current, "Edit");
    }

    /**
     * Undoes one step: pops the previous state from the undo stack and pushes
     * the current state onto the redo stack.
     *
     * @param current the current state to preserve for redo
     * @param label   label for the redo entry (describes what was just undone)
     * @return the previous snapshot to restore, or empty if nothing to undo
     */
    public Optional<Snapshot> undo(Snapshot current, String label) {
        if (undoStack.isEmpty()) {
            return Optional.empty();
        }
        redoStack.push(compress(current, label));
        return Optional.of(decompress(undoStack.pop()));
    }

    /**
     * Undoes one step with a default label.
     */
    public Optional<Snapshot> undo(Snapshot current) {
        return undo(current, "Undo");
    }

    /**
     * Redoes one step: pops the next state from the redo stack and pushes
     * the current state onto the undo stack.
     *
     * @param current the current state to preserve for undo
     * @param label   label for the undo entry
     * @return the next snapshot to restore, or empty if nothing to redo
     */
    public Optional<Snapshot> redo(Snapshot current, String label) {
        if (redoStack.isEmpty()) {
            return Optional.empty();
        }
        undoStack.push(compress(current, label));
        return Optional.of(decompress(redoStack.pop()));
    }

    /**
     * Redoes one step with a default label.
     */
    public Optional<Snapshot> redo(Snapshot current) {
        return redo(current, "Redo");
    }

    /**
     * Undoes multiple steps at once, jumping to the entry at the given depth
     * (0 = most recent undo entry, 1 = one before that, etc.).
     *
     * @param current the current state to preserve for redo
     * @param depth   the zero-based index into the undo stack to jump to
     * @return the snapshot at the target depth, or empty if depth is out of range
     */
    public Optional<Snapshot> undoTo(Snapshot current, int depth) {
        if (depth < 0 || depth >= undoStack.size()) {
            return Optional.empty();
        }
        // Push current to redo, then move entries from undo to redo
        redoStack.push(compress(current, "Current"));
        for (int i = 0; i < depth; i++) {
            redoStack.push(undoStack.pop());
        }
        return Optional.of(decompress(undoStack.pop()));
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
     * Returns the number of entries on the undo stack.
     */
    public int undoDepth() {
        return undoStack.size();
    }

    /**
     * Returns the action labels on the undo stack, most recent first.
     */
    public List<String> undoLabels() {
        List<String> labels = new ArrayList<>(undoStack.size());
        for (CompressedEntry entry : undoStack) {
            labels.add(entry.label());
        }
        return labels;
    }

    /**
     * Returns the action labels on the redo stack, most recent first.
     */
    public List<String> redoLabels() {
        List<String> labels = new ArrayList<>(redoStack.size());
        for (CompressedEntry entry : redoStack) {
            labels.add(entry.label());
        }
        return labels;
    }

    /**
     * Clears both undo and redo stacks.
     */
    public void clear() {
        undoStack.clear();
        redoStack.clear();
    }

    private static CompressedEntry compress(Snapshot snapshot, String label) {
        ModelDefinition model = snapshot.model();
        // Ensure the view is embedded in the model for serialization
        if (snapshot.view() != null && model.views().isEmpty()) {
            model = new ModelDefinition(
                    model.name(), model.comment(), model.moduleInterface(),
                    model.stocks(), model.flows(), model.auxiliaries(),
                    model.constants(), model.lookupTables(), model.modules(),
                    model.subscripts(), model.cldVariables(), model.causalLinks(),
                    List.of(snapshot.view()), model.defaultSimulation());
        }
        String json = SERIALIZER.toJson(model);
        byte[] compressed = gzipCompress(json.getBytes(StandardCharsets.UTF_8));
        return new CompressedEntry(compressed, label);
    }

    private static Snapshot decompress(CompressedEntry entry) {
        byte[] raw = gzipDecompress(entry.data());
        String json = new String(raw, StandardCharsets.UTF_8);
        ModelDefinition model = SERIALIZER.fromJson(json);
        // The view is stored inside the model's views list
        ViewDef view = model.views().isEmpty() ? null : model.views().getFirst();
        return new Snapshot(model, view);
    }

    private static byte[] gzipCompress(byte[] data) {
        ByteArrayOutputStream bos = new ByteArrayOutputStream(data.length / 4);
        try (GZIPOutputStream gzip = new GZIPOutputStream(bos)) {
            gzip.write(data);
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to compress undo snapshot", e);
        }
        return bos.toByteArray();
    }

    private static byte[] gzipDecompress(byte[] compressed) {
        ByteArrayOutputStream bos = new ByteArrayOutputStream(compressed.length * 4);
        try (GZIPInputStream gzip = new GZIPInputStream(
                new ByteArrayInputStream(compressed))) {
            byte[] buf = new byte[4096];
            int n;
            while ((n = gzip.read(buf)) >= 0) {
                bos.write(buf, 0, n);
            }
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to decompress undo snapshot", e);
        }
        return bos.toByteArray();
    }
}
