package systems.courant.sd.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * An immutable dimension definition for arrayed model elements.
 *
 * <p>A subscript defines a named dimension (e.g., "Region") with a set of labels
 * (e.g., "North", "South", "East"). Arrayed stocks, flows, and variables use a
 * subscript to expand into one instance per label.
 */
public class Subscript {

    private final String name;
    private final List<String> labels;

    /**
     * Creates a new subscript with the given name and labels.
     *
     * @param name   the dimension name (e.g., "Region")
     * @param labels the element labels (e.g., "North", "South", "East")
     * @throws IllegalArgumentException if name is blank, labels are empty, or labels contain duplicates
     */
    public Subscript(String name, String... labels) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Subscript name must not be blank");
        }
        if (labels == null || labels.length == 0) {
            throw new IllegalArgumentException("Subscript must have at least one label");
        }
        Set<String> seen = new HashSet<>();
        List<String> copy = new ArrayList<>(labels.length);
        for (String label : labels) {
            if (label == null || label.isBlank()) {
                throw new IllegalArgumentException("Subscript labels must not be blank");
            }
            if (!seen.add(label)) {
                throw new IllegalArgumentException("Duplicate subscript label: " + label);
            }
            copy.add(label);
        }
        this.name = name;
        this.labels = Collections.unmodifiableList(copy);
    }

    /**
     * Returns the dimension name.
     */
    public String getName() {
        return name;
    }

    /**
     * Returns the number of elements in this subscript.
     */
    public int size() {
        return labels.size();
    }

    /**
     * Returns the label at the given index.
     *
     * @param index the zero-based index
     * @throws IndexOutOfBoundsException if the index is out of range
     */
    public String getLabel(int index) {
        return labels.get(index);
    }

    /**
     * Returns the index of the given label.
     *
     * @param label the label to look up
     * @return the zero-based index
     * @throws IllegalArgumentException if the label is not found
     */
    public int indexOf(String label) {
        int index = labels.indexOf(label);
        if (index < 0) {
            throw new IllegalArgumentException(
                    "Label '" + label + "' not found in subscript '" + name + "'");
        }
        return index;
    }

    /**
     * Returns an unmodifiable list of all labels.
     */
    public List<String> getLabels() {
        return labels;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Subscript other)) {
            return false;
        }
        return name.equals(other.name) && labels.equals(other.labels);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, labels);
    }

    @Override
    public String toString() {
        return name + labels;
    }
}
