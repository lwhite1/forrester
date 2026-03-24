package systems.courant.sd.app;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Optional;
import java.util.Set;
import java.util.prefs.Preferences;

/**
 * Persists tutorial completion and resume state across sessions using
 * Java Preferences. Mirrors the pattern established by {@link LastDirectoryStore}.
 *
 * <p>Completion is stored as a comma-separated set of tutorial IDs.
 * The resume point records the user's last-viewed tutorial and step index
 * so the dialog can reopen at the same position.
 */
public final class TutorialProgressStore {

    private static final Preferences DEFAULT_PREFS =
            Preferences.userNodeForPackage(TutorialProgressStore.class);

    private static Preferences prefs = DEFAULT_PREFS;

    private static final String KEY_COMPLETED = "completedTutorials";
    private static final String KEY_RESUME_TUTORIAL = "resumeTutorialId";
    private static final String KEY_RESUME_STEP = "resumeStepIndex";

    private TutorialProgressStore() {
    }

    /**
     * A snapshot of the user's position within a tutorial.
     */
    public record ResumePoint(String tutorialId, int stepIndex) {
    }

    /**
     * Marks a tutorial as completed. Duplicate calls are idempotent.
     */
    public static void markCompleted(String tutorialId) {
        Set<String> completed = new LinkedHashSet<>(getCompleted());
        completed.add(tutorialId);
        prefs.put(KEY_COMPLETED, String.join(",", completed));
    }

    /**
     * Returns whether the given tutorial has been completed.
     */
    public static boolean isCompleted(String tutorialId) {
        return getCompleted().contains(tutorialId);
    }

    /**
     * Returns all completed tutorial IDs in the order they were completed.
     */
    public static Set<String> getCompleted() {
        String raw = prefs.get(KEY_COMPLETED, "");
        if (raw.isEmpty()) {
            return Set.of();
        }
        return Collections.unmodifiableSet(
                new LinkedHashSet<>(Arrays.asList(raw.split(","))));
    }

    /**
     * Counts how many of the given tutorial IDs have been completed.
     * Callers (e.g., the curriculum browser) pass in a tier's tutorial
     * IDs to compute tier-level progress.
     */
    public static int getCompletedCount(Iterable<String> tutorialIds) {
        Set<String> completed = getCompleted();
        int count = 0;
        for (String id : tutorialIds) {
            if (completed.contains(id)) {
                count++;
            }
        }
        return count;
    }

    /**
     * Returns the user's last resume point, if any.
     */
    public static Optional<ResumePoint> getResumePoint() {
        String tutorialId = prefs.get(KEY_RESUME_TUTORIAL, "");
        if (tutorialId.isEmpty()) {
            return Optional.empty();
        }
        int stepIndex = prefs.getInt(KEY_RESUME_STEP, 0);
        return Optional.of(new ResumePoint(tutorialId, stepIndex));
    }

    /**
     * Records the user's current position within a tutorial for later resume.
     */
    public static void setResumePoint(String tutorialId, int stepIndex) {
        prefs.put(KEY_RESUME_TUTORIAL, tutorialId);
        prefs.putInt(KEY_RESUME_STEP, stepIndex);
    }

    /**
     * Clears the resume point (e.g., when a tutorial is completed).
     */
    public static void clearResumePoint() {
        prefs.remove(KEY_RESUME_TUTORIAL);
        prefs.remove(KEY_RESUME_STEP);
    }

    /**
     * Resets all progress — completions and resume point.
     */
    public static void resetProgress() {
        prefs.remove(KEY_COMPLETED);
        prefs.remove(KEY_RESUME_TUTORIAL);
        prefs.remove(KEY_RESUME_STEP);
    }

    /** Replaces the Preferences backing store. Package-private for testing. */
    static void setPreferences(Preferences testPrefs) {
        prefs = testPrefs;
    }

    /** Restores the production Preferences node. Package-private for testing. */
    static void restoreDefaultPreferences() {
        prefs = DEFAULT_PREFS;
    }
}
