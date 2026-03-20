package systems.courant.sd.model.expr;

import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Walks an {@link Expr} AST and renames references from {@code oldName} to {@code newName}.
 * Handles both {@link Expr.Ref} nodes and lookup table names in {@link Expr.FunctionCall} nodes.
 *
 * <p>This provides a structurally correct alternative to string-based token replacement,
 * correctly handling backtick-quoted identifiers, lookup table names in function position,
 * and nested expressions without false matches on substrings.</p>
 */
public final class ExprRenamer {

    private static final Set<String> BUILTIN_FUNCTIONS = BuiltinFunctions.NAMES;

    private ExprRenamer() {
    }

    /**
     * Returns a new expression tree with all references to {@code oldName} replaced
     * by {@code newName}. If {@code newName} is a numeric literal (e.g. "0"),
     * produces an {@link Expr.Literal} instead of a {@link Expr.Ref}.
     * If the expression contains no matching references, the original tree is
     * returned unchanged.
     *
     * <p>Matching is exact-case, consistent with {@link ExprDependencies#extract}.
     * Callers needing case-insensitive rename should normalise names before calling.</p>
     *
     * @param expr    the expression to transform
     * @param oldName the reference name to replace
     * @param newName the new reference name (or numeric literal string)
     * @return a transformed expression, or the original if no changes were made
     */
    public static Expr rename(Expr expr, String oldName, String newName) {
        return switch (expr) {
            case Expr.Literal lit -> lit;
            case Expr.Ref ref -> {
                if (ref.name().equals(oldName)) {
                    yield replacementExpr(newName);
                }
                yield ref;
            }
            case Expr.UnaryOp un -> {
                Expr renamed = rename(un.operand(), oldName, newName);
                yield renamed == un.operand() ? un : new Expr.UnaryOp(un.operator(), renamed);
            }
            case Expr.BinaryOp bin -> {
                Expr left = rename(bin.left(), oldName, newName);
                Expr right = rename(bin.right(), oldName, newName);
                yield (left == bin.left() && right == bin.right())
                        ? bin
                        : new Expr.BinaryOp(left, bin.operator(), right);
            }
            case Expr.FunctionCall call -> {
                // Rename lookup table references: single-arg calls whose name is not a built-in
                String funcName = call.name();
                if (call.arguments().size() == 1
                        && !BUILTIN_FUNCTIONS.contains(funcName.toUpperCase(Locale.ROOT))
                        && funcName.equals(oldName)) {
                    funcName = newName;
                }
                List<Expr> args = renameArgs(call.arguments(), oldName, newName);
                yield (funcName.equals(call.name()) && args == call.arguments())
                        ? call
                        : new Expr.FunctionCall(funcName, args);
            }
            case Expr.Conditional cond -> {
                Expr condition = rename(cond.condition(), oldName, newName);
                Expr thenExpr = rename(cond.thenExpr(), oldName, newName);
                Expr elseExpr = rename(cond.elseExpr(), oldName, newName);
                yield (condition == cond.condition() && thenExpr == cond.thenExpr()
                        && elseExpr == cond.elseExpr())
                        ? cond
                        : new Expr.Conditional(condition, thenExpr, elseExpr, cond.shortCircuit());
            }
        };
    }

    /**
     * Creates the replacement expression node. If newName is a numeric literal,
     * produces an {@link Expr.Literal}; otherwise produces an {@link Expr.Ref}.
     */
    private static Expr replacementExpr(String newName) {
        try {
            double value = Double.parseDouble(newName);
            if (Double.isFinite(value)) {
                return new Expr.Literal(value);
            }
        } catch (NumberFormatException e) {
            // fall through to Ref
        }
        return new Expr.Ref(newName);
    }

    private static List<Expr> renameArgs(List<Expr> args, String oldName, String newName) {
        boolean changed = false;
        Expr[] renamed = new Expr[args.size()];
        for (int i = 0; i < args.size(); i++) {
            renamed[i] = rename(args.get(i), oldName, newName);
            if (renamed[i] != args.get(i)) {
                changed = true;
            }
        }
        return changed ? List.of(renamed) : args;
    }
}
