package systems.courant.sd.app.canvas;

import javafx.scene.Node;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Manages result tabs in the {@link DashboardPanel}. Encapsulates tab
 * creation, replacement, and cleanup so that new result types can be added
 * by calling {@link #ensureTab(String, Node)} — no additional fields or
 * close-handler branches needed.
 */
final class ResultTabManager {

    private final TabPane tabPane;
    private final Runnable onAllTabsClosed;
    private final Map<String, Tab> tabs = new LinkedHashMap<>();

    ResultTabManager(TabPane tabPane, Runnable onAllTabsClosed) {
        this.tabPane = tabPane;
        this.onAllTabsClosed = onAllTabsClosed;
    }

    /**
     * Creates a new tab with the given title and content, or replaces the
     * content of an existing tab with the same title. Returns the tab.
     */
    Tab ensureTab(String title, Node content) {
        Tab existing = tabs.get(title);
        if (existing != null && tabPane.getTabs().contains(existing)) {
            existing.setContent(content);
            return existing;
        }
        Tab tab = new Tab(title, content);
        tab.setOnClosed(e -> {
            tabs.remove(title);
            if (tabPane.getTabs().isEmpty()) {
                onAllTabsClosed.run();
            }
        });
        tabs.put(title, tab);
        tabPane.getTabs().add(tab);
        return tab;
    }

    /**
     * Returns the tab for the given title, or null if none exists.
     */
    Tab get(String title) {
        return tabs.get(title);
    }

    /**
     * Removes all tabs and clears internal state.
     */
    void clear() {
        tabs.clear();
        tabPane.getTabs().clear();
    }
}
