package systems.courant.sd.app.canvas;

import systems.courant.sd.model.def.CausalLinkDef;
import systems.courant.sd.model.def.CldVariableDef;
import systems.courant.sd.model.def.CommentDef;
import systems.courant.sd.model.def.FlowDef;
import systems.courant.sd.model.def.LookupTableDef;
import systems.courant.sd.model.def.ModuleInstanceDef;
import systems.courant.sd.model.def.StockDef;
import systems.courant.sd.model.def.VariableDef;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.UnaryOperator;

/**
 * Handles cross-cutting rename and remove cascades across all element lists.
 * Extracted from {@link ModelEditor} to isolate cascade coordination logic.
 */
final class ElementCascadeManager {

    private final List<StockDef> stocks;
    private final List<FlowDef> flows;
    private final List<VariableDef> variables;
    private final List<ModuleInstanceDef> modules;
    private final List<LookupTableDef> lookupTables;
    private final List<CldVariableDef> cldVariables;
    private final List<CausalLinkDef> causalLinks;
    private final List<CommentDef> comments;
    private final Set<String> nameIndex;
    private final EquationReferenceManager equationRefManager;

    ElementCascadeManager(List<StockDef> stocks, List<FlowDef> flows,
                          List<VariableDef> variables, List<ModuleInstanceDef> modules,
                          List<LookupTableDef> lookupTables, List<CldVariableDef> cldVariables,
                          List<CausalLinkDef> causalLinks, List<CommentDef> comments,
                          Set<String> nameIndex, EquationReferenceManager equationRefManager) {
        this.stocks = stocks;
        this.flows = flows;
        this.variables = variables;
        this.modules = modules;
        this.lookupTables = lookupTables;
        this.cldVariables = cldVariables;
        this.causalLinks = causalLinks;
        this.comments = comments;
        this.nameIndex = nameIndex;
        this.equationRefManager = equationRefManager;
    }

    /**
     * Renames an element across all model data. Updates the element's own name,
     * flow source/sink references, causal links, module bindings, and equation references.
     *
     * @return true if the element was found and renamed
     */
    boolean rename(String oldName, String newName) {
        if (oldName == null || newName == null || oldName.equals(newName)) {
            return false;
        }
        if (nameIndex.contains(newName)) {
            return false;
        }

        boolean found = renameInList(stocks, oldName, newName, StockDef::name,
                (s, n) -> new StockDef(n, s.comment(), s.initialValue(),
                        s.unit(), s.negativeValuePolicy()))
                || renameInList(flows, oldName, newName, FlowDef::name,
                (f, n) -> new FlowDef(n, f.comment(), f.equation(),
                        f.timeUnit(), f.materialUnit(), f.source(), f.sink(), f.subscripts()))
                || renameInList(variables, oldName, newName, VariableDef::name,
                (a, n) -> new VariableDef(n, a.comment(), a.equation(), a.unit(), a.subscripts()))
                || renameInList(modules, oldName, newName, ModuleInstanceDef::instanceName,
                (m, n) -> new ModuleInstanceDef(n, m.definition(),
                        m.inputBindings(), m.outputBindings()))
                || renameInList(lookupTables, oldName, newName, LookupTableDef::name,
                (lt, n) -> new LookupTableDef(n, lt.comment(),
                        lt.xValues(), lt.yValues(), lt.interpolation()))
                || renameInList(cldVariables, oldName, newName, CldVariableDef::name,
                (v, n) -> new CldVariableDef(n, v.comment()))
                || renameInList(comments, oldName, newName, CommentDef::name,
                (c, n) -> new CommentDef(n, c.text()));

        if (!found) {
            return false;
        }

        nameIndex.remove(oldName);
        nameIndex.add(newName);

        // Update causal link references
        for (int i = 0; i < causalLinks.size(); i++) {
            CausalLinkDef link = causalLinks.get(i);
            boolean fromMatch = oldName.equals(link.from());
            boolean toMatch = oldName.equals(link.to());
            if (fromMatch || toMatch) {
                causalLinks.set(i, new CausalLinkDef(
                        fromMatch ? newName : link.from(),
                        toMatch ? newName : link.to(),
                        link.polarity(),
                        link.comment(),
                        link.strength()));
            }
        }

        // Update flow source/sink references
        for (int i = 0; i < flows.size(); i++) {
            FlowDef f = flows.get(i);
            boolean sourceMatch = oldName.equals(f.source());
            boolean sinkMatch = oldName.equals(f.sink());
            if (sourceMatch || sinkMatch) {
                flows.set(i, new FlowDef(
                        f.name(), f.comment(), f.equation(), f.timeUnit(),
                        f.materialUnit(),
                        sourceMatch ? newName : f.source(),
                        sinkMatch ? newName : f.sink(),
                        f.subscripts()));
            }
        }

        // Underscore-form tokens used by both binding and equation updates
        String oldToken = oldName.replace(' ', '_');
        String newToken = newName.replace(' ', '_');

        // Update module input/output bindings that reference the old name
        for (int i = 0; i < modules.size(); i++) {
            ModuleInstanceDef m = modules.get(i);
            boolean changed = false;
            Map<String, String> newInputs = new java.util.LinkedHashMap<>(m.inputBindings());
            for (Map.Entry<String, String> entry : newInputs.entrySet()) {
                if (oldName.equals(entry.getValue())) {
                    entry.setValue(newName);
                    changed = true;
                } else if (oldToken.equals(entry.getValue())) {
                    entry.setValue(newToken);
                    changed = true;
                }
            }
            Map<String, String> newOutputs = new java.util.LinkedHashMap<>(m.outputBindings());
            for (Map.Entry<String, String> entry : newOutputs.entrySet()) {
                if (oldName.equals(entry.getValue())) {
                    entry.setValue(newName);
                    changed = true;
                } else if (oldToken.equals(entry.getValue())) {
                    entry.setValue(newToken);
                    changed = true;
                }
            }
            if (changed) {
                modules.set(i, new ModuleInstanceDef(
                        m.instanceName(), m.definition(), newInputs, newOutputs));
            }
        }

        // Update equation references (underscore convention)
        equationRefManager.updateEquationReferences(oldToken, newToken);

        return true;
    }

    /**
     * Removes an element from the appropriate list, nullifying flow source/sink
     * references if a stock is removed, and cleaning up causal links.
     *
     * @return the names of elements whose equations still reference the deleted element
     */
    List<String> remove(String name) {
        nameIndex.remove(name);
        boolean wasStock = stocks.removeIf(s -> s.name().equals(name));

        if (wasStock) {
            // Nullify flow source/sink references to the deleted stock
            for (int i = 0; i < flows.size(); i++) {
                FlowDef f = flows.get(i);
                boolean sourceMatch = name.equals(f.source());
                boolean sinkMatch = name.equals(f.sink());
                if (sourceMatch || sinkMatch) {
                    flows.set(i, new FlowDef(
                            f.name(),
                            f.comment(),
                            f.equation(),
                            f.timeUnit(),
                            f.materialUnit(),
                            sourceMatch ? null : f.source(),
                            sinkMatch ? null : f.sink(),
                            f.subscripts()
                    ));
                }
            }
        }

        if (!wasStock) {
            if (flows.removeIf(f -> f.name().equals(name))) {
                // flow removed — fall through to clean equations
            } else if (variables.removeIf(a -> a.name().equals(name))) {
                // variable removed — fall through to clean equations
            } else {
                if (!lookupTables.removeIf(lt -> lt.name().equals(name))) {
                    if (!modules.removeIf(m -> m.instanceName().equals(name))) {
                        if (!cldVariables.removeIf(v -> v.name().equals(name))) {
                            comments.removeIf(c -> c.name().equals(name));
                        }
                    }
                }
            }
        }

        // Remove causal links referencing the deleted element
        causalLinks.removeIf(link -> link.from().equals(name) || link.to().equals(name));

        // Return elements whose equations still reference the deleted element
        String deletedToken = name.replace(' ', '_');
        return equationRefManager.findReferencingElements(deletedToken);
    }

    private <T> boolean renameInList(List<T> list, String oldName, String newName,
                                      Function<T, String> nameGetter,
                                      BiFunction<T, String, T> renamer) {
        return updateInList(list, oldName, nameGetter, item -> renamer.apply(item, newName));
    }

    /**
     * Finds the element with the given name in the list and replaces it with the
     * result of applying the updater function. Returns true if the element was found.
     */
    static <T> boolean updateInList(List<T> list, String name,
                                     Function<T, String> nameGetter,
                                     UnaryOperator<T> updater) {
        for (int i = 0; i < list.size(); i++) {
            if (nameGetter.apply(list.get(i)).equals(name)) {
                list.set(i, updater.apply(list.get(i)));
                return true;
            }
        }
        return false;
    }
}
