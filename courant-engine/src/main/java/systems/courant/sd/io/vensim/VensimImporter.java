package systems.courant.sd.io.vensim;

import systems.courant.sd.io.AbstractModelImporter;
import systems.courant.sd.io.FormatUtils;
import systems.courant.sd.io.ImportResult;
import systems.courant.sd.io.ReferenceDataCsvReader;
import systems.courant.sd.model.def.ReferenceDataset;
import systems.courant.sd.model.def.VariableDef;
import systems.courant.sd.model.def.CldVariableDef;
import systems.courant.sd.model.def.FlowDef;
import systems.courant.sd.model.def.LookupTableDef;
import systems.courant.sd.model.def.ModelDefinitionBuilder;
import systems.courant.sd.model.def.StockDef;

import java.io.IOException;
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
public class VensimImporter extends AbstractModelImporter {

    private static final Pattern INTEG_PATTERN = Pattern.compile(
            "(?i)^INTEG\\s*\\(");
    private static final Set<String> SYSTEM_VAR_KEYS = Set.of(
            "INITIAL TIME", "FINAL TIME", "TIME STEP", "SAVEPER");
    private static final Pattern SUBSCRIPT_NAME_PATTERN = Pattern.compile(
            "^(.+?)\\[(.+?)\\]$");
    private static final Set<String> CONTROL_GROUPS = Set.of(".Control");
    // MAX_FILE_SIZE inherited from AbstractModelImporter

    @Override
    public ImportResult importModel(Path path) throws IOException {
        long size = Files.size(path);
        if (size > MAX_FILE_SIZE) {
            throw new IOException("File exceeds maximum allowed size of "
                    + (MAX_FILE_SIZE / (1024 * 1024)) + " MB: " + path);
        }
        String content = readFileContent(path);
        if (content.indexOf('\0') >= 0) {
            throw new IOException("File appears to be binary, not a valid Vensim .mdl file: " + path);
        }
        String modelName = extractModelName(path);
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

        List<MdlEquation> equations = expandMacros(parsed, warnings);

        SubscriptContext subscripts = SubscriptProcessor.collectNamesAndSubscripts(equations);
        SubscriptProcessor.resolveEquivalences(subscripts);

        SimulationSettings sim = extractSimulationSettings(
                subscripts.controlVars(), warnings);

        ModelDefinitionBuilder builder = new ModelDefinitionBuilder()
                .name(modelName)
                .defaultSimulation(new systems.courant.sd.model.def.SimulationSettings(
                        sim.timeUnit(), sim.duration(), sim.timeUnit(), sim.timeStep(),
                        false, 1, sim.initialTime()));

        SubscriptProcessor.registerEquivalenceDimensions(subscripts, builder);

        PreClassificationResult preClassification = ElementClassifier.preClassifyEquations(
                equations, subscripts, sim, warnings);

        boolean isCld = SketchProcessor.detectCldMode(
                preClassification.stockNames(), parsed.sketchLines());
        Set<String> sketchValveNames = SketchProcessor.extractSketchFlowValveNames(
                parsed.sketchLines());
        Map<String, MdlEquation> equationsByName = buildEquationsByNameIndex(
                equations, warnings);

        Set<String> sketchFlowNames = buildModelDefinitions(
                equations, isCld, builder, subscripts, preClassification,
                sketchValveNames, equationsByName, sim.timeUnit(), warnings);

        SketchProcessor.parseSketchViews(parsed.sketchLines(), builder, isCld,
                preClassification.stockNames(), sketchFlowNames,
                preClassification.lookupNames(), preClassification.cldVariableNames());

        if (baseDir != null) {
            resolveCompanionCsvFiles(equations, baseDir, builder, warnings);
        }

        return new ImportResult(builder.build(), warnings);
    }

    record SimulationSettings(double initialTime, double finalTime,
                              double timeStep, String timeUnit,
                              double duration) {}

    record PreClassificationResult(Set<String> stockNames,
                                   Set<String> flowNames,
                                   Set<String> lookupNames,
                                   Set<String> cldVariableNames,
                                   Set<String> allNormalizedNames,
                                   Map<String, Double> constantValues) {}

    record SubscriptContext(Set<String> vensimNames,
                            Map<String, MdlEquation> controlVars,
                            Map<String, List<String>> subscriptDimensions,
                            Map<String, List<String>> subscriptDisplayLabels,
                            Map<String, SubscriptMapping> subscriptMappings,
                            Map<String, String> equivalences,
                            Map<String, String> equivalenceDisplayNames) {}

    private List<MdlEquation> expandMacros(MdlParser.ParsedMdl parsed,
                                            List<String> warnings) {
        if (!parsed.macros().isEmpty()) {
            MacroExpander.ExpansionResult expansion = MacroExpander.expand(
                    parsed.equations(), parsed.macros());
            warnings.addAll(expansion.warnings());
            return expansion.expandedEquations();
        }
        return parsed.equations();
    }

    // Subscript collection and resolution delegated to SubscriptProcessor

    private SimulationSettings extractSimulationSettings(
            Map<String, MdlEquation> controlVars, List<String> warnings) {
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

        return new SimulationSettings(initialTime, finalTime, timeStepValue,
                timeUnit, duration);
    }

    // Pre-classification delegated to ElementClassifier
    // CLD detection and sketch parsing delegated to SketchProcessor

    private Map<String, MdlEquation> buildEquationsByNameIndex(
            List<MdlEquation> equations, List<String> warnings) {
        Map<String, MdlEquation> equationsByName = new LinkedHashMap<>();
        for (MdlEquation eq : equations) {
            String name = eq.name().strip();
            if (!name.isEmpty()) {
                String norm = VensimExprTranslator.normalizeName(name);
                MdlEquation previous = equationsByName.put(norm, eq);
                if (previous != null
                        && !":".equals(eq.operator())
                        && !":".equals(previous.operator())
                        && !"<->".equals(eq.operator())
                        && !"<->".equals(previous.operator())
                        && !ElementClassifier.isDocumentationBlock(previous.expression())) {
                    warnings.add("Duplicate normalized equation name '" + norm
                            + "' (from '" + name + "', previously from '"
                            + previous.name().strip() + "') \u2014 later definition wins");
                }
            }
        }
        return equationsByName;
    }

    private Set<String> buildModelDefinitions(
            List<MdlEquation> equations, boolean isCld,
            ModelDefinitionBuilder builder, SubscriptContext subscripts,
            PreClassificationResult preClassification,
            Set<String> sketchValveNames,
            Map<String, MdlEquation> equationsByName,
            String timeUnit, List<String> warnings) {
        Set<String> sketchFlowNames = new HashSet<>();

        ImportContext ctx = new ImportContext(builder, subscripts.vensimNames,
                preClassification.stockNames, preClassification.flowNames,
                preClassification.lookupNames, sketchFlowNames, sketchValveNames,
                equationsByName, preClassification.constantValues, timeUnit,
                subscripts.subscriptDimensions, subscripts.subscriptDisplayLabels,
                subscripts.subscriptMappings, preClassification.cldVariableNames,
                preClassification.allNormalizedNames, warnings);

        for (MdlEquation eq : equations) {
            String name = eq.name().strip();
            if (name.isEmpty() || isSystemVar(name)) {
                continue;
            }
            if (!isCld && ElementClassifier.isDocumentationBlock(eq.expression())) {
                continue;
            }
            String eqName = VensimExprTranslator.normalizeName(name);
            String displayName = VensimExprTranslator.normalizeDisplayName(name);
            if (!eq.operator().equals(":") && !eq.operator().equals("<->")
                    && !ctx.allNormalizedNames().add(eqName)) {
                warnings.add("Duplicate normalized name '" + eqName
                        + "' (from '" + name + "') — skipped");
                continue;
            }
            String comment = eq.comment().isBlank() ? name : eq.comment();

            if (isCld) {
                try {
                    classifyAndBuildCld(eq, displayName, eqName, comment, ctx);
                } catch (IllegalArgumentException e) {
                    warnings.add("Error processing '" + name + "': " + e.getMessage());
                }
            } else {
                String unit = cleanUnits(eq.units());
                try {
                    classifyAndBuild(eq, displayName, eqName, unit, comment, ctx);
                } catch (IllegalArgumentException e) {
                    warnings.add("Error processing '" + name + "': " + e.getMessage());
                }
            }
        }
        return sketchFlowNames;
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
                // Resolve symlinks to prevent symlink-based path traversal
                try {
                    Path realCsvPath = csvPath.toRealPath();
                    Path realBaseDir = baseDir.toRealPath();
                    if (!realCsvPath.startsWith(realBaseDir)) {
                        warnings.add("Rejected companion CSV path '" + filePath
                                + "': resolves outside model directory via symlink");
                        continue;
                    }
                } catch (IOException e) {
                    warnings.add("Failed to resolve companion CSV path '" + filePath
                            + "': " + e.getMessage());
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
                                   String unit, String comment, ImportContext ctx) {
        String operator = eq.operator();
        String expression = eq.expression();
        ModelDefinitionBuilder builder = ctx.builder();
        List<String> warnings = ctx.warnings();

        // Check if variable name contains subscript notation: name[Dimension]
        Matcher subscriptMatcher = SUBSCRIPT_NAME_PATTERN.matcher(eq.name().strip());
        if (subscriptMatcher.matches()) {
            String baseName = subscriptMatcher.group(1).strip();
            String dimNameRaw = subscriptMatcher.group(2).strip();
            String dimName = VensimExprTranslator.normalizeName(dimNameRaw);
            List<String> normalizedLabels = ctx.subscriptDimensions().get(dimName);
            List<String> displayLabels = ctx.subscriptDisplayLabels().get(dimName);
            if (normalizedLabels != null) {
                VensimSubscriptExpander.expandSubscriptedVariable(
                        eq, baseName, dimName, dimNameRaw,
                        normalizedLabels, displayLabels, unit, ctx,
                        (lEq, lDisp, lName, lUnit, lComment) ->
                                classifyAndBuild(lEq, lDisp, lName, lUnit, lComment, ctx));
                return;
            }

            // Check for multi-dimensional subscripts: name[sub1,sub2]
            if (dimNameRaw.contains(",")) {
                VensimSubscriptExpander.expandMultiDimSubscriptedVariable(
                        eq, baseName, dimNameRaw, unit, ctx,
                        (lEq, lDisp, lName, lUnit, lComment) ->
                                classifyAndBuild(lEq, lDisp, lName, lUnit, lComment, ctx));
                return;
            }

            // Check if subscript is a single label (not a dimension name)
            String normalizedSub = VensimExprTranslator.normalizeName(dimNameRaw);
            boolean isLabel = ctx.subscriptDimensions().values().stream()
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
                        unit, lComment, ctx);
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
            buildLookupTable(displayName, eqName, expression, unit, comment,
                    builder, ctx.lookupNames(), warnings);
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
            buildStock(eq, displayName, eqName, expression, unit, comment, ctx);
            return;
        }

        // Unchangeable constant (operator "==")
        if (operator.equals("==")) {
            if (isNumericLiteral(expression)) {
                builder.variable(new VariableDef(displayName, comment,
                        VariableDef.formatValue(Double.parseDouble(expression.strip())), unit));
            } else {
                VensimExprTranslator.TranslationResult tr =
                        VensimExprTranslator.translate(expression, eqName,
                                ctx.vensimNames(), ctx.lookupNames(),
                                ctx.subscriptDimensions());
                addExtractedLookups(tr, builder, ctx.lookupNames(), warnings);
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
        if (ctx.flowNames().contains(eqName)) {
            return;
        }

        // Numeric literal -> constant (literal-valued variable)
        if (isNumericLiteral(expression)) {
            builder.variable(new VariableDef(displayName, comment,
                    VariableDef.formatValue(Double.parseDouble(expression.strip())), unit));
            return;
        }

        // Check if expression contains WITH LOOKUP
        if (expression.toUpperCase(Locale.ROOT).contains("WITH LOOKUP")) {
            VensimExprTranslator.TranslationResult tr =
                    VensimExprTranslator.translate(expression, eqName,
                            ctx.vensimNames(), ctx.lookupNames(),
                            ctx.subscriptDimensions());
            addExtractedLookups(tr, builder, ctx.lookupNames(), warnings);
            builder.variable(new VariableDef(displayName, comment, tr.expression(), unit));
            warnings.addAll(tr.warnings());
            return;
        }

        // Default: variable
        VensimExprTranslator.TranslationResult tr =
                VensimExprTranslator.translate(expression, eqName,
                        ctx.vensimNames(), ctx.lookupNames(),
                        ctx.subscriptDimensions());
        addExtractedLookups(tr, builder, ctx.lookupNames(), warnings);
        builder.variable(new VariableDef(displayName, comment, tr.expression(), unit));
        warnings.addAll(tr.warnings());
    }

    private void classifyAndBuildCld(MdlEquation eq, String displayName, String eqName,
                                      String comment, ImportContext ctx) {
        String operator = eq.operator();

        // Skip subscript definitions and data variables in CLD mode
        if (operator.equals(":")) {
            return;
        }
        if (operator.equals(":=")) {
            ctx.warnings().add("Data variable '" + eq.name() + "' skipped (not supported)");
            return;
        }
        if (operator.equals("()")) {
            // Lookup tables don't map to CLD variables
            ctx.warnings().add("Lookup table '" + eq.name() + "' skipped in CLD mode");
            return;
        }

        ctx.builder().cldVariable(new CldVariableDef(displayName, comment));
        ctx.cldVariableNames().add(eqName);
    }

    private void buildStock(MdlEquation eq, String displayName, String eqName,
                             String expression, String unit, String comment,
                             ImportContext ctx) {
        ModelDefinitionBuilder builder = ctx.builder();
        List<String> warnings = ctx.warnings();

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
                initExpr, ctx.constantValues(), ctx.vensimNames(),
                ctx.lookupNames(), ctx.subscriptDimensions(), warnings, eq.name());

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
                String matchedValveName = SketchProcessor.matchSketchValveName(
                        term.expr, ctx.sketchValveNames());
                String flowDisplayName;
                String flowEqName;
                String flowEquation;
                if (matchedValveName != null) {
                    flowDisplayName = matchedValveName;
                    flowEqName = VensimExprTranslator.normalizeName(matchedValveName);
                    MdlEquation varEq = ctx.equationsByName().get(flowEqName);
                    if (varEq != null && !varEq.expression().isBlank()) {
                        VensimExprTranslator.TranslationResult varTr =
                                VensimExprTranslator.translate(varEq.expression(),
                                        flowEqName, ctx.vensimNames(),
                                        ctx.lookupNames(), ctx.subscriptDimensions());
                        warnings.addAll(varTr.warnings());
                        flowEquation = varTr.expression();
                    } else {
                        VensimExprTranslator.TranslationResult tr =
                                VensimExprTranslator.translate(term.expr, eqName,
                                        ctx.vensimNames(), ctx.lookupNames(),
                                        ctx.subscriptDimensions());
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
                                    ctx.vensimNames(), ctx.lookupNames(),
                                    ctx.subscriptDimensions());
                    warnings.addAll(tr.warnings());
                    flowEquation = tr.expression();
                }
                if (term.positive) {
                    builder.flow(new FlowDef(flowDisplayName, null,
                            flowEquation, ctx.timeUnit(), null, displayName));
                } else {
                    builder.flow(new FlowDef(flowDisplayName, null,
                            flowEquation, ctx.timeUnit(), displayName, null));
                }
                ctx.flowNames().add(flowEqName);
                ctx.sketchFlowNames().add(flowEqName);
            }
            ctx.sketchFlowNames().add(eqName);
        } else {
            // Fall back to single net flow
            String flowDisplayName = displayName + " net flow";
            String flowEqName = eqName + "_net_flow";
            VensimExprTranslator.TranslationResult tr =
                    VensimExprTranslator.translate(rateExpr, eqName,
                            ctx.vensimNames(), ctx.lookupNames(),
                            ctx.subscriptDimensions());
            warnings.addAll(tr.warnings());

            builder.flow(new FlowDef(flowDisplayName, "Net flow for " + eq.name(),
                    tr.expression(), ctx.timeUnit(), null, displayName));
            ctx.flowNames().add(flowEqName);
            ctx.sketchFlowNames().add(eqName);
            ctx.sketchFlowNames().add(flowEqName);
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
