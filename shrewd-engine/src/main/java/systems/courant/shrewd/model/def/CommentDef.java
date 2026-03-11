package systems.courant.shrewd.model.def;

/**
 * A free-text annotation placed on the canvas for documentation purposes.
 * Comments are purely visual decorations — they are excluded from simulation
 * and do not participate in any model computations.
 *
 * @param name the internal identifier (auto-generated, e.g. "Comment 1")
 * @param text the user-visible annotation text
 */
public record CommentDef(
        String name,
        String text
) implements ElementDef {

    public CommentDef {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Comment name must not be blank");
        }
    }

    /**
     * Creates a comment definition with empty text.
     *
     * @param name the internal identifier
     */
    public CommentDef(String name) {
        this(name, "");
    }
}
