package systems.courant.sd.app.canvas;

import systems.courant.sd.model.def.ConnectorRoute;
import systems.courant.sd.model.def.FlowDef;
import systems.courant.sd.model.def.ModelDefinition;
import systems.courant.sd.model.def.ModuleInstanceDef;
import systems.courant.sd.model.graph.ConnectorGenerator;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static systems.courant.sd.app.canvas.EquationReferenceManager.replaceToken;

/**
 * Manages flow endpoint reconnection and info-link rerouting operations.
 * Extracted from {@link ModelEditor} to isolate connection management logic.
 */
final class FlowConnectionManager {

    private final List<FlowDef> flows;
    private final List<ModuleInstanceDef> modules;
    private final Set<String> nameIndex;
    private final EquationReferenceManager equationRefManager;

    FlowConnectionManager(List<FlowDef> flows, List<ModuleInstanceDef> modules,
                           Set<String> nameIndex,
                           EquationReferenceManager equationRefManager) {
        this.flows = flows;
        this.modules = modules;
        this.nameIndex = nameIndex;
        this.equationRefManager = equationRefManager;
    }

    /**
     * Reconnects a flow endpoint to a different stock, or disconnects it (pass null for stockName).
     *
     * @param flowName the flow to modify
     * @param end which end (SOURCE or SINK) to reconnect
     * @param stockName the new stock to connect to, or null to disconnect (cloud)
     * @return true if the flow was found and updated
     */
    boolean reconnectFlow(String flowName, FlowEndpointCalculator.FlowEnd end,
                           String stockName) {
        // Validate: if a stock name is given, it must actually exist
        if (stockName != null && !nameIndex.contains(stockName)) {
            return false;
        }

        for (int i = 0; i < flows.size(); i++) {
            if (flows.get(i).name().equals(flowName)) {
                FlowDef f = flows.get(i);

                // Prevent self-loop: stockName must not equal the opposite endpoint
                if (stockName != null) {
                    String opposite = (end == FlowEndpointCalculator.FlowEnd.SOURCE)
                            ? f.sink() : f.source();
                    if (stockName.equals(opposite)) {
                        return false;
                    }
                }

                if (end == FlowEndpointCalculator.FlowEnd.SOURCE) {
                    flows.set(i, new FlowDef(f.name(), f.comment(), f.equation(),
                            f.timeUnit(), f.materialUnit(), stockName, f.sink(), f.subscripts()));
                } else {
                    flows.set(i, new FlowDef(f.name(), f.comment(), f.equation(),
                            f.timeUnit(), f.materialUnit(), f.source(), stockName, f.subscripts()));
                }
                return true;
            }
        }
        return false;
    }

    /**
     * Removes a single equation reference that creates the info link from {@code fromName}
     * to {@code toName}. The reference token is replaced with "0" in the target element's
     * equation only (not globally). Material flow connections are not affected.
     *
     * @return true if a reference was found and removed
     */
    boolean removeConnectionReference(String fromName, String toName) {
        String fromToken = fromName.replace(' ', '_');
        return equationRefManager.updateEquationByName(toName,
                eq -> replaceToken(eq, fromToken, "0"));
    }

    /**
     * Reroutes the source (from) end of an info link: in the target's equation,
     * replaces the old source token with the new source token.
     *
     * @return true if the equation was updated
     */
    boolean rerouteConnectionSource(String oldFrom, String newFrom, String to) {
        String oldToken = oldFrom.replace(' ', '_');
        String newToken = newFrom.replace(' ', '_');
        return equationRefManager.updateEquationByName(to,
                eq -> replaceToken(eq, oldToken, newToken));
    }

    /**
     * Reroutes the target (to) end of an info link: removes the source reference
     * from the old target's equation and adds it to the new target's equation.
     *
     * @return true if the reroute was performed
     */
    boolean rerouteConnectionTarget(String from, String oldTo, String newTo) {
        String fromToken = from.replace(' ', '_');

        // Remove reference from old target
        removeConnectionReference(from, oldTo);

        // Add reference to new target's equation
        return equationRefManager.addConnectionReference(newTo, fromToken);
    }

    /**
     * Generates connector routes from the given model definition's dependency graph,
     * including binding-derived connectors for module input/output bindings.
     */
    List<ConnectorRoute> generateConnectors(ModelDefinition modelDef) {
        List<ConnectorRoute> base = ConnectorGenerator.generate(modelDef);
        Set<String> seen = new LinkedHashSet<>();
        for (ConnectorRoute r : base) {
            seen.add(r.from() + " -> " + r.to());
        }
        List<ConnectorRoute> result = new ArrayList<>(base);

        for (ModuleInstanceDef module : modules) {
            String moduleName = module.instanceName();

            // Input bindings: single-token expressions that match an existing element
            for (Map.Entry<String, String> entry : module.inputBindings().entrySet()) {
                String expr = entry.getValue();
                if (expr != null && !expr.isBlank() && isSingleToken(expr)) {
                    String elementName = expr.replace('_', ' ');
                    if (nameIndex.contains(elementName)) {
                        String key = elementName + " -> " + moduleName;
                        if (seen.add(key)) {
                            result.add(new ConnectorRoute(elementName, moduleName));
                        }
                    }
                }
            }

            // Output bindings: alias names that match an existing element
            for (Map.Entry<String, String> entry : module.outputBindings().entrySet()) {
                String raw = entry.getValue();
                if (raw != null && !raw.isBlank()) {
                    String alias = raw.replace('_', ' ');
                    if (nameIndex.contains(alias)) {
                        String key = moduleName + " -> " + alias;
                        if (seen.add(key)) {
                            result.add(new ConnectorRoute(moduleName, alias));
                        }
                    }
                }
            }
        }

        return result;
    }

    /**
     * Returns true if the expression is a single identifier token (no operators or whitespace).
     */
    static boolean isSingleToken(String expr) {
        String trimmed = expr.trim();
        for (int i = 0; i < trimmed.length(); i++) {
            char c = trimmed.charAt(i);
            if (c == ' ' || c == '+' || c == '-' || c == '*' || c == '/'
                    || c == '(' || c == ')' || c == ',') {
                return false;
            }
        }
        return !trimmed.isEmpty();
    }
}
