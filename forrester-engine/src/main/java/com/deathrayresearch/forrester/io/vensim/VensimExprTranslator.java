package com.deathrayresearch.forrester.io.vensim;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Translates Vensim expression syntax to Forrester expression syntax.
 *
 * <p>Performs text-level transformations including multi-word name replacement,
 * function name mapping, logical operator conversion, and lookup extraction.
 */
public final class VensimExprTranslator {

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
    private static final Pattern CARET_PATTERN = Pattern.compile("\\^");
    private static final Pattern TIME_VAR_PATTERN = Pattern.compile(
            "(?i)\\bTime\\b");
    private static final Set<String> UNSUPPORTED_FUNCTIONS = Set.of(
            "PULSE", "PULSE TRAIN", "GAME", "DELAY N",
            "GET XLS DATA", "GET DIRECT DATA",
            "GET DIRECT CONSTANTS", "TABBED ARRAY", "SAMPLE IF TRUE",
            "VECTOR SELECT", "VECTOR ELM MAP", "VECTOR SORT ORDER",
            "ALLOCATE AVAILABLE", "FIND ZERO");

    /**
     * Result of translating a Vensim expression.
     *
     * @param expression the translated Forrester expression
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
     * Translates a Vensim expression to Forrester syntax.
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
     * Translates a Vensim expression to Forrester syntax.
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
        if (vensimExpr == null || vensimExpr.isBlank()) {
            return new TranslationResult(vensimExpr, List.of(), List.of());
        }

        List<String> warnings = new ArrayList<>();
        List<ExtractedLookup> lookups = new ArrayList<>();
        String expr = vensimExpr.strip();

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

        // 5. XIDZ and ZIDZ
        expr = translateXidz(expr, warnings);
        expr = translateZidz(expr, warnings);

        // 6. SMOOTH3 → SMOOTH (with warning)
        if (SMOOTH3_PATTERN.matcher(expr).find()) {
            expr = SMOOTH3_PATTERN.matcher(expr).replaceAll("SMOOTH(");
            warnings.add("SMOOTH3 approximated as SMOOTH");
        }
        if (SMOOTHI_PATTERN.matcher(expr).find()) {
            expr = SMOOTHI_PATTERN.matcher(expr).replaceAll("SMOOTH(");
            warnings.add("SMOOTHI approximated as SMOOTH (initial value semantics differ)");
        }
        if (SMOOTH3I_PATTERN.matcher(expr).find()) {
            expr = SMOOTH3I_PATTERN.matcher(expr).replaceAll("SMOOTH(");
            warnings.add("SMOOTH3I approximated as SMOOTH (third-order + initial value semantics differ)");
        }

        // 7. DELAY1 → DELAY3 (with warning)
        if (DELAY1_PATTERN.matcher(expr).find()) {
            expr = DELAY1_PATTERN.matcher(expr).replaceAll("DELAY3(");
            warnings.add("DELAY1 approximated as DELAY3");
        }
        if (DELAY1I_PATTERN.matcher(expr).find()) {
            expr = DELAY1I_PATTERN.matcher(expr).replaceAll("DELAY3(");
            warnings.add("DELAY1I approximated as DELAY3 (first-order + initial value semantics differ)");
        }

        // 8. DELAY FIXED → DELAY_FIXED
        expr = DELAY_FIXED_PATTERN.matcher(expr).replaceAll("DELAY_FIXED(");

        // 9. ^ → ** (Vensim uses ^ for power, Forrester uses **)
        expr = CARET_PATTERN.matcher(expr).replaceAll("**");

        // 10. Time → TIME (the built-in variable)
        expr = TIME_VAR_PATTERN.matcher(expr).replaceAll("TIME");

        // 11. Rewrite lookupName(arg) → LOOKUP(lookupName, arg)
        expr = rewriteLookupCalls(expr, lookupNames);

        // 12. Check for unsupported functions
        checkUnsupportedFunctions(expr, warnings);

        return new TranslationResult(expr, lookups, warnings);
    }

    /**
     * Normalizes a Vensim variable name to Forrester identifier format.
     * Converts spaces to underscores, strips quotes, and trims whitespace.
     *
     * @param vensimName the Vensim variable name
     * @return the normalized name
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
        double[][] points = parseLookupPoints(lookupData);
        if (points == null || points[0].length < 2) {
            warnings.add("Could not parse lookup data points in WITH LOOKUP");
            return expr;
        }

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
                .sorted(Comparator.comparingInt(String::length).reversed())
                .toList();

        for (String name : sortedNames) {
            String normalized = normalizeName(name);
            // Use word-boundary-aware replacement
            // Vensim names with spaces: match the literal multi-word name
            String escaped = Pattern.quote(name);
            Pattern p = Pattern.compile("(?<![a-zA-Z0-9_])" + escaped + "(?![a-zA-Z0-9_])",
                    Pattern.CASE_INSENSITIVE);
            expr = p.matcher(expr).replaceAll(normalized);
        }
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
            if (m.find()) {
                int funcStart = m.start();
                int argsStart = m.end();
                int closeParen = findMatchingParen(expr, argsStart - 1);
                if (closeParen > 0) {
                    String arg = expr.substring(argsStart, closeParen).strip();
                    String replacement = "LOOKUP(" + name + ", " + arg + ")";
                    expr = expr.substring(0, funcStart) + replacement
                            + expr.substring(closeParen + 1);
                }
            }
        }
        return expr;
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

    static double[][] parseLookupPoints(String data) {
        // Lookup data format: [(xmin,ymin)-(xmax,ymax)],(x1,y1),(x2,y2),...
        // or ([(xmin,ymin)-(xmax,ymax)],(x1,y1),(x2,y2),...)  (WITH LOOKUP wrapping)
        // or just (x1,y1),(x2,y2),...
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

        List<double[]> points = new ArrayList<>();
        Pattern pairPattern = Pattern.compile(
                "\\(\\s*(-?[\\d.eE+\\-]+)\\s*,\\s*(-?[\\d.eE+\\-]+)\\s*\\)");
        Matcher m = pairPattern.matcher(cleaned);
        while (m.find()) {
            try {
                double x = Double.parseDouble(m.group(1));
                double y = Double.parseDouble(m.group(2));
                points.add(new double[]{x, y});
            } catch (NumberFormatException e) {
                // Skip malformed pairs
            }
        }

        if (points.size() < 2) {
            return null;
        }

        double[] xValues = new double[points.size()];
        double[] yValues = new double[points.size()];
        for (int i = 0; i < points.size(); i++) {
            xValues[i] = points.get(i)[0];
            yValues[i] = points.get(i)[1];
        }
        return new double[][]{xValues, yValues};
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
