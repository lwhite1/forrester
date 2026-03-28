package systems.courant.sd.app.canvas.dialogs;

import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;

import org.commonmark.ext.gfm.tables.TableBlock;
import org.commonmark.ext.gfm.tables.TableBody;
import org.commonmark.ext.gfm.tables.TableCell;
import org.commonmark.ext.gfm.tables.TableHead;
import org.commonmark.ext.gfm.tables.TableRow;
import org.commonmark.ext.gfm.tables.TablesExtension;
import org.commonmark.node.AbstractVisitor;
import org.commonmark.node.BulletList;
import org.commonmark.node.Code;
import org.commonmark.node.CustomBlock;
import org.commonmark.node.CustomNode;
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
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

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
 *   <li>GFM-style tables ({@code | col | col |})</li>
 * </ul>
 */
public final class MarkdownToTextFlow {

    private static final Parser PARSER = Parser.builder()
            .extensions(List.of(TablesExtension.create()))
            .build();

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

        /** Accumulates cell texts for the current table row. */
        private List<String> currentRowCells;
        /** All rows (header + body) collected before rendering. */
        private List<List<String>> tableRows;
        /** Whether we are inside a table cell (suppresses normal emit). */
        private boolean inTableCell;
        private StringBuilder cellBuffer;

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

        // ── Table nodes ────────────────────────────────────────────────

        @Override
        public void visit(CustomBlock customBlock) {
            if (customBlock instanceof TableBlock) {
                if (!firstParagraph) {
                    emit("\n\n", null);
                }
                firstParagraph = false;
                tableRows = new ArrayList<>();
                visitChildren(customBlock);
                renderTable();
                tableRows = null;
            } else {
                super.visit(customBlock);
            }
        }

        @Override
        public void visit(CustomNode customNode) {
            if (customNode instanceof TableHead || customNode instanceof TableBody) {
                visitChildren(customNode);
            } else if (customNode instanceof TableRow) {
                currentRowCells = new ArrayList<>();
                visitChildren(customNode);
                tableRows.add(currentRowCells);
                currentRowCells = null;
            } else if (customNode instanceof TableCell) {
                inTableCell = true;
                cellBuffer = new StringBuilder();
                visitChildren(customNode);
                currentRowCells.add(cellBuffer.toString().trim());
                cellBuffer = null;
                inTableCell = false;
            } else {
                super.visit(customNode);
            }
        }

        private void renderTable() {
            if (tableRows.isEmpty()) {
                return;
            }
            // Compute column widths from all rows
            int cols = tableRows.stream().mapToInt(List::size).max().orElse(0);
            int[] widths = new int[cols];
            for (List<String> row : tableRows) {
                for (int i = 0; i < row.size(); i++) {
                    widths[i] = Math.max(widths[i], row.get(i).length());
                }
            }

            boolean first = true;
            for (int r = 0; r < tableRows.size(); r++) {
                List<String> row = tableRows.get(r);
                if (!first) {
                    emit("\n", null);
                }
                first = false;

                // Header row is bold
                boolean isHeader = (r == 0);
                StringBuilder line = new StringBuilder();
                for (int c = 0; c < cols; c++) {
                    String cell = c < row.size() ? row.get(c) : "";
                    if (c > 0) {
                        line.append("  \u2502  ");
                    }
                    line.append(pad(cell, widths[c]));
                }
                if (isHeader) {
                    emit(line.toString(), "-fx-font-weight: bold;");
                } else {
                    emit(line.toString(), null);
                }

                // Separator line after header
                if (isHeader) {
                    emit("\n", null);
                    StringBuilder sep = new StringBuilder();
                    for (int c = 0; c < cols; c++) {
                        if (c > 0) {
                            sep.append("\u2500\u2500\u253C\u2500\u2500");
                        }
                        sep.append("\u2500".repeat(widths[c]));
                    }
                    emit(sep.toString(), null);
                }
            }
        }

        private static String pad(String s, int width) {
            if (s.length() >= width) {
                return s;
            }
            return s + " ".repeat(width - s.length());
        }

        // ── Inline nodes ─────────────────────────────────────────────

        @Override
        public void visit(org.commonmark.node.Text text) {
            if (inTableCell) {
                cellBuffer.append(text.getLiteral());
            } else {
                String style = styleStack.isEmpty() ? null : styleStack.peek();
                emit(text.getLiteral(), style);
            }
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
            if (inTableCell) {
                cellBuffer.append(code.getLiteral());
            } else {
                emit(code.getLiteral(),
                        "-fx-font-family: monospace; -fx-font-weight: bold;");
            }
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
