package com.deathrayresearch.forrester.app.canvas;

import com.deathrayresearch.forrester.model.def.ModelDefinition;
import com.deathrayresearch.forrester.model.def.ModelDefinitionBuilder;
import com.deathrayresearch.forrester.model.def.ViewDef;

import java.util.List;

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
            UndoManager.Snapshot first = snapshot("First");
            manager.pushUndo(first);
            for (int i = 1; i < UndoManager.MAX_UNDO; i++) {
                manager.pushUndo(snapshot("S" + i));
            }
            assertThat(manager.canUndo()).isTrue();

            // Push one more — should evict the oldest (first)
            manager.pushUndo(snapshot("overflow"));

            // Drain all undo entries
            int count = 0;
            UndoManager.Snapshot current = snapshot("drain");
            while (manager.canUndo()) {
                current = manager.undo(current);
                count++;
            }
            assertThat(count).isEqualTo(UndoManager.MAX_UNDO);
            // The oldest entry should be "S1", not "First" (evicted)
            assertThat(current.model().name()).isEqualTo("S1");
        }
    }

    @Nested
    @DisplayName("undo")
    class Undo {

        @Test
        void shouldReturnPreviousSnapshot() {
            UndoManager.Snapshot s1 = snapshot("S1");
            manager.pushUndo(s1);

            UndoManager.Snapshot result = manager.undo(snapshot("Current"));

            assertThat(result).isSameAs(s1);
        }

        @Test
        void shouldPushCurrentToRedoStack() {
            manager.pushUndo(snapshot("S1"));
            UndoManager.Snapshot current = snapshot("Current");

            manager.undo(current);

            assertThat(manager.canRedo()).isTrue();
            UndoManager.Snapshot redone = manager.redo(snapshot("Dummy"));
            assertThat(redone).isSameAs(current);
        }

        @Test
        void shouldReturnNullWhenEmpty() {
            UndoManager.Snapshot result = manager.undo(snapshot("Current"));

            assertThat(result).isNull();
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
            UndoManager.Snapshot current = snapshot("Current");
            manager.undo(current);

            UndoManager.Snapshot result = manager.redo(snapshot("AfterUndo"));

            assertThat(result).isSameAs(current);
        }

        @Test
        void shouldPushCurrentToUndoStack() {
            manager.pushUndo(snapshot("S1"));
            manager.undo(snapshot("Current"));

            UndoManager.Snapshot afterUndo = snapshot("AfterUndo");
            manager.redo(afterUndo);

            assertThat(manager.canUndo()).isTrue();
        }

        @Test
        void shouldReturnNullWhenEmpty() {
            UndoManager.Snapshot result = manager.redo(snapshot("Current"));

            assertThat(result).isNull();
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
            UndoManager.Snapshot s1 = snapshot("S1");
            UndoManager.Snapshot s2 = snapshot("S2");
            UndoManager.Snapshot s3 = snapshot("S3");

            manager.pushUndo(s1);
            manager.pushUndo(s2);
            manager.pushUndo(s3);

            // Undo three times: S3, S2, S1
            UndoManager.Snapshot current = snapshot("S4");
            UndoManager.Snapshot r1 = manager.undo(current);
            assertThat(r1).isSameAs(s3);

            UndoManager.Snapshot r2 = manager.undo(r1);
            assertThat(r2).isSameAs(s2);

            UndoManager.Snapshot r3 = manager.undo(r2);
            assertThat(r3).isSameAs(s1);

            assertThat(manager.canUndo()).isFalse();

            // Redo three times
            UndoManager.Snapshot f1 = manager.redo(r3);
            assertThat(f1).isSameAs(s2);

            UndoManager.Snapshot f2 = manager.redo(f1);
            assertThat(f2).isSameAs(s3);

            UndoManager.Snapshot f3 = manager.redo(f2);
            assertThat(f3.model().name()).isEqualTo("S4");

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
}
