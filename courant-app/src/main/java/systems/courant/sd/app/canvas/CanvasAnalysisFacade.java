package systems.courant.sd.app.canvas;

import systems.courant.sd.model.def.ModelDefinition;
import systems.courant.sd.model.def.ModelValidator;
import systems.courant.sd.model.def.ValidationIssue;
import systems.courant.sd.model.def.ValidationIssue.Severity;
import systems.courant.sd.model.def.ValidationResult;
import systems.courant.sd.model.graph.CausalTraceAnalysis;
import systems.courant.sd.model.graph.DependencyGraph;
import systems.courant.sd.model.graph.FeedbackAnalysis;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Supplier;

import systems.courant.sd.app.canvas.controllers.CausalTraceController;
import systems.courant.sd.app.canvas.controllers.LoopHighlightController;

/**
 * Facade encapsulating model analysis responsibilities: loop highlighting,
 * causal tracing, validation indicators, and dependency queries.
 * Extracted from {@link ModelCanvas} to reduce its size and isolate
 * a coherent set of analysis concerns.
 */
public final class CanvasAnalysisFacade {

    private final CanvasState canvasState;
    private final Supplier<ModelDefinition> modelDefSupplier;
    private final Runnable onRedraw;
    private final Runnable onStatusChanged;

    // Feedback loop highlighting
    private final LoopHighlightController loopController = new LoopHighlightController();

    // Causal tracing
    private final CausalTraceController traceController = new CausalTraceController();

    // Validation issue indicators (element name -> highest severity)
    private Map<String, Severity> elementIssues = Map.of();

    // Full validation issues per element (for tooltips and dialog)
    private Map<String, List<ValidationIssue>> elementIssueDetails = Map.of();

    // Maturity analysis (missing equations, units, mismatches)
    private MaturityAnalysis maturityAnalysis = MaturityAnalysis.EMPTY;

    // Last validation result (for dialog access)
    private ValidationResult lastValidationResult = new ValidationResult(List.of());

    // Callback when validation counts change
    private Consumer<ValidationResult> onValidationChanged;

    CanvasAnalysisFacade(CanvasState canvasState,
                         Supplier<ModelDefinition> modelDefSupplier,
                         Runnable onRedraw,
                         Runnable onStatusChanged) {
        this.canvasState = canvasState;
        this.modelDefSupplier = modelDefSupplier;
        this.onRedraw = onRedraw;
        this.onStatusChanged = onStatusChanged;
    }

    // --- Loop analysis (delegated to LoopHighlightController) ---

    public void setLoopHighlightActive(boolean active) {
        loopController.setActive(active, modelDefSupplier);
        onRedraw.run();
    }

    public boolean isLoopHighlightActive() {
        return loopController.isActive();
    }

    public FeedbackAnalysis getLoopAnalysis() {
        return loopController.getAnalysis();
    }

    public int getActiveLoopIndex() {
        return loopController.getActiveIndex();
    }

    public void setActiveLoopIndex(int index) {
        if (loopController.setActiveIndex(index)) {
            onRedraw.run();
            onStatusChanged.run();
        }
    }

    public void stepLoopForward() {
        if (loopController.stepForward()) {
            onRedraw.run();
            onStatusChanged.run();
        }
    }

    public void stepLoopBack() {
        if (loopController.stepBack()) {
            onRedraw.run();
            onStatusChanged.run();
        }
    }

    public FeedbackAnalysis.LoopType getLoopTypeFilter() {
        return loopController.getTypeFilter();
    }

    public void setLoopTypeFilter(FeedbackAnalysis.LoopType filter) {
        if (loopController.setTypeFilter(filter)) {
            onRedraw.run();
            onStatusChanged.run();
        }
    }

    public int getFilteredLoopCount() {
        return loopController.filteredLoopCount();
    }

    public FeedbackAnalysis getActiveLoopAnalysis() {
        return loopController.getActiveAnalysis();
    }

    // --- Causal tracing ---

    public void traceUpstream(String elementName) {
        ModelDefinition def = modelDefSupplier.get();
        if (def == null) {
            return;
        }
        traceController.startTrace(elementName,
                CausalTraceAnalysis.TraceDirection.UPSTREAM, def);
        onRedraw.run();
    }

    public void traceDownstream(String elementName) {
        ModelDefinition def = modelDefSupplier.get();
        if (def == null) {
            return;
        }
        traceController.startTrace(elementName,
                CausalTraceAnalysis.TraceDirection.DOWNSTREAM, def);
        onRedraw.run();
    }

    public boolean isTraceActive() {
        return traceController.isActive();
    }

    public void clearTrace() {
        traceController.clearTrace();
    }

    CausalTraceAnalysis getTraceAnalysis() {
        return traceController.getAnalysis();
    }

    // --- Dependency queries ---

    public Set<String> whereUsed(String elementName) {
        ModelDefinition def = modelDefSupplier.get();
        if (def == null) {
            return Set.of();
        }
        DependencyGraph graph = DependencyGraph.fromDefinition(def);
        return graph.dependentsOf(elementName);
    }

    public Set<String> uses(String elementName) {
        ModelDefinition def = modelDefSupplier.get();
        if (def == null) {
            return Set.of();
        }
        DependencyGraph graph = DependencyGraph.fromDefinition(def);
        return graph.dependenciesOf(elementName);
    }

    public void showWhereUsed(String elementName) {
        Set<String> dependents = whereUsed(elementName);
        canvasState.clearSelection();
        dependents.forEach(canvasState::addToSelection);
        onStatusChanged.run();
        onRedraw.run();
    }

    public void showUses(String elementName) {
        Set<String> dependencies = uses(elementName);
        canvasState.clearSelection();
        dependencies.forEach(canvasState::addToSelection);
        onStatusChanged.run();
        onRedraw.run();
    }

    // --- Validation ---

    public void setOnValidationChanged(Consumer<ValidationResult> callback) {
        this.onValidationChanged = callback;
    }

    public ValidationResult getLastValidationResult() {
        return lastValidationResult;
    }

    public List<ValidationIssue> getValidationIssues(String elementName) {
        return elementIssueDetails.getOrDefault(elementName, List.of());
    }

    // --- Render accessors (package-private) ---

    Map<String, Severity> elementIssues() {
        return elementIssues;
    }

    Map<String, List<ValidationIssue>> elementIssueDetails() {
        return elementIssueDetails;
    }

    MaturityAnalysis maturityAnalysis() {
        return maturityAnalysis;
    }

    // --- Invalidation ---

    void invalidate() {
        ModelDefinition def = modelDefSupplier.get();
        loopController.invalidate(def);
        traceController.invalidate(def);
        recomputeValidation(def);
    }

    private void recomputeValidation(ModelDefinition def) {
        if (def == null) {
            elementIssues = Map.of();
            elementIssueDetails = Map.of();
            lastValidationResult = new ValidationResult(List.of());
            maturityAnalysis = MaturityAnalysis.EMPTY;
            return;
        }
        ValidationResult result = ModelValidator.validate(def);
        Map<String, Severity> issues = new LinkedHashMap<>();
        Map<String, List<ValidationIssue>> details = new LinkedHashMap<>();
        for (ValidationIssue issue : result.issues()) {
            if (issue.elementName() != null) {
                issues.merge(issue.elementName(), issue.severity(),
                        (existing, incoming) ->
                                existing == Severity.ERROR ? existing : incoming);
                details.computeIfAbsent(issue.elementName(), k -> new ArrayList<>()).add(issue);
            }
        }
        elementIssues = issues;
        elementIssueDetails = details;
        lastValidationResult = result;
        maturityAnalysis = MaturityAnalysis.analyze(def);
        if (onValidationChanged != null) {
            onValidationChanged.accept(result);
        }
    }
}
