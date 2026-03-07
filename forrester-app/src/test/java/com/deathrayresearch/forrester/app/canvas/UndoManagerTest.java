package com.deathrayresearch.forrester.app.canvas;

import com.deathrayresearch.forrester.model.def.ModelDefinition;
import com.deathrayresearch.forrester.model.def.ModelDefinitionBuilder;
import com.deathrayresearch.forrester.model.def.ViewDef;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("UndoManager")
class UndoManagerTest {

    private UndoManager manager;

    @BeforeEach
    void setUp() {
        manager = new UndoManager();
    }

    private static UndoManager.Snapshot snapshot(String name) {
        ModelDefinition model = new ModelDefinitionBuilder()
                .name(name)
                .build();
        ViewDef view = new ViewDef("Main", List.of(), List.of(), List.of());
        return new UndoManager.Snapshot(model, view);
    }

    private static void assertSnapshotName(UndoManager.Snapshot snapshot, String expectedName) {
        assertThat(snapshot).isNotNull();
        assertThat(snapshot.model().name()).isEqualTo(expectedName);
    }

    @Nested
    @DisplayName("pushUndo")
    class PushUndo {

        @Test
        void shouldMakeCanUndoTrue() {
            manager.pushUndo(snapshot("S1"));

            assertThat(manager.canUndo()).isTrue();
        }

        @Test
        void shouldClearRedoStack() {
            manager.pushUndo(snapshot("S1"));
            // Undo to move S1 to redo stack
            manager.undo(snapshot("S2"));
            assertThat(manager.canRedo()).isTrue();

            // New push should clear redo
            manager.pushUndo(snapshot("S3"));
            assertThat(manager.canRedo()).isFalse();
        }

        @Test
        void shouldEnforceMaximumCapacity() {
            manager.pushUndo(snapshot("First"));
            for (int i = 1; i < UndoManager.MAX_UNDO; i++) {
                manager.pushUndo(snapshot("S" + i));
            }
            assertThat(manager.canUndo()).isTrue();

            // Push one more — should evict the oldest (First)
            manager.pushUndo(snapshot("overflow"));

            // Drain all undo entries
            int count = 0;
            UndoManager.Snapshot current = snapshot("drain");
            while (manager.canUndo()) {
                current = manager.undo(current).orElseThrow();
                count++;
            }
            assertThat(count).isEqualTo(UndoManager.MAX_UNDO);
            // The oldest entry should be "S1", not "First" (evicted)
            assertSnapshotName(current, "S1");
        }
    }

    @Nested
    @DisplayName("undo")
    class Undo {

        @Test
        void shouldReturnPreviousSnapshot() {
            manager.pushUndo(snapshot("S1"));

            UndoManager.Snapshot result = manager.undo(snapshot("Current")).orElseThrow();

            assertSnapshotName(result, "S1");
        }

        @Test
        void shouldPushCurrentToRedoStack() {
            manager.pushUndo(snapshot("S1"));

            manager.undo(snapshot("Current"));

            assertThat(manager.canRedo()).isTrue();
            UndoManager.Snapshot redone = manager.redo(snapshot("Dummy")).orElseThrow();
            assertSnapshotName(redone, "Current");
        }

        @Test
        void shouldReturnEmptyWhenEmpty() {
            Optional<UndoManager.Snapshot> result = manager.undo(snapshot("Current"));

            assertThat(result).isEmpty();
        }

        @Test
        void shouldNotPushToRedoWhenEmpty() {
            manager.undo(snapshot("Current"));

            assertThat(manager.canRedo()).isFalse();
        }
    }

    @Nested
    @DisplayName("redo")
    class Redo {

        @Test
        void shouldReturnNextSnapshot() {
            manager.pushUndo(snapshot("S1"));
            manager.undo(snapshot("Current"));

            UndoManager.Snapshot result = manager.redo(snapshot("AfterUndo")).orElseThrow();

            assertSnapshotName(result, "Current");
        }

        @Test
        void shouldPushCurrentToUndoStack() {
            manager.pushUndo(snapshot("S1"));
            manager.undo(snapshot("Current"));

            manager.redo(snapshot("AfterUndo"));

            assertThat(manager.canUndo()).isTrue();
        }

        @Test
        void shouldReturnEmptyWhenEmpty() {
            Optional<UndoManager.Snapshot> result = manager.redo(snapshot("Current"));

            assertThat(result).isEmpty();
        }

        @Test
        void shouldNotPushToUndoWhenEmpty() {
            manager.redo(snapshot("Current"));

            assertThat(manager.canUndo()).isFalse();
        }
    }

    @Nested
    @DisplayName("clear")
    class Clear {

        @Test
        void shouldEmptyBothStacks() {
            manager.pushUndo(snapshot("S1"));
            manager.pushUndo(snapshot("S2"));
            manager.undo(snapshot("Current"));

            manager.clear();

            assertThat(manager.canUndo()).isFalse();
            assertThat(manager.canRedo()).isFalse();
        }
    }

    @Nested
    @DisplayName("sequences")
    class Sequences {

        @Test
        void shouldSupportMultipleUndoAndRedo() {
            manager.pushUndo(snapshot("S1"));
            manager.pushUndo(snapshot("S2"));
            manager.pushUndo(snapshot("S3"));

            // Undo three times: S3, S2, S1
            UndoManager.Snapshot current = snapshot("S4");
            UndoManager.Snapshot r1 = manager.undo(current).orElseThrow();
            assertSnapshotName(r1, "S3");

            UndoManager.Snapshot r2 = manager.undo(r1).orElseThrow();
            assertSnapshotName(r2, "S2");

            UndoManager.Snapshot r3 = manager.undo(r2).orElseThrow();
            assertSnapshotName(r3, "S1");

            assertThat(manager.canUndo()).isFalse();

            // Redo three times
            UndoManager.Snapshot f1 = manager.redo(r3).orElseThrow();
            assertSnapshotName(f1, "S2");

            UndoManager.Snapshot f2 = manager.redo(f1).orElseThrow();
            assertSnapshotName(f2, "S3");

            UndoManager.Snapshot f3 = manager.redo(f2).orElseThrow();
            assertSnapshotName(f3, "S4");

            assertThat(manager.canRedo()).isFalse();
        }

        @Test
        void shouldClearRedoOnNewPushAfterUndo() {
            manager.pushUndo(snapshot("S1"));
            manager.pushUndo(snapshot("S2"));

            manager.undo(snapshot("S3"));
            assertThat(manager.canRedo()).isTrue();

            // New push clears redo
            manager.pushUndo(snapshot("S4"));
            assertThat(manager.canRedo()).isFalse();

            // Only S2 and S4 should be on undo stack now
            assertThat(manager.canUndo()).isTrue();
        }
    }

    @Nested
    @DisplayName("labels")
    class Labels {

        @Test
        void shouldTrackUndoLabels() {
            manager.pushUndo(snapshot("S1"), "Add stock");
            manager.pushUndo(snapshot("S2"), "Move element");
            manager.pushUndo(snapshot("S3"), "Delete");

            List<String> labels = manager.undoLabels();

            assertThat(labels).containsExactly("Delete", "Move element", "Add stock");
        }

        @Test
        void shouldTrackRedoLabels() {
            manager.pushUndo(snapshot("S1"), "Add stock");
            manager.pushUndo(snapshot("S2"), "Move element");

            manager.undo(snapshot("S3"), "Current");
            manager.undo(snapshot("S4"), "Current");

            List<String> labels = manager.redoLabels();
            assertThat(labels).hasSize(2);
        }

        @Test
        void shouldReturnEmptyLabelsWhenStackIsEmpty() {
            assertThat(manager.undoLabels()).isEmpty();
            assertThat(manager.redoLabels()).isEmpty();
        }
    }

    @Nested
    @DisplayName("undoTo")
    class UndoTo {

        @Test
        void shouldJumpToDepth() {
            manager.pushUndo(snapshot("S1"), "Add stock");
            manager.pushUndo(snapshot("S2"), "Move");
            manager.pushUndo(snapshot("S3"), "Delete");

            UndoManager.Snapshot result = manager.undoTo(snapshot("Current"), 2).orElseThrow();

            assertSnapshotName(result, "S1");
            assertThat(manager.canUndo()).isFalse();
            // All intermediate entries + current should be on redo stack
            assertThat(manager.canRedo()).isTrue();
        }

        @Test
        void shouldJumpToMostRecent() {
            manager.pushUndo(snapshot("S1"), "Add stock");
            manager.pushUndo(snapshot("S2"), "Move");

            UndoManager.Snapshot result = manager.undoTo(snapshot("Current"), 0).orElseThrow();

            assertSnapshotName(result, "S2");
        }

        @Test
        void shouldReturnEmptyForInvalidDepth() {
            manager.pushUndo(snapshot("S1"), "Add stock");

            assertThat(manager.undoTo(snapshot("Current"), -1)).isEmpty();
            assertThat(manager.undoTo(snapshot("Current"), 1)).isEmpty();
        }
    }

    @Nested
    @DisplayName("compression")
    class Compression {

        @Test
        void shouldPreserveModelDataThroughCompression() {
            ModelDefinition model = new ModelDefinitionBuilder()
                    .name("TestModel")
                    .stock("Population", 1000, "people")
                    .constant("GrowthRate", 0.05, "dimensionless")
                    .build();
            ViewDef view = new ViewDef("Main", List.of(), List.of(), List.of());
            UndoManager.Snapshot original = new UndoManager.Snapshot(model, view);

            manager.pushUndo(original, "Add elements");

            UndoManager.Snapshot restored = manager.undo(snapshot("Current")).orElseThrow();

            assertThat(restored.model().name()).isEqualTo("TestModel");
            assertThat(restored.model().stocks()).hasSize(1);
            assertThat(restored.model().stocks().getFirst().name()).isEqualTo("Population");
            assertThat(restored.model().constants()).hasSize(1);
            assertThat(restored.model().constants().getFirst().name()).isEqualTo("GrowthRate");
            assertThat(restored.view()).isNotNull();
            assertThat(restored.view().name()).isEqualTo("Main");
        }

        @Test
        void shouldReportCorrectDepth() {
            manager.pushUndo(snapshot("S1"));
            manager.pushUndo(snapshot("S2"));
            manager.pushUndo(snapshot("S3"));

            assertThat(manager.undoDepth()).isEqualTo(3);

            manager.undo(snapshot("Current"));
            assertThat(manager.undoDepth()).isEqualTo(2);
        }
    }
}
