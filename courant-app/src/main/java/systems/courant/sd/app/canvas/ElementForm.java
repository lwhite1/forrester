package systems.courant.sd.app.canvas;

/**
 * Interface for element-type-specific property forms in the {@link PropertiesPanel}.
 * Each implementation knows how to build, update, and clean up a form for one element type.
 */
interface ElementForm {

    /**
     * Builds the form fields into the context's grid starting at the given row.
     *
     * @return the next available row index
     */
    int build(int startRow);

    /**
     * Updates field values for the cached form fast-path (same element type, different element).
     */
    void updateValues();

    /**
     * Cleans up resources (e.g., detach autocomplete popups).
     */
    default void dispose() {
    }
}
