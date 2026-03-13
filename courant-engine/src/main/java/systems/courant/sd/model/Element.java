package systems.courant.sd.model;

import com.google.common.base.Preconditions;

/**
 *  Parent of all model elements: Stock, Flow, etc.
 */
public abstract class Element {

    private final String name;
    private String comment;

    protected Element(String name) {
        Preconditions.checkArgument(name != null, "name cannot be null");
        Preconditions.checkArgument(!name.isEmpty(), "name cannot be empty");
        this.name = name;
    }

    /**
     * Returns the name of this element.
     */
    public String getName() {
        return name;
    }

    @Override
    public String toString() {
        return getName();
    }

    /**
     * Returns the optional descriptive comment for this element, or {@code null} if none has been set.
     */
    public String getComment() {
        return comment;
    }

    /**
     * Sets an optional descriptive comment for this element.
     *
     * @param comment the comment text
     */
    public void setComment(String comment) {
        this.comment = comment;
    }
}
