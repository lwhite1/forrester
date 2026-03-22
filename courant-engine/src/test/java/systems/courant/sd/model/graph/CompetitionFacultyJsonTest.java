package systems.courant.sd.model.graph;

import systems.courant.sd.io.json.ModelDefinitionSerializer;
import systems.courant.sd.model.def.ModelDefinition;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration test: loads the actual CompetitionFaculty JSON and verifies loop detection.
 * Regression test for #1208.
 */
class CompetitionFacultyJsonTest {

    @Test
    void shouldDetectLoopsInCompetitionFacultyJson() throws IOException {
        Path jsonPath = Path.of("../courant-app/src/main/resources/models/causal-loop/0201CompetitionFaculty.json");
        String json = Files.readString(jsonPath);

        ModelDefinitionSerializer serializer = new ModelDefinitionSerializer();
        ModelDefinition def = serializer.fromJson(json);

        assertThat(def.cldVariables()).as("CLD variables loaded").hasSize(8);
        assertThat(def.causalLinks()).as("causal links loaded").hasSize(10);

        FeedbackAnalysis analysis = FeedbackAnalysis.analyze(def);

        assertThat(analysis.loopCount()).as("should detect feedback loops").isGreaterThan(0);
        assertThat(analysis.causalLoops()).allSatisfy(loop ->
                assertThat(loop.type()).isEqualTo(FeedbackAnalysis.LoopType.REINFORCING));
    }
}
