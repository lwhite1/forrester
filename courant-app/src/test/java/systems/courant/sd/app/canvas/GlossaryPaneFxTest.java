package systems.courant.sd.app.canvas;

import javafx.application.Platform;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.TextFlow;
import javafx.stage.Stage;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.testfx.framework.junit5.ApplicationExtension;
import org.testfx.framework.junit5.Start;
import org.testfx.util.WaitForAsyncUtils;

import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("GlossaryPane (#1331)")
@ExtendWith(ApplicationExtension.class)
class GlossaryPaneFxTest {

    private GlossaryPane glossaryPane;

    @Start
    void start(Stage stage) {
        glossaryPane = new GlossaryPane();
        stage.setScene(new Scene(new StackPane(glossaryPane), 600, 400));
        stage.show();
    }

    @Test
    @DisplayName("should have a search field")
    void shouldHaveSearchField() {
        TextField search = (TextField) glossaryPane.lookup("#glossarySearch");
        assertThat(search).isNotNull();
        assertThat(search.getPromptText()).contains("Search");
    }

    @Test
    @DisplayName("should have a scroll pane with entries")
    void shouldHaveScrollPane() {
        ScrollPane scroll = (ScrollPane) glossaryPane.lookup("#glossaryScroll");
        assertThat(scroll).isNotNull();
        assertThat(scroll.getContent()).isInstanceOf(VBox.class);
        VBox entriesBox = (VBox) scroll.getContent();
        assertThat(entriesBox.getChildren()).isNotEmpty();
    }

    @Test
    @DisplayName("should display all glossary entries by default")
    void shouldDisplayAllEntries() {
        ScrollPane scroll = (ScrollPane) glossaryPane.lookup("#glossaryScroll");
        VBox entriesBox = (VBox) scroll.getContent();
        assertThat(entriesBox.getChildren().size())
                .isEqualTo(Glossary.instance().entries().size());
    }

    @Test
    @DisplayName("should filter entries when search text is typed")
    void shouldFilterEntries() {
        Platform.runLater(() -> {
            TextField search = (TextField) glossaryPane.lookup("#glossarySearch");
            search.setText("feedback");
        });
        WaitForAsyncUtils.waitForFxEvents();

        ScrollPane scroll = (ScrollPane) glossaryPane.lookup("#glossaryScroll");
        VBox entriesBox = (VBox) scroll.getContent();
        assertThat(entriesBox.getChildren().size())
                .isLessThan(Glossary.instance().entries().size());
        assertThat(entriesBox.getChildren().size()).isGreaterThan(0);
    }

    @Test
    @DisplayName("should restore all entries when search is cleared")
    void shouldRestoreAllOnClear() {
        Platform.runLater(() -> {
            TextField search = (TextField) glossaryPane.lookup("#glossarySearch");
            search.setText("xyz");
        });
        WaitForAsyncUtils.waitForFxEvents();

        Platform.runLater(() -> {
            TextField search = (TextField) glossaryPane.lookup("#glossarySearch");
            search.clear();
        });
        WaitForAsyncUtils.waitForFxEvents();

        ScrollPane scroll = (ScrollPane) glossaryPane.lookup("#glossaryScroll");
        VBox entriesBox = (VBox) scroll.getContent();
        assertThat(entriesBox.getChildren().size())
                .isEqualTo(Glossary.instance().entries().size());
    }

    @Test
    @DisplayName("showTerm should scroll to and highlight the requested term")
    void showTermShouldHighlight() {
        Platform.runLater(() -> glossaryPane.showTerm("Feedback Loop"));
        WaitForAsyncUtils.waitForFxEvents();

        ScrollPane scroll = (ScrollPane) glossaryPane.lookup("#glossaryScroll");
        VBox entriesBox = (VBox) scroll.getContent();

        boolean found = false;
        for (Node child : entriesBox.getChildren()) {
            if (child instanceof VBox entryBox
                    && "Feedback Loop".equals(entryBox.getUserData())) {
                found = true;
                assertThat(entryBox.getStyle()).contains("fff3cd");
            }
        }
        assertThat(found).as("Feedback Loop entry should exist").isTrue();
    }

    @Test
    @DisplayName("should invoke onTermNavigate callback when a related link is clicked")
    void shouldInvokeNavigateCallback() {
        AtomicReference<String> navigated = new AtomicReference<>();
        Platform.runLater(() -> glossaryPane.setOnTermNavigate(navigated::set));
        WaitForAsyncUtils.waitForFxEvents();

        ScrollPane scroll = (ScrollPane) glossaryPane.lookup("#glossaryScroll");
        VBox entriesBox = (VBox) scroll.getContent();

        Platform.runLater(() -> {
            for (Node child : entriesBox.getChildren()) {
                if (child instanceof VBox entryBox) {
                    for (Node inner : entryBox.getChildren()) {
                        if (inner instanceof TextFlow tf) {
                            for (Node n : tf.getChildren()) {
                                if (n instanceof Hyperlink link) {
                                    link.fire();
                                    return;
                                }
                            }
                        }
                    }
                }
            }
        });
        WaitForAsyncUtils.waitForFxEvents();

        assertThat(navigated.get()).isNotNull();
    }

    @Test
    @DisplayName("annotated help content should contain glossary hyperlinks")
    void helpContentShouldContainGlossaryHyperlinks() {
        AtomicReference<Node> result = new AtomicReference<>();
        Platform.runLater(() -> result.set(
                HelpContent.forTopic(HelpTopic.OVERVIEW, term -> {})));
        WaitForAsyncUtils.waitForFxEvents();

        ScrollPane scroll = (ScrollPane) result.get();
        TextFlow flow = (TextFlow) scroll.getContent();

        boolean hasHyperlink = flow.getChildren().stream()
                .anyMatch(n -> n instanceof Hyperlink);
        assertThat(hasHyperlink)
                .as("Overview help content should contain glossary hyperlinks")
                .isTrue();
    }

    @Test
    @DisplayName("unannotated help content should not contain hyperlinks")
    void helpContentShouldNotContainHyperlinksWithoutCallback() {
        AtomicReference<Node> result = new AtomicReference<>();
        Platform.runLater(() -> result.set(
                HelpContent.forTopic(HelpTopic.OVERVIEW)));
        WaitForAsyncUtils.waitForFxEvents();

        ScrollPane scroll = (ScrollPane) result.get();
        TextFlow flow = (TextFlow) scroll.getContent();

        boolean hasHyperlink = flow.getChildren().stream()
                .anyMatch(n -> n instanceof Hyperlink);
        assertThat(hasHyperlink)
                .as("Unannotated help content should not contain hyperlinks")
                .isFalse();
    }
}
