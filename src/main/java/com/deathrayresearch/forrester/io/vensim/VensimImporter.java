package com.deathrayresearch.forrester.io.vensim;

import com.deathrayresearch.forrester.io.ImportResult;
import com.deathrayresearch.forrester.io.ModelImporter;
import com.deathrayresearch.forrester.model.def.AuxDef;
import com.deathrayresearch.forrester.model.def.ConstantDef;
import com.deathrayresearch.forrester.model.def.FlowDef;
import com.deathrayresearch.forrester.model.def.LookupTableDef;
import com.deathrayresearch.forrester.model.def.ModelDefinitionBuilder;
import com.deathrayresearch.forrester.model.def.StockDef;
import com.deathrayresearch.forrester.model.def.ViewDef;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Imports Vensim .mdl model files into Forrester {@link com.deathrayresearch.forrester.model.def.ModelDefinition}.
 *
 * <p>Supports stocks (INTEG), constants, auxiliaries, lookup tables (standalone and
 * WITH LOOKUP), subscript ranges, simulation settings, and sketch/view data.
 *
 * <p>Usage:
 * <pre>{@code
 * VensimImporter importer = new VensimImporter();
 * ImportResult result = importer.importModel(Path.of("model.mdl"));
 * if (!result.isClean()) {
 *     result.warnings().forEach(System.out::println);
 * }
 * ModelDefinition def = result.definition();
 * }</pre>
 */
public class VensimImporter implements ModelImporter {

    private static final Pattern INTEG_PATTERN = Pattern.compile(
            "(?i)^INTEG\\s*\\(");
    private static final Pattern NUMERIC_PATTERN = Pattern.compile(
            "^[+-]?(\\d+\\.?\\d*|\\.\\d+)([eE][+-]?\\d+)?$");
    private static final Set<String> SYSTEM_VAR_KEYS = Set.of(
            "INITIAL TIME", "FINAL TIME", "TIME STEP", "SAVEPER");
    private static final Set<String> CONTROL_GROUPS = Set.of(".Control");

    @Override
    public ImportResult importModel(Path path) throws IOException {
        String content = Files.readString(path, StandardCharsets.UTF_8);
        String modelName = path.getFileName().toString();
        int dotPos = modelName.lastIndexOf('.');
        if (dotPos > 0) {
            modelName = modelName.substring(0, dotPos);
        }
        return importModel(content, modelName);
    }

    @Override
    public ImportResult importModel(String content, String modelName) {
        List<String> warnings = new ArrayList<>();
        MdlParser.ParsedMdl parsed = MdlParser.parse(content);

        // Pass 1: Collect all variable names
        Set<String> vensimNames = new HashSet<>();
        Map<String, MdlEquation> controlVars = new LinkedHashMap<>();

        for (MdlEquation eq : parsed.equations()) {
            String name = eq.name().strip();
            if (name.isEmpty()) {
                continue;
            }
            vensimNames.add(name);

            if (isSystemVar(name) || CONTROL_GROUPS.contains(eq.group())) {
                controlVars.put(normalizeSystemVarKey(name), eq);
            }
        }

        // Extract simulation settings from control variables
        double initialTime = getDoubleFromControl(controlVars, "INITIAL TIME", 0.0, warnings);
        double finalTime = getDoubleFromControl(controlVars, "FINAL TIME", 100.0, warnings);
        double timeStepValue = getDoubleFromControl(controlVars, "TIME STEP", 1.0, warnings);
        String timeUnit = inferTimeUnit(controlVars, "Day");

        if (timeStepValue != 1.0) {
            warnings.add("TIME STEP = " + timeStepValue
                    + " (Forrester uses fixed step; value preserved as metadata only)");
        }

        double duration = finalTime - initialTime;
        if (duration <= 0) {
            warnings.add("FINAL TIME (" + finalTime + ") <= INITIAL TIME ("
                    + initialTime + "), defaulting duration to 100");
            duration = 100;
        }

        ModelDefinitionBuilder builder = new ModelDefinitionBuilder()
                .name(modelName)
                .defaultSimulation(timeUnit, duration, timeUnit);

        // Pass 2: Classify and build model elements
        Set<String> stockNames = new HashSet<>();
        Set<String> flowNames = new HashSet<>();
        Set<String> lookupNames = new HashSet<>();
        Set<String> constantNames = new HashSet<>();
        Set<String> allNormalizedNames = new HashSet<>();

        // First sub-pass: identify stocks so we know which flows connect to them
        for (MdlEquation eq : parsed.equations()) {
            String name = eq.name().strip();
            if (name.isEmpty() || isSystemVar(name)) {
                continue;
            }
            String normalized = VensimExprTranslator.normalizeName(name);

            if (eq.operator().equals(":")) {
                // Subscript definition
                continue;
            }
            if (eq.operator().equals("()")) {
                lookupNames.add(normalized);
                continue;
            }
            if (INTEG_PATTERN.matcher(eq.expression()).find()) {
                stockNames.add(normalized);
                continue;
            }
            if (eq.operator().equals(":=")) {
                continue;
            }
            if (eq.operator().equals("==") || isNumericLiteral(eq.expression())) {
                constantNames.add(normalized);
            }
        }

        // Second sub-pass: build definitions
        Set<String> sketchFlowNames = new HashSet<>();
        for (MdlEquation eq : parsed.equations()) {
            String name = eq.name().strip();
            if (name.isEmpty() || isSystemVar(name)) {
                continue;
            }
            String normalized = VensimExprTranslator.normalizeName(name);
            if (!allNormalizedNames.add(normalized)) {
                warnings.add("Duplicate normalized name '" + normalized
                        + "' (from '" + name + "')");
            }
            String unit = cleanUnits(eq.units());
            String comment = eq.comment().isBlank() ? name : eq.comment();

            try {
                classifyAndBuild(eq, normalized, unit, comment, builder,
                        vensimNames, stockNames, flowNames, lookupNames,
                        sketchFlowNames, timeUnit, warnings);
            } catch (IllegalArgumentException e) {
                warnings.add("Error processing '" + name + "': " + e.getMessage());
            }
        }

        // Parse sketch section (use original sketch flow names, not auto-generated _net_flow names)
        if (!parsed.sketchLines().isEmpty()) {
            List<ViewDef> views = SketchParser.parse(
                    parsed.sketchLines(), stockNames, sketchFlowNames, lookupNames);
            for (ViewDef view : views) {
                builder.view(view);
            }
        }

        return new ImportResult(builder.build(), warnings);
    }

    private void classifyAndBuild(MdlEquation eq, String normalized, String unit,
                                   String comment, ModelDefinitionBuilder builder,
                                   Set<String> vensimNames, Set<String> stockNames,
                                   Set<String> flowNames, Set<String> lookupNames,
                                   Set<String> sketchFlowNames,
                                   String timeUnit, List<String> warnings) {
        String operator = eq.operator();
        String expression = eq.expression();

        // Subscript definition (operator ":")
        if (operator.equals(":")) {
            List<String> labels = Arrays.stream(expression.split(","))
                    .map(String::strip)
                    .filter(s -> !s.isEmpty())
                    .map(VensimExprTranslator::normalizeName)
                    .toList();
            if (!labels.isEmpty()) {
                builder.subscript(normalized, labels);
            }
            return;
        }

        // Standalone lookup table (operator "()")
        if (operator.equals("()")) {
            buildLookupTable(normalized, expression, unit, comment, builder, warnings);
            lookupNames.add(normalized);
            return;
        }

        // Data variable (operator ":=")
        if (operator.equals(":=")) {
            warnings.add("Data variable '" + eq.name() + "' skipped (not supported)");
            return;
        }

        // Stock (INTEG function)
        if (INTEG_PATTERN.matcher(expression).find()) {
            buildStock(eq, normalized, expression, unit, comment, builder,
                    vensimNames, flowNames, sketchFlowNames, timeUnit, warnings);
            return;
        }

        // Unchangeable constant (operator "==")
        if (operator.equals("==")) {
            if (isNumericLiteral(expression)) {
                builder.constant(new ConstantDef(normalized, comment,
                        Double.parseDouble(expression.strip()), unit));
            } else {
                // Non-numeric unchangeable — treat as auxiliary
                VensimExprTranslator.TranslationResult tr =
                        VensimExprTranslator.translate(expression, normalized, vensimNames);
                addExtractedLookups(tr, builder, lookupNames, warnings);
                builder.aux(new AuxDef(normalized, comment, tr.expression(), unit));
                warnings.addAll(tr.warnings());
            }
            return;
        }

        // Numeric literal → constant
        if (isNumericLiteral(expression)) {
            builder.constant(new ConstantDef(normalized, comment,
                    Double.parseDouble(expression.strip()), unit));
            return;
        }

        // Check if expression contains WITH LOOKUP
        if (expression.toUpperCase(Locale.ROOT).contains("WITH LOOKUP")) {
            VensimExprTranslator.TranslationResult tr =
                    VensimExprTranslator.translate(expression, normalized, vensimNames);
            addExtractedLookups(tr, builder, lookupNames, warnings);
            builder.aux(new AuxDef(normalized, comment, tr.expression(), unit));
            warnings.addAll(tr.warnings());
            return;
        }

        // Default: auxiliary variable
        VensimExprTranslator.TranslationResult tr =
                VensimExprTranslator.translate(expression, normalized, vensimNames);
        addExtractedLookups(tr, builder, lookupNames, warnings);
        builder.aux(new AuxDef(normalized, comment, tr.expression(), unit));
        warnings.addAll(tr.warnings());
    }

    private void buildStock(MdlEquation eq, String normalized, String expression,
                             String unit, String comment, ModelDefinitionBuilder builder,
                             Set<String> vensimNames, Set<String> flowNames,
                             Set<String> sketchFlowNames,
                             String timeUnit, List<String> warnings) {
        // Parse INTEG(rate_expr, initial_value)
        Matcher m = INTEG_PATTERN.matcher(expression);
        if (!m.find()) {
            return;
        }
        int argsStart = m.end();
        int closeParen = VensimExprTranslator.findMatchingParen(expression, argsStart - 1);
        if (closeParen < 0) {
            warnings.add("Malformed INTEG expression for '" + eq.name() + "'");
            return;
        }

        String argsContent = expression.substring(argsStart, closeParen);
        // Split on top-level comma
        int commaPos = findTopLevelComma(argsContent);
        String rateExpr;
        String initExpr;
        if (commaPos >= 0) {
            rateExpr = argsContent.substring(0, commaPos).strip();
            initExpr = argsContent.substring(commaPos + 1).strip();
        } else {
            rateExpr = argsContent.strip();
            initExpr = "0";
            warnings.add("INTEG for '" + eq.name() + "' missing initial value, defaulting to 0");
        }

        // Parse initial value
        double initialValue = parseInitialValue(initExpr, warnings, eq.name());

        // Create stock
        builder.stock(new StockDef(normalized, comment, initialValue, unit, null));

        // Create flow for the net rate
        String flowName = normalized + "_net_flow";
        VensimExprTranslator.TranslationResult tr =
                VensimExprTranslator.translate(rateExpr, normalized, vensimNames);
        warnings.addAll(tr.warnings());

        builder.flow(new FlowDef(flowName, "Net flow for " + eq.name(),
                tr.expression(), timeUnit, null, normalized));
        flowNames.add(flowName);
        // Track original normalized name for sketch flow valve matching
        sketchFlowNames.add(normalized);
        sketchFlowNames.add(flowName);
    }

    private void buildLookupTable(String normalized, String expression, String unit,
                                   String comment, ModelDefinitionBuilder builder,
                                   List<String> warnings) {
        if (expression.isBlank()) {
            warnings.add("Empty lookup table data for '" + normalized + "'");
            return;
        }

        double[][] points = VensimExprTranslator.parseLookupPoints(expression);
        if (points == null || points[0].length < 2) {
            warnings.add("Could not parse lookup data for '" + normalized + "'");
            return;
        }

        builder.lookupTable(new LookupTableDef(normalized, comment,
                points[0], points[1], "LINEAR"));
    }

    private void addExtractedLookups(VensimExprTranslator.TranslationResult tr,
                                      ModelDefinitionBuilder builder,
                                      Set<String> lookupNames,
                                      List<String> warnings) {
        for (VensimExprTranslator.ExtractedLookup lookup : tr.lookups()) {
            if (lookupNames.contains(lookup.name())) {
                continue;
            }
            try {
                builder.lookupTable(new LookupTableDef(lookup.name(), null,
                        lookup.xValues(), lookup.yValues(), "LINEAR"));
                lookupNames.add(lookup.name());
            } catch (IllegalArgumentException e) {
                warnings.add("Could not create lookup '" + lookup.name() + "': " + e.getMessage());
            }
        }
    }

    private double parseInitialValue(String initExpr, List<String> warnings, String varName) {
        String trimmed = initExpr.strip();
        if (isNumericLiteral(trimmed)) {
            return Double.parseDouble(trimmed);
        }
        warnings.add("Non-literal initial value for '" + varName + "': '" + trimmed
                + "', defaulting to 0.0");
        return 0.0;
    }

    private static boolean isNumericLiteral(String expr) {
        return expr != null && NUMERIC_PATTERN.matcher(expr.strip()).matches();
    }

    private static boolean isSystemVar(String name) {
        return SYSTEM_VAR_KEYS.contains(name.strip().toUpperCase(Locale.ROOT));
    }

    private static String normalizeSystemVarKey(String name) {
        return name.strip().toUpperCase(Locale.ROOT).replace(" ", "_");
    }

    private static double getDoubleFromControl(Map<String, MdlEquation> controlVars,
                                                String varName, double defaultValue,
                                                List<String> warnings) {
        String key = varName.toUpperCase(Locale.ROOT).replace(" ", "_");
        MdlEquation eq = controlVars.get(key);
        if (eq == null) {
            return defaultValue;
        }
        String expr = eq.expression().strip();
        if (isNumericLiteral(expr)) {
            return Double.parseDouble(expr);
        }
        warnings.add("Non-numeric " + varName + " value: '" + expr
                + "', defaulting to " + defaultValue);
        return defaultValue;
    }

    private static String inferTimeUnit(Map<String, MdlEquation> controlVars,
                                         String defaultUnit) {
        MdlEquation timeStep = controlVars.get("TIME_STEP");
        if (timeStep != null) {
            String units = cleanUnits(timeStep.units());
            if (!units.isEmpty()) {
                return capitalizeFirst(units);
            }
        }
        return defaultUnit;
    }

    private static String cleanUnits(String units) {
        if (units == null || units.isBlank()) {
            return "";
        }
        String cleaned = units.strip();
        // Remove range annotation like [0,100]
        int bracketPos = cleaned.indexOf('[');
        if (bracketPos >= 0) {
            cleaned = cleaned.substring(0, bracketPos).strip();
        }
        return cleaned;
    }

    private static String capitalizeFirst(String s) {
        if (s.isEmpty()) {
            return s;
        }
        return s.substring(0, 1).toUpperCase(Locale.ROOT) + s.substring(1).toLowerCase(Locale.ROOT);
    }

    private static int findTopLevelComma(String content) {
        int depth = 0;
        for (int i = 0; i < content.length(); i++) {
            char c = content.charAt(i);
            if (c == '(') {
                depth++;
            } else if (c == ')') {
                depth--;
            } else if (c == ',' && depth == 0) {
                return i;
            }
        }
        return -1;
    }
}
