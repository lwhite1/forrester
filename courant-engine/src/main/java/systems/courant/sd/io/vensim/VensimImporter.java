package systems.courant.sd.io.vensim;

import systems.courant.sd.io.FormatUtils;
import systems.courant.sd.io.ImportResult;
import systems.courant.sd.io.ModelImporter;
import systems.courant.sd.io.ReferenceDataCsvReader;
import systems.courant.sd.model.def.ReferenceDataset;
import systems.courant.sd.model.def.VariableDef;
import systems.courant.sd.model.def.CausalLinkDef;
import systems.courant.sd.model.def.CldVariableDef;
import systems.courant.sd.model.def.ConnectorRoute;
import systems.courant.sd.model.def.FlowDef;
import systems.courant.sd.model.def.LookupTableDef;
import systems.courant.sd.model.def.ModelDefinitionBuilder;
import systems.courant.sd.model.def.StockDef;
import systems.courant.sd.model.def.ViewDef;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.CharacterCodingException;
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
 * Imports Vensim .mdl model files into Courant {@link systems.courant.sd.model.def.ModelDefinition}.
 *
 * <p>Supports stocks (INTEG), constants, variables, lookup tables (standalone and
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
    private static final Pattern SUBSCRIPT_NAME_PATTERN = Pattern.compile(
            "^(.+?)\\[(.+?)\\]$");
    private static final Set<String> CONTROL_GROUPS = Set.of(".Control");
    private static final long MAX_FILE_SIZE = 10 * 1024 * 1024; // 10 MB

    @Override
    public ImportResult importModel(Path path) throws IOException {
        long size = Files.size(path);
        if (size > MAX_FILE_SIZE) {
            throw new IOException("File exceeds maximum allowed size of "
                    + (MAX_FILE_SIZE / (1024 * 1024)) + " MB: " + path);
        }
        String content;
        try {
            content = Files.readString(path, StandardCharsets.UTF_8);
        } catch (CharacterCodingException e) {
            content = Files.readString(path, Charset.forName("windows-1252"));
        }
        Path fileName = path.getFileName();
        String modelName = fileName != null ? fileName.toString() : path.toString();
        int dotPos = modelName.lastIndexOf('.');
        if (dotPos > 0) {
            modelName = modelName.substring(0, dotPos);
        }
        return importModel(content, modelName, path.getParent());
    }

    @Override
    public ImportResult importModel(String content, String modelName) {
        return importModel(content, modelName, null);
    }

    /**
     * Imports a Vensim .mdl model with optional companion file resolution.
     *
     * @param content   the .mdl file content
     * @param modelName the model name
     * @param baseDir   the directory containing the .mdl file, or null if unavailable.
     *                  When provided, GET DIRECT DATA/CONSTANTS references to CSV files
     *                  are resolved relative to this directory.
     * @return the import result
     */
    public ImportResult importModel(String content, String modelName, Path baseDir) {
        List<String> warnings = new ArrayList<>();
        MdlParser.ParsedMdl parsed = MdlParser.parse(content);

        // Expand macro calls before classification
        List<MdlEquation> equations;
        if (!parsed.macros().isEmpty()) {
            MacroExpander.ExpansionResult expansion = MacroExpander.expand(
                    parsed.equations(), parsed.macros());
            equations = expansion.expandedEquations();
            warnings.addAll(expansion.warnings());
        } else {
            equations = parsed.equations();
        }

        // Pass 1: Collect all variable names and subscript dimensions
        Set<String> vensimNames = new HashSet<>();
        Map<String, MdlEquation> controlVars = new LinkedHashMap<>();
        // Map from normalized dimension name → list of normalized labels
        Map<String, List<String>> subscriptDimensions = new LinkedHashMap<>();
        // Map from normalized dimension name → list of original (display) labels
        Map<String, List<String>> subscriptDisplayLabels = new LinkedHashMap<>();
        // Map from normalized mapping dim name → mapping info (for -> syntax)
        Map<String, SubscriptMapping> subscriptMappings = new LinkedHashMap<>();
        // Deferred <-> equivalences (resolved after all : definitions)
        Map<String, String> equivalences = new LinkedHashMap<>();
        Map<String, String> equivalenceDisplayNames = new LinkedHashMap<>();

        for (MdlEquation eq : equations) {
            String name = eq.name().strip();
            if (name.isEmpty()) {
                continue;
            }
            vensimNames.add(name);
            // Also add the unquoted form so replaceMultiWordNames can match
            // references that appear without quotes in other equations
            if (name.startsWith("\"") && name.endsWith("\"") && name.length() > 2) {
                vensimNames.add(name.substring(1, name.length() - 1));
            }
            // For subscripted names like "growth rate[Region]", also register
            // the base name "growth rate" so multi-word replacement works
            // after dimension substitution (e.g., "growth rate[North]")
            Matcher baseMatcher = SUBSCRIPT_NAME_PATTERN.matcher(name);
            if (baseMatcher.matches()) {
                vensimNames.add(baseMatcher.group(1).strip());
            }

            if (isSystemVar(name) || CONTROL_GROUPS.contains(eq.group())) {
                controlVars.put(normalizeSystemVarKey(name), eq);
            }

            // Collect subscript dimension definitions
            if (eq.operator().equals(":")) {
                String dimName = VensimExprTranslator.normalizeName(name);
                String rawExpr = eq.expression().strip();

                // Check for dimension mapping: labels -> targetDim
                int arrowPos = rawExpr.indexOf("->");
                String labelsStr = arrowPos >= 0
                        ? rawExpr.substring(0, arrowPos).strip() : rawExpr;

                List<String> normalizedLabels = Arrays.stream(labelsStr.split(","))
                        .map(String::strip)
                        .filter(s -> !s.isEmpty())
                        .map(VensimExprTranslator::normalizeName)
                        .toList();
                List<String> displayLabels = Arrays.stream(labelsStr.split(","))
                        .map(String::strip)
                        .filter(s -> !s.isEmpty())
                        .map(VensimExprTranslator::normalizeDisplayName)
                        .toList();
                if (!normalizedLabels.isEmpty()) {
                    subscriptDimensions.put(dimName, normalizedLabels);
                    subscriptDisplayLabels.put(dimName, displayLabels);
                }

                // Store mapping if -> is present
                if (arrowPos >= 0) {
                    String targetDim = VensimExprTranslator.normalizeName(
                            rawExpr.substring(arrowPos + 2).strip());
                    List<String> rawLabels = Arrays.stream(labelsStr.split(","))
                            .map(String::strip)
                            .filter(s -> !s.isEmpty())
                            .toList();
                    subscriptMappings.put(dimName, new SubscriptMapping(
                            targetDim, name.strip(), rawLabels));
                }
            }

            // Dimension equivalence: prereqtask <-> task
            if (eq.operator().equals("<->")) {
                String dimName = VensimExprTranslator.normalizeName(name);
                String displayName = VensimExprTranslator.normalizeDisplayName(name);
                String targetDim = VensimExprTranslator.normalizeName(
                        eq.expression().strip());
                equivalences.put(dimName, targetDim);
                equivalenceDisplayNames.put(dimName, displayName);
            }
        }

        // Resolve <-> equivalences after all dimension definitions.
        // Iterate until no new resolutions are made to handle transitive chains
        // (e.g. A <-> B, B <-> C: first pass resolves B, second pass resolves A via B).
        boolean changed = true;
        while (changed) {
            changed = false;
            for (var entry : equivalences.entrySet()) {
                String dimName = entry.getKey();
                String targetDim = entry.getValue();
                List<String> targetLabels = subscriptDimensions.get(targetDim);
                List<String> targetDisplayLabels = subscriptDisplayLabels.get(targetDim);
                if (targetLabels != null && !targetLabels.equals(subscriptDimensions.get(dimName))) {
                    subscriptDimensions.put(dimName, targetLabels);
                    subscriptDisplayLabels.put(dimName, targetDisplayLabels);
                    changed = true;
                }
            }
        }

        // Extract simulation settings from control variables
        double initialTime = getDoubleFromControl(controlVars, "INITIAL TIME", 0.0, warnings);
        double finalTime = getDoubleFromControl(controlVars, "FINAL TIME", 100.0, warnings);
        double timeStepValue = getDoubleFromControl(controlVars, "TIME STEP", 1.0, warnings);
        String timeUnit = inferTimeUnit(controlVars, "Day");

        double duration = finalTime - initialTime;
        if (duration <= 0) {
            warnings.add("FINAL TIME (" + finalTime + ") <= INITIAL TIME ("
                    + initialTime + "), defaulting duration to 100");
            duration = 100;
        }

        ModelDefinitionBuilder builder = new ModelDefinitionBuilder()
                .name(modelName)
                .defaultSimulation(timeUnit, duration, timeUnit, timeStepValue);

        // Register resolved equivalence dimensions as subscripts
        for (var entry : equivalences.entrySet()) {
            String dimName = entry.getKey();
            List<String> labels = subscriptDisplayLabels.get(dimName);
            String displayName = equivalenceDisplayNames.get(dimName);
            if (labels != null && displayName != null) {
                builder.subscript(displayName, labels);
            }
        }

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
        for (MdlEquation eq : equations) {
            String name = eq.name().strip();
            if (name.isEmpty() || isSystemVar(name)) {
                continue;
            }
            if (isDocumentationBlock(eq.expression())) {
                continue;
            }

            // Check for subscripted variable names and register expanded names
            Matcher subMatcher = SUBSCRIPT_NAME_PATTERN.matcher(name);
            if (subMatcher.matches()) {
                String baseName = subMatcher.group(1).strip();
                String dimNameRaw = subMatcher.group(2).strip();
                String dimKey = VensimExprTranslator.normalizeName(dimNameRaw);
                List<String> labels = subscriptDimensions.get(dimKey);
                if (labels != null) {
                    String normalizedBase = VensimExprTranslator.normalizeName(baseName);
                    boolean isInteg = INTEG_PATTERN.matcher(eq.expression()).find();
                    // Try splitting comma-separated constant values for expanded names
                    List<String> perLabelValues = splitSubscriptValues(
                            eq.expression(), labels.size());
                    for (int li = 0; li < labels.size(); li++) {
                        String expandedName = normalizedBase + "_" + labels.get(li);
                        if (isInteg) {
                            stockNames.add(expandedName);
                        }
                        // Register per-label numeric constants under both key forms:
                        // base_label (expanded name) and normalizeName(base[label]) (what
                        // parseInitialValue will look up)
                        if (perLabelValues != null) {
                            String val = perLabelValues.get(li).strip();
                            if (isNumericLiteral(val)) {
                                double numVal = Double.parseDouble(val);
                                constantValues.put(expandedName, numVal);
                                // Also register under the key normalizeName produces
                                // from "base[label]" (strips brackets without separator)
                                String altKey = VensimExprTranslator.normalizeName(
                                        baseName + "[" + labels.get(li) + "]");
                                constantValues.put(altKey, numVal);
                            }
                        }
                    }
                    continue;
                }

                // Check for multi-dimensional subscripts: name[sub1,sub2]
                if (dimNameRaw.contains(",")) {
                    String normalizedBase = VensimExprTranslator.normalizeName(baseName);
                    boolean isInteg = INTEG_PATTERN.matcher(eq.expression()).find();
                    List<List<String>> combos = resolveMultiDimLabels(
                            dimNameRaw, subscriptDimensions);
                    List<String> perLabelValues = splitSubscriptValues(
                            eq.expression(), combos.size());
                    for (int ci = 0; ci < combos.size(); ci++) {
                        String expandedName = normalizedBase + "_"
                                + String.join("_", combos.get(ci));
                        if (isInteg) {
                            stockNames.add(expandedName);
                        }
                        if (perLabelValues != null) {
                            String val = perLabelValues.get(ci).strip();
                            if (isNumericLiteral(val)) {
                                constantValues.put(expandedName,
                                        Double.parseDouble(val));
                            }
                        }
                    }
                    continue;
                }

                // Check if subscript is a single label (not a dimension name)
                String normalizedSub = VensimExprTranslator.normalizeName(dimNameRaw);
                boolean isLabel = subscriptDimensions.values().stream()
                        .anyMatch(lbls -> lbls.contains(normalizedSub));
                if (isLabel) {
                    String normalizedBase = VensimExprTranslator.normalizeName(baseName);
                    String expandedName = normalizedBase + "_" + normalizedSub;
                    boolean isInteg = INTEG_PATTERN.matcher(eq.expression()).find();
                    if (isInteg) {
                        stockNames.add(expandedName);
                    }
                    if (isNumericLiteral(eq.expression())) {
                        constantValues.put(expandedName,
                                Double.parseDouble(eq.expression().strip()));
                    }
                    continue;
                }
            }

            String eqName = VensimExprTranslator.normalizeName(name);

            if (eq.operator().equals(":") || eq.operator().equals("<->")) {
                // Subscript definition or equivalence
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

        // Pre-extract flow valve names from sketch section (type-11 lines).
        // These are the original Vensim flow valve names used to match decomposed
        // INTEG rate terms so we preserve the original names instead of synthetic ones.
        Set<String> sketchValveNames = extractSketchFlowValveNames(parsed.sketchLines());

        // Pre-collect variable equations for flow valve name matching.
        // When a decomposed INTEG rate term matches a sketch valve name, the flow
        // gets the variable's actual equation (not just a reference) and the variable
        // is skipped to avoid duplicate names.
        Map<String, MdlEquation> equationsByName = new LinkedHashMap<>();
        for (MdlEquation eq : equations) {
            String name = eq.name().strip();
            if (!name.isEmpty()) {
                String norm = VensimExprTranslator.normalizeName(name);
                equationsByName.put(norm, eq);
            }
        }

        // Second sub-pass: build definitions
        Set<String> sketchFlowNames = new HashSet<>();
        for (MdlEquation eq : equations) {
            String name = eq.name().strip();
            if (name.isEmpty() || isSystemVar(name)) {
                continue;
            }
            // Skip Vensim "A FUNCTION OF" documentation blocks — they're metadata,
            // not executable equations, and would create duplicates of the real equation
            if (isDocumentationBlock(eq.expression())) {
                continue;
            }
            String eqName = VensimExprTranslator.normalizeName(name);
            String displayName = VensimExprTranslator.normalizeDisplayName(name);
            // Subscript range names use a separate namespace — skip duplicate check
            if (!eq.operator().equals(":") && !eq.operator().equals("<->")
                    && !allNormalizedNames.add(eqName)) {
                warnings.add("Duplicate normalized name '" + eqName
                        + "' (from '" + name + "') — skipped");
                continue;
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
                            sketchFlowNames, sketchValveNames, equationsByName,
                            constantValues, timeUnit,
                            subscriptDimensions, subscriptDisplayLabels,
                            subscriptMappings, warnings);
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

        // Phase 2: Attempt to resolve companion CSV files for GET DIRECT DATA references
        if (baseDir != null) {
            resolveCompanionCsvFiles(equations, baseDir, builder, warnings);
        }

        return new ImportResult(builder.build(), warnings);
    }

    /**
     * Scans equations for GET DIRECT DATA / GET DIRECT CONSTANTS references and attempts
     * to load companion CSV files from the model's directory. Successfully loaded data
     * is added as reference datasets to the model definition.
     */
    private void resolveCompanionCsvFiles(List<MdlEquation> equations, Path baseDir,
                                          ModelDefinitionBuilder builder,
                                          List<String> warnings) {
        Set<String> resolvedFiles = new HashSet<>();
        Pattern getDirectPattern = Pattern.compile(
                "(?i)GET\\s+DIRECT\\s+(DATA|CONSTANTS|LOOKUPS)\\s*\\(");

        for (MdlEquation eq : equations) {
            String expression = eq.expression();
            if (expression == null || expression.isBlank()) {
                continue;
            }
            Matcher m = getDirectPattern.matcher(expression);
            while (m.find()) {
                int openParen = m.end() - 1;
                int closeParen = VensimExprTranslator.findMatchingParen(expression, openParen);
                if (closeParen <= 0) {
                    continue;
                }
                String argsStr = expression.substring(openParen + 1, closeParen);
                String filePath = VensimExprTranslator.extractFirstArgument(argsStr);
                if (filePath == null || filePath.isBlank() || !resolvedFiles.add(filePath)) {
                    continue;
                }
                // Only attempt CSV files
                if (!filePath.toLowerCase(Locale.ROOT).endsWith(".csv")) {
                    continue;
                }
                Path csvPath = baseDir.resolve(filePath).normalize();
                if (!csvPath.startsWith(baseDir.normalize())) {
                    warnings.add("Rejected companion CSV path '" + filePath
                            + "': resolves outside model directory");
                    continue;
                }
                if (!Files.isRegularFile(csvPath)) {
                    continue;
                }
                try {
                    ReferenceDataset dataset = ReferenceDataCsvReader.read(csvPath, filePath);
                    builder.referenceDataset(dataset);
                    warnings.add("Loaded companion CSV '" + filePath
                            + "' as reference dataset (" + dataset.size()
                            + " rows, " + dataset.variableNames().size() + " variables)");
                } catch (IOException e) {
                    warnings.add("Failed to read companion CSV '" + filePath
                            + "': " + e.getMessage());
                }
            }
        }
    }

    private void classifyAndBuild(MdlEquation eq, String displayName, String eqName,
                                   String unit, String comment,
                                   ModelDefinitionBuilder builder,
                                   Set<String> vensimNames, Set<String> stockNames,
                                   Set<String> flowNames, Set<String> lookupNames,
                                   Set<String> sketchFlowNames,
                                   Set<String> sketchValveNames,
                                   Map<String, MdlEquation> equationsByName,
                                   Map<String, Double> constantValues,
                                   String timeUnit,
                                   Map<String, List<String>> subscriptDimensions,
                                   Map<String, List<String>> subscriptDisplayLabels,
                                   Map<String, SubscriptMapping> subscriptMappings,
                                   List<String> warnings) {
        String operator = eq.operator();
        String expression = eq.expression();

        // Check if variable name contains subscript notation: name[Dimension]
        Matcher subscriptMatcher = SUBSCRIPT_NAME_PATTERN.matcher(eq.name().strip());
        if (subscriptMatcher.matches()) {
            String baseName = subscriptMatcher.group(1).strip();
            String dimNameRaw = subscriptMatcher.group(2).strip();
            String dimName = VensimExprTranslator.normalizeName(dimNameRaw);
            List<String> normalizedLabels = subscriptDimensions.get(dimName);
            List<String> displayLabels = subscriptDisplayLabels.get(dimName);
            if (normalizedLabels != null) {
                expandSubscriptedVariable(eq, baseName, dimName, dimNameRaw,
                        normalizedLabels, displayLabels, unit, builder,
                        vensimNames, stockNames, flowNames, lookupNames,
                        sketchFlowNames, sketchValveNames, equationsByName,
                        constantValues, timeUnit,
                        subscriptDimensions, subscriptDisplayLabels,
                        subscriptMappings, warnings);
                return;
            }

            // Check for multi-dimensional subscripts: name[sub1,sub2]
            if (dimNameRaw.contains(",")) {
                expandMultiDimSubscriptedVariable(eq, baseName, dimNameRaw,
                        unit, builder, vensimNames, stockNames, flowNames,
                        lookupNames, sketchFlowNames, sketchValveNames,
                        equationsByName, constantValues, timeUnit,
                        subscriptDimensions, subscriptDisplayLabels,
                        subscriptMappings, warnings);
                return;
            }

            // Check if subscript is a single label (not a dimension name)
            String normalizedSub = VensimExprTranslator.normalizeName(dimNameRaw);
            boolean isLabel = subscriptDimensions.values().stream()
                    .anyMatch(lbls -> lbls.contains(normalizedSub));
            if (isLabel) {
                String normalizedBase = VensimExprTranslator.normalizeName(baseName);
                String expandedEqName = normalizedBase + "_" + normalizedSub;
                String expandedDisplayName =
                        VensimExprTranslator.normalizeDisplayName(baseName) + " "
                        + VensimExprTranslator.normalizeDisplayName(dimNameRaw);
                String lComment = eq.comment().isBlank() ? eq.name().strip() : eq.comment();
                MdlEquation labelEq = new MdlEquation(
                        expandedDisplayName, operator, expression,
                        eq.units(), lComment, eq.group());
                classifyAndBuild(labelEq, expandedDisplayName, expandedEqName,
                        unit, lComment, builder, vensimNames, stockNames, flowNames,
                        lookupNames, sketchFlowNames, sketchValveNames, equationsByName,
                        constantValues, timeUnit, subscriptDimensions,
                        subscriptDisplayLabels, subscriptMappings, warnings);
                return;
            }
            // If nothing matches, fall through — the brackets will be normalized away
        }

        // Subscript definition (operator ":") or equivalence ("<->")
        if (operator.equals(":") || operator.equals("<->")) {
            if (operator.equals(":")) {
                String rawExpr = expression;
                int arrowPos = rawExpr.indexOf("->");
                String labelsStr = arrowPos >= 0
                        ? rawExpr.substring(0, arrowPos).strip() : rawExpr;
                List<String> labels = Arrays.stream(labelsStr.split(","))
                        .map(String::strip)
                        .filter(s -> !s.isEmpty())
                        .map(VensimExprTranslator::normalizeDisplayName)
                        .toList();
                if (!labels.isEmpty()) {
                    builder.subscript(displayName, labels);
                }
            }
            return;
        }

        // Standalone lookup table (operator "()")
        if (operator.equals("()")) {
            buildLookupTable(displayName, eqName, expression, unit, comment, builder,
                    lookupNames, warnings);
            return;
        }

        // Data variable (operator ":=") — create placeholder constant so references resolve
        if (operator.equals(":=")) {
            builder.variable(new VariableDef(displayName, comment, "0", unit));
            warnings.add("Data variable '" + eq.name()
                    + "' imported as constant 0 (external data source not supported)");
            return;
        }

        // Stock (INTEG function)
        if (INTEG_PATTERN.matcher(expression).find()) {
            buildStock(eq, displayName, eqName, expression, unit, comment, builder,
                    vensimNames, flowNames, lookupNames, sketchFlowNames,
                    sketchValveNames, equationsByName, constantValues, timeUnit,
                    subscriptDimensions, warnings);
            return;
        }

        // Unchangeable constant (operator "==")
        if (operator.equals("==")) {
            if (isNumericLiteral(expression)) {
                builder.variable(new VariableDef(displayName, comment,
                        VariableDef.formatValue(Double.parseDouble(expression.strip())), unit));
            } else {
                // Non-numeric unchangeable — treat as auxiliary
                VensimExprTranslator.TranslationResult tr =
                        VensimExprTranslator.translate(expression, eqName, vensimNames,
                                lookupNames, subscriptDimensions);
                addExtractedLookups(tr, builder, lookupNames, warnings);
                builder.variable(new VariableDef(displayName, comment, tr.expression(), unit));
                warnings.addAll(tr.warnings());
            }
            return;
        }

        // Bare variable name with no equation — create placeholder constant
        if (operator.isEmpty() && expression.isEmpty()) {
            builder.variable(new VariableDef(displayName, comment, "0", unit));
            warnings.add("Variable '" + eq.name()
                    + "' has no equation; imported as constant 0");
            return;
        }

        // Skip variable if it was already consumed as a flow during INTEG decomposition
        if (flowNames.contains(eqName)) {
            return;
        }

        // Numeric literal → constant (literal-valued variable)
        if (isNumericLiteral(expression)) {
            builder.variable(new VariableDef(displayName, comment,
                    VariableDef.formatValue(Double.parseDouble(expression.strip())), unit));
            return;
        }

        // Check if expression contains WITH LOOKUP
        if (expression.toUpperCase(Locale.ROOT).contains("WITH LOOKUP")) {
            VensimExprTranslator.TranslationResult tr =
                    VensimExprTranslator.translate(expression, eqName, vensimNames,
                            lookupNames, subscriptDimensions);
            addExtractedLookups(tr, builder, lookupNames, warnings);
            builder.variable(new VariableDef(displayName, comment, tr.expression(), unit));
            warnings.addAll(tr.warnings());
            return;
        }

        // Default: variable
        VensimExprTranslator.TranslationResult tr =
                VensimExprTranslator.translate(expression, eqName, vensimNames,
                        lookupNames, subscriptDimensions);
        addExtractedLookups(tr, builder, lookupNames, warnings);
        builder.variable(new VariableDef(displayName, comment, tr.expression(), unit));
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

    /**
     * Expands a subscripted variable into per-label scalar variables.
     * For comma-separated values, assigns each value to the corresponding label.
     * For formulas, creates copies with the dimension reference replaced by each label.
     */
    private void expandSubscriptedVariable(MdlEquation eq, String baseName,
                                            String dimName, String dimNameRaw,
                                            List<String> normalizedLabels,
                                            List<String> displayLabels,
                                            String unit, ModelDefinitionBuilder builder,
                                            Set<String> vensimNames, Set<String> stockNames,
                                            Set<String> flowNames, Set<String> lookupNames,
                                            Set<String> sketchFlowNames,
                                            Set<String> sketchValveNames,
                                            Map<String, MdlEquation> equationsByName,
                                            Map<String, Double> constantValues,
                                            String timeUnit,
                                            Map<String, List<String>> subscriptDimensions,
                                            Map<String, List<String>> subscriptDisplayLabels,
                                            Map<String, SubscriptMapping> subscriptMappings,
                                            List<String> warnings) {
        String operator = eq.operator();
        String expression = eq.expression();
        String normalizedBase = VensimExprTranslator.normalizeName(baseName);
        String displayBase = VensimExprTranslator.normalizeDisplayName(baseName);

        // Try to split expression into comma-separated per-label values
        List<String> perLabelValues = splitSubscriptValues(expression, normalizedLabels.size());

        for (int i = 0; i < normalizedLabels.size(); i++) {
            String label = normalizedLabels.get(i);
            String displayLabel = displayLabels.get(i);
            String expandedEqName = normalizedBase + "_" + label;
            String expandedDisplayName = displayBase + " " + displayLabel;
            String comment = eq.comment().isBlank() ? eq.name().strip() : eq.comment();

            String labelExpression;
            if (perLabelValues != null) {
                // Comma-separated values: assign per-label
                labelExpression = perLabelValues.get(i).strip();
            } else {
                // Formula: replace dimension references within brackets
                labelExpression = replaceDimInSubscripts(expression, dimNameRaw, label);

                // Apply mapped dimension replacements (e.g., previous cohort → infant)
                for (var entry : subscriptMappings.entrySet()) {
                    SubscriptMapping mapping = entry.getValue();
                    if (mapping.targetDimension().equals(dimName)
                            && i < mapping.rawLabels().size()) {
                        String mappedLabel = mapping.rawLabels().get(i);
                        labelExpression = replaceDimInSubscripts(
                                labelExpression, mapping.rawDimName(), mappedLabel);
                    }
                }
            }

            // Create a synthetic equation for this label and classify normally
            MdlEquation labelEq = new MdlEquation(
                    expandedDisplayName, operator, labelExpression,
                    eq.units(), comment, eq.group());
            classifyAndBuild(labelEq, expandedDisplayName, expandedEqName, unit, comment,
                    builder, vensimNames, stockNames, flowNames, lookupNames,
                    sketchFlowNames, sketchValveNames, equationsByName,
                    constantValues, timeUnit, subscriptDimensions,
                    subscriptDisplayLabels, subscriptMappings, warnings);
        }
    }

    /**
     * Splits a subscript expression into per-label values if it's comma-separated.
     * Returns null if the expression is a formula (not comma-separated values).
     */
    private static List<String> splitSubscriptValues(String expression, int expectedCount) {
        if (expression == null || expression.isBlank()) {
            return null;
        }
        // Split on top-level commas only (respect parentheses)
        List<String> parts = new ArrayList<>();
        int depth = 0;
        int start = 0;
        for (int i = 0; i < expression.length(); i++) {
            char c = expression.charAt(i);
            if (c == '(') {
                depth++;
            } else if (c == ')') {
                depth--;
            } else if (c == ',' && depth == 0) {
                parts.add(expression.substring(start, i));
                start = i + 1;
            }
        }
        parts.add(expression.substring(start));

        // Only treat as per-label values if count matches exactly
        if (parts.size() == expectedCount) {
            // Verify all parts are numeric literals (constants)
            boolean allNumeric = parts.stream()
                    .allMatch(p -> NUMERIC_PATTERN.matcher(p.strip()).matches());
            if (allNumeric) {
                return parts;
            }
        }
        return null;
    }

    /**
     * Replaces a dimension name within bracket subscripts in an expression.
     * Handles both single and comma-separated subscripts.
     * For example, with dimName="task", replacement="design":
     * "x[task]" → "x[design]" and "y[task,prereqtask]" → "y[design,prereqtask]"
     */
    private static String replaceDimInSubscripts(String expr, String dimName, String replacement) {
        StringBuilder result = new StringBuilder();
        int pos = 0;
        while (pos < expr.length()) {
            int bracketStart = expr.indexOf('[', pos);
            if (bracketStart < 0) {
                result.append(expr, pos, expr.length());
                break;
            }
            // Find matching ']' accounting for nested brackets and quoted strings
            int bracketEnd = findMatchingBracket(expr, bracketStart);
            if (bracketEnd < 0) {
                result.append(expr, pos, expr.length());
                break;
            }
            result.append(expr, pos, bracketStart + 1);
            String content = expr.substring(bracketStart + 1, bracketEnd);

            String[] parts = content.split(",", -1);
            for (int j = 0; j < parts.length; j++) {
                if (parts[j].strip().equals(dimName)) {
                    parts[j] = parts[j].replace(dimName, replacement);
                }
            }
            result.append(String.join(",", parts));
            result.append(']');
            pos = bracketEnd + 1;
        }
        return result.toString();
    }

    /**
     * Finds the closing ']' that matches the opening '[' at the given position,
     * accounting for nested brackets and skipping content inside double quotes.
     */
    private static int findMatchingBracket(String expr, int openPos) {
        int depth = 0;
        boolean inQuote = false;
        for (int i = openPos; i < expr.length(); i++) {
            char c = expr.charAt(i);
            if (c == '"') {
                inQuote = !inQuote;
            } else if (!inQuote) {
                if (c == '[') {
                    depth++;
                } else if (c == ']') {
                    depth--;
                    if (depth == 0) {
                        return i;
                    }
                }
            }
        }
        return -1;
    }

    /**
     * Resolves multi-dimensional subscript labels into a cross-product list.
     * Each sub is checked against subscriptDimensions; dimensions expand to labels,
     * specific labels become singletons.
     */
    private static List<List<String>> resolveMultiDimLabels(String dimNameRaw,
                                                             Map<String, List<String>> subscriptDimensions) {
        String[] subs = dimNameRaw.split(",");
        List<List<String>> perDimLabels = new ArrayList<>();
        for (String sub : subs) {
            String key = VensimExprTranslator.normalizeName(sub.strip());
            List<String> dimLabels = subscriptDimensions.get(key);
            if (dimLabels != null) {
                perDimLabels.add(dimLabels);
            } else {
                perDimLabels.add(List.of(key));
            }
        }
        return crossProduct(perDimLabels);
    }

    private static List<List<String>> crossProduct(List<List<String>> lists) {
        List<List<String>> result = new ArrayList<>();
        result.add(new ArrayList<>());
        for (List<String> list : lists) {
            List<List<String>> newResult = new ArrayList<>();
            for (List<String> existing : result) {
                for (String item : list) {
                    List<String> combo = new ArrayList<>(existing);
                    combo.add(item);
                    newResult.add(combo);
                }
            }
            result = newResult;
        }
        return result;
    }

    /**
     * Expands a variable with multi-dimensional subscripts (comma-separated).
     * Generates cross-product of dimension labels and creates per-combination variables.
     */
    private void expandMultiDimSubscriptedVariable(MdlEquation eq, String baseName,
                                                    String dimNameRaw, String unit,
                                                    ModelDefinitionBuilder builder,
                                                    Set<String> vensimNames,
                                                    Set<String> stockNames,
                                                    Set<String> flowNames,
                                                    Set<String> lookupNames,
                                                    Set<String> sketchFlowNames,
                                                    Set<String> sketchValveNames,
                                                    Map<String, MdlEquation> equationsByName,
                                                    Map<String, Double> constantValues,
                                                    String timeUnit,
                                                    Map<String, List<String>> subscriptDimensions,
                                                    Map<String, List<String>> subscriptDisplayLabels,
                                                    Map<String, SubscriptMapping> subscriptMappings,
                                                    List<String> warnings) {
        List<List<String>> combos = resolveMultiDimLabels(dimNameRaw, subscriptDimensions);

        // Build display label combos
        String[] rawSubs = dimNameRaw.split(",");
        List<List<String>> displayPerDim = new ArrayList<>();
        for (String sub : rawSubs) {
            String key = VensimExprTranslator.normalizeName(sub.strip());
            List<String> dimDisplayLabels = subscriptDisplayLabels.get(key);
            if (dimDisplayLabels != null) {
                displayPerDim.add(dimDisplayLabels);
            } else {
                displayPerDim.add(List.of(VensimExprTranslator.normalizeDisplayName(sub.strip())));
            }
        }
        List<List<String>> displayCombos = crossProduct(displayPerDim);

        String normalizedBase = VensimExprTranslator.normalizeName(baseName);
        String displayBase = VensimExprTranslator.normalizeDisplayName(baseName);

        List<String> perLabelValues = splitSubscriptValues(
                eq.expression(), combos.size());

        for (int ci = 0; ci < combos.size(); ci++) {
            List<String> combo = combos.get(ci);
            List<String> displayCombo = displayCombos.get(ci);

            String expandedEqName = normalizedBase + "_" + String.join("_", combo);
            String expandedDisplayName = displayBase + " " + String.join(" ", displayCombo);
            String comment = eq.comment().isBlank() ? eq.name().strip() : eq.comment();

            String labelExpression;
            if (perLabelValues != null) {
                labelExpression = perLabelValues.get(ci).strip();
            } else {
                // Formula: replace each dimension subscript with the specific label
                labelExpression = eq.expression();
                String[] subs = dimNameRaw.split(",");
                for (int si = 0; si < subs.length; si++) {
                    String sub = subs[si].strip();
                    String subKey = VensimExprTranslator.normalizeName(sub);
                    List<String> dimLabels = subscriptDimensions.get(subKey);
                    if (dimLabels != null) {
                        // This sub is a dimension — find which label from the combo
                        // by tracking which dimension index this is
                        int dimIdx = 0;
                        for (int pi = 0; pi < si; pi++) {
                            String prevKey = VensimExprTranslator.normalizeName(
                                    subs[pi].strip());
                            if (subscriptDimensions.containsKey(prevKey)) {
                                dimIdx++;
                            }
                        }
                        // combo has all labels flattened; need the one for this dim
                        // For cross-product, each combo element corresponds to the
                        // dimension order. Find the right label.
                        String label = combo.get(si);
                        labelExpression = replaceDimInSubscripts(
                                labelExpression, sub, label);
                    }
                }
            }

            MdlEquation labelEq = new MdlEquation(
                    expandedDisplayName, eq.operator(), labelExpression,
                    eq.units(), comment, eq.group());
            classifyAndBuild(labelEq, expandedDisplayName, expandedEqName,
                    unit, comment, builder, vensimNames, stockNames, flowNames,
                    lookupNames, sketchFlowNames, sketchValveNames, equationsByName,
                    constantValues, timeUnit, subscriptDimensions,
                    subscriptDisplayLabels, subscriptMappings, warnings);
        }
    }

    private void buildStock(MdlEquation eq, String displayName, String eqName,
                             String expression, String unit, String comment,
                             ModelDefinitionBuilder builder,
                             Set<String> vensimNames, Set<String> flowNames,
                             Set<String> lookupNames, Set<String> sketchFlowNames,
                             Set<String> sketchValveNames,
                             Map<String, MdlEquation> equationsByName,
                             Map<String, Double> constantValues,
                             String timeUnit,
                             Map<String, List<String>> subscriptDimensions,
                             List<String> warnings) {
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
                initExpr, constantValues, vensimNames, lookupNames,
                subscriptDimensions, warnings, eq.name());

        // Create stock (display name preserves spaces)
        if (initResult.expression != null) {
            builder.stock(new StockDef(displayName, comment, 0.0, initResult.expression,
                    unit, null, null));
        } else {
            builder.stock(new StockDef(displayName, comment, initResult.value, unit, null));
        }

        // Try to decompose rate expression into individual flows.
        // In Vensim, INTEG(a + b - c, init) implies inflows a, b and outflow c.
        List<RateTerm> terms = splitRateTerms(rateExpr);
        if (terms != null && terms.size() > 1) {
            for (int i = 0; i < terms.size(); i++) {
                RateTerm term = terms.get(i);
                // Try to match the rate term to a sketch flow valve name.
                // When matched, use the variable's actual equation as the flow equation
                // and skip creating the variable (since the flow replaces it).
                String matchedValveName = matchSketchValveName(term.expr, sketchValveNames);
                String flowDisplayName;
                String flowEqName;
                String flowEquation;
                if (matchedValveName != null) {
                    flowDisplayName = matchedValveName;
                    flowEqName = VensimExprTranslator.normalizeName(matchedValveName);
                    MdlEquation varEq = equationsByName.get(flowEqName);
                    if (varEq != null && !varEq.expression().isBlank()) {
                        VensimExprTranslator.TranslationResult varTr =
                                VensimExprTranslator.translate(varEq.expression(),
                                        flowEqName, vensimNames, lookupNames,
                                        subscriptDimensions);
                        warnings.addAll(varTr.warnings());
                        flowEquation = varTr.expression();
                    } else {
                        // Fallback: use the term expression directly
                        VensimExprTranslator.TranslationResult tr =
                                VensimExprTranslator.translate(term.expr, eqName,
                                        vensimNames, lookupNames, subscriptDimensions);
                        warnings.addAll(tr.warnings());
                        flowEquation = tr.expression();
                    }
                } else {
                    String flowSuffix = term.positive ? "_inflow_" + i : "_outflow_" + i;
                    flowDisplayName = displayName
                            + (term.positive ? " inflow " : " outflow ") + (i + 1);
                    flowEqName = eqName + flowSuffix;
                    VensimExprTranslator.TranslationResult tr =
                            VensimExprTranslator.translate(term.expr, eqName,
                                    vensimNames, lookupNames, subscriptDimensions);
                    warnings.addAll(tr.warnings());
                    flowEquation = tr.expression();
                }
                if (term.positive) {
                    builder.flow(new FlowDef(flowDisplayName, null,
                            flowEquation, timeUnit, null, displayName));
                } else {
                    builder.flow(new FlowDef(flowDisplayName, null,
                            flowEquation, timeUnit, displayName, null));
                }
                flowNames.add(flowEqName);
                sketchFlowNames.add(flowEqName);
            }
            sketchFlowNames.add(eqName);
        } else {
            // Fall back to single net flow
            String flowDisplayName = displayName + " net flow";
            String flowEqName = eqName + "_net_flow";
            VensimExprTranslator.TranslationResult tr =
                    VensimExprTranslator.translate(rateExpr, eqName, vensimNames,
                            lookupNames, subscriptDimensions);
            warnings.addAll(tr.warnings());

            builder.flow(new FlowDef(flowDisplayName, "Net flow for " + eq.name(),
                    tr.expression(), timeUnit, null, displayName));
            flowNames.add(flowEqName);
            sketchFlowNames.add(eqName);
            sketchFlowNames.add(flowEqName);
        }
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
                points[0], points[1], "LINEAR", unit));
        lookupNames.add(eqName);
    }

    /**
     * Sorts and deduplicates lookup table x-values so they are strictly increasing.
     * Non-monotonic x-values are sorted; duplicate x-values keep the last y-value.
     */
    private static double[][] deduplicateLookupPoints(double[][] points,
                                                       String name,
                                                       List<String> warnings) {
        double[] xs = points[0];
        double[] ys = points[1];

        // Check if x-values are already strictly increasing
        boolean sorted = true;
        for (int i = 1; i < xs.length; i++) {
            if (xs[i] <= xs[i - 1]) {
                sorted = false;
                break;
            }
        }
        if (sorted) {
            return points;
        }

        // Build index array and sort by x-value (stable sort preserves original order for ties)
        Integer[] indices = new Integer[xs.length];
        for (int i = 0; i < indices.length; i++) {
            indices[i] = i;
        }
        Arrays.sort(indices, (a, b) -> {
            int cmp = Double.compare(xs[a], xs[b]);
            return cmp != 0 ? cmp : Integer.compare(a, b);
        });

        double[] sortedXs = new double[xs.length];
        double[] sortedYs = new double[ys.length];
        for (int i = 0; i < indices.length; i++) {
            sortedXs[i] = xs[indices[i]];
            sortedYs[i] = ys[indices[i]];
        }

        // Deduplicate consecutive x-values (keep last y-value)
        List<Double> newXs = new ArrayList<>();
        List<Double> newYs = new ArrayList<>();
        newXs.add(sortedXs[0]);
        newYs.add(sortedYs[0]);
        int dupes = 0;
        for (int i = 1; i < sortedXs.length; i++) {
            if (sortedXs[i] == sortedXs[i - 1]) {
                newYs.set(newYs.size() - 1, sortedYs[i]);
                dupes++;
            } else {
                newXs.add(sortedXs[i]);
                newYs.add(sortedYs[i]);
            }
        }

        if (dupes > 0) {
            warnings.add("Lookup '" + name + "': sorted non-monotonic x-values and removed "
                    + dupes + " duplicate(s)");
        } else {
            warnings.add("Lookup '" + name + "': sorted non-monotonic x-values");
        }
        return new double[][]{
                newXs.stream().mapToDouble(Double::doubleValue).toArray(),
                newYs.stream().mapToDouble(Double::doubleValue).toArray()
        };
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
                                                  Map<String, List<String>> subscriptDimensions,
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
                VensimExprTranslator.translate(trimmed, varName + "_init", vensimNames,
                        lookupNames, subscriptDimensions);
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

    /**
     * Extracts flow valve display names from sketch section type-11 lines.
     *
     * @param sketchLines the raw sketch lines from the .mdl file
     * @return a set of display names for flow valve elements
     */
    private static Set<String> extractSketchFlowValveNames(List<String> sketchLines) {
        Set<String> names = new HashSet<>();
        for (String line : sketchLines) {
            String trimmed = line.strip();
            if (!trimmed.startsWith("11,")) {
                continue;
            }
            String[] parts = trimmed.split(",");
            if (parts.length < 3) {
                continue;
            }
            String displayName = VensimExprTranslator.normalizeDisplayName(parts[2].strip());
            if (!displayName.isEmpty()) {
                names.add(displayName);
            }
        }
        return names;
    }

    /**
     * Attempts to match a rate term expression to a sketch flow valve name.
     * Returns the matching display name when the term is a simple variable reference
     * that matches a sketch valve, or {@code null} if no match is found.
     *
     * @param termExpr the rate term expression (e.g., "Infection Rate")
     * @param sketchValveNames the set of known sketch flow valve display names
     * @return the matching display name, or null
     */
    private static String matchSketchValveName(String termExpr, Set<String> sketchValveNames) {
        if (termExpr == null || sketchValveNames.isEmpty()) {
            return null;
        }
        // Only match simple variable references (no operators, no function calls)
        String stripped = termExpr.strip();
        if (stripped.contains("(") || stripped.contains(")") || stripped.contains("+")
                || stripped.contains("-") || stripped.contains("*") || stripped.contains("/")) {
            return null;
        }
        String displayName = VensimExprTranslator.normalizeDisplayName(stripped);
        if (sketchValveNames.contains(displayName)) {
            return displayName;
        }
        return null;
    }

    /**
     * A single term from a rate expression, with sign information.
     */
    record RateTerm(String expr, boolean positive) {}

    /**
     * A subscript dimension mapping (e.g., "previous cohort" maps to "all but youngest").
     *
     * @param normalizedLabels the normalized label names of this mapping dimension
     * @param targetDimension  the normalized name of the target dimension
     * @param rawDimName       the raw Vensim name of this mapping dimension
     * @param rawLabels        the raw label names (for expression replacement)
     */
    record SubscriptMapping(String targetDimension,
                             String rawDimName, List<String> rawLabels) {}

    /**
     * Attempts to split a rate expression into individual additive/subtractive terms
     * at the top level (respecting parentheses). Returns {@code null} if the expression
     * cannot be decomposed (e.g., a single term or contains only complex sub-expressions).
     *
     * <p>Example: {@code "births - deaths"} → [{@code "births", true}, {@code "deaths", false}]
     */
    static List<RateTerm> splitRateTerms(String rateExpr) {
        if (rateExpr == null || rateExpr.isBlank()) {
            return null;
        }
        String expr = rateExpr.strip();
        List<RateTerm> terms = new ArrayList<>();
        int depth = 0;
        boolean inQuote = false;
        int termStart = 0;
        boolean positive = true;

        // Handle leading minus
        if (expr.charAt(0) == '-') {
            positive = false;
            termStart = 1;
        } else if (expr.charAt(0) == '+') {
            termStart = 1;
        }

        for (int i = termStart; i < expr.length(); i++) {
            char c = expr.charAt(i);
            if (c == '"') {
                inQuote = !inQuote;
            } else if (inQuote) {
                continue;
            } else if (c == '(') {
                depth++;
            } else if (c == ')') {
                depth--;
            } else if (depth == 0 && (c == '+' || c == '-') && isBinaryOperatorAt(expr, i)) {
                String termText = expr.substring(termStart, i).strip();
                if (!termText.isEmpty()) {
                    terms.add(new RateTerm(termText, positive));
                }
                positive = (c == '+');
                termStart = i + 1;
            }
        }
        // Add the last term
        String lastTerm = expr.substring(termStart).strip();
        if (!lastTerm.isEmpty()) {
            terms.add(new RateTerm(lastTerm, positive));
        }

        // Only decompose if we found more than one term
        if (terms.size() <= 1) {
            return null;
        }
        return terms;
    }

    private static final Pattern A_FUNCTION_OF_PATTERN = Pattern.compile(
            "(?i)^\\s*A\\s+FUNCTION\\s+OF\\s*\\(");

    /**
     * Returns true if the expression is a Vensim "A FUNCTION OF" documentation
     * block, which lists dependencies for documentation purposes only.
     */
    private static boolean isDocumentationBlock(String expression) {
        return expression != null && A_FUNCTION_OF_PATTERN.matcher(expression).find();
    }

    /**
     * Returns true if the {@code +} or {@code -} at position {@code i} is a binary
     * operator (subtraction/addition) rather than a unary sign. A sign is binary when
     * the previous non-whitespace character could end a term: a letter, digit,
     * underscore, closing paren, closing quote, or decimal point.
     */
    private static boolean isBinaryOperatorAt(String expr, int i) {
        for (int j = i - 1; j >= 0; j--) {
            char prev = expr.charAt(j);
            if (Character.isWhitespace(prev)) {
                continue;
            }
            return Character.isLetterOrDigit(prev) || prev == '_' || prev == ')'
                    || prev == '.' || prev == '"';
        }
        return false;
    }

    private static int findTopLevelComma(String content) {
        return FormatUtils.findTopLevelComma(content);
    }
}
