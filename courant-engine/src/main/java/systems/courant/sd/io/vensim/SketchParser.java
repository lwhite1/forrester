package systems.courant.sd.io.vensim;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import systems.courant.sd.model.def.CausalLinkDef;
import systems.courant.sd.model.def.CommentDef;
import systems.courant.sd.model.def.ConnectorRoute;
import systems.courant.sd.model.def.ElementPlacement;
import systems.courant.sd.model.def.ElementType;
import systems.courant.sd.model.def.FlowRoute;
import systems.courant.sd.model.def.ViewDef;

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
 *   <li>{@code 12,id,0,x,y,...} — text annotation (text on next line)</li>
 *   <li>{@code 12,id,48|2,...} — cloud (source/sink, skipped)</li>
 *   <li>{@code 1,id,from,to,...} — connector arrow</li>
 * </ul>
 */
public final class SketchParser {

    private static final Logger logger = LoggerFactory.getLogger(SketchParser.class);

    /**
     * Result of parsing sketch lines, containing view definitions and comment definitions.
     *
     * @param views the parsed view definitions (element placements, connectors, flow routes)
     * @param comments the comment definitions extracted from sketch text annotations
     */
    public record ParseResult(List<ViewDef> views, List<CommentDef> comments) {
        public ParseResult {
            views = List.copyOf(views);
            comments = List.copyOf(comments);
        }
    }

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
        return parseWithComments(sketchLines, stockNames, flowNames, lookupNames,
                cldVariableNames).views();
    }

    /**
     * Parses sketch lines into view definitions and comment definitions.
     *
     * <p>Text annotations (type 12, subtype 0) are converted to {@link CommentDef} elements
     * with their original canvas positions preserved as {@link ElementPlacement} entries.
     *
     * @param sketchLines the raw sketch lines from the .mdl file
     * @param stockNames the set of known stock names (normalized) for type classification
     * @param flowNames the set of known flow names (normalized) for type classification
     * @param lookupNames the set of known lookup table names (normalized) for type classification
     * @param cldVariableNames the set of CLD variable names (normalized) for type classification
     * @return a parse result containing view definitions and comment definitions
     */
    public static ParseResult parseWithComments(List<String> sketchLines, Set<String> stockNames,
                                                 Set<String> flowNames, Set<String> lookupNames,
                                                 Set<String> cldVariableNames) {
        List<ViewDef> views = new ArrayList<>();
        List<CommentDef> comments = new ArrayList<>();

        String currentViewName = null;
        List<ElementPlacement> elements = new ArrayList<>();
        List<ConnectorRoute> connectors = new ArrayList<>();
        List<FlowRoute> flowRoutes = new ArrayList<>();
        Map<String, String> idToName = new HashMap<>();

        for (int i = 0; i < sketchLines.size(); i++) {
            String trimmed = sketchLines.get(i).strip();
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
                case 12 -> i = parseType12Line(parts, i, sketchLines, elements, comments);
                case 1 -> parseConnectorLine(parts, connectors, idToName);
                default -> {
                    // Other line types — skip
                }
            }
        }

        // Save last view
        if (currentViewName != null) {
            views.add(new ViewDef(currentViewName, elements, connectors, flowRoutes));
        }

        return new ParseResult(views, comments);
    }

    /**
     * Parses a type 12 line. Subtype 0 lines are text annotations with the text on the
     * following line. Other subtypes (48, 2, etc.) are clouds and are skipped.
     *
     * @return the updated line index (advanced past the text line for subtype 0)
     */
    private static int parseType12Line(String[] parts, int currentIndex,
                                        List<String> sketchLines,
                                        List<ElementPlacement> elements,
                                        List<CommentDef> comments) {
        // Format: 12,id,subtype,x,y,width,height,...
        if (parts.length < 7) {
            return currentIndex;
        }

        int subtype;
        try {
            subtype = Integer.parseInt(parts[2].strip());
        } catch (NumberFormatException e) {
            return currentIndex;
        }

        if (subtype != 0) {
            // Cloud (source/sink boundary) — skip
            return currentIndex;
        }

        // Subtype 0 = text annotation; text is on the next line
        double x;
        double y;
        double width;
        double height;
        try {
            x = Double.parseDouble(parts[3].strip());
            y = Double.parseDouble(parts[4].strip());
            width = Double.parseDouble(parts[5].strip()) * 2;
            height = Double.parseDouble(parts[6].strip()) * 2;
        } catch (NumberFormatException e) {
            return currentIndex;
        }

        // Read annotation text from next line
        int nextIndex = currentIndex + 1;
        if (nextIndex >= sketchLines.size()) {
            return currentIndex;
        }
        String text = sketchLines.get(nextIndex).strip();
        if (text.isEmpty()) {
            return currentIndex;
        }

        String name = "Comment " + (comments.size() + 1);
        comments.add(new CommentDef(name, text));
        elements.add(new ElementPlacement(name, ElementType.COMMENT, x, y, width, height));

        return nextIndex;
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
        // Format: 1,id,fromId,toId[,shape,hidden,polarity,...]
        // Polarity is at field [6] as an ASCII code: 43 = '+', 45 = '-', 0 = none
        if (parts.length < 4) {
            return;
        }
        String fromId = parts[2].strip();
        String toId = parts[3].strip();

        String from = idToName.get(fromId);
        String to = idToName.get(toId);

        if (from == null || to == null) {
            if (from == null) {
                logger.warn("Connector references unknown element ID '{}'; skipping connector", fromId);
            }
            if (to == null) {
                logger.warn("Connector references unknown element ID '{}'; skipping connector", toId);
            }
            return;
        }

        if (from.isBlank() || to.isBlank()) {
            return;
        }

        CausalLinkDef.Polarity polarity = CausalLinkDef.Polarity.UNKNOWN;
        if (parts.length >= 7) {
            try {
                int code = Integer.parseInt(parts[6].strip());
                polarity = switch (code) {
                    case 43 -> CausalLinkDef.Polarity.POSITIVE;  // ASCII '+'
                    case 45 -> CausalLinkDef.Polarity.NEGATIVE;  // ASCII '-'
                    default -> CausalLinkDef.Polarity.UNKNOWN;
                };
            } catch (NumberFormatException e) {
                // Non-numeric polarity field — default to UNKNOWN
            }
        }

        connectors.add(new ConnectorRoute(from, to, polarity));
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
