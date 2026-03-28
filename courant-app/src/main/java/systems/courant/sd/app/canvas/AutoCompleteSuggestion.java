package systems.courant.sd.app.canvas;

/**
 * A suggestion item for the equation autocomplete popup.
 *
 * @param name        the identifier to insert (e.g. "STEP", "Population")
 * @param displayName display text for the primary line (e.g. "STEP(height, step_time)")
 * @param description secondary line (e.g. "Step function at a point in time")
 * @param kind        the kind of suggestion for visual styling
 * @param isFunction  whether to append "(" when inserting
 */
record AutoCompleteSuggestion(
        String name,
        String displayName,
        String description,
        Kind kind,
        boolean isFunction) {

    enum Kind {
        STOCK("S"),
        FLOW("F"),
        AUX("V"),
        LOOKUP("L"),
        MODULE("M"),
        FUNCTION("fn");

        private final String badge;

        Kind(String badge) {
            this.badge = badge;
        }

        String badge() {
            return badge;
        }
    }
}
