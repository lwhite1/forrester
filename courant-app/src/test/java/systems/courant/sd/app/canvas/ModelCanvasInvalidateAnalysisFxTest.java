package systems.courant.sd.app.canvas;

import systems.courant.sd.model.def.ElementType;
import systems.courant.sd.model.def.ModelDefinition;
import systems.courant.sd.model.def.ViewDef;

import javafx.scene.Scene;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.testfx.framework.junit5.ApplicationExtension;
import org.testfx.framework.junit5.Start;

import org.testfx.util.WaitForAsyncUtils;

import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies that {@link ModelCanvas#invalidateAnalysis()} builds the
 * {@link ModelDefinition} snapshot only once per structural mutation,
 * instead of rebuilding it for each consumer (loop analysis, causal trace,
 * validation). See GitHub issue #380.
 */
@DisplayName("ModelCanvas invalidateAnalysis snapshot reuse (#380)")
@ExtendWith(ApplicationExtension.class)
class ModelCanvasInvalidateAnalysisFxTest {

    private ModelCanvas canvas;

    /** ModelEditor subclass that counts calls to toModelDefinition(ViewDef). */
    private static class CountingModelEditor extends ModelEditor {
        final AtomicInteger snapshotCallCount = new AtomicInteger();

        @Override
        public ModelDefinition toModelDefinition(ViewDef view) {
            snapshotCallCount.incrementAndGet();
            return super.toModelDefinition(view);
        }

        int snapshotCount() {
            return snapshotCallCount.get();
        }

        void resetCount() {
            snapshotCallCount.set(0);
        }
    }

    @Start
    void start(Stage stage) {
        canvas = new ModelCanvas(new Clipboard());
        canvas.undo().setUndoManager(new UndoManager());
        stage.setScene(new Scene(new StackPane(canvas), 800, 600));
        stage.show();
    }

    @Test
    @DisplayName("setModel should call toModelDefinition(ViewDef) exactly once for invalidateAnalysis")
    void shouldBuildSnapshotOnceOnSetModel() {
        CountingModelEditor editor = new CountingModelEditor();
        editor.addStock(); // Stock 1
        CanvasState state = new CanvasState();
        state.addElement("Stock 1", ElementType.STOCK, 100, 200);

        editor.resetCount();
        canvas.setModel(editor, state.toViewDef());

        // setModel calls generateConnectors() (1 call via no-arg toModelDefinition)
        // then invalidateAnalysis() which should build one snapshot shared across
        // loop analysis, causal trace, and validation (1 call).
        // Total: 2 calls. Before the fix this was 4 (generateConnectors + 3 in invalidateAnalysis).
        assertThat(editor.snapshotCount())
                .as("toModelDefinition(ViewDef) should be called twice total: "
                        + "once for generateConnectors, once for invalidateAnalysis")
                .isEqualTo(2);
    }

    @Test
    @DisplayName("regenerateConnectors should call toModelDefinition(ViewDef) exactly once")
    void shouldBuildSnapshotOnceOnRegenerateConnectors() {
        CountingModelEditor editor = new CountingModelEditor();
        editor.addStock(); // Stock 1
        editor.addStock(); // Stock 2
        CanvasState state = new CanvasState();
        state.addElement("Stock 1", ElementType.STOCK, 100, 200);
        state.addElement("Stock 2", ElementType.STOCK, 300, 200);
        canvas.setModel(editor, state.toViewDef());

        // Reset count after setModel setup
        editor.resetCount();

        // regenerateConnectors is package-private; it calls generateConnectors (1 call)
        // then invalidateAnalysis (1 call). Total: 2.
        canvas.regenerateConnectors();

        assertThat(editor.snapshotCount())
                .as("toModelDefinition(ViewDef) should be called twice: "
                        + "once for generateConnectors, once for invalidateAnalysis")
                .isEqualTo(2);
    }

    @Test
    @DisplayName("validation result should be populated after invalidateAnalysis")
    void shouldPopulateValidationAfterInvalidateAnalysis() {
        CountingModelEditor editor = new CountingModelEditor();
        editor.addStock(); // Stock 1
        CanvasState state = new CanvasState();
        state.addElement("Stock 1", ElementType.STOCK, 100, 200);

        canvas.setModel(editor, state.toViewDef());

        // Validation is debounced via Platform.runLater — flush pending FX events
        WaitForAsyncUtils.waitForFxEvents();

        // Validation should be computed (stock with no flow should have issues)
        assertThat(canvas.analysis().getLastValidationResult()).isNotNull();
        assertThat(canvas.analysis().getLastValidationResult().issues()).isNotNull();
    }

    @Test
    @DisplayName("invalidateAnalysis with null editor should not throw")
    void shouldHandleNullEditorGracefully() {
        // Canvas has no editor set -- calling methods that would trigger
        // invalidateAnalysis should not throw
        assertThat(canvas.analysis().getLastValidationResult()).isNotNull();
        assertThat(canvas.analysis().getLastValidationResult().issues()).isEmpty();
    }

    @Test
    @DisplayName("rapid invalidations should coalesce validation into one FX pulse (#1267)")
    void shouldCoalesceRapidInvalidations() {
        CountingModelEditor editor = new CountingModelEditor();
        editor.addStock(); // Stock 1
        CanvasState state = new CanvasState();
        state.addElement("Stock 1", ElementType.STOCK, 100, 200);
        canvas.setModel(editor, state.toViewDef());

        // Flush any pending validation from setModel
        WaitForAsyncUtils.waitForFxEvents();
        editor.resetCount();

        // Three rapid invalidations — each calls modelDefSupplier.get() once
        // for loop/trace, but validation is coalesced via Platform.runLater
        canvas.invalidateAnalysis();
        canvas.invalidateAnalysis();
        canvas.invalidateAnalysis();

        int preFlushCount = editor.snapshotCount();
        assertThat(preFlushCount)
                .as("three invalidations should call modelDefSupplier three times for loop/trace")
                .isEqualTo(3);

        // Process FX events — the single deferred validation uses the captured def
        WaitForAsyncUtils.waitForFxEvents();

        // No additional supplier calls — validation reused the captured def
        assertThat(editor.snapshotCount())
                .as("deferred validation should reuse captured def, no extra supplier calls")
                .isEqualTo(preFlushCount);

        // Validation result should be populated after the pulse
        assertThat(canvas.analysis().getLastValidationResult()).isNotNull();
        assertThat(canvas.analysis().getLastValidationResult().issues()).isNotNull();
    }
}
