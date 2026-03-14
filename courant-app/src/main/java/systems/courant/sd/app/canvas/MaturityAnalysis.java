package systems.courant.sd.app.canvas;

import systems.courant.sd.model.def.FlowDef;
import systems.courant.sd.model.def.ModelDefinition;
import systems.courant.sd.model.def.StockDef;
import systems.courant.sd.model.def.VariableDef;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/**
 * Analyzes a {@link ModelDefinition} to determine the maturity status of each element.
 * Maturity indicators help users see which elements still need configuration without
 * requiring AI assistance — acting as a silent checklist.
 *
 * <p>Three types of incompleteness are detected:
 * <ul>
 *   <li><b>Missing equation</b> — flows or variables with the default placeholder equation "0"</li>
 *   <li><b>Missing unit</b> — elements whose unit is null, blank, or the default placeholder "units"</li>
 *   <li><b>Unit mismatch</b> — flow connections where the flow's material unit doesn't match
 *       the connected stock's unit</li>
 * </ul>
 *
 * @param missingEquation element names that have placeholder equations
 * @param missingUnit     element names that have no meaningful unit specified
 * @param unitMismatchFlows flow names with at least one unit-mismatched connection
 */
public record MaturityAnalysis(
        Set<String> missingEquation,
        Set<String> missingUnit,
        Set<String> unitMismatchFlows
) {

    /** Default placeholder equation assigned to newly created flows and variables. */
    static final String PLACEHOLDER_EQUATION = "0";

    /** Default placeholder unit assigned to newly created elements. */
    static final String PLACEHOLDER_UNIT = "units";

    public MaturityAnalysis {
        missingEquation = Set.copyOf(missingEquation);
        missingUnit = Set.copyOf(missingUnit);
        unitMismatchFlows = Set.copyOf(unitMismatchFlows);
    }

    /**
     * Returns true if the given element has any maturity issue.
     */
    public boolean isIncomplete(String name) {
        return missingEquation.contains(name)
                || missingUnit.contains(name)
                || unitMismatchFlows.contains(name);
    }

    /**
     * Returns true if there are no maturity issues in the model.
     */
    public boolean isFullySpecified() {
        return missingEquation.isEmpty() && missingUnit.isEmpty() && unitMismatchFlows.isEmpty();
    }

    /**
     * Analyzes the given model definition and returns the maturity status.
     */
    public static MaturityAnalysis analyze(ModelDefinition def) {
        Set<String> missingEq = new LinkedHashSet<>();
        Set<String> missingUn = new LinkedHashSet<>();
        Set<String> unitMismatch = new LinkedHashSet<>();

        // Build stock unit lookup for mismatch detection
        Map<String, String> stockUnits = new LinkedHashMap<>();
        for (StockDef stock : def.stocks()) {
            stockUnits.put(stock.name(), stock.unit());
            if (isMissingUnit(stock.unit())) {
                missingUn.add(stock.name());
            }
        }

        for (FlowDef flow : def.flows()) {
            if (isMissingEquation(flow.equation())) {
                missingEq.add(flow.name());
            }

            // Check unit mismatch between flow material unit and connected stocks
            String flowMaterial = flow.materialUnit();
            if (flowMaterial != null && !flowMaterial.isBlank() && !isMissingUnit(flowMaterial)) {
                if (flow.source() != null && stockUnits.containsKey(flow.source())) {
                    String sourceUnit = stockUnits.get(flow.source());
                    if (!isMissingUnit(sourceUnit) && !unitsMatch(flowMaterial, sourceUnit)) {
                        unitMismatch.add(flow.name());
                    }
                }
                if (flow.sink() != null && stockUnits.containsKey(flow.sink())) {
                    String sinkUnit = stockUnits.get(flow.sink());
                    if (!isMissingUnit(sinkUnit) && !unitsMatch(flowMaterial, sinkUnit)) {
                        unitMismatch.add(flow.name());
                    }
                }
            }
        }

        for (VariableDef var : def.variables()) {
            if (isMissingEquation(var.equation())) {
                missingEq.add(var.name());
            }
            if (isMissingUnit(var.unit())) {
                missingUn.add(var.name());
            }
        }

        return new MaturityAnalysis(missingEq, missingUn, unitMismatch);
    }

    /**
     * Analyzes maturity from a {@link ModelEditor}'s current state.
     */
    public static MaturityAnalysis analyze(ModelEditor editor) {
        return analyze(editor.toModelDefinition());
    }

    static boolean isMissingEquation(String equation) {
        return equation == null
                || equation.isBlank()
                || PLACEHOLDER_EQUATION.equals(equation.strip());
    }

    static boolean isMissingUnit(String unit) {
        return unit == null
                || unit.isBlank()
                || PLACEHOLDER_UNIT.equalsIgnoreCase(unit.strip());
    }

    private static boolean unitsMatch(String a, String b) {
        return a.strip().equalsIgnoreCase(b.strip());
    }

    /** Empty analysis with no issues. */
    public static final MaturityAnalysis EMPTY = new MaturityAnalysis(
            Collections.emptySet(), Collections.emptySet(), Collections.emptySet());
}
