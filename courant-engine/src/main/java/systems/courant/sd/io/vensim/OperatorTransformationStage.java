package systems.courant.sd.io.vensim;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Pipeline stage that transforms Vensim operators to Courant syntax.
 *
 * <p>Handles:
 * <ul>
 *   <li>{@code IF THEN ELSE(} → {@code IF(}</li>
 *   <li>{@code :AND:} → {@code and}, {@code :OR:} → {@code or}, {@code :NOT:} → {@code not()}</li>
 *   <li>{@code <>} → {@code !=}</li>
 *   <li>{@code ^} → {@code **} (power)</li>
 * </ul>
 */
final class OperatorTransformationStage implements ExprTransformationStage {

    private static final Pattern IF_THEN_ELSE_PATTERN = Pattern.compile(
            "(?i)IF\\s+THEN\\s+ELSE\\s*\\(");
    private static final Pattern AND_PATTERN = Pattern.compile("(?i):AND:");
    private static final Pattern OR_PATTERN = Pattern.compile("(?i):OR:");
    private static final Pattern NOT_PATTERN = Pattern.compile("(?i):NOT:");
    private static final Pattern NOT_EQUAL_PATTERN = Pattern.compile("<>");
    private static final Pattern CARET_PATTERN = Pattern.compile("\\^");

    @Override
    public void apply(TranslationContext ctx) {
        String expr = ctx.expression();

        // IF THEN ELSE → IF
        expr = IF_THEN_ELSE_PATTERN.matcher(expr).replaceAll("IF(");

        // Logical operators
        expr = AND_PATTERN.matcher(expr).replaceAll(" and ");
        expr = OR_PATTERN.matcher(expr).replaceAll(" or ");
        expr = translateNot(expr);

        // Not-equal operator: <> → !=
        expr = NOT_EQUAL_PATTERN.matcher(expr).replaceAll("!=");

        // Power operator: ^ → **
        expr = CARET_PATTERN.matcher(expr).replaceAll("**");

        ctx.setExpression(expr);
    }

    private static String translateNot(String expr) {
        Matcher m = NOT_PATTERN.matcher(expr);
        while (m.find()) {
            int notStart = m.start();
            int operandStart = m.end();
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
                            || ahead.matches("(?i)or[^a-zA-Z0-9_].*")
                            || ahead.matches("(?i)or$")) {
                        end = i;
                        break;
                    }
                }
            }
            String operand = expr.substring(operandStart, end).strip();
            if (operand.isEmpty()) {
                break;
            }
            String replacement = "not(" + operand + ")";
            expr = expr.substring(0, notStart) + replacement + expr.substring(end);
            m = NOT_PATTERN.matcher(expr);
        }
        return expr;
    }
}
