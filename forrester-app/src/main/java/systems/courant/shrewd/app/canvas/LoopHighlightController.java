package systems.courant.shrewd.app.canvas;

import systems.courant.shrewd.model.def.ModelDefinition;
import systems.courant.shrewd.model.graph.FeedbackAnalysis;
import systems.courant.shrewd.model.graph.FeedbackAnalysis.LoopType;

import java.util.List;
import java.util.function.Supplier;

/**
 * Manages feedback loop highlighting state: active/inactive toggle,
 * loop analysis computation, step-through navigation (forward/back),
 * and type filtering (R only, B only, or all).
 */
final class LoopHighlightController {

    private boolean active;
    private FeedbackAnalysis analysis;
    /** Active loop index for step-through mode. -1 = show all loops. */
    private int activeIndex = -1;
    /** Type filter: null = show all, REINFORCING or BALANCING = show only that type. */
    private LoopType typeFilter;

    boolean isActive() {
        return active;
    }

    FeedbackAnalysis getAnalysis() {
        return analysis;
    }

    int getActiveIndex() {
        return activeIndex;
    }

    LoopType getTypeFilter() {
        return typeFilter;
    }

    /**
     * Sets the type filter. Resets the active index to -1 (show all).
     * Returns true if the filter changed.
     */
    boolean setTypeFilter(LoopType filter) {
        if (this.typeFilter == filter) {
            return false;
        }
        this.typeFilter = filter;
        this.activeIndex = -1;
        return true;
    }

    /**
     * Toggles loop highlighting on or off. When turning on, recomputes
     * the loop analysis from the current model definition.
     */
    void setActive(boolean active, Supplier<ModelDefinition> modelSupplier) {
        this.active = active;
        this.activeIndex = -1;
        this.typeFilter = null;
        ModelDefinition model = active ? modelSupplier.get() : null;
        if (model != null) {
            recompute(model);
        } else {
            this.analysis = null;
        }
    }

    /**
     * Returns the filtered loop indices based on the current type filter.
     */
    private List<Integer> filteredIndices() {
        if (analysis == null) {
            return List.of();
        }
        return analysis.filteredIndices(typeFilter);
    }

    /**
     * Returns the number of loops matching the current type filter.
     */
    int filteredLoopCount() {
        return filteredIndices().size();
    }

    /**
     * Sets the active loop index. -1 = show all loops.
     * Returns true if the index changed.
     */
    boolean setActiveIndex(int index) {
        if (analysis == null) {
            return false;
        }
        int count = analysis.loopCount();
        if (index < -1 || index >= count) {
            index = -1;
        }
        this.activeIndex = index;
        return true;
    }

    /**
     * Steps to the next loop matching the type filter, wrapping around.
     * If showing all, goes to first matching loop.
     * Returns true if the index changed.
     */
    boolean stepForward() {
        if (analysis == null || analysis.loopCount() == 0) {
            return false;
        }
        List<Integer> indices = filteredIndices();
        if (indices.isEmpty()) {
            return false;
        }
        if (activeIndex < 0) {
            return setActiveIndex(indices.getFirst());
        }
        // Find next matching index after activeIndex
        for (int idx : indices) {
            if (idx > activeIndex) {
                return setActiveIndex(idx);
            }
        }
        // Wrap around
        return setActiveIndex(indices.getFirst());
    }

    /**
     * Steps to the previous loop matching the type filter, wrapping around.
     * If showing all, goes to last matching loop.
     * Returns true if the index changed.
     */
    boolean stepBack() {
        if (analysis == null || analysis.loopCount() == 0) {
            return false;
        }
        List<Integer> indices = filteredIndices();
        if (indices.isEmpty()) {
            return false;
        }
        if (activeIndex < 0) {
            return setActiveIndex(indices.getLast());
        }
        // Find previous matching index before activeIndex
        for (int i = indices.size() - 1; i >= 0; i--) {
            if (indices.get(i) < activeIndex) {
                return setActiveIndex(indices.get(i));
            }
        }
        // Wrap around
        return setActiveIndex(indices.getLast());
    }

    /**
     * Returns the filtered analysis for the active loop, or the full analysis
     * (optionally filtered by type) if showing all loops. Returns null if
     * highlighting is off.
     */
    FeedbackAnalysis getActiveAnalysis() {
        if (!active || analysis == null) {
            return null;
        }
        if (activeIndex >= 0) {
            return analysis.filterToLoop(activeIndex);
        }
        if (typeFilter != null) {
            return analysis.filterByType(typeFilter);
        }
        return analysis;
    }

    /**
     * Recomputes loop analysis if highlighting is active.
     * Must be called after any structural model mutation.
     */
    void invalidate(ModelDefinition modelDef) {
        if (active && modelDef != null) {
            recompute(modelDef);
        }
    }

    private void recompute(ModelDefinition modelDef) {
        analysis = FeedbackAnalysis.analyze(modelDef);
        if (activeIndex >= analysis.loopCount()) {
            activeIndex = -1;
        }
    }
}
