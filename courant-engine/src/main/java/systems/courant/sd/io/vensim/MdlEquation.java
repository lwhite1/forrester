package systems.courant.sd.io.vensim;

/**
 * A single parsed equation block from a Vensim .mdl file.
 *
 * <p>Each equation block in the .mdl format is separated by {@code |} and consists of
 * three tilde-separated sections: the equation, units, and comment.
 *
 * @param name the variable name (original Vensim form with spaces)
 * @param operator the equation operator: {@code "="}, {@code "=="}, {@code ":="}, {@code "()"}, or {@code ":"}
 * @param expression the right-hand side expression (after joining continuation lines)
 * @param units the units string (may include range annotations like {@code [0,100]})
 * @param comment the documentation comment
 * @param group the group name (e.g., ".Control"), or empty string if ungrouped
 */
public record MdlEquation(
        String name,
        String operator,
        String expression,
        String units,
        String comment,
        String group
) {

    public MdlEquation {
        if (name == null) {
            name = "";
        }
        if (operator == null) {
            operator = "";
        }
        if (expression == null) {
            expression = "";
        }
        if (units == null) {
            units = "";
        }
        if (comment == null) {
            comment = "";
        }
        if (group == null) {
            group = "";
        }
    }
}
