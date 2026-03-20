package systems.courant.sd.io.vensim;

import systems.courant.sd.io.ExportUtils;
import systems.courant.sd.io.FormatUtils;
import systems.courant.sd.model.def.VariableDef;
import systems.courant.sd.model.def.CausalLinkDef;
import systems.courant.sd.model.def.CldVariableDef;
import systems.courant.sd.model.def.ConnectorRoute;
import systems.courant.sd.model.def.ElementPlacement;
import systems.courant.sd.model.def.ElementType;
import systems.courant.sd.model.def.FlowDef;
import systems.courant.sd.model.def.LookupTableDef;
import systems.courant.sd.model.def.ModelDefinition;
import systems.courant.sd.model.def.SimulationSettings;
import systems.courant.sd.model.def.StockDef;
import systems.courant.sd.model.def.SubscriptDef;
import systems.courant.sd.model.def.ViewDef;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
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

    private static final Logger logger = LoggerFactory.getLogger(VensimExporter.class);

    /** System variable names that are already emitted in the control section. */
    private static final Set<String> SYSTEM_VAR_NAMES = Set.of(
            "TIME_STEP", "INITIAL_TIME", "FINAL_TIME", "SAVEPER");

    private static final Pattern IF_FUNC_PATTERN = Pattern.compile("(?i)\\bIF\\s*\\(");
    private static final Pattern AND_OP_PATTERN = Pattern.compile("\\band\\b", Pattern.CASE_INSENSITIVE);
    private static final Pattern OR_OP_PATTERN = Pattern.compile("\\bor\\b", Pattern.CASE_INSENSITIVE);
    private static final Pattern NOT_OP_PATTERN = Pattern.compile("\\bnot\\b", Pattern.CASE_INSENSITIVE);
    private static final Pattern DOUBLE_STAR_PATTERN = Pattern.compile("\\*\\*");
    private static final Pattern DOUBLE_EQ_PATTERN = Pattern.compile("==");
    private static final Pattern NOT_EQ_PATTERN = Pattern.compile("!=");
    private static final Pattern TIME_PATTERN = Pattern.compile("\\bTIME\\b");
    private static final Pattern EMBEDDED_LOOKUP_PATTERN = Pattern.compile(
            "(?i)\\bLOOKUP\\s*\\(");
    private static final Pattern DELAY_FIXED_EXPORT_PATTERN = Pattern.compile(
            "(?i)\\bDELAY_FIXED\\s*\\(");
    private static final Pattern RANDOM_UNIFORM_EXPORT_PATTERN = Pattern.compile(
            "(?i)\\bRANDOM_UNIFORM\\s*\\(");
    private static final Pattern PULSE_TRAIN_EXPORT_PATTERN = Pattern.compile(
            "(?i)\\bPULSE_TRAIN\\s*\\(");
    private static final Pattern SAMPLE_IF_TRUE_EXPORT_PATTERN = Pattern.compile(
            "(?i)\\bSAMPLE_IF_TRUE\\s*\\(");
    private static final Pattern FIND_ZERO_EXPORT_PATTERN = Pattern.compile(
            "(?i)\\bFIND_ZERO\\s*\\(");
    private static final Pattern LOOKUP_AREA_EXPORT_PATTERN = Pattern.compile(
            "(?i)\\bLOOKUP_AREA\\s*\\(");

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

        // Build name map for expression denormalization (normalized → display name)
        Map<String, String> nameMap = buildNameMap(def);

        // Collect lookup names referenced by variables (embedded as WITH LOOKUP)
        Set<String> embeddedLookupNames = ExportUtils.collectEmbeddedLookupNames(def);

        // Collect synthetic _net_flow names whose equations will be inlined into INTEG
        Set<String> inlinedFlowNames = collectInlinedFlowNames(def);

        // Write subscript definitions
        for (SubscriptDef subscript : def.subscripts()) {
            sb.append(buildSubscriptBlock(subscript));
        }

        // Write stocks
        for (StockDef stock : def.stocks()) {
            sb.append(buildStockBlock(stock, def, inlinedFlowNames, nameMap));
        }

        // Write flows (skip those inlined into INTEG)
        for (FlowDef flow : def.flows()) {
            if (!inlinedFlowNames.contains(flow.name())) {
                sb.append(buildFlowBlock(flow, nameMap));
            }
        }

        // Write variables (skip system vars — already in control section)
        for (VariableDef v : def.variables()) {
            if (isSystemVar(v.name())) {
                continue;
            }
            sb.append(buildVariableBlock(v, def, embeddedLookupNames, nameMap));
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
        sb.append("\\\\\\---///\n");
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
                                          Set<String> inlinedFlowNames,
                                          Map<String, String> nameMap) {
        String vensimName = denormalizeName(stock.name())
                + formatSubscriptSuffix(stock.subscripts());

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
                        ? toVensimExpr(flow.equation(), nameMap)
                        : denormalizeName(flow.name());
                if (i > 0) {
                    rateSb.append(" + ");
                }
                rateSb.append(term);
            }
            for (int i = 0; i < outflowDefs.size(); i++) {
                FlowDef flow = outflowDefs.get(i);
                String term = inlinedFlowNames.contains(flow.name())
                        ? toVensimExpr(flow.equation(), nameMap)
                        : denormalizeName(flow.name());
                if (inflowDefs.isEmpty() && i == 0) {
                    rateSb.append("-").append(term);
                } else {
                    rateSb.append(" - ").append(term);
                }
            }
            rateExpr = rateSb.toString();
        }

        String initialStr = (stock.initialExpression() != null && !stock.initialExpression().isBlank())
                ? toVensimExpr(stock.initialExpression(), nameMap)
                : formatDouble(stock.initialValue());
        String equation = "INTEG (\n\t" + rateExpr + ",\n\t\t"
                + initialStr + ")";
        String units = stock.unit() != null ? stock.unit() : "";
        String comment = stock.comment() != null ? stock.comment() : "";
        // Stocks use "= INTEG" on the same line (no newline after "=")
        return vensimName + "= " + equation + "\n"
                + "\t~\t" + escapeForVensim(units) + "\n"
                + "\t~\t" + escapeForVensim(comment) + "\n"
                + "\t|\n\n";
    }

    private static String buildFlowBlock(FlowDef flow, Map<String, String> nameMap) {
        String vensimName = denormalizeName(flow.name())
                + formatSubscriptSuffix(flow.subscripts());
        String equation = toVensimExpr(flow.equation(), nameMap);
        String units = flow.timeUnit() != null ? flow.timeUnit() : "";
        String comment = flow.comment() != null ? flow.comment() : "";
        return buildBlock(vensimName, "=", equation, units, comment);
    }

    private static String buildVariableBlock(VariableDef v, ModelDefinition def,
                                         Set<String> embeddedLookupNames,
                                         Map<String, String> nameMap) {
        String vensimName = denormalizeName(v.name())
                + formatSubscriptSuffix(v.subscripts());

        // Check if this variable is a simple LOOKUP(name, input) — convert to WITH LOOKUP
        Optional<String> lookupNameOpt = ExportUtils.extractLookupReference(v.equation());
        if (lookupNameOpt.isPresent()) {
            Optional<LookupTableDef> lookupOpt = ExportUtils.findLookup(def, lookupNameOpt.get());
            if (lookupOpt.isPresent()) {
                Optional<String> inputExprOpt = ExportUtils.extractLookupInput(v.equation());
                if (inputExprOpt.isPresent()) {
                    String vensimInput = toVensimExpr(inputExprOpt.get(), nameMap);
                    String lookupData = formatLookupData(lookupOpt.get());
                    String equation = "WITH LOOKUP (\n\t" + vensimInput
                            + ",\n\t\t(" + lookupData + "))";
                    String units = v.unit() != null ? v.unit() : "";
                    String comment = v.comment() != null ? v.comment() : "";
                    return buildBlock(vensimName, "=", equation, units, comment);
                }
            }
        }

        // Check for LOOKUP calls embedded in complex expressions
        String equation = inlineLookupCalls(v.equation(), def, embeddedLookupNames);
        equation = toVensimExpr(equation, nameMap);
        String units = v.unit() != null ? v.unit() : "";
        String comment = v.comment() != null ? v.comment() : "";
        return buildBlock(vensimName, "=", equation, units, comment);
    }

    private static String buildCldVariableBlock(CldVariableDef cldVar) {
        String vensimName = denormalizeName(cldVar.name());
        String comment = cldVar.comment() != null ? cldVar.comment() : "";
        return buildBlock(vensimName, "=", "0", "", comment);
    }

    private static String buildSubscriptBlock(SubscriptDef subscript) {
        String vensimName = denormalizeName(subscript.name());
        StringJoiner labels = new StringJoiner(", ");
        for (String label : subscript.labels()) {
            labels.add(denormalizeName(label));
        }
        return vensimName + ":\n\t" + labels + "\n"
                + "\t~\t\n"
                + "\t~\t\n"
                + "\t|\n\n";
    }

    /**
     * Formats a subscript suffix for a variable name, e.g. {@code [Region, Age]}.
     */
    private static String formatSubscriptSuffix(List<String> subscripts) {
        if (subscripts == null || subscripts.isEmpty()) {
            return "";
        }
        StringJoiner sj = new StringJoiner(",");
        for (String s : subscripts) {
            sj.add(denormalizeName(s));
        }
        return "[" + sj + "]";
    }

    private static String buildLookupBlock(LookupTableDef lookup) {
        String vensimName = denormalizeName(lookup.name());
        double[] xVals = lookup.xValues();
        double[] yVals = lookup.yValues();

        if (xVals.length == 0) {
            String comment = lookup.comment() != null ? lookup.comment() : "";
            return vensimName + "(\n\t[(0,0)-(0,0)])\n"
                    + "\t~\t\n"
                    + "\t~\t" + escapeForVensim(comment) + "\n"
                    + "\t|\n\n";
        }

        // Build range annotation: [(xmin,ymin)-(xmax,ymax)]
        double ymin = Double.MAX_VALUE;
        double ymax = -Double.MAX_VALUE;
        for (double y : yVals) {
            if (!Double.isNaN(y)) {
                ymin = Math.min(ymin, y);
                ymax = Math.max(ymax, y);
            }
        }
        if (ymin == Double.MAX_VALUE) {
            ymin = 0;
            ymax = 0;
        }
        String range = "[(" + formatDouble(xVals[0]) + "," + formatDouble(ymin) + ")-("
                + formatDouble(xVals[xVals.length - 1]) + "," + formatDouble(ymax) + ")]";

        // Build coordinate pairs
        StringJoiner pairs = new StringJoiner(",");
        for (int i = 0; i < xVals.length; i++) {
            pairs.add("(" + formatDouble(xVals[i]) + "," + formatDouble(yVals[i]) + ")");
        }

        String equation = range + "," + pairs;
        String units = lookup.unit() != null ? lookup.unit() : "";
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
        double initialTime = sim != null ? sim.initialTime() : 0;
        sb.append(buildControlBlock("INITIAL TIME", formatDouble(initialTime), durationUnit,
                "The initial time for the simulation."));

        // FINAL TIME
        sb.append(buildControlBlock("FINAL TIME", formatDouble(initialTime + duration), durationUnit,
                "The final time for the simulation."));

        // TIME STEP
        double dt = sim != null ? sim.dt() : 1;
        sb.append(buildControlBlock("TIME STEP", formatDouble(dt), durationUnit,
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

        // Write connector lines (type 1) — skip connectors with unresolved endpoints
        for (ConnectorRoute cr : view.connectors()) {
            Integer fromId = nameToId.get(cr.from());
            Integer toId = nameToId.get(cr.to());
            if (fromId == null || toId == null) {
                logger.warn("Skipping connector from '{}' to '{}': endpoint not found in view",
                        cr.from(), cr.to());
                continue;
            }
            int id = nextId++;
            sb.append("1,").append(id).append(",")
                    .append(fromId).append(",")
                    .append(toId);
            if (cr.polarity() != CausalLinkDef.Polarity.UNKNOWN) {
                int polarityCode = cr.polarity() == CausalLinkDef.Polarity.POSITIVE ? 43 : 45;
                sb.append(",1,0,").append(polarityCode);
            }
            sb.append("\n");
        }

        return sb.toString();
    }

    static String formatCoord(double value) {
        if (value == Math.floor(value) && !Double.isInfinite(value)
                && Math.abs(value) < 1e15) {
            return String.valueOf((long) value);
        }
        return String.valueOf(value);
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
     * Translates a Courant expression to Vensim syntax.
     * Backward-compatible version without a name map — falls back to replacing
     * all underscores with spaces in identifier tokens.
     */
    static String toVensimExpr(String sdExpr) {
        return toVensimExpr(sdExpr, Map.of());
    }

    /**
     * Translates a Courant expression to Vensim syntax using a name map.
     * The name map (normalized name → display name) preserves the original
     * underscore/space distinction for round-trip fidelity.
     */
    static String toVensimExpr(String sdExpr, Map<String, String> nameMap) {
        if (sdExpr == null || sdExpr.isBlank()) {
            return sdExpr;
        }

        String expr = sdExpr.strip();

        // Reverse XIDZ/ZIDZ patterns before general IF translation
        expr = reverseXidzZidz(expr);

        // DELAY_FIXED → DELAY FIXED
        expr = DELAY_FIXED_EXPORT_PATTERN.matcher(expr).replaceAll("DELAY FIXED(");

        // RANDOM_UNIFORM → RANDOM UNIFORM
        expr = RANDOM_UNIFORM_EXPORT_PATTERN.matcher(expr).replaceAll("RANDOM UNIFORM(");

        // PULSE_TRAIN → PULSE TRAIN
        expr = PULSE_TRAIN_EXPORT_PATTERN.matcher(expr).replaceAll("PULSE TRAIN(");

        // SAMPLE_IF_TRUE → SAMPLE IF TRUE
        expr = SAMPLE_IF_TRUE_EXPORT_PATTERN.matcher(expr).replaceAll("SAMPLE IF TRUE(");

        // FIND_ZERO → FIND ZERO
        expr = FIND_ZERO_EXPORT_PATTERN.matcher(expr).replaceAll("FIND ZERO(");

        // LOOKUP_AREA → LOOKUP AREA
        expr = LOOKUP_AREA_EXPORT_PATTERN.matcher(expr).replaceAll("LOOKUP AREA(");

        // IF(...) → IF THEN ELSE(...)
        expr = IF_FUNC_PATTERN.matcher(expr).replaceAll("IF THEN ELSE(");

        // and → :AND:
        expr = AND_OP_PATTERN.matcher(expr).replaceAll(":AND:");

        // or → :OR:
        expr = OR_OP_PATTERN.matcher(expr).replaceAll(":OR:");

        // not → :NOT:
        expr = NOT_OP_PATTERN.matcher(expr).replaceAll(":NOT:");

        // ** → ^ (Courant uses ** for power, Vensim uses ^)
        expr = DOUBLE_STAR_PATTERN.matcher(expr).replaceAll("^");

        // == → = (must precede != to avoid mangling malformed !==)
        expr = DOUBLE_EQ_PATTERN.matcher(expr).replaceAll("=");

        // != → <>
        expr = NOT_EQ_PATTERN.matcher(expr).replaceAll("<>");

        // TIME → Time
        expr = TIME_PATTERN.matcher(expr).replaceAll("Time");

        // Denormalize variable names using the name map
        expr = denormalizeNamesInExpr(expr, nameMap);

        return expr;
    }

    /**
     * Detects IF patterns produced by XIDZ/ZIDZ import and reverses them.
     *
     * <p>XIDZ(a, b, x) was imported as: {@code IF((b) == 0, x, (a) / (b))}
     * <p>ZIDZ(a, b)    was imported as: {@code IF((b) == 0, 0, (a) / (b))}
     */
    static String reverseXidzZidz(String expr) {
        // Pattern: IF((B) == 0, X, (A) / (B))
        Pattern pattern = Pattern.compile("(?i)\\bIF\\s*\\(");
        int searchFrom = 0;
        while (searchFrom < expr.length()) {
            Matcher m = pattern.matcher(expr);
            if (!m.find(searchFrom)) {
                break;
            }
            int funcStart = m.start();
            int openParen = m.end() - 1;
            int closeParen = FormatUtils.findMatchingCloseParen(expr, openParen);
            if (closeParen < 0) {
                break;
            }
            String argsContent = expr.substring(openParen + 1, closeParen);
            List<String> args = splitTopLevelArgs(argsContent);
            if (args.size() != 3) {
                // Not a 3-arg IF — skip past this IF call
                searchFrom = closeParen + 1;
                continue;
            }
            String condition = args.get(0).strip();
            String thenExpr = args.get(1).strip();
            String elseExpr = args.get(2).strip();

            // Check for (B) == 0 pattern
            String bFromCondition = extractEqZeroOperand(condition);
            if (bFromCondition == null) {
                searchFrom = closeParen + 1;
                continue;
            }

            // Check for (A) / (B) in elseExpr, where B matches bFromCondition
            String[] divParts = extractDivision(elseExpr, bFromCondition);
            if (divParts == null) {
                searchFrom = closeParen + 1;
                continue;
            }
            String a = divParts[0];
            String b = divParts[1];

            String replacement;
            if ("0".equals(thenExpr)) {
                // ZIDZ pattern
                replacement = "ZIDZ(" + a + ", " + b + ")";
            } else {
                // XIDZ pattern
                replacement = "XIDZ(" + a + ", " + b + ", " + thenExpr + ")";
            }
            expr = expr.substring(0, funcStart) + replacement + expr.substring(closeParen + 1);
            // After replacement, continue searching from after the replacement
            searchFrom = funcStart + replacement.length();
        }
        return expr;
    }

    /**
     * Extracts operand from a "(X) == 0" or "X == 0" pattern.
     * Returns the operand text (without outer parens), or null if no match.
     */
    private static String extractEqZeroOperand(String condition) {
        String trimmed = condition.strip();
        // Check for "(expr) == 0" pattern with balanced parens
        if (trimmed.startsWith("(")) {
            int closeParen = FormatUtils.findMatchingCloseParen(trimmed, 0);
            if (closeParen > 0) {
                String remainder = trimmed.substring(closeParen + 1).strip();
                if (remainder.matches("==\\s*0")) {
                    return trimmed.substring(1, closeParen).strip();
                }
            }
        }
        // Check for "expr == 0" pattern without parens
        if (trimmed.matches(".+\\s*==\\s*0$")) {
            int eqPos = trimmed.lastIndexOf("==");
            if (eqPos > 0) {
                return trimmed.substring(0, eqPos).strip();
            }
        }
        return null;
    }

    /**
     * Checks if expr is "(A) / (B)" where B matches expectedB.
     * Returns {A, B} or null.
     */
    private static String[] extractDivision(String expr, String expectedB) {
        // Find the top-level "/" operator
        int depth = 0;
        int divPos = -1;
        for (int i = 0; i < expr.length(); i++) {
            char c = expr.charAt(i);
            if (c == '(') {
                depth++;
            } else if (c == ')') {
                depth--;
            } else if (c == '/' && depth == 0) {
                divPos = i;
                break;
            }
        }
        if (divPos < 0) {
            return null;
        }
        String lhs = stripOuterParens(expr.substring(0, divPos).strip());
        String rhs = stripOuterParens(expr.substring(divPos + 1).strip());
        if (rhs.equals(expectedB)) {
            return new String[]{lhs, rhs};
        }
        return null;
    }

    private static String stripOuterParens(String s) {
        String trimmed = s.strip();
        if (trimmed.startsWith("(") && trimmed.endsWith(")")) {
            // Verify the parens are matched (not just "(...) + (...)")
            int depth = 0;
            for (int i = 0; i < trimmed.length() - 1; i++) {
                char c = trimmed.charAt(i);
                if (c == '(') {
                    depth++;
                } else if (c == ')') {
                    depth--;
                }
                if (depth == 0) {
                    return trimmed; // parens don't wrap the whole expression
                }
            }
            return trimmed.substring(1, trimmed.length() - 1).strip();
        }
        return trimmed;
    }

    private static List<String> splitTopLevelArgs(String content) {
        List<String> args = new ArrayList<>();
        int depth = 0;
        int start = 0;
        for (int i = 0; i < content.length(); i++) {
            char c = content.charAt(i);
            if (c == '(') {
                depth++;
            } else if (c == ')') {
                depth--;
            } else if (c == ',' && depth == 0) {
                args.add(content.substring(start, i));
                start = i + 1;
            }
        }
        args.add(content.substring(start));
        return args;
    }

    /**
     * Denormalizes a Courant name back to Vensim name format.
     *
     * <p>If the name already contains spaces (i.e. it came from VensimImporter's
     * {@code normalizeDisplayName} which preserves spaces), underscores are treated
     * as literal characters and preserved. Otherwise (XMILE import, native Courant
     * names), underscores are treated as word separators and replaced with spaces.
     *
     * <p>Leading underscore digit-prefix escapes (e.g. {@code _2nd}) are always
     * removed regardless of format.
     */
    static String denormalizeName(String sdName) {
        if (sdName == null || sdName.isBlank()) {
            return "";
        }
        String stripped = sdName.strip();
        // Strip digit-prefix escape: _2nd... → 2nd...
        if (stripped.length() >= 2 && stripped.charAt(0) == '_'
                && Character.isDigit(stripped.charAt(1))) {
            stripped = stripped.substring(1);
        }
        // If the name already contains spaces (Vensim display-name format),
        // underscores are literal — preserve them. Otherwise replace with spaces.
        if (!stripped.contains(" ")) {
            return stripped.replace('_', ' ');
        }
        return stripped;
    }

    /**
     * Denormalizes variable names within an expression using a name map.
     * Looks up each identifier token in the map (normalized name → display name)
     * to preserve the original underscore/space distinction. Falls back to
     * replacing underscores with spaces for identifiers not found in the map.
     *
     * @param expr    the expression with normalized identifiers
     * @param nameMap mapping from normalized names to display names
     */
    private static String denormalizeNamesInExpr(String expr, Map<String, String> nameMap) {
        StringBuilder result = new StringBuilder();
        int i = 0;
        while (i < expr.length()) {
            char c = expr.charAt(i);
            if (c == '"') {
                // Quoted name — read until closing quote and denormalize as a single unit
                int closeQuote = expr.indexOf('"', i + 1);
                if (closeQuote < 0) {
                    // No closing quote — append remainder verbatim
                    result.append(expr, i, expr.length());
                    break;
                }
                String quotedName = expr.substring(i + 1, closeQuote);
                String denormed = denormalizeName(quotedName);
                result.append('"').append(denormed).append('"');
                i = closeQuote + 1;
            } else if (Character.isLetter(c) || c == '_') {
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
                    // Look up in name map first; fall back to underscore→space replacement
                    String displayName = nameMap.get(token);
                    if (displayName != null) {
                        result.append(denormalizeName(displayName));
                    } else {
                        // Fallback for identifiers not in the model (e.g. test expressions)
                        String denormed = token.replace('_', ' ');
                        if (token.length() >= 2 && token.charAt(0) == '_'
                                && Character.isDigit(token.charAt(1))) {
                            denormed = denormed.stripLeading();
                        }
                        result.append(denormed);
                    }
                }
            } else {
                result.append(c);
                i++;
            }
        }
        return result.toString();
    }

    /**
     * Builds a mapping from normalized (equation-form) names to display names
     * for all elements in the model. This allows expression denormalization to
     * preserve the original underscore/space distinction.
     */
    private static Map<String, String> buildNameMap(ModelDefinition def) {
        Map<String, String> map = new HashMap<>();
        for (StockDef s : def.stocks()) {
            putDisplayName(map, s.name());
        }
        for (FlowDef f : def.flows()) {
            putDisplayName(map, f.name());
        }
        for (VariableDef v : def.variables()) {
            putDisplayName(map, v.name());
        }
        for (LookupTableDef l : def.lookupTables()) {
            putDisplayName(map, l.name());
        }
        for (CldVariableDef c : def.cldVariables()) {
            putDisplayName(map, c.name());
        }
        for (SubscriptDef s : def.subscripts()) {
            putDisplayName(map, s.name());
            for (String label : s.labels()) {
                putDisplayName(map, label);
            }
        }
        return map;
    }

    private static void putDisplayName(Map<String, String> map, String displayName) {
        if (displayName == null || displayName.isBlank()) {
            return;
        }
        // Convert display name to equation-form (spaces → underscores)
        String normalized = displayName.strip().replace(' ', '_');
        map.put(normalized, displayName);
    }

    /**
     * Checks if a name is a system variable (e.g. TIME_STEP, INITIAL_TIME)
     * that is already emitted in the control section.
     */
    private static boolean isSystemVar(String name) {
        if (name == null) {
            return false;
        }
        return SYSTEM_VAR_NAMES.contains(
                name.strip().toUpperCase(Locale.ROOT).replace(' ', '_'));
    }

    private static boolean isKnownFunction(String token) {
        String upper = token.toUpperCase(Locale.ROOT);
        return switch (upper) {
            case "IF", "THEN", "ELSE", "INTEG", "SMOOTH", "DELAY3", "DELAY3I", "MIN", "MAX",
                 "ABS", "EXP", "LN", "LOG", "SQRT", "SIN", "COS", "TAN",
                 "INT", "ROUND", "SUM", "MEAN",
                 "LOOKUP", "WITH", "XIDZ", "ZIDZ", "PULSE", "STEP",
                 "MODULO", "POWER", "QUANTUM",
                 "SMOOTH3", "SMOOTHI", "SMOOTH3I", "DELAY1", "DELAY1I", "RAMP",
                 "DELAY_FIXED", "DELAY", "FIXED",
                 "TREND", "FORECAST", "NPV",
                 "RANDOM_UNIFORM", "RANDOM_NORMAL", "RANDOM", "UNIFORM",
                 "PULSE_TRAIN", "TRAIN",
                 "SAMPLE_IF_TRUE", "SAMPLE", "TRUE",
                 "FIND_ZERO", "FIND", "ZERO",
                 "LOOKUP_AREA", "AREA",
                 "AND", "OR", "NOT", "TIME", "DT" -> true;
            default -> false;
        };
    }

    private static String formatDouble(double value) {
        return FormatUtils.formatDouble(value);
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

    private static String formatLookupData(LookupTableDef lookup) {
        double[] xVals = lookup.xValues();
        double[] yVals = lookup.yValues();

        if (xVals.length == 0) {
            return "[(0,0)-(0,0)]";
        }

        double ymin = Double.MAX_VALUE;
        double ymax = -Double.MAX_VALUE;
        for (double y : yVals) {
            if (!Double.isNaN(y)) {
                ymin = Math.min(ymin, y);
                ymax = Math.max(ymax, y);
            }
        }
        if (ymin == Double.MAX_VALUE) {
            ymin = 0;
            ymax = 0;
        }

        String range = "[(" + formatDouble(xVals[0]) + "," + formatDouble(ymin) + ")-("
                + formatDouble(xVals[xVals.length - 1]) + "," + formatDouble(ymax) + ")]";

        StringJoiner pairs = new StringJoiner(",");
        for (int i = 0; i < xVals.length; i++) {
            pairs.add("(" + formatDouble(xVals[i]) + "," + formatDouble(yVals[i]) + ")");
        }

        return range + "," + pairs;
    }

    /**
     * Replaces LOOKUP(name, input) calls in a complex expression with table-call
     * syntax: {@code name(input)}. The standalone lookup table must still be emitted
     * separately (unlike WITH LOOKUP which embeds the data inline).
     */
    private static String inlineLookupCalls(String equation, ModelDefinition def,
                                             Set<String> embeddedLookupNames) {
        if (equation == null) {
            return equation;
        }
        String expr = equation;
        Matcher m = EMBEDDED_LOOKUP_PATTERN.matcher(expr);
        while (m.find()) {
            int funcStart = m.start();
            int openParen = m.end() - 1;
            int closeParen = FormatUtils.findMatchingCloseParen(expr, openParen);
            if (closeParen < 0) {
                break;
            }
            String argsContent = expr.substring(openParen + 1, closeParen);
            int comma = FormatUtils.findTopLevelComma(argsContent, 0);
            if (comma < 0) {
                break;
            }
            String lookupName = argsContent.substring(0, comma).strip();
            String input = argsContent.substring(comma + 1).strip();
            // Only inline if the lookup table exists in the model
            Optional<LookupTableDef> lookupOpt = ExportUtils.findLookup(def, lookupName);
            if (lookupOpt.isPresent()) {
                // Do NOT add to embeddedLookupNames — standalone table must still be emitted
                String replacement = lookupName + "(" + input + ")";
                expr = expr.substring(0, funcStart) + replacement + expr.substring(closeParen + 1);
                m = EMBEDDED_LOOKUP_PATTERN.matcher(expr);
            } else {
                break;
            }
        }
        return expr;
    }

}
