package systems.courant.sd.app.canvas.controllers;

import systems.courant.sd.model.def.ModelDefinition;
import systems.courant.sd.model.graph.CausalTraceAnalysis;
import systems.courant.sd.model.graph.CausalTraceAnalysis.TraceDirection;

/**
 * Manages causal trace state: start/clear trace, recompute on model changes.
 * Follows the same pattern as {@link LoopHighlightController}.
 */
public final class CausalTraceController {

    private CausalTraceAnalysis traceAnalysis;
    private String tracedElement;
    private TraceDirection tracedDirection;

    public boolean isActive() {
        return traceAnalysis != null;
    }

    public CausalTraceAnalysis getAnalysis() {
        return traceAnalysis;
    }

    public void startTrace(String elementName, TraceDirection direction, ModelDefinition model) {
        this.tracedElement = elementName;
        this.tracedDirection = direction;
        this.traceAnalysis = CausalTraceAnalysis.trace(elementName, direction, model);
    }

    public void clearTrace() {
        this.traceAnalysis = null;
        this.tracedElement = null;
        this.tracedDirection = null;
    }

    /**
     * Recomputes the trace if active (e.g., after model mutation).
     */
    public void invalidate(ModelDefinition model) {
        if (isActive() && model != null) {
            this.traceAnalysis = CausalTraceAnalysis.trace(
                    tracedElement, tracedDirection, model);
        }
    }
}
