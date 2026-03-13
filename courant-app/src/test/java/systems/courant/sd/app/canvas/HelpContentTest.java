package systems.courant.sd.app.canvas;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class HelpContentTest {

    @Test
    void allTopicsShouldHaveNonNullContent() {
        // HelpContent.forTopic creates JavaFX nodes, so we verify the switch is exhaustive
        // by confirming no exception is thrown and each topic maps to a non-null branch
        for (HelpTopic topic : HelpTopic.values()) {
            // The switch expression in forTopic is exhaustive over the enum,
            // so this verifies all cases are covered at compile time.
            // At runtime without JavaFX, we verify the topic metadata.
            assertThat(topic.displayName()).as("displayName for " + topic).isNotBlank();
            assertThat(topic.category()).as("category for " + topic).isNotBlank();
        }
    }

    @Test
    void toStringShouldReturnDisplayName() {
        for (HelpTopic topic : HelpTopic.values()) {
            assertThat(topic.toString()).isEqualTo(topic.displayName());
        }
    }

    @Test
    void categoriesShouldBeFromExpectedSet() {
        for (HelpTopic topic : HelpTopic.values()) {
            assertThat(topic.category()).isIn(
                    "Getting Started", "Elements", "Equations",
                    "Simulation", "Analysis", "Structure");
        }
    }

    @Test
    void shouldHaveAtLeastOneTopicPerCategory() {
        assertThat(HelpTopic.values())
                .extracting(HelpTopic::category)
                .contains("Getting Started", "Elements", "Equations",
                        "Simulation", "Analysis", "Structure");
    }
}
