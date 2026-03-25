package systems.courant.sd.app.canvas.dialogs;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Loads the curriculum structure from {@code tutorials/curriculum.json}.
 * Returns a list of {@link Track} records containing tiers and tutorial IDs.
 * Individual tutorial metadata is loaded separately by {@link TutorialContentLoader}.
 */
public final class CurriculumLoader {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final String CURRICULUM_PATH = "tutorials/curriculum.json";

    private CurriculumLoader() {
    }

    public record Track(String id, String name, List<Tier> tiers) {
    }

    public record Tier(String id, String name, String description,
                       List<String> recommendedPrerequisites,
                       List<String> tutorialIds) {
    }

    /**
     * Loads all tracks and tiers from the curriculum manifest.
     */
    public static List<Track> load() {
        try (InputStream is = CurriculumLoader.class.getClassLoader()
                .getResourceAsStream(CURRICULUM_PATH)) {
            if (is == null) {
                throw new IOException("Curriculum manifest not found: " + CURRICULUM_PATH);
            }
            JsonNode root = MAPPER.readTree(is);
            List<Track> tracks = new ArrayList<>();
            for (JsonNode trackNode : root.get("tracks")) {
                List<Tier> tiers = new ArrayList<>();
                for (JsonNode tierNode : trackNode.get("tiers")) {
                    List<String> prereqs = new ArrayList<>();
                    for (JsonNode p : tierNode.get("recommendedPrerequisites")) {
                        prereqs.add(p.asText());
                    }
                    List<String> tutorialIds = new ArrayList<>();
                    for (JsonNode t : tierNode.get("tutorials")) {
                        tutorialIds.add(t.asText());
                    }
                    tiers.add(new Tier(
                            tierNode.get("id").asText(),
                            tierNode.get("name").asText(),
                            tierNode.get("description").asText(),
                            List.copyOf(prereqs),
                            List.copyOf(tutorialIds)));
                }
                tracks.add(new Track(
                        trackNode.get("id").asText(),
                        trackNode.get("name").asText(),
                        List.copyOf(tiers)));
            }
            return List.copyOf(tracks);
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to load curriculum", e);
        }
    }
}
