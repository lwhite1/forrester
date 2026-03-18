package systems.courant.sd.app.canvas;

import systems.courant.sd.model.def.ModelDefinition;
import systems.courant.sd.model.def.ModelDefinitionBuilder;
import systems.courant.sd.model.def.ViewDef;
import systems.courant.sd.model.graph.AutoLayout;

import javafx.scene.Scene;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.testfx.framework.junit5.ApplicationExtension;
import org.testfx.framework.junit5.Start;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

/**
 * TestFX test verifying that maturity visual indicators render correctly
 * on the model canvas when elements are incomplete (#89).
 */
@DisplayName("Maturity visual indicators on canvas (#89)")
@ExtendWith(ApplicationExtension.class)
class MaturityIndicatorFxTest {

    private ModelCanvas canvas;
    private ModelEditor editor;

    @Start
    void start(Stage stage) {
        // Build a model with some incomplete elements
        ModelDefinition def = new ModelDefinitionBuilder()
                .name("Maturity Test")
                .stock("Stock1", 100, "Person")       // fully specified
                .stock("Stock2", 0, "units")           // missing unit
                .flow("Flow1", "Stock1 * 0.1", "Day", "Stock1", null)  // fully specified
                .flow("Flow2", "0", "Day", null, "Stock2")             // missing equation
                .variable("Var1", "Stock1 * 0.5", "Dimensionless unit") // fully specified
                .variable("Var2", "0", "units")                        // missing equation + unit
                .defaultSimulation("Day", 10, "Day")
                .build();

        ViewDef view = AutoLayout.layout(def);

        canvas = new ModelCanvas(new Clipboard());
        canvas.undo().setUndoManager(new UndoManager());
        editor = new ModelEditor();
        editor.loadFrom(def);
        canvas.setModel(editor, view);

        stage.setScene(new Scene(new StackPane(canvas), 600, 400));
        stage.show();
    }

    @Test
    @DisplayName("canvas renders without error when model has incomplete elements")
    void shouldRenderWithoutError() {
        assertThatCode(() -> {
            // Force a redraw by resizing
            canvas.setWidth(601);
            canvas.setHeight(401);
        }).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("maturity analysis identifies missing equations")
    void shouldIdentifyMissingEquations() {
        MaturityAnalysis analysis = MaturityAnalysis.analyze(editor.toModelDefinition());

        assertThat(analysis.missingEquation())
                .contains("Flow2", "Var2")
                .doesNotContain("Flow1", "Var1", "Stock1", "Stock2");
    }

    @Test
    @DisplayName("maturity analysis identifies missing units")
    void shouldIdentifyMissingUnits() {
        MaturityAnalysis analysis = MaturityAnalysis.analyze(editor.toModelDefinition());

        assertThat(analysis.missingUnit())
                .contains("Stock2", "Var2")
                .doesNotContain("Stock1", "Var1");
    }

    @Test
    @DisplayName("fully specified elements are not flagged")
    void fullySpecifiedElementsNotFlagged() {
        MaturityAnalysis analysis = MaturityAnalysis.analyze(editor.toModelDefinition());

        assertThat(analysis.isIncomplete("Stock1")).isFalse();
        assertThat(analysis.isIncomplete("Flow1")).isFalse();
        assertThat(analysis.isIncomplete("Var1")).isFalse();
    }

    @Test
    @DisplayName("canvas renders with maturity indicators after equation change")
    void shouldUpdateAfterEquationChange() {
        // Fix Flow2's equation
        editor.setFlowEquation("Flow2", "Stock2 * 0.1");

        // Verify maturity analysis updates
        MaturityAnalysis analysis = MaturityAnalysis.analyze(editor.toModelDefinition());
        assertThat(analysis.missingEquation()).doesNotContain("Flow2");

        // Verify canvas still renders without error
        assertThatCode(() -> {
            canvas.setWidth(602);
            canvas.setHeight(402);
        }).doesNotThrowAnyException();
    }
}
