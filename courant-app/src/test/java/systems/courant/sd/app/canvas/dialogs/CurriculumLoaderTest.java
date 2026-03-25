package systems.courant.sd.app.canvas.dialogs;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("CurriculumLoader")
class CurriculumLoaderTest {

    private final List<CurriculumLoader.Track> tracks = CurriculumLoader.load();

    @Test
    @DisplayName("should load two tracks")
    void shouldLoadTwoTracks() {
        assertThat(tracks).hasSize(2);
    }

    @Test
    @DisplayName("first track is modeling")
    void firstTrackIsModeling() {
        assertThat(tracks.getFirst().id()).isEqualTo("modeling");
        assertThat(tracks.getFirst().name()).isEqualTo("System Dynamics Modeling");
    }

    @Test
    @DisplayName("second track is simulation tools")
    void secondTrackIsSimulationTools() {
        assertThat(tracks.get(1).id()).isEqualTo("simulation-tools");
    }

    @Test
    @DisplayName("modeling track has three tiers")
    void modelingTrackHasThreeTiers() {
        assertThat(tracks.getFirst().tiers()).hasSize(3);
    }

    @Test
    @DisplayName("foundations tier has three tutorials")
    void foundationsTierHasThreeTutorials() {
        CurriculumLoader.Tier foundations = tracks.getFirst().tiers().getFirst();
        assertThat(foundations.id()).isEqualTo("modeling-foundations");
        assertThat(foundations.tutorialIds()).containsExactly(
                "first-model", "feedback-loops", "cld-basics");
    }

    @Test
    @DisplayName("intermediate tier has one tutorial")
    void intermediateTierHasOneTutorial() {
        CurriculumLoader.Tier intermediate = tracks.getFirst().tiers().get(1);
        assertThat(intermediate.tutorialIds()).containsExactly("supply-chain");
    }

    @Test
    @DisplayName("advanced tier is empty")
    void advancedTierIsEmpty() {
        CurriculumLoader.Tier advanced = tracks.getFirst().tiers().get(2);
        assertThat(advanced.tutorialIds()).isEmpty();
    }

    @Test
    @DisplayName("tiers have descriptions")
    void tiersHaveDescriptions() {
        for (CurriculumLoader.Track track : tracks) {
            for (CurriculumLoader.Tier tier : track.tiers()) {
                assertThat(tier.description()).isNotBlank();
            }
        }
    }

    @Test
    @DisplayName("intermediate tier has foundations as prerequisite")
    void intermediateTierHasPrerequisite() {
        CurriculumLoader.Tier intermediate = tracks.getFirst().tiers().get(1);
        assertThat(intermediate.recommendedPrerequisites())
                .containsExactly("modeling-foundations");
    }
}
