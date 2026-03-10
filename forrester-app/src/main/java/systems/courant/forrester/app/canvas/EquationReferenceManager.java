package systems.courant.forrester.app.canvas;

import systems.courant.forrester.model.def.AuxDef;
import systems.courant.forrester.model.def.FlowDef;

import java.util.List;
import java.util.function.UnaryOperator;

/**
 * Manages equation token references across flows and auxiliaries.
 * Handles word-boundary-aware token replacement, bulk reference updates,
 * and info-link connection management.
 *
 * <p>Extracted from {@link ModelEditor} to isolate equation manipulation logic.</p>
 */
final class EquationReferenceManager {

    private final List<FlowDef> flows;
    private final List<AuxDef> auxiliaries;

    EquationReferenceManager(List<FlowDef> flows, List<AuxDef> auxiliaries) {
        this.flows = flows;
        this.auxiliaries = auxiliaries;
    }

    /**
     * Replaces all occurrences of {@code oldToken} with {@code newToken} in every
     * flow and auxiliary equation, respecting word boundaries.
     */
    void updateEquationReferences(String oldToken, String newToken) {
        if (oldToken.equals(newToken)) {
            return;
        }
        for (int i = 0; i < flows.size(); i++) {
            FlowDef f = flows.get(i);
            String updated = replaceToken(f.equation(), oldToken, newToken);
            if (!updated.equals(f.equation())) {
                flows.set(i, new FlowDef(f.name(), f.comment(), updated,
                        f.timeUnit(), f.materialUnit(), f.source(), f.sink(), f.subscripts()));
            }
        }
        for (int i = 0; i < auxiliaries.size(); i++) {
            AuxDef a = auxiliaries.get(i);
            String updated = replaceToken(a.equation(), oldToken, newToken);
            if (!updated.equals(a.equation())) {
                auxiliaries.set(i, new AuxDef(a.name(), a.comment(), updated, a.unit()));
            }
        }
    }

    /**
     * Finds the flow or auxiliary with the given name and applies a transform to its equation.
     * Returns true if the equation was actually changed.
     */
    boolean updateEquationByName(String targetName, UnaryOperator<String> transform) {
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
        for (int i = 0; i < auxiliaries.size(); i++) {
            AuxDef a = auxiliaries.get(i);
            if (a.name().equals(targetName)) {
                String updated = transform.apply(a.equation());
                if (!updated.equals(a.equation())) {
                    auxiliaries.set(i, new AuxDef(a.name(), a.comment(), updated, a.unit()));
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
    boolean addConnectionReference(String elementName, String token) {
        for (int i = 0; i < flows.size(); i++) {
            FlowDef f = flows.get(i);
            if (f.name().equals(elementName)) {
                String eq = f.equation();
                if (eq.contains(token)) {
                    return true;
                }
                String updated = "0".equals(eq.trim()) ? token : eq + " * " + token;
                flows.set(i, new FlowDef(f.name(), f.comment(), updated,
                        f.timeUnit(), f.materialUnit(), f.source(), f.sink(), f.subscripts()));
                return true;
            }
        }

        for (int i = 0; i < auxiliaries.size(); i++) {
            AuxDef a = auxiliaries.get(i);
            if (a.name().equals(elementName)) {
                String eq = a.equation();
                if (eq.contains(token)) {
                    return true;
                }
                String updated = "0".equals(eq.trim()) ? token : eq + " * " + token;
                auxiliaries.set(i, new AuxDef(a.name(), a.comment(), updated, a.unit()));
                return true;
            }
        }

        return false;
    }

    /**
     * Word-boundary-aware token replacement in an equation string.
     * Tokens in equations use underscores for spaces (e.g. Contact_Rate).
     */
    static String replaceToken(String equation, String oldToken, String newToken) {
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

    static boolean isTokenChar(char c) {
        return Character.isLetterOrDigit(c) || c == '_';
    }
}
