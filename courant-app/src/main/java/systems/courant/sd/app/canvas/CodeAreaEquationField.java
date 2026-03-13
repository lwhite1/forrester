package systems.courant.sd.app.canvas;

import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.value.ObservableValue;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.Node;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;

import org.fxmisc.richtext.StyleClassedTextArea;



/**
 * Multi-line equation editor with syntax highlighting, backed by a RichTextFX
 * {@link StyleClassedTextArea}. Implements {@link EquationField} so it can be
 * used interchangeably with single-line text fields.
 *
 * <p>Behaviour:
 * <ul>
 *   <li><b>Enter</b> commits the equation (fires the onAction handler).</li>
 *   <li><b>Shift+Enter</b> inserts a newline for multi-line editing.</li>
 *   <li>Auto-expands vertically from 2 to 5 lines based on content.</li>
 *   <li>Applies syntax highlighting for keywords, numbers, and operators.</li>
 * </ul>
 */
final class CodeAreaEquationField implements EquationField {

    private static final int MIN_ROWS = 2;
    private static final int MAX_ROWS = 5;
    private static final double LINE_HEIGHT = 20.0;
    private static final double VERTICAL_PADDING = 14.0;
    private static final String BASE_STYLE = "-fx-font-family: monospace; -fx-font-size: 13px;";


    private final StyleClassedTextArea codeArea;
    private final SimpleStringProperty textBridge = new SimpleStringProperty("");
    private final SimpleIntegerProperty caretBridge = new SimpleIntegerProperty(0);
    private EventHandler<ActionEvent> onAction;
    private boolean suppressSync;

    CodeAreaEquationField(String initialText) {
        codeArea = new StyleClassedTextArea();
        codeArea.setWrapText(true);
        codeArea.getStyleClass().add("equation-code-area");
        codeArea.setStyle(BASE_STYLE);

        // Load syntax highlighting stylesheet
        String cssUrl = getClass().getResource("/equation-highlight.css").toExternalForm();
        codeArea.getStylesheets().add(cssUrl);

        // Set initial text
        if (initialText != null && !initialText.isEmpty()) {
            codeArea.replaceText(initialText);
            textBridge.set(initialText);
        }

        // Bridge: CodeArea text → textBridge property
        codeArea.plainTextChanges().subscribe(change -> {
            if (!suppressSync) {
                suppressSync = true;
                textBridge.set(codeArea.getText());
                suppressSync = false;
            }
            caretBridge.set(codeArea.getCaretPosition());
            applyHighlighting();
            updatePrefHeight();
        });

        // Bridge: textBridge property → CodeArea (for programmatic setText)
        textBridge.addListener((obs, oldVal, newVal) -> {
            if (!suppressSync) {
                suppressSync = true;
                codeArea.replaceText(newVal != null ? newVal : "");
                suppressSync = false;
            }
        });

        // Bridge: caret position
        // Update on mouse click and key navigation
        codeArea.setOnMouseClicked(e -> caretBridge.set(codeArea.getCaretPosition()));
        codeArea.addEventFilter(KeyEvent.KEY_RELEASED, e -> {
            if (e.getCode().isArrowKey() || e.getCode() == KeyCode.HOME
                    || e.getCode() == KeyCode.END) {
                caretBridge.set(codeArea.getCaretPosition());
            }
        });

        // Enter key handling: plain Enter = commit, Shift+Enter = newline
        codeArea.addEventFilter(KeyEvent.KEY_PRESSED, e -> {
            if (e.getCode() == KeyCode.ENTER && !e.isShiftDown() && !e.isControlDown()) {
                if (onAction != null) {
                    onAction.handle(new ActionEvent(codeArea, codeArea));
                }
                e.consume();
            }
        });

        // Initial sizing
        updatePrefHeight();
        codeArea.setMinHeight(MIN_ROWS * LINE_HEIGHT + VERTICAL_PADDING);
        codeArea.setMaxHeight(MAX_ROWS * LINE_HEIGHT + VERTICAL_PADDING);

        // Initial highlighting
        applyHighlighting();
    }

    private void applyHighlighting() {
        String text = codeArea.getText();
        if (text.isEmpty()) {
            return;
        }
        try {
            codeArea.setStyleSpans(0, EquationHighlighter.computeHighlighting(text));
        } catch (IndexOutOfBoundsException e) {
            // Text changed between computation and application — ignore
        }
    }

    private void updatePrefHeight() {
        int paragraphs = codeArea.getParagraphs().size();
        int rows = Math.max(MIN_ROWS, Math.min(MAX_ROWS, paragraphs));
        codeArea.setPrefHeight(rows * LINE_HEIGHT + VERTICAL_PADDING);
    }

    // ── EquationField implementation ────────────────────────────────────

    @Override
    public String getText() {
        return codeArea.getText();
    }

    @Override
    public void setText(String text) {
        textBridge.set(text != null ? text : "");
    }

    @Override
    public void selectAll() {
        codeArea.selectAll();
    }

    @Override
    public int getCaretPosition() {
        return codeArea.getCaretPosition();
    }

    @Override
    public void positionCaret(int position) {
        codeArea.moveTo(Math.min(position, codeArea.getLength()));
    }

    @Override
    public void requestFocus() {
        codeArea.requestFocus();
    }

    @Override
    public ObservableValue<String> textObservable() {
        return textBridge;
    }

    @Override
    public ObservableValue<Number> caretPositionObservable() {
        return caretBridge;
    }

    @Override
    public ReadOnlyBooleanProperty focusedProperty() {
        return codeArea.focusedProperty();
    }

    @Override
    public Node node() {
        return codeArea;
    }

    @Override
    public void setFieldStyle(String style) {
        if (style == null || style.isEmpty()) {
            codeArea.setStyle(BASE_STYLE);
        } else {
            codeArea.setStyle(BASE_STYLE + " " + style);
        }
    }

    @Override
    public void setOnAction(EventHandler<ActionEvent> handler) {
        this.onAction = handler;
    }

    @Override
    public EventHandler<ActionEvent> getOnAction() {
        return onAction;
    }
}
