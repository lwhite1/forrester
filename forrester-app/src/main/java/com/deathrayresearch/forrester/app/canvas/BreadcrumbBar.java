package com.deathrayresearch.forrester.app.canvas;

import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;

import java.util.List;
import java.util.function.IntConsumer;

/**
 * A horizontal bar that displays the navigation path as clickable breadcrumbs.
 * Hidden when at root level (path length <= 1). Clicking an ancestor segment
 * navigates back to that depth.
 */
public class BreadcrumbBar extends HBox {

    private static final String SEPARATOR = " \u203A ";

    private IntConsumer onNavigateTo;

    public BreadcrumbBar() {
        setSpacing(2);
        setPadding(new Insets(4, 8, 4, 8));
        setVisible(false);
        setManaged(false);
    }

    /**
     * Sets the callback invoked when the user clicks a breadcrumb segment.
     * The argument is the target depth (0 = root).
     */
    public void setOnNavigateTo(IntConsumer onNavigateTo) {
        this.onNavigateTo = onNavigateTo;
    }

    /**
     * Updates the breadcrumb display with the given path segments.
     * If the path has only one segment (root), the bar is hidden.
     *
     * @param path ordered list of names from root to current level
     */
    public void update(List<String> path) {
        getChildren().clear();

        if (path == null || path.size() <= 1) {
            setVisible(false);
            setManaged(false);
            return;
        }

        setVisible(true);
        setManaged(true);

        // Clickable ancestor segments
        for (int i = 0; i < path.size() - 1; i++) {
            if (i > 0) {
                Label sep = new Label(SEPARATOR);
                sep.setStyle(Styles.MUTED_TEXT);
                getChildren().add(sep);
            }

            Button button = new Button(path.get(i));
            button.setStyle(Styles.BREADCRUMB_LINK);
            int depth = i;
            button.setOnAction(e -> {
                if (onNavigateTo != null) {
                    onNavigateTo.accept(depth);
                }
            });
            getChildren().add(button);
        }

        // Separator before current
        Label sep = new Label(SEPARATOR);
        sep.setStyle(Styles.MUTED_TEXT);
        getChildren().add(sep);

        // Bold label for current level (not clickable)
        Label current = new Label(path.get(path.size() - 1));
        current.setStyle(Styles.BREADCRUMB_CURRENT);
        getChildren().add(current);
    }
}
