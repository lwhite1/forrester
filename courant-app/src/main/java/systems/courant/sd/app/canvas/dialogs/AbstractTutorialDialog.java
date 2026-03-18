package systems.courant.sd.app.canvas.dialogs;

import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.text.TextFlow;
import javafx.stage.Stage;

import java.util.List;

/**
 * Base class for tabbed tutorial and reference dialogs. Provides the common
 * TabPane scaffolding and a {@link #createTab(String, TextFlow)} helper so
 * subclasses only need to supply tab content.
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

        setScene(new Scene(tabPane, width, height));
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
}
