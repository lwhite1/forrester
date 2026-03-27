package systems.courant.sd.io.vensim;

import systems.courant.sd.io.FormatUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * Shared parsing utilities used by multiple transformation stages.
 */
final class ExprParsingUtils {

    private ExprParsingUtils() {}

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

    static List<String> splitTopLevelArgs(String content) {
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

    static int findTopLevelComma(String content) {
        return FormatUtils.findTopLevelComma(content);
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
                return stripQuotes(argsStr.substring(0, i).strip());
            }
        }
        return stripQuotes(argsStr.strip());
    }

    private static String stripQuotes(String arg) {
        if (arg.length() >= 2
                && ((arg.startsWith("'") && arg.endsWith("'"))
                || (arg.startsWith("\"") && arg.endsWith("\"")))) {
            return arg.substring(1, arg.length() - 1);
        }
        return arg;
    }
}
