package com.deathrayresearch.forrester.io.xmile;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Bidirectional expression translator between XMILE and Forrester syntax.
 *
 * <p>XMILE expressions use different function names and operators than Forrester.
 * This class provides {@link #toForrester(String)} and {@link #toXmile(String)}
 * for converting expressions in both directions.
 */
public final class XmileExprTranslator {

    // --- XMILE → Forrester patterns ---
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
    private static final Pattern TIME_XMILE_PATTERN = Pattern.compile(
            "(?i)\\bTime\\b");

    // --- Forrester → XMILE patterns ---
    private static final Pattern IF_FUNC_PATTERN = Pattern.compile(
            "(?i)\\bIF\\s*\\(");
    private static final Pattern AND_OP_PATTERN = Pattern.compile("&&");
    private static final Pattern OR_OP_PATTERN = Pattern.compile("\\|\\|");
    private static final Pattern DOUBLE_EQ_PATTERN = Pattern.compile("==");
    private static final Pattern NOT_EQ_PATTERN = Pattern.compile("!=");
    private static final Pattern TIME_FORRESTER_PATTERN = Pattern.compile(
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
     * Translates an XMILE expression to Forrester syntax.
     *
     * @param xmileExpr the XMILE expression
     * @return the translation result
     */
    public static TranslationResult toForrester(String xmileExpr) {
        if (xmileExpr == null || xmileExpr.isBlank()) {
            return new TranslationResult(xmileExpr, List.of());
        }

        List<String> warnings = new ArrayList<>();
        String expr = xmileExpr.strip();

        // 1. IF_THEN_ELSE(...) or IF THEN ELSE(...) → IF(...)
        expr = IF_THEN_ELSE_PATTERN.matcher(expr).replaceAll("IF(");

        // 2. Logical operators: AND → &&, OR → ||, NOT → !
        expr = AND_KEYWORD_PATTERN.matcher(expr).replaceAll("&&");
        expr = OR_KEYWORD_PATTERN.matcher(expr).replaceAll("||");
        expr = translateNotKeyword(expr);

        // 3. Comparison operators: <> → !=, = → == (single = only)
        expr = INEQUALITY_PATTERN.matcher(expr).replaceAll("!=");
        expr = EQUALITY_SINGLE_PATTERN.matcher(expr).replaceAll("==");

        // 4. SMTH3/SMTH1 → SMOOTH (with warning)
        if (SMTH3_PATTERN.matcher(expr).find()) {
            expr = SMTH3_PATTERN.matcher(expr).replaceAll("SMOOTH(");
            warnings.add("SMTH3 approximated as SMOOTH");
        }
        if (SMTH1_PATTERN.matcher(expr).find()) {
            expr = SMTH1_PATTERN.matcher(expr).replaceAll("SMOOTH(");
            warnings.add("SMTH1 approximated as SMOOTH");
        }

        // 5. Time → TIME (the built-in variable)
        expr = TIME_XMILE_PATTERN.matcher(expr).replaceAll("TIME");

        return new TranslationResult(expr, warnings);
    }

    /**
     * Translates a Forrester expression to XMILE syntax.
     *
     * @param forresterExpr the Forrester expression
     * @return the translated XMILE expression
     */
    public static String toXmile(String forresterExpr) {
        if (forresterExpr == null || forresterExpr.isBlank()) {
            return forresterExpr;
        }

        String expr = forresterExpr.strip();

        // 1. IF(...) → IF_THEN_ELSE(...)
        expr = IF_FUNC_PATTERN.matcher(expr).replaceAll("IF_THEN_ELSE(");

        // 2. Logical operators: && → AND, || → OR, ! → NOT
        expr = AND_OP_PATTERN.matcher(expr).replaceAll(" AND ");
        expr = OR_OP_PATTERN.matcher(expr).replaceAll(" OR ");
        expr = translateNotOperator(expr);

        // 3. Comparison operators: == → =, != → <>
        expr = NOT_EQ_PATTERN.matcher(expr).replaceAll("<>");
        expr = DOUBLE_EQ_PATTERN.matcher(expr).replaceAll("=");

        // 4. TIME → Time
        expr = TIME_FORRESTER_PATTERN.matcher(expr).replaceAll("Time");

        return expr;
    }

    /**
     * Translates the XMILE NOT keyword to Forrester ! operator.
     * NOT is followed by its operand — we wrap it appropriately.
     */
    private static String translateNotKeyword(String expr) {
        Matcher m = NOT_KEYWORD_PATTERN.matcher(expr);
        StringBuilder sb = new StringBuilder();
        while (m.find()) {
            // Check we're not inside another word (word boundary already handled by \b)
            m.appendReplacement(sb, "!");
        }
        m.appendTail(sb);
        return sb.toString();
    }

    /**
     * Translates the Forrester ! operator to XMILE NOT keyword.
     * We replace standalone ! (not part of !=) with NOT.
     */
    private static String translateNotOperator(String expr) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < expr.length(); i++) {
            if (expr.charAt(i) == '!' && (i + 1 >= expr.length() || expr.charAt(i + 1) != '=')) {
                sb.append("NOT ");
            } else {
                sb.append(expr.charAt(i));
            }
        }
        return sb.toString();
    }
}
