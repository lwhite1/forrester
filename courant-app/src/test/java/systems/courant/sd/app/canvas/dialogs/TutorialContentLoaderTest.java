package systems.courant.sd.app.canvas.dialogs;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.io.UncheckedIOException;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("TutorialContentLoader")
class TutorialContentLoaderTest {

    @Nested
    @DisplayName("Loading first-model tutorial")
    class FirstModel {

        private final TutorialContent content =
                TutorialContentLoader.load("modeling/first-model.json");

        @Test
        @DisplayName("should load tutorial ID")
        void shouldLoadId() {
            assertThat(content.id()).isEqualTo("first-model");
        }

        @Test
        @DisplayName("should load title")
        void shouldLoadTitle() {
            assertThat(content.title()).isEqualTo("Your First Model");
        }

        @Test
        @DisplayName("should load difficulty")
        void shouldLoadDifficulty() {
            assertThat(content.difficulty()).isEqualTo("beginner");
        }

        @Test
        @DisplayName("should load estimated minutes")
        void shouldLoadEstimatedMinutes() {
            assertThat(content.estimatedMinutes()).isEqualTo(15);
        }

        @Test
        @DisplayName("should load model path")
        void shouldLoadModel() {
            assertThat(content.model()).isEqualTo("introductory/coffee-cooling.json");
        }

        @Test
        @DisplayName("should load all 6 steps")
        void shouldLoadAllSteps() {
            assertThat(content.steps()).hasSize(6);
        }

        @Test
        @DisplayName("step titles should match JSON metadata")
        void shouldLoadStepTitles() {
            List<String> titles = content.steps().stream()
                    .map(TutorialContent.Step::title)
                    .toList();
            assertThat(titles).containsExactly(
                    "The Idea", "Place Elements", "Connect & Equate",
                    "Simulate", "Experiment", "Next Steps");
        }

        @Test
        @DisplayName("step content should be non-empty Markdown")
        void shouldLoadStepContent() {
            for (TutorialContent.Step step : content.steps()) {
                assertThat(step.markdown()).isNotBlank();
            }
        }

        @Test
        @DisplayName("should load next tutorial reference")
        void shouldLoadNextTutorial() {
            assertThat(content.nextTutorial()).isEqualTo("stocks-flows");
        }
    }

    @Nested
    @DisplayName("Loading all tutorials")
    class AllTutorials {

        @Test
        @DisplayName("feedback-loops loads with 7 steps")
        void feedbackLoadsCorrectly() {
            TutorialContent content =
                    TutorialContentLoader.load("modeling/feedback-loops.json");
            assertThat(content.id()).isEqualTo("feedback-loops");
            assertThat(content.steps()).hasSize(7);
        }

        @Test
        @DisplayName("delays loads with 7 steps")
        void delaysLoadsCorrectly() {
            TutorialContent content =
                    TutorialContentLoader.load("modeling/delays.json");
            assertThat(content.id()).isEqualTo("delays");
            assertThat(content.steps()).hasSize(7);
        }

        @Test
        @DisplayName("cld-basics loads with 7 steps")
        void cldBasicsLoadsCorrectly() {
            TutorialContent content =
                    TutorialContentLoader.load("modeling/cld-basics.json");
            assertThat(content.id()).isEqualTo("cld-basics");
            assertThat(content.steps()).hasSize(7);
        }
    }

    @Nested
    @DisplayName("Error handling")
    class ErrorHandling {

        @Test
        @DisplayName("should throw on missing tutorial JSON")
        void shouldThrowOnMissingJson() {
            assertThatThrownBy(() -> TutorialContentLoader.load("nonexistent.json"))
                    .isInstanceOf(UncheckedIOException.class)
                    .hasMessageContaining("nonexistent.json");
        }
    }
}
