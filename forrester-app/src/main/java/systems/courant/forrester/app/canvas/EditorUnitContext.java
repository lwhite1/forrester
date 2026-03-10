package systems.courant.forrester.app.canvas;

import systems.courant.forrester.measure.CompositeUnit;
import systems.courant.forrester.measure.Dimension;
import systems.courant.forrester.measure.DimensionalAnalyzer;
import systems.courant.forrester.measure.Unit;
import systems.courant.forrester.measure.UnitRegistry;

import java.util.Map;
import java.util.Optional;

/**
 * Implements {@link DimensionalAnalyzer.UnitContext} by reading element definitions
 * from a {@link ModelEditor}. Bridges definition-layer types to {@link CompositeUnit}
 * values for dimensional analysis.
 */
final class EditorUnitContext implements DimensionalAnalyzer.UnitContext {

    private final ModelEditor editor;
    private final UnitRegistry registry;

    EditorUnitContext(ModelEditor editor, UnitRegistry registry) {
        this.editor = editor;
        this.registry = registry;
    }

    @Override
    public Optional<CompositeUnit> resolveUnit(String elementName) {
        String resolved = elementName.replace('_', ' ');

        // Check stocks
        var stockOpt = editor.getStockByName(elementName);
        if (stockOpt.isEmpty()) {
            stockOpt = editor.getStockByName(resolved);
        }
        if (stockOpt.isPresent()) {
            String unitName = stockOpt.get().unit();
            if (unitName != null && !unitName.isBlank()) {
                Unit unit = registry.resolve(unitName);
                return Optional.of(CompositeUnit.of(unit));
            }
            return Optional.of(CompositeUnit.dimensionless());
        }

        // Check flows — return rate dimension (material / time)
        var flowOpt = editor.getFlowByName(elementName);
        if (flowOpt.isEmpty()) {
            flowOpt = editor.getFlowByName(resolved);
        }
        if (flowOpt.isPresent()) {
            var flow = flowOpt.get();
            Unit materialUnit = resolveMaterialUnit(flow);
            return Optional.of(CompositeUnit.ofRate(materialUnit,
                    registry.resolveTimeUnit(flow.timeUnit())));
        }

        // Check auxiliaries
        var auxOpt = editor.getAuxByName(elementName);
        if (auxOpt.isEmpty()) {
            auxOpt = editor.getAuxByName(resolved);
        }
        if (auxOpt.isPresent()) {
            String unitName = auxOpt.get().unit();
            if (unitName != null && !unitName.isBlank()) {
                Unit unit = registry.resolve(unitName);
                return Optional.of(CompositeUnit.of(unit));
            }
            return Optional.of(CompositeUnit.dimensionless());
        }

        // Check lookup tables — return dimensionless
        var lookupNames = editor.getLookupTables().stream()
                .map(t -> t.name())
                .toList();
        if (lookupNames.contains(elementName) || lookupNames.contains(resolved)) {
            return Optional.of(CompositeUnit.dimensionless());
        }

        return Optional.empty();
    }

    /**
     * Resolves the material unit for a flow. Uses explicit materialUnit if set,
     * otherwise infers from connected stock, falling back to dimensionless.
     */
    private Unit resolveMaterialUnit(systems.courant.forrester.model.def.FlowDef flow) {
        if (flow.materialUnit() != null && !flow.materialUnit().isBlank()) {
            return registry.resolve(flow.materialUnit());
        }
        // Infer from connected stock
        if (flow.sink() != null) {
            var sink = editor.getStockByName(flow.sink());
            if (sink.isPresent() && sink.get().unit() != null && !sink.get().unit().isBlank()) {
                return registry.resolve(sink.get().unit());
            }
        }
        if (flow.source() != null) {
            var source = editor.getStockByName(flow.source());
            if (source.isPresent() && source.get().unit() != null
                    && !source.get().unit().isBlank()) {
                return registry.resolve(source.get().unit());
            }
        }
        return null;
    }
}
