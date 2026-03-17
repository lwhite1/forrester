package systems.courant.sd.app.canvas;

import systems.courant.sd.model.def.CausalLinkDef;
import systems.courant.sd.model.def.CldVariableDef;
import systems.courant.sd.model.def.CommentDef;
import systems.courant.sd.model.def.FlowDef;
import systems.courant.sd.model.def.LookupTableDef;
import systems.courant.sd.model.def.ModelDefinition;
import systems.courant.sd.model.def.ModuleInstanceDef;
import systems.courant.sd.model.def.StockDef;
import systems.courant.sd.model.def.VariableDef;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static systems.courant.sd.app.canvas.ElementNameValidator.parseIdSuffix;
import static systems.courant.sd.app.canvas.ElementNameValidator.resolveUniqueName;

/**
 * Creates new model elements with auto-generated names and manages per-type ID counters.
 * Extracted from {@link ModelEditor} to isolate element construction logic.
 */
final class ElementFactory {

    private static final Logger log = LoggerFactory.getLogger(ElementFactory.class);

    private final List<StockDef> stocks;
    private final List<FlowDef> flows;
    private final List<VariableDef> variables;
    private final List<ModuleInstanceDef> modules;
    private final List<LookupTableDef> lookupTables;
    private final List<CldVariableDef> cldVariables;
    private final List<CausalLinkDef> causalLinks;
    private final List<CommentDef> comments;
    private final Set<String> nameIndex;

    private int nextStockId = 1;
    private int nextFlowId = 1;
    private int nextVariableId = 1;
    private int nextModuleId = 1;
    private int nextLookupId = 1;
    private int nextCldVariableId = 1;
    private int nextCommentId = 1;

    ElementFactory(List<StockDef> stocks, List<FlowDef> flows,
                   List<VariableDef> variables, List<ModuleInstanceDef> modules,
                   List<LookupTableDef> lookupTables, List<CldVariableDef> cldVariables,
                   List<CausalLinkDef> causalLinks, List<CommentDef> comments,
                   Set<String> nameIndex) {
        this.stocks = stocks;
        this.flows = flows;
        this.variables = variables;
        this.modules = modules;
        this.lookupTables = lookupTables;
        this.cldVariables = cldVariables;
        this.causalLinks = causalLinks;
        this.comments = comments;
        this.nameIndex = nameIndex;
    }

    /**
     * Resets all ID counters based on the current list contents.
     * Called after {@code loadFrom()} populates the lists.
     */
    void resetCounters() {
        nextStockId = maxIdFrom(stocks.stream().map(StockDef::name), "Stock ");
        nextFlowId = maxIdFrom(flows.stream().map(FlowDef::name), "Flow ");
        nextVariableId = maxIdFrom(variables.stream().map(VariableDef::name), "Variable ");
        nextModuleId = maxIdFrom(modules.stream().map(ModuleInstanceDef::instanceName), "Module ");
        nextLookupId = maxIdFrom(lookupTables.stream().map(LookupTableDef::name), "Lookup ");
        nextCldVariableId = maxIdFrom(cldVariables.stream().map(CldVariableDef::name), "Variable ");
        nextCommentId = maxIdFrom(comments.stream().map(CommentDef::name), "Comment ");
    }

    String addStock() {
        String name = "Stock " + nextStockId++;
        stocks.add(new StockDef(name, 0, "units"));
        nameIndex.add(name);
        return name;
    }

    String addFlow(String source, String sink) {
        String name = "Flow " + nextFlowId++;
        String materialUnit = inferMaterialUnit(source, sink);
        flows.add(new FlowDef(name, null, "0", "Day", materialUnit, source, sink, List.of()));
        nameIndex.add(name);
        return name;
    }

    String addVariable() {
        String name = "Variable " + nextVariableId++;
        variables.add(new VariableDef(name, "0", "units"));
        nameIndex.add(name);
        return name;
    }

    String addStockFrom(StockDef template) {
        String name = resolveUniqueName(template.name(), "Stock ", nextStockId, nameIndex);
        if (name.startsWith("Stock ")) {
            nextStockId = parseIdSuffix(name, "Stock ") + 1;
        }
        stocks.add(new StockDef(name, template.comment(), template.initialValue(),
                template.unit(), template.negativeValuePolicy()));
        nameIndex.add(name);
        return name;
    }

    String addFlowFrom(FlowDef template, String source, String sink) {
        String name = resolveUniqueName(template.name(), "Flow ", nextFlowId, nameIndex);
        if (name.startsWith("Flow ")) {
            nextFlowId = parseIdSuffix(name, "Flow ") + 1;
        }
        String matUnit = template.materialUnit() != null
                ? template.materialUnit() : inferMaterialUnit(source, sink);
        flows.add(new FlowDef(name, template.comment(), template.equation(),
                template.timeUnit(), matUnit, source, sink, List.of()));
        nameIndex.add(name);
        return name;
    }

    String addVariableFrom(VariableDef template, String equation) {
        String name = resolveUniqueName(template.name(), "Variable ", nextVariableId, nameIndex);
        if (name.startsWith("Variable ")) {
            nextVariableId = parseIdSuffix(name, "Variable ") + 1;
        }
        variables.add(new VariableDef(name, template.comment(), equation, template.unit(),
                template.subscripts()));
        nameIndex.add(name);
        return name;
    }

    String addModuleFrom(ModuleInstanceDef template) {
        String name = resolveUniqueName(template.instanceName(), "Module ", nextModuleId, nameIndex);
        if (name.startsWith("Module ")) {
            nextModuleId = parseIdSuffix(name, "Module ") + 1;
        }
        modules.add(new ModuleInstanceDef(name, template.definition(),
                template.inputBindings(), template.outputBindings()));
        nameIndex.add(name);
        return name;
    }

    String addModule() {
        String name = "Module " + nextModuleId++;
        ModelDefinition emptyDef = new ModelDefinition(
                name, null, null,
                List.of(), List.of(), List.of(),
                List.of(), List.of(), List.of(), List.of(), null);
        modules.add(new ModuleInstanceDef(name, emptyDef, Map.of(), Map.of()));
        nameIndex.add(name);
        return name;
    }

    String addLookup() {
        String name = "Lookup " + nextLookupId++;
        lookupTables.add(new LookupTableDef(name,
                new double[]{0.0, 1.0}, new double[]{0.0, 1.0}, "LINEAR"));
        nameIndex.add(name);
        return name;
    }

    String addLookupFrom(LookupTableDef template) {
        String name = resolveUniqueName(template.name(), "Lookup ", nextLookupId, nameIndex);
        if (name.startsWith("Lookup ")) {
            nextLookupId = parseIdSuffix(name, "Lookup ") + 1;
        }
        lookupTables.add(new LookupTableDef(name, template.comment(),
                template.xValues(), template.yValues(), template.interpolation()));
        nameIndex.add(name);
        return name;
    }

    String addCldVariable() {
        String name = resolveUniqueName("Variable " + nextCldVariableId,
                "Variable ", nextCldVariableId, nameIndex);
        nextCldVariableId = parseIdSuffix(name, "Variable ") + 1;
        cldVariables.add(new CldVariableDef(name));
        nameIndex.add(name);
        return name;
    }

    String addCldVariableFrom(CldVariableDef template) {
        String name = resolveUniqueName(template.name(), "Variable ", nextCldVariableId, nameIndex);
        if (name.startsWith("Variable ")) {
            nextCldVariableId = parseIdSuffix(name, "Variable ") + 1;
        }
        cldVariables.add(new CldVariableDef(name, template.comment()));
        nameIndex.add(name);
        return name;
    }

    String addComment() {
        String name = "Comment " + nextCommentId++;
        comments.add(new CommentDef(name, ""));
        nameIndex.add(name);
        return name;
    }

    String addCommentFrom(CommentDef template) {
        String name = resolveUniqueName(template.name(), "Comment ", nextCommentId, nameIndex);
        if (name.startsWith("Comment ")) {
            nextCommentId = parseIdSuffix(name, "Comment ") + 1;
        }
        comments.add(new CommentDef(name, template.text()));
        nameIndex.add(name);
        return name;
    }

    boolean addCausalLink(String from, String to, CausalLinkDef.Polarity polarity) {
        if (!nameIndex.contains(from) || !nameIndex.contains(to)) {
            return false;
        }
        for (CausalLinkDef existing : causalLinks) {
            if (existing.from().equals(from) && existing.to().equals(to)) {
                return false;
            }
        }
        causalLinks.add(new CausalLinkDef(from, to, polarity));
        return true;
    }

    /**
     * Infers a material unit from the connected stock(s). Checks sink first, then source.
     */
    private String inferMaterialUnit(String source, String sink) {
        if (sink != null) {
            Optional<StockDef> sinkStock = findStock(sink);
            if (sinkStock.isPresent()) {
                return sinkStock.get().unit();
            }
        }
        if (source != null) {
            Optional<StockDef> sourceStock = findStock(source);
            if (sourceStock.isPresent()) {
                return sourceStock.get().unit();
            }
        }
        return null;
    }

    private Optional<StockDef> findStock(String name) {
        for (StockDef s : stocks) {
            if (s.name().equals(name)) {
                return Optional.of(s);
            }
        }
        return Optional.empty();
    }

    static int maxIdFrom(java.util.stream.Stream<String> names, String prefix) {
        int[] max = {0};
        names.forEach(name -> {
            if (name.startsWith(prefix)) {
                try {
                    int num = Integer.parseInt(name.substring(prefix.length()));
                    if (num > max[0]) {
                        max[0] = num;
                    }
                } catch (NumberFormatException ex) {
                    log.trace("Not an auto-named element: '{}'", name, ex);
                }
            }
        });
        return max[0] + 1;
    }
}
