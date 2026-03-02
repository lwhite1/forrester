package com.deathrayresearch.forrester.model.expr;

/**
 * Converts an {@link Expr} AST back into a readable infix string with minimal parentheses.
 * The output should round-trip through {@link ExprParser}: {@code parse(stringify(expr)) == expr}.
 */
public final class ExprStringifier {

    private ExprStringifier() {
    }

    /**
     * Converts the given expression AST to a human-readable infix string.
     */
    public static String stringify(Expr expr) {
        StringBuilder sb = new StringBuilder();
        appendExpr(sb, expr, -10); // -10 = lowest possible context precedence
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
        }
    }

    private static void appendLiteral(StringBuilder sb, Expr.Literal lit) {
        double v = lit.value();
        if (v == (long) v && !Double.isInfinite(v) && Math.abs(v) < 1e15) {
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
        // Reserved words that would be parsed as function calls
        return name.equals("IF") || name.equals("TIME") || name.equals("DT");
    }

    private static void appendBinaryOp(StringBuilder sb, Expr.BinaryOp bin, int contextPrecedence) {
        int prec = bin.operator().precedence();
        boolean needParens = prec < contextPrecedence;

        if (needParens) {
            sb.append('(');
        }
        appendExpr(sb, bin.left(), prec);
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
        boolean needParens = un.operand() instanceof Expr.BinaryOp;
        if (needParens) {
            sb.append('(');
        }
        appendExpr(sb, un.operand(), 10); // high precedence so inner binary ops get parens
        if (needParens) {
            sb.append(')');
        }
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
            appendExpr(sb, call.arguments().get(i), -10);
        }
        sb.append(')');
    }

    private static void appendConditional(StringBuilder sb, Expr.Conditional cond) {
        sb.append("IF(");
        appendExpr(sb, cond.condition(), -10);
        sb.append(", ");
        appendExpr(sb, cond.thenExpr(), -10);
        sb.append(", ");
        appendExpr(sb, cond.elseExpr(), -10);
        sb.append(')');
    }
}
