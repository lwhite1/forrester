package systems.courant.sd.app.canvas;

import javafx.application.Platform;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.StackPane;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import javafx.stage.Stage;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.testfx.api.FxRobot;
import org.testfx.framework.junit5.ApplicationExtension;
import org.testfx.framework.junit5.Start;
import org.testfx.util.WaitForAsyncUtils;

import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("HelpContent (TestFX)")
@ExtendWith(ApplicationExtension.class)
class HelpContentFxTest {

    @Start
    void start(Stage stage) {
        stage.setScene(new Scene(new StackPane(), 100, 100));
        stage.show();
    }

    @ParameterizedTest(name = "forTopic({0}) returns non-null ScrollPane with TextFlow content")
    @EnumSource(HelpTopic.class)
    @DisplayName("forTopic returns valid content for every HelpTopic")
    void forTopicReturnsValidContent(HelpTopic topic, FxRobot robot) {
        AtomicReference<Node> result = new AtomicReference<>();

        Platform.runLater(() -> result.set(HelpContent.forTopic(topic)));
        WaitForAsyncUtils.waitForFxEvents();

        Node node = result.get();
        assertThat(node).isNotNull();
        assertThat(node).isInstanceOf(ScrollPane.class);

        ScrollPane scroll = (ScrollPane) node;
        assertThat(scroll.getContent()).isInstanceOf(TextFlow.class);

        TextFlow textFlow = (TextFlow) scroll.getContent();
        assertThat(textFlow.getChildren()).isNotEmpty();
    }

    @Test
    @DisplayName("Simulation settings help matches dialog field names and order (#777)")
    void simulationSettingsHelpMatchesDialog(FxRobot robot) {
        AtomicReference<Node> result = new AtomicReference<>();
        Platform.runLater(() -> result.set(HelpContent.forTopic(HelpTopic.SIMULATION_SETTINGS)));
        WaitForAsyncUtils.waitForFxEvents();

        ScrollPane scroll = (ScrollPane) result.get();
        TextFlow textFlow = (TextFlow) scroll.getContent();

        // Extract all text from the TextFlow
        String fullText = textFlow.getChildren().stream()
                .filter(n -> n instanceof Text)
                .map(n -> ((Text) n).getText())
                .reduce("", String::concat);

        // Verify all dialog fields are mentioned
        assertThat(fullText).contains("Time Step");
        assertThat(fullText).contains("Duration Unit");
        assertThat(fullText).contains("Strict Mode");
        assertThat(fullText).contains("Save Per");

        // Verify old incorrect field names are NOT present
        assertThat(fullText).doesNotContain("Start Time");
        assertThat(fullText).doesNotContain("Stop Time");

        // Verify field order matches dialog: Time Step, DT, Save Per, Duration, Duration Unit, Strict Mode
        int timeStepIdx = fullText.indexOf("Time Step");
        int dtIdx = fullText.indexOf("DT");
        int savePerIdx = fullText.indexOf("Save Per");
        int durationIdx = fullText.indexOf("\nDuration");
        int durationUnitIdx = fullText.indexOf("Duration Unit");
        int strictModeIdx = fullText.indexOf("Strict Mode");

        assertThat(timeStepIdx).isLessThan(dtIdx);
        assertThat(dtIdx).isLessThan(savePerIdx);
        assertThat(savePerIdx).isLessThan(durationIdx);
        assertThat(durationUnitIdx).isLessThan(strictModeIdx);
    }
}
