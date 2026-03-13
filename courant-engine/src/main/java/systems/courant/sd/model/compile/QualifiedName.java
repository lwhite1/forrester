package systems.courant.sd.model.compile;

import java.util.Arrays;
import java.util.List;

/**
 * Utility for parsing and working with dot-separated qualified names
 * used in nested module hierarchies (e.g. "Workforce.Total Workforce").
 */
public record QualifiedName(List<String> parts) {

    public QualifiedName {
        parts = List.copyOf(parts);
        if (parts.isEmpty()) {
            throw new IllegalArgumentException("Qualified name must have at least one part");
        }
        for (String part : parts) {
            if (part == null || part.isBlank()) {
                throw new IllegalArgumentException(
                        "Qualified name must not contain blank parts: " + parts);
            }
        }
    }

    /**
     * Parses a dot-separated qualified name string.
     *
     * @throws IllegalArgumentException if the name is null, blank, or contains empty parts
     */
    public static QualifiedName parse(String name) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Qualified name must not be blank");
        }
        return new QualifiedName(Arrays.asList(name.split("\\.", -1)));
    }

    /**
     * Returns the leaf (last) part of the qualified name.
     */
    public String leaf() {
        return parts.get(parts.size() - 1);
    }

    /**
     * Returns true if the name has more than one part (is qualified).
     */
    public boolean isQualified() {
        return parts.size() > 1;
    }

    @Override
    public String toString() {
        return String.join(".", parts);
    }
}
