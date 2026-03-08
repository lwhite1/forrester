package systems.courant.forrester.app.canvas;

import systems.courant.forrester.model.graph.FeedbackAnalysis;
import systems.courant.forrester.model.graph.FeedbackAnalysis.LoopInfo;
import systems.courant.forrester.model.graph.FeedbackAnalysis.LoopType;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;

/**
 * Navigation bar for stepping through individual feedback loops.
 * Shows "Loop 1 of N: R1 (reinforcing)" with Previous/Next/All buttons.
 * Displayed below the toolbar when loop highlighting is active and
 * the model contains at least one feedback loop.
 */
public class LoopNavigatorBar extends HBox {

    private final Button prevButton = new Button("\u25C0");
    private final Button nextButton = new Button("\u25B6");
    private final Button allButton = new Button("All");
    private final Label loopLabel = new Label();

    private Runnable onPrev;
    private Runnable onNext;
    private Runnable onShowAll;

    public LoopNavigatorBar() {
        setId("loopNavigatorBar");
        setAlignment(Pos.CENTER_LEFT);
        setSpacing(6);
        setPadding(new Insets(3, 8, 3, 8));
        setStyle("-fx-background-color: #EBF0F5; -fx-border-color: #D0D6DD; "
                + "-fx-border-width: 0 0 1 0;");

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

        getChildren().addAll(prevButton, nextButton, loopLabel, allButton);
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

    /**
     * Updates the navigator display based on the current loop analysis and active index.
     *
     * @param analysis     the full (unfiltered) loop analysis
     * @param activeIndex  -1 for "all loops", otherwise the 0-based loop index
     */
    public void update(FeedbackAnalysis analysis, int activeIndex) {
        if (analysis == null || analysis.loopCount() == 0) {
            loopLabel.setText("No loops detected");
            loopLabel.setTooltip(null);
            prevButton.setDisable(true);
            nextButton.setDisable(true);
            allButton.setDisable(true);
            return;
        }

        int count = analysis.loopCount();
        prevButton.setDisable(false);
        nextButton.setDisable(false);
        allButton.setDisable(activeIndex < 0);

        if (activeIndex < 0) {
            loopLabel.setText(count + (count == 1 ? " loop" : " loops")
                    + " \u2014 click \u25B6 to step");
            loopLabel.setTooltip(null);
        } else {
            analysis.loopInfo(activeIndex).ifPresent(info -> {
                String typeDesc = formatType(info.type());
                String text = "Loop " + (activeIndex + 1) + " of " + count
                        + ": " + info.label();
                if (typeDesc != null) {
                    text += " (" + typeDesc + ")";
                }
                loopLabel.setText(text);
                loopLabel.setTooltip(new Tooltip(info.narrative()));
            });
        }
    }

    private static String formatType(LoopType type) {
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
