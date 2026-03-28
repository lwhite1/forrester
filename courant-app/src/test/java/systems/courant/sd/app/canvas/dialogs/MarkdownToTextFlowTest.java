package systems.courant.sd.app.canvas.dialogs;

import javafx.scene.Scene;
import javafx.scene.layout.StackPane;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import javafx.stage.Stage;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.testfx.api.FxRobot;
import org.testfx.framework.junit5.ApplicationExtension;
import org.testfx.framework.junit5.Start;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("MarkdownToTextFlow")
@ExtendWith(ApplicationExtension.class)
class MarkdownToTextFlowTest {

    @Start
    void start(Stage stage) {
        stage.setScene(new Scene(new StackPane(), 100, 100));
        stage.show();
    }

    private List<Text> render(String markdown) {
        TextFlow flow = MarkdownToTextFlow.convert(markdown);
        return flow.getChildren().stream()
                .filter(n -> n instanceof Text)
                .map(n -> (Text) n)
                .toList();
    }

    private String fullText(String markdown) {
        return render(markdown).stream()
                .map(Text::getText)
                .reduce("", String::concat);
    }

    @Nested
    @DisplayName("Inline formatting")
    class InlineFormatting {

        @Test
        @DisplayName("renders bold text")
        void shouldRenderBold(FxRobot robot) {
            List<Text> nodes = render("**bold text**");
            Text bold = nodes.stream()
                    .filter(t -> t.getText().contains("bold text"))
                    .findFirst().orElseThrow();
            assertThat(bold.getStyle()).contains("-fx-font-weight: bold");
        }

        @Test
        @DisplayName("renders italic text")
        void shouldRenderItalic(FxRobot robot) {
            List<Text> nodes = render("*italic text*");
            Text italic = nodes.stream()
                    .filter(t -> t.getText().contains("italic text"))
                    .findFirst().orElseThrow();
            assertThat(italic.getStyle()).contains("-fx-font-style: italic");
        }

        @Test
        @DisplayName("renders inline code")
        void shouldRenderCode(FxRobot robot) {
            List<Text> nodes = render("press `Ctrl+R`");
            Text code = nodes.stream()
                    .filter(t -> t.getText().contains("Ctrl+R"))
                    .findFirst().orElseThrow();
            assertThat(code.getStyle()).contains("-fx-font-family: monospace");
        }

        @Test
        @DisplayName("renders mixed inline styles")
        void shouldRenderMixedInline(FxRobot robot) {
            String text = fullText("plain **bold** and *italic*");
            assertThat(text).contains("plain").contains("bold").contains("italic");
        }
    }

    @Nested
    @DisplayName("Block elements")
    class BlockElements {

        @Test
        @DisplayName("renders headings as bold with font size")
        void shouldRenderHeading(FxRobot robot) {
            List<Text> nodes = render("## Section Title");
            Text heading = nodes.stream()
                    .filter(t -> t.getText().contains("Section Title"))
                    .findFirst().orElseThrow();
            assertThat(heading.getStyle()).contains("-fx-font-weight: bold");
            assertThat(heading.getStyle()).contains("-fx-font-size");
        }

        @Test
        @DisplayName("renders bullet lists with bullet character")
        void shouldRenderBulletList(FxRobot robot) {
            String text = fullText("- item one\n- item two");
            assertThat(text).contains("\u2022").contains("item one").contains("item two");
        }

        @Test
        @DisplayName("renders ordered lists with numbers")
        void shouldRenderOrderedList(FxRobot robot) {
            String text = fullText("1. first\n2. second");
            assertThat(text).contains("1.").contains("first")
                    .contains("2.").contains("second");
        }

        @Test
        @DisplayName("renders fenced code blocks in monospace")
        void shouldRenderFencedCodeBlock(FxRobot robot) {
            List<Text> nodes = render("```\nsome code\n```");
            Text code = nodes.stream()
                    .filter(t -> t.getText().contains("some code"))
                    .findFirst().orElseThrow();
            assertThat(code.getStyle()).contains("-fx-font-family: monospace");
        }

        @Test
        @DisplayName("separates paragraphs with blank lines")
        void shouldSeparateParagraphs(FxRobot robot) {
            String text = fullText("First paragraph.\n\nSecond paragraph.");
            assertThat(text).contains("First paragraph.")
                    .contains("Second paragraph.");
            // Should have double newline between paragraphs
            assertThat(text).contains("\n\n");
        }

        @Test
        @DisplayName("heading followed by paragraph has tight spacing")
        void shouldHaveTightSpacingAfterHeading(FxRobot robot) {
            String text = fullText("## Title\n\nParagraph text");
            // Heading ends with \n, then paragraph follows immediately (no extra blank line)
            assertThat(text).contains("Title\nParagraph");
        }

        @Test
        @DisplayName("two consecutive headings still have normal spacing")
        void shouldSeparateConsecutiveHeadings(FxRobot robot) {
            String text = fullText("## First\n\n## Second");
            assertThat(text).contains("First\n\n\nSecond");
        }
    }

    @Nested
    @DisplayName("Tables")
    class Tables {

        @Test
        @DisplayName("renders a simple two-column table")
        void shouldRenderSimpleTable(FxRobot robot) {
            String md = """
                    | Name | Value |
                    |------|-------|
                    | alpha | 1 |
                    | beta | 2 |
                    """;
            String text = fullText(md);
            assertThat(text).contains("Name").contains("Value");
            assertThat(text).contains("alpha").contains("1");
            assertThat(text).contains("beta").contains("2");
            // Header separator should use box-drawing characters
            assertThat(text).contains("\u2500");
        }

        @Test
        @DisplayName("renders header row in bold")
        void shouldRenderHeaderBold(FxRobot robot) {
            String md = """
                    | H1 | H2 |
                    |----|-----|
                    | a  | b  |
                    """;
            List<Text> nodes = render(md);
            Text header = nodes.stream()
                    .filter(t -> t.getText().contains("H1"))
                    .findFirst().orElseThrow();
            assertThat(header.getStyle()).contains("-fx-font-weight: bold");
        }

        @Test
        @DisplayName("renders the mental models table from the tutorial")
        void shouldRenderMentalModelsTable(FxRobot robot) {
            String md = """
                    | Mental Model | Formal SD Model |
                    |---|---|
                    | "Hiring more staff will fix the backlog" | Staff increases rate |
                    """;
            String text = fullText(md);
            assertThat(text).contains("Mental Model").contains("Formal SD Model");
            assertThat(text).contains("Hiring more staff");
            assertThat(text).doesNotContain("|");
        }
    }

    @Nested
    @DisplayName("Edge cases")
    class EdgeCases {

        @Test
        @DisplayName("handles empty input")
        void shouldHandleEmptyInput(FxRobot robot) {
            TextFlow flow = MarkdownToTextFlow.convert("");
            assertThat(flow.getChildren()).isEmpty();
        }

        @Test
        @DisplayName("preserves unicode characters")
        void shouldPreserveUnicode(FxRobot robot) {
            String text = fullText("R\u2080 \u2014 the basic reproduction number");
            assertThat(text).contains("R\u2080").contains("\u2014");
        }
    }
}
