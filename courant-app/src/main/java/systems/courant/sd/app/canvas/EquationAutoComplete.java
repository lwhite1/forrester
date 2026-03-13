package systems.courant.sd.app.canvas;

import systems.courant.sd.model.compile.FunctionDoc;
import systems.courant.sd.model.compile.FunctionDocRegistry;

import javafx.beans.value.ChangeListener;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Bounds;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import javafx.stage.Popup;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Attaches autocomplete behaviour to equation fields.
 * Suggests element names (with type badges) and built-in function names
 * (with signatures and descriptions) as the user types.
 *
 * <p>Usage: call {@link #attach} after creating the field and
 * {@link #detach} before discarding it.</p>
 */
public final class EquationAutoComplete {

    private static final String PROP_STATE = "EquationAutoComplete.state";
    private static final String PROP_ORIGINAL_ACTION = "EquationAutoComplete.originalAction";

    private static final int MAX_VISIBLE_ROWS = 8;
    private static final double ROW_HEIGHT = 40;
    private static final double POPUP_WIDTH = 360;
    private static final int MIN_PREFIX_LENGTH = 1;

    static final List<String> BUILT_IN_FUNCTIONS = FunctionDocRegistry.allNames();

    private static final Set<String> FUNCTION_SET = Set.copyOf(BUILT_IN_FUNCTIONS);

    private static final Logger log = LoggerFactory.getLogger(EquationAutoComplete.class);

    private EquationAutoComplete() { }

    // ── Token record ────────────────────────────────────────────────────

    record Token(String prefix, int start, int end) { }

    /**
     * Describes the function call context at the caret position.
     *
     * @param functionName the uppercase name of the enclosing function
     * @param paramIndex   the zero-based index of the parameter the caret is in
     */
    record FunctionCallContext(String functionName, int paramIndex) { }

    // ── Public API ──────────────────────────────────────────────────────

    /**
     * Attaches autocomplete to the given {@link EquationField}.
     *
     * @param field       the equation field
     * @param editor      the model editor providing element names
     * @param excludeName the name of the element being edited (excluded from suggestions)
     */
    static void attach(EquationField field, ModelEditor editor, String excludeName) {
        detach(field);
        Node node = field.node();
        State state = new State(field, editor, excludeName);
        node.getProperties().put(PROP_STATE, state);
        node.addEventFilter(KeyEvent.KEY_PRESSED, state.keyFilter);
        field.textObservable().addListener(state.textListener);
        field.caretPositionObservable().addListener(state.caretListener);
        field.focusedProperty().addListener(state.focusListener);

        // Wrap the existing onAction handler. When Enter fires, check if
        // the popup is showing with a selected item — if so, insert the suggestion
        // instead of committing the field.
        EventHandler<ActionEvent> originalAction = field.getOnAction();
        node.getProperties().put(PROP_ORIGINAL_ACTION, originalAction);
        field.setOnAction(e -> {
            boolean popupShowing = state.popup != null && state.popup.isShowing();
            AutoCompleteSuggestion selected = (state.listView != null)
                    ? state.listView.getSelectionModel().getSelectedItem() : null;
            log.debug("onAction: popupShowing={}, selected={}, text={}",
                    popupShowing, selected != null ? selected.name() : null, field.getText());
            if (popupShowing && selected != null) {
                log.debug("onAction: inserting suggestion '{}'", selected.name());
                state.insertSuggestion(selected);
            } else if (originalAction != null) {
                log.debug("onAction: delegating to original handler");
                originalAction.handle(e);
            }
        });
    }

    /**
     * Detaches autocomplete from the given {@link EquationField}, cleaning up listeners and popup.
     */
    static void detach(EquationField field) {
        if (field == null) {
            return;
        }
        Node node = field.node();
        Object obj = node.getProperties().remove(PROP_STATE);
        if (obj instanceof State state) {
            state.hidePopup();
            state.hideHint();
            node.removeEventFilter(KeyEvent.KEY_PRESSED, state.keyFilter);
            field.textObservable().removeListener(state.textListener);
            field.caretPositionObservable().removeListener(state.caretListener);
            field.focusedProperty().removeListener(state.focusListener);
        }
        // Restore the original onAction handler (only if we previously saved one)
        Object original = node.getProperties().remove(PROP_ORIGINAL_ACTION);
        if (original instanceof EventHandler<?> handler) {
            @SuppressWarnings("unchecked")
            EventHandler<ActionEvent> actionHandler = (EventHandler<ActionEvent>) handler;
            field.setOnAction(actionHandler);
        }
    }

    /**
     * Returns true if the autocomplete popup is currently showing for the given field.
     */
    static boolean isPopupShowing(EquationField field) {
        if (field == null) {
            return false;
        }
        Object obj = field.node().getProperties().get(PROP_STATE);
        return obj instanceof State state && state.popup != null && state.popup.isShowing();
    }

    // ── TextField convenience overloads (for InlineEditor compatibility) ─

    /**
     * Attaches autocomplete to a plain {@link TextField}.
     * Convenience overload that wraps the field in a {@link TextFieldEquationField}.
     */
    static void attach(TextField tf, ModelEditor editor, String excludeName) {
        attach(new TextFieldEquationField(tf), editor, excludeName);
    }

    /** Detaches autocomplete from a plain {@link TextField}. */
    static void detach(TextField tf) {
        if (tf == null) {
            return;
        }
        detach(new TextFieldEquationField(tf));
    }

    /** Returns true if the autocomplete popup is showing for the given {@link TextField}. */
    static boolean isPopupShowing(TextField tf) {
        if (tf == null) {
            return false;
        }
        return isPopupShowing(new TextFieldEquationField(tf));
    }

    // ── Token extraction (package-private for testing) ──────────────────

    static Token extractToken(String text, int caret) {
        if (text == null || caret <= 0 || caret > text.length()) {
            return null;
        }
        int end = caret;
        int start = caret;
        while (start > 0 && isIdentChar(text.charAt(start - 1))) {
            start--;
        }
        if (start == end) {
            return null;
        }
        return new Token(text.substring(start, end), start, end);
    }

    // ── Function call context detection (package-private for testing) ───

    /**
     * Determines if the caret is inside a function call's parentheses.
     * Scans backward from caret, counting parentheses depth and commas
     * to find the enclosing function name and current parameter index.
     *
     * @return the function call context, or null if the caret is not inside a function call
     */
    static FunctionCallContext detectFunctionContext(String text, int caret) {
        if (text == null || caret <= 0 || caret > text.length()) {
            return null;
        }

        int depth = 0;
        int commas = 0;

        for (int i = caret - 1; i >= 0; i--) {
            char c = text.charAt(i);
            if (c == ')') {
                depth++;
            } else if (c == '(') {
                if (depth > 0) {
                    depth--;
                } else {
                    // Found the matching open paren — extract function name before it
                    int nameEnd = i;
                    int nameStart = i;
                    while (nameStart > 0 && isIdentChar(text.charAt(nameStart - 1))) {
                        nameStart--;
                    }
                    if (nameStart == nameEnd) {
                        return null; // bare parentheses, no function name
                    }
                    String funcName = text.substring(nameStart, nameEnd).toUpperCase();
                    if (FunctionDocRegistry.get(funcName).isPresent()) {
                        return new FunctionCallContext(funcName, commas);
                    }
                    return null; // not a known function
                }
            } else if (c == ',' && depth == 0) {
                commas++;
            }
        }
        return null; // no enclosing open paren found
    }

    // ── Suggestion gathering (package-private for testing) ──────────────

    /**
     * Returns plain name strings for backward-compatible tests.
     */
    static List<String> getSuggestions(ModelEditor editor, String excludeName) {
        return getRichSuggestions(editor, excludeName).stream()
                .map(AutoCompleteSuggestion::name)
                .toList();
    }

    static List<AutoCompleteSuggestion> getRichSuggestions(ModelEditor editor, String excludeName) {
        String excludeUnderscore = excludeName != null
                ? excludeName.replace(' ', '_') : null;

        List<AutoCompleteSuggestion> result = new ArrayList<>();

        addElementSuggestions(result, editor.getStocks(), s -> s.name(),
                AutoCompleteSuggestion.Kind.STOCK, excludeUnderscore);
        addElementSuggestions(result, editor.getFlows(), f -> f.name(),
                AutoCompleteSuggestion.Kind.FLOW, excludeUnderscore);
        addElementSuggestions(result, editor.getVariables(), a -> a.name(),
                AutoCompleteSuggestion.Kind.AUX, excludeUnderscore);
        addElementSuggestions(result, editor.getLookupTables(), l -> l.name(),
                AutoCompleteSuggestion.Kind.LOOKUP, excludeUnderscore);
        addElementSuggestions(result, editor.getModules(), m -> m.instanceName(),
                AutoCompleteSuggestion.Kind.MODULE, excludeUnderscore);

        result.sort(Comparator.comparing(AutoCompleteSuggestion::name));

        // Add functions after elements
        for (String funcName : BUILT_IN_FUNCTIONS) {
            Optional<FunctionDoc> doc = FunctionDocRegistry.get(funcName);
            String displayName = doc.map(FunctionDoc::signature).orElse(funcName);
            String description = doc.map(FunctionDoc::oneLiner).orElse("");
            result.add(new AutoCompleteSuggestion(
                    funcName, displayName, description,
                    AutoCompleteSuggestion.Kind.FUNCTION, true));
        }

        return result;
    }

    // ── Filtering (package-private for testing) ─────────────────────────

    static List<String> filterSuggestions(List<String> all, String prefix) {
        if (prefix == null || prefix.isEmpty()) {
            return List.of();
        }
        String lower = prefix.toLowerCase();
        List<String> result = new ArrayList<>();
        for (String s : all) {
            if (s.toLowerCase().startsWith(lower)) {
                result.add(s);
            }
        }
        return result;
    }

    static List<AutoCompleteSuggestion> filterRichSuggestions(
            List<AutoCompleteSuggestion> all, String prefix) {
        if (prefix == null || prefix.isEmpty()) {
            return List.of();
        }
        String lower = prefix.toLowerCase();
        List<AutoCompleteSuggestion> result = new ArrayList<>();
        for (AutoCompleteSuggestion s : all) {
            if (s.name().toLowerCase().startsWith(lower)) {
                result.add(s);
            }
        }
        return result;
    }

    // ── Helpers ─────────────────────────────────────────────────────────

    private static boolean isIdentChar(char c) {
        return Character.isLetterOrDigit(c) || c == '_';
    }

    private static <T> void addElementSuggestions(
            List<AutoCompleteSuggestion> dest, List<T> items,
            java.util.function.Function<T, String> nameExtractor,
            AutoCompleteSuggestion.Kind kind, String excludeUnderscore) {
        for (T item : items) {
            String name = nameExtractor.apply(item).replace(' ', '_');
            if (name.equals(excludeUnderscore)) {
                continue;
            }
            String kindLabel = switch (kind) {
                case STOCK -> "Stock";
                case FLOW -> "Flow";
                case AUX -> "Variable";
                case LOOKUP -> "Lookup Table";
                case MODULE -> "Module";
                case FUNCTION -> "Function";
            };
            dest.add(new AutoCompleteSuggestion(name, name, kindLabel, kind, false));
        }
    }

    static boolean isBuiltInFunction(String name) {
        return FUNCTION_SET.contains(name);
    }

    // ── Custom cell for rich rendering ──────────────────────────────────

    private static class SuggestionCell extends ListCell<AutoCompleteSuggestion> {

        private final HBox root = new HBox(6);
        private final Label badge = new Label();
        private final VBox textBox = new VBox(1);
        private final Label primaryLabel = new Label();
        private final Label secondaryLabel = new Label();

        SuggestionCell() {
            root.setPadding(new Insets(2, 6, 2, 6));

            badge.setMinWidth(22);
            badge.setMaxWidth(22);
            badge.setStyle("-fx-font-size: 10; -fx-font-weight: bold; "
                    + "-fx-alignment: center; -fx-text-fill: #666;");

            primaryLabel.setStyle("-fx-font-size: 12; -fx-font-weight: bold;");
            primaryLabel.setMaxWidth(Double.MAX_VALUE);
            HBox.setHgrow(textBox, Priority.ALWAYS);

            secondaryLabel.setStyle("-fx-font-size: 10; -fx-text-fill: #888;");
            secondaryLabel.setMaxWidth(Double.MAX_VALUE);

            textBox.getChildren().addAll(primaryLabel, secondaryLabel);
            root.getChildren().addAll(badge, textBox);
        }

        @Override
        protected void updateItem(AutoCompleteSuggestion item, boolean empty) {
            super.updateItem(item, empty);
            if (empty || item == null) {
                setGraphic(null);
                setText(null);
                return;
            }

            badge.setText(item.kind().badge());
            badge.setStyle(badgeStyle(item.kind()));
            primaryLabel.setText(item.displayName());
            secondaryLabel.setText(item.description());
            secondaryLabel.setVisible(!item.description().isEmpty());
            secondaryLabel.setManaged(!item.description().isEmpty());

            setGraphic(root);
            setText(null);
        }

        private static String badgeStyle(AutoCompleteSuggestion.Kind kind) {
            String color = switch (kind) {
                case STOCK -> "#2563eb";
                case FLOW -> "#dc2626";
                case AUX -> "#059669";
                case LOOKUP -> "#d97706";
                case MODULE -> "#0891b2";
                case FUNCTION -> "#6b7280";
            };
            return "-fx-font-size: 10; -fx-font-weight: bold; "
                    + "-fx-alignment: center; -fx-text-fill: " + color + ";";
        }
    }

    // ── Inner state per attachment ──────────────────────────────────────

    private static final class State {
        final EquationField field;
        final ModelEditor editor;
        final String excludeName;

        Popup popup;
        ListView<AutoCompleteSuggestion> listView;
        Token currentToken;

        Popup hintPopup;
        VBox hintContent;
        FunctionCallContext lastHintContext;

        final EventHandler<KeyEvent> keyFilter;
        final ChangeListener<String> textListener;
        final ChangeListener<Number> caretListener;
        final ChangeListener<Boolean> focusListener;

        State(EquationField field, ModelEditor editor, String excludeName) {
            this.field = field;
            this.editor = editor;
            this.excludeName = excludeName;

            keyFilter = this::handleKey;
            textListener = (obs, oldVal, newVal) -> updatePopup();
            caretListener = (obs, oldVal, newVal) -> updatePopup();
            focusListener = (obs, wasFocused, isFocused) -> {
                if (!isFocused) {
                    hidePopup();
                    hideHint();
                }
            };
        }

        private void handleKey(KeyEvent event) {
            boolean popupVisible = popup != null && popup.isShowing();
            log.debug("handleKey: {} popupVisible={}", event.getCode(), popupVisible);

            switch (event.getCode()) {
                case DOWN -> {
                    if (popupVisible) {
                        int idx = listView.getSelectionModel().getSelectedIndex();
                        if (idx < listView.getItems().size() - 1) {
                            listView.getSelectionModel().select(idx + 1);
                        }
                        event.consume();
                    } else {
                        updatePopup();
                    }
                }
                case UP -> {
                    if (popupVisible) {
                        int idx = listView.getSelectionModel().getSelectedIndex();
                        if (idx > 0) {
                            listView.getSelectionModel().select(idx - 1);
                        }
                        event.consume();
                    }
                }
                case TAB -> {
                    if (popupVisible && listView.getSelectionModel().getSelectedItem() != null) {
                        insertSuggestion(listView.getSelectionModel().getSelectedItem());
                        event.consume();
                    }
                }
                case ESCAPE -> {
                    if (popupVisible) {
                        hidePopup();
                        hideHint();
                        event.consume();
                    }
                }
                default -> { }
            }
        }

        private void updatePopup() {
            updateHint();

            Token token = extractToken(field.getText(), field.getCaretPosition());
            currentToken = token;

            if (token == null || token.prefix().length() < MIN_PREFIX_LENGTH) {
                hidePopup();
                return;
            }

            List<AutoCompleteSuggestion> filtered = filterRichSuggestions(
                    getRichSuggestions(editor, excludeName), token.prefix());
            if (filtered.isEmpty()) {
                hidePopup();
                return;
            }

            ensurePopup();
            listView.getItems().setAll(filtered);
            listView.getSelectionModel().selectFirst();
            log.debug("updatePopup: {} suggestions for prefix '{}', first={}",
                    filtered.size(), token.prefix(),
                    listView.getSelectionModel().getSelectedItem() != null
                            ? listView.getSelectionModel().getSelectedItem().name() : null);

            if (!popup.isShowing()) {
                Node node = field.node();
                Bounds screenBounds = node.localToScreen(node.getBoundsInLocal());
                if (screenBounds != null) {
                    popup.show(node, screenBounds.getMinX(),
                            screenBounds.getMaxY());
                }
            }
        }

        private void ensurePopup() {
            if (popup != null) {
                return;
            }
            listView = new ListView<>();
            listView.setCellFactory(lv -> new SuggestionCell());
            listView.setFixedCellSize(ROW_HEIGHT);
            listView.setPrefHeight(MAX_VISIBLE_ROWS * ROW_HEIGHT + 2);
            listView.setPrefWidth(POPUP_WIDTH);
            listView.setFocusTraversable(false);
            listView.setOnMouseClicked(event -> {
                if (event.getClickCount() == 1
                        && listView.getSelectionModel().getSelectedItem() != null) {
                    insertSuggestion(listView.getSelectionModel().getSelectedItem());
                }
            });
            // Safety net: if the ListView somehow gets focus, handle Enter/Escape here
            listView.setOnKeyPressed(event -> {
                switch (event.getCode()) {
                    case ENTER -> {
                        AutoCompleteSuggestion selected =
                                listView.getSelectionModel().getSelectedItem();
                        if (selected != null) {
                            log.debug("listView keyHandler: ENTER selected={}", selected.name());
                            insertSuggestion(selected);
                        }
                        event.consume();
                    }
                    case ESCAPE -> {
                        hidePopup();
                        field.requestFocus();
                        event.consume();
                    }
                    default -> { }
                }
            });

            popup = new Popup();
            popup.setAutoHide(true);
            popup.setHideOnEscape(false);
            popup.getContent().add(listView);
        }

        private void insertSuggestion(AutoCompleteSuggestion suggestion) {
            if (currentToken == null) {
                return;
            }
            String text = field.getText();
            String insertion = suggestion.isFunction()
                    ? suggestion.name() + "(" : suggestion.name();
            String newText = text.substring(0, currentToken.start())
                    + insertion
                    + text.substring(currentToken.end());
            int newCaret = currentToken.start() + insertion.length();

            field.setText(newText);
            field.positionCaret(newCaret);
            hidePopup();
        }

        void hidePopup() {
            if (popup != null && popup.isShowing()) {
                popup.hide();
            }
        }

        // ── Parameter hint ──────────────────────────────────────────

        private void updateHint() {
            FunctionCallContext ctx = detectFunctionContext(
                    field.getText(), field.getCaretPosition());

            if (ctx == null) {
                hideHint();
                lastHintContext = null;
                return;
            }

            // Don't rebuild if context hasn't changed
            if (ctx.equals(lastHintContext) && hintPopup != null && hintPopup.isShowing()) {
                return;
            }
            lastHintContext = ctx;

            Optional<FunctionDoc> docOpt = FunctionDocRegistry.get(ctx.functionName());
            if (docOpt.isEmpty() || docOpt.get().parameters().isEmpty()) {
                hideHint();
                return;
            }
            FunctionDoc doc = docOpt.get();

            ensureHintPopup();
            hintContent.getChildren().clear();

            // Signature line with current parameter bolded
            TextFlow sigLine = buildSignatureLine(doc, ctx.paramIndex());
            hintContent.getChildren().add(sigLine);

            // Current parameter description
            if (ctx.paramIndex() < doc.parameters().size()) {
                FunctionDoc.ParamDoc param = doc.parameters().get(ctx.paramIndex());
                Label descLabel = new Label(param.name() + " \u2014 " + param.description());
                descLabel.setStyle("-fx-font-size: 11; -fx-text-fill: #555;");
                descLabel.setWrapText(true);
                descLabel.setMaxWidth(POPUP_WIDTH - 16);
                hintContent.getChildren().add(descLabel);
            }

            if (!hintPopup.isShowing()) {
                Node node = field.node();
                Bounds screenBounds = node.localToScreen(node.getBoundsInLocal());
                if (screenBounds != null) {
                    // Show above the field
                    double hintHeight = doc.parameters().isEmpty() ? 24 : 44;
                    hintPopup.show(node, screenBounds.getMinX(),
                            screenBounds.getMinY() - hintHeight);
                }
            }
        }

        private TextFlow buildSignatureLine(FunctionDoc doc, int activeParam) {
            TextFlow flow = new TextFlow();
            Font normal = Font.font("System", FontWeight.NORMAL, 12);
            Font bold = Font.font("System", FontWeight.BOLD, 12);

            // Function name
            Text nameText = new Text(doc.name() + "(");
            nameText.setFont(normal);
            flow.getChildren().add(nameText);

            List<FunctionDoc.ParamDoc> params = doc.parameters();
            for (int i = 0; i < params.size(); i++) {
                if (i > 0) {
                    Text comma = new Text(", ");
                    comma.setFont(normal);
                    flow.getChildren().add(comma);
                }
                Text paramText = new Text(params.get(i).name());
                paramText.setFont(i == activeParam ? bold : normal);
                if (i == activeParam) {
                    paramText.setStyle("-fx-fill: #2563eb;");
                }
                flow.getChildren().add(paramText);
            }

            Text closeParen = new Text(")");
            closeParen.setFont(normal);
            flow.getChildren().add(closeParen);

            return flow;
        }

        private void ensureHintPopup() {
            if (hintPopup != null) {
                return;
            }
            hintContent = new VBox(2);
            hintContent.setPadding(new Insets(4, 8, 4, 8));
            hintContent.setStyle("-fx-background-color: #fffde7; "
                    + "-fx-border-color: #e0e0e0; -fx-border-radius: 3; "
                    + "-fx-background-radius: 3; -fx-effect: dropshadow(gaussian, "
                    + "rgba(0,0,0,0.15), 4, 0, 0, 1);");
            hintContent.setMaxWidth(POPUP_WIDTH);

            hintPopup = new Popup();
            hintPopup.setAutoHide(false);
            hintPopup.setAutoFix(true);
            hintPopup.getContent().add(hintContent);
        }

        void hideHint() {
            if (hintPopup != null && hintPopup.isShowing()) {
                hintPopup.hide();
            }
        }
    }
}
