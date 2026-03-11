package systems.courant.shrewd.model.def;

import java.util.List;

/**
 * Definition of a flow (rate/process) in a model.
 *
 * @param name the flow name
 * @param comment optional description
 * @param equation the formula expression string
 * @param timeUnit the time unit name for the flow rate
 * @param materialUnit the material unit name (e.g. "Person"), or null to infer from connected stock
 * @param source the name of the source stock (outflow), or null for a source from outside the model
 * @param sink the name of the sink stock (inflow), or null for a sink outside the model
 * @param subscripts dimension names this flow is subscripted over (empty for scalar)
 */
public record FlowDef(
        String name,
        String comment,
        String equation,
        String timeUnit,
        String materialUnit,
        String source,
        String sink,
        List<String> subscripts
) implements ElementDef {

    public FlowDef {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Flow name must not be blank");
        }
        if (equation == null || equation.isBlank()) {
            throw new IllegalArgumentException("Flow equation must not be blank");
        }
        if (timeUnit == null || timeUnit.isBlank()) {
            throw new IllegalArgumentException("Flow timeUnit must not be blank");
        }
        subscripts = subscripts == null ? List.of() : List.copyOf(subscripts);
    }

    /**
     * Backward-compatible constructor without materialUnit or subscripts.
     */
    public FlowDef(String name, String comment, String equation, String timeUnit,
                   String source, String sink) {
        this(name, comment, equation, timeUnit, null, source, sink, List.of());
    }

    /**
     * Convenience constructor that creates a flow definition without a comment or materialUnit.
     */
    public FlowDef(String name, String equation, String timeUnit, String source, String sink) {
        this(name, null, equation, timeUnit, null, source, sink, List.of());
    }

    /**
     * Backward-compatible constructor without materialUnit, with subscripts.
     */
    public FlowDef(String name, String comment, String equation, String timeUnit,
                   String source, String sink, List<String> subscripts) {
        this(name, comment, equation, timeUnit, null, source, sink, subscripts);
    }
}
