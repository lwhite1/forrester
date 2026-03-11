package systems.courant.shrewd.io.vensim;

import systems.courant.shrewd.io.ImportResult;
import systems.courant.shrewd.io.ModelImporter;
import systems.courant.shrewd.model.def.AuxDef;
import systems.courant.shrewd.model.def.CausalLinkDef;
import systems.courant.shrewd.model.def.CldVariableDef;
import systems.courant.shrewd.model.def.ConnectorRoute;
import systems.courant.shrewd.model.def.FlowDef;
import systems.courant.shrewd.model.def.LookupTableDef;
import systems.courant.shrewd.model.def.ModelDefinitionBuilder;
import systems.courant.shrewd.model.def.StockDef;
import systems.courant.shrewd.model.def.ViewDef;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Imports Vensim .mdl model files into Shrewd {@link systems.courant.shrewd.model.def.ModelDefinition}.
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
    private static final long MAX_FILE_SIZE = 10 * 1024 * 1024; // 10 MB

    @Override
    public ImportResult importModel(Path path) throws IOException {
        long size = Files.size(path);
        if (size > MAX_FILE_SIZE) {
            throw new IOException("File exceeds maximum allowed size of "
                    + (MAX_FILE_SIZE / (1024 * 1024)) + " MB: " + path);
        }
        String content = Files.readString(path, StandardCharsets.UTF_8);
        Path fileName = path.getFileName();
        String modelName = fileName != null ? fileName.toString() : path.toString();
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
                    + " (Shrewd uses fixed step; value preserved as metadata only)");
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

        // Inject Vensim built-in simulation constants so expressions can reference them
        builder.constant("TIME_STEP", timeStepValue, timeUnit);
        builder.constant("INITIAL_TIME", initialTime, timeUnit);
        builder.constant("FINAL_TIME", finalTime, timeUnit);

        // Pass 2: Classify and build model elements
        Set<String> stockNames = new HashSet<>();
        Set<String> flowNames = new HashSet<>();
        Set<String> lookupNames = new HashSet<>();
        Set<String> cldVariableNames = new HashSet<>();
        Set<String> allNormalizedNames = new HashSet<>();

        // First sub-pass: identify stocks, lookups, and collect constant values
        // so that INTEG initial values like "Initial number of muskrats" can be resolved
        Map<String, Double> constantValues = new HashMap<>();
        constantValues.put("TIME_STEP", timeStepValue);
        constantValues.put("INITIAL_TIME", initialTime);
        constantValues.put("FINAL_TIME", finalTime);
        for (MdlEquation eq : parsed.equations()) {
            String name = eq.name().strip();
            if (name.isEmpty() || isSystemVar(name)) {
                continue;
            }
            String eqName = VensimExprTranslator.normalizeName(name);

            if (eq.operator().equals(":")) {
                // Subscript definition
                continue;
            }
            if (eq.operator().equals("()")) {
                lookupNames.add(eqName);
                continue;
            }
            if (INTEG_PATTERN.matcher(eq.expression()).find()) {
                stockNames.add(eqName);
                continue;
            }
            if (eq.operator().equals(":=")) {
                continue;
            }
            // Collect numeric constants for initial value resolution
            if (isNumericLiteral(eq.expression())) {
                constantValues.put(eqName, Double.parseDouble(eq.expression().strip()));
                // Also map the original multi-word name (normalized with underscores)
                String altNormalized = name.replace(" ", "_");
                if (!altNormalized.equals(eqName)) {
                    constantValues.put(altNormalized, Double.parseDouble(eq.expression().strip()));
                }
            }
        }

        // Detect CLD mode: no stocks AND no flow valves in sketch section.
        // A pure CLD has only type-10 elements and type-1 connectors, no type-11 flow valves.
        boolean hasFlowValves = parsed.sketchLines().stream()
                .anyMatch(line -> line.strip().startsWith("11,"));
        boolean isCld = stockNames.isEmpty() && !hasFlowValves
                && !parsed.sketchLines().isEmpty();

        // Second sub-pass: build definitions
        Set<String> sketchFlowNames = new HashSet<>();
        for (MdlEquation eq : parsed.equations()) {
            String name = eq.name().strip();
            if (name.isEmpty() || isSystemVar(name)) {
                continue;
            }
            String eqName = VensimExprTranslator.normalizeName(name);
            String displayName = VensimExprTranslator.normalizeDisplayName(name);
            if (!allNormalizedNames.add(eqName)) {
                warnings.add("Duplicate normalized name '" + eqName
                        + "' (from '" + name + "')");
            }
            String comment = eq.comment().isBlank() ? name : eq.comment();

            if (isCld) {
                try {
                    classifyAndBuildCld(eq, displayName, eqName, comment, builder,
                            cldVariableNames, warnings);
                } catch (IllegalArgumentException e) {
                    warnings.add("Error processing '" + name + "': " + e.getMessage());
                }
            } else {
                String unit = cleanUnits(eq.units());
                try {
                    classifyAndBuild(eq, displayName, eqName, unit, comment, builder,
                            vensimNames, stockNames, flowNames, lookupNames,
                            sketchFlowNames, constantValues, timeUnit, warnings);
                } catch (IllegalArgumentException e) {
                    warnings.add("Error processing '" + name + "': " + e.getMessage());
                }
            }
        }

        // Parse sketch section (use original sketch flow names, not auto-generated _net_flow names)
        if (!parsed.sketchLines().isEmpty()) {
            List<ViewDef> views = SketchParser.parse(
                    parsed.sketchLines(), stockNames, sketchFlowNames, lookupNames,
                    cldVariableNames);
            for (ViewDef view : views) {
                builder.view(view);
                // In CLD mode, convert sketch connectors to causal links
                if (isCld) {
                    for (ConnectorRoute connector : view.connectors()) {
                        builder.causalLink(new CausalLinkDef(
                                connector.from(), connector.to(),
                                CausalLinkDef.Polarity.UNKNOWN));
                    }
                }
            }
        }

        return new ImportResult(builder.build(), warnings);
    }

    private void classifyAndBuild(MdlEquation eq, String displayName, String eqName,
                                   String unit, String comment,
                                   ModelDefinitionBuilder builder,
                                   Set<String> vensimNames, Set<String> stockNames,
                                   Set<String> flowNames, Set<String> lookupNames,
                                   Set<String> sketchFlowNames,
                                   Map<String, Double> constantValues,
                                   String timeUnit, List<String> warnings) {
        String operator = eq.operator();
        String expression = eq.expression();

        // Subscript definition (operator ":")
        if (operator.equals(":")) {
            List<String> labels = Arrays.stream(expression.split(","))
                    .map(String::strip)
                    .filter(s -> !s.isEmpty())
                    .map(VensimExprTranslator::normalizeDisplayName)
                    .toList();
            if (!labels.isEmpty()) {
                builder.subscript(displayName, labels);
            }
            return;
        }

        // Standalone lookup table (operator "()")
        if (operator.equals("()")) {
            buildLookupTable(displayName, eqName, expression, unit, comment, builder,
                    lookupNames, warnings);
            return;
        }

        // Data variable (operator ":=")
        if (operator.equals(":=")) {
            warnings.add("Data variable '" + eq.name() + "' skipped (not supported)");
            return;
        }

        // Stock (INTEG function)
        if (INTEG_PATTERN.matcher(expression).find()) {
            buildStock(eq, displayName, eqName, expression, unit, comment, builder,
                    vensimNames, flowNames, lookupNames, sketchFlowNames,
                    constantValues, timeUnit, warnings);
            return;
        }

        // Unchangeable constant (operator "==")
        if (operator.equals("==")) {
            if (isNumericLiteral(expression)) {
                builder.aux(new AuxDef(displayName, comment,
                        AuxDef.formatValue(Double.parseDouble(expression.strip())), unit));
            } else {
                // Non-numeric unchangeable — treat as auxiliary
                VensimExprTranslator.TranslationResult tr =
                        VensimExprTranslator.translate(expression, eqName, vensimNames, lookupNames);
                addExtractedLookups(tr, builder, lookupNames, warnings);
                builder.aux(new AuxDef(displayName, comment, tr.expression(), unit));
                warnings.addAll(tr.warnings());
            }
            return;
        }

        // Numeric literal → constant (literal-valued auxiliary)
        if (isNumericLiteral(expression)) {
            builder.aux(new AuxDef(displayName, comment,
                    AuxDef.formatValue(Double.parseDouble(expression.strip())), unit));
            return;
        }

        // Check if expression contains WITH LOOKUP
        if (expression.toUpperCase(Locale.ROOT).contains("WITH LOOKUP")) {
            VensimExprTranslator.TranslationResult tr =
                    VensimExprTranslator.translate(expression, eqName, vensimNames, lookupNames);
            addExtractedLookups(tr, builder, lookupNames, warnings);
            builder.aux(new AuxDef(displayName, comment, tr.expression(), unit));
            warnings.addAll(tr.warnings());
            return;
        }

        // Default: auxiliary variable
        VensimExprTranslator.TranslationResult tr =
                VensimExprTranslator.translate(expression, eqName, vensimNames, lookupNames);
        addExtractedLookups(tr, builder, lookupNames, warnings);
        builder.aux(new AuxDef(displayName, comment, tr.expression(), unit));
        warnings.addAll(tr.warnings());
    }

    private void classifyAndBuildCld(MdlEquation eq, String displayName, String eqName,
                                      String comment, ModelDefinitionBuilder builder,
                                      Set<String> cldVariableNames,
                                      List<String> warnings) {
        String operator = eq.operator();

        // Skip subscript definitions and data variables in CLD mode
        if (operator.equals(":")) {
            return;
        }
        if (operator.equals(":=")) {
            warnings.add("Data variable '" + eq.name() + "' skipped (not supported)");
            return;
        }
        if (operator.equals("()")) {
            // Lookup tables don't map to CLD variables
            warnings.add("Lookup table '" + eq.name() + "' skipped in CLD mode");
            return;
        }

        builder.cldVariable(new CldVariableDef(displayName, comment));
        cldVariableNames.add(eqName);
    }

    private void buildStock(MdlEquation eq, String displayName, String eqName,
                             String expression, String unit, String comment,
                             ModelDefinitionBuilder builder,
                             Set<String> vensimNames, Set<String> flowNames,
                             Set<String> lookupNames, Set<String> sketchFlowNames,
                             Map<String, Double> constantValues,
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
        InitialValueResult initResult = parseInitialValue(
                initExpr, constantValues, vensimNames, lookupNames, warnings, eq.name());

        // Create stock (display name preserves spaces)
        if (initResult.expression != null) {
            builder.stock(new StockDef(displayName, comment, 0.0, initResult.expression,
                    unit, null, null));
        } else {
            builder.stock(new StockDef(displayName, comment, initResult.value, unit, null));
        }

        // Create flow for the net rate (synthetic name uses display form)
        String flowDisplayName = displayName + " net flow";
        String flowEqName = eqName + "_net_flow";
        VensimExprTranslator.TranslationResult tr =
                VensimExprTranslator.translate(rateExpr, eqName, vensimNames, lookupNames);
        warnings.addAll(tr.warnings());

        builder.flow(new FlowDef(flowDisplayName, "Net flow for " + eq.name(),
                tr.expression(), timeUnit, null, displayName));
        flowNames.add(flowEqName);
        // Track equation-form names for sketch flow valve matching
        sketchFlowNames.add(eqName);
        sketchFlowNames.add(flowEqName);
    }

    private void buildLookupTable(String displayName, String eqName,
                                   String expression, String unit, String comment,
                                   ModelDefinitionBuilder builder,
                                   Set<String> lookupNames, List<String> warnings) {
        if (expression.isBlank()) {
            warnings.add("Empty lookup table data for '" + displayName + "'");
            return;
        }

        java.util.Optional<double[][]> pointsOpt = VensimExprTranslator.parseLookupPoints(expression);
        if (pointsOpt.isEmpty() || pointsOpt.get()[0].length < 2) {
            warnings.add("Could not parse lookup data for '" + displayName + "'");
            return;
        }
        double[][] points = pointsOpt.get();

        points = deduplicateLookupPoints(points, displayName, warnings);
        builder.lookupTable(new LookupTableDef(displayName, comment,
                points[0], points[1], "LINEAR"));
        lookupNames.add(eqName);
    }

    /**
     * Removes consecutive duplicate x-values from lookup table data,
     * keeping the last y-value for each x.
     */
    private static double[][] deduplicateLookupPoints(double[][] points,
                                                       String name,
                                                       List<String> warnings) {
        double[] xs = points[0];
        double[] ys = points[1];
        List<Double> newXs = new ArrayList<>();
        List<Double> newYs = new ArrayList<>();
        newXs.add(xs[0]);
        newYs.add(ys[0]);
        int dupes = 0;
        for (int i = 1; i < xs.length; i++) {
            if (xs[i] == xs[i - 1]) {
                // Replace last y with this one (keep last value for duplicate x)
                newYs.set(newYs.size() - 1, ys[i]);
                dupes++;
            } else {
                newXs.add(xs[i]);
                newYs.add(ys[i]);
            }
        }
        if (dupes > 0) {
            warnings.add("Lookup '" + name + "': removed " + dupes
                    + " duplicate x-value(s)");
            return new double[][]{
                    newXs.stream().mapToDouble(Double::doubleValue).toArray(),
                    newYs.stream().mapToDouble(Double::doubleValue).toArray()
            };
        }
        return points;
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
                double[][] pts = deduplicateLookupPoints(
                        new double[][]{lookup.xValues(), lookup.yValues()},
                        lookup.name(), warnings);
                builder.lookupTable(new LookupTableDef(lookup.name(), null,
                        pts[0], pts[1], "LINEAR"));
                lookupNames.add(lookup.name());
            } catch (IllegalArgumentException e) {
                warnings.add("Could not create lookup '" + lookup.name() + "': " + e.getMessage());
            }
        }
    }

    private record InitialValueResult(double value, String expression) {
        static InitialValueResult ofValue(double v) {
            return new InitialValueResult(v, null);
        }
        static InitialValueResult ofExpression(String expr) {
            return new InitialValueResult(0.0, expr);
        }
    }

    private InitialValueResult parseInitialValue(String initExpr,
                                                  Map<String, Double> constantValues,
                                                  Set<String> vensimNames,
                                                  Set<String> lookupNames,
                                                  List<String> warnings,
                                                  String varName) {
        String trimmed = initExpr.strip();
        if (isNumericLiteral(trimmed)) {
            return InitialValueResult.ofValue(Double.parseDouble(trimmed));
        }
        // Try resolving as a reference to a known constant
        String normalized = VensimExprTranslator.normalizeName(trimmed);
        Double value = constantValues.get(normalized);
        if (value != null) {
            return InitialValueResult.ofValue(value);
        }
        // Fall back to treating as an expression (compiled at model build time)
        VensimExprTranslator.TranslationResult tr =
                VensimExprTranslator.translate(trimmed, varName + "_init", vensimNames, lookupNames);
        warnings.addAll(tr.warnings());
        return InitialValueResult.ofExpression(tr.expression());
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
