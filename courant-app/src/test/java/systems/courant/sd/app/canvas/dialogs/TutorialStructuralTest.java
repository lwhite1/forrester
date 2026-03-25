package systems.courant.sd.app.canvas.dialogs;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestFactory;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Tutorial structural validation")
class TutorialStructuralTest {

    private final List<CurriculumLoader.Track> tracks = CurriculumLoader.load();

    // ── Helpers ──────────────────────────────────────────────────────

    record LoadedTutorial(String id, String trackId, String jsonPath, TutorialContent content) {
    }

    private List<LoadedTutorial> loadAllTutorials() {
        List<LoadedTutorial> result = new ArrayList<>();
        for (CurriculumLoader.Track track : tracks) {
            for (CurriculumLoader.Tier tier : track.tiers()) {
                for (String tutorialId : tier.tutorialIds()) {
                    String path = track.id() + "/" + tutorialId + ".json";
                    TutorialContent content = TutorialContentLoader.load(path);
                    result.add(new LoadedTutorial(tutorialId, track.id(), path, content));
                }
            }
        }
        return result;
    }

    /**
     * Collects every tutorial ID that appears anywhere in the curriculum
     * (across all tracks and tiers).
     */
    private Set<String> allCurriculumTutorialIds() {
        Set<String> ids = new HashSet<>();
        for (CurriculumLoader.Track track : tracks) {
            for (CurriculumLoader.Tier tier : track.tiers()) {
                ids.addAll(tier.tutorialIds());
            }
        }
        return ids;
    }

    // ── Curriculum integrity ─────────────────────────────────────────

    @Nested
    @DisplayName("Curriculum integrity")
    class CurriculumIntegrity {

        @TestFactory
        @DisplayName("all tutorial IDs in curriculum load successfully")
        Stream<DynamicTest> allTutorialIdsInCurriculumLoadSuccessfully() {
            List<DynamicTest> tests = new ArrayList<>();
            for (CurriculumLoader.Track track : tracks) {
                for (CurriculumLoader.Tier tier : track.tiers()) {
                    for (String tutorialId : tier.tutorialIds()) {
                        String path = track.id() + "/" + tutorialId + ".json";
                        String name = track.id() + " / " + tier.id() + " / " + tutorialId;
                        tests.add(DynamicTest.dynamicTest(name, () -> {
                            TutorialContent content = TutorialContentLoader.load(path);
                            assertThat(content).as("loaded tutorial for %s", path).isNotNull();
                            assertThat(content.id()).isEqualTo(tutorialId);
                        }));
                    }
                }
            }
            return tests.stream();
        }

        @Test
        @DisplayName("no duplicate tutorial IDs across tiers")
        void noDuplicateTutorialIds() {
            Set<String> seen = new HashSet<>();
            List<String> duplicates = new ArrayList<>();
            for (CurriculumLoader.Track track : tracks) {
                for (CurriculumLoader.Tier tier : track.tiers()) {
                    for (String tutorialId : tier.tutorialIds()) {
                        if (!seen.add(tutorialId)) {
                            duplicates.add(tutorialId + " (in " + tier.id() + ")");
                        }
                    }
                }
            }
            assertThat(duplicates)
                    .as("tutorial IDs appearing in more than one tier")
                    .isEmpty();
        }

        @TestFactory
        @DisplayName("all tiers have at least one tutorial")
        Stream<DynamicTest> allTiersHaveAtLeastOneTutorial() {
            List<DynamicTest> tests = new ArrayList<>();
            for (CurriculumLoader.Track track : tracks) {
                for (CurriculumLoader.Tier tier : track.tiers()) {
                    String name = track.id() + " / " + tier.id();
                    tests.add(DynamicTest.dynamicTest(name, () ->
                            assertThat(tier.tutorialIds())
                                    .as("tutorials in tier %s", tier.id())
                                    .isNotEmpty()));
                }
            }
            return tests.stream();
        }
    }

    // ── Step content ────────────────────────────────────────────────

    @Nested
    @DisplayName("Step content")
    class StepContent {

        @TestFactory
        @DisplayName("all steps have non-empty markdown")
        Stream<DynamicTest> allStepsHaveNonEmptyMarkdown() {
            return loadAllTutorials().stream().flatMap(tutorial ->
                    tutorial.content().steps().stream().map(step -> {
                        String name = tutorial.id() + " / " + step.title();
                        return DynamicTest.dynamicTest(name, () ->
                                assertThat(step.markdown())
                                        .as("markdown for step '%s' in tutorial '%s'",
                                                step.title(), tutorial.id())
                                        .isNotBlank());
                    }));
        }

        @TestFactory
        @DisplayName("all steps have non-empty titles")
        Stream<DynamicTest> allStepsHaveNonEmptyTitles() {
            return loadAllTutorials().stream().flatMap(tutorial -> {
                List<DynamicTest> stepTests = new ArrayList<>();
                List<TutorialContent.Step> steps = tutorial.content().steps();
                for (int i = 0; i < steps.size(); i++) {
                    TutorialContent.Step step = steps.get(i);
                    int stepNumber = i + 1;
                    String name = tutorial.id() + " / step " + stepNumber;
                    stepTests.add(DynamicTest.dynamicTest(name, () ->
                            assertThat(step.title())
                                    .as("title of step %d in tutorial '%s'",
                                            stepNumber, tutorial.id())
                                    .isNotBlank()));
                }
                return stepTests.stream();
            });
        }

        @TestFactory
        @DisplayName("all tutorials have at least two steps")
        Stream<DynamicTest> allTutorialsHaveAtLeastTwoSteps() {
            return loadAllTutorials().stream().map(tutorial ->
                    DynamicTest.dynamicTest(tutorial.id(), () ->
                            assertThat(tutorial.content().steps())
                                    .as("steps in tutorial '%s'", tutorial.id())
                                    .hasSizeGreaterThanOrEqualTo(2)));
        }
    }

    // ── Model references ────────────────────────────────────────────

    @Nested
    @DisplayName("Model references")
    class ModelReferences {

        @TestFactory
        @DisplayName("all model references point to existing files")
        Stream<DynamicTest> allModelReferencesPointToExistingFiles() {
            return loadAllTutorials().stream()
                    .filter(tutorial -> tutorial.content().model() != null)
                    .map(tutorial -> {
                        String model = tutorial.content().model();
                        String name = tutorial.id() + " -> " + model;
                        return DynamicTest.dynamicTest(name, () -> {
                            String resourcePath = "/models/" + model;
                            try (InputStream is = getClass().getResourceAsStream(resourcePath)) {
                                assertThat(is)
                                        .as("model resource '%s' referenced by tutorial '%s'",
                                                resourcePath, tutorial.id())
                                        .isNotNull();
                            }
                        });
                    });
        }
    }

    // ── Next tutorial chain ─────────────────────────────────────────

    @Nested
    @DisplayName("Next tutorial chain")
    class NextTutorialChain {

        @TestFactory
        @DisplayName("all nextTutorial references are valid")
        Stream<DynamicTest> allNextTutorialReferencesAreValid() {
            Set<String> allIds = allCurriculumTutorialIds();
            return loadAllTutorials().stream()
                    .filter(tutorial -> tutorial.content().nextTutorial() != null)
                    .map(tutorial -> {
                        String next = tutorial.content().nextTutorial();
                        String name = tutorial.id() + " -> " + next;
                        return DynamicTest.dynamicTest(name, () ->
                                assertThat(allIds)
                                        .as("nextTutorial '%s' referenced by '%s' must exist in curriculum",
                                                next, tutorial.id())
                                        .contains(next));
                    });
        }

        @TestFactory
        @DisplayName("no circular nextTutorial chains")
        Stream<DynamicTest> noCircularNextTutorialChains() {
            List<LoadedTutorial> all = loadAllTutorials();

            // Build a map from tutorial ID to its nextTutorial
            java.util.Map<String, String> nextMap = new java.util.HashMap<>();
            for (LoadedTutorial tutorial : all) {
                if (tutorial.content().nextTutorial() != null) {
                    nextMap.put(tutorial.id(), tutorial.content().nextTutorial());
                }
            }

            return all.stream().map(tutorial ->
                    DynamicTest.dynamicTest(tutorial.id(), () -> {
                        Set<String> visited = new HashSet<>();
                        String current = tutorial.id();
                        while (current != null && nextMap.containsKey(current)) {
                            assertThat(visited.add(current))
                                    .as("circular chain detected at '%s' starting from '%s'",
                                            current, tutorial.id())
                                    .isTrue();
                            current = nextMap.get(current);
                        }
                    }));
        }
    }

    // ── Metadata quality ────────────────────────────────────────────

    @Nested
    @DisplayName("Metadata quality")
    class MetadataQuality {

        private static final Set<String> VALID_DIFFICULTIES =
                Set.of("beginner", "intermediate", "advanced");

        @TestFactory
        @DisplayName("all tutorials have valid difficulty")
        Stream<DynamicTest> allTutorialsHaveDifficulty() {
            return loadAllTutorials().stream().map(tutorial ->
                    DynamicTest.dynamicTest(tutorial.id(), () ->
                            assertThat(tutorial.content().difficulty())
                                    .as("difficulty of tutorial '%s'", tutorial.id())
                                    .isIn(VALID_DIFFICULTIES)));
        }

        @TestFactory
        @DisplayName("all tutorials have positive estimatedMinutes")
        Stream<DynamicTest> allTutorialsHavePositiveEstimatedMinutes() {
            return loadAllTutorials().stream().map(tutorial ->
                    DynamicTest.dynamicTest(tutorial.id(), () ->
                            assertThat(tutorial.content().estimatedMinutes())
                                    .as("estimatedMinutes of tutorial '%s'", tutorial.id())
                                    .isPositive()));
        }

        @TestFactory
        @DisplayName("all tutorials have descriptions")
        Stream<DynamicTest> allTutorialsHaveDescriptions() {
            return loadAllTutorials().stream().map(tutorial ->
                    DynamicTest.dynamicTest(tutorial.id(), () ->
                            assertThat(tutorial.content().description())
                                    .as("description of tutorial '%s'", tutorial.id())
                                    .isNotBlank()));
        }
    }
}
