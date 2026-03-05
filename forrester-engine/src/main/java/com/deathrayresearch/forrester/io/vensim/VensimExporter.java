package com.deathrayresearch.forrester.io.vensim;

import com.deathrayresearch.forrester.model.def.AuxDef;
import com.deathrayresearch.forrester.model.def.ConstantDef;
import com.deathrayresearch.forrester.model.def.FlowDef;
import com.deathrayresearch.forrester.model.def.LookupTableDef;
import com.deathrayresearch.forrester.model.def.ModelDefinition;
import com.deathrayresearch.forrester.model.def.SimulationSettings;
import com.deathrayresearch.forrester.model.def.StockDef;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
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
    private static final Pattern AND_OP_PATTERN = Pattern.compile("&&");
    private static final Pattern OR_OP_PATTERN = Pattern.compile("\\|\\|");
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

        // Write stocks
        for (StockDef stock : def.stocks()) {
            sb.append(buildStockBlock(stock, def));
        }

        // Write flows
        for (FlowDef flow : def.flows()) {
            sb.append(buildFlowBlock(flow));
        }

        // Write auxiliaries
        for (AuxDef aux : def.auxiliaries()) {
            sb.append(buildAuxBlock(aux, def, embeddedLookupNames));
        }

        // Write constants
        for (ConstantDef constant : def.constants()) {
            sb.append(buildConstantBlock(constant));
        }

        // Write standalone lookup tables
        for (LookupTableDef lookup : def.lookupTables()) {
            if (!embeddedLookupNames.contains(lookup.name())) {
                sb.append(buildLookupBlock(lookup));
            }
        }

        // Write control section
        sb.append(buildControlSection(def));

        // Sketch terminator
        sb.append("\\---/// Sketch\n");

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

    private static String buildStockBlock(StockDef stock, ModelDefinition def) {
        String vensimName = denormalizeName(stock.name());

        // Find inflows and outflows from flow definitions
        List<String> inflows = new ArrayList<>();
        List<String> outflows = new ArrayList<>();
        for (FlowDef flow : def.flows()) {
            if (stock.name().equals(flow.sink())) {
                inflows.add(denormalizeName(flow.name()));
            }
            if (stock.name().equals(flow.source())) {
                outflows.add(denormalizeName(flow.name()));
            }
        }

        // Build rate expression
        String rateExpr;
        if (inflows.isEmpty() && outflows.isEmpty()) {
            rateExpr = "0";
        } else {
            StringJoiner joiner = new StringJoiner("");
            for (int i = 0; i < inflows.size(); i++) {
                if (i > 0) {
                    joiner.add(" + ");
                }
                joiner.add(inflows.get(i));
            }
            for (int i = 0; i < outflows.size(); i++) {
                if (inflows.isEmpty() && i == 0) {
                    joiner.add("-" + outflows.get(i));
                } else {
                    joiner.add(" - " + outflows.get(i));
                }
            }
            rateExpr = joiner.toString();
        }

        String equation = "INTEG (\n\t" + rateExpr + ",\n\t\t"
                + formatDouble(stock.initialValue()) + ")";
        String units = stock.unit() != null ? stock.unit() : "";
        String comment = stock.comment() != null ? stock.comment() : "";
        return buildBlock(vensimName, "=", equation, units, comment);
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
        String lookupName = extractLookupReference(aux.equation());
        if (lookupName != null) {
            LookupTableDef lookup = findLookup(def, lookupName);
            if (lookup != null) {
                String inputExpr = extractLookupInput(aux.equation());
                if (inputExpr != null) {
                    String vensimInput = toVensimExpr(inputExpr);
                    String lookupData = formatLookupData(lookup);
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

    private static String buildConstantBlock(ConstantDef constant) {
        String vensimName = denormalizeName(constant.name());
        String equation = formatDouble(constant.value());
        String units = constant.unit() != null ? constant.unit() : "";
        String comment = constant.comment() != null ? constant.comment() : "";
        return buildBlock(vensimName, "=", equation, units, comment);
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
        sb.append(buildBlock("INITIAL TIME", "=", "0", durationUnit, "The initial time for the simulation."));

        // FINAL TIME
        sb.append(buildBlock("FINAL TIME", "=", formatDouble(duration), durationUnit, "The final time for the simulation."));

        // TIME STEP
        sb.append(buildBlock("TIME STEP", "=", "1", durationUnit, "The time step for the simulation."));

        // SAVEPER
        sb.append(buildBlock("SAVEPER", "=", "\n        TIME STEP", durationUnit, "The frequency with which output is stored."));

        return sb.toString();
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
     * Translates a Forrester expression to Vensim syntax.
     */
    static String toVensimExpr(String forresterExpr) {
        if (forresterExpr == null || forresterExpr.isBlank()) {
            return forresterExpr;
        }

        String expr = forresterExpr.strip();

        // IF(...) → IF THEN ELSE(...)
        expr = IF_FUNC_PATTERN.matcher(expr).replaceAll("IF THEN ELSE(");

        // && → :AND:
        expr = AND_OP_PATTERN.matcher(expr).replaceAll(":AND:");

        // || → :OR:
        expr = OR_OP_PATTERN.matcher(expr).replaceAll(":OR:");

        // !( → :NOT:(  — translate standalone ! (not part of !=) to :NOT:
        expr = translateNotOperator(expr);

        // != → <>  (must be done after ! translation)
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
     * Translates the Forrester ! operator to Vensim :NOT: operator.
     * Handles standalone ! (not part of !=).
     */
    private static String translateNotOperator(String expr) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < expr.length(); i++) {
            if (expr.charAt(i) == '!' && (i + 1 >= expr.length() || expr.charAt(i + 1) != '=')) {
                sb.append(":NOT:");
            } else {
                sb.append(expr.charAt(i));
            }
        }
        return sb.toString();
    }

    /**
     * Denormalizes a Forrester identifier back to Vensim name format.
     * Replaces underscores with spaces.
     */
    static String denormalizeName(String forresterName) {
        if (forresterName == null || forresterName.isBlank()) {
            return "";
        }
        return forresterName.strip().replace('_', ' ');
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
                    result.append(token.replace('_', ' '));
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
                 "LOOKUP", "WITH", "XIDZ", "ZIDZ", "PULSE", "STEP",
                 "MODULO", "POWER", "QUANTUM", "INTEGER",
                 "SMOOTH3", "SMOOTHI", "SMOOTH3I", "DELAY1", "DELAY1I", "RAMP",
                 "AND", "OR", "NOT", "TIME" -> true;
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

    private static Set<String> collectEmbeddedLookupNames(ModelDefinition def) {
        Set<String> names = new HashSet<>();
        for (AuxDef aux : def.auxiliaries()) {
            String lookupName = extractLookupReference(aux.equation());
            if (lookupName != null) {
                names.add(lookupName);
            }
        }
        return names;
    }

    /**
     * Extracts the lookup table name from a LOOKUP(name, input) expression.
     */
    static String extractLookupReference(String equation) {
        if (equation == null) {
            return null;
        }
        Matcher m = LOOKUP_REF_PATTERN.matcher(equation.strip());
        if (!m.find()) {
            return null;
        }
        int openParen = equation.strip().indexOf('(');
        int comma = findTopLevelComma(equation.strip(), openParen + 1);
        if (comma < 0) {
            return null;
        }
        return equation.strip().substring(openParen + 1, comma).strip();
    }

    /**
     * Extracts the input expression from a LOOKUP(name, input) expression.
     */
    static String extractLookupInput(String equation) {
        if (equation == null) {
            return null;
        }
        String trimmed = equation.strip();
        Matcher m = LOOKUP_REF_PATTERN.matcher(trimmed);
        if (!m.find()) {
            return null;
        }
        int openParen = trimmed.indexOf('(');
        int comma = findTopLevelComma(trimmed, openParen + 1);
        if (comma < 0) {
            return null;
        }
        int closeParen = trimmed.lastIndexOf(')');
        if (closeParen <= comma) {
            return null;
        }
        return trimmed.substring(comma + 1, closeParen).strip();
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

    private static LookupTableDef findLookup(ModelDefinition def, String name) {
        for (LookupTableDef lt : def.lookupTables()) {
            if (lt.name().equals(name)) {
                return lt;
            }
        }
        return null;
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
