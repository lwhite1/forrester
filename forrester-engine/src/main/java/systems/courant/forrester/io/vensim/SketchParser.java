package systems.courant.forrester.io.vensim;

import systems.courant.forrester.model.def.ConnectorRoute;
import systems.courant.forrester.model.def.ElementPlacement;
import systems.courant.forrester.model.def.ElementType;
import systems.courant.forrester.model.def.FlowRoute;
import systems.courant.forrester.model.def.ViewDef;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Parses Vensim sketch section lines into {@link ViewDef} records.
 *
 * <p>Sketch line types:
 * <ul>
 *   <li>{@code *View Name} — new view declaration</li>
 *   <li>{@code 10,id,name,x,y,...} — variable/stock placement</li>
 *   <li>{@code 11,id,...,x,y,...} — flow valve placement</li>
 *   <li>{@code 12,id,...} — cloud (source/sink, skipped)</li>
 *   <li>{@code 1,id,from,to,...} — connector arrow</li>
 * </ul>
 */
public final class SketchParser {

    private SketchParser() {
    }

    /**
     * Parses sketch lines into a list of view definitions.
     *
     * @param sketchLines the raw sketch lines from the .mdl file
     * @param stockNames the set of known stock names (normalized) for type classification
     * @param flowNames the set of known flow names (normalized) for type classification
     * @param lookupNames the set of known lookup table names (normalized) for type classification
     * @return a list of view definitions
     */
    public static List<ViewDef> parse(List<String> sketchLines, Set<String> stockNames,
                                       Set<String> flowNames, Set<String> lookupNames) {
        return parse(sketchLines, stockNames, flowNames, lookupNames, Set.of());
    }

    /**
     * Parses sketch lines into a list of view definitions with CLD variable support.
     *
     * @param sketchLines the raw sketch lines from the .mdl file
     * @param stockNames the set of known stock names (normalized) for type classification
     * @param flowNames the set of known flow names (normalized) for type classification
     * @param lookupNames the set of known lookup table names (normalized) for type classification
     * @param cldVariableNames the set of CLD variable names (normalized) for type classification
     * @return a list of view definitions
     */
    public static List<ViewDef> parse(List<String> sketchLines, Set<String> stockNames,
                                       Set<String> flowNames, Set<String> lookupNames,
                                       Set<String> cldVariableNames) {
        List<ViewDef> views = new ArrayList<>();

        String currentViewName = null;
        List<ElementPlacement> elements = new ArrayList<>();
        List<ConnectorRoute> connectors = new ArrayList<>();
        List<FlowRoute> flowRoutes = new ArrayList<>();
        Map<String, String> idToName = new HashMap<>();

        for (String line : sketchLines) {
            String trimmed = line.strip();
            if (trimmed.isEmpty()) {
                continue;
            }

            // New view declaration
            if (trimmed.startsWith("*")) {
                // Save previous view if any
                if (currentViewName != null) {
                    views.add(new ViewDef(currentViewName, elements, connectors, flowRoutes));
                }
                currentViewName = trimmed.substring(1).strip();
                if (currentViewName.isEmpty()) {
                    currentViewName = "View";
                }
                elements = new ArrayList<>();
                connectors = new ArrayList<>();
                flowRoutes = new ArrayList<>();
                idToName = new HashMap<>();
                continue;
            }

            String[] parts = trimmed.split(",");
            if (parts.length < 2) {
                continue;
            }

            int lineType;
            try {
                lineType = Integer.parseInt(parts[0].strip());
            } catch (NumberFormatException e) {
                continue;
            }

            switch (lineType) {
                case 10 -> parseElementLine(parts, elements, idToName,
                        stockNames, flowNames, lookupNames, cldVariableNames);
                case 11 -> parseFlowValveLine(parts, elements, flowRoutes, idToName);
                case 12 -> {
                    // Cloud (source/sink boundary) — skip
                }
                case 1 -> parseConnectorLine(parts, connectors, idToName);
                default -> {
                    // Other line types (annotations, etc.) — skip
                }
            }
        }

        // Save last view
        if (currentViewName != null) {
            views.add(new ViewDef(currentViewName, elements, connectors, flowRoutes));
        }

        return views;
    }

    private static void parseElementLine(String[] parts, List<ElementPlacement> elements,
                                          Map<String, String> idToName,
                                          Set<String> stockNames, Set<String> flowNames,
                                          Set<String> lookupNames,
                                          Set<String> cldVariableNames) {
        // Format: 10,id,name,x,y,...
        if (parts.length < 5) {
            return;
        }
        String id = parts[1].strip();
        String rawName = parts[2].strip();
        String displayName = VensimExprTranslator.normalizeDisplayName(rawName);
        String eqName = VensimExprTranslator.normalizeName(rawName);
        if (displayName.isEmpty()) {
            return;
        }

        double x;
        double y;
        try {
            x = Double.parseDouble(parts[3].strip());
            y = Double.parseDouble(parts[4].strip());
        } catch (NumberFormatException e) {
            return;
        }

        idToName.put(id, displayName);
        ElementType type = classifyElementType(eqName, stockNames, flowNames, lookupNames,
                cldVariableNames);
        elements.add(new ElementPlacement(displayName, type, x, y));
    }

    private static void parseFlowValveLine(String[] parts, List<ElementPlacement> elements,
                                            List<FlowRoute> flowRoutes,
                                            Map<String, String> idToName) {
        // Format: 11,id,name,x,y,... (valve/flow center)
        if (parts.length < 5) {
            return;
        }
        String id = parts[1].strip();
        String rawName = parts[2].strip();
        String displayName = VensimExprTranslator.normalizeDisplayName(rawName);
        if (displayName.isEmpty()) {
            return;
        }

        double x;
        double y;
        try {
            x = Double.parseDouble(parts[3].strip());
            y = Double.parseDouble(parts[4].strip());
        } catch (NumberFormatException e) {
            return;
        }

        idToName.put(id, displayName);
        elements.add(new ElementPlacement(displayName, ElementType.FLOW, x, y));
        flowRoutes.add(new FlowRoute(displayName, List.of(new double[]{x, y})));
    }

    private static void parseConnectorLine(String[] parts, List<ConnectorRoute> connectors,
                                            Map<String, String> idToName) {
        // Format: 1,id,fromId,toId,...
        if (parts.length < 4) {
            return;
        }
        String fromId = parts[2].strip();
        String toId = parts[3].strip();

        String from = idToName.getOrDefault(fromId, fromId);
        String to = idToName.getOrDefault(toId, toId);

        if (from.isBlank() || to.isBlank()) {
            return;
        }

        connectors.add(new ConnectorRoute(from, to));
    }

    private static ElementType classifyElementType(String name, Set<String> stockNames,
                                                    Set<String> flowNames,
                                                    Set<String> lookupNames,
                                                    Set<String> cldVariableNames) {
        if (stockNames.contains(name)) {
            return ElementType.STOCK;
        }
        if (flowNames.contains(name)) {
            return ElementType.FLOW;
        }
        if (lookupNames.contains(name)) {
            return ElementType.LOOKUP;
        }
        if (cldVariableNames.contains(name)) {
            return ElementType.CLD_VARIABLE;
        }
        return ElementType.AUX;
    }
}
