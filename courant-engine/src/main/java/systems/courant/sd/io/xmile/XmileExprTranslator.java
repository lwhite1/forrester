package systems.courant.sd.io.xmile;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Bidirectional expression translator between XMILE and Courant syntax.
 *
 * <p>XMILE expressions use different function names and operators than Courant.
 * This class provides {@link #toCourant(String)} and {@link #toXmile(String)}
 * for converting expressions in both directions.
 */
public final class XmileExprTranslator {

    // --- XMILE → Courant patterns ---
    private static final Pattern IF_THEN_ELSE_PATTERN = Pattern.compile(
            "(?i)IF_THEN_ELSE\\s*\\(|IF\\s+THEN\\s+ELSE\\s*\\(");
    private static final Pattern AND_KEYWORD_PATTERN = Pattern.compile(
            "(?i)\\bAND\\b");
    private static final Pattern OR_KEYWORD_PATTERN = Pattern.compile(
            "(?i)\\bOR\\b");
    private static final Pattern NOT_KEYWORD_PATTERN = Pattern.compile(
            "(?i)\\bNOT\\b");
    private static final Pattern INEQUALITY_PATTERN = Pattern.compile("<>");
    private static final Pattern EQUALITY_SINGLE_PATTERN = Pattern.compile(
            "(?<![<>!=])=(?!=)");
    private static final Pattern SMTH3_PATTERN = Pattern.compile(
            "(?i)\\bSMTH3\\s*\\(");
    private static final Pattern SMTH1_PATTERN = Pattern.compile(
            "(?i)\\bSMTH1\\s*\\(");
    private static final Pattern CARET_PATTERN = Pattern.compile("\\^");
    private static final Pattern TIME_XMILE_PATTERN = Pattern.compile(
            "(?<!\\w\\s)(?i)\\bTime\\b(?!\\s\\w)");

    // --- Unsupported XMILE function patterns (warn) ---
    private static final Pattern SAFEDIV_PATTERN = Pattern.compile(
            "(?i)\\bSAFEDIV\\s*\\(");
    private static final Pattern INIT_FUNC_PATTERN = Pattern.compile(
            "(?i)\\bINIT\\s*\\(");
    private static final Pattern PREVIOUS_PATTERN = Pattern.compile(
            "(?i)\\bPREVIOUS\\s*\\(");
    private static final Pattern HISTORY_PATTERN = Pattern.compile(
            "(?i)\\bHISTORY\\s*\\(");

    // --- Courant → XMILE patterns ---
    private static final Pattern IF_FUNC_PATTERN = Pattern.compile(
            "(?i)\\bIF\\s*\\(");
    private static final Pattern AND_OP_PATTERN = Pattern.compile("(?i)\\band\\b");
    private static final Pattern OR_OP_PATTERN = Pattern.compile("(?i)\\bor\\b");
    private static final Pattern NOT_OP_PATTERN = Pattern.compile("(?i)\\bnot\\b");
    private static final Pattern DOUBLE_STAR_PATTERN = Pattern.compile("\\*\\*");
    private static final Pattern DOUBLE_EQ_PATTERN = Pattern.compile("==");
    private static final Pattern NOT_EQ_PATTERN = Pattern.compile("!=");
    private static final Pattern TIME_SD_PATTERN = Pattern.compile(
            "\\bTIME\\b");

    /**
     * Result of translating an expression.
     *
     * @param expression the translated expression
     * @param warnings any warnings generated during translation
     */
    public record TranslationResult(String expression, List<String> warnings) {
        public TranslationResult {
            warnings = warnings == null ? List.of() : List.copyOf(warnings);
        }
    }

    private XmileExprTranslator() {
    }

    /**
     * Translates an XMILE expression to Courant syntax.
     *
     * @param xmileExpr the XMILE expression
     * @return the translation result
     */
    public static TranslationResult toCourant(String xmileExpr) {
        if (xmileExpr == null || xmileExpr.isBlank()) {
            return new TranslationResult(xmileExpr, List.of());
        }

        List<String> warnings = new ArrayList<>();
        String expr = xmileExpr.strip();

        // 1. IF_THEN_ELSE(...) or IF THEN ELSE(...) → IF(...)
        expr = IF_THEN_ELSE_PATTERN.matcher(expr).replaceAll("IF(");

        // 2. Logical operators: AND → and, OR → or, NOT → not
        expr = AND_KEYWORD_PATTERN.matcher(expr).replaceAll("and");
        expr = OR_KEYWORD_PATTERN.matcher(expr).replaceAll("or");
        expr = translateNotKeyword(expr);

        // 3. ^ → ** (XMILE uses ^ for power, Courant uses **)
        expr = CARET_PATTERN.matcher(expr).replaceAll("**");

        // 4. Comparison operators: <> → !=, = → == (single = only)
        expr = INEQUALITY_PATTERN.matcher(expr).replaceAll("!=");
        expr = EQUALITY_SINGLE_PATTERN.matcher(expr).replaceAll("==");

        // 4. SMTH3 → SMOOTH3, SMTH1 → SMOOTH
        expr = SMTH3_PATTERN.matcher(expr).replaceAll("SMOOTH3(");
        expr = SMTH1_PATTERN.matcher(expr).replaceAll("SMOOTH(");

        // 5. Time → TIME (the built-in variable)
        expr = TIME_XMILE_PATTERN.matcher(expr).replaceAll("TIME");

        // 6. Warn about unsupported XMILE functions (left in equation text)
        if (SAFEDIV_PATTERN.matcher(expr).find()) {
            warnings.add("SAFEDIV function not supported (left in equation as-is)");
        }
        if (INIT_FUNC_PATTERN.matcher(expr).find()) {
            warnings.add("INIT function not supported (left in equation as-is)");
        }
        if (PREVIOUS_PATTERN.matcher(expr).find()) {
            warnings.add("PREVIOUS function not supported (left in equation as-is)");
        }
        if (HISTORY_PATTERN.matcher(expr).find()) {
            warnings.add("HISTORY function not supported (left in equation as-is)");
        }

        return new TranslationResult(expr, warnings);
    }

    /**
     * Translates a Courant expression to XMILE syntax.
     *
     * @param sdExpr the Courant expression
     * @return the translated XMILE expression
     */
    public static String toXmile(String sdExpr) {
        if (sdExpr == null || sdExpr.isBlank()) {
            return sdExpr;
        }

        String expr = sdExpr.strip();

        // 1. IF(...) → IF_THEN_ELSE(...)
        expr = IF_FUNC_PATTERN.matcher(expr).replaceAll("IF_THEN_ELSE(");

        // 2. Logical operators: and → AND, or → OR, not → NOT
        expr = AND_OP_PATTERN.matcher(expr).replaceAll("AND");
        expr = OR_OP_PATTERN.matcher(expr).replaceAll("OR");
        expr = NOT_OP_PATTERN.matcher(expr).replaceAll("NOT");

        // 3. ** → ^ (Courant uses ** for power, XMILE uses ^)
        expr = DOUBLE_STAR_PATTERN.matcher(expr).replaceAll("^");

        // 4. Comparison operators: == → =, != → <>
        expr = NOT_EQ_PATTERN.matcher(expr).replaceAll("<>");
        expr = DOUBLE_EQ_PATTERN.matcher(expr).replaceAll("=");

        // 4. TIME → Time
        expr = TIME_SD_PATTERN.matcher(expr).replaceAll("Time");

        return expr;
    }

    /**
     * Translates the XMILE NOT keyword to Courant "not" keyword.
     * NOT is followed by its operand.
     */
    private static String translateNotKeyword(String expr) {
        Matcher m = NOT_KEYWORD_PATTERN.matcher(expr);
        StringBuilder sb = new StringBuilder();
        while (m.find()) {
            m.appendReplacement(sb, "not");
        }
        m.appendTail(sb);
        return sb.toString();
    }

}
