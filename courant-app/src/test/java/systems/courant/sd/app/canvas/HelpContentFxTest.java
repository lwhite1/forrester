package systems.courant.sd.app.canvas;

import javafx.application.Platform;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.StackPane;
import javafx.scene.text.TextFlow;
import javafx.stage.Stage;

import org.junit.jupiter.api.DisplayName;
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
}
