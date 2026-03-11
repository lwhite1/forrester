package systems.courant.shrewd.app.canvas;

import systems.courant.shrewd.model.def.ModelDefinition;
import systems.courant.shrewd.model.graph.CausalTraceAnalysis;
import systems.courant.shrewd.model.graph.CausalTraceAnalysis.TraceDirection;

/**
 * Manages causal trace state: start/clear trace, recompute on model changes.
 * Follows the same pattern as {@link LoopHighlightController}.
 */
final class CausalTraceController {

    private CausalTraceAnalysis traceAnalysis;
    private String tracedElement;
    private TraceDirection tracedDirection;

    boolean isActive() {
        return traceAnalysis != null;
    }

    CausalTraceAnalysis getAnalysis() {
        return traceAnalysis;
    }

    void startTrace(String elementName, TraceDirection direction, ModelDefinition model) {
        this.tracedElement = elementName;
        this.tracedDirection = direction;
        this.traceAnalysis = CausalTraceAnalysis.trace(elementName, direction, model);
    }

    void clearTrace() {
        this.traceAnalysis = null;
        this.tracedElement = null;
        this.tracedDirection = null;
    }

    /**
     * Recomputes the trace if active (e.g., after model mutation).
     */
    void invalidate(ModelDefinition model) {
        if (isActive() && model != null) {
            this.traceAnalysis = CausalTraceAnalysis.trace(
                    tracedElement, tracedDirection, model);
        }
    }
}
