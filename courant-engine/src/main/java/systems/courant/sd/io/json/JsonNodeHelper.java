package systems.courant.sd.io.json;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * Shared helper methods for reading fields from Jackson {@link JsonNode} trees.
 */
public final class JsonNodeHelper {

    private JsonNodeHelper() {
    }

    /**
     * Returns the text value of the named field, or {@code null} if the field
     * is absent or explicitly {@code null}.
     */
    public static String textOrNull(JsonNode node, String field) {
        JsonNode child = node.get(field);
        return (child != null && !child.isNull()) ? child.asText() : null;
    }

    /**
     * Returns the text value of the named field, throwing if absent or null.
     */
    public static String requiredText(JsonNode node, String field) {
        JsonNode child = node.get(field);
        if (child == null || child.isNull()) {
            throw new IllegalArgumentException("Missing required field: " + field);
        }
        return child.asText();
    }
}
