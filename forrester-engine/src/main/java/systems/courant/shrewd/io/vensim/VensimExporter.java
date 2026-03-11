package systems.courant.forrester.io.vensim;

import systems.courant.forrester.model.def.AuxDef;
import systems.courant.forrester.model.def.CldVariableDef;
import systems.courant.forrester.model.def.ConnectorRoute;
import systems.courant.forrester.model.def.ElementPlacement;
import systems.courant.forrester.model.def.ElementType;
import systems.courant.forrester.model.def.FlowDef;
import systems.courant.forrester.model.def.LookupTableDef;
import systems.courant.forrester.model.def.ModelDefinition;
import systems.courant.forrester.model.def.SimulationSettings;
import systems.courant.forrester.model.def.StockDef;
import systems.courant.forrester.model.def.ViewDef;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.StringJoiner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Exports a {@link ModelDefinition} to Vensim .mdl text format.
 *
 * <p>Generates valid .mdl content that can be opened by Vensim and re-imported
 * by {@link VensimImporter}. Stocks are reconstructed as {@code INTEG()} calls
 * using the flow connectivity in the model definition.
 *
 * <p>Usage:
 * <pre>{@code
 * String mdl = VensimExporter.toVensim(modelDefinition);
 * VensimExporter.toFile(modelDefinition, Path.of("model.mdl"));
 * }</pre>
 */
public final class VensimExporter {

    private static final Pattern IF_FUNC_PATTERN = Pattern.compile("(?i)\\bIF\\s*\\(");
    private static final Pattern AND_OP_PATTERN = Pattern.compile("\\band\\b");
    private static final Pattern OR_OP_PATTERN = Pattern.compile("\\bor\\b");
    private static final Pattern NOT_OP_PATTERN = Pattern.compile("\\bnot\\b");
    private static final Pattern DOUBLE_STAR_PATTERN = Pattern.compile("\\*\\*");
    private static final Pattern DOUBLE_EQ_PATTERN = Pattern.compile("==");
    private static final Pattern NOT_EQ_PATTERN = Pattern.compile("!=");
    private static final Pattern TIME_PATTERN = Pattern.compile("\\bTIME\\b");
    private static final Pattern LOOKUP_REF_PATTERN = Pattern.compile(
            "(?i)^LOOKUP\\s*\\(");

    private VensimExporter() {
    }

    /**
     * Exports a model definition to a Vensim .mdl string.
     *
     * @param def the model definition to export
     * @return the .mdl file content
     */
    public static String toVensim(ModelDefinition def) {
        StringBuilder sb = new StringBuilder();
        sb.append("{UTF-8}\n");

        // Collect lookup names referenced by auxiliaries (embedded as WITH LOOKUP)
        Set<String> embeddedLookupNames = collectEmbeddedLookupNames(def);

        // Collect synthetic _net_flow names whose equations will be inlined into INTEG
        Set<String> inlinedFlowNames = collectInlinedFlowNames(def);

        // Write stocks
        for (StockDef stock : def.stocks()) {
            sb.append(buildStockBlock(stock, def, inlinedFlowNames));
        }

        // Write flows (skip those inlined into INTEG)
        for (FlowDef flow : def.flows()) {
            if (!inlinedFlowNames.contains(flow.name())) {
                sb.append(buildFlowBlock(flow));
            }
        }

        // Write auxiliaries
        for (AuxDef aux : def.auxiliaries()) {
            sb.append(buildAuxBlock(aux, def, embeddedLookupNames));
        }

        // Write standalone lookup tables
        for (LookupTableDef lookup : def.lookupTables()) {
            if (!embeddedLookupNames.contains(lookup.name())) {
                sb.append(buildLookupBlock(lookup));
            }
        }

        // Write CLD variables
        for (CldVariableDef cldVar : def.cldVariables()) {
            sb.append(buildCldVariableBlock(cldVar));
        }

        // Write control section
        sb.append(buildControlSection(def));

        // Sketch section
        sb.append("\\---///\n");
        for (ViewDef view : def.views()) {
            sb.append(buildSketchView(view));
        }

        return sb.toString();
    }

    /**
     * Exports a model definition to a Vensim .mdl file.
     *
     * @param def the model definition to export
     * @param path the output file path
     * @throws IOException if the file cannot be written
     */
    public static void toFile(ModelDefinition def, Path path) throws IOException {
        String mdl = toVensim(def);
        Files.writeString(path, mdl, StandardCharsets.UTF_8);
    }

    private static String buildStockBlock(StockDef stock, ModelDefinition def,
                                          Set<String> inlinedFlowNames) {
        String vensimName = denormalizeName(stock.name());

        // Find inflows and outflows from flow definitions
        List<FlowDef> inflowDefs = new ArrayList<>();
        List<FlowDef> outflowDefs = new ArrayList<>();
        for (FlowDef flow : def.flows()) {
            if (stock.name().equals(flow.sink())) {
                inflowDefs.add(flow);
            }
            if (stock.name().equals(flow.source())) {
                outflowDefs.add(flow);
            }
        }

        // Build rate expression — inline synthetic _net_flow equations directly
        String rateExpr;
        if (inflowDefs.isEmpty() && outflowDefs.isEmpty()) {
            rateExpr = "0";
        } else {
            StringBuilder rateSb = new StringBuilder();
            for (int i = 0; i < inflowDefs.size(); i++) {
                FlowDef flow = inflowDefs.get(i);
                String term = inlinedFlowNames.contains(flow.name())
                        ? toVensimExpr(flow.equation())
                        : denormalizeName(flow.name());
                if (i > 0) {
                    rateSb.append(" + ");
                }
                rateSb.append(term);
            }
            for (int i = 0; i < outflowDefs.size(); i++) {
                FlowDef flow = outflowDefs.get(i);
                String term = inlinedFlowNames.contains(flow.name())
                        ? toVensimExpr(flow.equation())
                        : denormalizeName(flow.name());
                if (inflowDefs.isEmpty() && i == 0) {
                    rateSb.append("-").append(term);
                } else {
                    rateSb.append(" - ").append(term);
                }
            }
            rateExpr = rateSb.toString();
        }

        String equation = "INTEG (\n\t" + rateExpr + ",\n\t\t"
                + formatDouble(stock.initialValue()) + ")";
        String units = stock.unit() != null ? stock.unit() : "";
        String comment = stock.comment() != null ? stock.comment() : "";
        // Stocks use "= INTEG" on the same line (no newline after "=")
        return vensimName + "= " + equation + "\n"
                + "\t~\t" + escapeForVensim(units) + "\n"
                + "\t~\t" + escapeForVensim(comment) + "\n"
                + "\t|\n\n";
    }

    private static String buildFlowBlock(FlowDef flow) {
        String vensimName = denormalizeName(flow.name());
        String equation = toVensimExpr(flow.equation());
        String units = flow.timeUnit() != null ? flow.timeUnit() : "";
        String comment = flow.comment() != null ? flow.comment() : "";
        return buildBlock(vensimName, "=", equation, units, comment);
    }

    private static String buildAuxBlock(AuxDef aux, ModelDefinition def,
                                         Set<String> embeddedLookupNames) {
        String vensimName = denormalizeName(aux.name());

        // Check if this aux references a lookup — convert to WITH LOOKUP
        Optional<String> lookupNameOpt = extractLookupReference(aux.equation());
        if (lookupNameOpt.isPresent()) {
            Optional<LookupTableDef> lookupOpt = findLookup(def, lookupNameOpt.get());
            if (lookupOpt.isPresent()) {
                Optional<String> inputExprOpt = extractLookupInput(aux.equation());
                if (inputExprOpt.isPresent()) {
                    String vensimInput = toVensimExpr(inputExprOpt.get());
                    String lookupData = formatLookupData(lookupOpt.get());
                    String equation = "WITH LOOKUP (\n\t" + vensimInput
                            + ",\n\t\t(" + lookupData + "))";
                    String units = aux.unit() != null ? aux.unit() : "";
                    String comment = aux.comment() != null ? aux.comment() : "";
                    return buildBlock(vensimName, "=", equation, units, comment);
                }
            }
        }

        // Regular auxiliary
        String equation = toVensimExpr(aux.equation());
        String units = aux.unit() != null ? aux.unit() : "";
        String comment = aux.comment() != null ? aux.comment() : "";
        return buildBlock(vensimName, "=", equation, units, comment);
    }

    private static String buildCldVariableBlock(CldVariableDef cldVar) {
        String vensimName = denormalizeName(cldVar.name());
        String comment = cldVar.comment() != null ? cldVar.comment() : "";
        return buildBlock(vensimName, "=", "0", "", comment);
    }

    private static String buildLookupBlock(LookupTableDef lookup) {
        String vensimName = denormalizeName(lookup.name());
        double[] xVals = lookup.xValues();
        double[] yVals = lookup.yValues();

        // Build range annotation: [(xmin,ymin)-(xmax,ymax)]
        double ymin = Double.MAX_VALUE;
        double ymax = -Double.MAX_VALUE;
        for (double y : yVals) {
            ymin = Math.min(ymin, y);
            ymax = Math.max(ymax, y);
        }
        String range = "[(" + formatDouble(xVals[0]) + "," + formatDouble(ymin) + ")-("
                + formatDouble(xVals[xVals.length - 1]) + "," + formatDouble(ymax) + ")]";

        // Build coordinate pairs
        StringJoiner pairs = new StringJoiner(",");
        for (int i = 0; i < xVals.length; i++) {
            pairs.add("(" + formatDouble(xVals[i]) + "," + formatDouble(yVals[i]) + ")");
        }

        String equation = range + "," + pairs;
        String units = "";
        String comment = lookup.comment() != null ? lookup.comment() : "";

        // Lookup tables use the () operator format: Name( data )
        return vensimName + "(\n\t" + equation + ")\n"
                + "\t~\t" + escapeForVensim(units) + "\n"
                + "\t~\t" + escapeForVensim(comment) + "\n"
                + "\t|\n\n";
    }

    private static String buildControlSection(ModelDefinition def) {
        StringBuilder sb = new StringBuilder();

        // Control group header
        sb.append("********************************************************\n");
        sb.append("\t.Control\n");
        sb.append("********************************************************~\n");
        sb.append("\t\tSimulation Control Parameters.\n");
        sb.append("\t|\n\n");

        SimulationSettings sim = def.defaultSimulation();
        double duration = sim != null ? sim.duration() : 100;
        String durationUnit = sim != null ? sim.durationUnit() : "Day";

        // INITIAL TIME
        sb.append(buildControlBlock("INITIAL TIME", "0", durationUnit,
                "The initial time for the simulation."));

        // FINAL TIME
        sb.append(buildControlBlock("FINAL TIME", formatDouble(duration), durationUnit,
                "The final time for the simulation."));

        // TIME STEP
        sb.append(buildControlBlock("TIME STEP", "1", durationUnit,
                "The time step for the simulation."));

        // SAVEPER — value on next line with 8-space indent (Vensim convention)
        sb.append("SAVEPER  =\n        TIME STEP\n"
                + "\t~\t" + escapeForVensim(durationUnit) + "\n"
                + "\t~\t" + escapeForVensim("The frequency with which output is stored.") + "\n"
                + "\t|\n\n");

        return sb.toString();
    }

    private static String buildSketchView(ViewDef view) {
        StringBuilder sb = new StringBuilder();
        sb.append("*").append(view.name()).append("\n");

        int nextId = 1;
        Map<String, Integer> nameToId = new HashMap<>();

        // Write element lines (type 10 for non-flow, type 11 for flow valves)
        for (ElementPlacement ep : view.elements()) {
            int id = nextId++;
            nameToId.put(ep.name(), id);

            int lineType = (ep.type() == ElementType.FLOW) ? 11 : 10;
            String vensimName = denormalizeName(ep.name());
            sb.append(lineType).append(",").append(id).append(",")
                    .append(vensimName).append(",")
                    .append(formatCoord(ep.x())).append(",")
                    .append(formatCoord(ep.y())).append("\n");
        }

        // Write connector lines (type 1)
        for (ConnectorRoute cr : view.connectors()) {
            int id = nextId++;
            String fromRef = String.valueOf(nameToId.getOrDefault(cr.from(), 0));
            String toRef = String.valueOf(nameToId.getOrDefault(cr.to(), 0));
            sb.append("1,").append(id).append(",")
                    .append(fromRef).append(",")
                    .append(toRef).append("\n");
        }

        return sb.toString();
    }

    private static String formatCoord(double value) {
        if (value == Math.floor(value) && !Double.isInfinite(value)) {
            return String.valueOf((int) value);
        }
        return String.valueOf(Math.round(value));
    }

    /**
     * Formats a single variable block in .mdl format.
     */
    private static String buildBlock(String name, String operator, String equation,
                                      String units, String comment) {
        return name + operator + "\n\t" + equation + "\n"
                + "\t~\t" + escapeForVensim(units) + "\n"
                + "\t~\t" + escapeForVensim(comment) + "\n"
                + "\t|\n\n";
    }

    /**
     * Formats a control variable block on a single line: {@code NAME  = value}.
     */
    private static String buildControlBlock(String name, String value, String units,
                                             String comment) {
        return name + "  = " + value + "\n"
                + "\t~\t" + escapeForVensim(units) + "\n"
                + "\t~\t" + escapeForVensim(comment) + "\n"
                + "\t|\n\n";
    }

    /**
     * Translates a Forrester expression to Vensim syntax.
     */
    static String toVensimExpr(String forresterExpr) {
        if (forresterExpr == null || forresterExpr.isBlank()) {
            return forresterExpr;
        }

        String expr = forresterExpr.strip();

        // IF(...) → IF THEN ELSE(...)
        expr = IF_FUNC_PATTERN.matcher(expr).replaceAll("IF THEN ELSE(");

        // and → :AND:
        expr = AND_OP_PATTERN.matcher(expr).replaceAll(":AND:");

        // or → :OR:
        expr = OR_OP_PATTERN.matcher(expr).replaceAll(":OR:");

        // not → :NOT:
        expr = NOT_OP_PATTERN.matcher(expr).replaceAll(":NOT:");

        // ** → ^ (Forrester uses ** for power, Vensim uses ^)
        expr = DOUBLE_STAR_PATTERN.matcher(expr).replaceAll("^");

        // != → <>
        expr = NOT_EQ_PATTERN.matcher(expr).replaceAll("<>");

        // == → =
        expr = DOUBLE_EQ_PATTERN.matcher(expr).replaceAll("=");

        // TIME → Time
        expr = TIME_PATTERN.matcher(expr).replaceAll("Time");

        // Denormalize variable names: replace underscores with spaces
        expr = denormalizeNamesInExpr(expr);

        return expr;
    }

    /**
     * Denormalizes a Forrester identifier back to Vensim name format.
     * Replaces underscores with spaces, and strips a leading underscore
     * that was added as a digit-prefix escape (e.g. {@code _2nd_Batch → 2nd Batch}).
     */
    static String denormalizeName(String forresterName) {
        if (forresterName == null || forresterName.isBlank()) {
            return "";
        }
        String stripped = forresterName.strip();
        String result = stripped.replace('_', ' ');
        if (stripped.length() >= 2 && stripped.charAt(0) == '_'
                && Character.isDigit(stripped.charAt(1))) {
            result = result.stripLeading();
        }
        return result;
    }

    /**
     * Denormalizes variable names within an expression.
     * Replaces underscores with spaces in identifier tokens, while preserving
     * operators, numbers, and function syntax.
     */
    private static String denormalizeNamesInExpr(String expr) {
        // Replace underscores in identifiers with spaces.
        // An identifier is a sequence of word characters that contains at least one underscore
        // and is not purely numeric.
        StringBuilder result = new StringBuilder();
        int i = 0;
        while (i < expr.length()) {
            char c = expr.charAt(i);
            if (Character.isLetter(c) || c == '_') {
                // Read the full identifier token
                int start = i;
                while (i < expr.length() && (Character.isLetterOrDigit(expr.charAt(i))
                        || expr.charAt(i) == '_')) {
                    i++;
                }
                String token = expr.substring(start, i);
                // Don't denormalize function names or known keywords
                if (isKnownFunction(token)) {
                    result.append(token);
                } else {
                    String denormed = token.replace('_', ' ');
                    if (token.length() >= 2 && token.charAt(0) == '_'
                            && Character.isDigit(token.charAt(1))) {
                        denormed = denormed.stripLeading();
                    }
                    result.append(denormed);
                }
            } else {
                result.append(c);
                i++;
            }
        }
        return result.toString();
    }

    /**
     * Checks if a token is a known function name that should not be denormalized.
     */
    private static boolean isKnownFunction(String token) {
        String upper = token.toUpperCase();
        return switch (upper) {
            case "IF", "THEN", "ELSE", "INTEG", "SMOOTH", "DELAY3", "MIN", "MAX",
                 "ABS", "EXP", "LN", "LOG", "SQRT", "SIN", "COS", "TAN",
                 "INT", "ROUND", "SUM", "MEAN",
                 "LOOKUP", "WITH", "XIDZ", "ZIDZ", "PULSE", "STEP",
                 "MODULO", "POWER", "QUANTUM",
                 "SMOOTH3", "SMOOTHI", "SMOOTH3I", "DELAY1", "DELAY1I", "RAMP",
                 "DELAY_FIXED", "TREND", "FORECAST", "NPV", "RANDOM_NORMAL",
                 "AND", "OR", "NOT", "TIME", "DT" -> true;
            default -> false;
        };
    }

    private static String formatDouble(double value) {
        if (value == Math.floor(value) && !Double.isInfinite(value)
                && Math.abs(value) < 1e15) {
            return String.valueOf((long) value);
        }
        return String.valueOf(value);
    }

    private static String escapeForVensim(String text) {
        if (text == null) {
            return "";
        }
        // Tildes and pipes are structural in .mdl — escape them if present in comments/units
        return text.replace("~", "\\~").replace("|", "\\|");
    }

    /**
     * Identifies synthetic _net_flow flows created by VensimImporter that should
     * be inlined back into INTEG calls rather than emitted as separate equation blocks.
     *
     * <p>A flow is considered synthetic if its name follows the {@code {stock}_net_flow}
     * pattern and it is the sole flow connected to that stock as an inflow with no
     * outflows (matching how VensimImporter creates a single net-flow per stock).
     */
    private static Set<String> collectInlinedFlowNames(ModelDefinition def) {
        Set<String> stockNames = new HashSet<>();
        for (StockDef stock : def.stocks()) {
            stockNames.add(stock.name());
        }

        Set<String> inlined = new HashSet<>();
        for (FlowDef flow : def.flows()) {
            // Recognize both space-form (" net flow") and legacy underscore-form ("_net_flow")
            String candidateStock;
            if (flow.name().endsWith(" net flow")) {
                candidateStock = flow.name().substring(0,
                        flow.name().length() - " net flow".length());
            } else if (flow.name().endsWith("_net_flow")) {
                candidateStock = flow.name().substring(0,
                        flow.name().length() - "_net_flow".length());
            } else {
                continue;
            }
            // Check if this matches the pattern: sink is a stock, source is null,
            // and the stock name is a prefix of the flow name
            if (flow.sink() != null && flow.sink().equals(candidateStock)
                    && flow.source() == null && stockNames.contains(candidateStock)) {
                inlined.add(flow.name());
            }
        }
        return inlined;
    }

    private static Set<String> collectEmbeddedLookupNames(ModelDefinition def) {
        Set<String> names = new HashSet<>();
        for (AuxDef aux : def.auxiliaries()) {
            extractLookupReference(aux.equation()).ifPresent(names::add);
        }
        return names;
    }

    /**
     * Extracts the lookup table name from a LOOKUP(name, input) expression.
     */
    static Optional<String> extractLookupReference(String equation) {
        if (equation == null) {
            return Optional.empty();
        }
        Matcher m = LOOKUP_REF_PATTERN.matcher(equation.strip());
        if (!m.find()) {
            return Optional.empty();
        }
        int openParen = equation.strip().indexOf('(');
        int comma = findTopLevelComma(equation.strip(), openParen + 1);
        if (comma < 0) {
            return Optional.empty();
        }
        return Optional.of(equation.strip().substring(openParen + 1, comma).strip());
    }

    /**
     * Extracts the input expression from a LOOKUP(name, input) expression.
     */
    static Optional<String> extractLookupInput(String equation) {
        if (equation == null) {
            return Optional.empty();
        }
        String trimmed = equation.strip();
        Matcher m = LOOKUP_REF_PATTERN.matcher(trimmed);
        if (!m.find()) {
            return Optional.empty();
        }
        int openParen = trimmed.indexOf('(');
        int comma = findTopLevelComma(trimmed, openParen + 1);
        if (comma < 0) {
            return Optional.empty();
        }
        int closeParen = trimmed.lastIndexOf(')');
        if (closeParen <= comma) {
            return Optional.empty();
        }
        return Optional.of(trimmed.substring(comma + 1, closeParen).strip());
    }

    private static String formatLookupData(LookupTableDef lookup) {
        double[] xVals = lookup.xValues();
        double[] yVals = lookup.yValues();

        double ymin = Double.MAX_VALUE;
        double ymax = -Double.MAX_VALUE;
        for (double y : yVals) {
            ymin = Math.min(ymin, y);
            ymax = Math.max(ymax, y);
        }

        String range = "[(" + formatDouble(xVals[0]) + "," + formatDouble(ymin) + ")-("
                + formatDouble(xVals[xVals.length - 1]) + "," + formatDouble(ymax) + ")]";

        StringJoiner pairs = new StringJoiner(",");
        for (int i = 0; i < xVals.length; i++) {
            pairs.add("(" + formatDouble(xVals[i]) + "," + formatDouble(yVals[i]) + ")");
        }

        return range + "," + pairs;
    }

    private static Optional<LookupTableDef> findLookup(ModelDefinition def, String name) {
        for (LookupTableDef lt : def.lookupTables()) {
            if (lt.name().equals(name)) {
                return Optional.of(lt);
            }
        }
        return Optional.empty();
    }

    private static int findTopLevelComma(String content, int startPos) {
        int depth = 0;
        for (int i = startPos; i < content.length(); i++) {
            char c = content.charAt(i);
            if (c == '(') {
                depth++;
            } else if (c == ')') {
                if (depth == 0) {
                    return -1;
                }
                depth--;
            } else if (c == ',' && depth == 0) {
                return i;
            }
        }
        return -1;
    }
}
