package systems.courant.sd.app.canvas.dialogs;

import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.text.TextFlow;
import javafx.stage.Stage;

import systems.courant.sd.app.TutorialProgressStore;

import java.util.List;

/**
 * Base class for tabbed tutorial and reference dialogs. Provides the common
 * TabPane scaffolding, a {@link #createTab(String, TextFlow)} helper so
 * subclasses only need to supply tab content, and progress tracking via
 * {@link TutorialProgressStore}.
 *
 * <p>Subclasses that override {@link #getTutorialId()} participate in
 * progress tracking: the tutorial is marked complete when the user reaches
 * the last tab, and the user's position is saved for later resume.
 */
public abstract class AbstractTutorialDialog extends Stage {

    private final double contentMaxWidth;

    /**
     * @param title           the dialog window title
     * @param width           scene width
     * @param height          scene height
     * @param contentMaxWidth max width for TextFlow content inside tabs
     */
    protected AbstractTutorialDialog(String title, double width, double height,
                                      double contentMaxWidth) {
        this.contentMaxWidth = contentMaxWidth;
        setTitle(title);

        TabPane tabPane = new TabPane();
        tabPane.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        tabPane.getTabs().addAll(buildTabs());
        installProgressTracking(tabPane);

        setScene(new Scene(tabPane, width, height));
    }

    /**
     * Returns the unique tutorial identifier for progress tracking, or
     * {@code null} if this dialog does not participate in tracking.
     * Subclasses should override to return a stable, kebab-case ID
     * (e.g., {@code "first-model"}, {@code "sir-epidemic"}).
     */
    protected String getTutorialId() {
        return null;
    }

    /**
     * Subclasses return the ordered list of tabs to display.
     */
    protected abstract List<Tab> buildTabs();

    /**
     * Creates a scrollable tab wrapping a {@link TextFlow} content pane.
     */
    protected Tab createTab(String title, TextFlow content) {
        content.setPadding(new Insets(16));
        content.setLineSpacing(4);
        content.setMaxWidth(contentMaxWidth);

        ScrollPane scroll = new ScrollPane(content);
        scroll.setFitToWidth(true);

        return new Tab(title, scroll);
    }

    private void installProgressTracking(TabPane tabPane) {
        String tutorialId = getTutorialId();
        if (tutorialId == null) {
            return;
        }

        int tabCount = tabPane.getTabs().size();

        tabPane.getSelectionModel().selectedIndexProperty().addListener(
                (obs, oldIdx, newIdx) -> {
                    int idx = newIdx.intValue();
                    if (idx == tabCount - 1) {
                        TutorialProgressStore.markCompleted(tutorialId);
                        TutorialProgressStore.clearResumePoint();
                    } else {
                        TutorialProgressStore.setResumePoint(tutorialId, idx);
                    }
                });

        // Resume at the last-viewed step if the user is returning to this tutorial
        TutorialProgressStore.getResumePoint()
                .filter(rp -> rp.tutorialId().equals(tutorialId))
                .ifPresent(rp -> {
                    int target = Math.min(rp.stepIndex(), tabCount - 1);
                    if (target > 0) {
                        tabPane.getSelectionModel().select(target);
                    }
                });
    }
}
