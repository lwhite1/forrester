package systems.courant.forrester.app.canvas;

import systems.courant.forrester.model.def.ModelDefinition;
import systems.courant.forrester.model.def.ViewDef;
import systems.courant.forrester.model.graph.FeedbackAnalysis;

import java.util.function.Supplier;

/**
 * Manages feedback loop highlighting state: active/inactive toggle,
 * loop analysis computation, and step-through navigation (forward/back).
 */
final class LoopHighlightController {

    private boolean active;
    private FeedbackAnalysis analysis;
    /** Active loop index for step-through mode. -1 = show all loops. */
    private int activeIndex = -1;

    boolean isActive() {
        return active;
    }

    FeedbackAnalysis getAnalysis() {
        return analysis;
    }

    int getActiveIndex() {
        return activeIndex;
    }

    /**
     * Toggles loop highlighting on or off. When turning on, recomputes
     * the loop analysis from the current model definition.
     */
    void setActive(boolean active, Supplier<ModelDefinition> modelSupplier) {
        this.active = active;
        this.activeIndex = -1;
        if (active && modelSupplier.get() != null) {
            recompute(modelSupplier.get());
        } else {
            this.analysis = null;
        }
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
     * Steps to the next loop, wrapping around. If showing all, goes to first loop.
     * Returns true if the index changed.
     */
    boolean stepForward() {
        if (analysis == null || analysis.loopCount() == 0) {
            return false;
        }
        int count = analysis.loopCount();
        if (activeIndex < 0) {
            return setActiveIndex(0);
        } else {
            return setActiveIndex((activeIndex + 1) % count);
        }
    }

    /**
     * Steps to the previous loop, wrapping around. If showing all, goes to last loop.
     * Returns true if the index changed.
     */
    boolean stepBack() {
        if (analysis == null || analysis.loopCount() == 0) {
            return false;
        }
        int count = analysis.loopCount();
        if (activeIndex < 0) {
            return setActiveIndex(count - 1);
        } else {
            return setActiveIndex((activeIndex - 1 + count) % count);
        }
    }

    /**
     * Returns the filtered analysis for the active loop, or the full analysis
     * if showing all loops. Returns null if highlighting is off.
     */
    FeedbackAnalysis getActiveAnalysis() {
        if (!active || analysis == null) {
            return null;
        }
        if (activeIndex >= 0) {
            return analysis.filterToLoop(activeIndex);
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
