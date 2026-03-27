package systems.courant.sd.api;

import systems.courant.sd.model.compile.FunctionDoc;
import systems.courant.sd.model.compile.FunctionDocRegistry;
import systems.courant.sd.model.expr.Expr;
import systems.courant.sd.model.expr.ExprDependencies;
import systems.courant.sd.model.expr.ExprParser;
import systems.courant.sd.model.expr.ExprRenamer;
import systems.courant.sd.model.expr.ExprStringifier;
import systems.courant.sd.model.expr.ParseException;

import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Stable API facade for expression-related operations. Provides equation
 * validation, reference extraction, renaming, and built-in function
 * metadata without requiring callers to import internal AST or compiler
 * classes.
 *
 * <p>This is the primary entry point for courant-app code that needs to
 * work with equations. Internal classes ({@code ExprParser},
 * {@code ExprDependencies}, {@code FunctionDocRegistry}, etc.) should not
 * be imported directly by application code.</p>
 */
public final class ExpressionFacade {

    private ExpressionFacade() {
    }

    /**
     * Validates equation syntax. Returns an empty optional if the equation
     * is syntactically valid, or an error message describing the parse failure.
     *
     * @param equation the equation text to validate
     * @return empty if valid, or an error message
     */
    public static Optional<String> validateSyntax(String equation) {
        try {
            ExprParser.parse(equation);
            return Optional.empty();
        } catch (ParseException e) {
            return Optional.of(formatParseError(e));
        }
    }

    /**
     * Parses an equation and extracts the set of variable names it references.
     * Returns an empty set if the equation cannot be parsed.
     *
     * @param equation the equation text
     * @return set of referenced variable names (normalized), or empty set on parse failure
     */
    public static Set<String> extractReferences(String equation) {
        try {
            Expr expr = ExprParser.parse(equation);
            return ExprDependencies.extract(expr);
        } catch (ParseException e) {
            return Set.of();
        }
    }

    /**
     * Renames a variable reference within an equation using AST-based
     * transformation. Falls back to the original equation if parsing fails
     * (callers should handle text-based fallback themselves if needed).
     *
     * @param equation the equation text
     * @param oldName  the current variable name (normalized)
     * @param newName  the replacement variable name (normalized)
     * @return the equation with references renamed, or the original if parsing fails
     */
    public static String renameReference(String equation, String oldName, String newName) {
        try {
            Expr ast = ExprParser.parse(equation);
            Expr renamed = ExprRenamer.rename(ast, oldName, newName);
            if (renamed == ast) {
                return equation;
            }
            return ExprStringifier.stringify(renamed);
        } catch (ParseException e) {
            return equation;
        }
    }

    /**
     * Returns the names of all built-in functions recognized by the
     * expression compiler.
     */
    public static List<String> builtinFunctionNames() {
        return FunctionDocRegistry.allNames();
    }

    /**
     * Returns the documentation for a specific built-in function.
     *
     * @param name the function name (case-insensitive)
     * @return the function documentation, or empty if not found
     */
    public static Optional<FunctionDoc> getFunctionDoc(String name) {
        return FunctionDocRegistry.get(name);
    }

    /**
     * Returns documentation for all built-in functions.
     */
    public static List<FunctionDoc> allFunctionDocs() {
        return FunctionDocRegistry.all();
    }

    private static String formatParseError(ParseException e) {
        String msg = e.getMessage();
        if (msg == null || msg.isBlank()) {
            return "Syntax error";
        }
        // Strip the "(at position N)" suffix added by ParseException constructor
        int idx = msg.lastIndexOf(" (at position ");
        if (idx > 0) {
            return msg.substring(0, idx);
        }
        return msg;
    }
}
