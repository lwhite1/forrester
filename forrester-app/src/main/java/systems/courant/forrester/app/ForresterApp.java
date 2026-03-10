package systems.courant.forrester.app;

import systems.courant.forrester.app.canvas.Clipboard;

import javafx.application.Application;
import javafx.stage.Stage;
import javafx.stage.Window;

import java.util.ArrayList;
import java.util.List;

/**
 * Application lifecycle manager. Creates and tracks open {@link ModelWindow}s.
 * Each window is an independent model editor with its own canvas, undo stack, and file state.
 */
public class ForresterApp extends Application {

    private final List<ModelWindow> openWindows = new ArrayList<>();
    private final Clipboard clipboard = new Clipboard();

    @Override
    public void start(Stage primaryStage) {
        openWindow(primaryStage);
    }

    public void openNewWindow() {
        openWindow(new Stage());
    }

    private void openWindow(Stage stage) {
        ModelWindow window = new ModelWindow(stage, this, clipboard);
        openWindows.add(window);
        stage.setOnHidden(e -> {
            openWindows.remove(window);
            if (openWindows.isEmpty()) {
                // Close any remaining windows (help dialogs, etc.)
                List.copyOf(Window.getWindows()).forEach(w -> {
                    if (w instanceof Stage s) {
                        s.close();
                    }
                });
            }
        });
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
