package systems.courant.sd.app;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.prefs.Preferences;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("TutorialProgressStore")
class TutorialProgressStoreTest {

    private Preferences testPrefs;

    @BeforeEach
    void setUp() throws Exception {
        testPrefs = Preferences.userRoot().node("/test/courant/tutorial-progress");
        testPrefs.clear();
        TutorialProgressStore.setPreferences(testPrefs);
    }

    @AfterEach
    void tearDown() throws Exception {
        testPrefs.removeNode();
        TutorialProgressStore.restoreDefaultPreferences();
    }

    @Nested
    @DisplayName("Completion tracking")
    class Completion {

        @Test
        @DisplayName("should return false when tutorial not completed")
        void shouldReturnFalseWhenNotCompleted() {
            assertThat(TutorialProgressStore.isCompleted("first-model")).isFalse();
        }

        @Test
        @DisplayName("should mark tutorial as completed")
        void shouldMarkTutorialCompleted() {
            TutorialProgressStore.markCompleted("first-model");

            assertThat(TutorialProgressStore.isCompleted("first-model")).isTrue();
        }

        @Test
        @DisplayName("should track multiple completions independently")
        void shouldTrackMultipleCompletions() {
            TutorialProgressStore.markCompleted("first-model");
            TutorialProgressStore.markCompleted("sir-epidemic");

            assertThat(TutorialProgressStore.isCompleted("first-model")).isTrue();
            assertThat(TutorialProgressStore.isCompleted("sir-epidemic")).isTrue();
            assertThat(TutorialProgressStore.isCompleted("supply-chain")).isFalse();
        }

        @Test
        @DisplayName("should handle duplicate completions idempotently")
        void shouldHandleDuplicateCompletions() {
            TutorialProgressStore.markCompleted("first-model");
            TutorialProgressStore.markCompleted("first-model");

            Set<String> completed = TutorialProgressStore.getCompleted();
            assertThat(completed).containsExactly("first-model");
        }

        @Test
        @DisplayName("should return all completed tutorial IDs")
        void shouldReturnAllCompleted() {
            TutorialProgressStore.markCompleted("first-model");
            TutorialProgressStore.markCompleted("sir-epidemic");
            TutorialProgressStore.markCompleted("cld-basics");

            assertThat(TutorialProgressStore.getCompleted())
                    .containsExactly("first-model", "sir-epidemic", "cld-basics");
        }

        @Test
        @DisplayName("should return empty set when nothing completed")
        void shouldReturnEmptySetWhenNothingCompleted() {
            assertThat(TutorialProgressStore.getCompleted()).isEmpty();
        }

        @Test
        @DisplayName("should count completed tutorials from a given set")
        void shouldReturnCompletedCountForGivenIds() {
            TutorialProgressStore.markCompleted("first-model");
            TutorialProgressStore.markCompleted("sir-epidemic");

            List<String> tierTutorials = List.of(
                    "first-model", "sir-epidemic", "cld-basics", "supply-chain");

            assertThat(TutorialProgressStore.getCompletedCount(tierTutorials))
                    .isEqualTo(2);
        }

        @Test
        @DisplayName("should return zero count when none in tier completed")
        void shouldReturnZeroCountWhenNoneCompleted() {
            TutorialProgressStore.markCompleted("first-model");

            List<String> otherTier = List.of("sir-epidemic", "cld-basics");

            assertThat(TutorialProgressStore.getCompletedCount(otherTier))
                    .isZero();
        }
    }

    @Nested
    @DisplayName("Resume point")
    class Resume {

        @Test
        @DisplayName("should return empty when no resume point set")
        void shouldReturnEmptyWhenNoResumePoint() {
            assertThat(TutorialProgressStore.getResumePoint()).isEmpty();
        }

        @Test
        @DisplayName("should set and get resume point")
        void shouldSetAndGetResumePoint() {
            TutorialProgressStore.setResumePoint("sir-epidemic", 3);

            Optional<TutorialProgressStore.ResumePoint> rp =
                    TutorialProgressStore.getResumePoint();

            assertThat(rp).isPresent();
            assertThat(rp.get().tutorialId()).isEqualTo("sir-epidemic");
            assertThat(rp.get().stepIndex()).isEqualTo(3);
        }

        @Test
        @DisplayName("should overwrite previous resume point")
        void shouldOverwritePreviousResumePoint() {
            TutorialProgressStore.setResumePoint("sir-epidemic", 2);
            TutorialProgressStore.setResumePoint("supply-chain", 5);

            Optional<TutorialProgressStore.ResumePoint> rp =
                    TutorialProgressStore.getResumePoint();

            assertThat(rp).isPresent();
            assertThat(rp.get().tutorialId()).isEqualTo("supply-chain");
            assertThat(rp.get().stepIndex()).isEqualTo(5);
        }

        @Test
        @DisplayName("should clear resume point")
        void shouldClearResumePoint() {
            TutorialProgressStore.setResumePoint("sir-epidemic", 3);
            TutorialProgressStore.clearResumePoint();

            assertThat(TutorialProgressStore.getResumePoint()).isEmpty();
        }

        @Test
        @DisplayName("should handle resume point at step zero")
        void shouldHandleResumePointAtStepZero() {
            TutorialProgressStore.setResumePoint("first-model", 0);

            Optional<TutorialProgressStore.ResumePoint> rp =
                    TutorialProgressStore.getResumePoint();

            assertThat(rp).isPresent();
            assertThat(rp.get().stepIndex()).isZero();
        }
    }

    @Nested
    @DisplayName("Reset")
    class ResetProgress {

        @Test
        @DisplayName("should clear all completions and resume point")
        void shouldResetAllProgress() {
            TutorialProgressStore.markCompleted("first-model");
            TutorialProgressStore.markCompleted("sir-epidemic");
            TutorialProgressStore.setResumePoint("supply-chain", 4);

            TutorialProgressStore.resetProgress();

            assertThat(TutorialProgressStore.getCompleted()).isEmpty();
            assertThat(TutorialProgressStore.getResumePoint()).isEmpty();
        }

        @Test
        @DisplayName("should be safe to call when already empty")
        void shouldBeSafeWhenAlreadyEmpty() {
            TutorialProgressStore.resetProgress();

            assertThat(TutorialProgressStore.getCompleted()).isEmpty();
            assertThat(TutorialProgressStore.getResumePoint()).isEmpty();
        }
    }
}
