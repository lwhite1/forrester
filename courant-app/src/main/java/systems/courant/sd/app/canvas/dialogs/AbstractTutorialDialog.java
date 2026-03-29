package systems.courant.sd.app.canvas.dialogs;

import javafx.geometry.Insets;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Labeled;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.KeyCode;
import javafx.scene.text.Text;
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
    private final String tutorialId;

    /**
     * Constructs the dialog, calling {@link #buildTabs()} and
     * {@link #getTutorialId()} to obtain tabs and the tracking ID.
     *
     * @param title           the dialog window title
     * @param width           scene width
     * @param height          scene height
     * @param contentMaxWidth max width for TextFlow content inside tabs
     */
    protected AbstractTutorialDialog(String title, double width, double height,
                                      double contentMaxWidth) {
        this.contentMaxWidth = contentMaxWidth;
        this.tutorialId = getTutorialId();
        setTitle(title);

        TabPane tabPane = new TabPane();
        tabPane.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        tabPane.getTabs().addAll(buildTabs());
        installProgressTracking(tabPane);

        setScene(new Scene(tabPane, width, height));
    }

    /**
     * Constructs the dialog with pre-built tabs and an explicit tutorial ID,
     * bypassing {@link #buildTabs()} and {@link #getTutorialId()}.
     * Use this constructor when those methods depend on subclass fields that
     * are not yet initialized during the super constructor call.
     *
     * @param title           the dialog window title
     * @param width           scene width
     * @param height          scene height
     * @param contentMaxWidth max width for TextFlow content inside tabs
     * @param tutorialId      tutorial ID for progress tracking, or {@code null}
     * @param prebuiltTabs    tabs to display
     */
    protected AbstractTutorialDialog(String title, double width, double height,
                                      double contentMaxWidth, String tutorialId,
                                      List<Tab> prebuiltTabs) {
        this.contentMaxWidth = contentMaxWidth;
        this.tutorialId = tutorialId;
        setTitle(title);

        TabPane tabPane = new TabPane();
        tabPane.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        tabPane.getTabs().addAll(prebuiltTabs);
        installProgressTracking(tabPane);

        setScene(new Scene(tabPane, width, height));
    }

    /**
     * Returns the unique tutorial identifier for progress tracking, or
     * {@code null} if this dialog does not participate in tracking.
     * Subclasses may override to return a stable, kebab-case ID
     * (e.g., {@code "first-model"}, {@code "feedback-loops"}).
     *
     * <p>When using the pre-built-tabs constructor, this method is not
     * called during construction — the ID is passed explicitly instead.
     */
    protected String getTutorialId() {
        return null;
    }

    /**
     * Returns the tutorial ID used for progress tracking.
     */
    final String resolvedTutorialId() {
        return tutorialId;
    }

    /**
     * Subclasses return the ordered list of tabs to display.
     * Not called if pre-built tabs are supplied to the constructor.
     */
    protected abstract List<Tab> buildTabs();

    /**
     * Creates a scrollable tab wrapping a {@link TextFlow} content pane.
     */
    protected Tab createTab(String title, TextFlow content) {
        return createStyledTab(title, content, contentMaxWidth);
    }

    /**
     * Creates a scrollable tab. Static variant for use when tabs must be
     * built before the instance is fully constructed.
     */
    protected static Tab createStyledTab(String title, TextFlow content,
                                          double maxWidth) {
        content.setPadding(new Insets(16));
        content.setLineSpacing(4);
        content.setMaxWidth(maxWidth);
        makeTextCopyable(content);

        ScrollPane scroll = new ScrollPane(content);
        scroll.setFitToWidth(true);

        return new Tab(title, scroll);
    }

    /**
     * Makes the text in a {@link TextFlow} copyable via {@code Ctrl+C}
     * and a right-click context menu. The flow is made focusable and the
     * cursor changes to a text cursor to signal that text can be copied.
     */
    static void makeTextCopyable(TextFlow flow) {
        flow.setFocusTraversable(true);
        flow.setCursor(Cursor.TEXT);
        flow.setOnMouseClicked(e -> flow.requestFocus());

        flow.setOnKeyPressed(e -> {
            if (e.isShortcutDown() && e.getCode() == KeyCode.C) {
                copyTextToClipboard(flow);
                e.consume();
            }
        });

        MenuItem copyItem = new MenuItem("Copy All Text");
        copyItem.setOnAction(e -> copyTextToClipboard(flow));
        ContextMenu contextMenu = new ContextMenu(copyItem);
        flow.setOnContextMenuRequested(e -> {
            contextMenu.show(flow, e.getScreenX(), e.getScreenY());
            e.consume();
        });
    }

    private static void copyTextToClipboard(TextFlow flow) {
        StringBuilder sb = new StringBuilder();
        for (Node child : flow.getChildren()) {
            if (child instanceof Text t) {
                sb.append(t.getText());
            } else if (child instanceof Labeled labeled) {
                sb.append(labeled.getText());
            }
        }
        if (sb.isEmpty()) {
            return;
        }
        ClipboardContent content = new ClipboardContent();
        content.putString(sb.toString());
        Clipboard.getSystemClipboard().setContent(content);
    }

    private void installProgressTracking(TabPane tabPane) {
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
