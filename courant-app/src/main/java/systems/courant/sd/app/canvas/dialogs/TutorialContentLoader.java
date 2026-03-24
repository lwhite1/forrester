package systems.courant.sd.app.canvas.dialogs;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * Loads tutorial metadata (JSON) and step content (Markdown) from the
 * classpath under {@code tutorials/}.
 *
 * <p>Each tutorial is defined by a JSON file (e.g.,
 * {@code tutorials/modeling/first-model.json}) whose {@code steps} array
 * references Markdown files relative to the {@code tutorials/modeling/}
 * directory.
 */
public final class TutorialContentLoader {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final String BASE_PATH = "tutorials/";

    private TutorialContentLoader() {
    }

    /**
     * Loads a tutorial from the given resource path relative to
     * {@code tutorials/}.
     *
     * @param jsonPath path to the tutorial JSON, e.g., {@code "modeling/first-model.json"}
     * @return fully loaded tutorial content with Markdown step text
     * @throws UncheckedIOException if the JSON or any step file cannot be read
     */
    public static TutorialContent load(String jsonPath) {
        String fullJsonPath = BASE_PATH + jsonPath;
        try (InputStream is = resourceStream(fullJsonPath)) {
            JsonNode root = MAPPER.readTree(is);

            String id = root.get("id").asText();
            String title = root.get("title").asText();
            String description = root.get("description").asText();
            String difficulty = root.get("difficulty").asText();
            int estimatedMinutes = root.get("estimatedMinutes").asInt();
            String model = jsonText(root, "model");
            String nextTutorial = jsonText(root, "nextTutorial");

            List<String> prereqs = new ArrayList<>();
            JsonNode prereqNode = root.get("recommendedPrerequisites");
            if (prereqNode != null && prereqNode.isArray()) {
                for (JsonNode p : prereqNode) {
                    prereqs.add(p.asText());
                }
            }

            // Resolve step .md paths relative to the directory containing the JSON
            String dir = jsonPath.contains("/")
                    ? jsonPath.substring(0, jsonPath.lastIndexOf('/') + 1)
                    : "";

            List<TutorialContent.Step> steps = new ArrayList<>();
            for (JsonNode stepNode : root.get("steps")) {
                String stepTitle = stepNode.get("title").asText();
                String contentPath = stepNode.get("content").asText();
                String markdown = readResource(BASE_PATH + dir + contentPath);
                steps.add(new TutorialContent.Step(stepTitle, markdown));
            }

            return new TutorialContent(
                    id, title, description, difficulty, estimatedMinutes,
                    model, List.copyOf(prereqs), List.copyOf(steps), nextTutorial);

        } catch (IOException e) {
            throw new UncheckedIOException("Failed to load tutorial: " + jsonPath, e);
        }
    }

    private static InputStream resourceStream(String path) {
        InputStream is = TutorialContentLoader.class.getClassLoader()
                .getResourceAsStream(path);
        if (is == null) {
            throw new UncheckedIOException(
                    new IOException("Tutorial resource not found: " + path));
        }
        return is;
    }

    private static String readResource(String path) {
        try (InputStream is = resourceStream(path)) {
            return new String(is.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException(
                    "Failed to read tutorial step: " + path, e);
        }
    }

    private static String jsonText(JsonNode root, String field) {
        JsonNode node = root.get(field);
        if (node == null || node.isNull()) {
            return null;
        }
        return node.asText();
    }
}
