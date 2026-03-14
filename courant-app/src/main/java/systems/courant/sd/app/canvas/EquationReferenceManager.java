package systems.courant.sd.app.canvas;

import systems.courant.sd.model.def.VariableDef;
import systems.courant.sd.model.def.FlowDef;
import systems.courant.sd.model.expr.ExprParser;
import systems.courant.sd.model.expr.ExprRenamer;
import systems.courant.sd.model.expr.ExprStringifier;
import systems.courant.sd.model.expr.Expr;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.function.UnaryOperator;

/**
 * Manages equation token references across flows and variables.
 * Handles word-boundary-aware token replacement, bulk reference updates,
 * and info-link connection management.
 *
 * <p>Extracted from {@link ModelEditor} to isolate equation manipulation logic.</p>
 */
public final class EquationReferenceManager {

    private static final Logger log = LoggerFactory.getLogger(EquationReferenceManager.class);

    private final List<FlowDef> flows;
    private final List<VariableDef> variables;

    public EquationReferenceManager(List<FlowDef> flows, List<VariableDef> variables) {
        this.flows = flows;
        this.variables = variables;
    }

    /**
     * Replaces all occurrences of {@code oldToken} with {@code newToken} in every
     * flow and variable equation, respecting word boundaries.
     *
     * @return the names of elements whose equations were actually modified
     */
    public List<String> updateEquationReferences(String oldToken, String newToken) {
        if (oldToken.equals(newToken)) {
            return List.of();
        }
        List<String> modified = new ArrayList<>();
        for (int i = 0; i < flows.size(); i++) {
            FlowDef f = flows.get(i);
            String updated = replaceToken(f.equation(), oldToken, newToken);
            if (!updated.equals(f.equation())) {
                flows.set(i, new FlowDef(f.name(), f.comment(), updated,
                        f.timeUnit(), f.materialUnit(), f.source(), f.sink(), f.subscripts()));
                modified.add(f.name());
            }
        }
        for (int i = 0; i < variables.size(); i++) {
            VariableDef a = variables.get(i);
            String updated = replaceToken(a.equation(), oldToken, newToken);
            if (!updated.equals(a.equation())) {
                variables.set(i, new VariableDef(a.name(), a.comment(), updated, a.unit(), a.subscripts()));
                modified.add(a.name());
            }
        }
        return modified;
    }

    /**
     * Finds the flow or auxiliary with the given name and applies a transform to its equation.
     * Returns true if the equation was actually changed.
     */
    public boolean updateEquationByName(String targetName, UnaryOperator<String> transform) {
        for (int i = 0; i < flows.size(); i++) {
            FlowDef f = flows.get(i);
            if (f.name().equals(targetName)) {
                String updated = transform.apply(f.equation());
                if (!updated.equals(f.equation())) {
                    flows.set(i, new FlowDef(f.name(), f.comment(), updated,
                            f.timeUnit(), f.materialUnit(), f.source(), f.sink(), f.subscripts()));
                    return true;
                }
                return false;
            }
        }
        for (int i = 0; i < variables.size(); i++) {
            VariableDef a = variables.get(i);
            if (a.name().equals(targetName)) {
                String updated = transform.apply(a.equation());
                if (!updated.equals(a.equation())) {
                    variables.set(i, new VariableDef(a.name(), a.comment(), updated, a.unit(), a.subscripts()));
                    return true;
                }
                return false;
            }
        }
        return false;
    }

    /**
     * Adds a reference token to the named element's equation.
     * If the equation is exactly "0", replaces it with the token.
     * Otherwise appends " * token" to the equation.
     */
    public boolean addConnectionReference(String elementName, String token) {
        for (int i = 0; i < flows.size(); i++) {
            FlowDef f = flows.get(i);
            if (f.name().equals(elementName)) {
                String eq = f.equation();
                if (containsWholeToken(eq, token)) {
                    return true;
                }
                String updated = "0".equals(eq.trim()) ? token : eq + " * " + token;
                flows.set(i, new FlowDef(f.name(), f.comment(), updated,
                        f.timeUnit(), f.materialUnit(), f.source(), f.sink(), f.subscripts()));
                return true;
            }
        }

        for (int i = 0; i < variables.size(); i++) {
            VariableDef a = variables.get(i);
            if (a.name().equals(elementName)) {
                String eq = a.equation();
                if (containsWholeToken(eq, token)) {
                    return true;
                }
                String updated = "0".equals(eq.trim()) ? token : eq + " * " + token;
                variables.set(i, new VariableDef(a.name(), a.comment(), updated, a.unit(), a.subscripts()));
                return true;
            }
        }

        return false;
    }

    /**
     * Checks whether the equation contains the token as a whole word,
     * respecting word boundaries (adjacent characters must not be token characters).
     */
    static boolean containsWholeToken(String equation, String token) {
        int len = equation.length();
        int tokenLen = token.length();
        int i = 0;
        while (i <= len - tokenLen) {
            int idx = equation.indexOf(token, i);
            if (idx < 0) {
                return false;
            }
            boolean startOk = idx == 0 || !isTokenChar(equation.charAt(idx - 1));
            boolean endOk = idx + tokenLen >= len || !isTokenChar(equation.charAt(idx + tokenLen));
            if (startOk && endOk) {
                return true;
            }
            i = idx + 1;
        }
        return false;
    }

    /**
     * Renames references in an equation string using AST-based transformation.
     * Parses the equation to an AST, renames matching references, and stringifies
     * the result. Falls back to word-boundary string replacement if parsing fails.
     */
    public static String replaceToken(String equation, String oldToken, String newToken) {
        if (equation == null || equation.isBlank()) {
            return equation;
        }
        try {
            Expr ast = ExprParser.parse(equation);
            Expr renamed = ExprRenamer.rename(ast, oldToken, newToken);
            if (renamed == ast) {
                return equation;
            }
            return ExprStringifier.stringify(renamed);
        } catch (Exception e) {
            log.debug("AST parse failed for equation '{}', falling back to string replacement: {}",
                    equation, e.getMessage());
            return replaceTokenByString(equation, oldToken, newToken);
        }
    }

    /**
     * Fallback word-boundary-aware string token replacement.
     * Used when the equation cannot be parsed to an AST.
     */
    public static String replaceTokenByString(String equation, String oldToken, String newToken) {
        StringBuilder result = new StringBuilder();
        int len = equation.length();
        int tokenLen = oldToken.length();
        int i = 0;

        while (i < len) {
            int idx = equation.indexOf(oldToken, i);
            if (idx < 0) {
                result.append(equation, i, len);
                break;
            }

            boolean startOk = idx == 0 || !isTokenChar(equation.charAt(idx - 1));
            boolean endOk = idx + tokenLen >= len || !isTokenChar(equation.charAt(idx + tokenLen));

            if (startOk && endOk) {
                result.append(equation, i, idx);
                result.append(newToken);
                i = idx + tokenLen;
            } else {
                result.append(equation, i, idx + 1);
                i = idx + 1;
            }
        }

        return result.toString();
    }

    public static boolean isTokenChar(char c) {
        return Character.isLetterOrDigit(c) || c == '_';
    }
}
