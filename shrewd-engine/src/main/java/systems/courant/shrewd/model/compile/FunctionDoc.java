package systems.courant.shrewd.model.compile;

import java.util.List;

/**
 * Structured documentation for a built-in function.
 *
 * @param name       uppercase function name (e.g. "STEP")
 * @param signature  full signature with parameter names (e.g. "STEP(height, step_time)")
 * @param oneLiner   one-line description for autocomplete popups
 * @param category   grouping: "SD", "Math", or "Special"
 * @param parameters per-parameter documentation
 * @param behavior   multi-sentence explanation of what the function does
 * @param example    concrete usage example with expected output
 * @param related    names of related functions for cross-reference
 */
public record FunctionDoc(
        String name,
        String signature,
        String oneLiner,
        String category,
        List<ParamDoc> parameters,
        String behavior,
        String example,
        List<String> related) {

    /**
     * Documentation for a single function parameter.
     */
    public record ParamDoc(String name, String description) { }
}
