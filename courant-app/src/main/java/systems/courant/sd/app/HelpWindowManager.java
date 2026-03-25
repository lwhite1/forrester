package systems.courant.sd.app;

import javafx.application.Platform;
import javafx.stage.Stage;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Supplier;

/**
 * Manages singleton help/tutorial window instances.
 * Replaces individual Stage fields in {@link ModelWindow} with a single typed registry.
 */
final class HelpWindowManager {

    private final Map<Object, Stage> windows = new LinkedHashMap<>();
    private final Stage owner;

    HelpWindowManager(Stage owner) {
        this.owner = owner;
    }

    /**
     * Shows the window of the given type, creating it via the factory if it does not
     * exist or is no longer showing. Returns the window instance (typed).
     */
    @SuppressWarnings("unchecked")
    <T extends Stage> T showOrBring(Class<T> type, Supplier<T> factory) {
        Stage existing = windows.get(type);
        if (existing != null && existing.isShowing()) {
            bringToFront(existing);
            return (T) existing;
        }
        T window = factory.get();
        if (window.getOwner() == null) {
            window.initOwner(owner);
        }
        window.show();
        windows.put(type, window);
        return window;
    }

    /**
     * Shows the window identified by a string key, creating it via the factory
     * if it does not exist or is no longer showing. Useful for content-driven
     * dialogs that share a class but differ by content (e.g., tutorial ID).
     */
    Stage showOrBring(String key, Supplier<? extends Stage> factory) {
        Stage existing = windows.get(key);
        if (existing != null && existing.isShowing()) {
            bringToFront(existing);
            return existing;
        }
        Stage window = factory.get();
        if (window.getOwner() == null) {
            window.initOwner(owner);
        }
        window.show();
        windows.put(key, window);
        return window;
    }

    /**
     * Closes all tracked help windows.
     */
    void closeAll() {
        for (Stage window : windows.values()) {
            if (window.isShowing()) {
                window.close();
            }
        }
        windows.clear();
    }

    /**
     * Forces a window to the front, working around platform-specific focus-stealing
     * prevention by briefly toggling alwaysOnTop.
     */
    static void bringToFront(Stage window) {
        window.setAlwaysOnTop(true);
        window.toFront();
        window.requestFocus();
        Platform.runLater(() -> window.setAlwaysOnTop(false));
    }
}
