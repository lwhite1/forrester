package systems.courant.sd.io.vensim;

import systems.courant.sd.io.FormatUtils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Translates Vensim expression syntax to Courant expression syntax.
 *
 * <p>Performs text-level transformations including multi-word name replacement,
 * function name mapping, logical operator conversion, and lookup extraction.
 */
public final class VensimExprTranslator {

    private static final Logger log = LoggerFactory.getLogger(VensimExprTranslator.class);

    private static final Pattern IF_THEN_ELSE_PATTERN = Pattern.compile(
            "(?i)IF\\s+THEN\\s+ELSE\\s*\\(");
    private static final Pattern AND_PATTERN = Pattern.compile(
            "(?i):AND:");
    private static final Pattern OR_PATTERN = Pattern.compile(
            "(?i):OR:");
    private static final Pattern NOT_PATTERN = Pattern.compile(
            "(?i):NOT:");
    private static final Pattern XIDZ_PATTERN = Pattern.compile(
            "(?i)XIDZ\\s*\\(");
    private static final Pattern ZIDZ_PATTERN = Pattern.compile(
            "(?i)ZIDZ\\s*\\(");
    private static final Pattern WITH_LOOKUP_PATTERN = Pattern.compile(
            "(?i)WITH\\s+LOOKUP\\s*\\(");
    private static final Pattern SMOOTH3_PATTERN = Pattern.compile(
            "(?i)SMOOTH3\\s*\\(");
    private static final Pattern SMOOTHI_PATTERN = Pattern.compile(
            "(?i)SMOOTHI\\s*\\(");
    private static final Pattern SMOOTH3I_PATTERN = Pattern.compile(
            "(?i)SMOOTH3I\\s*\\(");
    private static final Pattern DELAY1_PATTERN = Pattern.compile(
            "(?i)DELAY1\\s*\\(");
    private static final Pattern DELAY1I_PATTERN = Pattern.compile(
            "(?i)DELAY1I\\s*\\(");
    private static final Pattern DELAY_FIXED_PATTERN = Pattern.compile(
            "(?i)DELAY\\s+FIXED\\s*\\(");
    private static final Pattern GAME_PATTERN = Pattern.compile(
            "(?i)GAME\\s*\\(");
    private static final Pattern RANDOM_NORMAL_PATTERN = Pattern.compile(
            "(?i)RANDOM\\s+NORMAL\\s*\\(");
    private static final Pattern RANDOM_UNIFORM_PATTERN = Pattern.compile(
            "(?i)RANDOM\\s+UNIFORM\\s*\\(");
    private static final Pattern PULSE_TRAIN_PATTERN = Pattern.compile(
            "(?i)PULSE\\s+TRAIN\\s*\\(");
    private static final Pattern DELAY_MATERIAL_PATTERN = Pattern.compile(
            "(?i)DELAY\\s+MATERIAL\\s*\\(");
    private static final Pattern RANDOM_0_1_PATTERN = Pattern.compile(
            "(?i)RANDOM\\s+0\\s+1\\s*\\(\\s*\\)");
    private static final Pattern SUM_FUNC_PATTERN = Pattern.compile(
            "(?i)\\bSUM\\s*\\(");
    private static final Pattern VMIN_FUNC_PATTERN = Pattern.compile(
            "(?i)\\bVMIN\\s*\\(");
    private static final Pattern BANG_DIM_PATTERN = Pattern.compile(
            "([a-zA-Z_][a-zA-Z0-9_]*)!");
    private static final Pattern NOT_EQUAL_PATTERN = Pattern.compile("<>");
    private static final Pattern MESSAGE_PATTERN = Pattern.compile(
            "(?i)MESSAGE\\s*\\(");
    private static final Pattern SIMULTANEOUS_PATTERN = Pattern.compile(
            "(?i)SIMULTANEOUS\\s*\\(");
    private static final Pattern ACTIVE_INITIAL_PATTERN = Pattern.compile(
            "(?i)ACTIVE\\s+INITIAL\\s*\\(");
    private static final Pattern SAMPLE_IF_TRUE_PATTERN = Pattern.compile(
            "(?i)SAMPLE\\s+IF\\s+TRUE\\s*\\(");
    private static final Pattern FIND_ZERO_PATTERN = Pattern.compile(
            "(?i)FIND\\s+ZERO\\s*\\(");
    private static final Pattern LOOKUP_AREA_PATTERN = Pattern.compile(
            "(?i)LOOKUP\\s+AREA\\s*\\(");
    private static final Pattern GET_XLS_DATA_PATTERN = Pattern.compile(
            "(?i)GET\\s+XLS\\s+DATA\\s*\\(");
    private static final Pattern GET_DIRECT_DATA_PATTERN = Pattern.compile(
            "(?i)GET\\s+DIRECT\\s+DATA\\s*\\(");
    private static final Pattern GET_XLS_CONSTANTS_PATTERN = Pattern.compile(
            "(?i)GET\\s+XLS\\s+CONSTANTS\\s*\\(");
    private static final Pattern GET_DIRECT_CONSTANTS_PATTERN = Pattern.compile(
            "(?i)GET\\s+DIRECT\\s+CONSTANTS\\s*\\(");
    private static final Pattern GET_XLS_LOOKUPS_PATTERN = Pattern.compile(
            "(?i)GET\\s+XLS\\s+LOOKUPS\\s*\\(");
    private static final Pattern GET_DIRECT_LOOKUPS_PATTERN = Pattern.compile(
            "(?i)GET\\s+DIRECT\\s+LOOKUPS\\s*\\(");
    private static final Pattern CARET_PATTERN = Pattern.compile("\\^");
    private static final Pattern TIME_VAR_PATTERN = Pattern.compile(
            "(?i)\\bTime\\b");
    private static final Set<String> UNSUPPORTED_FUNCTIONS = Set.of(
            "DELAY N", "TABBED ARRAY",
            "VECTOR SELECT", "VECTOR ELM MAP", "VECTOR SORT ORDER",
            "ALLOCATE AVAILABLE");

    /**
     * Result of translating a Vensim expression.
     *
     * @param expression the translated Courant expression
     * @param lookups any lookup tables extracted from WITH LOOKUP constructs
     * @param warnings any translation warnings
     */
    public record TranslationResult(
            String expression,
            List<ExtractedLookup> lookups,
            List<String> warnings
    ) {
        public TranslationResult {
            lookups = lookups == null ? List.of() : List.copyOf(lookups);
            warnings = warnings == null ? List.of() : List.copyOf(warnings);
        }
    }

    /**
     * A lookup table extracted from a WITH LOOKUP expression.
     *
     * @param name the generated lookup table name
     * @param xValues the x-axis data points
     * @param yValues the y-axis data points
     */
    public record ExtractedLookup(String name, double[] xValues, double[] yValues) {}

    private VensimExprTranslator() {
    }

    /**
     * Translates a Vensim expression to Courant syntax.
     *
     * @param vensimExpr the Vensim expression string
     * @param varName the name of the variable this expression belongs to (used for lookup naming)
     * @param knownNames the set of all known multi-word variable names (in original Vensim form)
     * @return the translation result
     */
    public static TranslationResult translate(String vensimExpr, String varName,
                                               Set<String> knownNames) {
        return translate(vensimExpr, varName, knownNames, Set.of());
    }

    /**
     * Translates a Vensim expression to Courant syntax.
     *
     * @param vensimExpr the Vensim expression string
     * @param varName the name of the variable this expression belongs to (used for lookup naming)
     * @param knownNames the set of all known multi-word variable names (in original Vensim form)
     * @param lookupNames the set of known lookup table names (normalized); function calls matching
     *                    these are rewritten to LOOKUP(name, input) syntax
     * @return the translation result
     */
    public static TranslationResult translate(String vensimExpr, String varName,
                                               Set<String> knownNames,
                                               Set<String> lookupNames) {
        return translate(vensimExpr, varName, knownNames, lookupNames, Map.of());
    }

    /**
     * Translates a Vensim expression to Courant syntax with subscript dimension info
     * for expanding vector functions like SUM and VMIN.
     *
     * @param vensimExpr the Vensim expression string
     * @param varName the name of the variable this expression belongs to (used for lookup naming)
     * @param knownNames the set of all known multi-word variable names (in original Vensim form)
     * @param lookupNames the set of known lookup table names (normalized)
     * @param subscriptDimensions map from normalized dimension name to its normalized labels
     * @return the translation result
     */
    public static TranslationResult translate(String vensimExpr, String varName,
                                               Set<String> knownNames,
                                               Set<String> lookupNames,
                                               Map<String, List<String>> subscriptDimensions) {
        if (vensimExpr == null || vensimExpr.isBlank()) {
            return new TranslationResult(vensimExpr, List.of(), List.of());
        }

        List<String> warnings = new ArrayList<>();
        List<ExtractedLookup> lookups = new ArrayList<>();
        String expr = vensimExpr.strip();

        // 0. Replace quoted variable names: "name with (special) chars" → name_with_special_chars
        expr = translateQuotedNames(expr);

        // 1. Handle WITH LOOKUP first (before name replacement)
        expr = translateWithLookup(expr, varName, lookups, warnings);

        // 2. Replace multi-word names with underscored versions (longest first)
        expr = replaceMultiWordNames(expr, knownNames);

        // 3. IF THEN ELSE → IF
        expr = IF_THEN_ELSE_PATTERN.matcher(expr).replaceAll("IF(");

        // 4. Logical operators
        expr = AND_PATTERN.matcher(expr).replaceAll(" and ");
        expr = OR_PATTERN.matcher(expr).replaceAll(" or ");
        // :NOT: in Vensim has lower precedence than comparisons, so :NOT: x > 0 means
        // NOT(x > 0). We translate to not(...) by wrapping the remaining expression up to
        // the next logical operator or closing paren. For simple cases, we insert not(
        // and find the end of the operand.
        expr = translateNot(expr);

        // 4a. Not-equal operator: <> → !=
        expr = NOT_EQUAL_PATTERN.matcher(expr).replaceAll("!=");

        // 4b. Single = (equality) → == (Vensim uses = for both assignment and comparison;
        // by this point we're processing the RHS, so any remaining = is equality)
        expr = expr.replaceAll("(?<![<>=!])=(?!=)", "==");

        // 5. XIDZ and ZIDZ
        expr = translateXidz(expr, warnings);
        expr = translateZidz(expr, warnings);

        // 6. SMOOTH variants → native engine functions
        expr = SMOOTH3I_PATTERN.matcher(expr).replaceAll("SMOOTH3I(");
        expr = SMOOTH3_PATTERN.matcher(expr).replaceAll("SMOOTH3(");
        expr = SMOOTHI_PATTERN.matcher(expr).replaceAll("SMOOTHI(");

        // 7. DELAY1/DELAY1I → native engine functions
        expr = DELAY1I_PATTERN.matcher(expr).replaceAll("DELAY1I(");
        expr = DELAY1_PATTERN.matcher(expr).replaceAll("DELAY1(");

        // 8. DELAY FIXED → DELAY_FIXED
        expr = DELAY_FIXED_PATTERN.matcher(expr).replaceAll("DELAY_FIXED(");

        // 8-1. DELAY MATERIAL(input, delay, init, transit) → DELAY_FIXED(input, delay, init)
        expr = translateDelayMaterial(expr);

        // 8-2. RANDOM 0 1() → RANDOM_UNIFORM(0, 1, 0)
        expr = RANDOM_0_1_PATTERN.matcher(expr).replaceAll("RANDOM_UNIFORM(0, 1, 0)");

        // 8a. GAME(expr) → expr (pass-through; GAME is Vensim's interactive override)
        expr = translateGame(expr);

        // 8b. MESSAGE(args) → 0 (Vensim interactive messaging, no-op in Courant)
        expr = translateMessage(expr);

        // 8c. SIMULTANEOUS(args) → 0 (solver hint, no-op for Euler integration)
        expr = translateSimultaneous(expr);

        // 8d. ACTIVE INITIAL(expr, initial) → expr (pass-through first arg)
        expr = translateActiveInitial(expr);

        // 8e. RANDOM UNIFORM → RANDOM_UNIFORM, RANDOM NORMAL → RANDOM_NORMAL
        expr = RANDOM_UNIFORM_PATTERN.matcher(expr).replaceAll("RANDOM_UNIFORM(");
        expr = RANDOM_NORMAL_PATTERN.matcher(expr).replaceAll("RANDOM_NORMAL(");

        // 8f. SAMPLE IF TRUE → SAMPLE_IF_TRUE
        expr = SAMPLE_IF_TRUE_PATTERN.matcher(expr).replaceAll("SAMPLE_IF_TRUE(");

        // 8g. FIND ZERO → FIND_ZERO
        expr = FIND_ZERO_PATTERN.matcher(expr).replaceAll("FIND_ZERO(");

        // 8h. PULSE TRAIN → PULSE_TRAIN
        expr = PULSE_TRAIN_PATTERN.matcher(expr).replaceAll("PULSE_TRAIN(");

        // 8i. LOOKUP AREA → LOOKUP_AREA
        expr = LOOKUP_AREA_PATTERN.matcher(expr).replaceAll("LOOKUP_AREA(");

        // 9. ^ → ** (Vensim uses ^ for power, Courant uses **)
        expr = CARET_PATTERN.matcher(expr).replaceAll("**");

        // 10. Time → TIME (the built-in variable), unless "Time" is a user-defined name
        if (knownNames.stream().noneMatch(n -> n.equalsIgnoreCase("Time"))) {
            expr = TIME_VAR_PATTERN.matcher(expr).replaceAll("TIME");
        }

        // 10a. Expand SUM(expr[dim!]) and VMIN(expr[dim!]) using subscript dimensions
        if (subscriptDimensions != null && !subscriptDimensions.isEmpty()) {
            expr = expandVectorFunctions(expr, subscriptDimensions, warnings);
        }

        // 11. Translate subscript bracket notation: name[label] → name_label
        expr = translateSubscriptBrackets(expr);

        // 12. Rewrite lookupName(arg) → LOOKUP(lookupName, arg)
        expr = rewriteLookupCalls(expr, lookupNames);

        // 13. GET XLS/DIRECT functions → 0 placeholder with warning
        expr = translateGetFunction(expr, GET_XLS_DATA_PATTERN, "GET XLS DATA", warnings);
        expr = translateGetFunction(expr, GET_DIRECT_DATA_PATTERN, "GET DIRECT DATA", warnings);
        expr = translateGetFunction(expr, GET_XLS_CONSTANTS_PATTERN, "GET XLS CONSTANTS", warnings);
        expr = translateGetFunction(expr, GET_DIRECT_CONSTANTS_PATTERN,
                "GET DIRECT CONSTANTS", warnings);
        expr = translateGetFunction(expr, GET_XLS_LOOKUPS_PATTERN, "GET XLS LOOKUPS", warnings);
        expr = translateGetFunction(expr, GET_DIRECT_LOOKUPS_PATTERN,
                "GET DIRECT LOOKUPS", warnings);

        // 14. Check for unsupported functions
        checkUnsupportedFunctions(expr, warnings);

        // 15. Underscore any remaining consecutive identifiers that are not keywords.
        // At this point, all known multi-word function names (IF THEN ELSE, RANDOM NORMAL,
        // GET XLS DATA, etc.) have been translated. Any remaining adjacent identifiers
        // (separated only by whitespace, not operators) must be unknown multi-word variable
        // names — since two identifiers can never appear adjacent in a valid expression.
        expr = underscoreConsecutiveIdentifiers(expr);

        return new TranslationResult(expr, lookups, warnings);
    }

    /**
     * Normalizes a Vensim variable name to Courant identifier format.
     * Converts spaces to underscores, strips quotes, and trims whitespace.
     * Use this form for equation references (identifiers in formula text).
     *
     * @param vensimName the Vensim variable name
     * @return the normalized name with underscores instead of spaces
     */
    public static String normalizeName(String vensimName) {
        if (vensimName == null || vensimName.isBlank()) {
            return "";
        }
        String name = vensimName.strip();
        // Remove surrounding quotes if present
        if (name.startsWith("\"") && name.endsWith("\"") && name.length() > 2) {
            name = name.substring(1, name.length() - 1);
        }
        // Replace spaces and newlines with underscores
        name = name.replaceAll("\\s+", "_");
        // Remove any characters not valid in identifiers
        name = name.replaceAll("[^a-zA-Z0-9_]", "");
        // Ensure it doesn't start with a digit
        if (!name.isEmpty() && Character.isDigit(name.charAt(0))) {
            name = "_" + name;
        }
        return name;
    }

    /**
     * Normalizes a Vensim variable name to a human-readable display form.
     * Preserves spaces (collapsed to single space), strips quotes and special
     * characters. Use this form for element names displayed on the canvas,
     * in plots, and in dashboards.
     *
     * @param vensimName the Vensim variable name
     * @return the display name with spaces preserved
     */
    public static String normalizeDisplayName(String vensimName) {
        if (vensimName == null || vensimName.isBlank()) {
            return "";
        }
        String name = vensimName.strip();
        // Remove surrounding quotes if present
        if (name.startsWith("\"") && name.endsWith("\"") && name.length() > 2) {
            name = name.substring(1, name.length() - 1);
        }
        // Collapse whitespace to single spaces
        name = name.replaceAll("\\s+", " ");
        // Remove any characters not valid in display names (allow letters, digits, spaces, underscores)
        name = name.replaceAll("[^a-zA-Z0-9_ ]", "");
        // Trim after removing characters
        name = name.strip();
        // Ensure it doesn't start with a digit
        if (!name.isEmpty() && Character.isDigit(name.charAt(0))) {
            name = "_" + name;
        }
        return name;
    }

    /**
     * Replaces quoted variable references in Vensim expressions with normalized names.
     * Vensim uses quotes for names containing special characters: "electric vehicles (EV)".
     */
    private static String translateQuotedNames(String expr) {
        Pattern quoted = Pattern.compile("\"([^\"]+)\"");
        Matcher m = quoted.matcher(expr);
        StringBuilder sb = new StringBuilder();
        while (m.find()) {
            String name = m.group(1);
            m.appendReplacement(sb, Matcher.quoteReplacement(normalizeName(name)));
        }
        m.appendTail(sb);
        return sb.toString();
    }

    private static String translateWithLookup(String expr, String varName,
                                               List<ExtractedLookup> lookups,
                                               List<String> warnings) {
        Matcher m = WITH_LOOKUP_PATTERN.matcher(expr);
        if (!m.find()) {
            return expr;
        }

        int funcStart = m.start();
        int argsStart = m.end(); // position after opening paren
        // Find matching closing paren for WITH LOOKUP(...)
        int closeParen = findMatchingParen(expr, argsStart - 1);
        if (closeParen < 0) {
            warnings.add("Malformed WITH LOOKUP expression");
            return expr;
        }

        String argsContent = expr.substring(argsStart, closeParen);

        // Split into input expression and lookup data
        // The first comma at the top level separates input from lookup data
        int splitComma = findTopLevelComma(argsContent);
        if (splitComma < 0) {
            warnings.add("Malformed WITH LOOKUP: cannot find input/data separator");
            return expr;
        }

        String inputExpr = argsContent.substring(0, splitComma).strip();
        String lookupData = argsContent.substring(splitComma + 1).strip();

        // Parse lookup data points
        Optional<double[][]> pointsOpt = parseLookupPoints(lookupData);
        if (pointsOpt.isEmpty() || pointsOpt.get()[0].length < 2) {
            warnings.add("Could not parse lookup data points in WITH LOOKUP");
            return expr;
        }
        double[][] points = pointsOpt.get();

        String lookupName = normalizeName(varName) + "_lookup";
        lookups.add(new ExtractedLookup(lookupName, points[0], points[1]));

        // Replace WITH LOOKUP(...) with LOOKUP(name, input)
        String replacement = "LOOKUP(" + lookupName + ", " + inputExpr + ")";
        return expr.substring(0, funcStart) + replacement
                + expr.substring(closeParen + 1);
    }

    private static String translateXidz(String expr, List<String> warnings) {
        while (true) {
            Matcher m = XIDZ_PATTERN.matcher(expr);
            if (!m.find()) {
                break;
            }
            int funcStart = m.start();
            int argsStart = m.end();
            int closeParen = findMatchingParen(expr, argsStart - 1);
            if (closeParen < 0) {
                warnings.add("Malformed XIDZ expression");
                break;
            }
            String argsContent = expr.substring(argsStart, closeParen);
            List<String> args = splitTopLevelArgs(argsContent);
            if (args.size() != 3) {
                warnings.add("XIDZ requires 3 arguments, got " + args.size());
                break;
            }
            String a = args.get(0).strip();
            String b = args.get(1).strip();
            String x = args.get(2).strip();
            String replacement = "IF((" + b + ") == 0, " + x + ", (" + a + ") / (" + b + "))";
            expr = expr.substring(0, funcStart) + replacement + expr.substring(closeParen + 1);
        }
        return expr;
    }

    private static String translateZidz(String expr, List<String> warnings) {
        while (true) {
            Matcher m = ZIDZ_PATTERN.matcher(expr);
            if (!m.find()) {
                break;
            }
            int funcStart = m.start();
            int argsStart = m.end();
            int closeParen = findMatchingParen(expr, argsStart - 1);
            if (closeParen < 0) {
                warnings.add("Malformed ZIDZ expression");
                break;
            }
            String argsContent = expr.substring(argsStart, closeParen);
            List<String> args = splitTopLevelArgs(argsContent);
            if (args.size() != 2) {
                warnings.add("ZIDZ requires 2 arguments, got " + args.size());
                break;
            }
            String a = args.get(0).strip();
            String b = args.get(1).strip();
            String replacement = "IF((" + b + ") == 0, 0, (" + a + ") / (" + b + "))";
            expr = expr.substring(0, funcStart) + replacement + expr.substring(closeParen + 1);
        }
        return expr;
    }

    private static String translateNot(String expr) {
        // :NOT: in Vensim has lower precedence than comparisons.
        // We wrap the operand (up to the next logical operator, comma, or closing paren at depth 0)
        // in parentheses: :NOT: x > 0 → not(x > 0)
        Matcher m = NOT_PATTERN.matcher(expr);
        while (m.find()) {
            int notStart = m.start();
            int operandStart = m.end();
            // Find the end of the operand scope
            int depth = 0;
            int end = expr.length();
            for (int i = operandStart; i < expr.length(); i++) {
                char c = expr.charAt(i);
                if (c == '(') {
                    depth++;
                } else if (c == ')') {
                    if (depth == 0) {
                        end = i;
                        break;
                    }
                    depth--;
                } else if (depth == 0 && c == ',') {
                    end = i;
                    break;
                } else if (depth == 0 && i + 3 <= expr.length()) {
                    String ahead = expr.substring(i, Math.min(i + 4, expr.length()));
                    if (ahead.matches("(?i)and[^a-zA-Z0-9_].*") || ahead.matches("(?i)and$")
                            || ahead.matches("(?i)or[^a-zA-Z0-9_].*") || ahead.matches("(?i)or$")) {
                        end = i;
                        break;
                    }
                }
            }
            String operand = expr.substring(operandStart, end).strip();
            if (operand.isEmpty()) {
                // Trailing :NOT: with no operand — skip replacement
                break;
            }
            String replacement = "not(" + operand + ")";
            expr = expr.substring(0, notStart) + replacement + expr.substring(end);
            m = NOT_PATTERN.matcher(expr);
        }
        return expr;
    }

    private static String replaceMultiWordNames(String expr, Set<String> knownNames) {
        // Sort names longest-first to avoid partial matches
        List<String> sortedNames = knownNames.stream()
                .filter(n -> n.contains(" "))
                .sorted(Comparator.comparingInt(String::length).reversed()
                        .thenComparing(Comparator.naturalOrder()))
                .toList();

        // Two-pass approach: first try exact (case-sensitive) match, then fall back
        // to case-insensitive for any names not found exactly. This ensures that when
        // two names differ only by case, each is replaced with its own normalized form.
        List<String> unmatchedNames = new ArrayList<>();
        for (String name : sortedNames) {
            String normalized = normalizeName(name);
            String escaped = Pattern.quote(name);
            Pattern exact = Pattern.compile("(?<![a-zA-Z0-9_])" + escaped + "(?![a-zA-Z0-9_])");
            Matcher m = exact.matcher(expr);
            if (m.find()) {
                expr = m.replaceAll(normalized);
            } else {
                unmatchedNames.add(name);
            }
        }

        // Second pass: case-insensitive fallback for names not found exactly
        for (String name : unmatchedNames) {
            String normalized = normalizeName(name);
            String escaped = Pattern.quote(name);
            Pattern p = Pattern.compile("(?<![a-zA-Z0-9_])" + escaped + "(?![a-zA-Z0-9_])",
                    Pattern.CASE_INSENSITIVE);
            expr = p.matcher(expr).replaceAll(normalized);
        }
        return expr;
    }

    /**
     * Joins consecutive bare identifiers with underscores, excluding logical keywords
     * ({@code and}, {@code or}, {@code not}). In a valid Vensim expression, two identifiers
     * can never appear next to each other without an operator between them — so any remaining
     * consecutive identifiers must be unknown multi-word variable names.
     */
    private static final Set<String> EXPRESSION_KEYWORDS = Set.of("and", "or", "not");
    private static final Pattern CONSECUTIVE_IDENTS = Pattern.compile(
            "([a-zA-Z_][a-zA-Z0-9_]*)\\s+([a-zA-Z_][a-zA-Z0-9_]*)");

    private static String underscoreConsecutiveIdentifiers(String expr) {
        String prev;
        do {
            prev = expr;
            Matcher m = CONSECUTIVE_IDENTS.matcher(expr);
            StringBuilder sb = new StringBuilder();
            while (m.find()) {
                String left = m.group(1);
                String right = m.group(2);
                if (EXPRESSION_KEYWORDS.contains(left.toLowerCase(Locale.ROOT))
                        || EXPRESSION_KEYWORDS.contains(right.toLowerCase(Locale.ROOT))) {
                    m.appendReplacement(sb, Matcher.quoteReplacement(m.group()));
                } else {
                    m.appendReplacement(sb, Matcher.quoteReplacement(left + "_" + right));
                }
            }
            m.appendTail(sb);
            expr = sb.toString();
        } while (!expr.equals(prev));
        return expr;
    }

    static int findMatchingParen(String expr, int openParenPos) {
        if (openParenPos < 0 || openParenPos >= expr.length()
                || expr.charAt(openParenPos) != '(') {
            return -1;
        }
        int depth = 1;
        for (int i = openParenPos + 1; i < expr.length(); i++) {
            char c = expr.charAt(i);
            if (c == '(') {
                depth++;
            } else if (c == ')') {
                depth--;
                if (depth == 0) {
                    return i;
                }
            }
        }
        return -1;
    }

    /**
     * Rewrites function calls that match known lookup table names into explicit
     * LOOKUP(name, input) syntax. In Vensim, standalone lookup tables can be
     * invoked with function-call syntax: {@code tableName(input)}.
     */
    private static String rewriteLookupCalls(String expr, Set<String> lookupNames) {
        if (lookupNames == null || lookupNames.isEmpty()) {
            return expr;
        }
        for (String name : lookupNames) {
            // Match name followed by '(' (with optional whitespace)
            Pattern p = Pattern.compile("\\b(" + Pattern.quote(name) + ")\\s*\\(");
            Matcher m = p.matcher(expr);
            while (m.find()) {
                int funcStart = m.start();
                int argsStart = m.end();
                int closeParen = findMatchingParen(expr, argsStart - 1);
                if (closeParen > 0) {
                    String arg = expr.substring(argsStart, closeParen).strip();
                    String replacement = "LOOKUP(" + name + ", " + arg + ")";
                    expr = expr.substring(0, funcStart) + replacement
                            + expr.substring(closeParen + 1);
                    // Re-create matcher on the modified string, starting after the replacement
                    m = p.matcher(expr);
                } else {
                    break;
                }
            }
        }
        return expr;
    }

    private static int findTopLevelComma(String content) {
        return FormatUtils.findTopLevelComma(content);
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

    static Optional<double[][]> parseLookupPoints(String data) {
        // Lookup data format: [(xmin,ymin)-(xmax,ymax)],(x1,y1),(x2,y2),...
        // or ([(xmin,ymin)-(xmax,ymax)],(x1,y1),(x2,y2),...)  (WITH LOOKUP wrapping)
        // or just (x1,y1),(x2,y2),...
        // or flat CSV: x1,x2,...,xN,y1,y2,...,yN (older Vensim format)
        String cleaned = data.strip();

        // Strip Vensim range annotation: [(xmin,ymin)-(xmax,ymax)]
        // May be preceded by an outer paren from WITH LOOKUP context
        // This is a display range hint, not a data point
        Pattern rangePattern = Pattern.compile(
                "^\\s*\\(?\\s*\\[\\s*\\([^)]*\\)\\s*-\\s*\\([^)]*\\)\\s*\\]\\s*,?");
        Matcher rangeMatcher = rangePattern.matcher(cleaned);
        if (rangeMatcher.find()) {
            cleaned = cleaned.substring(rangeMatcher.end()).strip();
        } else {
            // Strip simple outer brackets if present
            if (cleaned.startsWith("[")) {
                cleaned = cleaned.substring(1);
            }
            if (cleaned.endsWith("]")) {
                cleaned = cleaned.substring(0, cleaned.length() - 1);
            }
            cleaned = cleaned.strip();
        }

        // Try parenthesized pair format: (x1,y1),(x2,y2),...
        List<double[]> points = new ArrayList<>();
        Pattern pairPattern = Pattern.compile(
                "\\(\\s*(-?[\\d.eE+\\-]+)\\s*,\\s*(-?[\\d.eE+\\-]+)\\s*\\)");
        Matcher m = pairPattern.matcher(cleaned);
        while (m.find()) {
            try {
                double x = Double.parseDouble(m.group(1));
                double y = Double.parseDouble(m.group(2));
                points.add(new double[]{x, y});
            } catch (NumberFormatException ex) {
                log.debug("Skip malformed lookup pair: ({}, {})", m.group(1), m.group(2), ex);
            }
        }

        if (points.size() >= 2) {
            double[] xValues = new double[points.size()];
            double[] yValues = new double[points.size()];
            for (int i = 0; i < points.size(); i++) {
                xValues[i] = points.get(i)[0];
                yValues[i] = points.get(i)[1];
            }
            return Optional.of(new double[][]{xValues, yValues});
        }

        // Fall back to flat CSV format: x1,x2,...,xN,y1,y2,...,yN
        // Used by older Vensim versions (e.g., BURNOUT.MDL, WORLD.MDL)
        return parseFlatCsvLookup(cleaned);
    }

    private static Optional<double[][]> parseFlatCsvLookup(String data) {
        Pattern numberPattern = Pattern.compile("-?[\\d.eE+\\-]+");
        Matcher m = numberPattern.matcher(data);
        List<Double> values = new ArrayList<>();
        while (m.find()) {
            try {
                values.add(Double.parseDouble(m.group()));
            } catch (NumberFormatException ex) {
                log.debug("Skip malformed flat lookup value: {}", m.group(), ex);
            }
        }

        // Need an even number of values >= 4 (at least 2 x + 2 y)
        if (values.size() < 4 || values.size() % 2 != 0) {
            return Optional.empty();
        }

        int half = values.size() / 2;
        double[] xValues = new double[half];
        double[] yValues = new double[half];
        for (int i = 0; i < half; i++) {
            xValues[i] = values.get(i);
            yValues[i] = values.get(half + i);
        }
        return Optional.of(new double[][]{xValues, yValues});
    }

    private static final Pattern SUBSCRIPT_BRACKET_PATTERN = Pattern.compile(
            "([a-zA-Z_][a-zA-Z0-9_]*)\\[([^\\]]+)\\]");

    /**
     * Translates subscript bracket notation to flattened names.
     * Converts {@code name[label]} to {@code name_label} and
     * {@code name[label1,label2]} to {@code name_label1_label2}.
     */
    private static String translateSubscriptBrackets(String expr) {
        Matcher m = SUBSCRIPT_BRACKET_PATTERN.matcher(expr);
        StringBuilder sb = new StringBuilder();
        while (m.find()) {
            String varName = m.group(1);
            String rawSubscript = m.group(2);
            String subscript;
            if (rawSubscript.contains(",")) {
                StringBuilder subSb = new StringBuilder();
                String[] parts = rawSubscript.split(",");
                for (int j = 0; j < parts.length; j++) {
                    if (j > 0) {
                        subSb.append("_");
                    }
                    subSb.append(normalizeName(parts[j].strip()));
                }
                subscript = subSb.toString();
            } else {
                subscript = normalizeName(rawSubscript);
            }
            m.appendReplacement(sb, Matcher.quoteReplacement(varName + "_" + subscript));
        }
        m.appendTail(sb);
        return sb.toString();
    }

    /**
     * Strips GAME(expr) wrappers, replacing with just the inner expression.
     * In Vensim, GAME() allows interactive override during game mode;
     * during normal simulation it simply returns its argument.
     */
    private static String translateGame(String expr) {
        Matcher m = GAME_PATTERN.matcher(expr);
        while (m.find()) {
            int openParen = m.end() - 1;
            int closeParen = findMatchingParen(expr, openParen);
            if (closeParen > 0) {
                String inner = expr.substring(openParen + 1, closeParen).strip();
                expr = expr.substring(0, m.start()) + inner + expr.substring(closeParen + 1);
                m = GAME_PATTERN.matcher(expr);
            } else {
                break;
            }
        }
        return expr;
    }

    /**
     * Strips ACTIVE INITIAL(expr, initial) wrappers, returning the first argument.
     * In Vensim, ACTIVE INITIAL returns the first argument during normal simulation
     * and uses the second argument only during game-mode initialization.
     * Since Courant doesn't support game mode, we pass through the first argument.
     */
    private static String translateActiveInitial(String expr) {
        Matcher m = ACTIVE_INITIAL_PATTERN.matcher(expr);
        while (m.find()) {
            int openParen = m.end() - 1;
            int closeParen = findMatchingParen(expr, openParen);
            if (closeParen > 0) {
                String argsContent = expr.substring(openParen + 1, closeParen);
                List<String> args = splitTopLevelArgs(argsContent);
                // First argument is the active expression
                String firstArg = args.get(0).strip();
                expr = expr.substring(0, m.start()) + firstArg + expr.substring(closeParen + 1);
                m = ACTIVE_INITIAL_PATTERN.matcher(expr);
            } else {
                break;
            }
        }
        return expr;
    }

    /**
     * Strips MESSAGE(args) wrappers, replacing with 0.
     * In Vensim, MESSAGE() displays an interactive message during simulation;
     * it has no effect outside the Vensim UI.
     */
    private static String translateMessage(String expr) {
        Matcher m = MESSAGE_PATTERN.matcher(expr);
        while (m.find()) {
            int openParen = m.end() - 1;
            int closeParen = findMatchingParen(expr, openParen);
            if (closeParen > 0) {
                expr = expr.substring(0, m.start()) + "0" + expr.substring(closeParen + 1);
                m = MESSAGE_PATTERN.matcher(expr);
            } else {
                break;
            }
        }
        return expr;
    }

    /**
     * Strips SIMULTANEOUS(args) wrappers, replacing with 0.
     * In Vensim, SIMULTANEOUS() is a solver hint for simultaneous equation solving;
     * Courant uses Euler integration which doesn't need this hint.
     */
    private static String translateSimultaneous(String expr) {
        Matcher m = SIMULTANEOUS_PATTERN.matcher(expr);
        while (m.find()) {
            int openParen = m.end() - 1;
            int closeParen = findMatchingParen(expr, openParen);
            if (closeParen > 0) {
                expr = expr.substring(0, m.start()) + "0" + expr.substring(closeParen + 1);
                m = SIMULTANEOUS_PATTERN.matcher(expr);
            } else {
                break;
            }
        }
        return expr;
    }

    /**
     * Translates DELAY MATERIAL(input, delay, init, transit) to DELAY_FIXED(input, delay, init).
     * DELAY MATERIAL is a pipeline delay; dropping the 4th transit argument gives equivalent
     * behavior for Euler integration.
     */
    private static String translateDelayMaterial(String expr) {
        Matcher m = DELAY_MATERIAL_PATTERN.matcher(expr);
        while (m.find()) {
            int openParen = m.end() - 1;
            int closeParen = findMatchingParen(expr, openParen);
            if (closeParen > 0) {
                String argsContent = expr.substring(openParen + 1, closeParen);
                List<String> args = splitTopLevelArgs(argsContent);
                // Take first 3 args (input, delay, init), drop 4th (transit)
                String replacement;
                if (args.size() >= 3) {
                    replacement = "DELAY_FIXED(" + args.get(0).strip()
                            + ", " + args.get(1).strip()
                            + ", " + args.get(2).strip() + ")";
                } else {
                    // Fallback: just rename the function
                    replacement = "DELAY_FIXED(" + argsContent + ")";
                }
                expr = expr.substring(0, m.start()) + replacement
                        + expr.substring(closeParen + 1);
                m = DELAY_MATERIAL_PATTERN.matcher(expr);
            } else {
                break;
            }
        }
        return expr;
    }

    /**
     * Replaces a GET function call with {@code 0} and emits a warning containing
     * the referenced file path (extracted from the first argument).
     */
    static String translateGetFunction(String expr, Pattern pattern, String funcName,
                                       List<String> warnings) {
        Matcher m = pattern.matcher(expr);
        while (m.find()) {
            int openParen = m.end() - 1;
            int closeParen = findMatchingParen(expr, openParen);
            if (closeParen > 0) {
                String argsStr = expr.substring(openParen + 1, closeParen);
                String filePath = extractFirstArgument(argsStr);
                String warning = "Variable references external file via " + funcName;
                if (filePath != null && !filePath.isEmpty()) {
                    warning += " ('" + filePath + "')";
                }
                warning += " — substituted with 0";
                warnings.add(warning);
                expr = expr.substring(0, m.start()) + "0" + expr.substring(closeParen + 1);
                m = pattern.matcher(expr);
            } else {
                break;
            }
        }
        return expr;
    }

    /**
     * Extracts the first comma-delimited argument from a function argument string,
     * stripping surrounding quotes and whitespace. Respects nested parentheses.
     */
    static String extractFirstArgument(String argsStr) {
        if (argsStr == null || argsStr.isBlank()) {
            return null;
        }
        int depth = 0;
        for (int i = 0; i < argsStr.length(); i++) {
            char c = argsStr.charAt(i);
            if (c == '(') {
                depth++;
            } else if (c == ')') {
                depth--;
            } else if (c == ',' && depth == 0) {
                String arg = argsStr.substring(0, i).strip();
                // Strip surrounding quotes
                if (arg.length() >= 2
                        && ((arg.startsWith("'") && arg.endsWith("'"))
                        || (arg.startsWith("\"") && arg.endsWith("\"")))) {
                    arg = arg.substring(1, arg.length() - 1);
                }
                return arg;
            }
        }
        // No comma found — entire string is the first arg
        String arg = argsStr.strip();
        if (arg.length() >= 2
                && ((arg.startsWith("'") && arg.endsWith("'"))
                || (arg.startsWith("\"") && arg.endsWith("\"")))) {
            arg = arg.substring(1, arg.length() - 1);
        }
        return arg;
    }

    /**
     * Expands SUM(expr[dim!]) and VMIN(expr[dim!]) vector functions.
     * SUM expands to (val1 + val2 + ...), VMIN expands to MIN(val1, val2, ...).
     */
    private static String expandVectorFunctions(String expr,
                                                 Map<String, List<String>> subscriptDimensions,
                                                 List<String> warnings) {
        expr = expandSingleVectorFunction(expr, SUM_FUNC_PATTERN, " + ",
                subscriptDimensions, warnings);
        expr = expandSingleVectorFunction(expr, VMIN_FUNC_PATTERN, null,
                subscriptDimensions, warnings);
        return expr;
    }

    private static String expandSingleVectorFunction(String expr, Pattern funcPattern,
                                                      String joinOp,
                                                      Map<String, List<String>> subscriptDimensions,
                                                      List<String> warnings) {
        Matcher m = funcPattern.matcher(expr);
        while (m.find()) {
            int openParen = m.end() - 1;
            int closeParen = findMatchingParen(expr, openParen);
            if (closeParen < 0) {
                break;
            }

            String innerExpr = expr.substring(openParen + 1, closeParen).strip();

            // Find dimension marked with !
            Matcher bangMatcher = BANG_DIM_PATTERN.matcher(innerExpr);
            String dimName = null;
            List<String> labels = null;
            while (bangMatcher.find()) {
                String candidate = bangMatcher.group(1);
                List<String> candidateLabels = subscriptDimensions.get(candidate);
                if (candidateLabels != null) {
                    dimName = candidate;
                    labels = candidateLabels;
                    break;
                }
            }

            if (dimName == null || labels.isEmpty()) {
                break;
            }

            List<String> expanded = new ArrayList<>();
            for (String label : labels) {
                expanded.add(innerExpr.replace(dimName + "!", label));
            }

            String replacement;
            if (joinOp != null) {
                replacement = "(" + String.join(joinOp, expanded) + ")";
            } else {
                // Nest MIN calls: MIN(a, MIN(b, c)) for 3+ elements
                replacement = expanded.getLast();
                for (int ei = expanded.size() - 2; ei >= 0; ei--) {
                    replacement = "MIN(" + expanded.get(ei) + ", " + replacement + ")";
                }
            }

            expr = expr.substring(0, m.start()) + replacement
                    + expr.substring(closeParen + 1);
            m = funcPattern.matcher(expr);
        }
        return expr;
    }

    private static void checkUnsupportedFunctions(String expr, List<String> warnings) {
        String upper = expr.toUpperCase();
        for (String func : UNSUPPORTED_FUNCTIONS) {
            if (upper.contains(func.toUpperCase())) {
                // Verify it's a function call: name must be followed by whitespace+paren or direct paren
                Pattern p = Pattern.compile(
                        "(?i)\\b" + Pattern.quote(func) + "\\s*\\(");
                if (p.matcher(expr).find()) {
                    warnings.add("Unsupported Vensim function: " + func);
                }
            }
        }
    }
}
