package systems.courant.sd.app.canvas;

import systems.courant.sd.model.graph.FeedbackAnalysis;
import systems.courant.sd.model.graph.FeedbackAnalysis.LoopType;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.Scene;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Duration;

import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * Navigation bar for stepping through individual feedback loops.
 * Shows "Loop 1 of N: R1 (reinforcing)" with Previous/Next/All buttons
 * and R/B/All type filter toggles.
 * Displayed below the toolbar when loop highlighting is active and
 * the model contains at least one feedback loop.
 */
public class LoopNavigatorBar extends HBox {

    private static final String FILTER_BUTTON_STYLE =
            "-fx-font-size: 11px; -fx-padding: 2 6 2 6; -fx-min-width: 28;";
    private static final String FILTER_ACTIVE_STYLE = FILTER_BUTTON_STYLE
            + " -fx-background-color: #C8D6E5; -fx-border-color: #7B8FA0; -fx-border-radius: 3;";

    private final Button prevButton = new Button("\u25C0");
    private final Button nextButton = new Button("\u25B6");
    private final Button allButton = new Button("All");
    private final Label loopLabel = new Label();

    private final ToggleButton filterAllBtn = new ToggleButton("All");
    private final ToggleButton filterRBtn = new ToggleButton("Reinforcing");
    private final ToggleButton filterBBtn = new ToggleButton("Balancing");
    private final ToggleGroup filterGroup = new ToggleGroup();
    private final Button helpIcon = new Button("?");
    private final Button elementsButton = new Button("Elements");

    private final TextField nameField = new TextField();

    private Runnable onPrev;
    private Runnable onNext;
    private Runnable onShowAll;
    private Consumer<LoopType> onFilterChanged;
    private BiConsumer<String, String> onLoopRenamed;
    private Stage elementsPopup;
    private Map<String, String> loopNames = Map.of();
    private String activeLoopLabel;

    public LoopNavigatorBar() {
        setId("loopNavigatorBar");
        setAlignment(Pos.CENTER_LEFT);
        setSpacing(6);
        setPadding(new Insets(3, 8, 3, 8));
        setStyle("-fx-background-color: #EBF0F5; -fx-border-color: #D0D6DD; "
                + "-fx-border-width: 0 0 1 0;");

        configureNavigationButtons();
        configureFilterButtons();
        configureHelpIcon();
        configureElementsButton();
        assembleChildren();
    }

    private void configureNavigationButtons() {
        prevButton.setId("loopPrev");
        prevButton.setTooltip(new Tooltip("Previous loop ([)"));
        prevButton.setOnAction(e -> {
            if (onPrev != null) {
                onPrev.run();
            }
        });
        prevButton.setFocusTraversable(false);

        nextButton.setId("loopNext");
        nextButton.setTooltip(new Tooltip("Next loop (])"));
        nextButton.setOnAction(e -> {
            if (onNext != null) {
                onNext.run();
            }
        });
        nextButton.setFocusTraversable(false);

        allButton.setId("loopAll");
        allButton.setTooltip(new Tooltip("Show all loops"));
        allButton.setOnAction(e -> {
            if (onShowAll != null) {
                onShowAll.run();
            }
        });
        allButton.setFocusTraversable(false);

        loopLabel.setId("loopNavigatorLabel");
        loopLabel.setStyle("-fx-font-size: 12px;");

        nameField.setId("loopNameField");
        nameField.setStyle("-fx-font-size: 11px;");
        nameField.setPrefWidth(150);
        nameField.setMaxWidth(200);
        nameField.setPromptText("Name this loop");
        nameField.setVisible(false);
        nameField.setManaged(false);
        nameField.setFocusTraversable(false);
        nameField.setOnAction(e -> commitLoopName());
        nameField.focusedProperty().addListener((obs, wasFocused, isFocused) -> {
            if (!isFocused) {
                commitLoopName();
            }
        });
    }

    private void configureFilterButtons() {
        filterAllBtn.setId("filterAll");
        filterAllBtn.setToggleGroup(filterGroup);
        filterAllBtn.setSelected(true);
        filterAllBtn.setTooltip(new Tooltip("Show all loop types"));
        filterAllBtn.setStyle(FILTER_ACTIVE_STYLE);
        filterAllBtn.setFocusTraversable(false);

        filterRBtn.setId("filterR");
        filterRBtn.setToggleGroup(filterGroup);
        filterRBtn.setTooltip(new Tooltip("Show only reinforcing loops"));
        filterRBtn.setStyle(FILTER_BUTTON_STYLE);
        filterRBtn.setFocusTraversable(false);

        filterBBtn.setId("filterB");
        filterBBtn.setToggleGroup(filterGroup);
        filterBBtn.setTooltip(new Tooltip("Show only balancing loops"));
        filterBBtn.setStyle(FILTER_BUTTON_STYLE);
        filterBBtn.setFocusTraversable(false);

        filterGroup.selectedToggleProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal == null) {
                // Prevent deselecting all — reselect "All"
                filterAllBtn.setSelected(true);
                return;
            }
            updateFilterStyles();
            if (onFilterChanged != null) {
                onFilterChanged.accept(getSelectedFilter());
            }
        });
    }

    private void configureHelpIcon() {
        helpIcon.setId("loopHelpIcon");
        helpIcon.setStyle("-fx-font-size: 11px; -fx-font-weight: bold;"
                + " -fx-min-width: 22; -fx-min-height: 22;"
                + " -fx-max-width: 22; -fx-max-height: 22;"
                + " -fx-padding: 0; -fx-background-radius: 11;");
        helpIcon.setFocusTraversable(false);
        String helpText =
                "Feedback Loop Navigator\n\n"
                + "Use \u25C0 \u25B6 to step through individual loops, or 'All' to see them together.\n\n"
                + "Reinforcing (R) loops amplify change \u2014 growth breeds more growth.\n"
                + "Balancing (B) loops resist change \u2014 they push toward a goal or equilibrium.\n\n"
                + "In stock-and-flow models, feedback groups show which stocks mutually influence\n"
                + "each other's rates of change through flows and intermediary variables.\n\n"
                + "Hover over the loop label for a description of the feedback dynamics.";
        Tooltip helpTip = new Tooltip(helpText);
        helpTip.setWrapText(true);
        helpTip.setMaxWidth(400);
        helpTip.setShowDelay(Duration.millis(200));
        helpIcon.setTooltip(helpTip);
        helpIcon.setOnAction(e -> {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Feedback Loops");
            alert.setHeaderText("Feedback Loop Navigator");
            alert.setContentText(helpText);
            alert.getDialogPane().setMinWidth(450);
            alert.showAndWait();
        });
    }

    private void configureElementsButton() {
        elementsButton.setId("loopElements");
        elementsButton.setTooltip(new Tooltip("Show loop elements"));
        elementsButton.setStyle("-fx-font-size: 11px; -fx-padding: 2 8 2 8;");
        elementsButton.setFocusTraversable(false);
        elementsButton.setDisable(true);
    }

    private void showElementsPopup(FeedbackAnalysis.CausalLoop loop) {
        if (elementsPopup != null) {
            elementsPopup.close();
        }

        boolean isSFGroup = loop.type() == LoopType.INDETERMINATE
                && loop.label().startsWith("Feedback Group");

        VBox content = new VBox(2);
        content.setPadding(new Insets(6, 14, 10, 14));
        content.setStyle("-fx-background-color: white;");

        List<String> path = loop.path();
        if (isSFGroup) {
            // S&F feedback group: plain list of stocks (no polarity)
            for (String stock : path) {
                Label row = new Label("\u2022 " + stock);
                row.setStyle("-fx-font-size: 12px; -fx-padding: 1 0 1 4;");
                content.getChildren().add(row);
            }
        } else {
            // CLD loop: show polarity arrows between elements
            var polarities = loop.polarities();
            for (int i = 0; i < path.size(); i++) {
                String polSymbol = switch (polarities.get(i)) {
                    case POSITIVE -> " \u2192(+) ";
                    case NEGATIVE -> " \u2192(\u2212) ";
                    case UNKNOWN -> " \u2192(?) ";
                };
                String next = path.get((i + 1) % path.size());
                Label row = new Label(path.get(i) + polSymbol + next);
                row.setStyle("-fx-font-size: 12px; -fx-padding: 1 0 1 4;");
                content.getChildren().add(row);
            }
        }

        Stage popup = new Stage(StageStyle.UTILITY);
        popup.initModality(Modality.NONE);
        String windowTitle = isSFGroup
                ? loop.label() + " \u2014 " + path.size() + " Stocks"
                : loop.label() + " \u2014 " + formatType(loop.type());
        popup.setTitle(windowTitle);
        Scene scene = new Scene(content);
        popup.setScene(scene);
        popup.setAlwaysOnTop(true);

        // Position near the button
        var bounds = elementsButton.localToScreen(elementsButton.getBoundsInLocal());
        if (bounds != null) {
            popup.setX(bounds.getMinX());
            popup.setY(bounds.getMaxY() + 4);
        }

        popup.show();

        // Ensure window is wide enough for the title text
        double minWidth = computeTextWidth(windowTitle) + 60;
        if (popup.getWidth() < minWidth) {
            popup.setWidth(minWidth);
        }
        elementsPopup = popup;
    }

    private void assembleChildren() {
        Label filterLabel = new Label("Filter:");
        filterLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #666;");

        Region spacer = new Region();
        spacer.setPrefWidth(12);

        Region spacer2 = new Region();
        spacer2.setPrefWidth(8);

        getChildren().addAll(prevButton, nextButton, loopLabel, nameField, allButton,
                elementsButton,
                spacer, filterLabel, filterAllBtn, filterRBtn, filterBBtn,
                spacer2, helpIcon);
    }

    private LoopType getSelectedFilter() {
        if (filterRBtn.isSelected()) {
            return LoopType.REINFORCING;
        }
        if (filterBBtn.isSelected()) {
            return LoopType.BALANCING;
        }
        return null;
    }

    private void updateFilterStyles() {
        filterAllBtn.setStyle(filterAllBtn.isSelected() ? FILTER_ACTIVE_STYLE : FILTER_BUTTON_STYLE);
        filterRBtn.setStyle(filterRBtn.isSelected() ? FILTER_ACTIVE_STYLE : FILTER_BUTTON_STYLE);
        filterBBtn.setStyle(filterBBtn.isSelected() ? FILTER_ACTIVE_STYLE : FILTER_BUTTON_STYLE);
    }

    public void setOnPrev(Runnable callback) {
        this.onPrev = callback;
    }

    public void setOnNext(Runnable callback) {
        this.onNext = callback;
    }

    public void setOnShowAll(Runnable callback) {
        this.onShowAll = callback;
    }

    public void setOnFilterChanged(Consumer<LoopType> callback) {
        this.onFilterChanged = callback;
    }

    /**
     * Sets the callback invoked when the user names a loop.
     * Parameters: (loopLabel, customName) — customName may be empty to clear.
     */
    public void setOnLoopRenamed(BiConsumer<String, String> callback) {
        this.onLoopRenamed = callback;
    }

    /**
     * Sets the loop names map (typically from ViewDef). Call before update().
     */
    public void setLoopNames(Map<String, String> names) {
        this.loopNames = names != null ? names : Map.of();
    }

    /**
     * Updates the navigator display based on the current loop analysis, active index,
     * and type filter.
     *
     * @param analysis        the full (unfiltered) loop analysis
     * @param activeIndex     -1 for "all loops", otherwise the 0-based loop index
     * @param typeFilter      active type filter (null = all types)
     * @param filteredCount   number of loops matching the current type filter
     */
    public void update(FeedbackAnalysis analysis, int activeIndex,
                       LoopType typeFilter, int filteredCount) {
        boolean popupWasShowing = elementsPopup != null && elementsPopup.isShowing();
        activeLoopLabel = null;

        if (analysis == null) {
            loopLabel.setText("Loop analysis not available");
            loopLabel.setTooltip(null);
            prevButton.setDisable(true);
            nextButton.setDisable(true);
            allButton.setDisable(true);
            filterRBtn.setDisable(true);
            filterBBtn.setDisable(true);
            elementsButton.setDisable(true);
            hideNameField();
            closePopup();
            return;
        }
        if (analysis.loopCount() == 0) {
            loopLabel.setText("No feedback loops found");
            loopLabel.setTooltip(null);
            prevButton.setDisable(true);
            nextButton.setDisable(true);
            allButton.setDisable(true);
            filterRBtn.setDisable(true);
            filterBBtn.setDisable(true);
            elementsButton.setDisable(true);
            hideNameField();
            closePopup();
            return;
        }

        boolean hasTypedLoops = analysis.causalLoops().stream()
                .anyMatch(l -> l.type() == LoopType.REINFORCING || l.type() == LoopType.BALANCING);
        filterRBtn.setDisable(!hasTypedLoops);
        filterBBtn.setDisable(!hasTypedLoops);

        int displayCount = filteredCount;
        boolean hasMatches = displayCount > 0;
        prevButton.setDisable(!hasMatches);
        nextButton.setDisable(!hasMatches);
        allButton.setDisable(activeIndex < 0);

        elementsButton.setDisable(true);

        if (!hasMatches) {
            String filterDesc = typeFilter == LoopType.REINFORCING ? "reinforcing" : "balancing";
            loopLabel.setText("No " + filterDesc + " loops");
            loopLabel.setTooltip(null);
            hideNameField();
            closePopup();
        } else if (activeIndex < 0) {
            String suffix = typeFilter != null
                    ? " " + formatType(typeFilter)
                    : "";
            loopLabel.setText(displayCount + (displayCount == 1 ? " loop" : " loops")
                    + suffix + " \u2014 click \u25B6 to step");
            loopLabel.setTooltip(null);
            hideNameField();
            closePopup();
        } else {
            FeedbackAnalysis.CausalLoop activeLoop = analysis.causalLoops().get(activeIndex);
            analysis.loopInfo(activeIndex).ifPresent(info -> {
                // Show position within filtered set
                List<Integer> indices = analysis.filteredIndices(typeFilter);
                int pos = indices.indexOf(activeIndex) + 1;

                String text;
                if (info.type() == LoopType.INDETERMINATE
                        && info.label().startsWith("Feedback Group")) {
                    // S&F feedback group — show group name and stock count
                    text = info.label() + " (" + info.path().size() + " stocks)";
                    if (displayCount > 1) {
                        text = pos + " of " + displayCount + ": " + text;
                    }
                } else {
                    // CLD loop — show loop label and type
                    String typeDesc = formatType(info.type());
                    text = "Loop " + pos + " of " + displayCount
                            + ": " + info.label();
                    if (typeDesc != null) {
                        text += " (" + typeDesc + ")";
                    }
                }
                loopLabel.setText(text);
                Tooltip tip = new Tooltip(info.narrative());
                tip.setWrapText(true);
                tip.setMaxWidth(400);
                loopLabel.setTooltip(tip);

                activeLoopLabel = info.label();
                showNameField(info.label());
            });
            elementsButton.setDisable(false);
            elementsButton.setOnAction(e -> showElementsPopup(activeLoop));
            if (popupWasShowing) {
                showElementsPopup(activeLoop);
            }
        }
    }

    /**
     * Closes the elements popup if it is currently showing.
     */
    public void closePopup() {
        if (elementsPopup != null) {
            elementsPopup.close();
            elementsPopup = null;
        }
    }

    private void showNameField(String loopLabel) {
        String customName = loopNames.getOrDefault(loopLabel, "");
        nameField.setText(customName);
        nameField.setVisible(true);
        nameField.setManaged(true);
    }

    private void hideNameField() {
        nameField.setVisible(false);
        nameField.setManaged(false);
    }

    private void commitLoopName() {
        if (activeLoopLabel == null || onLoopRenamed == null) {
            return;
        }
        String name = nameField.getText().trim();
        String existing = loopNames.getOrDefault(activeLoopLabel, "");
        if (!name.equals(existing)) {
            onLoopRenamed.accept(activeLoopLabel, name);
        }
    }

    /**
     * Resets the filter toggle to "All" without firing the callback.
     */
    public void resetFilter() {
        filterAllBtn.setSelected(true);
        updateFilterStyles();
    }

    private static double computeTextWidth(String text) {
        javafx.scene.text.Text measure = new javafx.scene.text.Text(text);
        measure.setFont(javafx.scene.text.Font.getDefault());
        return measure.getLayoutBounds().getWidth();
    }

    public static String formatType(LoopType type) {
        if (type == null) {
            return null;
        }
        return switch (type) {
            case REINFORCING -> "reinforcing";
            case BALANCING -> "balancing";
            case INDETERMINATE -> "indeterminate";
        };
    }
}
