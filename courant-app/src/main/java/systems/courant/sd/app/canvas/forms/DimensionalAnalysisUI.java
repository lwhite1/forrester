package systems.courant.sd.app.canvas.forms;

import systems.courant.sd.measure.CompositeUnit;
import systems.courant.sd.measure.DimensionalAnalyzer;
import systems.courant.sd.measure.UnitRegistry;
import systems.courant.sd.model.expr.Expr;
import systems.courant.sd.model.expr.ExprParser;
import systems.courant.sd.model.expr.ParseException;

import javafx.animation.PauseTransition;
import javafx.scene.control.Label;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.util.Duration;

import systems.courant.sd.app.canvas.EditorUnitContext;
import systems.courant.sd.app.canvas.EquationField;
import systems.courant.sd.app.canvas.EquationValidator;
import systems.courant.sd.app.canvas.Styles;

/**
 * Handles real-time equation validation and dimensional analysis feedback
 * for equation fields in the properties panel.
 */
public class DimensionalAnalysisUI {

    private final FormContext ctx;

    /** Cached registry for dimensional analysis — avoids rebuilding on every keystroke. */
    private final UnitRegistry unitRegistry = new UnitRegistry();

    public DimensionalAnalysisUI(FormContext ctx) {
        this.ctx = ctx;
    }

    /**
     * Attaches real-time equation validation and dimensional analysis to an equation field.
     * Shows a red border and error label for syntax/reference errors, and an inferred
     * unit label for dimensional analysis feedback.
     *
     * @param field   the equation field
     * @param row     the grid row where the equation field sits
     * @return the error label (for cleanup if needed)
     */
    public Label attachEquationValidation(EquationField field, int row) {
        Label errorLabel = new Label();
        errorLabel.setStyle(Styles.EQUATION_ERROR_LABEL);
        errorLabel.setWrapText(true);
        errorLabel.setMaxWidth(Double.MAX_VALUE);
        errorLabel.setVisible(false);
        errorLabel.setManaged(false);
        GridPane.setHgrow(errorLabel, Priority.ALWAYS);
        ctx.getGrid().add(errorLabel, 1, row);

        Label dimensionLabel = new Label();
        dimensionLabel.setStyle(Styles.DIMENSION_LABEL);
        dimensionLabel.setWrapText(true);
        dimensionLabel.setMaxWidth(Double.MAX_VALUE);
        dimensionLabel.setVisible(false);
        dimensionLabel.setManaged(false);
        GridPane.setHgrow(dimensionLabel, Priority.ALWAYS);
        // Dimension label goes in same row — we'll toggle visibility with error label
        ctx.getGrid().add(dimensionLabel, 1, row);

        PauseTransition debounce = new PauseTransition(Duration.millis(400));
        debounce.setOnFinished(e -> validateEquation(field, errorLabel, dimensionLabel));

        field.textObservable().addListener((obs, oldVal, newVal) -> {
            if (!ctx.isUpdatingFields()) {
                debounce.playFromStart();
            }
        });

        field.focusedProperty().addListener((obs, wasFocused, isFocused) -> {
            if (!isFocused && !ctx.isUpdatingFields()) {
                debounce.stop();
                validateEquation(field, errorLabel, dimensionLabel);
            }
        });

        // Initial validation
        validateEquation(field, errorLabel, dimensionLabel);

        return errorLabel;
    }

    private void validateEquation(EquationField field, Label errorLabel, Label dimensionLabel) {
        String text = field.getText().trim();
        if (text.isEmpty()) {
            clearEquationError(field, errorLabel);
            hideDimensionLabel(dimensionLabel);
            return;
        }
        EquationValidator.Result result =
                EquationValidator.validate(text, ctx.getEditor(), ctx.getElementName());
        if (result.valid()) {
            clearEquationError(field, errorLabel);
            runDimensionalAnalysis(text, dimensionLabel);
        } else {
            field.setFieldStyle(Styles.EQUATION_ERROR_BORDER);
            errorLabel.setText(result.message());
            errorLabel.setVisible(true);
            errorLabel.setManaged(true);
            hideDimensionLabel(dimensionLabel);
        }
    }

    private void runDimensionalAnalysis(String equationText, Label dimensionLabel) {
        try {
            Expr expr = ExprParser.parse(equationText);
            EditorUnitContext unitContext = new EditorUnitContext(ctx.getEditor(), unitRegistry);
            DimensionalAnalyzer analyzer = new DimensionalAnalyzer(unitContext);
            DimensionalAnalyzer.AnalysisResult analysis = analyzer.analyze(expr);

            if (analysis.inferredUnit() == null) {
                hideDimensionLabel(dimensionLabel);
                return;
            }

            // Build display text
            String inferredDisplay = analysis.inferredUnit().displayString();
            CompositeUnit expected = getExpectedUnit(unitRegistry);

            if (!analysis.isConsistent()) {
                // Show first warning
                String warning = analysis.warnings().getFirst().message();
                dimensionLabel.setText("Warning: " + warning);
                dimensionLabel.setStyle(Styles.DIMENSION_MISMATCH);
            } else if (expected != null && !expected.isCompatibleWith(analysis.inferredUnit())) {
                dimensionLabel.setText("Equation yields " + inferredDisplay
                        + ", expected " + expected.displayString());
                dimensionLabel.setStyle(Styles.DIMENSION_MISMATCH);
            } else {
                dimensionLabel.setText("= " + inferredDisplay);
                dimensionLabel.setStyle(expected != null ? Styles.DIMENSION_MATCH
                        : Styles.DIMENSION_LABEL);
            }
            dimensionLabel.setVisible(true);
            dimensionLabel.setManaged(true);
        } catch (ParseException e) {
            hideDimensionLabel(dimensionLabel);
        }
    }

    /**
     * Returns the expected composite unit for the current element, or null if unknown.
     */
    private CompositeUnit getExpectedUnit(UnitRegistry registry) {
        // For flows: expected is material / time
        var flowOpt = ctx.getEditor().getFlowByName(ctx.getElementName());
        if (flowOpt.isPresent()) {
            var flow = flowOpt.get();
            systems.courant.sd.measure.Unit materialUnit = null;
            if (flow.materialUnit() != null && !flow.materialUnit().isBlank()) {
                materialUnit = registry.resolve(flow.materialUnit());
            } else if (flow.sink() != null) {
                var sink = ctx.getEditor().getStockByName(flow.sink());
                if (sink.isPresent() && sink.get().unit() != null
                        && !sink.get().unit().isBlank()) {
                    materialUnit = registry.resolve(sink.get().unit());
                }
            } else if (flow.source() != null) {
                var source = ctx.getEditor().getStockByName(flow.source());
                if (source.isPresent() && source.get().unit() != null
                        && !source.get().unit().isBlank()) {
                    materialUnit = registry.resolve(source.get().unit());
                }
            }
            try {
                var timeUnit = registry.resolveTimeUnit(flow.timeUnit());
                return CompositeUnit.ofRate(materialUnit, timeUnit);
            } catch (IllegalArgumentException e) {
                return null;
            }
        }

        // For variables: expected is the declared unit
        var auxOpt = ctx.getEditor().getVariableByName(ctx.getElementName());
        if (auxOpt.isPresent()) {
            String unitName = auxOpt.get().unit();
            if (unitName != null && !unitName.isBlank()) {
                return CompositeUnit.of(registry.resolve(unitName));
            }
        }

        return null;
    }

    private void clearEquationError(EquationField field, Label errorLabel) {
        field.setFieldStyle("");
        errorLabel.setVisible(false);
        errorLabel.setManaged(false);
    }

    private void hideDimensionLabel(Label dimensionLabel) {
        dimensionLabel.setVisible(false);
        dimensionLabel.setManaged(false);
    }
}
