package systems.courant.shrewd.app.canvas;

/**
 * Listener interface for model editing events. Implementations receive
 * notifications when elements are added, removed, renamed, or modified,
 * and when simulations, validations, or analyses are run.
 */
public interface ModelEditListener {

    default void onElementAdded(String name, String typeName) {
    }

    default void onElementRemoved(String name) {
    }

    default void onElementRenamed(String oldName, String newName) {
    }

    default void onEquationChanged(String elementName) {
    }

    default void onSimulationRun() {
    }

    default void onValidation(int errors, int warnings) {
    }

    default void onAnalysisRun(String type, String details) {
    }

    default void onModelSaved(String filename) {
    }

    default void onModelOpened(String filename) {
    }
}
