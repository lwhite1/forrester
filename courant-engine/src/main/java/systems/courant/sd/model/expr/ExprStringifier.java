package systems.courant.sd.model.expr;

import java.util.Locale;

/**
 * Converts an {@link Expr} AST back into a readable infix string with minimal parentheses.
 *
 * <p>Note: round-tripping is not perfectly structural. A negative literal like
 * {@code Literal(-5)} stringifies to {@code -5} but re-parses as
 * {@code UnaryOp(NEGATE, Literal(5))}. Zero-arg function calls (e.g. {@code TIME})
 * stringify without parentheses and re-parse as {@code Ref}. These are semantically
 * equivalent but not structurally identical.
 */
public final class ExprStringifier {

    /** Lowest possible context precedence — used when no surrounding operator constrains. */
    private static final int MIN_PRECEDENCE = -10;

    /** High precedence context for unary operands — forces parentheses on inner binary ops. */
    private static final int UNARY_CONTEXT_PRECEDENCE = 10;

    private ExprStringifier() {
    }

    /**
     * Converts the given expression AST to a human-readable infix string.
     */
    public static String stringify(Expr expr) {
        StringBuilder sb = new StringBuilder();
        appendExpr(sb, expr, MIN_PRECEDENCE);
        return sb.toString();
    }

    private static void appendExpr(StringBuilder sb, Expr expr, int contextPrecedence) {
        if (expr instanceof Expr.Literal lit) {
            appendLiteral(sb, lit);
        } else if (expr instanceof Expr.Ref ref) {
            appendRef(sb, ref);
        } else if (expr instanceof Expr.BinaryOp bin) {
            appendBinaryOp(sb, bin, contextPrecedence);
        } else if (expr instanceof Expr.UnaryOp un) {
            appendUnaryOp(sb, un);
        } else if (expr instanceof Expr.FunctionCall call) {
            appendFunctionCall(sb, call);
        } else if (expr instanceof Expr.Conditional cond) {
            appendConditional(sb, cond);
        } else {
            throw new IllegalArgumentException("Unknown Expr type: " + expr.getClass());
        }
    }

    private static void appendLiteral(StringBuilder sb, Expr.Literal lit) {
        double v = lit.value();
        if (Double.isNaN(v)) {
            sb.append("NAN");
            return;
        }
        if (Double.isInfinite(v)) {
            sb.append(v > 0 ? "INF" : "(-INF)");
            return;
        }
        if (Math.abs(v) < (1L << 53) && v == (long) v) {
            sb.append((long) v);
        } else {
            sb.append(v);
        }
    }

    private static void appendRef(StringBuilder sb, Expr.Ref ref) {
        String name = ref.name();
        if (needsQuoting(name)) {
            sb.append('`').append(name).append('`');
        } else {
            sb.append(name);
        }
    }

    private static boolean needsQuoting(String name) {
        if (name.isEmpty()) {
            return true;
        }
        // Check for subscript bracket notation: Name[Label]
        // These are valid identifiers in the parser and don't need quoting
        int bracketIdx = name.indexOf('[');
        if (bracketIdx > 0 && name.endsWith("]")) {
            String baseName = name.substring(0, bracketIdx);
            String label = name.substring(bracketIdx + 1, name.length() - 1);
            return needsQuoting(baseName) || label.isEmpty();
        }
        for (int i = 0; i < name.length(); i++) {
            char c = name.charAt(i);
            if (i == 0) {
                if (!Character.isLetter(c) && c != '_') {
                    return true;
                }
            } else {
                if (!Character.isLetterOrDigit(c) && c != '_') {
                    return true;
                }
            }
        }
        // Reserved words that would be parsed as keywords or function calls
        String lower = name.toLowerCase(Locale.ROOT);
        if (lower.equals("and") || lower.equals("or") || lower.equals("not")) {
            return true;
        }
        return name.equals("IF") || name.equals("IF_SHORT")
                || name.equals("TIME") || name.equals("DT")
                || name.equals("PI");
    }

    private static void appendBinaryOp(StringBuilder sb, Expr.BinaryOp bin, int contextPrecedence) {
        int prec = bin.operator().precedence();
        boolean needParens = prec < contextPrecedence;

        if (needParens) {
            sb.append('(');
        }
        // Left operand: use prec+1 for right-associative (POW) to force parens on (a^b)^c
        int leftContext = bin.operator() == BinaryOperator.POW ? prec + 1 : prec;
        appendExpr(sb, bin.left(), leftContext);
        sb.append(' ').append(bin.operator().symbol()).append(' ');

        // Right operand: use prec+1 for left-associative, prec for right-associative (POW)
        int rightContext = bin.operator() == BinaryOperator.POW ? prec : prec + 1;
        appendExpr(sb, bin.right(), rightContext);

        if (needParens) {
            sb.append(')');
        }
    }

    private static void appendUnaryOp(StringBuilder sb, Expr.UnaryOp un) {
        sb.append(un.operator().symbol());
        // UNARY_CONTEXT_PRECEDENCE forces binary operands to self-parenthesize
        appendExpr(sb, un.operand(), UNARY_CONTEXT_PRECEDENCE);
    }

    private static void appendFunctionCall(StringBuilder sb, Expr.FunctionCall call) {
        if (call.arguments().isEmpty()) {
            // Zero-arg builtins like TIME, DT
            sb.append(call.name());
            return;
        }
        sb.append(call.name()).append('(');
        for (int i = 0; i < call.arguments().size(); i++) {
            if (i > 0) {
                sb.append(", ");
            }
            appendExpr(sb, call.arguments().get(i), MIN_PRECEDENCE);
        }
        sb.append(')');
    }

    private static void appendConditional(StringBuilder sb, Expr.Conditional cond) {
        sb.append(cond.shortCircuit() ? "IF_SHORT(" : "IF(");
        appendExpr(sb, cond.condition(), MIN_PRECEDENCE);
        sb.append(", ");
        appendExpr(sb, cond.thenExpr(), MIN_PRECEDENCE);
        sb.append(", ");
        appendExpr(sb, cond.elseExpr(), MIN_PRECEDENCE);
        sb.append(')');
    }
}
