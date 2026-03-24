package systems.courant.sd.app.canvas.forms;

import systems.courant.sd.measure.CompositeUnit;
import systems.courant.sd.measure.Dimension;
import systems.courant.sd.measure.DimensionalAnalyzer;
import systems.courant.sd.measure.TimeUnit;
import systems.courant.sd.measure.UnitRegistry;
import systems.courant.sd.model.expr.Expr;
import systems.courant.sd.model.expr.ExprParser;
import systems.courant.sd.model.expr.ParseException;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;

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

    /** Retained references for revalidation when the selected element changes. */
    private EquationField attachedField;
    private Label attachedErrorLabel;
    private Label attachedDimensionLabel;

    public DimensionalAnalysisUI(FormContext ctx) {
        this.ctx = ctx;
    }

    /**
     * Re-runs equation validation and dimensional analysis for the currently attached field.
     * Call this after {@code updateValues()} to refresh the dimension label when the selected
     * element changes on the fast path (same form type reused).
     */
    public void revalidate() {
        if (attachedField != null && attachedErrorLabel != null && attachedDimensionLabel != null) {
            validateEquation(attachedField, attachedErrorLabel, attachedDimensionLabel);
        }
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

        // Retain references for revalidation on element switch
        this.attachedField = field;
        this.attachedErrorLabel = errorLabel;
        this.attachedDimensionLabel = dimensionLabel;

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
            String rawExpected = getRawExpectedUnitLabel();

            if (!analysis.isConsistent()) {
                // Show first warning
                String warning = analysis.warnings().getFirst().message();
                dimensionLabel.setText("Warning: " + warning);
                dimensionLabel.setStyle(Styles.DIMENSION_MISMATCH);
            } else if (expected != null && !expected.isCompatibleWith(analysis.inferredUnit())) {
                // Pure constant expressions carry the declared unit implicitly — no mismatch
                if (analysis.inferredUnit().isDimensionless() && isPureConstant(expr)) {
                    String label = rawExpected != null ? rawExpected : expected.displayString();
                    dimensionLabel.setText("= " + label);
                    dimensionLabel.setStyle(Styles.DIMENSION_MATCH);
                } else {
                    String expectedLabel = rawExpected != null ? rawExpected
                            : expected.displayString();
                    dimensionLabel.setText("Equation yields " + inferredDisplay
                            + ", expected " + expectedLabel);
                    dimensionLabel.setStyle(Styles.DIMENSION_MISMATCH);
                }
            } else {
                // When matched, prefer showing raw declared unit for readability
                String label = (rawExpected != null && expected != null)
                        ? rawExpected : inferredDisplay;
                dimensionLabel.setText("= " + label);
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
     * Returns the raw declared unit string for the current element, for use in display messages.
     * Returns null if no raw string is available (e.g. unknown element type or no declared unit).
     */
    private String getRawExpectedUnitLabel() {
        var flowOpt = ctx.getEditor().getFlowByName(ctx.getElementName());
        if (flowOpt.isPresent()) {
            var flow = flowOpt.get();
            String material = null;
            if (flow.materialUnit() != null && !flow.materialUnit().isBlank()) {
                material = flow.materialUnit();
            } else if (flow.sink() != null) {
                var sink = ctx.getEditor().getStockByName(flow.sink());
                if (sink.isPresent() && sink.get().unit() != null
                        && !sink.get().unit().isBlank()) {
                    material = sink.get().unit();
                }
            } else if (flow.source() != null) {
                var source = ctx.getEditor().getStockByName(flow.source());
                if (source.isPresent() && source.get().unit() != null
                        && !source.get().unit().isBlank()) {
                    material = source.get().unit();
                }
            }
            if (material != null && flow.timeUnit() != null) {
                return material + "/" + flow.timeUnit();
            }
            return null;
        }
        var auxOpt = ctx.getEditor().getVariableByName(ctx.getElementName());
        if (auxOpt.isPresent()) {
            String unitName = auxOpt.get().unit();
            if (unitName != null && !unitName.isBlank()) {
                return unitName;
            }
        }
        return null;
    }

    /**
     * Returns true if the expression contains only literals and arithmetic — no variable
     * references or function calls. Constants carry their declared unit implicitly.
     */
    private boolean isPureConstant(Expr expr) {
        return switch (expr) {
            case Expr.Literal _ -> true;
            case Expr.Ref _ -> false;
            case Expr.BinaryOp op -> isPureConstant(op.left()) && isPureConstant(op.right());
            case Expr.UnaryOp op -> isPureConstant(op.operand());
            case Expr.FunctionCall _ -> false;
            case Expr.Conditional cond -> isPureConstant(cond.condition())
                    && isPureConstant(cond.thenExpr()) && isPureConstant(cond.elseExpr());
        };
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
                return registry.resolveComposite(unitName);
            }
        }

        return null;
    }

    /**
     * Infers the unit from the current equation text using dimensional analysis.
     * Returns the inferred {@link CompositeUnit}, or null if the equation is empty,
     * unparseable, or yields no dimensional information.
     */
    public CompositeUnit inferUnit(String equationText) {
        if (equationText == null || equationText.isBlank()) {
            return null;
        }
        try {
            Expr expr = ExprParser.parse(equationText);
            if (isPureConstant(expr)) {
                return null;
            }
            EditorUnitContext unitContext = new EditorUnitContext(ctx.getEditor(), unitRegistry);
            DimensionalAnalyzer analyzer = new DimensionalAnalyzer(unitContext);
            DimensionalAnalyzer.AnalysisResult result = analyzer.analyze(expr);
            if (result.inferredUnit() == null || !result.isConsistent()) {
                return null;
            }
            return result.inferredUnit();
        } catch (ParseException e) {
            return null;
        }
    }

    /**
     * Infers the material unit for a flow by dividing the equation's inferred unit
     * by the flow's time unit. Returns a display string using the original declared
     * unit names from referenced elements, or null if inference fails.
     *
     * @param equationText the flow equation
     * @param timeUnitName the flow's declared time unit (e.g. "Day")
     */
    public String inferFlowMaterialUnit(String equationText, String timeUnitName) {
        CompositeUnit inferred = inferUnit(equationText);
        if (inferred == null || inferred.isDimensionless()) {
            return null;
        }
        if (timeUnitName == null || timeUnitName.isBlank()) {
            return null;
        }
        try {
            TimeUnit timeUnit = unitRegistry.resolveTimeUnit(timeUnitName);
            CompositeUnit timeComposite = CompositeUnit.of(timeUnit);
            CompositeUnit material = inferred.multiply(timeComposite);
            if (material.isDimensionless()) {
                return null;
            }
            Expr expr = ExprParser.parse(equationText);
            Map<Dimension, String> dimNames = buildDimensionNameMap(expr);
            return renderWithOriginalNames(material, dimNames);
        } catch (ParseException | IllegalArgumentException e) {
            return null;
        }
    }

    /**
     * Infers the unit for a variable from its equation. Returns a display string
     * using the original declared unit names from referenced elements, or null if
     * inference fails or the result is dimensionless.
     */
    public String inferVariableUnit(String equationText) {
        CompositeUnit inferred = inferUnit(equationText);
        if (inferred == null || inferred.isDimensionless()) {
            return null;
        }
        try {
            Expr expr = ExprParser.parse(equationText);
            Map<Dimension, String> dimNames = buildDimensionNameMap(expr);
            return renderWithOriginalNames(inferred, dimNames);
        } catch (ParseException e) {
            return inferred.displayString();
        }
    }

    /**
     * Walks the expression AST collecting element references, then maps each
     * referenced element's declared unit dimension to its raw unit name.
     */
    private Map<Dimension, String> buildDimensionNameMap(Expr expr) {
        List<String> refs = new ArrayList<>();
        collectRefs(expr, refs);
        Map<Dimension, String> map = new LinkedHashMap<>();
        for (String ref : refs) {
            String rawUnit = getRawUnitForElement(ref);
            if (rawUnit != null && !rawUnit.isBlank()) {
                CompositeUnit resolved = unitRegistry.resolveComposite(rawUnit);
                for (Dimension dim : resolved.exponents().keySet()) {
                    map.putIfAbsent(dim, rawUnit);
                }
            }
        }
        return map;
    }

    private void collectRefs(Expr expr, List<String> refs) {
        if (expr instanceof Expr.Ref r) {
            refs.add(r.name());
        } else if (expr instanceof Expr.BinaryOp op) {
            collectRefs(op.left(), refs);
            collectRefs(op.right(), refs);
        } else if (expr instanceof Expr.UnaryOp op) {
            collectRefs(op.operand(), refs);
        } else if (expr instanceof Expr.FunctionCall fc) {
            fc.arguments().forEach(a -> collectRefs(a, refs));
        } else if (expr instanceof Expr.Conditional c) {
            collectRefs(c.condition(), refs);
            collectRefs(c.thenExpr(), refs);
            collectRefs(c.elseExpr(), refs);
        }
    }

    /**
     * Returns the raw declared unit string for an element, or null if unknown.
     */
    private String getRawUnitForElement(String name) {
        String resolved = name.replace('_', ' ');
        var stockOpt = ctx.getEditor().getStockByName(name);
        if (stockOpt.isEmpty()) {
            stockOpt = ctx.getEditor().getStockByName(resolved);
        }
        if (stockOpt.isPresent()) {
            return stockOpt.get().unit();
        }
        var varOpt = ctx.getEditor().getVariableByName(name);
        if (varOpt.isEmpty()) {
            varOpt = ctx.getEditor().getVariableByName(resolved);
        }
        if (varOpt.isPresent()) {
            return varOpt.get().unit();
        }
        var flowOpt = ctx.getEditor().getFlowByName(name);
        if (flowOpt.isEmpty()) {
            flowOpt = ctx.getEditor().getFlowByName(resolved);
        }
        if (flowOpt.isPresent()) {
            return flowOpt.get().materialUnit();
        }
        return null;
    }

    /**
     * Renders a CompositeUnit using raw unit names from the dimension map where available,
     * falling back to the dimension's base unit name.
     */
    private String renderWithOriginalNames(CompositeUnit unit, Map<Dimension, String> dimNames) {
        if (unit.isDimensionless()) {
            return "Dimensionless";
        }
        StringJoiner numerator = new StringJoiner(" * ");
        StringJoiner denominator = new StringJoiner(" * ");

        for (Map.Entry<Dimension, Integer> e : unit.exponents().entrySet()) {
            String name = dimNames.getOrDefault(e.getKey(), e.getKey().getBaseUnit().getName());
            int exp = e.getValue();
            if (exp > 0) {
                numerator.add(exp == 1 ? name : name + "^" + exp);
            } else {
                int absExp = -exp;
                denominator.add(absExp == 1 ? name : name + "^" + absExp);
            }
        }
        if (numerator.length() == 0) {
            numerator.add("1");
        }
        if (denominator.length() == 0) {
            return numerator.toString();
        }
        return numerator + " / " + denominator;
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
