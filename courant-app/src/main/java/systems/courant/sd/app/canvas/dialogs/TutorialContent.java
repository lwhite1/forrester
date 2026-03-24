package systems.courant.sd.app.canvas.dialogs;

import java.util.List;

/**
 * Parsed tutorial metadata and step content loaded from
 * the {@code tutorials/} resource directory.
 *
 * @param id                        unique kebab-case identifier (e.g., "first-model")
 * @param title                     display title
 * @param description               short description for the curriculum browser
 * @param difficulty                 "beginner", "intermediate", or "advanced"
 * @param estimatedMinutes          estimated completion time
 * @param model                     path to a shipped example model, or {@code null}
 * @param recommendedPrerequisites  tutorial IDs the user should complete first
 * @param steps                     ordered step content (title + raw Markdown)
 * @param nextTutorial              suggested next tutorial ID, or {@code null}
 */
public record TutorialContent(
        String id,
        String title,
        String description,
        String difficulty,
        int estimatedMinutes,
        String model,
        List<String> recommendedPrerequisites,
        List<Step> steps,
        String nextTutorial
) {

    /**
     * A single step within a tutorial.
     *
     * @param title    tab/step title
     * @param markdown raw Markdown content
     */
    public record Step(String title, String markdown) {
    }
}
