package systems.courant.forrester.integration;

import systems.courant.forrester.io.json.ModelDefinitionSerializer;
import systems.courant.forrester.model.compile.ModelCompiler;
import systems.courant.forrester.model.def.ConstantDef;
import systems.courant.forrester.model.def.FlowDef;
import systems.courant.forrester.model.def.ModelDefinition;
import systems.courant.forrester.model.def.ModelDefinitionBuilder;
import systems.courant.forrester.model.def.StockDef;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration test: series of model edits with undo/redo through the definition layer.
 *
 * <p>Since UndoManager is an app-layer component tied to JavaFX, this test exercises
 * the underlying pattern: a series of definition mutations serialized as snapshots,
 * verifying that reverting to earlier snapshots restores clean state.
 *
 * <p>This tests the same serialization path that UndoManager relies on for snapshots.
 */
@DisplayName("Undo/redo integration (snapshot-based)")
class UndoRedoIntegrationTest {

    private final ModelDefinitionSerializer serializer = new ModelDefinitionSerializer();
    private final ModelCompiler compiler = new ModelCompiler();

    @Nested
    @DisplayName("Snapshot stack")
    class SnapshotStack {

        @Test
        @DisplayName("should restore original state after series of edits and full undo")
        void shouldRestoreOriginalAfterFullUndo() {
            // Snapshot 0: original model
            ModelDefinition v0 = new ModelDefinitionBuilder()
                    .name("UndoTest")
                    .defaultSimulation("Day", 10, "Day")
                    .stock("Tank", 100, "Liter")
                    .flow(new FlowDef("drain", "Tank * 0.1", "Day", "Tank", null))
                    .build();

            List<String> snapshots = new ArrayList<>();
            snapshots.add(serializer.toJson(v0));

            // Edit 1: add a constant
            ModelDefinition v1 = new ModelDefinitionBuilder()
                    .name("UndoTest")
                    .defaultSimulation("Day", 10, "Day")
                    .stock("Tank", 100, "Liter")
                    .flow(new FlowDef("drain", "Tank * rate", "Day", "Tank", null))
                    .constant("rate", 0.1, "1/Day")
                    .build();
            snapshots.add(serializer.toJson(v1));

            // Edit 2: change stock initial value
            ModelDefinition v2 = new ModelDefinitionBuilder()
                    .name("UndoTest")
                    .defaultSimulation("Day", 10, "Day")
                    .stock("Tank", 500, "Liter")
                    .flow(new FlowDef("drain", "Tank * rate", "Day", "Tank", null))
                    .constant("rate", 0.1, "1/Day")
                    .build();
            snapshots.add(serializer.toJson(v2));

            // Edit 3: add an inflow
            ModelDefinition v3 = new ModelDefinitionBuilder()
                    .name("UndoTest")
                    .defaultSimulation("Day", 10, "Day")
                    .stock("Tank", 500, "Liter")
                    .flow(new FlowDef("drain", "Tank * rate", "Day", "Tank", null))
                    .flow(new FlowDef("fill", "20", "Day", null, "Tank"))
                    .constant("rate", 0.1, "1/Day")
                    .build();
            snapshots.add(serializer.toJson(v3));

            // Verify current state (v3)
            ModelDefinition current = serializer.fromJson(snapshots.get(3));
            assertThat(current.flows()).hasSize(2);
            assertThat(current.stocks().getFirst().initialValue()).isEqualTo(500.0);

            // Undo to v2
            current = serializer.fromJson(snapshots.get(2));
            assertThat(current.flows()).hasSize(1);
            assertThat(current.stocks().getFirst().initialValue()).isEqualTo(500.0);

            // Undo to v1
            current = serializer.fromJson(snapshots.get(1));
            assertThat(current.constants()).hasSize(1);
            assertThat(current.stocks().getFirst().initialValue()).isEqualTo(100.0);

            // Undo to v0 (original)
            current = serializer.fromJson(snapshots.get(0));
            assertThat(current.constants()).isEmpty();
            assertThat(current.flows()).hasSize(1);
            assertThat(current.stocks().getFirst().initialValue()).isEqualTo(100.0);

            // The restored model should still compile and simulate
            var compiled = compiler.compile(current);
            var sim = compiled.createSimulation();
            sim.execute();
            assertThat(compiled.getModel().getStocks().getFirst().getValue()).isFinite();
        }

        @Test
        @DisplayName("should support redo after undo")
        void shouldSupportRedo() {
            ModelDefinition v0 = new ModelDefinitionBuilder()
                    .name("RedoTest")
                    .defaultSimulation("Day", 5, "Day")
                    .stock("S", 10, "Thing")
                    .build();

            ModelDefinition v1 = new ModelDefinitionBuilder()
                    .name("RedoTest")
                    .defaultSimulation("Day", 5, "Day")
                    .stock("S", 10, "Thing")
                    .constant("k", 42, "Thing")
                    .build();

            String snap0 = serializer.toJson(v0);
            String snap1 = serializer.toJson(v1);

            // At v1, undo to v0
            ModelDefinition undone = serializer.fromJson(snap0);
            assertThat(undone.constants()).isEmpty();

            // Redo to v1
            ModelDefinition redone = serializer.fromJson(snap1);
            assertThat(redone.constants()).hasSize(1);
            assertThat(redone.constants().getFirst().name()).isEqualTo("k");
            assertThat(redone.constants().getFirst().value()).isEqualTo(42.0);
        }

        @Test
        @DisplayName("should preserve snapshot fidelity across many edits")
        void shouldPreserveFidelityAcrossManyEdits() {
            List<String> snapshots = new ArrayList<>();

            // Build 20 incremental snapshots, each adding a constant
            for (int i = 0; i < 20; i++) {
                ModelDefinitionBuilder builder = new ModelDefinitionBuilder()
                        .name("ManyEdits")
                        .defaultSimulation("Day", 10, "Day")
                        .stock("S", 1, "Thing");
                for (int j = 0; j <= i; j++) {
                    builder.constant("c" + j, j, "Thing");
                }
                snapshots.add(serializer.toJson(builder.build()));
            }

            // Undo to each snapshot and verify constant count
            for (int i = 19; i >= 0; i--) {
                ModelDefinition restored = serializer.fromJson(snapshots.get(i));
                assertThat(restored.constants())
                        .as("Snapshot %d should have %d constants", i, i + 1)
                        .hasSize(i + 1);
            }
        }
    }
}
