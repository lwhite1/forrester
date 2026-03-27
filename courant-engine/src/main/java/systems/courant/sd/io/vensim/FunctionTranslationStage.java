package systems.courant.sd.io.vensim;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Pipeline stage that translates Vensim function calls to Courant equivalents.
 *
 * <p>Handles division-by-zero functions (XIDZ, ZIDZ), delay variants, smooth variants,
 * random functions, pass-through functions (GAME, MESSAGE, SIMULTANEOUS, ACTIVE INITIAL),
 * multi-word function renaming (PULSE TRAIN, SAMPLE IF TRUE, etc.), GET XLS/DIRECT
 * functions, and unsupported function detection.
 */
final class FunctionTranslationStage implements ExprTransformationStage {

    private static final Pattern XIDZ_PATTERN = Pattern.compile("(?i)XIDZ\\s*\\(");
    private static final Pattern ZIDZ_PATTERN = Pattern.compile("(?i)ZIDZ\\s*\\(");
    private static final Pattern SMOOTH3_PATTERN = Pattern.compile("(?i)SMOOTH3\\s*\\(");
    private static final Pattern SMOOTHI_PATTERN = Pattern.compile("(?i)SMOOTHI\\s*\\(");
    private static final Pattern SMOOTH3I_PATTERN = Pattern.compile("(?i)SMOOTH3I\\s*\\(");
    private static final Pattern DELAY1_PATTERN = Pattern.compile("(?i)DELAY1\\s*\\(");
    private static final Pattern DELAY1I_PATTERN = Pattern.compile("(?i)DELAY1I\\s*\\(");
    private static final Pattern DELAY_FIXED_PATTERN = Pattern.compile(
            "(?i)DELAY\\s+FIXED\\s*\\(");
    private static final Pattern DELAY_MATERIAL_PATTERN = Pattern.compile(
            "(?i)DELAY\\s+MATERIAL\\s*\\(");
    private static final Pattern GAME_PATTERN = Pattern.compile("(?i)GAME\\s*\\(");
    private static final Pattern RANDOM_NORMAL_PATTERN = Pattern.compile(
            "(?i)RANDOM\\s+NORMAL\\s*\\(");
    private static final Pattern RANDOM_UNIFORM_PATTERN = Pattern.compile(
            "(?i)RANDOM\\s+UNIFORM\\s*\\(");
    private static final Pattern RANDOM_0_1_PATTERN = Pattern.compile(
            "(?i)RANDOM\\s+0\\s+1\\s*\\(\\s*\\)");
    private static final Pattern PULSE_TRAIN_PATTERN = Pattern.compile(
            "(?i)PULSE\\s+TRAIN\\s*\\(");
    private static final Pattern MESSAGE_PATTERN = Pattern.compile("(?i)MESSAGE\\s*\\(");
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

    private static final Set<String> UNSUPPORTED_FUNCTIONS = Set.of(
            "DELAY N", "TABBED ARRAY",
            "VECTOR SELECT", "VECTOR ELM MAP", "VECTOR SORT ORDER",
            "ALLOCATE AVAILABLE");
    private static final List<Pattern> UNSUPPORTED_FUNCTION_PATTERNS;
    static {
        List<Pattern> patterns = new ArrayList<>();
        for (String func : UNSUPPORTED_FUNCTIONS) {
            patterns.add(Pattern.compile(
                    "(?i)\\b" + Pattern.quote(func) + "\\s*\\("));
        }
        UNSUPPORTED_FUNCTION_PATTERNS = List.copyOf(patterns);
    }

    @Override
    public void apply(TranslationContext ctx) {
        String expr = ctx.expression();
        List<String> warnings = ctx.warnings();

        // XIDZ and ZIDZ
        expr = translateXidz(expr, warnings);
        expr = translateZidz(expr, warnings);

        // SMOOTH variants → native engine functions
        expr = SMOOTH3I_PATTERN.matcher(expr).replaceAll("SMOOTH3I(");
        expr = SMOOTH3_PATTERN.matcher(expr).replaceAll("SMOOTH3(");
        expr = SMOOTHI_PATTERN.matcher(expr).replaceAll("SMOOTHI(");

        // DELAY1/DELAY1I → native engine functions
        expr = DELAY1I_PATTERN.matcher(expr).replaceAll("DELAY1I(");
        expr = DELAY1_PATTERN.matcher(expr).replaceAll("DELAY1(");

        // DELAY FIXED → DELAY_FIXED
        expr = DELAY_FIXED_PATTERN.matcher(expr).replaceAll("DELAY_FIXED(");

        // DELAY MATERIAL → DELAY_FIXED (drop 4th transit arg)
        expr = translateDelayMaterial(expr);

        // RANDOM 0 1() → RANDOM_UNIFORM(0, 1, 0)
        expr = RANDOM_0_1_PATTERN.matcher(expr).replaceAll("RANDOM_UNIFORM(0, 1, 0)");

        // GAME(expr) → expr (pass-through)
        expr = translateGame(expr);

        // MESSAGE(args) → 0
        expr = translateMessage(expr);

        // SIMULTANEOUS(args) → 0
        expr = translateSimultaneous(expr);

        // ACTIVE INITIAL(expr, initial) → expr
        expr = translateActiveInitial(expr);

        // Multi-word function name normalization
        expr = RANDOM_UNIFORM_PATTERN.matcher(expr).replaceAll("RANDOM_UNIFORM(");
        expr = RANDOM_NORMAL_PATTERN.matcher(expr).replaceAll("RANDOM_NORMAL(");
        expr = SAMPLE_IF_TRUE_PATTERN.matcher(expr).replaceAll("SAMPLE_IF_TRUE(");
        expr = FIND_ZERO_PATTERN.matcher(expr).replaceAll("FIND_ZERO(");
        expr = PULSE_TRAIN_PATTERN.matcher(expr).replaceAll("PULSE_TRAIN(");
        expr = LOOKUP_AREA_PATTERN.matcher(expr).replaceAll("LOOKUP_AREA(");

        // GET XLS/DIRECT functions → 0 placeholder with warning
        expr = translateGetFunction(expr, GET_XLS_DATA_PATTERN, "GET XLS DATA", warnings);
        expr = translateGetFunction(expr, GET_DIRECT_DATA_PATTERN, "GET DIRECT DATA", warnings);
        expr = translateGetFunction(expr, GET_XLS_CONSTANTS_PATTERN, "GET XLS CONSTANTS", warnings);
        expr = translateGetFunction(expr, GET_DIRECT_CONSTANTS_PATTERN,
                "GET DIRECT CONSTANTS", warnings);
        expr = translateGetFunction(expr, GET_XLS_LOOKUPS_PATTERN, "GET XLS LOOKUPS", warnings);
        expr = translateGetFunction(expr, GET_DIRECT_LOOKUPS_PATTERN,
                "GET DIRECT LOOKUPS", warnings);

        // Check for unsupported functions
        checkUnsupportedFunctions(expr, warnings);

        ctx.setExpression(expr);
    }

    private static String translateXidz(String expr, List<String> warnings) {
        while (true) {
            Matcher m = XIDZ_PATTERN.matcher(expr);
            if (!m.find()) {
                break;
            }
            int funcStart = m.start();
            int argsStart = m.end();
            int closeParen = ExprParsingUtils.findMatchingParen(expr, argsStart - 1);
            if (closeParen < 0) {
                warnings.add("Malformed XIDZ expression");
                break;
            }
            String argsContent = expr.substring(argsStart, closeParen);
            List<String> args = ExprParsingUtils.splitTopLevelArgs(argsContent);
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
            int closeParen = ExprParsingUtils.findMatchingParen(expr, argsStart - 1);
            if (closeParen < 0) {
                warnings.add("Malformed ZIDZ expression");
                break;
            }
            String argsContent = expr.substring(argsStart, closeParen);
            List<String> args = ExprParsingUtils.splitTopLevelArgs(argsContent);
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

    private static String translateGame(String expr) {
        Matcher m = GAME_PATTERN.matcher(expr);
        while (m.find()) {
            int openParen = m.end() - 1;
            int closeParen = ExprParsingUtils.findMatchingParen(expr, openParen);
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

    private static String translateActiveInitial(String expr) {
        Matcher m = ACTIVE_INITIAL_PATTERN.matcher(expr);
        while (m.find()) {
            int openParen = m.end() - 1;
            int closeParen = ExprParsingUtils.findMatchingParen(expr, openParen);
            if (closeParen > 0) {
                String argsContent = expr.substring(openParen + 1, closeParen);
                List<String> args = ExprParsingUtils.splitTopLevelArgs(argsContent);
                String firstArg = args.get(0).strip();
                expr = expr.substring(0, m.start()) + firstArg + expr.substring(closeParen + 1);
                m = ACTIVE_INITIAL_PATTERN.matcher(expr);
            } else {
                break;
            }
        }
        return expr;
    }

    private static String translateMessage(String expr) {
        Matcher m = MESSAGE_PATTERN.matcher(expr);
        while (m.find()) {
            int openParen = m.end() - 1;
            int closeParen = ExprParsingUtils.findMatchingParen(expr, openParen);
            if (closeParen > 0) {
                expr = expr.substring(0, m.start()) + "0" + expr.substring(closeParen + 1);
                m = MESSAGE_PATTERN.matcher(expr);
            } else {
                break;
            }
        }
        return expr;
    }

    private static String translateSimultaneous(String expr) {
        Matcher m = SIMULTANEOUS_PATTERN.matcher(expr);
        while (m.find()) {
            int openParen = m.end() - 1;
            int closeParen = ExprParsingUtils.findMatchingParen(expr, openParen);
            if (closeParen > 0) {
                expr = expr.substring(0, m.start()) + "0" + expr.substring(closeParen + 1);
                m = SIMULTANEOUS_PATTERN.matcher(expr);
            } else {
                break;
            }
        }
        return expr;
    }

    private static String translateDelayMaterial(String expr) {
        Matcher m = DELAY_MATERIAL_PATTERN.matcher(expr);
        while (m.find()) {
            int openParen = m.end() - 1;
            int closeParen = ExprParsingUtils.findMatchingParen(expr, openParen);
            if (closeParen > 0) {
                String argsContent = expr.substring(openParen + 1, closeParen);
                List<String> args = ExprParsingUtils.splitTopLevelArgs(argsContent);
                String replacement;
                if (args.size() >= 3) {
                    replacement = "DELAY_FIXED(" + args.get(0).strip()
                            + ", " + args.get(1).strip()
                            + ", " + args.get(2).strip() + ")";
                } else {
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

    static String translateGetFunction(String expr, Pattern pattern, String funcName,
                                       List<String> warnings) {
        Matcher m = pattern.matcher(expr);
        while (m.find()) {
            int openParen = m.end() - 1;
            int closeParen = ExprParsingUtils.findMatchingParen(expr, openParen);
            if (closeParen > 0) {
                String argsStr = expr.substring(openParen + 1, closeParen);
                String filePath = ExprParsingUtils.extractFirstArgument(argsStr);
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

    private static void checkUnsupportedFunctions(String expr, List<String> warnings) {
        for (Pattern p : UNSUPPORTED_FUNCTION_PATTERNS) {
            Matcher m = p.matcher(expr);
            if (m.find()) {
                String matched = m.group().strip();
                int parenIdx = matched.indexOf('(');
                String funcName = (parenIdx > 0)
                        ? matched.substring(0, parenIdx).strip()
                        : matched;
                warnings.add("Unsupported Vensim function: " + funcName);
            }
        }
    }
}
