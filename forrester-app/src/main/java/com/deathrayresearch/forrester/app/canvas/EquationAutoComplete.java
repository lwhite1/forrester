package com.deathrayresearch.forrester.app.canvas;

import javafx.beans.value.ChangeListener;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Bounds;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.stage.Popup;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

/**
 * Attaches autocomplete behaviour to equation TextFields.
 * Suggests element names and built-in function names as the user types.
 *
 * <p>Usage: call {@link #attach} after creating the TextField and
 * {@link #detach} before discarding it.</p>
 */
public final class EquationAutoComplete {

    private static final String PROP_STATE = "EquationAutoComplete.state";
    private static final String PROP_ORIGINAL_ACTION = "EquationAutoComplete.originalAction";

    private static final int MAX_VISIBLE_ROWS = 8;
    private static final int MIN_PREFIX_LENGTH = 1;

    static final List<String> BUILT_IN_FUNCTIONS = List.of(
            "TIME", "DT", "IF", "ABS", "SQRT", "LN", "EXP",
            "MIN", "MAX", "SUM", "MEAN", "SMOOTH", "DELAY3",
            "STEP", "RAMP", "LOOKUP");

    private static final Set<String> FUNCTION_SET = Set.copyOf(BUILT_IN_FUNCTIONS);

    private static final Logger log = LoggerFactory.getLogger(EquationAutoComplete.class);

    private EquationAutoComplete() { }

    // ── Token record ────────────────────────────────────────────────────

    record Token(String prefix, int start, int end) { }

    // ── Public API ──────────────────────────────────────────────────────

    /**
     * Attaches autocomplete to the given TextField.
     *
     * @param field       the equation TextField
     * @param editor      the model editor providing element names
     * @param excludeName the name of the element being edited (excluded from suggestions)
     */
    static void attach(TextField field, ModelEditor editor, String excludeName) {
        detach(field);
        State state = new State(field, editor, excludeName);
        field.getProperties().put(PROP_STATE, state);
        field.addEventFilter(KeyEvent.KEY_PRESSED, state.keyFilter);
        field.textProperty().addListener(state.textListener);
        field.caretPositionProperty().addListener(state.caretListener);
        field.focusedProperty().addListener(state.focusListener);

        // Wrap the existing onAction handler. When Enter fires ActionEvent, check if
        // the popup is showing with a selected item — if so, insert the suggestion
        // instead of committing the field. This is more reliable than intercepting
        // KEY_PRESSED in an event filter, because ActionEvent always fires after
        // TextField processes Enter internally.
        EventHandler<ActionEvent> originalAction = field.getOnAction();
        field.getProperties().put(PROP_ORIGINAL_ACTION, originalAction);
        field.setOnAction(e -> {
            boolean popupShowing = state.popup != null && state.popup.isShowing();
            String selected = (state.listView != null)
                    ? state.listView.getSelectionModel().getSelectedItem() : null;
            log.debug("onAction: popupShowing={}, selected={}, text={}",
                    popupShowing, selected, field.getText());
            if (popupShowing && selected != null) {
                log.debug("onAction: inserting suggestion '{}'", selected);
                state.insertSuggestion(selected);
            } else if (originalAction != null) {
                log.debug("onAction: delegating to original handler");
                originalAction.handle(e);
            }
        });
    }

    /**
     * Detaches autocomplete from the given TextField, cleaning up listeners and popup.
     */
    static void detach(TextField field) {
        if (field == null) {
            return;
        }
        Object obj = field.getProperties().remove(PROP_STATE);
        if (obj instanceof State state) {
            state.hidePopup();
            field.removeEventFilter(KeyEvent.KEY_PRESSED, state.keyFilter);
            field.textProperty().removeListener(state.textListener);
            field.caretPositionProperty().removeListener(state.caretListener);
            field.focusedProperty().removeListener(state.focusListener);
        }
        // Restore the original onAction handler (only if we previously saved one)
        Object original = field.getProperties().remove(PROP_ORIGINAL_ACTION);
        if (original instanceof EventHandler<?> handler) {
            @SuppressWarnings("unchecked")
            EventHandler<ActionEvent> actionHandler = (EventHandler<ActionEvent>) handler;
            field.setOnAction(actionHandler);
        }
    }

    /**
     * Returns true if the autocomplete popup is currently showing for the given field.
     * Used by commit handlers to avoid committing while the user is selecting a suggestion.
     */
    static boolean isPopupShowing(TextField field) {
        if (field == null) {
            return false;
        }
        Object obj = field.getProperties().get(PROP_STATE);
        return obj instanceof State state && state.popup != null && state.popup.isShowing();
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

    // ── Suggestion gathering (package-private for testing) ──────────────

    static List<String> getSuggestions(ModelEditor editor, String excludeName) {
        String excludeUnderscore = excludeName != null
                ? excludeName.replace(' ', '_') : null;

        List<String> elements = new ArrayList<>();
        addNames(elements, editor.getStocks(), s -> s.name());
        addNames(elements, editor.getFlows(), f -> f.name());
        addNames(elements, editor.getAuxiliaries(), a -> a.name());
        addNames(elements, editor.getConstants(), c -> c.name());
        addNames(elements, editor.getLookupTables(), l -> l.name());
        addNames(elements, editor.getModules(), m -> m.instanceName());

        elements.replaceAll(name -> name.replace(' ', '_'));
        if (excludeUnderscore != null) {
            elements.remove(excludeUnderscore);
        }
        elements.sort(Comparator.naturalOrder());

        List<String> functions = new ArrayList<>(BUILT_IN_FUNCTIONS);
        functions.sort(Comparator.naturalOrder());

        List<String> all = new ArrayList<>(elements.size() + functions.size());
        all.addAll(elements);
        all.addAll(functions);
        return all;
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

    // ── Helpers ─────────────────────────────────────────────────────────

    private static boolean isIdentChar(char c) {
        return Character.isLetterOrDigit(c) || c == '_';
    }

    private static <T> void addNames(List<String> dest, List<T> items,
                                     java.util.function.Function<T, String> nameExtractor) {
        for (T item : items) {
            dest.add(nameExtractor.apply(item));
        }
    }

    static boolean isBuiltInFunction(String name) {
        return FUNCTION_SET.contains(name);
    }

    // ── Inner state per attachment ──────────────────────────────────────

    private static final class State {
        final TextField field;
        final ModelEditor editor;
        final String excludeName;

        Popup popup;
        ListView<String> listView;
        List<String> allSuggestions;
        Token currentToken;

        final EventHandler<KeyEvent> keyFilter;
        final ChangeListener<String> textListener;
        final ChangeListener<Number> caretListener;
        final ChangeListener<Boolean> focusListener;

        State(TextField field, ModelEditor editor, String excludeName) {
            this.field = field;
            this.editor = editor;
            this.excludeName = excludeName;

            keyFilter = this::handleKey;
            textListener = (obs, oldVal, newVal) -> updatePopup();
            caretListener = (obs, oldVal, newVal) -> updatePopup();
            focusListener = (obs, wasFocused, isFocused) -> {
                if (!isFocused) {
                    hidePopup();
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
                        event.consume();
                    }
                }
                default -> { }
            }
        }

        private void updatePopup() {
            Token token = extractToken(field.getText(), field.getCaretPosition());
            currentToken = token;

            if (token == null || token.prefix().length() < MIN_PREFIX_LENGTH) {
                hidePopup();
                return;
            }

            if (allSuggestions == null) {
                allSuggestions = getSuggestions(editor, excludeName);
            }

            List<String> filtered = filterSuggestions(allSuggestions, token.prefix());
            if (filtered.isEmpty()) {
                hidePopup();
                return;
            }

            ensurePopup();
            listView.getItems().setAll(filtered);
            listView.getSelectionModel().selectFirst();
            log.debug("updatePopup: {} suggestions for prefix '{}', first={}",
                    filtered.size(), token.prefix(),
                    listView.getSelectionModel().getSelectedItem());

            if (!popup.isShowing()) {
                Bounds screenBounds = field.localToScreen(field.getBoundsInLocal());
                if (screenBounds != null) {
                    popup.show(field, screenBounds.getMinX(),
                            screenBounds.getMaxY());
                }
            }
        }

        private void ensurePopup() {
            if (popup != null) {
                return;
            }
            listView = new ListView<>();
            listView.setPrefHeight(MAX_VISIBLE_ROWS * 24 + 2);
            listView.setPrefWidth(field.getWidth());
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
                        String selected = listView.getSelectionModel().getSelectedItem();
                        if (selected != null) {
                            log.debug("listView keyHandler: ENTER selected={}", selected);
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
            popup.getContent().add(listView);
        }

        private void insertSuggestion(String suggestion) {
            if (currentToken == null) {
                return;
            }
            String text = field.getText();
            String insertion = isBuiltInFunction(suggestion)
                    ? suggestion + "(" : suggestion;
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
    }
}
