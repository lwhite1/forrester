package systems.courant.sd.app.canvas.forms;

import systems.courant.sd.model.def.LookupTableDef;

import javafx.scene.Scene;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.testfx.framework.junit5.ApplicationExtension;
import org.testfx.framework.junit5.Start;

import static org.assertj.core.api.Assertions.assertThat;
import systems.courant.sd.app.canvas.Clipboard;
import systems.courant.sd.app.canvas.ModelCanvas;
import systems.courant.sd.app.canvas.ModelEditor;

@DisplayName("LookupForm (#401)")
@ExtendWith(ApplicationExtension.class)
class LookupFormTest {

    private ModelEditor editor;
    private LookupForm form;

    @Start
    void start(Stage stage) {
        editor = new ModelEditor();
        editor.addLookup(); // Creates "Lookup 1"

        LookupTableDef testLookup = new LookupTableDef("Lookup 1", null,
                new double[]{0, 1, 2, 3, 4},
                new double[]{0, 10, 20, 30, 40},
                "LINEAR");
        editor.setLookupTable("Lookup 1", testLookup);

        ModelCanvas canvas = new ModelCanvas(new Clipboard()) {
            @Override
            public void applyMutation(Runnable mutation) {
                mutation.run();
            }
        };

        FormContext ctx = new FormContext();
        ctx.setEditor(editor);
        ctx.setCanvas(canvas);
        ctx.setGrid(new GridPane());
        ctx.setElementName("Lookup 1");

        form = new LookupForm(ctx);

        stage.setScene(new Scene(new StackPane(), 100, 100));
        stage.show();
    }

    // --- addPointAtPosition ---

    @Test
    @DisplayName("addPoint should insert at the correct sorted position")
    void shouldInsertPointSorted() {
        form.addPointAtPosition(1.5, 15);

        LookupTableDef updated = editor.getLookupTableByName("Lookup 1").orElseThrow();
        assertThat(updated.xValues()).hasSize(6);
        assertThat(updated.xValues()).containsExactly(0, 1, 1.5, 2, 3, 4);
        assertThat(updated.yValues()).containsExactly(0, 10, 15, 20, 30, 40);
    }

    @Test
    @DisplayName("addPoint should insert at the beginning when x is smallest")
    void shouldInsertAtBeginning() {
        form.addPointAtPosition(-1, -10);

        LookupTableDef updated = editor.getLookupTableByName("Lookup 1").orElseThrow();
        assertThat(updated.xValues()).containsExactly(-1, 0, 1, 2, 3, 4);
        assertThat(updated.yValues()).containsExactly(-10, 0, 10, 20, 30, 40);
    }

    @Test
    @DisplayName("addPoint should insert at the end when x is largest")
    void shouldInsertAtEnd() {
        form.addPointAtPosition(5, 50);

        LookupTableDef updated = editor.getLookupTableByName("Lookup 1").orElseThrow();
        assertThat(updated.xValues()).containsExactly(0, 1, 2, 3, 4, 5);
        assertThat(updated.yValues()).containsExactly(0, 10, 20, 30, 40, 50);
    }

    @Test
    @DisplayName("addPoint should not add duplicate x value")
    void shouldNotAddDuplicateX() {
        form.addPointAtPosition(2.0, 99);

        LookupTableDef unchanged = editor.getLookupTableByName("Lookup 1").orElseThrow();
        assertThat(unchanged.xValues()).hasSize(5);
        assertThat(unchanged.yValues()[2]).isEqualTo(20);
    }

    @Test
    @DisplayName("addPoint should preserve interpolation mode")
    void shouldPreserveInterpolation() {
        LookupTableDef splineLookup = new LookupTableDef("Lookup 1", null,
                new double[]{0, 1, 2}, new double[]{0, 10, 20}, "SPLINE");
        editor.setLookupTable("Lookup 1", splineLookup);

        form.addPointAtPosition(0.5, 5);

        LookupTableDef updated = editor.getLookupTableByName("Lookup 1").orElseThrow();
        assertThat(updated.interpolation()).isEqualTo("SPLINE");
        assertThat(updated.xValues()).containsExactly(0, 0.5, 1, 2);
    }

    // --- deletePointByIndex ---

    @Test
    @DisplayName("deletePoint should remove the point at the given index")
    void shouldDeletePoint() {
        form.deletePointByIndex(2);

        LookupTableDef updated = editor.getLookupTableByName("Lookup 1").orElseThrow();
        assertThat(updated.xValues()).hasSize(4);
        assertThat(updated.xValues()).containsExactly(0, 1, 3, 4);
        assertThat(updated.yValues()).containsExactly(0, 10, 30, 40);
    }

    @Test
    @DisplayName("deletePoint should remove the first point")
    void shouldDeleteFirstPoint() {
        form.deletePointByIndex(0);

        LookupTableDef updated = editor.getLookupTableByName("Lookup 1").orElseThrow();
        assertThat(updated.xValues()).containsExactly(1, 2, 3, 4);
    }

    @Test
    @DisplayName("deletePoint should remove the last point")
    void shouldDeleteLastPoint() {
        form.deletePointByIndex(4);

        LookupTableDef updated = editor.getLookupTableByName("Lookup 1").orElseThrow();
        assertThat(updated.xValues()).containsExactly(0, 1, 2, 3);
    }

    @Test
    @DisplayName("deletePoint should not delete when at minimum points")
    void shouldNotDeleteAtMinimum() {
        LookupTableDef twoPoints = new LookupTableDef("Lookup 1", null,
                new double[]{0, 1}, new double[]{0, 10}, "LINEAR");
        editor.setLookupTable("Lookup 1", twoPoints);

        form.deletePointByIndex(0);

        LookupTableDef unchanged = editor.getLookupTableByName("Lookup 1").orElseThrow();
        assertThat(unchanged.xValues()).hasSize(2);
    }

    @Test
    @DisplayName("deletePoint should not delete when index is out of bounds")
    void shouldNotDeleteOutOfBounds() {
        form.deletePointByIndex(10);

        LookupTableDef unchanged = editor.getLookupTableByName("Lookup 1").orElseThrow();
        assertThat(unchanged.xValues()).hasSize(5);
    }

    @Test
    @DisplayName("deletePoint should not delete when index is negative")
    void shouldNotDeleteNegativeIndex() {
        form.deletePointByIndex(-1);

        LookupTableDef unchanged = editor.getLookupTableByName("Lookup 1").orElseThrow();
        assertThat(unchanged.xValues()).hasSize(5);
    }

    // --- formatPointTooltip ---

    @Test
    @DisplayName("formatPointTooltip should format integer values without decimals")
    void shouldFormatIntegers() {
        String result = LookupForm.formatPointTooltip(2.0, 10.0);
        assertThat(result).isEqualTo("(2, 10)");
    }

    @Test
    @DisplayName("formatPointTooltip should format decimal values")
    void shouldFormatDecimals() {
        String result = LookupForm.formatPointTooltip(1.5, 7.25);
        assertThat(result).contains("1.5").contains("7.25");
    }

    // --- Constants ---

    @Test
    @DisplayName("MIN_POINTS should be 2")
    void minPointsShouldBeTwo() {
        assertThat(LookupForm.MIN_POINTS).isEqualTo(2);
    }

    @Test
    @DisplayName("POINT_STYLE and POINT_DRAG_STYLE should differ")
    void stylesShouldDiffer() {
        assertThat(LookupForm.POINT_STYLE).contains("-fx-background-color");
        assertThat(LookupForm.POINT_DRAG_STYLE).isNotEqualTo(LookupForm.POINT_STYLE);
    }
}
