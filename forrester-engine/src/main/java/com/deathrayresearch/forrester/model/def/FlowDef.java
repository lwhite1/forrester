package com.deathrayresearch.forrester.model.def;

import java.util.List;

/**
 * Definition of a flow (rate/process) in a model.
 *
 * @param name the flow name
 * @param comment optional description
 * @param equation the formula expression string
 * @param timeUnit the time unit name for the flow rate
 * @param source the name of the source stock (outflow), or null for a source from outside the model
 * @param sink the name of the sink stock (inflow), or null for a sink outside the model
 * @param subscripts dimension names this flow is subscripted over (empty for scalar)
 */
public record FlowDef(
        String name,
        String comment,
        String equation,
        String timeUnit,
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
     * Backward-compatible constructor without subscripts.
     */
    public FlowDef(String name, String comment, String equation, String timeUnit,
                   String source, String sink) {
        this(name, comment, equation, timeUnit, source, sink, List.of());
    }

    /**
     * Convenience constructor that creates a flow definition without a comment.
     */
    public FlowDef(String name, String equation, String timeUnit, String source, String sink) {
        this(name, null, equation, timeUnit, source, sink, List.of());
    }
}
