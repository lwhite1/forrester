package systems.courant.sd.app.canvas;

import systems.courant.sd.io.json.ModelDefinitionSerializer;
import systems.courant.sd.model.def.ModelDefinition;
import systems.courant.sd.model.def.ViewDef;

import net.jpountz.lz4.LZ4Compressor;
import net.jpountz.lz4.LZ4Factory;
import net.jpountz.lz4.LZ4FastDecompressor;

import java.nio.charset.StandardCharsets;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Snapshot-based undo/redo manager. Stores LZ4-compressed JSON snapshots
 * on two stacks to reduce memory usage for large models. Each entry carries
 * an action label for display in the undo history panel.
 *
 * <p>Serialization and compression run on a background thread to avoid
 * blocking the FX thread during drag operations. The snapshot (immutable
 * model state) is captured on the FX thread before being handed off.
 *
 * <p>Pushing a new undo state clears the redo stack. The undo stack
 * is capped at {@value MAX_UNDO} entries.
 */
public class UndoManager implements AutoCloseable {

    static final int MAX_UNDO = 100;

    private static final LZ4Factory LZ4 = LZ4Factory.fastestInstance();
    private static final LZ4Compressor COMPRESSOR = LZ4.fastCompressor();
    private static final LZ4FastDecompressor DECOMPRESSOR = LZ4.fastDecompressor();

    /**
     * Immutable snapshot of both model data and canvas layout.
     */
    public record Snapshot(ModelDefinition model, ViewDef view) {}

    /**
     * LZ4-compressed JSON bytes. Stores the original (uncompressed) length
     * for decompression.
     */
    record CompressedData(byte[] data, int originalLength) {}

    /**
     * An undo/redo entry whose compression may still be in progress.
     * The label is available immediately; the compressed data arrives
     * via a future that completes on a background thread. The raw snapshot
     * is kept until compression completes to avoid blocking the FX thread.
     */
    static final class UndoEntry {
        private final CompletableFuture<CompressedData> future;
        private final String label;
        final AtomicReference<Snapshot> rawSnapshot;

        UndoEntry(CompletableFuture<CompressedData> future, String label, Snapshot rawSnapshot) {
            this.future = future;
            this.label = label;
            this.rawSnapshot = new AtomicReference<>(rawSnapshot);
        }

        CompletableFuture<CompressedData> future() { return future; }
        String label() { return label; }
    }

    private static final ModelDefinitionSerializer SERIALIZER = new ModelDefinitionSerializer();

    private final Deque<UndoEntry> undoStack = new ArrayDeque<>();
    private final Deque<UndoEntry> redoStack = new ArrayDeque<>();
    private final ExecutorService compressor =
            Executors.newSingleThreadExecutor(r -> {
                Thread t = new Thread(r, "undo-compressor");
                t.setDaemon(true);
                return t;
            });

    /**
     * Saves the current state before a mutation. Clears the redo stack and
     * enforces the maximum undo depth.
     *
     * @param current the state snapshot captured before the mutation
     * @param label   a human-readable description of the action about to be performed
     */
    public void pushUndo(Snapshot current, String label) {
        undoStack.push(compressAsync(current, label));
        cancelAll(redoStack);
        if (undoStack.size() > MAX_UNDO) {
            UndoEntry evicted = undoStack.removeLast();
            evicted.future().cancel(false);
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
     * Saves the current state tentatively, without clearing the redo stack.
     * Use this when the operation may be rejected or may fail, so that
     * redo history is preserved if the undo entry is later discarded.
     *
     * <p>If the operation succeeds, call {@link #confirmLastUndo()} to
     * clear the redo stack. If it fails, call {@link #discardLastUndo()}.
     *
     * @param current the state snapshot captured before the mutation
     * @param label   a human-readable description of the action
     */
    public void pushUndoTentative(Snapshot current, String label) {
        undoStack.push(compressAsync(current, label));
        if (undoStack.size() > MAX_UNDO) {
            UndoEntry evicted = undoStack.removeLast();
            evicted.future().cancel(false);
        }
    }

    /**
     * Confirms the most recent tentative undo entry by clearing the redo
     * stack. Called after {@link #pushUndoTentative} when the operation
     * succeeds and the redo history should be invalidated.
     */
    public void confirmLastUndo() {
        cancelAll(redoStack);
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
        redoStack.push(compressAsync(current, label));
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
        undoStack.push(compressAsync(current, label));
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
        redoStack.push(compressAsync(current, "Current"));
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
        for (UndoEntry entry : undoStack) {
            labels.add(entry.label());
        }
        return labels;
    }

    /**
     * Returns the action labels on the redo stack, most recent first.
     */
    public List<String> redoLabels() {
        List<String> labels = new ArrayList<>(redoStack.size());
        for (UndoEntry entry : redoStack) {
            labels.add(entry.label());
        }
        return labels;
    }

    /**
     * Pushes a raw entry onto the undo stack. Package-private for testing.
     */
    void pushEntry(UndoEntry entry) {
        undoStack.push(entry);
        cancelAll(redoStack);
        if (undoStack.size() > MAX_UNDO) {
            UndoEntry evicted = undoStack.removeLast();
            evicted.future().cancel(false);
        }
    }

    /**
     * Clears both undo and redo stacks.
     */
    public void clear() {
        cancelAll(undoStack);
        cancelAll(redoStack);
    }

    private static void cancelAll(Deque<UndoEntry> stack) {
        for (UndoEntry entry : stack) {
            entry.future().cancel(false);
        }
        stack.clear();
    }

    /**
     * Discards the most recent undo entry without moving it to the redo stack.
     * Used when an operation saved undo state but was subsequently rejected
     * (e.g., a failed flow reconnection).
     */
    public void discardLastUndo() {
        if (!undoStack.isEmpty()) {
            UndoEntry entry = undoStack.pop();
            entry.future().cancel(false);
        }
    }

    /**
     * Shuts down the background compressor thread. Call when this manager
     * is no longer needed (e.g., when navigating out of a module).
     */
    @Override
    public void close() {
        compressor.shutdownNow();
    }

    /**
     * Returns true if this manager has been closed.
     */
    boolean isClosed() {
        return compressor.isShutdown();
    }

    private UndoEntry compressAsync(Snapshot snapshot, String label) {
        ModelDefinition model = snapshot.model();
        // Ensure the view is embedded in the model for serialization
        if (snapshot.view() != null && model.views().isEmpty()) {
            model = model.toBuilder()
                    .clearViews()
                    .view(snapshot.view())
                    .build();
        }
        ModelDefinition finalModel = model;
        CompletableFuture<CompressedData> future = CompletableFuture.supplyAsync(() -> {
            String json = SERIALIZER.toJson(finalModel);
            byte[] raw = json.getBytes(StandardCharsets.UTF_8);
            byte[] compressed = COMPRESSOR.compress(raw);
            return new CompressedData(compressed, raw.length);
        }, compressor);
        UndoEntry entry = new UndoEntry(future, label, snapshot);
        future.thenRun(() -> entry.rawSnapshot.set(null));
        return entry;
    }

    private static Snapshot decompress(UndoEntry entry) {
        // Atomically read-and-clear the raw snapshot. Using getAndSet(null)
        // ensures that at most one thread obtains the raw reference, fixing
        // the race where two concurrent decompress() calls could share it.
        Snapshot raw = entry.rawSnapshot.getAndSet(null);
        if (raw != null) {
            return raw;
        }
        // Prefer non-blocking getNow() to avoid stalling the FX thread.
        // Fall back to a bounded get() with a 5-second timeout to prevent
        // indefinite blocking if something goes wrong.
        CompressedData data;
        try {
            data = entry.future().getNow(null);
        } catch (java.util.concurrent.CompletionException e) {
            throw new IllegalStateException("Undo compression failed", e.getCause());
        }
        if (data == null) {
            try {
                data = entry.future().get(5, TimeUnit.SECONDS);
            } catch (TimeoutException e) {
                throw new IllegalStateException(
                        "Undo decompression timed out — compression did not complete within 5 seconds", e);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException("Undo decompression interrupted", e);
            } catch (ExecutionException e) {
                throw new IllegalStateException("Undo compression failed", e.getCause());
            }
        }
        byte[] rawBytes = new byte[data.originalLength()];
        DECOMPRESSOR.decompress(data.data(), 0, rawBytes, 0, data.originalLength());
        String json = new String(rawBytes, StandardCharsets.UTF_8);
        ModelDefinition model = SERIALIZER.fromJson(json);
        // The view is stored inside the model's views list
        ViewDef view = model.views().isEmpty() ? null : model.views().getFirst();
        return new Snapshot(model, view);
    }
}
