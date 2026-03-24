package systems.courant.sd.app.canvas.dialogs;

import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;

import org.commonmark.node.AbstractVisitor;
import org.commonmark.node.BulletList;
import org.commonmark.node.Code;
import org.commonmark.node.Document;
import org.commonmark.node.Emphasis;
import org.commonmark.node.FencedCodeBlock;
import org.commonmark.node.HardLineBreak;
import org.commonmark.node.Heading;
import org.commonmark.node.IndentedCodeBlock;
import org.commonmark.node.ListItem;
import org.commonmark.node.Node;
import org.commonmark.node.OrderedList;
import org.commonmark.node.Paragraph;
import org.commonmark.node.SoftLineBreak;
import org.commonmark.node.StrongEmphasis;
import org.commonmark.parser.Parser;

import java.util.ArrayDeque;
import java.util.Deque;

/**
 * Converts a CommonMark Markdown string into a JavaFX {@link TextFlow},
 * reusing the styling conventions established by {@link StyledText}.
 *
 * <p>Supported Markdown constructs:
 * <ul>
 *   <li>{@code **bold**} — bold weight</li>
 *   <li>{@code *italic*} — italic style</li>
 *   <li>{@code `code`} — monospace bold</li>
 *   <li>{@code # Heading} — bold with increased font size</li>
 *   <li>Bullet lists ({@code - item})</li>
 *   <li>Ordered lists ({@code 1. item})</li>
 *   <li>Fenced / indented code blocks</li>
 *   <li>Paragraphs separated by blank lines</li>
 * </ul>
 */
public final class MarkdownToTextFlow {

    private static final Parser PARSER = Parser.builder().build();

    private MarkdownToTextFlow() {
    }

    /**
     * Parses the given Markdown string and returns a {@link TextFlow}
     * containing styled {@link Text} nodes.
     */
    public static TextFlow convert(String markdown) {
        Node document = PARSER.parse(markdown);
        Visitor visitor = new Visitor();
        document.accept(visitor);
        return visitor.result();
    }

    /**
     * Walks the CommonMark AST and emits JavaFX Text nodes into a TextFlow.
     */
    private static final class Visitor extends AbstractVisitor {

        private final TextFlow flow = new TextFlow();

        /** Style stack tracks nested inline formatting (bold inside italic, etc.) */
        private final Deque<String> styleStack = new ArrayDeque<>();

        private boolean firstParagraph = true;
        private int orderedListIndex;

        TextFlow result() {
            return flow;
        }

        // ── Block nodes ──────────────────────────────────────────────

        @Override
        public void visit(Document document) {
            visitChildren(document);
        }

        @Override
        public void visit(Heading heading) {
            if (!firstParagraph) {
                emit("\n\n", null);
            }
            firstParagraph = false;

            int level = heading.getLevel();
            String fontSize = level <= 1 ? "16px" : "14px";
            styleStack.push("-fx-font-weight: bold; -fx-font-size: " + fontSize + ";");
            visitChildren(heading);
            styleStack.pop();
            emit("\n", null);
        }

        @Override
        public void visit(Paragraph paragraph) {
            boolean insideListItem = paragraph.getParent() instanceof ListItem;
            if (!insideListItem) {
                if (!firstParagraph) {
                    emit("\n\n", null);
                }
                firstParagraph = false;
            }
            visitChildren(paragraph);
        }

        @Override
        public void visit(BulletList bulletList) {
            if (!firstParagraph) {
                emit("\n", null);
            }
            firstParagraph = false;
            visitChildren(bulletList);
        }

        @Override
        public void visit(OrderedList orderedList) {
            if (!firstParagraph) {
                emit("\n", null);
            }
            firstParagraph = false;
            orderedListIndex = orderedList.getStartNumber();
            visitChildren(orderedList);
        }

        @Override
        public void visit(ListItem listItem) {
            boolean ordered = listItem.getParent() instanceof OrderedList;
            if (ordered) {
                emit("  " + orderedListIndex + ". ", null);
                orderedListIndex++;
            } else {
                emit("  \u2022 ", null);
            }
            visitChildren(listItem);
            emit("\n", null);
        }

        @Override
        public void visit(FencedCodeBlock fencedCodeBlock) {
            if (!firstParagraph) {
                emit("\n\n", null);
            }
            firstParagraph = false;
            String code = fencedCodeBlock.getLiteral();
            if (code.endsWith("\n")) {
                code = code.substring(0, code.length() - 1);
            }
            emit("  " + code.replace("\n", "\n  "),
                    "-fx-font-family: monospace; -fx-font-weight: bold;");
        }

        @Override
        public void visit(IndentedCodeBlock indentedCodeBlock) {
            if (!firstParagraph) {
                emit("\n\n", null);
            }
            firstParagraph = false;
            String code = indentedCodeBlock.getLiteral();
            if (code.endsWith("\n")) {
                code = code.substring(0, code.length() - 1);
            }
            emit(code, "-fx-font-family: monospace; -fx-font-weight: bold;");
        }

        // ── Inline nodes ─────────────────────────────────────────────

        @Override
        public void visit(org.commonmark.node.Text text) {
            String style = styleStack.isEmpty() ? null : styleStack.peek();
            emit(text.getLiteral(), style);
        }

        @Override
        public void visit(StrongEmphasis strongEmphasis) {
            styleStack.push("-fx-font-weight: bold;");
            visitChildren(strongEmphasis);
            styleStack.pop();
        }

        @Override
        public void visit(Emphasis emphasis) {
            styleStack.push("-fx-font-style: italic;");
            visitChildren(emphasis);
            styleStack.pop();
        }

        @Override
        public void visit(Code code) {
            emit(code.getLiteral(),
                    "-fx-font-family: monospace; -fx-font-weight: bold;");
        }

        @Override
        public void visit(SoftLineBreak softLineBreak) {
            emit(" ", null);
        }

        @Override
        public void visit(HardLineBreak hardLineBreak) {
            emit("\n", null);
        }

        // ── Helpers ──────────────────────────────────────────────────

        private void emit(String content, String style) {
            Text text = new Text(content);
            if (style != null) {
                text.setStyle(style);
            }
            flow.getChildren().add(text);
        }
    }
}
